package com.gadgeski.igniter.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import com.gadgeski.igniter.R
import com.gadgeski.igniter.opengl.ShaderHelper
import com.gadgeski.igniter.opengl.TextureHelper
import com.gadgeski.igniter.settings.Theme
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
        // [X, Y, U, V] × 4頂点 (2トライアングルのトライアングルストリップ)
        private val FULLSCREEN_QUAD = floatArrayOf(
            -1f,  1f,  0f, 0f,  // 左上
            -1f, -1f,  0f, 1f,  // 左下
            1f,  1f,  1f, 0f,  // 右上
            1f, -1f,  1f, 1f   // 右下
        )

        private const val COORDS_PER_VERTEX = 4
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4
        private const val RIPPLE_DURATION = 2.0f

        // 波の基本値は 0 にする
        private const val BASE_WAVE_INTENSITY = 0.0f
        private const val MAX_WAVE_INTENSITY = 3.0f

        // これ未満なら「静止」とみなして shader 側の水面計算を止める
        private const val WATER_MOTION_THRESHOLD = 0.02f
    }

    // --- 背景シェーダー location キャッシュ ---
    private var uTimeLocation = -1
    private var uTiltLocation = -1
    private var uWaveIntensityLocation = -1
    private var uEnableWaterMotionLocation = -1
    private var bgPosLoc = -1
    private var bgUvLoc = -1
    private var bgTextureLoc = -1

    // --- 波紋シェーダー location キャッシュ ---
    private var ripplePosLoc = -1
    private var rippleUvLoc = -1
    private var rippleTouchLoc = -1
    private var rippleTimeLoc = -1
    private var rippleResolutionLoc = -1

    // --- 波の連動用変数 ---
    private var targetWaveIntensity = BASE_WAVE_INTENSITY
    private var currentWaveIntensity = BASE_WAVE_INTENSITY

    // --- OpenGL リソース ---
    private var backgroundProgram = 0
    private var rippleProgram = 0
    private var backgroundTextureId = 0

    private var currentTheme = Theme.CYBERPUNK

    // 描画開始時刻
    private var rendererStartMs = 0L

    // 頂点バッファ
    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(FULLSCREEN_QUAD.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(FULLSCREEN_QUAD)
            position(0)
        }

    // --- 画面サイズ ---
    private var screenWidth = 1f
    private var screenHeight = 1f

    // --- 加速度（傾き）状態 ---
    @Volatile
    private var tiltX = 0f

    @Volatile
    private var tiltY = 0f

    // --- タッチ状態 ---
    @Volatile
    private var touchNormX = 0.5f

    @Volatile
    private var touchNormY = 0.5f

    @Volatile
    private var touchStartMs = -1L

    @Volatile
    private var isRippleActive = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        rendererStartMs = SystemClock.elapsedRealtime()

        GLES20.glClearColor(0f, 0f, 0f, 1f)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width x $height")
        GLES20.glViewport(0, 0, width, height)
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        // 波の勢いを静かな値へ戻す
        targetWaveIntensity = (targetWaveIntensity - 0.05f).coerceAtLeast(BASE_WAVE_INTENSITY)

        // 滑らかに補間
        currentWaveIntensity += (targetWaveIntensity - currentWaveIntensity) * 0.1f

        // しきい値未満は明示的に 0 扱いにして静止へ寄せる
        if (currentWaveIntensity < WATER_MOTION_THRESHOLD) {
            currentWaveIntensity = 0f
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        drawBackground()

        if (isRippleActive) {
            val elapsed = (SystemClock.elapsedRealtime() - touchStartMs) / 1000f
            if (elapsed <= RIPPLE_DURATION) {
                drawRipple(elapsed)
            } else {
                isRippleActive = false
            }
        }
    }

    private fun drawBackground() {
        if (backgroundProgram == 0 || backgroundTextureId == 0) return
        if (bgPosLoc < 0 || bgUvLoc < 0 || bgTextureLoc < 0) return

        GLES20.glUseProgram(backgroundProgram)

        quadBuffer.position(0)
        GLES20.glVertexAttribPointer(
            bgPosLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            quadBuffer
        )
        GLES20.glEnableVertexAttribArray(bgPosLoc)

        quadBuffer.position(2)
        GLES20.glVertexAttribPointer(
            bgUvLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            quadBuffer
        )
        GLES20.glEnableVertexAttribArray(bgUvLoc)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)
        GLES20.glUniform1i(bgTextureLoc, 0)

        if (uTiltLocation >= 0) {
            GLES20.glUniform2f(uTiltLocation, tiltX, tiltY)
        }

        if (uTimeLocation >= 0) {
            val rawElapsed = (SystemClock.elapsedRealtime() - rendererStartMs) / 1000.0
            val safeElapsed = (rawElapsed % (Math.PI * 1000.0)).toFloat()
            GLES20.glUniform1f(uTimeLocation, safeElapsed)
        }

        if (uWaveIntensityLocation >= 0) {
            GLES20.glUniform1f(uWaveIntensityLocation, currentWaveIntensity)
        }

        if (uEnableWaterMotionLocation >= 0) {
            val isWaterMotionEnabled = currentWaveIntensity > WATER_MOTION_THRESHOLD
            GLES20.glUniform1f(
                uEnableWaterMotionLocation,
                if (isWaterMotionEnabled) 1f else 0f
            )
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(bgPosLoc)
        GLES20.glDisableVertexAttribArray(bgUvLoc)
    }

    private fun drawRipple(elapsedSeconds: Float) {
        if (rippleProgram == 0) return
        if (ripplePosLoc < 0 || rippleUvLoc < 0) return

        GLES20.glUseProgram(rippleProgram)

        quadBuffer.position(0)
        GLES20.glVertexAttribPointer(
            ripplePosLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            quadBuffer
        )
        GLES20.glEnableVertexAttribArray(ripplePosLoc)

        quadBuffer.position(2)
        GLES20.glVertexAttribPointer(
            rippleUvLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            quadBuffer
        )
        GLES20.glEnableVertexAttribArray(rippleUvLoc)

        if (rippleTouchLoc >= 0) {
            GLES20.glUniform2f(rippleTouchLoc, touchNormX, touchNormY)
        }
        if (rippleTimeLoc >= 0) {
            GLES20.glUniform1f(rippleTimeLoc, elapsedSeconds)
        }
        if (rippleResolutionLoc >= 0) {
            GLES20.glUniform2f(rippleResolutionLoc, screenWidth, screenHeight)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(ripplePosLoc)
        GLES20.glDisableVertexAttribArray(rippleUvLoc)
    }

    /**
     * テーマを変更する。必ずGLスレッドから呼ぶこと。
     */
    fun setTheme(theme: Theme) {
        if (currentTheme == theme && backgroundProgram != 0) return

        Log.d(TAG, "setTheme: changing theme to $theme")
        currentTheme = theme

        release()

        val bgTextureRes: Int
        val bgFragmentStr: Int
        val rippleFragmentStr: Int

        when (theme) {
            Theme.CYBERPUNK -> {
                bgTextureRes = R.drawable.igniter_bg
                bgFragmentStr = R.raw.bg_cyberpunk_fragment_shader
                rippleFragmentStr = R.raw.ripple_cyberpunk_fragment_shader
            }

            Theme.SUMMER_BEACH -> {
                bgTextureRes = R.drawable.bg_summer_beach
                bgFragmentStr = R.raw.bg_beach_fragment_shader
                rippleFragmentStr = R.raw.ripple_beach_fragment_shader
            }
        }

        backgroundProgram = ShaderHelper.buildProgram(
            context,
            R.raw.background_vertex_shader,
            bgFragmentStr
        )
        rippleProgram = ShaderHelper.buildProgram(
            context,
            R.raw.ripple_vertex_shader,
            rippleFragmentStr
        )

        if (backgroundProgram == 0 || rippleProgram == 0) {
            Log.e(TAG, "Shader program build failed for $theme!")
        }

        // 背景シェーダー
        uTimeLocation = GLES20.glGetUniformLocation(backgroundProgram, "u_Time")
        uTiltLocation = GLES20.glGetUniformLocation(backgroundProgram, "u_Tilt")
        uWaveIntensityLocation = GLES20.glGetUniformLocation(backgroundProgram, "u_WaveIntensity")
        uEnableWaterMotionLocation =
            GLES20.glGetUniformLocation(backgroundProgram, "u_EnableWaterMotion")
        bgPosLoc = GLES20.glGetAttribLocation(backgroundProgram, "a_Position")
        bgUvLoc = GLES20.glGetAttribLocation(backgroundProgram, "a_TexCoord")
        bgTextureLoc = GLES20.glGetUniformLocation(backgroundProgram, "u_Texture")

        // 波紋シェーダー
        ripplePosLoc = GLES20.glGetAttribLocation(rippleProgram, "a_Position")
        rippleUvLoc = GLES20.glGetAttribLocation(rippleProgram, "a_TexCoord")
        rippleTouchLoc = GLES20.glGetUniformLocation(rippleProgram, "u_Touch")
        rippleTimeLoc = GLES20.glGetUniformLocation(rippleProgram, "u_Time")
        rippleResolutionLoc = GLES20.glGetUniformLocation(rippleProgram, "u_Resolution")

        backgroundTextureId = TextureHelper.loadTexture(context, bgTextureRes)
        if (backgroundTextureId == 0) {
            Log.e(TAG, "Background texture load failed for $theme!")
        }

        // テーマ変更直後に前テーマの残り勢いを持ち越さない
        targetWaveIntensity = BASE_WAVE_INTENSITY
        currentWaveIntensity = BASE_WAVE_INTENSITY
    }

    fun addWaveMomentum(momentum: Float) {
        targetWaveIntensity =
            (targetWaveIntensity + momentum * 0.5f).coerceAtMost(MAX_WAVE_INTENSITY)
    }

    fun updateTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
    }

    fun updateTouch(x: Float, y: Float) {
        touchNormX = x / screenWidth
        touchNormY = y / screenHeight

        touchStartMs = SystemClock.elapsedRealtime()
        isRippleActive = true
    }

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

        uTimeLocation = -1
        uTiltLocation = -1
        uWaveIntensityLocation = -1
        uEnableWaterMotionLocation = -1
        bgPosLoc = -1
        bgUvLoc = -1
        bgTextureLoc = -1

        ripplePosLoc = -1
        rippleUvLoc = -1
        rippleTouchLoc = -1
        rippleTimeLoc = -1
        rippleResolutionLoc = -1

        targetWaveIntensity = BASE_WAVE_INTENSITY
        currentWaveIntensity = BASE_WAVE_INTENSITY
    }
}