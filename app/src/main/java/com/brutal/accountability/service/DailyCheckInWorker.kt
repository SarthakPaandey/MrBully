package com.brutal.accountability.service

import android.Manifest
import android.app.PendingIntent
import android.app.NotificationManager
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
            val apiKey = repo.apiKeyFlow.firstOrNull()
            
            if (profile != null && !apiKey.isNullOrBlank()) {
                try {
                    val prompt = """
                        You are a strict MALE accountability voice in the user's phone. Write a 1-sentence push notification to the user (${profile.nickname}) 
                        reminding them to do their daily checkin. Mention their goal (${profile.goal}) and their insecurity (${profile.insecurity}).
                        Make it harsh and guilt-inducing. Keep it fresh and different from generic lines. No quotes, no intro.
                    """.trimIndent()
                    aiMessage = com.brutal.accountability.data.GroqClient().generateLine(apiKey, prompt, "Remind me.")
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
