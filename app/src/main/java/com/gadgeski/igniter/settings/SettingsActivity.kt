package com.gadgeski.igniter.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gadgeski.igniter.ui.theme.IgniterTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = getSharedPreferences("igniter_prefs", Context.MODE_PRIVATE)

        setContent {
            IgniterTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Igniter Settings") }
                        )
                    }
                ) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        prefs = prefs
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, prefs: SharedPreferences) {
    var selectedTheme by remember {
        mutableStateOf(
            Theme.valueOf(
                prefs.getString("selected_theme", Theme.CYBERPUNK.name) ?: Theme.CYBERPUNK.name
            )
        )
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Theme Selection", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val radioOptions = listOf(Theme.CYBERPUNK, Theme.SUMMER_BEACH)

        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { theme ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (theme == selectedTheme),
                            onClick = {
                                selectedTheme = theme
                                prefs.edit().putString("selected_theme", theme.name).apply()
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (theme == selectedTheme),
                        onClick = null 
                    )
                    Text(
                        text = theme.name.replace("_", " "),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}
