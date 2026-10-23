package com.example.smartbillscanner.ocr

import java.util.Calendar
import java.util.Locale

data class ParsedItem(val name: String, val price: Double, val quantity: Int = 1)

data class ParsedReceipt(
    val storeName: String = "",
    val dateMillis: Long? = null,
    val items: List<ParsedItem> = emptyList(),
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val tip: Double = 0.0,
    val total: Double = 0.0
) {
    /** A photo only counts as a bill when real item rows were read (and it has a total or 2+ items). */
    val isEmpty: Boolean get() = items.isEmpty() || (total <= 0.0 && items.size < 2)
}

/**
 * Turns the text rows of a receipt (already merged left-to-right by [MlKitTextParser]) into
 * store name, date, items, tax, discount, tip and total. Pure Kotlin, no Android classes.
 */
object ReceiptParser {

    private val totalStrong = Regex(
        """\b(grand\s*total|net\s*(payable|amount|total)|amount\s*(payable|due)|total\s*(payable|due)|balance\s*due|to\s*pay|payable)\b"""
    )
    private val totalMedium = Regex("""\b(total\s*amount|bill\s*(amount|total)|total\s*bill|invoice\s*(amount|total)|total\s*amt)\b""")
    private val totalWeak = Regex("""^\W*total\b""")
    private val totalNotMoney = Regex("""\btotal\s*(qty|quantity|items?|item\s*count|tax|gst|discount|savings?|cgst|sgst)\b""")
    private val subtotal = Regex("""\b(sub\s*-?\s*total|taxable|gross|item\s*total)\b""")
    private val taxRow = Regex("""\b(cgst|sgst|igst|utgst|gst|vat|tax|service\s*(charge|tax)|s\.?\s*charge|cess|packing|packaging|delivery)\b""")
    private val discountRow = Regex("""\b(discount|disc|savings?|saved|coupon|promo)\b""")
    private val tipRow = Regex("""\b(tip|gratuity)\b""")
    private val header = Regex("""\b(qty|quantity)\b.*\b(rate|price|amt|amount|mrp)\b|\b(rate|price|mrp)\b.*\b(amt|amount)\b""")
    private val ignoreRow = Regex(
        """round\s*-?\s*off|rounding|\bchange\b|\bcash\b|tender|\bpaid\b|\bupi\b|\bcard\b|\bvisa\b|mastercard|""" +
            """\binvoice\b|\bbill\s*(no|number|#)|\border\s*(no|id|#|number)|\btable\b|\btbl\b|\btoken\b|""" +
            """\b(phone|ph|tel|mob|mobile|contact)\b|gstin|fssai|\bpan\b|\bdate\b|\btime\b|""" +
            """\b(thank|visit again|welcome)\b|\b(server|cashier|waiter|steward|captain)\b|\bpax\b|\bguests?\b|""" +
            """www\.|http|\.com|\bemail\b|\baddress\b|\bpin\b|\bcustomer\b|\bdine\s*in\b|\btake\s*away\b"""
    )
    private val storeBad = Regex("""invoice|bill|receipt|gst|welcome|thank|cash\s*memo|tax|phone|tel\b|mob|www|http|\.com|order|table|date|time""")

    private val currency = Regex("""(?i)(₹|\brs\.?|\binr)\s*(?=\d)""")

    // ---------- money ----------

    fun parseAmount(raw: String): Double? {
        var s = raw.trim()
        s = s.replace("₹", "")
        s = s.replace(Regex("(?i)^(rs\\.?|inr)"), "").trim()
        if (s.endsWith("/-")) s = s.dropLast(2)
        s = s.trim().trimEnd('-').trim()
        if (s.isEmpty() || !s.any { it.isDigit() }) return null
        if (!s.all { it.isDigit() || it == '.' || it == ',' }) return null
        s = if (!s.contains('.') && Regex("""^\d+,\d{2}$""").matches(s)) s.replace(',', '.') else s.replace(",", "")
        if (s.count { it == '.' } > 1) return null
        val v = s.toDoubleOrNull() ?: return null
        if (v >= 10_000_000.0) return null // phone numbers / invoice numbers, not money
        return v
    }

    private fun isWholeSmall(raw: String, max: Int = 99): Boolean {
        val n = raw.trim().toIntOrNull() ?: return false
        return n in 1..max && raw.none { it == '.' || it == ',' }
    }

    private fun lastAmount(row: String): Double? {
        val tokens = clean(row).split(Regex("\\s+")).filter { it.isNotEmpty() }
        for (i in tokens.indices.reversed()) {
            val v = parseAmount(tokens[i])
            if (v != null) return v
        }
        return null
    }

    private fun clean(row: String): String =
        row.replace(currency, "").replace('|', ' ').replace('\t', ' ').trim()

    private fun round2(d: Double): Double = Math.round(d * 100.0) / 100.0

    // ---------- single item row ----------

    internal fun parseItemRow(row: String): ParsedItem? {
        val tokens = clean(row).split(Regex("\\s+")).filter { it.isNotEmpty() }
        var end = tokens.size
        val rawNums = ArrayList<String>()
        val nums = ArrayList<Double>()
        while (end > 0 && nums.size < 4) {
            val v = parseAmount(tokens[end - 1]) ?: break
            nums.add(0, v)
            rawNums.add(0, tokens[end - 1])
            end--
        }
        if (nums.isEmpty()) return null

        val nameTokens = tokens.subList(0, end).toMutableList()
        var qty = 1
        var amount: Double

        when {
            nums.size == 1 -> amount = nums[0]
            nums.size == 2 -> {
                if (isWholeSmall(rawNums[0]) && nums[1] >= nums[0]) {
                    qty = nums[0].toInt(); amount = nums[1]
                } else {
                    amount = nums[1]
                    val ratio = if (nums[0] > 0) nums[1] / nums[0] else 0.0
                    if (ratio >= 1.0 && Math.abs(ratio - Math.round(ratio)) < 0.02 && Math.round(ratio) <= 99) {
                        qty = Math.round(ratio).toInt()
                    }
                }
            }
            else -> {
                val n = nums.size
                amount = nums[n - 1]
                if (isWholeSmall(rawNums[n - 3])) qty = nums[n - 3].toInt()
            }
        }

        // serial number / qty glued to the name: "1." "2x" "3 Paneer ..."
        if (nameTokens.isNotEmpty() && Regex("""^\d{1,3}[.)]$""").matches(nameTokens[0])) nameTokens.removeAt(0)
        if (nums.size == 1 && nameTokens.size >= 2) {
            val m = Regex("""^(\d{1,2})[xX×]$""").matchEntire(nameTokens[0])
            if (m != null) {
                qty = m.groupValues[1].toInt(); nameTokens.removeAt(0)
            } else if (Regex("""^\d{1,2}$""").matches(nameTokens[0]) && nameTokens[0].toInt() in 1..20) {
                qty = nameTokens[0].toInt(); nameTokens.removeAt(0)
            }
        } else if (nums.size >= 2 && nameTokens.isNotEmpty() && Regex("""^\d{1,3}$""").matches(nameTokens[0]) && nameTokens.size >= 2) {
            nameTokens.removeAt(0) // serial number column
        }

        // "2 x Masala Chai": drop the lone multiplication sign that is left behind
        if (nameTokens.size >= 2 && (nameTokens[0] == "x" || nameTokens[0] == "X" || nameTokens[0] == "×")) nameTokens.removeAt(0)

        var name = nameTokens.joinToString(" ")
        val trailingQty = Regex("""(?i)\s*[x×]\s*(\d{1,2})$""").find(name)
        if (trailingQty != null && nums.size == 1 && qty == 1) {
            qty = trailingQty.groupValues[1].toInt()
            name = name.removeRange(trailingQty.range).trim()
        }
        name = name.trim(' ', '-', ':', '.', '*', '_', '=', '#', '|', '@', ',')
        name = name.replace(Regex("\\s+"), " ")

        if (name.count { it.isLetter() } < 2) return null
        if (amount <= 0.0 || qty < 1) return null
        return ParsedItem(name = name, price = round2(amount / qty), quantity = qty)
    }

    // ---------- whole receipt ----------

    fun parse(rows: List<String>): ParsedReceipt {
        val items = ArrayList<ParsedItem>()
        val taxLines = ArrayList<Double>()
        var totalTax: Double? = null
        var discount = 0.0
        var tip = 0.0
        var total = 0.0
        var totalPriority = 0
        var pendingName: String? = null

        for (rawRow in rows) {
            val row = rawRow.trim()
            if (row.isEmpty()) continue
            val low = row.lowercase(Locale.ROOT)

            if (header.containsMatchIn(low)) { pendingName = null; continue }

            // totals
            if (!totalNotMoney.containsMatchIn(low) && !subtotal.containsMatchIn(low)) {
                val prio = when {
                    totalStrong.containsMatchIn(low) -> 3
                    totalMedium.containsMatchIn(low) -> 2
                    totalWeak.containsMatchIn(low) -> 1
                    else -> 0
                }
                if (prio > 0) {
                    val v = lastAmount(row)
                    if (v != null && v > 0 && prio >= totalPriority) { total = v; totalPriority = prio }
                    pendingName = null
                    continue
                }
            }
            if (subtotal.containsMatchIn(low) || totalNotMoney.containsMatchIn(low)) { pendingName = null; continue }

            if (tipRow.containsMatchIn(low)) {
                lastAmount(row)?.let { tip = it }
                pendingName = null; continue
            }
            if (discountRow.containsMatchIn(low)) {
                lastAmount(row)?.let { if (it > discount) discount = it }
                pendingName = null; continue
            }
            if (taxRow.containsMatchIn(low)) {
                val v = lastAmount(row)
                if (v != null) {
                    if (Regex("""total\s*(tax|gst)|tax\s*total""").containsMatchIn(low)) totalTax = v else taxLines.add(v)
                }
                pendingName = null; continue
            }
            if (ignoreRow.containsMatchIn(low)) { pendingName = null; continue }

            val item = parseItemRow(row)
            if (item != null) {
                items.add(item); pendingName = null
            } else {
                // numbers-only row right after a name-only row (wrapped item line)
                val tokens = clean(row).split(Regex("\\s+")).filter { it.isNotEmpty() }
                val allNumeric = tokens.isNotEmpty() && tokens.all { parseAmount(it) != null || it.equals("x", true) }
                if (allNumeric && pendingName != null) {
                    parseItemRow(pendingName + " " + clean(row))?.let { items.add(it) }
                    pendingName = null
                } else if (row.count { it.isLetter() } >= 3 && lastAmount(row) == null) {
                    pendingName = row
                } else {
                    pendingName = null
                }
            }
        }

        // Only what was really read from the photo. No invented / placeholder items.
        val tax = totalTax ?: taxLines.sum()
        val finalItems: List<ParsedItem> = items

        return ParsedReceipt(
            storeName = findStoreName(rows),
            dateMillis = findDate(rows),
            items = finalItems,
            tax = round2(tax),
            discount = round2(discount),
            tip = round2(tip),
            total = round2(total)
        )
    }

    // ---------- store + date ----------

    internal fun findStoreName(rows: List<String>): String {
        for (row in rows.take(6)) {
            val r = row.trim()
            val letters = r.count { it.isLetter() }
            val visible = r.count { !it.isWhitespace() }
            if (letters < 3 || visible == 0) continue
            if (letters.toDouble() / visible < 0.6) continue
            if (storeBad.containsMatchIn(r.lowercase(Locale.ROOT))) continue
            val cleaned = r.trim(' ', '*', '-', '=', '_', '.', ',', ':', '#', '|')
            return if (cleaned == cleaned.uppercase(Locale.ROOT)) {
                cleaned.lowercase(Locale.ROOT).split(" ").joinToString(" ") { w ->
                    w.replaceFirstChar { c -> c.uppercase() }
                }
            } else cleaned
        }
        return ""
    }

    private val numericDate = Regex("""(?<!\d)(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4}|\d{2})(?!\d)""")
    private val textDate = Regex("""(?i)(?<!\d)(\d{1,2})[\s\-]*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\s\-,.]*(\d{4}|\d{2})(?!\d)""")
    private val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    internal fun findDate(rows: List<String>): Long? {
        val now = System.currentTimeMillis()
        for (row in rows) {
            var d = 0; var m = 0; var y = 0
            val t = textDate.find(row)
            val n = numericDate.find(row)
            when {
                t != null -> {
                    d = t.groupValues[1].toInt()
                    m = months.indexOf(t.groupValues[2].lowercase(Locale.ROOT)) + 1
                    y = t.groupValues[3].toInt()
                }
                n != null -> {
                    d = n.groupValues[1].toInt(); m = n.groupValues[2].toInt(); y = n.groupValues[3].toInt()
                }
                else -> continue
            }
            if (y < 100) y += 2000
            if (d !in 1..31 || m !in 1..12 || y < 2000) continue
            val cal = Calendar.getInstance()
            cal.clear()
            cal.set(y, m - 1, d, 12, 0, 0)
            val ms = cal.timeInMillis
            if (ms > now + 86_400_000L) continue // OCR junk from the future
            return ms
        }
        return null
    }
}
