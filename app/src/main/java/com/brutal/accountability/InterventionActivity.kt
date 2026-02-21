package com.brutal.accountability

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.brutal.accountability.ui.components.BrutalButton
import com.brutal.accountability.ui.components.BrutalTextField
import com.brutal.accountability.ui.theme.BrutalRed
import com.brutal.accountability.ui.theme.BrutalRedDark
import com.brutal.accountability.ui.theme.BrutalRedSubtle
import com.brutal.accountability.ui.theme.BrutalTheme
import com.brutal.accountability.ui.theme.CardSurface
import com.brutal.accountability.ui.theme.DeepBlack
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

class InterventionActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageNameBlocked = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val app = application as BrutalApp
        val repository = app.repository

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        setContent {
            BrutalTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(DeepBlack, BrutalRedDark.copy(alpha = 0.15f), DeepBlack)
                            )
                        )
                ) {
                    InterventionScreen(
                        blockedPackage = packageNameBlocked,
                        loadPhrase = {
                            val label = packageNameBlocked.substringAfterLast('.')
                            val staticPhrase = repository.getCurrentPhrase()
                            repository.generateHumiliationPhrase(label, staticPhrase)
                        },
                        loadMessage = {
                            val label = packageNameBlocked.substringAfterLast('.')
                            repository.generateInterventionLine(label)
                        },
                        onMessageReady = { msg -> speakMessage(msg) },
                        onUnlocked = {
                            this@InterventionActivity.lifecycleScope.launch {
                                repository.insertEpisodic(
                                    "App Block Escape",
                                    "Escaped from $packageNameBlocked intervention."
                                )
                                finishAndRemoveTask()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun speakMessage(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "brutal_intervention")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package"
        const val EXTRA_WITH_HEADPHONES = "extra_with_headphones"
    }
}

@Composable
private fun InterventionScreen(
    blockedPackage: String,
    loadPhrase: suspend () -> String,
    loadMessage: suspend () -> String,
    onMessageReady: (String) -> Unit,
    onUnlocked: () -> Unit
) {
    BackHandler(enabled = true) {}

    val phrase by produceState(initialValue = "…loading…", blockedPackage) {
        value = withContext(Dispatchers.IO) { loadPhrase() }
    }
    val message by produceState(initialValue = "…", blockedPackage) {
        val generated = withContext(Dispatchers.IO) { loadMessage() }
        value = generated
    }
    var input by remember { mutableStateOf("") }
    val isMatch = phrase.isNotBlank() && phrase != "…loading…" && input.trim() == phrase.trim()

    // Shake animation state
    var shakeTriggered by remember { mutableStateOf(false) }
    var shakeOffset by remember { mutableStateOf(0f) }

    // Trigger shake on wrong input attempt  
    LaunchedEffect(shakeTriggered) {
        if (shakeTriggered) {
            repeat(6) { i ->
                shakeOffset = if (i % 2 == 0) 12f else -12f
                delay(60)
            }
            shakeOffset = 0f
            shakeTriggered = false
        }
    }

    // Speak message when ready
    LaunchedEffect(message) {
        if (message != "…" && message.isNotBlank()) {
            onMessageReady(message)
        }
    }

    // Pulsing border animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Warning icon with pulse
        Icon(
            Icons.Default.Warning,
            contentDescription = "Warning",
            tint = BrutalRed.copy(alpha = pulseAlpha),
            modifier = Modifier.alpha(pulseAlpha)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "INTERVENTION",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = BrutalRed,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.roundToInt(), 0) },
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(
                width = 2.dp,
                color = BrutalRed.copy(alpha = pulseAlpha * 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    lineHeight = 24.sp
                )

                Text(
                    "Blocked: $blockedPackage",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                BrutalTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "Type exact phrase to escape"
                )

                BrutalButton(
                    text = if (isMatch) "🔓 UNLOCK" else "🔒 LOCKED",
                    onClick = {
                        if (isMatch) {
                            onUnlocked()
                        } else {
                            shakeTriggered = true
                        }
                    },
                    enabled = input.isNotBlank()
                )
            }
        }
    }
}
