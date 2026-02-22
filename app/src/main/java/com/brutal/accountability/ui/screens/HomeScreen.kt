package com.brutal.accountability.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brutal.accountability.data.EventLogEntity
import com.brutal.accountability.ui.components.AnimatedScreen
import com.brutal.accountability.ui.components.BrutalButton
import com.brutal.accountability.ui.components.BrutalCard
import com.brutal.accountability.ui.components.SectionHeader
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    recentEvents: List<EventLogEntity>,
    onGenerateRoast: suspend () -> String
) {
    val scope = rememberCoroutineScope()
    var roastText by remember {
        mutableStateOf("Tu app khol raha hai, par apni life close kar raha hai.")
    }
    var isLoadingRoast by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val dayMillis = 24L * 60L * 60L * 1000L
    val opensToday = recentEvents.count { now - it.atMillis <= dayMillis }
    val opensRecent = recentEvents.size

    AnimatedScreen {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Track openings and face the roast.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }

            item {
                BrutalCard {
                    SectionHeader(title = "Open Count", icon = Icons.Default.QueryStats)
                    Text(
                        text = "Opened today: $opensToday times",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Recent tracked opens: $opensRecent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item {
                BrutalCard {
                    SectionHeader(title = "Thought + Roast", icon = Icons.Default.AutoAwesome)
                    Text(
                        text = "Thought: Every open is a vote for your old habits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (isLoadingRoast) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = roastText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    BrutalButton(
                        text = if (isLoadingRoast) "LOADING..." else "NEW ROAST",
                        enabled = !isLoadingRoast,
                        onClick = {
                            scope.launch {
                                isLoadingRoast = true
                                roastText = onGenerateRoast()
                                isLoadingRoast = false
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
