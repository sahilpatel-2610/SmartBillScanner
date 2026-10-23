package com.example.smartbillscanner.ui.screens.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbillscanner.domain.model.BillItem
import com.example.smartbillscanner.domain.model.BillReceipt
import com.example.smartbillscanner.ui.components.*
import com.example.smartbillscanner.ui.theme.*

@Composable
fun VerifyBillScreen(
    bill: BillReceipt,
    onSaveAndSplit: (BillReceipt) -> Unit,
    onNavigateBack: () -> Unit
) {
    var storeName by remember { mutableStateOf(bill.storeName) }
    var title by remember { mutableStateOf(bill.title) }
    var taxStr by remember { mutableStateOf(numText(bill.tax)) }
    var tipStr by remember { mutableStateOf(numText(bill.tip)) }
    var discountStr by remember { mutableStateOf(numText(bill.discount)) }

    // Only the items that were really read from the bill (or added by the user)
    val items = remember { mutableStateListOf<BillItem>().apply { addAll(bill.items) } }

    // Total printed on the paper bill (0 when entered manually)
    val printedTotal = bill.totalAmount

    var showAddItemDialog by remember { mutableStateOf(false) }

    val subtotal = items.sumOf { it.totalPrice }
    val tax = taxStr.toDoubleOrNull() ?: 0.0
    val tip = tipStr.toDoubleOrNull() ?: 0.0
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val grandTotal = (subtotal + tax + tip - discount).coerceAtLeast(0.0)

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomActionBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grand total", color = TextMuted, fontSize = 12.sp, letterSpacing = 0.4.sp)
                        Text(
                            "${items.size} items",
                            color = TextFaint,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        rupees(grandTotal),
                        style = androidx.compose.ui.text.TextStyle(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(AccentChampagne, AccentGold)),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = "Proceed to Split & Pay",
                    icon = Icons.Default.CallSplit,
                    enabled = items.isNotEmpty() && grandTotal > 0.0,
                    onClick = {
                        val updatedBill = bill.copy(
                            title = title,
                            storeName = storeName,
                            items = items.toList(),
                            tax = tax,
                            tip = tip,
                            discount = discount,
                            totalAmount = grandTotal
                        )
                        onSaveAndSplit(updatedBill)
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Verify bill",
                    subtitle = "Review the scanned items and amounts",
                    onBack = onNavigateBack
                )
            }

            // Store info
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        CardLabel("RECEIPT DETAILS")
                        Spacer(modifier = Modifier.height(12.dp))
                        PremiumField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = "Store / Merchant name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PremiumField(
                            value = title,
                            onValueChange = { title = it },
                            label = "Bill description / tag",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Items header
            item {
                SectionTitle("Items (${items.size})") {
                    SmallActionPill(
                        text = "Add item",
                        icon = Icons.Default.Add,
                        onClick = { showAddItemDialog = true }
                    )
                }
            }

            if (items.isEmpty()) {
                item {
                    NeonCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Text(
                            "No items yet. Tap \"Add item\" to enter what was ordered.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (printedTotal > 0.0 && kotlin.math.abs(printedTotal - grandTotal) > 1.0) {
                item {
                    NeonCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Text(
                            "The bill shows ${rupees(printedTotal)} but these items add up to ${rupees(grandTotal)}. " +
                                "Some items, tax or discount may be missing or misread — please check.",
                            color = AccentAmber,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Items list
            itemsIndexed(items) { index, item ->
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    contentPadding = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(AccentGold.copy(alpha = 0.28f), AccentBronze.copy(alpha = 0.12f))
                                    ),
                                    RoundedCornerShape(11.dp)
                                )
                                .border(1.dp, AccentGold.copy(alpha = 0.40f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                color = AccentGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.quantity} × ${rupees(item.price)}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rupees(item.totalPrice),
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { items.removeAt(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = DangerRed.copy(alpha = 0.75f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Taxes & adjustments
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        CardLabel("TAXES & ADJUSTMENTS")
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumField(
                                value = taxStr,
                                onValueChange = { taxStr = it },
                                label = "Tax / GST",
                                keyboardType = KeyboardType.Decimal,
                                prefix = "₹",
                                modifier = Modifier.weight(1f)
                            )
                            PremiumField(
                                value = tipStr,
                                onValueChange = { tipStr = it },
                                label = "Tip",
                                keyboardType = KeyboardType.Decimal,
                                prefix = "₹",
                                modifier = Modifier.weight(1f)
                            )
                            PremiumField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = "Discount",
                                keyboardType = KeyboardType.Decimal,
                                prefix = "₹",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Breakdown
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), hasGlow = true) {
                    Column {
                        CardLabel("SUMMARY")
                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryLine("Subtotal", rupees(subtotal))
                        SummaryLine("Tax / GST", "+ " + rupees(tax))
                        SummaryLine("Tip", "+ " + rupees(tip))
                        SummaryLine("Discount", "− " + rupees(discount), valueColor = SuccessGreen)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(HairLine)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                rupees(grandTotal),
                                color = AccentChampagne,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    // Dialog for adding manual item
    if (showAddItemDialog) {
        var newItemName by remember { mutableStateOf("") }
        var newItemPrice by remember { mutableStateOf("") }
        var newItemQty by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            containerColor = SurfaceDark,
            titleContentColor = TextWhite,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Add bill item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = "Item name",
                        modifier = Modifier.fillMaxWidth()
                    )
                    PremiumField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it },
                        label = "Price",
                        keyboardType = KeyboardType.Decimal,
                        prefix = "₹",
                        modifier = Modifier.fillMaxWidth()
                    )
                    PremiumField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it },
                        label = "Quantity",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = newItemPrice.toDoubleOrNull() ?: 0.0
                        val q = newItemQty.toIntOrNull() ?: 1
                        if (newItemName.isNotBlank() && p > 0.0) {
                            items.add(
                                BillItem(
                                    id = System.currentTimeMillis(),
                                    name = newItemName,
                                    price = p,
                                    quantity = q
                                )
                            )
                            showAddItemDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Add", color = OnGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

/** 54.0 -> "54", 12.5 -> "12.5", 0.0 -> "" (empty so the user just types) */
private fun numText(value: Double): String = when {
    value <= 0.0 -> ""
    value == Math.floor(value) -> value.toLong().toString()
    else -> value.toString()
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text,
        color = AccentChampagne,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    )
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: Color = TextWhite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
