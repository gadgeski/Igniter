package com.gadgeski.igniter

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

    inner class IgniterEngine : Engine() {

        private val renderer = IgniterRenderer()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var drawJob: Job? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true) // Enable touch events!
            Log.d(TAG, "Engine.onCreate: Engine created")
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d(TAG, "Engine.onVisibilityChanged: visible=$visible")
            if (visible) {
                startDrawingLoop()
            } else {
                stopDrawingLoop()
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

        override fun onDestroy() {
            super.onDestroy()
            Log.d(TAG, "Engine.onDestroy: Engine destroyed")
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
