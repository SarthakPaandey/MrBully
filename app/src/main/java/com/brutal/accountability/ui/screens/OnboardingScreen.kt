package com.brutal.accountability.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brutal.accountability.ui.components.AnimatedScreen
import com.brutal.accountability.ui.components.BrutalButton
import com.brutal.accountability.ui.components.BrutalCard
import com.brutal.accountability.ui.components.BrutalTextField
import com.brutal.accountability.ui.components.SectionHeader
import com.brutal.accountability.ui.theme.BrutalRed
import com.brutal.accountability.ui.theme.BrutalRedSubtle
import com.brutal.accountability.ui.theme.CardSurface
import com.brutal.accountability.ui.theme.CardSurfaceElevated
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary

private data class PersonaOption(
    val name: String,
    val emoji: String,
    val tagline: String
)

private val personas = listOf(
    PersonaOption("Brutal Papa", "👨‍👦", "Disappointed father energy"),
    PersonaOption("Toxic Ex", "💔", "Your worst breakup personified"),
    PersonaOption("Army Havildar", "🪖", "Boot camp discipline"),
    PersonaOption("Corporate Satan Boss", "👔", "Your nightmare manager"),
    PersonaOption("Savage Best Friend", "🔥", "No filter, no mercy"),
    PersonaOption("Failed Version of Yourself", "🪞", "Mirror of regret"),
    PersonaOption("IIT Topper Cousin", "📚", "Sharma ji ka beta"),
    PersonaOption("Strict Tuition Teacher", "👩‍🏫", "Ruler-wielding terror")
)

@Composable
fun OnboardingScreen(onSave: (Map<String, String>) -> Unit) {
    val questions = listOf(
        "Nickname",
        "Profession",
        "Goal",
        "Insecurity",
        "Fear",
        "What are you avoiding?",
        "Most distracting app",
        "Who would be disappointed?",
        "Dream life"
    )
    val answers = remember { mutableStateMapOf<String, String>() }
    var selectedPersona by remember { mutableStateOf("Brutal Papa") }

    AnimatedScreen {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader(
                title = "Build Your Profile",
                icon = Icons.Default.Person
            )
            Text(
                "We need to know your weak spots to hold you accountable.",
                style = MaterialTheme.typography.bodyMedium
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, false)
            ) {
                itemsIndexed(questions) { index, q ->
                    AnimatedScreen(delayMillis = index * 50) {
                        BrutalTextField(
                            value = answers[q].orEmpty(),
                            onValueChange = { answers[q] = it },
                            label = q,
                            singleLine = q.length < 18
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Choose Your Bully",
                        icon = Icons.Default.Psychology
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(personas) { index, persona ->
                    AnimatedScreen(delayMillis = 400 + index * 60) {
                        val isSelected = selectedPersona == persona.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPersona = persona.name },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BrutalRedSubtle else CardSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(persona.emoji, style = MaterialTheme.typography.headlineMedium)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        persona.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                    Text(
                                        persona.tagline,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPersona = persona.name },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = BrutalRed,
                                        unselectedColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            BrutalButton(
                text = "CONTINUE →",
                onClick = {
                    val payload = questions.associateWith { answers[it].orEmpty() }.toMutableMap()
                    payload["Persona"] = selectedPersona
                    onSave(payload)
                },
                enabled = answers["Nickname"].isNullOrBlank().not() && answers["Goal"].isNullOrBlank().not()
            )
        }
    }
}
