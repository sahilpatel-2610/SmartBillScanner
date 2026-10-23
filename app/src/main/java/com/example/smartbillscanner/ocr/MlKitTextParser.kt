package com.example.smartbillscanner.ocr

import com.google.mlkit.vision.text.Text

/** Converts ML Kit output into text rows, then hands them to [ReceiptParser]. */
object MlKitTextParser {

    private data class L(val text: String, val centerY: Float, val height: Float, val left: Int)

    /**
     * ML Kit often returns the item name and its price as separate "lines" (they sit in different
     * columns). We merge every line that sits on the same visual row, left to right.
     */
    fun toRows(visionText: Text): List<String> {
        val lines = visionText.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            val b = line.boundingBox ?: return@mapNotNull null
            val txt = line.text.trim()
            if (txt.isEmpty()) null else L(txt, (b.top + b.bottom) / 2f, (b.bottom - b.top).toFloat().coerceAtLeast(1f), b.left)
        }
        if (lines.isEmpty()) {
            return visionText.textBlocks.flatMap { it.lines }.map { it.text.trim() }.filter { it.isNotEmpty() }
        }

        val sorted = lines.sortedBy { it.centerY }
        val rows = ArrayList<MutableList<L>>()
        for (l in sorted) {
            val current = rows.lastOrNull()
            if (current != null) {
                val rowCenter = current.map { it.centerY }.average().toFloat()
                val rowHeight = current.map { it.height }.average().toFloat()
                if (kotlin.math.abs(l.centerY - rowCenter) <= 0.6f * minOf(rowHeight, l.height).coerceAtLeast(8f)) {
                    current.add(l)
                    continue
                }
            }
            rows.add(mutableListOf(l))
        }
        return rows.map { r -> r.sortedBy { it.left }.joinToString(" ") { it.text } }
    }

    fun parseReceipt(visionText: Text): ParsedReceipt = ReceiptParser.parse(toRows(visionText))

    /** Cheap count used for the live "Found N items" chip. */
    fun countItems(visionText: Text): Int = parseReceipt(visionText).items.size
}
