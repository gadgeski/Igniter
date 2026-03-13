package com.gadgeski.igniter.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.gadgeski.igniter.ui.theme.IgniterTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "igniter_prefs"
        private const val KEY_SELECTED_THEME = "selected_theme"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val initialTheme = WallpaperTheme.fromName(
            prefs.getString(KEY_SELECTED_THEME, WallpaperTheme.CYBERPUNK.name)
        )

        setContent {
            IgniterTheme {
                SettingsScreen(
                    initialTheme = initialTheme,
                    onThemeSelected = { theme ->
                        prefs.edit {
                            putString(KEY_SELECTED_THEME, theme.name)
                        }
                    },
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    initialTheme: WallpaperTheme,
    onThemeSelected: (WallpaperTheme) -> Unit,
    onClose: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(initialTheme) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Wallpaper Settings") },
                actions = {
                    TextButton(onClick = onClose) {
                        Text("Done")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose a wallpaper theme",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Your selection is saved immediately and reflected in the live wallpaper.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    WallpaperTheme.entries.forEachIndexed { index, theme ->
                        ThemeRow(
                            theme = theme,
                            selected = selectedTheme == theme,
                            onClick = {
                                if (selectedTheme != theme) {
                                    selectedTheme = theme
                                    onThemeSelected(theme)

                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "${theme.displayName} applied"
                                        )
                                    }
                                }
                            }
                        )

                        if (index != WallpaperTheme.entries.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(
    theme: WallpaperTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RadioButton(
            selected = selected,
            onClick = null
        )
    }
}