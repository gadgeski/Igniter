package com.gadgeski.igniter

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gadgeski.igniter.settings.SettingsActivity
import com.gadgeski.igniter.ui.theme.IgniterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IgniterTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSetWallpaper = { setWallpaper() },
                        onOpenSettings = { openSettings() }
                    )
                }
            }
        }
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
    modifier: Modifier = Modifier,
    onSetWallpaper: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Igniter Wallpaper",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choose a theme and activate the live wallpaper."
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onSetWallpaper) {
            Text(text = "Set Wallpaper")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onOpenSettings) {
            Text(text = "Open Settings")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    IgniterTheme {
        MainScreen(
            onSetWallpaper = {},
            onOpenSettings = {}
        )
    }
}