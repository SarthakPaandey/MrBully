package com.brutal.accountability.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brutal.accountability.ui.components.AnimatedScreen
import com.brutal.accountability.ui.components.BrutalButton
import com.brutal.accountability.ui.components.BrutalCard
import com.brutal.accountability.ui.components.BrutalSwitch
import com.brutal.accountability.ui.components.BrutalTextField
import com.brutal.accountability.ui.components.SectionHeader
import com.brutal.accountability.ui.theme.BrutalRed
import com.brutal.accountability.ui.theme.StatusGreen
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    strictMode: Boolean,
    savedApiKey: String,
    isAccessibilityEnabled: Boolean,
    isNotificationEnabled: Boolean,
    onToggleStrictMode: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenRestrictedApps: () -> Unit,
    onSavePhrase: (String) -> Unit,
    onSaveDaily: (String, String) -> Unit,
    onRememberNote: (String) -> Unit,
    onSaveApiKey: (String) -> Unit
) {
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var phrase by remember { mutableStateOf("") }
    var morningPlan by remember { mutableStateOf("") }
    var nightReflection by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        containerColor = com.brutal.accountability.ui.theme.DeepBlack
    ) { innerPadding ->
        AnimatedScreen {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Configure how strict this app should be.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }

                item {
                    BrutalCard {
                        SectionHeader(title = "Monitoring", icon = Icons.Default.Security)

                        val statusColor = if (isAccessibilityEnabled) StatusGreen else BrutalRed
                        val statusText = if (isAccessibilityEnabled) "Service is ACTIVE" else "Service is DISABLED"
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Enable accessibility service to detect restricted app launches.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val notificationStatusColor = if (isNotificationEnabled) StatusGreen else BrutalRed
                        val notificationStatusText = if (isNotificationEnabled) "Notifications are ON" else "Notifications are OFF"
                        Text(
                            text = notificationStatusText,
                            style = MaterialTheme.typography.titleSmall,
                            color = notificationStatusColor,
                            fontWeight = FontWeight.Bold
                        )
                        BrutalButton(
                            text = "OPEN ACCESSIBILITY SETTINGS",
                            onClick = onOpenAccessibilitySettings
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BrutalButton(
                            text = "OPEN NOTIFICATION SETTINGS",
                            onClick = onOpenNotificationSettings
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BrutalButton(
                            text = "MANAGE RESTRICTED APPS",
                            onClick = onOpenRestrictedApps
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Strict Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            BrutalSwitch(
                                checked = strictMode,
                                onCheckedChange = onToggleStrictMode
                            )
                        }
                    }
                }

                item {
                    BrutalCard {
                        SectionHeader(title = "Unlock Phrase", icon = Icons.Default.Lock)
                        Text(
                            "Type this exact phrase to escape intervention mode.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        BrutalTextField(
                            value = phrase,
                            onValueChange = { phrase = it },
                            label = "Exact unlock phrase"
                        )
                        BrutalButton(
                            text = "SAVE PHRASE",
                            onClick = {
                                onSavePhrase(phrase)
                                scope.launch { snackbarHostState.showSnackbar("Phrase saved") }
                            },
                            enabled = phrase.isNotBlank()
                        )
                    }
                }

                item {
                    BrutalCard {
                        SectionHeader(title = "Daily Check-in", icon = Icons.Default.WbSunny)
                        BrutalTextField(
                            value = morningPlan,
                            onValueChange = { morningPlan = it },
                            label = "☀️ Morning plan"
                        )
                        BrutalTextField(
                            value = nightReflection,
                            onValueChange = { nightReflection = it },
                            label = "🌙 Night reflection"
                        )
                        BrutalButton(
                            text = "SAVE CHECK-IN",
                            onClick = {
                                onSaveDaily(morningPlan, nightReflection)
                                scope.launch { snackbarHostState.showSnackbar("Daily check-in saved") }
                            }
                        )
                    }
                }

                item {
                    BrutalCard {
                        SectionHeader(title = "Remember This", icon = Icons.Default.SaveAlt)
                        Text(
                            "Save personal notes for better accountability context.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        BrutalTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = "Memory note"
                        )
                        BrutalButton(
                            text = "STORE MEMORY",
                            onClick = {
                                onRememberNote(note)
                                note = ""
                                scope.launch { snackbarHostState.showSnackbar("Memory saved") }
                            },
                            enabled = note.isNotBlank()
                        )
                    }
                }

                item {
                    BrutalCard(showAccent = false) {
                        SectionHeader(title = "Groq API Key", icon = Icons.Default.Key)
                        Text(
                            "Free key from console.groq.com",
                            style = MaterialTheme.typography.bodySmall
                        )
                        BrutalTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = "Paste Groq API key"
                        )
                        BrutalButton(
                            text = "SAVE KEY",
                            onClick = {
                                onSaveApiKey(apiKeyInput)
                                scope.launch { snackbarHostState.showSnackbar("API key saved") }
                            },
                            enabled = apiKeyInput.isNotBlank()
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
