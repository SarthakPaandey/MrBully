package com.brutal.accountability.service

import android.accessibilityservice.AccessibilityService
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
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
import java.util.Locale

class AppAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lastTriggerByPackage = mutableMapOf<String, Long>()
    private var lastNotificationFingerprint: String? = null
    private var lastNotificationAt: Long = 0L
    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTextToSpeechReady = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(this, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTextToSpeechReady = true
            textToSpeech?.language = Locale.getDefault()
        } else {
            isTextToSpeechReady = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return

        val app = application as BrutalApp
        val repository = app.repository
        val now = SystemClock.elapsedRealtime()
        val lastTriggerAt = lastTriggerByPackage[pkg] ?: 0L
        if (now - lastTriggerAt < 10000L) return

        scope.launch {
            val restricted = repository.getRestrictedPackageNames()
            if (!restricted.contains(pkg)) return@launch

            val triggerEngine = TriggerEngine(this@AppAccessibilityService, repository)
            val withHeadphones = triggerEngine.hasHeadphones()
            repository.logEvent(pkg, withHeadphones)

            val strict = triggerEngine.strictModeEnabled()
            if (!strict) return@launch

            lastTriggerByPackage[pkg] = SystemClock.elapsedRealtime()
            val label = runCatching {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            }.getOrElse { pkg.substringAfterLast('.') }
            val message = repository.generateInterventionLine(label)
            val fingerprint = "$pkg|${message.lowercase(Locale.getDefault())}"
            val sameRecentNotification = fingerprint == lastNotificationFingerprint &&
                (SystemClock.elapsedRealtime() - lastNotificationAt) < 45000L
            if (sameRecentNotification) return@launch

            val notificationManager = NotificationManagerCompat.from(this@AppAccessibilityService)

            scope.launch {
                try {
                    val audioBytes = repository.generateSpeech(message)
                    playInterventionAudio(message, audioBytes)
                } catch (e: Exception) {
                    android.util.Log.e("AppAccessibilityService", "Audio intervention failed", e)
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
                lastNotificationFingerprint = fingerprint
                lastNotificationAt = SystemClock.elapsedRealtime()
            }

            repository.insertEpisodic(
                "App Block Escape",
                "Escaped from $pkg intervention."
            )
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTextToSpeechReady = false
    }

    private fun canPostNotifications(notificationManager: NotificationManagerCompat): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun playInterventionAudio(message: String, audioBytes: ByteArray?) {
        val playedRemote = if (audioBytes != null) {
            runCatching { playRemoteAudio(audioBytes) }.isSuccess
        } else {
            false
        }
        if (!playedRemote) {
            speakWithDeviceTts(message)
        }
    }

    private fun playRemoteAudio(audioBytes: ByteArray) {
        val file = File(cacheDir, "intervention_${SystemClock.elapsedRealtime()}.mp3")
        FileOutputStream(file).use { it.write(audioBytes) }

        releaseMediaPlayer()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { player ->
                player.release()
                if (mediaPlayer === player) {
                    mediaPlayer = null
                }
                file.delete()
            }
            setOnErrorListener { player, _, _ ->
                player.release()
                if (mediaPlayer === player) {
                    mediaPlayer = null
                }
                file.delete()
                false
            }
            prepare()
            start()
        }
    }

    private fun speakWithDeviceTts(message: String) {
        if (!isTextToSpeechReady) return
        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "intervention_${SystemClock.elapsedRealtime()}"
        )
    }

    private fun releaseMediaPlayer() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }
}
