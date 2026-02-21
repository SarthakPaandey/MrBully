package com.brutal.accountability.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brutal.accountability.ui.components.*
import com.brutal.accountability.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

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

private data class DynamicQuestion(
    val id: String,
    val question: String,
    val type: String, // "text" or "dropdown"
    val options: List<String>
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onSave: (Map<String, String>) -> Unit,
    onFetchQuestion: suspend (Map<String, String>, String) -> String
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }
    
    var apiKey by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    
    // Dynamic steps state
    var isLoadingQuestion by remember { mutableStateOf(false) }
    val dynamicQuestions = remember { mutableStateListOf<DynamicQuestion>() }
    var selectedPersona by remember { mutableStateOf("Brutal Papa") }
    
    val totalDynamicQuestions = 3

    fun proceedToNext() {
        if (currentStep == 0) {
            answers["API_KEY"] = apiKey
            currentStep++
        } else if (currentStep == 1) {
            answers["Nickname"] = nickname
            answers["Goal"] = goal
            currentStep++
        } else if (currentStep >= 2 && currentStep < 2 + totalDynamicQuestions) {
            currentStep++
        } else if (currentStep == 2 + totalDynamicQuestions) {
            val payload = answers.toMutableMap()
            payload["Persona"] = selectedPersona
            onSave(payload)
        }
    }

    LaunchedEffect(currentStep) {
        val dynIndex = currentStep - 2
        if (dynIndex in 0 until totalDynamicQuestions) {
            if (dynIndex >= dynamicQuestions.size) {
                // Fetch new question
                isLoadingQuestion = true
                val jsonStr = onFetchQuestion(answers, apiKey)
                try {
                    val json = JSONObject(jsonStr)
                    val qText = json.optString("question", "What else are you hiding?")
                    val qType = json.optString("type", "text")
                    val optsArray = json.optJSONArray("options")
                    val opts = mutableListOf<String>()
                    if (optsArray != null) {
                        for (i in 0 until optsArray.length()) {
                            opts.add(optsArray.optString(i))
                        }
                    }
                    val newQ = DynamicQuestion(
                        id = "DynamicQ_$dynIndex",
                        question = qText,
                        type = qType,
                        options = opts
                    )
                    dynamicQuestions.add(newQ)
                    if (!answers.containsKey(newQ.question)) {
                        answers[newQ.question] = ""
                    }
                } catch (e: Exception) {
                    dynamicQuestions.add(
                        DynamicQuestion(
                            id = "DynamicQ_$dynIndex",
                            question = "What is your deepest flaw?",
                            type = "text",
                            options = emptyList()
                        )
                    )
                }
                isLoadingQuestion = false
            }
        }
    }

    AnimatedScreen {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / (3f + totalDynamicQuestions) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = BrutalRed,
                trackColor = CardSurface
            )

            AnimatedContent(targetState = currentStep, label = "OnboardingSteps") { step ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when {
                        step == 0 -> {
                            // API Key Step
                            SectionHeader(title = "Welcome to Hell.", icon = Icons.Default.Key)
                            Text(
                                "To destroy your laziness, we need an AI brain. Enter your Groq API Key.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            BrutalCard(showAccent = false) {
                                BrutalTextField(
                                    value = apiKey,
                                    onValueChange = { apiKey = it },
                                    label = "Groq API Key (gsk_...)"
                                )
                                Text(
                                    "Get it free from console.groq.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            BrutalButton(
                                text = "START INQUISITION →",
                                onClick = { proceedToNext() },
                                enabled = apiKey.isNotBlank()
                            )
                        }
                        step == 1 -> {
                            // Seed Step
                            SectionHeader(title = "Who are you?", icon = Icons.Default.Person)
                            Text(
                                "Give us the basics before the AI drills into your soul.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            BrutalCard(showAccent = false) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    BrutalTextField(
                                        value = nickname,
                                        onValueChange = { nickname = it },
                                        label = "What should we call you? (e.g. Loser, User)"
                                    )
                                    BrutalTextField(
                                        value = goal,
                                        onValueChange = { goal = it },
                                        label = "What's the goal you keep giving up on?"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            BrutalButton(
                                text = "NEXT →",
                                onClick = { proceedToNext() },
                                enabled = nickname.isNotBlank() && goal.isNotBlank()
                            )
                        }
                        step >= 2 && step < 2 + totalDynamicQuestions -> {
                            // Dynamic Questions Step
                            val dynIndex = step - 2
                            if (isLoadingQuestion || dynIndex >= dynamicQuestions.size) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = BrutalRed)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("AI is analyzing your profile...", color = TextSecondary)
                                }
                            } else {
                                val currentQ = dynamicQuestions[dynIndex]
                                SectionHeader(title = "Question ${dynIndex + 1}/$totalDynamicQuestions", icon = Icons.Default.Psychology)
                                Text(
                                    currentQ.question,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                BrutalCard(showAccent = false) {
                                    if (currentQ.type == "dropdown" && currentQ.options.isNotEmpty()) {
                                        // Dropdown/Radio simulation
                                        currentQ.options.forEach { opt ->
                                            val isSelected = answers[currentQ.question] == opt
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { answers[currentQ.question] = opt }
                                                    .padding(vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { answers[currentQ.question] = opt },
                                                    colors = RadioButtonDefaults.colors(selectedColor = BrutalRed)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(opt, color = if (isSelected) TextPrimary else TextSecondary)
                                            }
                                        }
                                    } else {
                                        BrutalTextField(
                                            value = answers[currentQ.question] ?: "",
                                            onValueChange = { answers[currentQ.question] = it },
                                            label = "Your honest answer"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                BrutalButton(
                                    text = "NEXT →",
                                    onClick = { proceedToNext() },
                                    enabled = !answers[currentQ.question].isNullOrBlank()
                                )
                            }
                        }
                        step == 2 + totalDynamicQuestions -> {
                            // Persona selection
                            SectionHeader(title = "Choose Your Bully", icon = Icons.Default.Person)
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(personas) { index, persona ->
                                    AnimatedScreen(delayMillis = index * 50) {
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
                            Spacer(modifier = Modifier.height(16.dp))
                            BrutalButton(
                                text = "FINALIZE PROFILE",
                                onClick = { proceedToNext() }
                            )
                        }
                    }
                }
            }
        }
    }
}
