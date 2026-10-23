package com.example.smartbillscanner.domain.model

data class Participant(
    val id: String,
    val name: String,
    val upiId: String = "",
    val isPaid: Boolean = false,
    val shareAmount: Double = 0.0
)
