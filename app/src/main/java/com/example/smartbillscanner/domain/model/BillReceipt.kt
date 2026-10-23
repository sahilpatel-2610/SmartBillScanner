package com.example.smartbillscanner.domain.model

data class BillReceipt(
    val id: Long = 0,
    val title: String = "New bill",
    val storeName: String = "",
    val date: Long = System.currentTimeMillis(),
    val items: List<BillItem> = emptyList(),
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val tip: Double = 0.0,
    val totalAmount: Double = 0.0,
    val participants: List<Participant> = emptyList(),
    val isPaid: Boolean = false,
    /** UPI ID where the money has to be sent (merchant, or the friend who paid the bill). */
    val upiId: String = ""
) {
    val subtotal: Double
        get() = items.sumOf { it.totalPrice }

    val calculatedTotal: Double
        get() = (subtotal + tax + tip - discount).coerceAtLeast(0.0)
}
