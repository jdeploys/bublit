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

        assertTrue(plan.bounds.left <= bubble.bounds.left)
        assertTrue(plan.bounds.top <= bubble.bounds.top)
        assertTrue(plan.bounds.right >= bubble.bounds.right)
        assertTrue(plan.bounds.bottom >= bubble.bounds.bottom)
        assertTrue(plan.lines.size > 1)
        assertTrue(plan.fontSizePx >= 10)
        assertTrue(plan.estimatedWidthPx <= plan.bounds.width)
        assertTrue(plan.estimatedHeightPx <= plan.bounds.height)
    }

    @Test
    fun renderBoundsExpandBeyondRawOcrTextBounds() {
        val bubble = AcceptedBubbleText(
            originalText = "Wait.",
            bounds = BubbleBounds(left = 100, top = 120, width = 80, height = 24),
            sourceLanguage = SourceLanguage.English,
        )

        val plan = planner.plan(
            bubble = bubble,
            translatedText = "잠깐.",
        )

        assertTrue(plan.bounds.left < bubble.bounds.left)
        assertTrue(plan.bounds.top < bubble.bounds.top)
        assertTrue(plan.bounds.width > bubble.bounds.width)
        assertTrue(plan.bounds.height > bubble.bounds.height)
    }
}
