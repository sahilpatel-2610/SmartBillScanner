package com.example.smartbillscanner.ui.screens.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbillscanner.ui.theme.*

@Composable
fun CameraOverlay(
    modifier: Modifier = Modifier
) {
    // Laser scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val frameWidth = width * 0.84f
            val frameHeight = height * 0.52f
            val left = (width - frameWidth) / 2f
            val top = height * 0.20f
            val right = left + frameWidth
            val bottom = top + frameHeight
            val radius = 28.dp.toPx()

            // Dim everything outside the viewfinder (even-odd cut-out)
            val scrim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, width, height))
                addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(radius, radius)))
            }
            drawPath(scrim, Color(0x99050506))

            // Hairline frame
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 1.dp.toPx())
            )

            // Rounded corner brackets
            val len = 44.dp.toPx()
            val r = radius
            val brackets = Path().apply {
                // top-left
                moveTo(left, top + len)
                lineTo(left, top + r)
                arcTo(Rect(left, top, left + 2 * r, top + 2 * r), 180f, 90f, false)
                lineTo(left + len, top)
                // top-right
                moveTo(right - len, top)
                lineTo(right - r, top)
                arcTo(Rect(right - 2 * r, top, right, top + 2 * r), 270f, 90f, false)
                lineTo(right, top + len)
                // bottom-right
                moveTo(right, bottom - len)
                lineTo(right, bottom - r)
                arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 0f, 90f, false)
                lineTo(right - len, bottom)
                // bottom-left
                moveTo(left + len, bottom)
                lineTo(left + r, bottom)
                arcTo(Rect(left, bottom - 2 * r, left + 2 * r, bottom), 90f, 90f, false)
                lineTo(left, bottom - len)
            }
            drawPath(
                brackets,
                color = AccentGold.copy(alpha = 0.25f),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                brackets,
                color = AccentGold,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Scanning laser with a soft light trail
            val laserY = top + (frameHeight * laserPosition)
            val trailTop = (laserY - 70.dp.toPx()).coerceAtLeast(top)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, AccentGold.copy(alpha = 0.16f)),
                    startY = trailTop,
                    endY = laserY
                ),
                topLeft = Offset(left + 6.dp.toPx(), trailTop),
                size = Size(frameWidth - 12.dp.toPx(), laserY - trailTop)
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        AccentGold.copy(alpha = 0.85f),
                        AccentChampagne,
                        AccentGold.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    startX = left,
                    endX = right
                ),
                topLeft = Offset(left, laserY - 1.5.dp.toPx()),
                size = Size(frameWidth, 3.dp.toPx())
            )
        }

        // Hint chip (sits below the top bar)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp)
                .background(Color(0xB30A0A0C), RoundedCornerShape(50))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(AccentGold, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Align the receipt inside the frame",
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
