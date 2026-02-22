package com.brutal.accountability.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brutal.accountability.ui.components.AnimatedScreen
import com.brutal.accountability.ui.components.BrutalButton
import com.brutal.accountability.ui.components.BrutalCard
import com.brutal.accountability.ui.components.BrutalTextField
import com.brutal.accountability.ui.theme.BrutalRedSubtle
import com.brutal.accountability.ui.theme.CardSurface
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary
import android.util.Log
import kotlinx.coroutines.launch

private data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatScreen(
    onAskAiPartner: suspend (String) -> String
) {
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(ChatMessage("Send a message and I’ll respond with a strict action plan.", false))
    }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AnimatedScreen {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Talk to your AI accountability partner.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }

            item {
                BrutalCard {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(messages) { message ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (message.isUser) BrutalRedSubtle else CardSurface,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (message.isUser) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    BrutalTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = "Type your message"
                    )
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    }
                    BrutalButton(
                        text = if (isLoading) "SENDING..." else "SEND",
                        enabled = input.isNotBlank() && !isLoading,
                        onClick = {
                            val prompt = input.trim()
                            if (prompt.isBlank()) return@BrutalButton
                            input = ""
                            messages.add(ChatMessage(prompt, true))
                            scope.launch {
                                isLoading = true
                                try {
                                    val reply = onAskAiPartner(prompt)
                                    messages.add(ChatMessage(reply, false))
                                } catch (e: Exception) {
                                    Log.e("ChatScreen", "AI partner request failed", e)
                                    messages.add(ChatMessage("Couldn’t fetch reply right now. Try again in a moment.", false))
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
