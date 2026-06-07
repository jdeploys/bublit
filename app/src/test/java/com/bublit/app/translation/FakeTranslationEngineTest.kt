package com.bublit.app.translation

import com.bublit.app.domain.SourceLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeTranslationEngineTest {
    @Test
    fun translatesEnglishToKoreanPreview() {
        val engine = FakeTranslationEngine()

        val translated = engine.translate("Where are we?", SourceLanguage.English)

        assertEquals("어디에 있는 거야?", translated)
    }

    @Test
    fun translatesChineseToKoreanPreview() {
        val engine = FakeTranslationEngine()

        val translated = engine.translate("我们走吧", SourceLanguage.Chinese)

        assertEquals("가자", translated)
    }

    @Test
    fun unknownTextKeepsLocalPreviewMarker() {
        val engine = FakeTranslationEngine()

        val translated = engine.translate("Custom line", SourceLanguage.Unknown)

        assertEquals("Custom line (로컬 번역 대기)", translated)
    }
}
