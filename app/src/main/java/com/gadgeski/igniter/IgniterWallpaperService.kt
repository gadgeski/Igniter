package com.gadgeski.igniter

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.gadgeski.igniter.renderer.IgniterRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class IgniterWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "IgniterWallpaperService"
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

        private val renderer = IgniterRenderer()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var drawJob: Job? = null

        // センサー管理
        // バッテリー対策: visible=true の時だけリスナーを登録する
        private val sensorManager: SensorManager by lazy {
            getSystemService(SENSOR_SERVICE) as SensorManager
        }
        private val accelerometer: Sensor? by lazy {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true) // Enable touch events!
            Log.d(TAG, "Engine.onCreate: Engine created")
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "Engine.onSurfaceChanged: $width x $height")
            renderer.setSurfaceSize(width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d(TAG, "Engine.onVisibilityChanged: visible=$visible")
            if (visible) {
                startDrawingLoop()
                // 表示時のみセンサーリスナーを登録（バッテリー対策）
                accelerometer?.let {
                    sensorManager.registerListener(
                        this,
                        it,
                        SensorManager.SENSOR_DELAY_GAME // ゲーム用: 約50Hz
                    )
                }
            } else {
                stopDrawingLoop()
                // 非表示時はセンサーリスナーを解除（バッテリー対策）
                sensorManager.unregisterListener(this)
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            event?.let {
                if (it.action == MotionEvent.ACTION_DOWN || it.action == MotionEvent.ACTION_MOVE) {
                    renderer.updateTouch(it.x, it.y)
                }
            }
        }

        // --- SensorEventListener ---

        override fun onSensorChanged(event: SensorEvent?) {
            // TYPE_ACCELEROMETER: values[0]=X, values[1]=Y, values[2]=Z
            // X軸: 左傾き=正値, 右傾き=負値 (重力の影響)
            // 範囲は約 -10 ~ +10 (m/s²)
            event?.let {
                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // X軸の傾きをRendererへ渡す
                    renderer.setTilt(it.values[0])
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // 精度変化は今回は無視
        }

        // --- Lifecycle ---

        override fun onDestroy() {
            super.onDestroy()
            Log.d(TAG, "Engine.onDestroy: Engine destroyed")
            sensorManager.unregisterListener(this) // 念のため解除
            scope.cancel() // Cancel all coroutines
        }

        private fun startDrawingLoop() {
            if (drawJob?.isActive == true) return
            Log.d(TAG, "Loop started")
            drawJob = scope.launch {
                while (isActive) {
                    drawFrame()
                    delay(16) // Target ~60fps
                }
            }
        }

        private fun stopDrawingLoop() {
            drawJob?.cancel()
            Log.d(TAG, "Loop stopped")
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: android.graphics.Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderer.draw(canvas)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in drawFrame", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unlocking canvas", e)
                    }
                }
            }
        }
    }
}
