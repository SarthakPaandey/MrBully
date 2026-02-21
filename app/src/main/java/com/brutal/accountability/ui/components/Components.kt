package com.brutal.accountability.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.brutal.accountability.ui.theme.BrutalRedLight
import com.brutal.accountability.ui.theme.BrutalRedSubtle
import com.brutal.accountability.ui.theme.CardSurface
import com.brutal.accountability.ui.theme.DeepBlack
import com.brutal.accountability.ui.theme.DividerDark
import com.brutal.accountability.ui.theme.GlassSurface
import com.brutal.accountability.ui.theme.GlowRed
import com.brutal.accountability.ui.theme.GradientRedEnd
import com.brutal.accountability.ui.theme.GradientRedStart
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextOnRed
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.SolidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    showAccent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = if (showAccent) BorderStroke(1.dp, GlowRed) else BorderStroke(1.dp, DividerDark.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(150),
        label = "ButtonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = TextOnRed,
            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledContentColor = TextMuted
        ),
        border = if (enabled) BorderStroke(1.dp, BrutalRedLight.copy(alpha=0.5f)) else BorderStroke(1.dp, DividerDark),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(GradientRedStart, GradientRedEnd))
                    else SolidColor(BrutalRedDark.copy(alpha = 0.3f))
                )
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
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
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextSecondary,
            cursorColor = BrutalRedLight,
            focusedBorderColor = BrutalRed,
            unfocusedBorderColor = DividerDark.copy(alpha=0.3f),
            focusedLabelColor = BrutalRedLight,
            unfocusedLabelColor = TextMuted,
            focusedContainerColor = DeepBlack.copy(alpha=0.5f),
            unfocusedContainerColor = CardSurface.copy(alpha=0.5f)
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

@Composable
fun BrutalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        label = "SwitchThumbAnim"
    )
    val trackColor = if (checked) GlowRed else CardSurface
    val thumbColor = if (checked) BrutalRed else TextMuted
    val borderColor = if (checked) BrutalRed else DividerDark.copy(alpha = 0.5f)
    
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 28.dp)
            .background(trackColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(20.dp)
                .background(thumbColor, RoundedCornerShape(10.dp))
                // subtle glow
                .border(1.dp, if (checked) BrutalRedLight else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp))
        )
    }
}
