package com.gadgeski.igniter

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.gadgeski.igniter.opengl.EglHelper
import com.gadgeski.igniter.renderer.IgniterRenderer
import com.gadgeski.igniter.settings.Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

@AndroidEntryPoint
class IgniterWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "IgniterWallpaperService"
        private const val TARGET_FPS_MS = 16L  // ~60fps
        private const val ALPHA = 0.1f
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroyed")
    }

    override fun onCreateEngine(): Engine {
        Log.d(TAG, "onCreateEngine: Creating new engine")
        return IgniterEngine()
    }

    // =========================================================================
    // Engine
    // =========================================================================

    inner class IgniterEngine : Engine(), SensorEventListener {

        private val renderer = IgniterRenderer(applicationContext)
        private val eglHelper = EglHelper()

        // 【最重要】OpenGL専用の単一スレッドを作成（EGLコンテキストを維持するため）
        private val glExecutor = Executors.newSingleThreadExecutor()
        private val glDispatcher = glExecutor.asCoroutineDispatcher()

        // すべてのGL処理をこのスコープ（専用スレッド）で実行する
        private val scope = CoroutineScope(SupervisorJob() + glDispatcher)
        private var drawJob: Job? = null

        // サーフェスの準備完了フラグ
        @Volatile private var surfaceReady = false

        // センサー関連
        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null
        
        // ローパスフィルタ用（0.0: 無反応 〜 1.0: そのまま）
        private var smoothedTiltX = 0f
        private var smoothedTiltY = 0f

        // テーマ監視
        private lateinit var prefs: SharedPreferences
        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "selected_theme") {
                val themeName = sharedPreferences.getString("selected_theme", Theme.CYBERPUNK.name) ?: Theme.CYBERPUNK.name
                val theme = try { Theme.valueOf(themeName) } catch (e: Exception) { Theme.CYBERPUNK }
                Log.d(TAG, "Theme changed to $theme, scheduling reload on GL thread")
                // 安全にGLスレッドでテーマを適用する
                scope.launch {
                    if (surfaceReady && eglHelper.isReady) {
                        renderer.setTheme(theme)
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // ライフサイクル
        // -------------------------------------------------------------------------

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            Log.d(TAG, "Engine.onCreate")

            prefs = applicationContext.getSharedPreferences("igniter_prefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)

            sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "Engine.onSurfaceCreated")

            // EGL の初期化と OpenGL リソースのセットアップは GL スレッドで行う
            scope.launch {
                if (holder == null) return@launch

                val success = eglHelper.init(holder)
                if (!success) {
                    Log.e(TAG, "EGL initialization failed")
                    return@launch
                }

                // GL コンテキストがカレントになっている前提で Renderer を初期化し、初期テーマをロード
                renderer.onSurfaceCreated(null, null)
                val themeName = prefs.getString("selected_theme", Theme.CYBERPUNK.name) ?: Theme.CYBERPUNK.name
                val initialTheme = try { Theme.valueOf(themeName) } catch(e: Exception) { Theme.CYBERPUNK }
                renderer.setTheme(initialTheme)

                val size = holder.surfaceFrame
                renderer.onSurfaceChanged(null, size.width(), size.height())

                surfaceReady = true
                Log.d(TAG, "GL surface ready")
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "Engine.onSurfaceChanged: $width x $height")

            scope.launch {
                if (surfaceReady) {
                    renderer.onSurfaceChanged(null, width, height)
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            Log.d(TAG, "Engine.onSurfaceDestroyed")
            surfaceReady = false

            // 【修正】非同期ではなく、同期(runBlocking)で確実にGLスレッド上でリソースを解放する
            runBlocking(glDispatcher) {
                renderer.release()
                eglHelper.destroySurface()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d(TAG, "Engine.onVisibilityChanged: visible=$visible")
            if (visible) {
                sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                startDrawingLoop()
            } else {
                sensorManager?.unregisterListener(this)
                stopDrawingLoop()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.d(TAG, "Engine.onDestroy")
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            sensorManager?.unregisterListener(this)
            stopDrawingLoop()

            // エンジン破棄時にEGLコンテキストも完全に解放
            runBlocking(glDispatcher) {
                eglHelper.release()
            }

            // 全てが終わってからスレッドプールを閉じる
            scope.cancel()
            glExecutor.shutdown()
        }

        // -------------------------------------------------------------------------
        // タッチイベント
        // -------------------------------------------------------------------------

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            event?.let {
                when (it.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> renderer.updateTouch(it.x, it.y)
                }
            }
        }

        // -------------------------------------------------------------------------
        // センサーイベント
        // -------------------------------------------------------------------------

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]

                // ローパスフィルタ適用
                smoothedTiltX = ALPHA * x + (1 - ALPHA) * smoothedTiltX
                smoothedTiltY = ALPHA * y + (1 - ALPHA) * smoothedTiltY

                renderer.updateTilt(smoothedTiltX, smoothedTiltY)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Use it if needed
        }

        // -------------------------------------------------------------------------
        // 描画ループ
        // -------------------------------------------------------------------------

        private fun startDrawingLoop() {
            if (drawJob?.isActive == true) return
            Log.d(TAG, "Draw loop started")

            drawJob = scope.launch {
                while (isActive) {
                    if (surfaceReady && eglHelper.isReady) {
                        drawFrame()
                    }
                    delay(TARGET_FPS_MS)
                }
            }
        }

        private fun stopDrawingLoop() {
            drawJob?.cancel()
            drawJob = null
            Log.d(TAG, "Draw loop stopped")
        }

        private fun drawFrame() {
            try {
                renderer.onDrawFrame(null)
                eglHelper.swapBuffers()
            } catch (e: Exception) {
                Log.e(TAG, "Error in drawFrame", e)
            }
        }
    }
}