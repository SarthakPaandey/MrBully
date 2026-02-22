package com.brutal.accountability

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.brutal.accountability.data.AccountabilityDatabase
import com.brutal.accountability.data.AppPrefs
import com.brutal.accountability.data.LocalRepository
import com.brutal.accountability.service.DailyCheckInWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BrutalApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: LocalRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AccountabilityDatabase.get(this)
        val prefs = AppPrefs(this)
        repository = LocalRepository(
            database = database,
            prefs = prefs,
            applicationContext = this
        )
        appScope.launch {
            val existing = repository.apiKeyFlow.first().trim()
            val buildKey = BuildConfig.GROQ_API_KEY.trim()
            if (buildKey.startsWith("gsk_") && existing != buildKey) {
                repository.setApiKey(buildKey)
            }
        }
        createChannel()
        scheduleDailyCheckIn()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                DailyCheckInWorker.CHANNEL_ID,
                "Accountability",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun scheduleDailyCheckIn() {
        val request = PeriodicWorkRequestBuilder<DailyCheckInWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyCheckInWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
