package com.gadgeski.igniter.settings

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.gadgeski.igniter.R

enum class WallpaperTheme(
    val displayName: String,
    val description: String,
    @field:DrawableRes val backgroundDrawableRes: Int,
    @field:DrawableRes val thumbnailDrawableRes: Int,
    @field:RawRes val backgroundFragmentShaderRes: Int,
    @field:RawRes val rippleFragmentShaderRes: Int,

    // --- Motion profile ---
    val waveBoostScale: Float,
    val minVisibleWaveAmplitude: Float,
    val maxWaveAmplitude: Float,
    val waterPulseDurationSec: Float,
    val minWaveRetriggerMs: Long
) {
    CYBERPUNK(
        displayName = "Cyberpunk",
        description = "Neon glow with a futuristic cyber mood.",
        backgroundDrawableRes = R.drawable.bg_cyberpunk,
        thumbnailDrawableRes = R.drawable.bg_cyberpunk,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
        waveBoostScale = 0.28f,
        minVisibleWaveAmplitude = 0.14f,
        maxWaveAmplitude = 1.35f,
        waterPulseDurationSec = 1.8f,
        minWaveRetriggerMs = 380L
    ),

    SUMMER_BEACH(
        displayName = "Summer Beach",
        description = "Warm beach atmosphere with soft water motion.",
        backgroundDrawableRes = R.drawable.bg_summer_beach,
        thumbnailDrawableRes = R.drawable.bg_summer_beach,
        backgroundFragmentShaderRes = R.raw.bg_beach_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_beach_fragment_shader,
        waveBoostScale = 0.33f,
        minVisibleWaveAmplitude = 0.18f,
        maxWaveAmplitude = 1.55f,
        waterPulseDurationSec = 2.4f,
        minWaveRetriggerMs = 500L
    ),

    FLOWER_STORM(
        displayName = "Flower Storm",
        description = "Floral energy with a gentle flowing feel.",
        backgroundDrawableRes = R.drawable.bg_flower_storm,
        thumbnailDrawableRes = R.drawable.bg_flower_storm,
        backgroundFragmentShaderRes = R.raw.bg_flower_storm_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_flower_storm_fragment_shader,
        waveBoostScale = 0.38f,
        minVisibleWaveAmplitude = 0.22f,
        maxWaveAmplitude = 1.75f,
        waterPulseDurationSec = 2.7f,
        minWaveRetriggerMs = 520L
    ),

    SILENT_CITY(
        displayName = "Silent City",
        description = "Quiet urban mood with calm, restrained motion.",
        backgroundDrawableRes = R.drawable.bg_silent_city,
        thumbnailDrawableRes = R.drawable.bg_silent_city,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
        waveBoostScale = 0.22f,
        minVisibleWaveAmplitude = 0.10f,
        maxWaveAmplitude = 1.00f,
        waterPulseDurationSec = 1.5f,
        minWaveRetriggerMs = 650L
    ),

    SPARKLING_SKY(
        displayName = "Sparkling Sky",
        description = "Light, airy sky scene with subtle shimmer.",
        backgroundDrawableRes = R.drawable.bg_sparkling_sky,
        thumbnailDrawableRes = R.drawable.bg_sparkling_sky,
        backgroundFragmentShaderRes = R.raw.bg_beach_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_sparkling_sky_fragment_shader,
        waveBoostScale = 0.24f,
        minVisibleWaveAmplitude = 0.12f,
        maxWaveAmplitude = 1.15f,
        waterPulseDurationSec = 2.0f,
        minWaveRetriggerMs = 600L
    ),

    SUNLIGHT_TREES(
    displayName = "Sunlight Trees",
    description = "Dappled light through the leaves with a warm, glowing feel.",
    backgroundDrawableRes = R.drawable.bg_sunlight_trees,
    thumbnailDrawableRes = R.drawable.bg_sunlight_trees,
    backgroundFragmentShaderRes = R.raw.bg_sunlight_trees_fragment_shader,
    rippleFragmentShaderRes = R.raw.ripple_sunlight_trees_fragment_shader,
    waveBoostScale = 0.26f,
    minVisibleWaveAmplitude = 0.12f,
    maxWaveAmplitude = 1.20f,
    waterPulseDurationSec = 2.2f,
    minWaveRetriggerMs = 580L
    ),

    SPLASHING_INK(
        displayName = "Splashing Ink",
        description = "Abstract ink explosion with glitch and deep purple ripple.",
        backgroundDrawableRes = R.drawable.bg_splashing_ink,
        thumbnailDrawableRes = R.drawable.bg_splashing_ink,
        backgroundFragmentShaderRes = R.raw.bg_splashing_ink_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_splashing_ink_fragment_shader,
        waveBoostScale = 0.35f,
        minVisibleWaveAmplitude = 0.16f,
        maxWaveAmplitude = 1.45f,
        waterPulseDurationSec = 1.6f,
        minWaveRetriggerMs = 420L
    ),

    LARGE_PAINT(
        displayName = "Large Paint",
        description = "Vivid painted texture with pulsing light and fiery ripple.",
        backgroundDrawableRes = R.drawable.bg_large_paint,
        thumbnailDrawableRes = R.drawable.bg_large_paint,
        backgroundFragmentShaderRes = R.raw.bg_large_paint_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_large_paint_fragment_shader,
        waveBoostScale = 0.30f,
        minVisibleWaveAmplitude = 0.14f,
        maxWaveAmplitude = 1.40f,
        waterPulseDurationSec = 2.0f,
        minWaveRetriggerMs = 480L
    );

    companion object {
        fun fromName(name: String?): WallpaperTheme {
            return entries.firstOrNull { it.name == name } ?: CYBERPUNK
        }
    }
}