package com.bublit.app.pipeline

import com.bublit.app.domain.OcrScanLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrScanLanguageTest {
    @Test
    fun japanesePreferenceScansJapaneseBeforeOtherLanguages() {
        val order = ocrScanOrder(OcrScanLanguage.Japanese)

        assertEquals(
            listOf(OcrScanLanguage.Japanese, OcrScanLanguage.English, OcrScanLanguage.Chinese),
            order,
        )
    }

    @Test
    fun englishPreferenceKeepsEnglishBeforeOtherLanguages() {
        val order = ocrScanOrder(OcrScanLanguage.English)

        assertEquals(
            listOf(OcrScanLanguage.English, OcrScanLanguage.Chinese, OcrScanLanguage.Japanese),
            order,
        )
    }
}
