package com.brutal.accountability.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.brutal.accountability.data.EventLogEntity
import com.brutal.accountability.ui.components.AnimatedScreen
import com.brutal.accountability.ui.components.BrutalButton
import com.brutal.accountability.ui.components.BrutalCard
import com.brutal.accountability.ui.components.BrutalSwitch
import com.brutal.accountability.ui.components.BrutalTextField
import com.brutal.accountability.ui.components.SectionHeader
import com.brutal.accountability.ui.theme.BrutalRed
import com.brutal.accountability.ui.theme.CardSurface
import com.brutal.accountability.ui.theme.StatusGreen
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun DashboardScreen(
    strictMode: Boolean,
    savedApiKey: String,
    recentEvents: List<EventLogEntity>,
    isAccessibilityEnabled: Boolean,
    onToggleStrictMode: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSavePhrase: (String) -> Unit,
    onSaveDaily: (String, String) -> Unit,
    onRememberNote: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onAskAiPartner: suspend (String) -> String
) {
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var phrase by remember { mutableStateOf("") }
    var morningPlan by remember { mutableStateOf("") }
    var nightReflection by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var partnerInput by remember { mutableStateOf("") }
    var partnerReply by remember { mutableStateOf<String?>(null) }
    var isPartnerLoading by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val dayMillis = 24L * 60L * 60L * 1000L
    val weekMillis = 7L * dayMillis
    val violationsToday = recentEvents.count { now - it.atMillis <= dayMillis }
    val violationsWeek = recentEvents.count { now - it.atMillis <= weekMillis }
    val topOffender = recentEvents
        .groupingBy { it.packageName.substringAfterLast('.') }
        .eachCount()
        .maxByOrNull { it.value }

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
                        text = "Your Accountability Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Customize your suffering.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // ─── Monitoring Status ───
                item {
                    BrutalCard {
                        SectionHeader(title = "Monitoring", icon = Icons.Default.Security)

                        val statusColor = if (isAccessibilityEnabled) StatusGreen else BrutalRed
                        val statusText = if (isAccessibilityEnabled) "Service is ACTIVE" else "Service is DISABLED"
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleSmall,
                            color = statusColor,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        Text(
                            "Enable accessibility service to detect restricted app launches.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        BrutalButton(
                            text = "OPEN ACCESSIBILITY SETTINGS",
                            onClick = onOpenAccessibilitySettings
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

                // ─── Home Stats ───
                item {
                    BrutalCard {
                        SectionHeader(title = "Home Stats", icon = Icons.Default.QueryStats)
                        Text(
                            text = "Violations today: $violationsToday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Violations in 7 days: $violationsWeek",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Top distraction: ${topOffender?.key ?: "None yet"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // ─── Intervention Phrase ───
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
                                scope.launch {
                                    snackbarHostState.showSnackbar("Phrase saved successfully")
                                }
                            },
                            enabled = phrase.isNotBlank()
                        )
                    }
                }

                // ─── Daily Check-in ───
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
                                scope.launch {
                                    snackbarHostState.showSnackbar("Daily check-in saved")
                                }
                            }
                        )
                    }
                }

                // ─── Memory Note ───
                item {
                    BrutalCard {
                        SectionHeader(title = "Remember This", icon = Icons.Default.SaveAlt)
                        Text(
                            "Feed the AI something personal. It'll use it against you.",
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
                                scope.launch {
                                    snackbarHostState.showSnackbar("Memory sent to AI for processing")
                                }
                            },
                            enabled = note.isNotBlank()
                        )
                    }
                }

                // ─── API Key ───
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
                                scope.launch {
                                    snackbarHostState.showSnackbar("API key saved securely")
                                }
                            },
                            enabled = apiKeyInput.isNotBlank()
                        )
                    }
                }

                // ─── AI Partner ───
                item {
                    BrutalCard {
                        SectionHeader(title = "AI Partner", icon = Icons.Default.Psychology)
                        Text(
                            "Ask for a plan, reset, or strict guidance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        BrutalTextField(
                            value = partnerInput,
                            onValueChange = { partnerInput = it },
                            label = "What do you need help with?"
                        )
                        BrutalButton(
                            text = if (isPartnerLoading) "ASKING..." else "ASK AI PARTNER",
                            onClick = {
                                scope.launch {
                                    isPartnerLoading = true
                                    partnerReply = onAskAiPartner(partnerInput)
                                    isPartnerLoading = false
                                }
                            },
                            enabled = partnerInput.isNotBlank() && !isPartnerLoading
                        )
                        partnerReply?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
