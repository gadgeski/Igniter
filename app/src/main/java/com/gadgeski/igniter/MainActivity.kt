@file:Suppress("ktlint:standard:function-naming")

package com.gadgeski.igniter

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gadgeski.igniter.settings.SettingsActivity
import com.gadgeski.igniter.settings.WallpaperTheme
import com.gadgeski.igniter.ui.theme.IgniterTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "igniter_prefs"
        private const val KEY_SELECTED_THEME = "selected_theme"
    }

    private var currentTheme by mutableStateOf(WallpaperTheme.CYBERPUNK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadSelectedTheme()

        setContent {
            IgniterTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        currentTheme = currentTheme,
                        onSetWallpaper = { setWallpaper() },
                        onOpenSettings = { openSettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSelectedTheme()
    }

    private fun loadSelectedTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentTheme = WallpaperTheme.fromName(
            prefs.getString(KEY_SELECTED_THEME, WallpaperTheme.CYBERPUNK.name)
        )
    }

    private fun setWallpaper() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, IgniterWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Failed to open wallpaper picker",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

@Composable
fun MainScreen(
    currentTheme: WallpaperTheme,
    modifier: Modifier = Modifier,
    onSetWallpaper: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Igniter Wallpaper",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Choose a theme and activate the live wallpaper.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        CurrentThemeCard(theme = currentTheme)

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onSetWallpaper,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Set Wallpaper")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Open Settings")
        }
    }
}

@Composable
private fun CurrentThemeCard(theme: WallpaperTheme) {
    val thumbnailShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Current Theme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(id = theme.thumbnailDrawableRes),
                contentDescription = "${theme.displayName} preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(thumbnailShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    IgniterTheme {
        MainScreen(
            currentTheme = WallpaperTheme.CYBERPUNK,
            onSetWallpaper = {},
            onOpenSettings = {}
        )
    }
}