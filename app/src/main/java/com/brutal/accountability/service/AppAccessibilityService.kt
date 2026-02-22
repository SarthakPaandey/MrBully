package com.brutal.accountability.service

import android.accessibilityservice.AccessibilityService
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.media.MediaPlayer
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.brutal.accountability.BrutalApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AppAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lastTriggerByPackage = mutableMapOf<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return

        val app = application as BrutalApp
        val repository = app.repository
        val now = SystemClock.elapsedRealtime()
        val lastTriggerAt = lastTriggerByPackage[pkg] ?: 0L
        if (now - lastTriggerAt < 2500L) return

        scope.launch {
            val restricted = repository.getRestrictedPackageNames()
            if (!restricted.contains(pkg)) return@launch

            val triggerEngine = TriggerEngine(this@AppAccessibilityService, repository)
            val withHeadphones = triggerEngine.hasHeadphones()
            repository.logEvent(pkg, withHeadphones)

            val strict = triggerEngine.strictModeEnabled()
            if (!strict) return@launch

            lastTriggerByPackage[pkg] = SystemClock.elapsedRealtime()
            
            val label = pkg.substringAfterLast('.')
            val message = repository.generateInterventionLine(label)

            val notificationManager = NotificationManagerCompat.from(this@AppAccessibilityService)

            // Generate and play TTS in background without delaying the notification
            scope.launch {
                try {
                    val audioBytes = repository.generateSpeech(message)
                    if (audioBytes != null) {
                        val file = File(cacheDir, "intervention.mp3")
                        FileOutputStream(file).use { it.write(audioBytes) }
                        
                        val player = MediaPlayer()
                        player.setDataSource(file.absolutePath)
                        player.prepare()
                        player.start()
                        
                        player.setOnCompletionListener {
                            it.release()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "intervention_channel",
                    "Brutal Interventions",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this@AppAccessibilityService, "intervention_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("BRUTAL INTERVENTION")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            if (canPostNotifications(notificationManager)) {
                val id = (SystemClock.uptimeMillis() and 0x0FFFFFFF).toInt()
                notificationManager.notify(id, notification)
            }

            repository.insertEpisodic(
                "App Block Escape",
                "Escaped from $pkg intervention."
            )
        }
    }

    override fun onInterrupt() = Unit

    private fun canPostNotifications(notificationManager: NotificationManagerCompat): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
