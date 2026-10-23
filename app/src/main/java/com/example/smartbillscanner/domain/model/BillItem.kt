package com.example.smartbillscanner.domain.model

data class BillItem(
    val id: Long = 0,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val assignedTo: List<String> = emptyList()
) {
    val totalPrice: Double
        get() = price * quantity
}
