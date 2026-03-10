package com.gadgeski.igniter

import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
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
import kotlin.math.hypot

@AndroidEntryPoint
class IgniterWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "IgniterWallpaperService"

        // 高負荷が必要な直後
        private const val ACTIVE_FRAME_MS = 16L      // 約60fps

        // 少し落ち着いた状態
        private const val IDLE_FRAME_MS = 33L        // 約30fps

        // しばらく静止している状態
        private const val DEEP_IDLE_FRAME_MS = 66L   // 約15fps

        // タッチ・大きな動きの直後は高fpsを維持
        private const val ACTIVE_MODE_KEEP_MS = 2_200L

        // そこからさらに静かな状態が続いたら deep idle へ
        private const val IDLE_MODE_KEEP_MS = 8_000L

        // 傾きのローパスフィルタ係数
        private const val ALPHA = 0.1f

        // 波の勢いを足す最低しきい値
        private const val WAVE_TRIGGER_THRESHOLD = 0.5f

        // フレーム間で溜め込む勢いの上限
        private const val MAX_PENDING_WAVE_MOMENTUM = 6.0f
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

    inner class IgniterEngine : Engine(), SensorEventListener {

        private val renderer = IgniterRenderer(applicationContext)
        private val eglHelper = EglHelper()

        // OpenGL専用単一スレッド
        private val glExecutor = Executors.newSingleThreadExecutor()
        private val glDispatcher = glExecutor.asCoroutineDispatcher()
        private val scope = CoroutineScope(SupervisorJob() + glDispatcher)

        private var drawJob: Job? = null

        @Volatile
        private var surfaceReady = false

        @Volatile
        private var engineVisible = false

        // センサー関連
        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null

        // ローパスフィルタ済み傾き
        private var smoothedTiltX = 0f
        private var smoothedTiltY = 0f

        // 前回センサー値
        private var lastAccelX = 0f
        private var lastAccelY = 0f
        private var isFirstSensorEvent = true

        // Renderer 反映待ち状態
        @Volatile
        private var pendingTiltX = 0f

        @Volatile
        private var pendingTiltY = 0f

        private val pendingStateLock = Any()
        private var pendingTouchX = 0f
        private var pendingTouchY = 0f
        private var hasPendingTouch = false
        private var pendingWaveMomentum = 0f

        @Volatile
        private var lastInteractionMs = 0L

        // 検証用: fpsモード切り替えログ
        private var lastLoggedFrameMs = -1L

        // テーマ監視
        private lateinit var prefs: SharedPreferences
        private val prefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == "selected_theme") {
                    val themeName = sharedPreferences.getString(
                        "selected_theme",
                        Theme.CYBERPUNK.name
                    ) ?: Theme.CYBERPUNK.name

                    val theme = try {
                        Theme.valueOf(themeName)
                    } catch (_: Exception) {
                        Theme.CYBERPUNK
                    }

                    Log.d(TAG, "Theme changed to $theme, scheduling reload on GL thread")
                    scope.launch {
                        if (surfaceReady && eglHelper.isReady) {
                            renderer.setTheme(theme)
                            lastInteractionMs = SystemClock.elapsedRealtime()
                        }
                    }
                }
            }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            Log.d(TAG, "Engine.onCreate")

            prefs = applicationContext.getSharedPreferences("igniter_prefs", MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)

            sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            Log.d(TAG, "Engine.onSurfaceCreated")

            scope.launch {
                if (holder == null) return@launch

                val success = eglHelper.init(holder)
                if (!success) {
                    Log.e(TAG, "EGL initialization failed")
                    return@launch
                }

                renderer.onSurfaceCreated(null, null)

                val themeName = prefs.getString(
                    "selected_theme",
                    Theme.CYBERPUNK.name
                ) ?: Theme.CYBERPUNK.name

                val initialTheme = try {
                    Theme.valueOf(themeName)
                } catch (_: Exception) {
                    Theme.CYBERPUNK
                }

                renderer.setTheme(initialTheme)

                val size = holder.surfaceFrame
                renderer.onSurfaceChanged(null, size.width(), size.height())

                surfaceReady = true
                lastInteractionMs = SystemClock.elapsedRealtime()
                lastLoggedFrameMs = -1L
                Log.d(TAG, "GL surface ready")

                if (engineVisible) {
                    startDrawingLoop()
                }
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
                if (surfaceReady && eglHelper.isReady) {
                    renderer.onSurfaceChanged(null, width, height)
                    lastInteractionMs = SystemClock.elapsedRealtime()
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            Log.d(TAG, "Engine.onSurfaceDestroyed")

            surfaceReady = false
            stopDrawingLoop()
            clearPendingState()

            runBlocking(glDispatcher) {
                renderer.release()
                eglHelper.destroySurface()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            engineVisible = visible
            Log.d(TAG, "Engine.onVisibilityChanged: visible=$visible")

            if (visible) {
                resetSensorTracking()
                sensorManager?.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
                lastInteractionMs = SystemClock.elapsedRealtime()
                lastLoggedFrameMs = -1L

                if (surfaceReady && eglHelper.isReady) {
                    startDrawingLoop()
                }
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
            surfaceReady = false
            clearPendingState()

            runBlocking(glDispatcher) {
                renderer.release()
                eglHelper.release()
            }

            scope.cancel()
            glExecutor.shutdown()
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)

            event ?: return

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    synchronized(pendingStateLock) {
                        pendingTouchX = event.x
                        pendingTouchY = event.y
                        hasPendingTouch = true
                    }
                    lastInteractionMs = SystemClock.elapsedRealtime()
                }
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

            val x = event.values[0]
            val y = event.values[1]

            // 1. 傾き（パララックス用）のローパスフィルタ
            smoothedTiltX = ALPHA * x + (1f - ALPHA) * smoothedTiltX
            smoothedTiltY = ALPHA * y + (1f - ALPHA) * smoothedTiltY

            pendingTiltX = smoothedTiltX
            pendingTiltY = smoothedTiltY

            // 2. 動きの大きさ（波のうねり用）
            if (isFirstSensorEvent) {
                lastAccelX = x
                lastAccelY = y
                isFirstSensorEvent = false
                return
            }

            val deltaX = x - lastAccelX
            val deltaY = y - lastAccelY
            val movement = hypot(deltaX, deltaY)

            lastAccelX = x
            lastAccelY = y

            if (movement > WAVE_TRIGGER_THRESHOLD) {
                synchronized(pendingStateLock) {
                    pendingWaveMomentum =
                        (pendingWaveMomentum + movement).coerceAtMost(MAX_PENDING_WAVE_MOMENTUM)
                }
                lastInteractionMs = SystemClock.elapsedRealtime()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // 必要になったら使う
        }

        private fun startDrawingLoop() {
            if (drawJob?.isActive == true) return

            Log.d(TAG, "Draw loop started")

            drawJob = scope.launch {
                while (isActive) {
                    val frameStartMs = SystemClock.elapsedRealtime()

                    if (engineVisible && surfaceReady && eglHelper.isReady) {
                        applyPendingRendererUpdates()
                        drawFrame()
                    }

                    val targetFrameMs = currentTargetFrameMs(frameStartMs)
                    logFrameModeIfNeeded(targetFrameMs, frameStartMs)

                    val frameElapsedMs = SystemClock.elapsedRealtime() - frameStartMs
                    val sleepMs = (targetFrameMs - frameElapsedMs).coerceAtLeast(0L)
                    delay(sleepMs)
                }
            }
        }

        private fun stopDrawingLoop() {
            drawJob?.cancel()
            drawJob = null
            lastLoggedFrameMs = -1L
            Log.d(TAG, "Draw loop stopped")
        }

        private fun currentTargetFrameMs(nowMs: Long): Long {
            val quietMs = nowMs - lastInteractionMs

            return when {
                quietMs <= ACTIVE_MODE_KEEP_MS -> ACTIVE_FRAME_MS
                quietMs <= IDLE_MODE_KEEP_MS -> IDLE_FRAME_MS
                else -> DEEP_IDLE_FRAME_MS
            }
        }

        private fun logFrameModeIfNeeded(targetFrameMs: Long, nowMs: Long) {
            if (targetFrameMs == lastLoggedFrameMs) return

            lastLoggedFrameMs = targetFrameMs

            val mode = when (targetFrameMs) {
                ACTIVE_FRAME_MS -> "ACTIVE_60FPS"
                IDLE_FRAME_MS -> "IDLE_30FPS"
                DEEP_IDLE_FRAME_MS -> "DEEP_IDLE_15FPS"
                else -> "UNKNOWN"
            }

            val quietMs = nowMs - lastInteractionMs
            Log.d(
                TAG,
                "Frame mode changed: $mode (target=${targetFrameMs}ms, quietMs=${quietMs}ms)"
            )
        }

        private fun applyPendingRendererUpdates() {
            renderer.updateTilt(pendingTiltX, pendingTiltY)

            var localHasTouch = false
            var localTouchX = 0f
            var localTouchY = 0f
            var localWaveMomentum = 0f

            synchronized(pendingStateLock) {
                if (hasPendingTouch) {
                    localHasTouch = true
                    localTouchX = pendingTouchX
                    localTouchY = pendingTouchY
                    hasPendingTouch = false
                }

                if (pendingWaveMomentum > 0f) {
                    localWaveMomentum = pendingWaveMomentum
                    pendingWaveMomentum = 0f
                }
            }

            if (localHasTouch) {
                renderer.updateTouch(localTouchX, localTouchY)
            }

            if (localWaveMomentum > 0f) {
                renderer.addWaveMomentum(localWaveMomentum)
            }
        }

        private fun drawFrame() {
            try {
                renderer.onDrawFrame(null)
                eglHelper.swapBuffers()
            } catch (e: Exception) {
                Log.e(TAG, "Error in drawFrame", e)
            }
        }

        private fun resetSensorTracking() {
            smoothedTiltX = 0f
            smoothedTiltY = 0f
            lastAccelX = 0f
            lastAccelY = 0f
            isFirstSensorEvent = true
        }

        private fun clearPendingState() {
            pendingTiltX = 0f
            pendingTiltY = 0f

            synchronized(pendingStateLock) {
                pendingTouchX = 0f
                pendingTouchY = 0f
                hasPendingTouch = false
                pendingWaveMomentum = 0f
            }
        }
    }
}