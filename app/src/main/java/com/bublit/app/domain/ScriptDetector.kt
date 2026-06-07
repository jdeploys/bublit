package com.bublit.app.domain

class ScriptDetector {
    fun detectSourceLanguage(text: String): SourceLanguage {
        val counts = text.fold(ScriptCounts()) { counts, char ->
            when {
                char.isChineseHan() -> counts.copy(chinese = counts.chinese + 1)
                char.isLatinLetter() -> counts.copy(english = counts.english + 1)
                char.isHangul() -> counts.copy(korean = counts.korean + 1)
                else -> counts
            }
        }

        return when {
            counts.chinese > 0 && counts.chinese >= counts.english -> SourceLanguage.Chinese
            counts.english > 0 -> SourceLanguage.English
            else -> SourceLanguage.Unknown
        }
    }

    private data class ScriptCounts(
        val english: Int = 0,
        val chinese: Int = 0,
        val korean: Int = 0,
    )

    private fun Char.isLatinLetter(): Boolean {
        return this in 'A'..'Z' || this in 'a'..'z'
    }

    private fun Char.isChineseHan(): Boolean {
        return this in '\u4E00'..'\u9FFF' ||
            this in '\u3400'..'\u4DBF' ||
            this in '\uF900'..'\uFAFF'
    }

    private fun Char.isHangul(): Boolean {
        return this in '\uAC00'..'\uD7AF' ||
            this in '\u1100'..'\u11FF' ||
            this in '\u3130'..'\u318F'
    }
}
