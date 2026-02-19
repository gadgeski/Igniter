package com.gadgeski.igniter.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

/**
 * CRTモニタ風の走査線を描画するシステム。
 * パフォーマンスのため、線座標は resize() でキャッシュし、onDraw では計算しない。
 */
class ScanlineSystem {

    companion object {
        // 4dp相当のピクセル間隔（density非依存で固定値を使用）
        private const val LINE_SPACING_PX = 8f
        // 基本Alpha値（極薄）
        private const val BASE_ALPHA = 50
        // チラつきのランダム変動幅
        private const val FLICKER_RANGE = 15
    }

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = false
    }

    // resize() でキャッシュされた線座標配列
    // drawLines の形式: [x0, y0, x1, y1, x2, y2, ...]
    private var lineCoords: FloatArray = FloatArray(0)
    private var lineCount = 0

    /**
     * サーフェスサイズ変更時に線座標を事前計算してキャッシュする。
     * onDraw では呼ばない。
     */
    fun resize(width: Int, height: Int) {
        val numLines = (height / LINE_SPACING_PX).toInt() + 1
        // 各線は 4要素 (x0, y0, x1, y1)
        lineCoords = FloatArray(numLines * 4)
        lineCount = 0

        var y = 0f
        var idx = 0
        while (y <= height) {
            lineCoords[idx++] = 0f
            lineCoords[idx++] = y
            lineCoords[idx++] = width.toFloat()
            lineCoords[idx++] = y
            lineCount++
            y += LINE_SPACING_PX
        }
    }

    /**
     * 走査線を描画する。
     * Alphaをランダムに ±FLICKER_RANGE 変動させてアナログな「チラつき」を再現する。
     */
    fun draw(canvas: Canvas) {
        if (lineCount == 0) return

        // チラつき: draw() ごとにAlphaをランダム変動
        val flicker = Random.nextInt(-FLICKER_RANGE, FLICKER_RANGE + 1)
        val alpha = (BASE_ALPHA + flicker).coerceIn(10, 80)
        paint.alpha = alpha

        canvas.drawLines(lineCoords, 0, lineCount * 4, paint)
    }
}
