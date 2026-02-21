package com.gadgeski.igniter

import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.gadgeski.igniter.opengl.EglHelper
import com.gadgeski.igniter.renderer.IgniterRenderer
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

    inner class IgniterEngine : Engine() {

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

        // -------------------------------------------------------------------------
        // ライフサイクル
        // -------------------------------------------------------------------------

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            Log.d(TAG, "Engine.onCreate")
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

                // GL コンテキストがカレントになっている前提で Renderer を初期化
                renderer.onSurfaceCreated(null, null)

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
                startDrawingLoop()
            } else {
                stopDrawingLoop()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.d(TAG, "Engine.onDestroy")
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