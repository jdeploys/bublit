package com.bublit.app.translation

import com.bublit.app.domain.SourceLanguage
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class MlKitTranslationEngine {
    fun translateAsync(
        text: String,
        sourceLanguage: SourceLanguage,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val source = when (sourceLanguage) {
            SourceLanguage.English -> TranslateLanguage.ENGLISH
            SourceLanguage.Chinese -> TranslateLanguage.CHINESE
            SourceLanguage.Unknown -> TranslateLanguage.ENGLISH
        }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(TranslateLanguage.KOREAN)
            .build()
        val translator = Translation.getClient(options)

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
}
