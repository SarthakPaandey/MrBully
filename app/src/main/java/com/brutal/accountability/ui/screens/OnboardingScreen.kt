package com.brutal.accountability.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import org.json.JSONObject

private enum class OnboardingQuestionType { TEXT, DROPDOWN }

private data class OnboardingQuestion(
    val key: String,
    val prompt: String,
    val type: OnboardingQuestionType,
    val options: List<String> = emptyList(),
    val inputLabel: String = "Your answer",
    val singleLine: Boolean = true
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

    val staticQuestions = remember {
        listOf(
            OnboardingQuestion(
                key = "Profession",
                prompt = "What stage are you in right now?",
                type = OnboardingQuestionType.DROPDOWN,
                options = listOf(
                    "College student",
                    "Working professional",
                    "Preparing for exams",
                    "Building startup/business",
                    "Freelancer / creator",
                    "Other"
                ),
                inputLabel = "Choose your current stage"
            ),
            OnboardingQuestion(
                key = "Relationship status",
                prompt = "What is your relationship situation?",
                type = OnboardingQuestionType.DROPDOWN,
                options = listOf(
                    "Single",
                    "In a relationship",
                    "Complicated",
                    "Recently went through a breakup",
                    "Prefer not to say"
                ),
                inputLabel = "Choose relationship status"
            ),
            OnboardingQuestion(
                key = "Is an ex still affecting focus?",
                prompt = "Is an ex still affecting your focus?",
                type = OnboardingQuestionType.DROPDOWN,
                options = listOf(
                    "Yes, a lot",
                    "Sometimes",
                    "No"
                ),
                inputLabel = "Choose one"
            ),
            OnboardingQuestion(
                key = "College or office environment",
                prompt = "Where do distractions hit you more?",
                type = OnboardingQuestionType.DROPDOWN,
                options = listOf(
                    "College",
                    "Office",
                    "Home",
                    "Everywhere"
                ),
                inputLabel = "Pick your main environment"
            ),
            OnboardingQuestion(
                key = "Gym status",
                prompt = "What is your gym / fitness situation?",
                type = OnboardingQuestionType.DROPDOWN,
                options = listOf(
                    "Regularly going",
                    "Inconsistent but trying",
                    "Want to start soon",
                    "Not a priority right now"
                ),
                inputLabel = "Pick your fitness status"
            ),
            OnboardingQuestion(
                key = "Who would be disappointed?",
                prompt = "Whose respect are you risking if you stay distracted?",
                type = OnboardingQuestionType.TEXT,
                inputLabel = "Name people who matter",
                singleLine = false
            ),
            OnboardingQuestion(
                key = "What are you avoiding?",
                prompt = "What hard task are you avoiding every day?",
                type = OnboardingQuestionType.TEXT,
                inputLabel = "Be brutally specific",
                singleLine = false
            ),
            OnboardingQuestion(
                key = "Current routine",
                prompt = "What does your current daily routine look like?",
                type = OnboardingQuestionType.TEXT,
                inputLabel = "Morning to night in short",
                singleLine = false
            ),
            OnboardingQuestion(
                key = "Insecurity",
                prompt = "What insecurity hurts you the most right now?",
                type = OnboardingQuestionType.TEXT,
                inputLabel = "The insecurity you hide",
                singleLine = false
            ),
            OnboardingQuestion(
                key = "Fear",
                prompt = "If this distraction continues, what are you most scared of?",
                type = OnboardingQuestionType.TEXT,
                inputLabel = "Your biggest fear",
                singleLine = false
            )
        )
    }

    val totalAiQuestions = 2
    val totalQuestionSteps = staticQuestions.size + totalAiQuestions
    val finalQuestionStep = 1 + totalQuestionSteps

    var isLoadingQuestion by remember { mutableStateOf(false) }
    val aiQuestions = remember { mutableStateListOf<OnboardingQuestion>() }

    fun currentQuestion(step: Int): OnboardingQuestion? {
        val questionIndex = step - 2
        if (questionIndex !in 0 until totalQuestionSteps) return null
        return if (questionIndex < staticQuestions.size) {
            staticQuestions[questionIndex]
        } else {
            val aiIndex = questionIndex - staticQuestions.size
            aiQuestions.getOrNull(aiIndex)
        }
    }

    fun proceedToNext() {
        when {
            currentStep == 0 -> {
                answers["API_KEY"] = apiKey.trim()
                currentStep++
            }

            currentStep == 1 -> {
                answers["Nickname"] = nickname.trim()
                answers["Goal"] = goal.trim()
                currentStep++
            }

            currentStep in 2..finalQuestionStep -> {
                if (currentStep < finalQuestionStep) {
                    currentStep++
                } else {
                    onSave(answers.toMap())
                }
            }
        }
    }

    LaunchedEffect(currentStep) {
        val questionIndex = currentStep - 2
        val aiIndex = questionIndex - staticQuestions.size
        if (aiIndex in 0 until totalAiQuestions && aiIndex >= aiQuestions.size) {
            isLoadingQuestion = true
            try {
                val context = answers.toMutableMap().apply {
                    put("Nickname", nickname)
                    put("Goal", goal)
                }
                val jsonStr = onFetchQuestion(context, apiKey)
                val json = JSONObject(jsonStr)
                val questionText = json.optString("question", "What else are you hiding?")
                    .ifBlank { "What else are you hiding?" }
                val typeText = json.optString("type", "text")
                val options = buildList {
                    val array = json.optJSONArray("options")
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val option = array.optString(i).trim()
                            if (option.isNotBlank()) add(option)
                        }
                    }
                }.distinct().take(6)

                val existingKeys = answers.keys + staticQuestions.map { it.key } + aiQuestions.map { it.key }
                val key = if (questionText in existingKeys) {
                    "$questionText (${aiIndex + 1})"
                } else {
                    questionText
                }

                val question = OnboardingQuestion(
                    key = key,
                    prompt = questionText,
                    type = if (typeText.equals("dropdown", ignoreCase = true) && options.isNotEmpty()) {
                        OnboardingQuestionType.DROPDOWN
                    } else {
                        OnboardingQuestionType.TEXT
                    },
                    options = options,
                    inputLabel = if (options.isNotEmpty()) "Choose one" else "Your honest answer",
                    singleLine = options.isNotEmpty()
                )
                aiQuestions.add(question)
                if (!answers.containsKey(question.key)) {
                    answers[question.key] = ""
                }
            } catch (_: Exception) {
                val fallback = OnboardingQuestion(
                    key = "Distraction pattern ${aiIndex + 1}",
                    prompt = if (aiIndex == 0) {
                        "When do you lose control of your phone the most?"
                    } else {
                        "What excuse do you repeat before wasting time?"
                    },
                    type = OnboardingQuestionType.TEXT,
                    inputLabel = "Be honest",
                    singleLine = false
                )
                aiQuestions.add(fallback)
                if (!answers.containsKey(fallback.key)) {
                    answers[fallback.key] = ""
                }
            } finally {
                isLoadingQuestion = false
            }
        }
    }

    AnimatedScreen {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val progress = (currentStep + 1) / (2f + totalQuestionSteps)
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = progress,
                label = "ProgressAnim"
            )
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(CardSurface, RoundedCornerShape(3.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(GradientRedStart, GradientRedEnd)), RoundedCornerShape(3.dp))
                )
            }

            AnimatedContent(targetState = currentStep, label = "OnboardingSteps") { step ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when {
                        step == 0 -> {
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

                        step >= 2 && step <= finalQuestionStep -> {
                            val currentQ = currentQuestion(step)
                            if (isLoadingQuestion || currentQ == null) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = BrutalRed, strokeWidth = 4.dp, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "AI is analyzing your profile...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BrutalRedLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                val questionIndex = step - 2
                                SectionHeader(
                                    title = "Question ${questionIndex + 1}/$totalQuestionSteps",
                                    icon = Icons.Default.Psychology
                                )
                                Text(
                                    currentQ.prompt,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                BrutalCard(showAccent = false) {
                                    when (currentQ.type) {
                                        OnboardingQuestionType.DROPDOWN -> BrutalDropdownField(
                                            value = answers[currentQ.key].orEmpty(),
                                            onValueChange = { answers[currentQ.key] = it },
                                            label = currentQ.inputLabel,
                                            options = currentQ.options
                                        )

                                        OnboardingQuestionType.TEXT -> BrutalTextField(
                                            value = answers[currentQ.key].orEmpty(),
                                            onValueChange = { answers[currentQ.key] = it },
                                            label = currentQ.inputLabel,
                                            singleLine = currentQ.singleLine
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                BrutalButton(
                                    text = if (step == finalQuestionStep) "FINALIZE PROFILE" else "NEXT →",
                                    onClick = { proceedToNext() },
                                    enabled = !answers[currentQ.key].isNullOrBlank()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
