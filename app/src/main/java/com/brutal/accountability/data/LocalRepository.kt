package com.brutal.accountability.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class LocalRepository(
    private val database: AccountabilityDatabase,
    private val prefs: AppPrefs,
    private val applicationContext: Context
) {
    private var lastInterventionLine: String? = null
    private val recentInterventionLines = ArrayDeque<String>()

    private fun rememberInterventionLine(line: String) {
        lastInterventionLine = line
        recentInterventionLines.removeAll { it.equals(line, ignoreCase = true) }
        recentInterventionLines.addLast(line)
        while (recentInterventionLines.size > 5) {
            recentInterventionLines.removeFirst()
        }
    }

    private fun pickFreshLine(candidates: List<String>): String {
        val recent = recentInterventionLines.map { it.lowercase(Locale.getDefault()) }.toSet()
        val filtered = candidates.filterNot { it.lowercase(Locale.getDefault()) in recent }
        val pool = if (filtered.isEmpty()) candidates else filtered
        return pool.random()
    }

    private fun leverageValue(leverage: List<Pair<String, String>>, vararg keys: String): String {
        for (key in keys) {
            val value = leverage.firstOrNull { it.first.equals(key, ignoreCase = true) }
                ?.second
                .orEmpty()
                .trim()
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun personalizedFallbackLines(
        nickname: String,
        currentAppLabel: String,
        goal: String,
        insecurity: String,
        fear: String,
        profession: String,
        relationshipStatus: String,
        gymStatus: String
    ): List<String> {
        val insecurityTail = if (insecurity.isBlank()) {
            ""
        } else {
            " Aur haan, $insecurity abhi bhi fix nahi hua."
        }
        val fearTail = if (fear.isBlank()) {
            ""
        } else {
            " Yehi pace raha toh $fear sach ho jayega."
        }
        val professionTail = if (profession.isBlank()) {
            ""
        } else {
            " $profession hoke bhi discipline zero."
        }
        val relationshipTail = if (relationshipStatus.isBlank()) {
            ""
        } else {
            " $relationshipStatus ho ya single, excuses sabko cheap lagte hain."
        }
        val gymTail = if (gymStatus.isBlank()) {
            ""
        } else {
            " Gym status '$gymStatus' bolne se body aur confidence nahi banega."
        }

        return listOf(
            "$nickname, $currentAppLabel phir se? $goal khud se complete nahi hoga.$fearTail",
            "$nickname, abhi $currentAppLabel band kar. Discipline ke bina $goal sirf fantasy hai.$insecurityTail",
            "$nickname, tu live mode me apna future trade kar raha hai for $currentAppLabel. Back to $goal.",
            "$nickname, har swipe tera future salary aur respect ka cut hai.$professionTail",
            "$nickname, focus tod ke tu apni image khud destroy kar raha hai.$relationshipTail",
            "$nickname, $goal ka sapna bolta hai aur action me zero deta hai.$gymTail"
        )
    }

    val profileFlow: Flow<UserProfileEntity?> = database.profileDao().observeProfile()
    val restrictedAppsFlow: Flow<List<RestrictedAppEntity>> = database.restrictedAppsDao().observeAll()
    val recentEventsFlow: Flow<List<EventLogEntity>> = database.eventLogDao().observeRecent()
    val strictModeFlow: Flow<Boolean> = prefs.strictModeFlow
    val phraseFlow: Flow<String> = prefs.accountabilityPhraseFlow
    val apiKeyFlow: Flow<String> = prefs.apiKeyFlow

    suspend fun upsertProfile(
        nickname: String,
        profession: String,
        goal: String,
        insecurity: String,
        fear: String,
        leverage: Map<String, String>
    ) {
        val json = JSONObject(leverage).toString()
        database.profileDao().upsert(
            UserProfileEntity(
                nickname = nickname,
                profession = profession,
                goal = goal,
                insecurity = insecurity,
                fear = fear,
                leverageJson = json
            )
        )
    }

    suspend fun setRestrictedApps(apps: List<RestrictedAppEntity>) {
        database.restrictedAppsDao().clear()
        database.restrictedAppsDao().insertAll(apps)
    }

    suspend fun getRestrictedPackageNames(): Set<String> {
        return database.restrictedAppsDao().getPackageNames().toSet()
    }

    suspend fun logEvent(packageName: String, withHeadphones: Boolean) {
        database.eventLogDao().insert(
            EventLogEntity(
                packageName = packageName,
                atMillis = System.currentTimeMillis(),
                withHeadphones = withHeadphones
            )
        )
    }

    suspend fun saveDailyCheckIn(morningPlan: String, nightReflection: String) {
        val date = LocalDate.now().toString()
        database.dailyCheckinDao().upsert(
            DailyCheckinEntity(
                dateIso = date,
                morningPlan = morningPlan,
                nightReflection = nightReflection
            )
        )
    }

    suspend fun addSemanticNote(note: String) {
        processSavageMemory(note)
    }

    suspend fun insertEpisodic(eventName: String, description: String) {
        database.episodicMemoryDao().insert(
            com.brutal.accountability.data.EpisodicMemoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                type = "app_block_escape",
                durationMinutes = 0,
                restrictedAppsJson = "[\"$eventName\"]",
                totalOpens = 1,
                headline = eventName,
                userReaction = description,
                occurredAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setStrictMode(enabled: Boolean) = prefs.setStrictMode(enabled)

    suspend fun setPhrase(phrase: String) = prefs.setPhrase(phrase)

    suspend fun setApiKey(key: String) = prefs.setApiKey(key.trim())

    suspend fun getCurrentPhrase(): String = phraseFlow.first()

    suspend fun generateInterventionLine(currentAppLabel: String): String {
        val profile = runCatching { database.profileDao().getProfile() }.getOrNull()
        val leverage = profile?.leverageJson?.let { parseLeverageJson(it) } ?: emptyList()
        val nickname = profile?.nickname ?: "You"
        val goal = profile?.goal ?: "your goal"
        val insecurity = profile?.insecurity.orEmpty()
        val fear = profile?.fear.orEmpty()
        val profession = profile?.profession.orEmpty()
        val relationshipStatus = leverageValue(leverage, "Relationship status")
        val gymStatus = leverageValue(leverage, "Gym status")
        val personalizedFallbacks = personalizedFallbackLines(
            nickname = nickname,
            currentAppLabel = currentAppLabel,
            goal = goal,
            insecurity = insecurity,
            fear = fear,
            profession = profession,
            relationshipStatus = relationshipStatus,
            gymStatus = gymStatus
        )

        val key = apiKeyFlow.first().trim()
        if (key.isBlank()) {
            val noKeyLine = pickFreshLine(personalizedFallbacks)
            rememberInterventionLine(noKeyLine)
            return noKeyLine
        }
        if (!key.startsWith("gsk_")) {
            return "$nickname, this does not look like a Groq key. Save a valid gsk_ key."
        }

        return try {
            val notes = database.semanticNotesDao().observeRecent().first()
                .sortedByDescending { it.weight }
                .take(5)
                .joinToString("\n") { "- ${it.content} (weight ${it.weight}/10)" }

            val today = LocalDate.now().toString()
            val checkin = database.dailyCheckinDao().getByDate(today)
            val checkinContext = if (checkin != null) {
                "Today's plan: ${checkin.morningPlan}"
            } else ""

            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val recentViolations = database.eventLogDao().observeRecent().first()
                .take(5)
                .joinToString(", ") { "${it.packageName.substringAfterLast('.')} at ${fmt.format(Date(it.atMillis))}" }

            val leverageText = leverage
                .take(5)
                .joinToString("; ") { (k, v) -> "$k: $v" }
                .let { if (it.isNotBlank()) "Stakes: $it" else "" }
            val disappointed = leverageValue(leverage, "Who would be disappointed?")
            val avoiding = leverageValue(leverage, "What are you avoiding?")
            val exImpact = leverageValue(leverage, "Is an ex still affecting focus?")
            val routine = leverageValue(leverage, "Current routine")
            val environment = leverageValue(leverage, "College or office environment")

            val systemPrompt = """
                You are the voice inside the user's phone whose only job is to shame them into keeping their promises.
                Always speak as a MALE voice.
                You speak in short, vicious, emotionally targeted sentences.
                Use Hinglish whenever it hurts more.
                Always use the user's nickname.
                Never threaten physical violence or self-harm.
                Focus entirely on: guilt, wasted potential, social embarrassment, parental disappointment, money/status loss, being average, broken promises.

                Do not use any persona, character roleplay, or celebrity imitation.
                Every response must be a NEW roast and should not repeat phrasing from previous responses.
                Keep each message 1-3 short sentences max. Make it sting instantly.
            """.trimIndent()

            val userContext = buildString {
                val attemptToken = System.currentTimeMillis().toString()
                appendLine("Nickname: $nickname")
                appendLine("Current app opened: $currentAppLabel")
                val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                appendLine("Time: $currentTime")
                appendLine("Attempt token: $attemptToken")
                appendLine("Today's goal they are breaking: $goal")
                if (profession.isNotBlank()) appendLine("Profession / current life stage: $profession")
                if (checkinContext.isNotBlank()) appendLine("Yesterday's waste / Today's plan: $checkinContext")
                if (insecurity.isNotBlank()) appendLine("Biggest insecurity: $insecurity")
                if (fear.isNotBlank()) appendLine("Fear: $fear")
                if (relationshipStatus.isNotBlank()) appendLine("Relationship status: $relationshipStatus")
                if (exImpact.isNotBlank()) appendLine("Ex impact: $exImpact")
                if (gymStatus.isNotBlank()) appendLine("Gym status: $gymStatus")
                if (environment.isNotBlank()) appendLine("Main environment: $environment")
                if (disappointed.isNotBlank()) appendLine("Who would be disappointed: $disappointed")
                if (avoiding.isNotBlank()) appendLine("What they are avoiding: $avoiding")
                if (routine.isNotBlank()) appendLine("Current routine: $routine")
                if (leverageText.isNotBlank()) appendLine("Other stakes/leverage: $leverageText")
                if (notes.isNotBlank()) {
                    appendLine("Past failures/things to hold against them:")
                    appendLine(notes)
                }
                if (recentViolations.isNotBlank()) appendLine("Recent violations today: $recentViolations")
                if (!lastInterventionLine.isNullOrBlank()) {
                    appendLine("Previous roast used: ${lastInterventionLine}")
                    appendLine("Write a different roast than the previous one.")
                }
                
                appendLine("Generate the savage bullying message RIGHT NOW.")
            }

            val line = GroqClient().generateLine(
                apiKey = key,
                systemPrompt = systemPrompt,
                userContext = userContext
            ).trim()

            val finalLine = if (line.isBlank() || recentInterventionLines.any { it.equals(line, ignoreCase = true) }) {
                pickFreshLine(personalizedFallbacks)
            } else {
                line
            }
            rememberInterventionLine(finalLine)
            finalLine
        } catch (e: Exception) {
            Log.e("LocalRepository", "Groq intervention generation failed", e)
            val finalFallback = pickFreshLine(personalizedFallbacks)
            rememberInterventionLine(finalFallback)
            finalFallback
        }
    }

    suspend fun generateAiPartnerReply(userMessage: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val cleanMessage = userMessage.trim()
            if (cleanMessage.isBlank()) return@withContext "Type something first."

            val profile = database.profileDao().getProfile()
            val leverage = profile?.leverageJson?.let { parseLeverageJson(it) } ?: emptyList()
            val nickname = profile?.nickname ?: "friend"
            val goal = profile?.goal ?: "your goal"
            val insecurity = profile?.insecurity.orEmpty()
            val fear = profile?.fear.orEmpty()
            val profession = profile?.profession.orEmpty()
            val relationshipStatus = leverageValue(leverage, "Relationship status")
            val avoiding = leverageValue(leverage, "What are you avoiding?")
            val gymStatus = leverageValue(leverage, "Gym status")

            val recentViolations = database.eventLogDao().observeRecent().first().take(6)
            val violationContext = if (recentViolations.isEmpty()) {
                "No recent violations recorded."
            } else {
                recentViolations.joinToString(", ") {
                    "${it.packageName.substringAfterLast('.')} (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.atMillis))})"
                }
            }

            val key = apiKeyFlow.first().trim()
            if (key.isBlank() || !key.startsWith("gsk_")) {
                return@withContext "I’m here, $nickname. No API key set yet—save a valid Groq key to enable AI partner replies."
            }

            return@withContext try {
                val systemPrompt = """
                    You are a direct but supportive accountability AI partner.
                    Keep reply to 2-4 lines max.
                    Use practical steps, not fluff.
                    Use light Hinglish if natural.
                    Be strict, respectful, and action-oriented.
                    Personalize using profile facts when available.
                    Never suggest self-harm or abuse.
                """.trimIndent()

                val context = buildString {
                    appendLine("User nickname: $nickname")
                    appendLine("Main goal: $goal")
                    if (profession.isNotBlank()) appendLine("Current stage: $profession")
                    if (insecurity.isNotBlank()) appendLine("Insecurity: $insecurity")
                    if (fear.isNotBlank()) appendLine("Fear: $fear")
                    if (relationshipStatus.isNotBlank()) appendLine("Relationship status: $relationshipStatus")
                    if (gymStatus.isNotBlank()) appendLine("Gym status: $gymStatus")
                    if (avoiding.isNotBlank()) appendLine("Avoiding: $avoiding")
                    appendLine("Recent restricted-app violations: $violationContext")
                    appendLine("User message: $cleanMessage")
                }

                GroqClient().generateLine(key, systemPrompt, context)
                    .ifBlank { "$nickname, next step: put phone down for 10 minutes and start one task toward $goal." }
            } catch (e: Exception) {
                Log.e("LocalRepository", "AI partner generation failed", e)
                "$nickname, quick reset: 1) close distractions, 2) 10-minute timer, 3) start the first task toward $goal."
            }
        } catch (e: Exception) {
            Log.e("LocalRepository", "AI partner fatal failure", e)
            "Quick reset: close distractions, 10-minute timer, start first task now."
        }
    }

    fun parseLeverageJson(leverageJson: String): List<Pair<String, String>> {
        return try {
            val json = JSONObject(leverageJson)
            json.keys().asSequence().map { key ->
                key to json.optString(key)
            }.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeSimpleList(values: List<String>): String {
        return JSONArray(values).toString()
    }

    suspend fun generateHumiliationPhrase(currentAppLabel: String, fallbackPhrase: String): String {
        val profile = database.profileDao().getProfile()
        val nickname = profile?.nickname ?: "You"
        val insecurity = profile?.insecurity ?: ""
        val goal = profile?.goal ?: ""
        
        val defaultFallback = fallbackPhrase.ifBlank { "Main lazy hu aur mera koi dream nahi hai" }
        val key = apiKeyFlow.first()
        if (key.isBlank()) return defaultFallback

        return try {
            val systemPrompt = """
                Generate ONE extremely humiliating unlock phrase the user must type exactly.
                Rules:
                - Max 12 words
                - Must be self-deprecating and savage
                - Hindi preferred (or Hinglish) because it hurts more for Indian users
                - Use their insecurity, goal, nickname, or current failure subtly
                - Make them feel like a loser while typing it
                - Never promote self-harm
                Output ONLY the phrase, nothing else. No quotes, no prefix.
            """.trimIndent()

            val userContext = """
                Nickname: $nickname
                Insecurity: $insecurity
                Goal failing: $goal
                Current app: $currentAppLabel
            """.trimIndent()

            GroqClient().generateLine(key, systemPrompt, userContext)
        } catch (_: Exception) {
            defaultFallback
        }
    }

    private suspend fun processSavageMemory(rawNote: String) {
        val key = apiKeyFlow.first()
        if (key.isBlank()) return

        try {
            val systemPrompt = """
                User just said a raw statement about themselves.
                Convert this into a savage, high-emotional-weight memory that can be used to bully them later.
                Output ONLY a JSON object with this exact structure:
                {
                  "type": "insecurity", // or "failure", "trigger", "ego_wound"
                  "weight": 9, // 1-10 pain score
                  "content": "The brutal extraction of their pain point",
                  "tags": ["tag1", "tag2"]
                }
            """.trimIndent()

            val rawJson = GroqClient().generateLine(key, systemPrompt, "User statement: $rawNote")
            val cleanJson = rawJson.substringAfter("{").substringBeforeLast("}")
            val parsed = JSONObject("{$cleanJson}")

            val entity = SemanticNoteEntity(
                id = "mem_${System.currentTimeMillis()}",
                type = parsed.optString("type", "ego_wound"),
                weight = parsed.optInt("weight", 9),
                content = parsed.optString("content", rawNote),
                source = "remember_this",
                tagsJson = parsed.optJSONArray("tags")?.toString() ?: "[]"
            )
            database.semanticNotesDao().insert(entity)
        } catch (_: Exception) {
            // Silently fail if LLM processing fails
        }
    }

    suspend fun generateDynamicQuestion(contextMap: Map<String, String>, tempApiKey: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val key = tempApiKey.ifBlank { apiKeyFlow.first() }.trim()

        fun localFallbackQuestion(): String {
            data class FallbackQuestion(
                val key: String,
                val question: String,
                val options: List<String> = emptyList()
            )
            val orderedFallbacks = listOf(
                FallbackQuestion(
                    key = "Relationship status",
                    question = "What is your relationship situation?",
                    options = listOf("Single", "In a relationship", "Complicated", "Recently broke up")
                ),
                FallbackQuestion(
                    key = "Is an ex still affecting focus?",
                    question = "Is an ex still affecting your focus?",
                    options = listOf("Yes", "Sometimes", "No")
                ),
                FallbackQuestion(
                    key = "College or office environment",
                    question = "Where do distractions hit you harder?",
                    options = listOf("College", "Office", "Home", "Everywhere")
                ),
                FallbackQuestion(
                    key = "Gym status",
                    question = "What's your current gym/fitness status?",
                    options = listOf("Regular", "Inconsistent", "Want to start", "Not focused on fitness")
                ),
                FallbackQuestion(
                    key = "Who would be disappointed?",
                    question = "Who gets hurt first when you stay distracted?"
                ),
                FallbackQuestion(
                    key = "What are you avoiding?",
                    question = "What difficult task are you avoiding daily?"
                )
            )
            val nextQuestion = orderedFallbacks.firstOrNull { contextMap[it.key].isNullOrBlank() }
                ?: FallbackQuestion(
                    key = "Pain point",
                    question = "What excuse do you repeat before wasting time?"
                )
            val type = if (nextQuestion.options.isEmpty()) "text" else "dropdown"
            val options = JSONArray(nextQuestion.options)
            return JSONObject()
                .put("question", nextQuestion.question)
                .put("type", type)
                .put("options", options)
                .toString()
        }

        if (key.isBlank() || !key.startsWith("gsk_")) return@withContext localFallbackQuestion()

        try {
            val systemPrompt = """
                You are profiling a user for accountability and need one deeper follow-up question.
                Generate ONE question that is not already answered in the provided context map.
                Prioritize missing details around relationship pressure, ex impact, college/office pressure, gym discipline, fear, shame, and specific avoided tasks.
                Output ONLY valid JSON with this exact structure:
                {
                  "question": "Question text max 12 words",
                  "type": "text",
                  "options": []
                }
                If choices make sense, set "type" to "dropdown" and provide 3-5 concise strings in "options".
                Do not add markdown, quotes outside JSON, or any explanation.
            """.trimIndent()

            val contextStr = contextMap.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            val rawJson = GroqClient().generateLine(key, systemPrompt, "Context so far:\n$contextStr")
            val cleanJson = rawJson.substringAfter("{", "").substringBeforeLast("}", "")
            if (cleanJson.isBlank()) return@withContext localFallbackQuestion()
            "{$cleanJson}"
        } catch (_: Exception) {
            localFallbackQuestion()
        }
    }

    private fun shouldWarnForModelTerms(error: Throwable): Boolean {
        return error.message?.contains("model_terms_required", ignoreCase = true) == true
    }

    suspend fun generateSpeech(text: String): ByteArray? {
        val key = apiKeyFlow.first().trim()
        if (key.isBlank() || !key.startsWith("gsk_")) return null

        return try {
            GroqClient().generateSpeech(apiKey = key, input = text)
        } catch (e: Exception) {
            if (shouldWarnForModelTerms(e)) {
                Log.w(
                    "LocalRepository",
                    "Groq TTS model terms not accepted. Accept terms in Groq console for voice model."
                )
            }
            Log.e("LocalRepository", "Groq TTS generation failed", e)
            null
        }
    }
}
