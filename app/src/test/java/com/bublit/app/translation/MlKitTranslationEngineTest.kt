package com.bublit.app.translation

import com.bublit.app.domain.SourceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class MlKitTranslationEngineTest {
    @Test
    fun mlKitTranslationClosesTranslatorAfterSuccessfulTranslation() = runBlocking {
        val translator = FakeMlKitTranslator(
            downloadResult = FakeMlKitResult.Success(Unit),
            translateResult = FakeMlKitResult.Success("안녕"),
        )
        val engine = MlKitTranslationEngine(
            translatorFactory = RecordingTranslatorFactory(translator),
        )

        val translated = engine.translate("hello", SourceLanguage.English)

        assertEquals("안녕", translated)
        assertTrue(translator.closed)
    }

    @Test
    fun mlKitTranslationClosesTranslatorWhenModelDownloadFails() = runBlocking {
        val expectedError = IllegalStateException("model unavailable")
        val translator = FakeMlKitTranslator(
            downloadResult = FakeMlKitResult.Failure(expectedError),
            translateResult = FakeMlKitResult.Success("unused"),
        )
        val engine = MlKitTranslationEngine(
            translatorFactory = RecordingTranslatorFactory(translator),
        )

        val actualError = runCatching {
            engine.translate("hello", SourceLanguage.English)
        }.exceptionOrNull()

        assertTrue(actualError is IllegalStateException)
        assertEquals(expectedError.message, actualError?.message)
        assertTrue(translator.closed)
    }

    @Test
    fun mlKitTranslationUsesJapaneseModelForJapaneseSource() = runBlocking {
        val translator = FakeMlKitTranslator(
            downloadResult = FakeMlKitResult.Success(Unit),
            translateResult = FakeMlKitResult.Success("가자"),
        )
        val factory = RecordingTranslatorFactory(translator)
        val engine = MlKitTranslationEngine(translatorFactory = factory)

        engine.translate("行こう", SourceLanguage.Japanese)

        assertEquals("ja", factory.sourceLanguage)
        assertEquals("ko", factory.targetLanguage)
    }
}

private class RecordingTranslatorFactory(
    private val translator: FakeMlKitTranslator,
) : MlKitTranslatorFactory {
    var sourceLanguage: String? = null
        private set
    var targetLanguage: String? = null
        private set

    override fun create(sourceLanguage: String, targetLanguage: String): MlKitTranslator {
        this.sourceLanguage = sourceLanguage
        this.targetLanguage = targetLanguage
        return translator
    }
}

private class FakeMlKitTranslator(
    private val downloadResult: FakeMlKitResult<Unit>,
    private val translateResult: FakeMlKitResult<String>,
) : MlKitTranslator {
    var closed = false
        private set

    override fun downloadModelIfNeeded(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        onCanceled: () -> Unit,
    ) {
        downloadResult.complete({ onSuccess() }, onFailure)
    }

    override fun translate(
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onCanceled: () -> Unit,
    ) {
        translateResult.complete(onSuccess, onFailure)
    }

    override fun close() {
        closed = true
    }
}

private sealed interface FakeMlKitResult<out T> {
    data class Success<T>(val value: T) : FakeMlKitResult<T>
    data class Failure(val error: Exception) : FakeMlKitResult<Nothing>

    fun complete(onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
        when (this) {
            is Success -> onSuccess(value)
            is Failure -> onFailure(error)
        }
    }
}
