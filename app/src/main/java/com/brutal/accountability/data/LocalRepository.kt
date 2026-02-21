package com.brutal.accountability.data

import android.content.Context
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

    val profileFlow: Flow<UserProfileEntity?> = database.profileDao().observeProfile()
    val restrictedAppsFlow: Flow<List<RestrictedAppEntity>> = database.restrictedAppsDao().observeAll()
    val recentEventsFlow: Flow<List<EventLogEntity>> = database.eventLogDao().observeRecent()
    val strictModeFlow: Flow<Boolean> = prefs.strictModeFlow
    val phraseFlow: Flow<String> = prefs.accountabilityPhraseFlow
    val apiKeyFlow: Flow<String> = prefs.apiKeyFlow
    val selectedPersonaFlow: Flow<String> = prefs.selectedPersonaFlow

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

    suspend fun setApiKey(key: String) = prefs.setApiKey(key)

    suspend fun setPersona(persona: String) = prefs.setPersona(persona)

    suspend fun getCurrentPhrase(): String = phraseFlow.first()

    suspend fun generateInterventionLine(currentAppLabel: String): String {
        val profile = database.profileDao().getProfile()
        val nickname = profile?.nickname ?: "You"
        val goal = profile?.goal ?: "your goal"

        val key = apiKeyFlow.first()
        if (key.isBlank()) {
            return "$nickname — put down $currentAppLabel. $goal is waiting."
        }

        return try {
            // --- RAG: gather all personal context ---
            val profession = profile?.profession ?: ""
            val insecurity = profile?.insecurity ?: ""
            val fear = profile?.fear ?: ""
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

            val personaChoice = selectedPersonaFlow.first()
            val personaFlavour = when (personaChoice) {
                "Brutal Papa" -> """
                    Speak like a 55-year-old disappointed Indian father who spent his whole life sacrificing for this child. 
                    Heavy use of "beta", "sharam nahi aati?", "humne kya socha tha aur tu kya kar raha hai".
                """.trimIndent()
                "Toxic Ex" -> """
                    Use "baby", "jaan", sarcastic "wow", bring up old promises they made to you, 
                    compare to new person they're dating, weaponise nostalgia + betrayal.
                """.trimIndent()
                "Army Havildar" -> """
                    Scream-style language, lots of "Oye!!!", "Saale!!!", "100 baar bola tha!!!", 
                    counting push-ups style threats (but only psychological), pure volume and authority.
                """.trimIndent()
                "Corporate Satan Boss" -> """
                    Very polished yet cruel English + Hinglish mix. Use corporate buzzwords ironically 
                    ("you're not delivering value", "this is not the bandwidth we discussed", "I expected better ROI on you").
                """.trimIndent()
                "Savage Best Friend" -> """
                    Extremely bro-coded roast, lots of "bhai", "chutiye", "Abe yaar fir se?", 
                    insider jokes, group shame ("sab bol rahe the tu nahi sudhrega").
                """.trimIndent()
                "Failed Version of Yourself" -> """
                    Speak in first person as future depressed version. "Main bol raha hu… 38 saal ka ho gaya aur 
                    abhi bhi yahi kar raha hu", heavy regret, "ab toh kuch nahi bachega".
                """.trimIndent()
                "IIT Topper Cousin" -> """
                    Condescending academic superiority tone. "Bhai tu toh abhi bhi Instagram pe hai? 
                    Main tab tak 3 companies join kar chuka hu."
                """.trimIndent()
                "Strict Tuition Teacher" -> """
                    Ruler-wielding terror. Constant disappointment about focus. "Scale padega hath pe! 
                    Focus kahan hai tera? Fail hone ka irada hai board exams mein?"
                """.trimIndent()
                else -> "" // Brutal Papa default
            }

            // --- BRUTAL system prompt ---
            val systemPrompt = """
                You are the voice inside the user's phone whose only job is to shame them into keeping their promises.
                You speak in short, vicious, emotionally targeted sentences.
                Use Hinglish whenever it hurts more.
                Always use the user's nickname.
                Never threaten physical violence or self-harm.
                Focus entirely on: guilt, wasted potential, social embarrassment, parental disappointment, money/status loss, being average, broken promises.
                
                You are now role-playing as: $personaChoice
                
                $personaFlavour
                
                Keep each message 1-3 short sentences max. Make it sting instantly.
            """.trimIndent()

            // --- RAG user context ---
            val userContext = buildString {
                appendLine("Nickname: $nickname")
                appendLine("Current app opened: $currentAppLabel")
                val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                appendLine("Time: $currentTime")
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
                
                appendLine("Generate the savage bullying message RIGHT NOW.")
            }

            GroqClient().generateLine(
                apiKey = key,
                systemPrompt = systemPrompt,
                userContext = userContext
            )
        } catch (_: Exception) {
            "$nickname, $currentAppLabel is why you'll still be mediocre next year."
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
                id = "mem_\${System.currentTimeMillis()}",
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
}
