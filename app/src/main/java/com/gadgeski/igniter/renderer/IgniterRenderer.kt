package com.gadgeski.igniter.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import com.gadgeski.igniter.R
import com.gadgeski.igniter.opengl.ShaderHelper
import com.gadgeski.igniter.opengl.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 を使って背景テクスチャとデジタル波紋を描画する Renderer。
 * WallpaperService の GLThread から呼ばれる。
 */
class IgniterRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "IgniterRenderer"

        // フルスクリーンクワッドの頂点定義 (NDC座標, UV付き)
        // [X, Y, U, V]  ×  4頂点 (2トライアングルのトライアングルストリップ)
        private val FULLSCREEN_QUAD = floatArrayOf(
            -1f,  1f,  0f, 0f,  // 左上
            -1f, -1f,  0f, 1f,  // 左下
            1f,  1f,  1f, 0f,  // 右上
            1f, -1f,  1f, 1f   // 右下
        )

        private const val COORDS_PER_VERTEX = 4          // X,Y,U,V
        private const val VERTEX_STRIDE     = COORDS_PER_VERTEX * 4 // 4 bytes/float
        private const val RIPPLE_DURATION   = 2.0f        // 秒
    }

    // --- OpenGL リソース ---
    private var backgroundProgram = 0
    private var rippleProgram = 0
    private var backgroundTextureId = 0

    // 頂点バッファを宣言と同時に初期化（警告解消・安全性向上）
    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(FULLSCREEN_QUAD.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(FULLSCREEN_QUAD)
            position(0)
        }

    // --- 画面サイズ ---
    private var screenWidth  = 1f
    private var screenHeight = 1f

    // --- 加速度（傾き）状態 ---
    @Volatile private var tiltX = 0f
    @Volatile private var tiltY = 0f

    // --- タッチ状態（スレッドセーフのため @Volatile）---
    @Volatile private var touchNormX = 0.5f    // 正規化タッチ座標 X (0〜1)
    @Volatile private var touchNormY = 0.5f    // 正規化タッチ座標 Y (0〜1, 上=0)
    @Volatile private var touchStartMs = -1L   // タッチ開始時刻（ms）、-1=非アクティブ
    @Volatile private var isRippleActive = false

    // -------------------------------------------------------------------------
    // GLSurfaceView.Renderer コールバック
    // -------------------------------------------------------------------------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")

        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // ブレンド設定（波紋エフェクトはアルファブレンドで重ねる）
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // シェーダープログラムをビルド
        backgroundProgram = ShaderHelper.buildProgram(
            context,
            R.raw.background_vertex_shader,
            R.raw.background_fragment_shader
        )
        rippleProgram = ShaderHelper.buildProgram(
            context,
            R.raw.ripple_vertex_shader,
            R.raw.ripple_fragment_shader
        )
        if (backgroundProgram == 0 || rippleProgram == 0) {
            Log.e(TAG, "Shader program build failed!")
        }

        // 背景テクスチャをロード
        backgroundTextureId = TextureHelper.loadTexture(context, R.drawable.igniter_bg)
        if (backgroundTextureId == 0) {
            Log.e(TAG, "Background texture load failed!")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width x $height")
        GLES20.glViewport(0, 0, width, height)
        screenWidth  = width.toFloat()
        screenHeight = height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 1. 背景テクスチャを描画
        drawBackground()

        // 2. 波紋エフェクトを描画（アクティブな場合のみ）
        if (isRippleActive) {
            val elapsed = (SystemClock.elapsedRealtime() - touchStartMs) / 1000f
            if (elapsed <= RIPPLE_DURATION) {
                drawRipple(elapsed)
            } else {
                isRippleActive = false
            }
        }
    }

    // -------------------------------------------------------------------------
    // 描画処理
    // -------------------------------------------------------------------------

    private fun drawBackground() {
        if (backgroundProgram == 0 || backgroundTextureId == 0) return

        GLES20.glUseProgram(backgroundProgram)

        // 頂点バッファをバインド
        val posLoc = GLES20.glGetAttribLocation(backgroundProgram, "a_Position")
        val uvLoc  = GLES20.glGetAttribLocation(backgroundProgram, "a_TexCoord")
        val tiltLoc = GLES20.glGetUniformLocation(backgroundProgram, "u_Tilt")

        quadBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, quadBuffer)
        GLES20.glEnableVertexAttribArray(posLoc)

        quadBuffer.position(2) // UVはX,Yの後に続く
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, quadBuffer)
        GLES20.glEnableVertexAttribArray(uvLoc)

        // テクスチャをユニット0にバインド
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(backgroundProgram, "u_Texture"), 0)

        // シェーダーの u_Tilt に現在の傾きを渡す
        if (tiltLoc >= 0) {
            GLES20.glUniform2f(tiltLoc, tiltX, tiltY)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(uvLoc)
    }

    private fun drawRipple(elapsedSeconds: Float) {
        if (rippleProgram == 0) return

        GLES20.glUseProgram(rippleProgram)

        val posLoc = GLES20.glGetAttribLocation(rippleProgram, "a_Position")
        val uvLoc  = GLES20.glGetAttribLocation(rippleProgram, "a_TexCoord")

        quadBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, quadBuffer)
        GLES20.glEnableVertexAttribArray(posLoc)

        quadBuffer.position(2)
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, quadBuffer)
        GLES20.glEnableVertexAttribArray(uvLoc)

        // Uniform変数へ値を渡す
        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(rippleProgram, "u_Touch"),
            touchNormX, touchNormY
        )
        GLES20.glUniform1f(
            GLES20.glGetUniformLocation(rippleProgram, "u_Time"),
            elapsedSeconds
        )
        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(rippleProgram, "u_Resolution"),
            screenWidth, screenHeight
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(uvLoc)
    }

    // -------------------------------------------------------------------------
    // 外部から呼ばれるAPI
    // -------------------------------------------------------------------------

    /**
     * デバイスの傾き（加速度）センサーの値を更新する。
     */
    fun updateTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
    }

    /**
     * タッチイベントを受け取り、波紋アニメーションを（再）開始する。
     * メインスレッドから呼ばれるので @Volatile で保護。
     *
     * @param x タッチX座標（ピクセル）
     * @param y タッチY座標（ピクセル）
     */
    fun updateTouch(x: Float, y: Float) {
        // 正規化 (0〜1へ変換。Y軸: 上が0になるよう反転)
        touchNormX = x / screenWidth
        touchNormY = y / screenHeight  // GLSLのv_TexCoordと同じ向きのためそのまま

        // 前の波紋をキャンセルして新しい開始時刻をセット
        touchStartMs = SystemClock.elapsedRealtime()
        isRippleActive = true
    }

    /**
     * サーフェス破棄時にOpenGLリソースを解放する。
     * GL スレッドから呼ぶこと。
     */
    fun release() {
        TextureHelper.deleteTexture(backgroundTextureId)
        backgroundTextureId = 0
        if (backgroundProgram != 0) {
            GLES20.glDeleteProgram(backgroundProgram)
            backgroundProgram = 0
        }
        if (rippleProgram != 0) {
            GLES20.glDeleteProgram(rippleProgram)
            rippleProgram = 0
        }
    }
}