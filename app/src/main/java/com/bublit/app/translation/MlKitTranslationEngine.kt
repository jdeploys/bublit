package com.bublit.app.translation

import com.bublit.app.domain.SourceLanguage
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.isActive

class MlKitTranslationEngine internal constructor(
    private val translatorFactory: MlKitTranslatorFactory = GoogleMlKitTranslatorFactory,
) {
    fun translateAsync(
        text: String,
        sourceLanguage: SourceLanguage,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val translator = translatorFactory.create(
            sourceLanguage = sourceLanguage.toMlKitLanguage(),
            targetLanguage = TranslateLanguage.KOREAN,
        )
        val closed = AtomicBoolean(false)
        fun closeTranslator() {
            if (closed.compareAndSet(false, true)) {
                translator.close()
            }
        }

        translator.downloadModelIfNeeded(
            onSuccess = {
                translator.translate(
                    text = text,
                    onSuccess = { translated ->
                        closeTranslator()
                        onSuccess(translated)
                    },
                    onFailure = { error ->
                        closeTranslator()
                        onFailure(error)
                    },
                    onCanceled = {
                        closeTranslator()
                        onFailure(CancellationException("ML Kit task was cancelled"))
                    },
                )
            },
            onFailure = { error ->
                closeTranslator()
                onFailure(error)
            },
            onCanceled = {
                closeTranslator()
                onFailure(CancellationException("ML Kit task was cancelled"))
            },
        )
    }

    suspend fun translate(text: String, sourceLanguage: SourceLanguage): String {
        val translator = translatorFactory.create(
            sourceLanguage = sourceLanguage.toMlKitLanguage(),
            targetLanguage = TranslateLanguage.KOREAN,
        )

        try {
            translator.awaitModelDownload()
            return translator.awaitTranslation(text)
        } finally {
            translator.close()
        }
    }
}

internal fun interface MlKitTranslatorFactory {
    fun create(sourceLanguage: String, targetLanguage: String): MlKitTranslator
}

internal interface MlKitTranslator {
    fun downloadModelIfNeeded(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        onCanceled: () -> Unit,
    )
    fun translate(
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onCanceled: () -> Unit,
    )
    fun close()
}

private object GoogleMlKitTranslatorFactory : MlKitTranslatorFactory {
    override fun create(sourceLanguage: String, targetLanguage: String): MlKitTranslator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
        return GoogleMlKitTranslator(Translation.getClient(options))
    }
}

private class GoogleMlKitTranslator(
    private val translator: Translator,
) : MlKitTranslator {
    override fun downloadModelIfNeeded(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        onCanceled: () -> Unit,
    ) {
        translator.downloadModelIfNeeded().addCompletionListeners(
            onSuccess = { onSuccess() },
            onFailure = onFailure,
            onCanceled = onCanceled,
        )
    }

    override fun translate(
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onCanceled: () -> Unit,
    ) {
        translator.translate(text).addCompletionListeners(
            onSuccess = onSuccess,
            onFailure = onFailure,
            onCanceled = onCanceled,
        )
    }

    override fun close() {
        translator.close()
    }
}

private fun SourceLanguage.toMlKitLanguage(): String {
    return when (this) {
        SourceLanguage.English -> TranslateLanguage.ENGLISH
        SourceLanguage.Chinese -> TranslateLanguage.CHINESE
        SourceLanguage.Japanese -> TranslateLanguage.JAPANESE
        SourceLanguage.Unknown -> TranslateLanguage.ENGLISH
    }
}

private suspend fun MlKitTranslator.awaitModelDownload() {
    return suspendCancellableCoroutine { continuation ->
        downloadModelIfNeeded(
            onSuccess = {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            },
            onFailure = { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            },
            onCanceled = {
                if (continuation.isActive) {
                    continuation.resumeWithException(CancellationException("ML Kit task was cancelled"))
                }
            },
        )
    }
}

private suspend fun MlKitTranslator.awaitTranslation(text: String): String {
    return suspendCancellableCoroutine { continuation ->
        translate(
            text = text,
            onSuccess = { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            },
            onFailure = { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            },
            onCanceled = {
                if (continuation.isActive) {
                    continuation.resumeWithException(CancellationException("ML Kit task was cancelled"))
                }
            },
        )
    }
}

private fun <T> Task<T>.addCompletionListeners(
    onSuccess: (T) -> Unit,
    onFailure: (Exception) -> Unit,
    onCanceled: () -> Unit,
) {
    addOnSuccessListener(onSuccess)
    addOnFailureListener(onFailure)
    addOnCanceledListener {
        onCanceled()
    }
}
