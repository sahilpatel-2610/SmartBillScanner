package com.example.smartbillscanner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbillscanner.R
import com.example.smartbillscanner.ui.theme.*

/** "₹1,234.50" */
fun rupees(amount: Double, decimals: Int = 2): String = "₹" + "%,.${decimals}f".format(amount)

/** App-wide backdrop: deep ink with soft gold / champagne light pools. */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDark, BgDeep)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(AccentGold.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.95f, 0f),
                        radius = size.width * 1.2f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(AccentBronze.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(0f, size.height * 0.45f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(AccentChampagne.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.width * 1.0f
                    )
                )
            },
        content = content
    )
}

/** Brand mark shown inside the app: the full app-icon logo in its rounded square. */
@Composable
fun AppLogoMark(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Image(
        painter = painterResource(R.drawable.ic_logo_mark),
        contentDescription = "SmartBill",
        modifier = modifier.size(size)
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextWhite,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceDark.copy(alpha = 0.85f))
            .border(1.dp, HairLine, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            GlassIconButton(Icons.Default.ArrowBack, "Back", onBack)
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            if (subtitle != null) {
                Text(subtitle, color = TextMuted, fontSize = 13.sp)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(AccentChampagne, AccentBronze)))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SmallActionPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(AccentGold.copy(alpha = 0.12f))
            .border(1.dp, AccentGold.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text, color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.30f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
    }
}

@Composable
fun premiumFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = AccentGold,
    unfocusedBorderColor = SurfaceBorder,
    focusedLabelColor = AccentGold,
    unfocusedLabelColor = TextMuted,
    cursorColor = AccentGold,
    focusedContainerColor = SurfaceDark.copy(alpha = 0.6f),
    unfocusedContainerColor = SurfaceDark.copy(alpha = 0.6f)
)

@Composable
fun PremiumField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null
) {
    val prefixContent: (@Composable () -> Unit)? =
        if (prefix != null) ({ Text(prefix, color = TextMuted) }) else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        prefix = prefixContent,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        colors = premiumFieldColors(),
        shape = RoundedCornerShape(14.dp)
    )
}

/** Sticky bottom bar for flow screens (Verify / Split). Handles the gesture-bar inset itself. */
@Composable
fun BottomActionBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(SurfaceDark, BgDeep)))
            .drawBehind {
                drawLine(HairLine, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        content = content
    )
}
