package com.example.smartbillscanner.ui.screens.split

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbillscanner.domain.model.BillReceipt
import com.example.smartbillscanner.domain.model.Participant
import com.example.smartbillscanner.ui.components.*
import com.example.smartbillscanner.ui.theme.*
import java.util.Locale

@Composable
fun SplitPayScreen(
    bill: BillReceipt,
    onComplete: (BillReceipt) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("smartbill_prefs", Context.MODE_PRIVATE) }

    // Only the real people on this bill. "You" paid the bill, friends pay you back.
    val participants = remember(bill.id) {
        mutableStateListOf<Participant>().apply {
            if (bill.participants.isNotEmpty()) {
                addAll(bill.participants)
            } else {
                add(Participant(id = "1", name = "You", isPaid = true))
            }
        }
    }

    // UPI ID that receives the money (remembered for next time)
    var upiId by remember(bill.id) {
        mutableStateOf(bill.upiId.ifBlank { prefs.getString("upi_id", "") ?: "" })
    }
    var upiError by remember { mutableStateOf(false) }
    var payingId by remember { mutableStateOf<String?>(null) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    val total = if (bill.totalAmount > 0) bill.totalAmount else bill.calculatedTotal
    val perPersonShare = if (participants.isNotEmpty()) total / participants.size else 0.0

    val updatedParticipants = participants.map { p -> p.copy(shareAmount = perPersonShare) }

    val paidCount = updatedParticipants.count { it.isPaid }
    val collected = updatedParticipants.filter { it.isPaid }.sumOf { it.shareAmount }
    val progress = if (total > 0) (collected / total).toFloat().coerceIn(0f, 1f) else 0f

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    fun upiReady(): Boolean {
        if (!isValidUpi(upiId)) {
            upiError = true
            toast("Enter a valid UPI ID first (example: name@okhdfcbank)")
            return false
        }
        upiError = false
        prefs.edit().putString("upi_id", upiId.trim()).apply()
        return true
    }

    // UPI apps (GPay / PhonePe / Paytm / BHIM) report the result back to us
    val upiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val response = data?.getStringExtra("response") ?: data?.getStringExtra("Status") ?: ""
        val id = payingId
        payingId = null
        if (id != null) {
            if (response.contains("success", ignoreCase = true)) {
                val idx = participants.indexOfFirst { it.id == id }
                if (idx >= 0) participants[idx] = participants[idx].copy(isPaid = true)
                toast("Payment successful ✅")
            } else {
                toast("Payment not confirmed. If you did pay, tick the checkbox to mark it paid.")
            }
        }
    }

    fun startPay(p: Participant, amount: Double) {
        if (!upiReady()) return
        payingId = p.id
        val note = "Bill split - ${bill.storeName.ifBlank { bill.title }}"
        try {
            upiLauncher.launch(Intent(Intent.ACTION_VIEW, upiUri(upiId.trim(), amount, note)))
        } catch (e: Exception) {
            payingId = null
            toast("No UPI app found. Install GPay, PhonePe or Paytm.")
        }
    }

    fun sendRequest(p: Participant, amount: Double) {
        if (!upiReady()) return
        val place = bill.storeName.ifBlank { "our bill" }
        val note = "Bill split - ${bill.storeName.ifBlank { bill.title }}"
        val text = "Hi ${p.name}! Your share for $place is ₹${money(amount)}.\n" +
            "Please pay on UPI ID: ${upiId.trim()}\n" +
            "Or open this link: ${upiUri(upiId.trim(), amount, note)}"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Request payment"))
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomActionBar {
                NeonPrimaryButton(
                    text = "Save & Complete Settlement",
                    icon = Icons.Default.CheckCircle,
                    onClick = {
                        val finalBill = bill.copy(
                            participants = updatedParticipants,
                            totalAmount = total,
                            isPaid = updatedParticipants.all { it.isPaid },
                            upiId = upiId.trim()
                        )
                        if (isValidUpi(upiId)) prefs.edit().putString("upi_id", upiId.trim()).apply()
                        onComplete(finalBill)
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
                    title = "Split & Pay",
                    subtitle = bill.storeName.ifEmpty { "Bill split" },
                    onBack = onNavigateBack
                )
            }

            // Total overview
            item {
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    hasGlow = true,
                    glowColor = AccentGold,
                    cornerRadius = 26.dp,
                    contentPadding = 20.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    "TOTAL BILL",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.4.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    rupees(total),
                                    color = TextWhite,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.6).sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Each person", color = TextMuted, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    rupees(perPersonShare),
                                    color = AccentGold,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Collection progress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(AccentGold, AccentChampagne)))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$paidCount of ${updatedParticipants.size} paid",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                "${rupees(collected)} collected",
                                color = AccentGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Where the money goes
            item {
                NeonCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "PAYMENT DETAILS",
                            color = AccentChampagne,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PremiumField(
                            value = upiId,
                            onValueChange = { upiId = it.trim(); upiError = false },
                            label = "UPI ID of who paid the bill (name@okhdfcbank)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (upiError) "Enter a valid UPI ID to continue"
                            else "Enter the UPI ID of the person who paid the restaurant (usually you). " +
                                "Friends then pay their share to this ID: tap \"Pay\" to open GPay / PhonePe / Paytm " +
                                "with the amount filled in, or \"Request\" to send them a payment message.",
                            color = if (upiError) DangerRed else TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Share summary
            item {
                NeonOutlinedButton(
                    text = "Share summary",
                    icon = Icons.Default.Share,
                    onClick = {
                        val shareText = StringBuilder()
                        shareText.append("🧾 *Bill Breakdown - ${bill.storeName.ifBlank { bill.title }}*\n\n")
                        shareText.append("Total Amount: ₹${money(total)}\n")
                        shareText.append("Per Person Share: ₹${money(perPersonShare)}\n\n")
                        shareText.append("*Split Among:*\n")
                        updatedParticipants.forEach { p ->
                            val status = if (p.isPaid) "✅ Paid" else "⏳ Pending"
                            shareText.append("• ${p.name}: ₹${money(p.shareAmount)} ($status)\n")
                        }
                        if (isValidUpi(upiId)) shareText.append("\nPay via UPI to: ${upiId.trim()}")

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText.toString())
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Bill Summary"))
                    }
                )
            }

            // Participants header
            item {
                SectionTitle("Split with friends (${participants.size})") {
                    SmallActionPill(
                        text = "Add friend",
                        icon = Icons.Default.PersonAdd,
                        onClick = { showAddFriendDialog = true }
                    )
                }
            }

            // Participant cards
            itemsIndexed(participants) { index, participant ->
                val share = perPersonShare
                val tone = if (participant.isPaid) SuccessGreen else AccentAmber

                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    contentPadding = 14.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (participant.isPaid)
                                            Brush.linearGradient(listOf(AccentGold.copy(alpha = 0.35f), AccentBronze.copy(alpha = 0.15f)))
                                        else
                                            Brush.linearGradient(listOf(SurfaceHigh, SurfaceBorder)),
                                        CircleShape
                                    )
                                    .border(1.dp, tone.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = participant.name.take(1).uppercase(),
                                    color = if (participant.isPaid) SuccessGreen else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    participant.name,
                                    color = TextWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusChip(if (participant.isPaid) "PAID" else "PENDING", tone)
                            }

                            Text(
                                rupees(share),
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Checkbox(
                                checked = participant.isPaid,
                                onCheckedChange = { checked ->
                                    participants[index] = participant.copy(isPaid = checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentGold,
                                    checkmarkColor = OnGold,
                                    uncheckedColor = TextFaint
                                )
                            )
                        }

                        // Real payment actions for everyone who still owes their share
                        if (!participant.isPaid) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PayPill(
                                    text = "Pay ${rupees(share)}",
                                    icon = Icons.Default.NorthEast,
                                    color = AccentGold,
                                    modifier = Modifier.weight(1f),
                                    onClick = { startPay(participant, share) }
                                )
                                PayPill(
                                    text = "Request",
                                    icon = Icons.Default.Send,
                                    color = AccentChampagne,
                                    modifier = Modifier.weight(1f),
                                    onClick = { sendRequest(participant, share) }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (showAddFriendDialog) {
        var friendName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            containerColor = SurfaceDark,
            titleContentColor = TextWhite,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Add friend to split") },
            text = {
                PremiumField(
                    value = friendName,
                    onValueChange = { friendName = it },
                    label = "Friend's name",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (friendName.isNotBlank()) {
                            participants.add(
                                Participant(
                                    id = System.currentTimeMillis().toString(),
                                    name = friendName.trim(),
                                    isPaid = false
                                )
                            )
                            showAddFriendDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Add", color = OnGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}


// ---------- UPI helpers ----------

private val UPI_REGEX = Regex("^[A-Za-z0-9._\\-]{2,256}@[A-Za-z][A-Za-z0-9.\\-]{1,63}$")

private fun isValidUpi(id: String): Boolean = UPI_REGEX.matches(id.trim())

/** Always "123.45" with a dot, whatever the phone language is. */
private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun upiUri(upiId: String, amount: Double, note: String): Uri =
    Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", upiId.substringBefore('@'))
        .appendQueryParameter("am", money(amount))
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", note)
        .build()

@Composable
private fun PayPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.40f), shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
