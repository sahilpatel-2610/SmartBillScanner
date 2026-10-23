package com.example.smartbillscanner.data.repository

import android.content.Context
import com.example.smartbillscanner.domain.model.BillItem
import com.example.smartbillscanner.domain.model.BillReceipt
import com.example.smartbillscanner.domain.model.Participant
import com.example.smartbillscanner.domain.repository.BillRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * Real, persistent storage: every bill is saved to bills.json in the app's private storage,
 * so data survives app restarts. Starts EMPTY (no dummy bills).
 */
class BillRepositoryImpl(context: Context) : BillRepository {

    private val file = File(context.applicationContext.filesDir, "bills.json")
    private val writeLock = Any()

    private val bills = MutableStateFlow(readFromDisk())
    private val idCounter = AtomicLong(bills.value.maxOfOrNull { it.id } ?: 0L)

    override fun getAllBills(): Flow<List<BillReceipt>> = bills.asStateFlow()

    override fun getBillById(id: Long): Flow<BillReceipt?> =
        bills.map { list -> list.find { it.id == id } }

    override suspend fun saveBill(bill: BillReceipt): Long {
        val newId = if (bill.id <= 0) idCounter.incrementAndGet() else bill.id
        val finalBill = bill.copy(
            id = newId,
            totalAmount = if (bill.totalAmount > 0) bill.totalAmount else bill.calculatedTotal
        )
        val updated = bills.value.toMutableList()
        val index = updated.indexOfFirst { it.id == newId }
        if (index >= 0) updated[index] = finalBill else updated.add(0, finalBill)
        bills.value = updated
        writeToDisk(updated)
        return newId
    }

    override suspend fun deleteBill(id: Long) {
        val updated = bills.value.filterNot { it.id == id }
        bills.value = updated
        writeToDisk(updated)
    }

    override suspend fun updateBill(bill: BillReceipt) {
        saveBill(bill)
    }

    // ---------- disk ----------

    private suspend fun writeToDisk(list: List<BillReceipt>) = withContext(Dispatchers.IO) {
        synchronized(writeLock) {
            try {
                val json = JSONArray().also { arr -> list.forEach { arr.put(it.toJson()) } }
                val tmp = File(file.parentFile, "bills.json.tmp")
                tmp.writeText(json.toString())
                if (!tmp.renameTo(file)) {
                    file.writeText(json.toString())
                    tmp.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun readFromDisk(): List<BillReceipt> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { arr.getJSONObject(it).toBill() }
        } catch (e: Exception) {
            // unreadable file: keep a copy instead of overwriting it with an empty list later
            e.printStackTrace()
            try { file.copyTo(File(file.parentFile, "bills.corrupt.json"), overwrite = true) } catch (_: Exception) {}
            emptyList()
        }
    }

    private fun BillReceipt.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("storeName", storeName)
        put("date", date)
        put("tax", tax)
        put("discount", discount)
        put("tip", tip)
        put("totalAmount", totalAmount)
        put("isPaid", isPaid)
        put("upiId", upiId)
        put("items", JSONArray().also { arr ->
            items.forEach { i ->
                arr.put(JSONObject().apply {
                    put("id", i.id)
                    put("name", i.name)
                    put("price", i.price)
                    put("quantity", i.quantity)
                    put("assignedTo", JSONArray(i.assignedTo))
                })
            }
        })
        put("participants", JSONArray().also { arr ->
            participants.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("upiId", p.upiId)
                    put("isPaid", p.isPaid)
                    put("shareAmount", p.shareAmount)
                })
            }
        })
    }

    private fun JSONObject.toBill(): BillReceipt {
        val itemsArr = optJSONArray("items") ?: JSONArray()
        val partsArr = optJSONArray("participants") ?: JSONArray()
        return BillReceipt(
            id = optLong("id"),
            title = optString("title", "Bill"),
            storeName = optString("storeName", ""),
            date = optLong("date", System.currentTimeMillis()),
            items = (0 until itemsArr.length()).map { idx ->
                val o = itemsArr.getJSONObject(idx)
                val assigned = o.optJSONArray("assignedTo")
                BillItem(
                    id = o.optLong("id"),
                    name = o.optString("name", ""),
                    price = o.optDouble("price", 0.0),
                    quantity = o.optInt("quantity", 1),
                    assignedTo = if (assigned == null) emptyList()
                    else (0 until assigned.length()).map { assigned.getString(it) }
                )
            },
            tax = optDouble("tax", 0.0),
            discount = optDouble("discount", 0.0),
            tip = optDouble("tip", 0.0),
            totalAmount = optDouble("totalAmount", 0.0),
            participants = (0 until partsArr.length()).map { idx ->
                val o = partsArr.getJSONObject(idx)
                Participant(
                    id = o.optString("id", idx.toString()),
                    name = o.optString("name", ""),
                    upiId = o.optString("upiId", ""),
                    isPaid = o.optBoolean("isPaid", false),
                    shareAmount = o.optDouble("shareAmount", 0.0)
                )
            },
            isPaid = optBoolean("isPaid", false),
            upiId = optString("upiId", "")
        )
    }
}