package com.bublit.app.ui

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserScreenSourceTest {
    @Test
    fun ocrLanguageBottomSheetShowsOneLanguagePerLine() {
        val source = browserScreenSource()
        val bottomSheetSource = source.substringAfter("private fun PreferredOcrLanguageBottomSheet")

        assertFalse(bottomSheetSource.contains("FlowRow("))
        assertTrue(bottomSheetSource.contains(".fillMaxWidth()"))
        assertTrue(bottomSheetSource.contains(".height(42.dp)"))
    }

    @Test
    fun ocrLanguageTopBarChipKeepsCompactWidth() {
        val source = browserScreenSource()
        val chipSource = source.substringAfter("private fun PreferredOcrLanguageChip")
            .substringBefore("@OptIn")

        assertTrue(chipSource.contains(".width(44.dp)"))
        assertTrue(chipSource.contains(".height(30.dp)"))
    }

    private fun browserScreenSource(): String {
        return String(
            Files.readAllBytes(Paths.get("src/main/java/com/bublit/app/ui/BrowserScreen.kt")),
        )
    }
}
