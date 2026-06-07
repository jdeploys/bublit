package com.bublit.app.session

fun imageTranslationStatusText(translatedCount: Int, failedCount: Int): String {
    return when {
        translatedCount > 0 && failedCount > 0 -> "이미지 번역 완료 ${translatedCount}개 / 실패 ${failedCount}개"
        translatedCount > 0 -> "이미지 번역 완료 ${translatedCount}개"
        failedCount > 0 -> "이미지 번역 실패 ${failedCount}개"
        else -> ""
    }
}
