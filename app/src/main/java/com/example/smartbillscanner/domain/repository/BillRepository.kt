package com.example.smartbillscanner.domain.repository

import com.example.smartbillscanner.domain.model.BillReceipt
import kotlinx.coroutines.flow.Flow

interface BillRepository {
    fun getAllBills(): Flow<List<BillReceipt>>
    fun getBillById(id: Long): Flow<BillReceipt?>
    suspend fun saveBill(bill: BillReceipt): Long
    suspend fun deleteBill(id: Long)
    suspend fun updateBill(bill: BillReceipt)
}
