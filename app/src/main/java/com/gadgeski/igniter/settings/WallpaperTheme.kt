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
    ),

    VIOLET_VENT(
        displayName = "Violet Vent",
        description = "Armored plating breathing violet light through honeycomb vents.",
        backgroundDrawableRes = R.drawable.bg_violet_vent,
        thumbnailDrawableRes = R.drawable.bg_violet_vent,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
        waveBoostScale = 0.26f,
        minVisibleWaveAmplitude = 0.12f,
        maxWaveAmplitude = 1.20f,
        waterPulseDurationSec = 1.7f,
        minWaveRetriggerMs = 400L
    ),

    MAGENTA_CORE(
        displayName = "Magenta Core",
        description = "The exposed circuitry of a machine, glowing magenta.",
        backgroundDrawableRes = R.drawable.bg_magenta_core,
        thumbnailDrawableRes = R.drawable.bg_magenta_core,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
        waveBoostScale = 0.30f,
        minVisibleWaveAmplitude = 0.14f,
        maxWaveAmplitude = 1.30f,
        waterPulseDurationSec = 1.5f,
        minWaveRetriggerMs = 380L
    ),

    IGNITION_LINE(
    displayName = "Ignition Line",
    description = "Cyan circuitry running vertical through dark plating.",
    backgroundDrawableRes = R.drawable.bg_ignition_line,
    thumbnailDrawableRes = R.drawable.bg_ignition_line,
    backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
    rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
    waveBoostScale = 0.28f,
    minVisibleWaveAmplitude = 0.13f,
    maxWaveAmplitude = 1.25f,
    waterPulseDurationSec = 1.6f,
    minWaveRetriggerMs = 390L
    ),

    EMBER_LINE(
    displayName = "Ember Line",
    description = "Molten lines tracing a frame of dark amber.",
    backgroundDrawableRes = R.drawable.bg_ember_line,
    thumbnailDrawableRes = R.drawable.bg_ember_line,
    backgroundFragmentShaderRes = R.raw.bg_large_paint_fragment_shader,
    rippleFragmentShaderRes = R.raw.ripple_large_paint_fragment_shader,
    waveBoostScale = 0.32f,
    minVisibleWaveAmplitude = 0.15f,
    maxWaveAmplitude = 1.40f,
    waterPulseDurationSec = 1.9f,
    minWaveRetriggerMs = 440L
    ),

    VIRIDIAN_LINE(
    displayName = "Viridian Line",
    description = "Green light seeping through layered hull plating.",
    backgroundDrawableRes = R.drawable.bg_viridian_line,
    thumbnailDrawableRes = R.drawable.bg_viridian_line,
    backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
    rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
    waveBoostScale = 0.24f,
    minVisibleWaveAmplitude = 0.11f,
    maxWaveAmplitude = 1.10f,
    waterPulseDurationSec = 1.5f,
    minWaveRetriggerMs = 420L
    ),

    IRIS_CORE(
    displayName = "Iris Core",
    description = "A monochrome interface ring at the center of the machine.",
    backgroundDrawableRes = R.drawable.bg_iris_core,
    thumbnailDrawableRes = R.drawable.bg_iris_core,
    backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
    rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
    waveBoostScale = 0.22f,
    minVisibleWaveAmplitude = 0.10f,
    maxWaveAmplitude = 1.05f,
    waterPulseDurationSec = 1.4f,
    minWaveRetriggerMs = 450L
    ),

    AMBER_IRIS(
        displayName = "Amber Iris",
        description = "A luminous interface ring pulsing in amber and lime.",
        backgroundDrawableRes = R.drawable.bg_amber_iris,
        thumbnailDrawableRes = R.drawable.bg_amber_iris,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
        waveBoostScale = 0.22f,
        minVisibleWaveAmplitude = 0.10f,
        maxWaveAmplitude = 1.05f,
        waterPulseDurationSec = 1.4f,
        minWaveRetriggerMs = 450L
    ),

    COBALT_CIRCUIT(
        displayName = "Cobalt Circuit",
        description = "Layered plating over dense cobalt circuitry.",
        backgroundDrawableRes = R.drawable.bg_cobalt_circuit,
        thumbnailDrawableRes = R.drawable.bg_cobalt_circuit,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
        waveBoostScale = 0.28f,
        minVisibleWaveAmplitude = 0.13f,
        maxWaveAmplitude = 1.25f,
        waterPulseDurationSec = 1.6f,
        minWaveRetriggerMs = 400L
    ),

    VOID_HULL(
        displayName = "Void Hull",
        description = "Unlit plating drifting against distant starlight.",
        backgroundDrawableRes = R.drawable.bg_void_hull,
        thumbnailDrawableRes = R.drawable.bg_void_hull,
        backgroundFragmentShaderRes = R.raw.bg_splashing_ink_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader,
        waveBoostScale = 0.20f,
        minVisibleWaveAmplitude = 0.09f,
        maxWaveAmplitude = 0.95f,
        waterPulseDurationSec = 1.3f,
        minWaveRetriggerMs = 500L
    ),

    ASH_CIRCUIT(
        displayName = "Ash Circuit",
        description = "Circuit traces drifting through smoke and ash.",
        backgroundDrawableRes = R.drawable.bg_ash_circuit,
        thumbnailDrawableRes = R.drawable.bg_ash_circuit,
        backgroundFragmentShaderRes = R.raw.bg_splashing_ink_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_splashing_ink_fragment_shader,
        waveBoostScale = 0.26f,
        minVisibleWaveAmplitude = 0.12f,
        maxWaveAmplitude = 1.20f,
        waterPulseDurationSec = 1.8f,
        minWaveRetriggerMs = 430L
    );

    companion object {
        fun fromName(name: String?): WallpaperTheme {
            return entries.firstOrNull { it.name == name } ?: CYBERPUNK
        }
    }
}