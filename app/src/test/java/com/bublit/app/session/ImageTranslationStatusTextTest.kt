package com.bublit.app.session

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageTranslationStatusTextTest {
    @Test
    fun translatedAndFailedStatusNamesImageTranslationCounts() {
        assertEquals(
            "이미지 번역 완료 3개 / 실패 1개",
            imageTranslationStatusText(translatedCount = 3, failedCount = 1),
        )
    }

    @Test
    fun translatedOnlyStatusNamesImageTranslationCounts() {
        assertEquals(
            "이미지 번역 완료 3개",
            imageTranslationStatusText(translatedCount = 3, failedCount = 0),
        )
    }

    @Test
    fun failedOnlyStatusNamesImageTranslationCounts() {
        assertEquals(
            "이미지 번역 실패 1개",
            imageTranslationStatusText(translatedCount = 0, failedCount = 1),
        )
    }
}
