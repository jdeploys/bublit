package com.bublit.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypesetPlannerTest {
    private val planner = TypesetPlanner()

    @Test
    fun koreanTextRenderPlansFitInsideAcceptedBubbleBounds() {
        val bubble = AcceptedBubbleText(
            originalText = "Please wait here until I come back.",
            bounds = BubbleBounds(left = 48, top = 80, width = 180, height = 72),
            sourceLanguage = SourceLanguage.English,
        )

        val plan = planner.plan(
            bubble = bubble,
            translatedText = "내가 돌아올 때까지 여기서 기다려 줘.",
        )

        assertEquals(bubble.bounds, plan.bounds)
        assertTrue(plan.lines.size > 1)
        assertTrue(plan.fontSizePx >= 10)
        assertTrue(plan.estimatedWidthPx <= bubble.bounds.width)
        assertTrue(plan.estimatedHeightPx <= bubble.bounds.height)
    }
}
