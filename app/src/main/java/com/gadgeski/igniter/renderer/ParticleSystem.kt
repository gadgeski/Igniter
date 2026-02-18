package com.gadgeski.igniter.renderer

import android.graphics.Color
import com.gadgeski.igniter.model.Particle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleSystem {

    companion object {
        // Limit Break Parameters
        private const val POOL_SIZE = 1000
        private const val EXPLOSION_COUNT = 50
        private const val SPEED_MIN = 15f
        private const val SPEED_MAX = 30f // Super fast

        // Colors from Design Rules
        private const val COLOR_CYAN = 0xFF00E5FF.toInt()
        private const val COLOR_MAGENTA = 0xFFD500F9.toInt()
        private const val COLOR_WHITE = 0xFFFFFFFF.toInt()

        // カメレオン・シフト: tiltXの最大値
        private const val TILT_MAX = 10f
        // 色相シフトの最大強度 (0~255のRGB加算量)
        private const val MAX_SHIFT = 80
    }

    // Fixed pool of particles
    val particles = Array(POOL_SIZE) { Particle() }

    // 加速度センサーから取得したX軸傾き値 (-10 ~ +10)
    @Volatile
    private var tiltX: Float = 0f

    /**
     * センサーから取得したX軸傾き値を設定する。
     * スレッドセーフのため @Volatile を使用。
     */
    fun setTilt(tiltX: Float) {
        this.tiltX = tiltX.coerceIn(-TILT_MAX, TILT_MAX)
    }

    fun update(width: Int, height: Int) {
        val currentTilt = tiltX // スナップショットを取得してループ内で一貫性を保つ

        particles.forEach { p ->
            if (p.isActive) {
                p.update()

                // Wall Bounce Logic
                var bounced = false
                if (p.x <= 0 || p.x >= width) {
                    p.dx = -p.dx
                    bounced = true
                }
                if (p.y <= 0 || p.y >= height) {
                    p.dy = -p.dy
                    bounced = true
                }

                if (bounced) {
                    // バウンス時に色を更新（Whiteは特別扱い）
                    if (p.color != COLOR_WHITE) {
                        val baseColor = if (Random.nextBoolean()) COLOR_CYAN else COLOR_MAGENTA
                        p.color = applyTiltShift(baseColor, currentTilt)
                    }
                }

                // Slower decay for longer trails
                if (p.life > 0) {
                    p.life -= 0.005f
                    if (p.life <= 0) p.isActive = false
                }
            }
        }
    }

    fun ignite(startX: Float, startY: Float) {
        val currentTilt = tiltX
        var spawned = 0
        // Find inactive particles to respawn
        for (i in particles.indices) {
            if (!particles[i].isActive) {
                val angle = Random.nextDouble() * 2 * Math.PI
                val speed = Random.nextDouble(SPEED_MIN.toDouble(), SPEED_MAX.toDouble()).toFloat()

                val dx = (cos(angle) * speed).toFloat()
                val dy = (sin(angle) * speed).toFloat()

                // Rare Spark (5%)
                val isRare = Random.nextDouble() < 0.05
                val baseColor = if (isRare) COLOR_WHITE else (if (Random.nextBoolean()) COLOR_CYAN else COLOR_MAGENTA)
                // Whiteはシフトしない（特別なスパーク）
                val color = if (isRare) baseColor else applyTiltShift(baseColor, currentTilt)

                // Randomized Stroke Width (1dp ~ 4dp roughly)
                val strokeWidth = Random.nextDouble(3.0, 12.0).toFloat()

                // Slightly randomized life
                val life = Random.nextDouble(0.8, 1.0).toFloat()

                particles[i].reset(startX, startY, dx, dy, color, life, strokeWidth)

                spawned++
                if (spawned >= EXPLOSION_COUNT) break
            }
        }
    }

    /**
     * カメレオン・シフト: tiltXに基づいて色のRGBチャンネルをシフトする。
     * Color.HSVToColor は重いため、簡易的なRGB加算で実装。
     *
     * tiltX > 0 (右傾き): 青/紫寄り → R成分を減らし、B成分を増やす
     * tiltX < 0 (左傾き): 緑/黄寄り → R成分を増やし、B成分を減らす
     */
    private fun applyTiltShift(baseColor: Int, tiltX: Float): Int {
        val ratio = tiltX / TILT_MAX // -1.0 ~ +1.0
        val shift = (ratio * MAX_SHIFT).toInt()

        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)

        // 右傾き(shift > 0): 青/紫方向 → R↓ B↑
        // 左傾き(shift < 0): 緑/黄方向 → R↑ B↓
        val newR = (r - shift).coerceIn(0, 255)
        val newG = g // Greenは変化させない（Cyan/Magentaの特性を維持）
        val newB = (b + shift).coerceIn(0, 255)

        return Color.argb(255, newR, newG, newB)
    }
}
