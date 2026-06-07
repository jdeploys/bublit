package com.bublit.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MlKitOcrEngine {
    fun recognizeLatinAsync(
        bitmap: Bitmap,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    fun recognizeChineseAsync(
        bitmap: Bitmap,
        onSuccess: (Text) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            .process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }
}
