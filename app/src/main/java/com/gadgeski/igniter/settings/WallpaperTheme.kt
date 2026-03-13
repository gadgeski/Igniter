package com.gadgeski.igniter.settings

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.gadgeski.igniter.R

enum class WallpaperTheme(
    val displayName: String,
    val description: String,
    @field:DrawableRes val backgroundDrawableRes: Int,
    @field:RawRes val backgroundFragmentShaderRes: Int,
    @field:RawRes val rippleFragmentShaderRes: Int
) {
    CYBERPUNK(
        displayName = "Cyberpunk",
        description = "Neon glow with a futuristic cyber mood.",
        backgroundDrawableRes = R.drawable.igniter_bg,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader
    ),

    SUMMER_BEACH(
        displayName = "Summer Beach",
        description = "Warm beach atmosphere with soft water motion.",
        backgroundDrawableRes = R.drawable.bg_summer_beach,
        backgroundFragmentShaderRes = R.raw.bg_beach_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_beach_fragment_shader
    ),

    FLOWER_STORM(
        displayName = "Flower Storm",
        description = "Floral energy with a gentle flowing feel.",
        backgroundDrawableRes = R.drawable.bg_flower_storm,
        backgroundFragmentShaderRes = R.raw.bg_beach_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_beach_fragment_shader
    ),

    SILENT_CITY(
        displayName = "Silent City",
        description = "Quiet urban mood with calm, restrained motion.",
        backgroundDrawableRes = R.drawable.bg_silent_city,
        backgroundFragmentShaderRes = R.raw.bg_cyberpunk_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_cyberpunk_fragment_shader
    ),

    SPARKLING_SKY(
        displayName = "Sparkling Sky",
        description = "Light, airy sky scene with subtle shimmer.",
        backgroundDrawableRes = R.drawable.bg_sparkling_sky,
        backgroundFragmentShaderRes = R.raw.bg_beach_fragment_shader,
        rippleFragmentShaderRes = R.raw.ripple_beach_fragment_shader
    );

    companion object {
        fun fromName(name: String?): WallpaperTheme {
            return entries.firstOrNull { it.name == name } ?: CYBERPUNK
        }
    }
}