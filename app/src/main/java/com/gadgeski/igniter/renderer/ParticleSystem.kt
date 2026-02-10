package com.gadgeski.igniter.renderer

import android.graphics.Color
import com.gadgeski.igniter.model.Particle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleSystem {

    companion object {
        private const val POOL_SIZE = 200
        private const val EXPLOSION_COUNT = 30
        private const val SPEED_MIN = 10f
        private const val SPEED_MAX = 20f
        
        // Colors from Design Rules
        private const val COLOR_CYAN = 0xFF00E5FF.toInt()
        private const val COLOR_MAGENTA = 0xFFD500F9.toInt()
    }

    // Fixed pool of particles
    val particles = Array(POOL_SIZE) { Particle() }

    fun update(width: Int, height: Int) {
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
                    // Randomly switch color on bounce
                    p.color = if (Random.nextBoolean()) COLOR_CYAN else COLOR_MAGENTA
                }
            }
        }
    }

    fun ignite(startX: Float, startY: Float) {
        var spawned = 0
        // Find inactive particles to respawn
        for (i in particles.indices) {
            if (!particles[i].isActive) {
                val angle = Random.nextDouble() * 2 * Math.PI
                val speed = Random.nextDouble(SPEED_MIN.toDouble(), SPEED_MAX.toDouble()).toFloat()
                
                val dx = (cos(angle) * speed).toFloat()
                val dy = (sin(angle) * speed).toFloat()
                
                // Random start color
                val color = if (Random.nextBoolean()) COLOR_CYAN else COLOR_MAGENTA
                
                // Slightly randomized life
                val life = Random.nextDouble(0.8, 1.0).toFloat()

                particles[i].reset(startX, startY, dx, dy, color, life)
                
                spawned++
                if (spawned >= EXPLOSION_COUNT) break
            }
        }
    }
}
