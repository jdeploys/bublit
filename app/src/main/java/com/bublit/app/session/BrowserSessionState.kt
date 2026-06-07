package com.bublit.app.session

data class BrowserSessionState(
    val pageUrl: String,
    val addressText: String,
) {
    companion object {
        const val DefaultHomeUrl = "https://www.google.com"

        fun restore(
            savedPageUrl: String?,
            savedAddressText: String?,
        ): BrowserSessionState {
            val pageUrl = savedPageUrl?.trim().takeUnless { it.isNullOrBlank() } ?: DefaultHomeUrl
            val addressText = savedAddressText?.trim().takeUnless { it.isNullOrBlank() } ?: pageUrl
            return BrowserSessionState(
                pageUrl = pageUrl,
                addressText = addressText,
            )
        }
    }
}
