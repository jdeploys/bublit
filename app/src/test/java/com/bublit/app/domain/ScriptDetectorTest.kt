package com.bublit.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptDetectorTest {
    private val detector = ScriptDetector()

    @Test
    fun englishTextChoosesEnglishSourceForKoreanTranslation() {
        val language = detector.detectSourceLanguage("Please wait here until I come back.")

        assertEquals(SourceLanguage.English, language)
    }

    @Test
    fun chineseTextChoosesChineseSourceForKoreanTranslation() {
        val language = detector.detectSourceLanguage("请在这里等我回来。")

        assertEquals(SourceLanguage.Chinese, language)
    }

    @Test
    fun japaneseTextChoosesJapaneseSourceForKoreanTranslation() {
        val language = detector.detectSourceLanguage("ここで待っていて。")

        assertEquals(SourceLanguage.Japanese, language)
    }
}
