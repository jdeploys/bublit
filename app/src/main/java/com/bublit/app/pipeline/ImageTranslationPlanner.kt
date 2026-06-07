package com.bublit.app.pipeline

import com.bublit.app.domain.OcrTextBlock
import com.bublit.app.domain.ScriptDetector
import com.bublit.app.domain.SourceLanguage
import com.bublit.app.domain.SpeechBubbleClassifier
import com.bublit.app.domain.TextRenderPlan
import com.bublit.app.domain.TypesetPlanner

class ImageTranslationPlanner(
    private val classifier: SpeechBubbleClassifier = SpeechBubbleClassifier(),
    private val scriptDetector: ScriptDetector = ScriptDetector(),
    private val typesetPlanner: TypesetPlanner = TypesetPlanner(),
    private val translator: (String, SourceLanguage) -> String,
) {
    fun plan(blocks: List<OcrTextBlock>): ImageTranslationPlan {
        val accepted = classifier.acceptedBubbleTexts(blocks, scriptDetector)
        val typesetBlocks = accepted.map { bubble ->
            val translatedText = translator(bubble.originalText, bubble.sourceLanguage)
            TypesetBlock(
                sourceText = bubble.originalText,
                translatedText = translatedText,
                sourceLanguage = bubble.sourceLanguage,
                renderPlan = typesetPlanner.plan(bubble, translatedText),
            )
        }

        return ImageTranslationPlan(
            blocks = typesetBlocks,
            rejectedBlocks = blocks.size - accepted.size,
        )
    }
}

data class ImageTranslationPlan(
    val blocks: List<TypesetBlock>,
    val rejectedBlocks: Int,
)

data class TypesetBlock(
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: SourceLanguage,
    val renderPlan: TextRenderPlan,
)
