package com.example.smartbillscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartbillscanner.ui.theme.AccentGold
import com.example.smartbillscanner.ui.theme.SurfaceBorder
import com.example.smartbillscanner.ui.theme.SurfaceDark

/**
 * Premium glass card: soft gradient fill, hairline gradient border.
 * hasGlow = a tinted border + faint colour wash instead of a heavy drop-shadow.
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    glowColor: Color = AccentGold,
    hasGlow: Boolean = false,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = SurfaceBorder,
    backgroundColor: Color = SurfaceDark,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val fill = if (hasGlow) {
        Brush.verticalGradient(
            listOf(
                glowColor.copy(alpha = 0.12f).compositeOver(backgroundColor),
                backgroundColor
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.045f).compositeOver(backgroundColor),
                backgroundColor
            )
        )
    }

    val border = if (hasGlow) {
        Brush.linearGradient(
            listOf(
                glowColor.copy(alpha = 0.55f),
                glowColor.copy(alpha = 0.08f),
                glowColor.copy(alpha = 0.28f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.10f),
                borderColor,
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}
