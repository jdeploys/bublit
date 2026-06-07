package com.bublit.app.translation

import com.bublit.app.domain.SourceLanguage

class FakeTranslationEngine : TranslationEngine {
    override fun translate(text: String, sourceLanguage: SourceLanguage): String {
        val normalized = text.trim()
        return when {
            sourceLanguage == SourceLanguage.English && normalized.equals("Where are we?", ignoreCase = true) ->
                "어디에 있는 거야?"
            sourceLanguage == SourceLanguage.English && normalized.equals("Keep moving.", ignoreCase = true) ->
                "계속 움직여."
            sourceLanguage == SourceLanguage.Chinese && normalized == "我们走吧" ->
                "가자"
            sourceLanguage == SourceLanguage.Chinese ->
                "중국어 대사 번역 준비 중"
            sourceLanguage == SourceLanguage.English ->
                "$normalized (한국어 미리보기)"
            else ->
                "$normalized (로컬 번역 대기)"
        }
    }
}
