package com.brutal.accountability.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brutal.accountability.MainActivity
import com.brutal.accountability.R
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyCheckInWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            7,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val context = applicationContext
        val repository = (context as? com.brutal.accountability.BrutalApp)?.repository
        
        var aiMessage = applicationContext.getString(R.string.daily_checkin_body)
        
        repository?.let { repo ->
            val profile = repo.profileFlow.firstOrNull()
            val apiKey = repo.apiKeyFlow.firstOrNull().orEmpty().trim()

            if (profile != null && apiKey.startsWith("gsk_")) {
                try {
                    val leverage = repo.parseLeverageJson(profile.leverageJson)
                    val relationship = leverage.firstOrNull {
                        it.first.equals("Relationship status", ignoreCase = true)
                    }?.second.orEmpty()
                    val avoiding = leverage.firstOrNull {
                        it.first.equals("What are you avoiding?", ignoreCase = true)
                    }?.second.orEmpty()
                    val gymStatus = leverage.firstOrNull {
                        it.first.equals("Gym status", ignoreCase = true)
                    }?.second.orEmpty()
                    val now = SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date())
                    val attemptToken = System.currentTimeMillis().toString()

                    val prompt = """
                        You are a strict MALE accountability voice in the user's phone.
                        Write a 1-sentence push notification reminder for the user's daily check-in.
                        Personalize with their profile details and current context.
                        Make it harsh and guilt-inducing. Keep it fresh and different from generic lines. No quotes, no intro.
                    """.trimIndent()

                    val context = buildString {
                        appendLine("Nickname: ${profile.nickname}")
                        appendLine("Goal: ${profile.goal}")
                        if (profile.profession.isNotBlank()) appendLine("Current stage: ${profile.profession}")
                        if (profile.insecurity.isNotBlank()) appendLine("Insecurity: ${profile.insecurity}")
                        if (profile.fear.isNotBlank()) appendLine("Fear: ${profile.fear}")
                        if (relationship.isNotBlank()) appendLine("Relationship status: $relationship")
                        if (avoiding.isNotBlank()) appendLine("Avoiding: $avoiding")
                        if (gymStatus.isNotBlank()) appendLine("Gym status: $gymStatus")
                        appendLine("Current time: $now")
                        appendLine("Variation token: $attemptToken")
                    }
                    aiMessage = com.brutal.accountability.data.GroqClient()
                        .generateLine(apiKey, prompt, context)
                        .ifBlank { aiMessage }
                } catch (e: Exception) {
                    // Fallback to default if network or API fails
                }
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(applicationContext.getString(R.string.daily_checkin_title))
            .setContentText(aiMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aiMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "daily_checkin"
        const val WORK_NAME = "daily_checkin_work"
    }
}
