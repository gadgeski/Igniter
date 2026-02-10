package com.gadgeski.igniter.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import com.gadgeski.igniter.model.Particle

class IgniterRenderer {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f // ~3dp roughly
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // Single Path object reused for drawing
    private val path = Path()

    private val particleSystem = ParticleSystem()
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    fun setSurfaceSize(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    fun updateTouch(x: Float, y: Float) {
        particleSystem.ignite(x, y)
    }

    fun draw(canvas: Canvas) {
        // Update physics first
        particleSystem.update(surfaceWidth, surfaceHeight)

        // Clear background
        canvas.drawColor(Color.BLACK)

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

        // Trace back history
        // history is a ring buffer implicitly, but we just stored it linearly in the model for simplicity
        // Ideally we connect points.
        // Let's just draw lines to history points for now.
        // To make a smooth trail, we should iterate backwards from current.
        
        var currentIndex = p.historyIndex
        var pointsDrawn = 0
        
        // Correct way to read ring buffer (newest is at historyIndex - 1)
        
        var iterIndex = currentIndex - 1
        if (iterIndex < 0) iterIndex = p.historyX.size - 1

        var lastX = p.x
        var lastY = p.y

        // Draw segments with varying opacity? 
        // Canvas.drawPath uses one paint. If we want gradients, we need Shader or multiple drawLines.
        // For "Laser Sparks", a solid path with single color fading by alpha might be tricky in one drawPath call without Shader.
        // Let's use a simpler approach: Draw the path with the particle's color, alpha based on `life`.
        // AND maybe a gradient shader is overkill for performance.
        // Let's just draw the path as a solid "streak" for now, fading by life.

        paint.color = p.color
        paint.alpha = (p.life * 255).toInt().coerceIn(0, 255)

        for (i in 0 until p.historyCount) {
             val histX = p.historyX[iterIndex]
             val histY = p.historyY[iterIndex]
             
             path.lineTo(histX, histY)
             
             iterIndex--
             if (iterIndex < 0) iterIndex = p.historyX.size - 1
        }
        
        canvas.drawPath(path, paint)
    }
}
