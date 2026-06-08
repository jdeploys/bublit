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

    @Test
    fun detectedBubbleRegionProvidesHorizontalKoreanTypesettingSpaceForVerticalTextSeed() {
        val bubble = AcceptedBubbleText(
            originalText = "恥ずかしすぎるだろー",
            bounds = BubbleBounds(left = 142, top = 88, width = 24, height = 112),
            sourceLanguage = SourceLanguage.Japanese,
            boundsSource = AcceptedBubbleBoundsSource.DetectedBubbleRegion,
            containingBounds = BubbleBounds(left = 108, top = 54, width = 96, height = 174),
        )

        val plan = planner.plan(
            bubble = bubble,
            translatedText = "너무 부끄럽잖아",
        )

        assertTrue(plan.bounds.width >= 72)
        assertTrue(plan.bounds.width > bubble.bounds.width * 2)
        assertTrue(plan.bounds.left >= bubble.containingBounds!!.left)
        assertTrue(plan.bounds.right <= bubble.containingBounds.right)
        assertTrue(plan.bounds.top >= bubble.containingBounds.top)
        assertTrue(plan.bounds.bottom <= bubble.containingBounds.bottom)
    }

    @Test
    fun detectedBubbleRegionSeparatesPatchBoundsFromInsetTextBounds() {
        val containingBounds = BubbleBounds(left = 108, top = 54, width = 126, height = 174)
        val bubble = AcceptedBubbleText(
            originalText = "そのステッキを\nかまえて",
            bounds = BubbleBounds(left = 142, top = 88, width = 46, height = 112),
            sourceLanguage = SourceLanguage.Japanese,
            boundsSource = AcceptedBubbleBoundsSource.DetectedBubbleRegion,
            containingBounds = containingBounds,
        )

        val plan = planner.plan(
            bubble = bubble,
            translatedText = "그 지팡이를 들어",
        )

        assertEquals(containingBounds, plan.patchBounds)
        assertTrue(plan.bounds.left > plan.patchBounds.left)
        assertTrue(plan.bounds.top > plan.patchBounds.top)
        assertTrue(plan.bounds.right < plan.patchBounds.right)
        assertTrue(plan.bounds.bottom < plan.patchBounds.bottom)
        assertTrue(plan.estimatedWidthPx <= plan.bounds.width)
        assertTrue(plan.estimatedHeightPx <= plan.bounds.height)
    }

    @Test
    fun oversizedDetectedRegionIsConstrainedAroundVerticalTextSeed() {
        val bubble = AcceptedBubbleText(
            originalText = "そのステッキを\nかまえて",
            bounds = BubbleBounds(left = 132, top = 48, width = 46, height = 92),
            sourceLanguage = SourceLanguage.Japanese,
            boundsSource = AcceptedBubbleBoundsSource.DetectedBubbleRegion,
            containingBounds = BubbleBounds(left = 0, top = 0, width = 320, height = 640),
        )

        val plan = planner.plan(
            bubble = bubble,
            translatedText = "그 지팡이를 들고",
        )

        assertTrue(plan.patchBounds.width <= 140)
        assertTrue(plan.patchBounds.height <= 190)
        assertTrue(plan.patchBounds.left <= bubble.bounds.left)
        assertTrue(plan.patchBounds.right >= bubble.bounds.right)
        assertTrue(plan.patchBounds.top <= bubble.bounds.top)
        assertTrue(plan.patchBounds.bottom >= bubble.bounds.bottom)
        assertTrue(plan.bounds.left > plan.patchBounds.left)
        assertTrue(plan.bounds.right < plan.patchBounds.right)
    }
}
