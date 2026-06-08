package com.bublit.app.pipeline

import com.bublit.app.domain.OcrTextBlock
import com.bublit.app.domain.ScriptDetector
import com.bublit.app.domain.SourceLanguage
import com.bublit.app.domain.SpeechBubbleClassifier
import com.bublit.app.domain.SpeechBubbleRejectionReason
import com.bublit.app.domain.TextRenderPlan
import com.bublit.app.domain.TypesetPlanner

class ImageTranslationPlanner(
    private val classifier: SpeechBubbleClassifier = SpeechBubbleClassifier(),
    private val scriptDetector: ScriptDetector = ScriptDetector(),
    private val typesetPlanner: TypesetPlanner = TypesetPlanner(),
    private val translator: (String, SourceLanguage) -> String,
) {
    fun plan(blocks: List<OcrTextBlock>): ImageTranslationPlan {
        val classification = classifier.classifyBlocks(blocks, scriptDetector)
        val typesetBlocks = classification.acceptedBubbleTexts.map { bubble ->
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
            rejectedBlocks = classification.rejectedBlocks,
            rejectionReasonCounts = classification.rejectionReasonCounts,
        )
    }
}

data class ImageTranslationPlan(
    val blocks: List<TypesetBlock>,
    val rejectedBlocks: Int,
    val rejectionReasonCounts: Map<SpeechBubbleRejectionReason, Int> = emptyMap(),
)

data class TypesetBlock(
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: SourceLanguage,
    val renderPlan: TextRenderPlan,
)
