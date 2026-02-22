package com.brutal.accountability.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GroqClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json".toMediaType()

    companion object {
        const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        private val CHAT_MODELS = listOf(
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "llama-3.3-70b-versatile"
        )
        const val TTS_URL = "https://api.groq.com/openai/v1/audio/speech"
        const val TTS_MODEL = "canopylabs/orpheus-v1-english"
        const val TTS_VOICE_PRIMARY = "echo"
        const val TTS_VOICE_SECONDARY = "onyx"
    }

    fun generateLine(apiKey: String, systemPrompt: String, userContext: String): String {
        var lastError: String? = null
        for (model in CHAT_MODELS) {
            try {
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userContext))

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("max_tokens", 120)
                    .put("temperature", 0.9)
                    .toString()
                    .toRequestBody(jsonType)

                val request = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string().orEmpty().take(240)
                        lastError = "model=$model code=${response.code} msg=${response.message} body=$errorBody"
                        return@use
                    }

                    val payload = response.body?.string().orEmpty()
                    val content = JSONObject(payload)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()
                        .trim()
                        .ifBlank { "Stop wasting time. You know what you're supposed to be doing." }
                    return content
                }
            } catch (e: Exception) {
                lastError = "model=$model exception=${e.message}"
            }
        }

        throw IllegalStateException("Groq chat call failed for all models. Last error: $lastError")
    }

    fun generateSpeech(apiKey: String, input: String): ByteArray {
        return runCatching { requestSpeech(apiKey = apiKey, input = input, voice = TTS_VOICE_PRIMARY) }
            .recoverCatching { requestSpeech(apiKey = apiKey, input = input, voice = TTS_VOICE_SECONDARY) }
            .getOrThrow()
        }

        private fun requestSpeech(apiKey: String, input: String, voice: String): ByteArray {
        val body = JSONObject()
            .put("model", TTS_MODEL)
            .put("input", input)
            .put("voice", voice)
            .toString()
            .toRequestBody(jsonType)

        val request = Request.Builder()
            .url(TTS_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty().take(240)
                throw IllegalStateException("Groq call failed: ${response.code} ${response.message} | $errorBody")
            }
            return response.body?.bytes() ?: throw IllegalStateException("Empty response body")
        }
    }
}
