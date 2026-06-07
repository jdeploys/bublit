package com.bublit.app.translation

import com.bublit.app.domain.SourceLanguage

interface TranslationEngine {
    fun translate(text: String, sourceLanguage: SourceLanguage): String
}
