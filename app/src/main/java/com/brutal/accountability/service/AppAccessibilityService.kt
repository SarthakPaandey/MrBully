package com.brutal.accountability.service

import android.accessibilityservice.AccessibilityService
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.brutal.accountability.BrutalApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastTriggerAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return

        val app = application as BrutalApp
        val repository = app.repository
        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerAt < 2500L) return

        scope.launch {
            val restricted = repository.getRestrictedPackageNames()
            if (!restricted.contains(pkg)) return@launch

            val triggerEngine = TriggerEngine(this@AppAccessibilityService, repository)
            val withHeadphones = triggerEngine.hasHeadphones()
            repository.logEvent(pkg, withHeadphones)

            val strict = triggerEngine.strictModeEnabled()
            if (!strict) return@launch

            lastTriggerAt = SystemClock.elapsedRealtime()
            
            val label = pkg.substringAfterLast('.')
            val message = repository.generateInterventionLine(label)

            val notificationManager = NotificationManagerCompat.from(this@AppAccessibilityService)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "intervention_channel",
                    "Brutal Interventions",
                    android.app.NotificationManager.IMPORTANCE_HIGH
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

            if (ContextCompat.checkSelfPermission(
                    this@AppAccessibilityService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(pkg.hashCode(), notification)
            }

            repository.insertEpisodic(
                "App Block Escape",
                "Escaped from $pkg intervention."
            )
        }
    }

    override fun onInterrupt() = Unit
}
