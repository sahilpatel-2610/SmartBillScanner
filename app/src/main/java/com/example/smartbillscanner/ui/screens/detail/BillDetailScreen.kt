package com.example.smartbillscanner.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbillscanner.domain.model.BillReceipt
import com.example.smartbillscanner.ui.components.*
import com.example.smartbillscanner.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything that was saved for one bill: items, taxes, total, people and payment status. */
@Composable
fun BillDetailScreen(
    bill: BillReceipt,
    onEdit: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val date = remember(bill.date) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(bill.date))
    }
    val tone = if (bill.isPaid) SuccessGreen else AccentAmber
    val total = if (bill.totalAmount > 0) bill.totalAmount else bill.calculatedTotal

    androidx.compose.material3.Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomActionBar {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonOutlinedButton(
                        text = "Edit",
                        icon = Icons.Default.Edit,
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    )
                    NeonPrimaryButton(
                        text = "Split & Pay",
                        icon = Icons.Default.CallSplit,
                        onClick = onSplit,
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = bill.storeName.ifBlank { bill.title },
                    subtitle = date,
                    onBack = onNavigateBack
                ) {
                    GlassIconButton(Icons.Default.DeleteOutline, "Delete bill", { confirmDelete = true }, tint = DangerRed)
                }
            }

            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), hasGlow = true, cornerRadius = 26.dp, contentPadding = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(rupees(total), color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                            if (bill.title.isNotBlank() && bill.storeName.isNotBlank()) {
                                Text(bill.title, color = TextMuted, fontSize = 12.sp)
                            }
                        }
                        StatusChip(if (bill.isPaid) "SETTLED" else "PENDING", tone)
                    }
                }
            }

            item { SectionTitle("Items (${bill.items.size})") }

            if (bill.items.isEmpty()) {
                item { Text("No items saved for this bill.", color = TextMuted, fontSize = 13.sp) }
            }
            itemsIndexed(bill.items) { index, item ->
                NeonCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = 14.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.width(26.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text("${item.quantity} × ${rupees(item.price)}", color = TextMuted, fontSize = 12.sp)
                        }
                        Text(rupees(item.totalPrice), color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Line("Subtotal", rupees(bill.subtotal))
                        if (bill.tax > 0) Line("Tax / GST", "+ " + rupees(bill.tax))
                        if (bill.tip > 0) Line("Tip", "+ " + rupees(bill.tip))
                        if (bill.discount > 0) Line("Discount", "− " + rupees(bill.discount), SuccessGreen)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(HairLine))
                        Spacer(Modifier.height(8.dp))
                        Line("Total", rupees(total), AccentChampagne, bold = true)
                    }
                }
            }

            if (bill.participants.isNotEmpty()) {
                item { SectionTitle("Split (${bill.participants.size} people)") }
                itemsIndexed(bill.participants) { _, p ->
                    val c = if (p.isPaid) SuccessGreen else AccentAmber
                    NeonCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = 14.dp) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(rupees(p.shareAmount), color = TextMuted, fontSize = 12.sp)
                            }
                            StatusChip(if (p.isPaid) "PAID" else "PENDING", c)
                        }
                    }
                }
                if (bill.upiId.isNotBlank()) {
                    item { Text("Pay to UPI: ${bill.upiId}", color = TextMuted, fontSize = 12.sp) }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = TextFaint, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Saved privately on this phone only", color = TextFaint, fontSize = 11.sp)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SurfaceDark,
            titleContentColor = TextWhite,
            textContentColor = TextMuted,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Delete this bill?") },
            text = { Text("This removes the bill and its split details from your phone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun Line(label: String, value: String, valueColor: Color = TextWhite, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (bold) TextWhite else TextMuted, fontSize = if (bold) 15.sp else 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = valueColor, fontSize = if (bold) 19.sp else 14.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium)
    }
}
