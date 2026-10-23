package com.example.smartbillscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbillscanner.ui.theme.*

/** Floating glass navigation pill with a raised scan button in the middle. */
@Composable
fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val pill = RoundedCornerShape(34.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = pill,
                    clip = false,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(pill)
                .background(Brush.verticalGradient(listOf(SurfaceHigh, SurfaceDark)))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))),
                    pill
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarItem(
                icon = Icons.Default.GridView,
                label = "Home",
                selected = currentRoute == "dashboard",
                onClick = { onNavigate("dashboard") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(76.dp))
            BarItem(
                icon = Icons.Default.CallSplit,
                label = "Split & Pay",
                selected = currentRoute == "split",
                onClick = { onNavigate("split") },
                modifier = Modifier.weight(1f)
            )
        }

        // Raised scan button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(68.dp)
                .background(BgDark, CircleShape)
                .padding(6.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = AccentGold.copy(alpha = 0.35f),
                    spotColor = AccentChampagne.copy(alpha = 0.60f)
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AccentChampagne, AccentGold, AccentBronze)))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.05f))),
                    CircleShape
                )
                .clickable { onNavigate("scanner") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DocumentScanner,
                contentDescription = "Scan",
                tint = OnGold,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) AccentGold else TextMuted
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(96.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) AccentGold.copy(alpha = 0.14f) else Color.Transparent)
                .border(
                    1.dp,
                    if (selected) AccentGold.copy(alpha = 0.35f) else Color.Transparent,
                    RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}
