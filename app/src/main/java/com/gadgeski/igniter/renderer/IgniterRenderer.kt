package com.gadgeski.igniter.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import com.gadgeski.igniter.R
import com.gadgeski.igniter.opengl.ShaderHelper
import com.gadgeski.igniter.opengl.TextureHelper
import com.gadgeski.igniter.settings.WallpaperTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class IgniterRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "IgniterRenderer"

        private val FULLSCREEN_QUAD = floatArrayOf(
            -1f,  1f,  0f, 0f,
            -1f, -1f,  0f, 1f,
            1f,  1f,  1f, 0f,
            1f, -1f,  1f, 1f
        )

        private const val COORDS_PER_VERTEX = 4
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4
        private const val RIPPLE_DURATION = 2.0f
        private const val WATER_MOTION_THRESHOLD = 0.05f
    }

    // --- 背景シェーダー location キャッシュ ---
    private var uTimeLocation = -1
    private var uTiltLocation = -1
    private var uWaveIntensityLocation = -1
    private var uWaveProgressLocation = -1
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

    // --- OpenGL リソース ---
    private var backgroundProgram = 0
    private var rippleProgram = 0
    private var backgroundTextureId = 0

    private var currentTheme = WallpaperTheme.CYBERPUNK
    private var rendererStartMs = 0L

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

    // --- 傾き ---
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

    // --- 背景うねりパルス状態 ---
    private var currentWaveAmplitude = 0f
    private var currentWaveProgress = 1f
    private var isWaterPulseActive = false
    private var waterPulseStartMs = -1L
    private var lastWaveTriggerMs = -1L

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
        updateWaterPulseState()

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

    private fun updateWaterPulseState() {
        if (!isWaterPulseActive || waterPulseStartMs < 0L) {
            currentWaveAmplitude = 0f
            currentWaveProgress = 1f
            return
        }

        val elapsedSec = (SystemClock.elapsedRealtime() - waterPulseStartMs) / 1000f
        if (elapsedSec >= currentTheme.waterPulseDurationSec) {
            isWaterPulseActive = false
            currentWaveAmplitude = 0f
            currentWaveProgress = 1f
            return
        }

        currentWaveProgress =
            (elapsedSec / currentTheme.waterPulseDurationSec).coerceIn(0f, 1f)
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
            GLES20.glUniform1f(
                uWaveIntensityLocation,
                if (isWaterPulseActive) currentWaveAmplitude else 0f
            )
        }

        if (uWaveProgressLocation >= 0) {
            GLES20.glUniform1f(
                uWaveProgressLocation,
                if (isWaterPulseActive) currentWaveProgress else 1f
            )
        }

        if (uEnableWaterMotionLocation >= 0) {
            val enabled = isWaterPulseActive && currentWaveAmplitude > WATER_MOTION_THRESHOLD
            GLES20.glUniform1f(uEnableWaterMotionLocation, if (enabled) 1f else 0f)
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

    fun setTheme(theme: WallpaperTheme) {
        if (currentTheme == theme && backgroundProgram != 0) return

        Log.d(TAG, "setTheme: changing theme to $theme")
        currentTheme = theme

        release()

        backgroundProgram = ShaderHelper.buildProgram(
            context,
            R.raw.background_vertex_shader,
            theme.backgroundFragmentShaderRes
        )
        rippleProgram = ShaderHelper.buildProgram(
            context,
            R.raw.ripple_vertex_shader,
            theme.rippleFragmentShaderRes
        )

        if (backgroundProgram == 0 || rippleProgram == 0) {
            Log.e(TAG, "Shader program build failed for $theme!")
        }

        uTimeLocation = GLES20.glGetUniformLocation(backgroundProgram, "u_Time")
        uTiltLocation = GLES20.glGetUniformLocation(backgroundProgram, "u_Tilt")
        uWaveIntensityLocation = GLES20.glGetUniformLocation(backgroundProgram, "u_WaveIntensity")
        uWaveProgressLocation = GLES20.glGetUniformLocation(backgroundProgram, "u_WaveProgress")
        uEnableWaterMotionLocation =
            GLES20.glGetUniformLocation(backgroundProgram, "u_EnableWaterMotion")
        bgPosLoc = GLES20.glGetAttribLocation(backgroundProgram, "a_Position")
        bgUvLoc = GLES20.glGetAttribLocation(backgroundProgram, "a_TexCoord")
        bgTextureLoc = GLES20.glGetUniformLocation(backgroundProgram, "u_Texture")

        ripplePosLoc = GLES20.glGetAttribLocation(rippleProgram, "a_Position")
        rippleUvLoc = GLES20.glGetAttribLocation(rippleProgram, "a_TexCoord")
        rippleTouchLoc = GLES20.glGetUniformLocation(rippleProgram, "u_Touch")
        rippleTimeLoc = GLES20.glGetUniformLocation(rippleProgram, "u_Time")
        rippleResolutionLoc = GLES20.glGetUniformLocation(rippleProgram, "u_Resolution")

        backgroundTextureId = TextureHelper.loadTexture(context, theme.backgroundDrawableRes)
        if (backgroundTextureId == 0) {
            Log.e(TAG, "Background texture load failed for $theme!")
        }

        resetWaterPulseState()
    }

    fun addWaveMomentum(momentum: Float) {
        val nowMs = SystemClock.elapsedRealtime()
        val amplitudeBoost = (momentum * currentTheme.waveBoostScale).coerceIn(0.12f, 0.75f)

        if (lastWaveTriggerMs > 0L &&
            nowMs - lastWaveTriggerMs < currentTheme.minWaveRetriggerMs
        ) {
            currentWaveAmplitude =
                (currentWaveAmplitude + amplitudeBoost * 0.35f)
                    .coerceAtMost(currentTheme.maxWaveAmplitude)
            return
        }

        currentWaveAmplitude =
            (currentWaveAmplitude * 0.35f + amplitudeBoost)
                .coerceIn(
                    currentTheme.minVisibleWaveAmplitude,
                    currentTheme.maxWaveAmplitude
                )

        waterPulseStartMs = nowMs
        lastWaveTriggerMs = nowMs
        currentWaveProgress = 0f
        isWaterPulseActive = true
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
        uWaveProgressLocation = -1
        uEnableWaterMotionLocation = -1
        bgPosLoc = -1
        bgUvLoc = -1
        bgTextureLoc = -1

        ripplePosLoc = -1
        rippleUvLoc = -1
        rippleTouchLoc = -1
        rippleTimeLoc = -1
        rippleResolutionLoc = -1

        resetWaterPulseState()
    }

    private fun resetWaterPulseState() {
        currentWaveAmplitude = 0f
        currentWaveProgress = 1f
        isWaterPulseActive = false
        waterPulseStartMs = -1L
        lastWaveTriggerMs = -1L
    }
}