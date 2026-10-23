package com.example.smartbillscanner.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbillscanner.domain.model.BillReceipt
import com.example.smartbillscanner.ui.components.*
import com.example.smartbillscanner.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    bills: List<BillReceipt>,
    onBillClick: (BillReceipt) -> Unit,
    onDeleteBill: (Long) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<BillReceipt?>(null) }
    pendingDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = SurfaceDark,
            titleContentColor = TextWhite,
            textContentColor = TextMuted,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Delete this bill?") },
            text = { Text("${b.storeName.ifEmpty { b.title }} (${rupees(b.totalAmount)}) will be removed from your phone.") },
            confirmButton = { TextButton(onClick = { onDeleteBill(b.id); pendingDelete = null }) { Text("Delete", color = DangerRed) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = TextMuted) } }
        )
    }
    val totalExpense = bills.sumOf { it.totalAmount }
    val pendingBills = bills.filter { !it.isPaid }
    val pendingAmount = pendingBills.sumOf { it.totalAmount }
    val settledCount = bills.size - pendingBills.size

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = topInset + 16.dp,
            bottom = bottomInset + 120.dp   // room for the floating bar
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLogoMark(size = 46.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Bill Scanner",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Scan  •  Split  •  Settle",
                        color = TextMuted,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // Hero summary
        item {
            val heroShape = RoundedCornerShape(28.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3A2D12), Color(0xFF1C160B), Color(0xFF0D0B07))
                        )
                    )
                    .drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(AccentGold.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(size.width * 0.95f, 0f),
                                radius = size.width * 0.85f
                            )
                        )
                        // decorative rings, like a premium card
                        val ringColor = Color.White.copy(alpha = 0.07f)
                        drawCircle(ringColor, radius = size.width * 0.34f, center = Offset(size.width * 1.0f, size.height * 0.05f), style = Stroke(1.5.dp.toPx()))
                        drawCircle(ringColor, radius = size.width * 0.52f, center = Offset(size.width * 1.0f, size.height * 0.05f), style = Stroke(1.dp.toPx()))
                    }
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(AccentChampagne.copy(alpha = 0.70f), Color.White.copy(alpha = 0.06f), AccentGold.copy(alpha = 0.35f))
                        ),
                        heroShape
                    )
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = AccentChampagne,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "TOTAL SCANNED",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    rupees(totalExpense, 0),
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(Color.White, AccentChampagne)),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroStat("Bills", "${bills.size}", TextWhite, Modifier.weight(1f))
                    HeroDivider()
                    HeroStat("Pending", rupees(pendingAmount, 0), AccentAmber, Modifier.weight(1.3f))
                    HeroDivider()
                    HeroStat("Settled", "$settledCount", SuccessGreen, Modifier.weight(1f))
                }
            }
        }

        // Section title
        item {
            SectionTitle("Recent bills") {
                Text(
                    "${bills.size} total",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (bills.isEmpty()) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(AccentGold.copy(alpha = 0.10f), CircleShape)
                                .border(1.dp, AccentGold.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "No bills yet",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Tap the gold scan button below to add your first bill",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(bills) { bill ->
                BillRow(
                    bill = bill,
                    onClick = { onBillClick(bill) },
                    onDelete = { pendingDelete = bill }
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextMuted, fontSize = 11.sp, letterSpacing = 0.4.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeroDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.10f))
    )
}

@Composable
private fun BillRow(
    bill: BillReceipt,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(bill.date))
    val tone = if (bill.isPaid) SuccessGreen else AccentAmber

    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(tone.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .border(1.dp, tone.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.Receipt,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.storeName.ifEmpty { bill.title },
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$formattedDate  •  ${bill.items.size} items",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rupees(bill.totalAmount),
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(if (bill.isPaid) "SETTLED" else "PENDING", tone)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = DangerRed.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
