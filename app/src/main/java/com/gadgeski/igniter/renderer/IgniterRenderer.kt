package com.gadgeski.igniter.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class IgniterRenderer {

    private val paint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var circleX = 0f
    private var circleY = 0f
    private val radius = 50f

    fun updateTouch(x: Float, y: Float) {
        circleX = x
        circleY = y
    }

    fun draw(canvas: Canvas) {
        // Clear background
        canvas.drawColor(Color.BLACK)

        // Draw test circle
        canvas.drawCircle(circleX, circleY, radius, paint)
    }
}
