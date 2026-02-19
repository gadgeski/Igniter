package com.gadgeski.igniter.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.gadgeski.igniter.model.Particle

class IgniterRenderer {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Single Path object reused for drawing
    private val path = Path()

    private val particleSystem = ParticleSystem()
    private val scanlineSystem = ScanlineSystem()

    private var surfaceWidth = 0
    private var surfaceHeight = 0

    fun setSurfaceSize(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        // ScanlineSystemはresizeで線座標をキャッシュする
        scanlineSystem.resize(width, height)
    }

    fun updateTouch(x: Float, y: Float) {
        particleSystem.ignite(x, y)
    }

    /**
     * 加速度センサーから取得したX軸傾き値を設定する。
     * 範囲: -10 ~ +10
     */
    fun setTilt(tiltX: Float) {
        particleSystem.setTilt(tiltX)
    }

    fun draw(canvas: Canvas) {
        // Update physics first
        particleSystem.update(surfaceWidth, surfaceHeight)

        // Clear background
        canvas.drawColor(Color.BLACK)

        // Draw CRT Scanlines (HexGridの代替)
        scanlineSystem.draw(canvas)

        // Draw particles
        val particles = particleSystem.particles
        for (p in particles) {
            if (p.isActive) {
                drawParticleTrail(canvas, p)
            }
        }
    }

    private fun drawParticleTrail(canvas: Canvas, p: Particle) {
        path.reset()

        // Start from current position
        path.moveTo(p.x, p.y)

        var iterIndex = p.historyIndex - 1
        if (iterIndex < 0) iterIndex = p.historyX.size - 1

        paint.color = p.color
        paint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
        paint.strokeWidth = p.strokeWidth

        repeat(p.historyCount) {
            val histX = p.historyX[iterIndex]
            val histY = p.historyY[iterIndex]

            path.lineTo(histX, histY)

            iterIndex--
            if (iterIndex < 0) iterIndex = p.historyX.size - 1
        }

        canvas.drawPath(path, paint)
    }
}
