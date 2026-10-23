package com.example.smartbillscanner.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Reads a bill photo (camera shot or gallery image) and returns the parsed receipt. */
object BillOcr {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    fun read(
        context: Context,
        uri: Uri,
        onResult: (ParsedReceipt) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onError(e)
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { text ->
                try {
                    onResult(MlKitTextParser.parseReceipt(text))
                } catch (e: Exception) {
                    onError(e)
                }
            }
            .addOnFailureListener { onError(it) }
    }
}
