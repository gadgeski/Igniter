```
Igniter/
├── AGENTS.md
├── Readme.md
├── STRUCTURE.md
├── app
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src
│       ├── androidTest
│       │   └── java
│       │       └── com
│       │           └── gadgeski
│       │               └── igniter
│       │                   └── ExampleInstrumentedTest.kt
│       ├── main
│       │   ├── AndroidManifest.xml
│       │   ├── ic_launcher-playstore.png
│       │   ├── java
│       │   │   └── com
│       │   │       └── gadgeski
│       │   │           └── igniter
│       │   │               ├── IgniterApp.kt
│       │   │               ├── IgniterWallpaperService.kt
│       │   │               ├── MainActivity.kt
│       │   │               ├── opengl
│       │   │               │   ├── EglHelper.kt
│       │   │               │   ├── ShaderHelper.kt
│       │   │               │   └── TextureHelper.kt
│       │   │               ├── renderer
│       │   │               │   └── IgniterRenderer.kt
│       │   │               ├── settings
│       │   │               │   ├── SettingsActivity.kt
│       │   │               │   └── WallpaperTheme.kt
│       │   │               └── ui
│       │   │                   └── theme
│       │   │                       ├── Color.kt
│       │   │                       ├── Theme.kt
│       │   │                       └── Type.kt
│       │   └── res
│       │       ├── drawable
│       │       │   ├── bg_amber_iris.webp
│       │       │   ├── bg_ash_circuit.webp
│       │       │   ├── bg_cobalt_circuit.webp
│       │       │   ├── bg_cyberpunk.webp
│       │       │   ├── bg_ember_line.webp
│       │       │   ├── bg_flower_storm.webp
│       │       │   ├── bg_ignition_line.webp
│       │       │   ├── bg_iris_core.webp
│       │       │   ├── bg_large_paint.webp
│       │       │   ├── bg_magenta_core.webp
│       │       │   ├── bg_silent_city.webp
│       │       │   ├── bg_sparkling_sky.webp
│       │       │   ├── bg_splashing_ink.webp
│       │       │   ├── bg_summer_beach.webp
│       │       │   ├── bg_sunlight_trees.webp
│       │       │   ├── bg_violet_vent.webp
│       │       │   ├── bg_viridian_line.webp
│       │       │   ├── bg_void_hull.webp
│       │       │   ├── ic_launcher_background.xml
│       │       │   └── ic_launcher_foreground.xml
│       │       ├── mipmap-anydpi
│       │       ├── mipmap-anydpi-v26
│       │       │   ├── ic_launcher.xml
│       │       │   └── ic_launcher_round.xml
│       │       ├── mipmap-hdpi
│       │       │   ├── ic_launcher.webp
│       │       │   ├── ic_launcher_foreground.webp
│       │       │   ├── ic_launcher_monochrome.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-mdpi
│       │       │   ├── ic_launcher.webp
│       │       │   ├── ic_launcher_foreground.webp
│       │       │   ├── ic_launcher_monochrome.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-xhdpi
│       │       │   ├── ic_launcher.webp
│       │       │   ├── ic_launcher_foreground.webp
│       │       │   ├── ic_launcher_monochrome.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-xxhdpi
│       │       │   ├── ic_launcher.webp
│       │       │   ├── ic_launcher_foreground.webp
│       │       │   ├── ic_launcher_monochrome.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── mipmap-xxxhdpi
│       │       │   ├── ic_launcher.webp
│       │       │   ├── ic_launcher_foreground.webp
│       │       │   ├── ic_launcher_monochrome.webp
│       │       │   └── ic_launcher_round.webp
│       │       ├── raw
│       │       │   ├── background_vertex_shader.glsl
│       │       │   ├── bg_beach_fragment_shader.glsl
│       │       │   ├── bg_cyberpunk_fragment_shader.glsl
│       │       │   ├── bg_flower_storm_fragment_shader.glsl
│       │       │   ├── bg_large_paint_fragment_shader.glsl
│       │       │   ├── bg_splashing_ink_fragment_shader.glsl
│       │       │   ├── bg_sunlight_trees_fragment_shader.glsl
│       │       │   ├── ripple_beach_fragment_shader.glsl
│       │       │   ├── ripple_cyberpunk_fragment_shader.glsl
│       │       │   ├── ripple_flower_storm_fragment_shader.glsl
│       │       │   ├── ripple_large_paint_fragment_shader.glsl
│       │       │   ├── ripple_sparkling_sky_fragment_shader.glsl
│       │       │   ├── ripple_splashing_ink_fragment_shader.glsl
│       │       │   ├── ripple_sunlight_trees_fragment_shader.glsl
│       │       │   └── ripple_vertex_shader.glsl
│       │       ├── values
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml
│       │           ├── backup_rules.xml
│       │           ├── data_extraction_rules.xml
│       │           └── wallpaper.xml
│       └── test
│           └── java
│               └── com
│                   └── gadgeski
│                       └── igniter
│                           └── ExampleUnitTest.kt
├── build.gradle.kts
├── gradle
│   ├── libs.versions.toml
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
└── settings.gradle.kts

37 directories, 93 files
```
