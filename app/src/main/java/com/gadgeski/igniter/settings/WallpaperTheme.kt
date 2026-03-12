package com.gadgeski.igniter.settings

enum class WallpaperTheme(
    val displayName: String
) {
    CYBERPUNK("Cyberpunk"),
    SUMMER_BEACH("Summer Beach"),
    FLOWER_STORM("Flower Storm"),
    SILENT_CITY("Silent City"),
    SPARKLING_SKY("Sparkling Sky");

    companion object {
        fun fromName(name: String?): WallpaperTheme {
            return entries.firstOrNull { it.name == name } ?: CYBERPUNK
        }
    }
}