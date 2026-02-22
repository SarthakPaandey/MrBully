package com.brutal.accountability.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    AnimatedScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            BrutalCard(
                modifier = Modifier.weight(1f),
                showAccent = false
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(messages) { message ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (message.isUser) BrutalRedSubtle else CardSurface)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (message.isUser) TextPrimary else TextSecondary,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                                Text("Thinking...", color = TextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                BrutalTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "Type your message",
                    singleLine = false
                )
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
