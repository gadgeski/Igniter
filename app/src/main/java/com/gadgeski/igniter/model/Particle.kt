package com.gadgeski.igniter.model

import android.graphics.Color

/**
 * Represents a single particle in the simulation.
 * Optimized for object pooling (reusable).
 */
class Particle {
    var x: Float = 0f
    var y: Float = 0f
    var dx: Float = 0f
    var dy: Float = 0f
    var color: Int = Color.CYAN
    var life: Float = 0f

    // History for trail effect (Ring Buffer concept could be used, but fixed array is simple)
    // We'll store up to 10 past positions
    val historyX = FloatArray(10)
    val historyY = FloatArray(10)
    var historyIndex = 0
    var historyCount = 0

    var isActive: Boolean = false

    fun reset(startX: Float, startY: Float, startDx: Float, startDy: Float, startColor: Int, startLife: Float) {
        x = startX
        y = startY
        dx = startDx
        dy = startDy
        color = startColor
        life = startLife
        isActive = true
        
        // Reset history
        historyIndex = 0
        historyCount = 0
        for (i in historyX.indices) {
            historyX[i] = startX
            historyY[i] = startY
        }
    }

    fun update() {
        if (!isActive) return

        // Save current position to history before updating
        historyX[historyIndex] = x
        historyY[historyIndex] = y
        historyIndex = (historyIndex + 1) % historyX.size
        if (historyCount < historyX.size) {
            historyCount++
        }

        // Move
        x += dx
        y += dy

        // Decay life
        life -= 0.01f // Adjust decay rate as needed
        if (life <= 0) {
            isActive = false
        }
    }
}
