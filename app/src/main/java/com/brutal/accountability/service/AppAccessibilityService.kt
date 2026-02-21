package com.brutal.accountability.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.brutal.accountability.BrutalApp
import com.brutal.accountability.InterventionActivity
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
            val intent = Intent(this@AppAccessibilityService, InterventionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, pkg)
                .putExtra(InterventionActivity.EXTRA_WITH_HEADPHONES, withHeadphones)
            startActivity(intent)
        }
    }

    override fun onInterrupt() = Unit
}
