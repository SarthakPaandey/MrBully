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

    private fun pickFreshLine(candidates: List<String>): String {
        val previous = lastInterventionLine
        val filtered = if (previous.isNullOrBlank()) {
            candidates
        } else {
            candidates.filterNot { it.equals(previous, ignoreCase = true) }
        }
        val pool = if (filtered.isEmpty()) candidates else filtered
        return pool.random()
    }

    private fun personalizedFallbackLines(
        nickname: String,
        currentAppLabel: String,
        goal: String,
        insecurity: String,
        fear: String
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

        return listOf(
            "$nickname, $currentAppLabel phir se? $goal khud se complete nahi hoga.$fearTail",
            "$nickname, abhi $currentAppLabel band kar. Discipline ke bina $goal sirf fantasy hai.$insecurityTail",
            "$nickname, tu live mode me apna future trade kar raha hai for $currentAppLabel. Back to $goal."
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
        val nickname = profile?.nickname ?: "You"
        val goal = profile?.goal ?: "your goal"
        val insecurity = profile?.insecurity.orEmpty()
        val fear = profile?.fear.orEmpty()
        val personalizedFallbacks = personalizedFallbackLines(
            nickname = nickname,
            currentAppLabel = currentAppLabel,
            goal = goal,
            insecurity = insecurity,
            fear = fear
        )

        val key = apiKeyFlow.first().trim()
        if (key.isBlank()) {
            val noKeyLine = pickFreshLine(personalizedFallbacks)
            lastInterventionLine = noKeyLine
            return noKeyLine
        }
        if (!key.startsWith("gsk_")) {
            return "$nickname, this does not look like a Groq key. Save a valid gsk_ key."
        }

        return try {
            // --- RAG: gather all personal context ---
            val leverage = profile?.leverageJson?.let { parseLeverageJson(it) } ?: emptyList()

            // Recent semantic notes sorted by emotional weight (most painful first)
            val notes = database.semanticNotesDao().observeRecent().first()
                .sortedByDescending { it.weight }
                .take(5)
                .joinToString("\n") { "- ${it.content} (weight ${it.weight}/10)" }

            // Last daily check-in
            val today = LocalDate.now().toString()
            val checkin = database.dailyCheckinDao().getByDate(today)
            val checkinContext = if (checkin != null) {
                "Today's plan: ${checkin.morningPlan}"
            } else ""

            // Recent violations (last 5 blocked app events)
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val recentViolations = database.eventLogDao().observeRecent().first()
                .take(5)
                .joinToString(", ") { "${it.packageName.substringAfterLast('.')} at ${fmt.format(Date(it.atMillis))}" }

            // Leverage (what's at stake)
            val leverageText = leverage
                .take(3)
                .joinToString("; ") { (k, v) -> "$k: $v" }
                .let { if (it.isNotBlank()) "Stakes: $it" else "" }

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

            // --- RAG user context ---
            val userContext = buildString {
                val attemptToken = System.currentTimeMillis().toString()
                appendLine("Nickname: $nickname")
                appendLine("Current app opened: $currentAppLabel")
                val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                appendLine("Time: $currentTime")
                appendLine("Attempt token: $attemptToken")
                appendLine("Today's goal they are breaking: $goal")
                
                if (checkinContext.isNotBlank()) appendLine("Yesterday's waste / Today's plan: $checkinContext")
                if (insecurity.isNotBlank()) appendLine("Biggest insecurity: $insecurity")
                if (fear.isNotBlank()) appendLine("Fear: $fear")
                
                // Map the leverage questions as close as possible to Boss, Salary, Ex
                val disappointed = leverage.find { it.first == "Who would be disappointed?" }?.second
                if (!disappointed.isNullOrBlank()) appendLine("Who would be disappointed: $disappointed")
                
                val avoiding = leverage.find { it.first == "What are you avoiding?" }?.second
                if (!avoiding.isNullOrBlank()) appendLine("What they are avoiding: $avoiding")
                
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

            val finalLine = if (line.isBlank() || line.equals(lastInterventionLine, ignoreCase = true)) {
                pickFreshLine(personalizedFallbacks)
            } else {
                line
            }
            lastInterventionLine = finalLine
            finalLine
        } catch (e: Exception) {
            Log.e("LocalRepository", "Groq intervention generation failed", e)
            val finalFallback = pickFreshLine(personalizedFallbacks)
            lastInterventionLine = finalFallback
            finalFallback
        }
    }

    suspend fun generateSpeech(text: String): ByteArray? {
        val key = apiKeyFlow.first().trim()
        if (key.isBlank() || !key.startsWith("gsk_")) return null

        return try {
            GroqClient().generateSpeech(apiKey = key, input = text)
        } catch (e: Exception) {
            Log.e("LocalRepository", "Groq TTS generation failed", e)
            null
        }
    }

    suspend fun generateAiPartnerReply(userMessage: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
        val cleanMessage = userMessage.trim()
        if (cleanMessage.isBlank()) return@withContext "Type something first."

        val profile = database.profileDao().getProfile()
        val nickname = profile?.nickname ?: "friend"
        val goal = profile?.goal ?: "your goal"
        val insecurity = profile?.insecurity ?: ""

        val recentViolations = database.eventLogDao().observeRecent().first().take(5)
        val violationContext = if (recentViolations.isEmpty()) {
            "No recent violations recorded."
        } else {
            recentViolations.joinToString(", ") { it.packageName.substringAfterLast('.') }
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
                Never suggest self-harm or abuse.
            """.trimIndent()

            val context = """
                User nickname: $nickname
                Main goal: $goal
                Insecurity: $insecurity
                Recent restricted-app violations: $violationContext
                User message: $cleanMessage
            """.trimIndent()

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
            val question = when {
                !contextMap["Goal"].isNullOrBlank() && contextMap["What are you avoiding?"].isNullOrBlank() -> "What are you avoiding right now?"
                !contextMap["Nickname"].isNullOrBlank() && contextMap["Who would be disappointed?"].isNullOrBlank() -> "Who feels your broken promises first?"
                else -> "What is your deepest flaw?"
            }
            return JSONObject()
                .put("question", question)
                .put("type", "text")
                .put("options", JSONArray())
                .toString()
        }

        if (key.isBlank() || !key.startsWith("gsk_")) return@withContext localFallbackQuestion()
        
        try {
            val systemPrompt = """
                You are a toxic accountability AI profiling a new user to find their deepest insecurities, fears, and leverage points.
                Based on the context so far, generate ONE highly probing question to ask them next.
                Output ONLY a valid JSON object with this exact structure:
                {
                  "question": "The question text, max 10 words",
                  "type": "text",
                  "options": []
                }
                If you want to give them choices, set "type": "dropdown" and provide 2-4 strings in "options".
                Make it invasive. Dig deeper into their failures.
            """.trimIndent()

            val contextStr = contextMap.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            
            val rawJson = GroqClient().generateLine(key, systemPrompt, "Context so far:\n$contextStr")
            val cleanJson = rawJson.substringAfter("{").substringBeforeLast("}")
            "{$cleanJson}"
        } catch (_: Exception) {
            localFallbackQuestion()
        }
    }
}
