package com.brutal.accountability.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.brutal.accountability.ui.theme.BrutalRed
import com.brutal.accountability.ui.theme.BrutalRedDark
import com.brutal.accountability.ui.theme.BrutalRedSubtle
import com.brutal.accountability.ui.theme.CardSurface
import com.brutal.accountability.ui.theme.DividerDark
import com.brutal.accountability.ui.theme.GradientRedEnd
import com.brutal.accountability.ui.theme.GradientRedStart
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextOnRed
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary

@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    showAccent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = if (showAccent) BorderStroke(1.dp, BrutalRedSubtle) else BorderStroke(1.dp, DividerDark)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrutalRed,
            contentColor = TextOnRed,
            disabledContainerColor = BrutalRedDark.copy(alpha = 0.3f),
            disabledContentColor = TextMuted
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (enabled) Brush.horizontalGradient(
                        listOf(GradientRedStart, GradientRedEnd)
                    ) else Brush.horizontalGradient(
                        listOf(BrutalRedDark.copy(alpha = 0.3f), BrutalRedDark.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun BrutalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextSecondary,
            cursorColor = BrutalRed,
            focusedBorderColor = BrutalRed,
            unfocusedBorderColor = DividerDark,
            focusedLabelColor = BrutalRed,
            unfocusedLabelColor = TextMuted
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = BrutalRed,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun AnimatedScreen(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val state = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(animationSpec = tween(400, delayMillis = delayMillis)) +
                slideInVertically(
                    animationSpec = tween(400, delayMillis = delayMillis),
                    initialOffsetY = { it / 8 }
                )
    ) {
        content()
    }
}
