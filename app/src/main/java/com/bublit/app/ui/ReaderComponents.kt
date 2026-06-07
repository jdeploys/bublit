package com.bublit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

enum class ReaderMode(
    val label: String,
    val helperText: String,
) {
    Continuous("Reader", "Continuous vertical reading"),
    Focus("Focus", "One page at a time"),
}

enum class ImageProcessingStage(
    val label: String,
    val progress: Float,
) {
    Queued("Queued", 0.18f),
    ExtractingText("OCR", 0.48f),
    Translating("Translating", 0.76f),
    Ready("Ready", 1f),
}

data class ReaderImageItem(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val imageUrl: String? = null,
    val originalCaption: String,
    val translatedCaption: String,
    val stage: ImageProcessingStage,
    val paletteColor: Long,
)

@Composable
fun ExtractionStatusCard(
    status: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Extraction status",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ReaderModeSelector(
    selectedMode: ReaderMode,
    onModeSelected: (ReaderMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Reader mode",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
        Text(
            text = selectedMode.helperText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TranslationToggle(
    showTranslated: Boolean,
    onShowTranslatedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (showTranslated) "Translated" else "Original",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Toggle source artwork or Korean overlay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = showTranslated,
            onCheckedChange = onShowTranslatedChange,
        )
    }
}

@Composable
fun ReaderImageCard(
    item: ReaderImageItem,
    showTranslated: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.sourceUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(item.stage.label) },
                )
            }

            ComicImagePreview(
                item = item,
                showTranslated = showTranslated,
            )

            LinearProgressIndicator(
                progress = { item.stage.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ComicImagePreview(
    item: ReaderImageItem,
    showTranslated: Boolean,
    modifier: Modifier = Modifier,
) {
    if (item.imageUrl.isNullOrBlank()) {
        ComicImagePlaceholder(
            item = item,
            showTranslated = showTranslated,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth(),
            )

            if (showTranslated) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = item.translatedCaption,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicImagePlaceholder(
    item: ReaderImageItem,
    showTranslated: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseColor = Color(item.paletteColor)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(6.dp))
            .background(baseColor.copy(alpha = 0.26f))
            .padding(18.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = 118.dp, height = 76.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (showTranslated) item.translatedCaption else item.originalCaption,
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 180.dp, height = 210.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(baseColor.copy(alpha = 0.42f)),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 126.dp, height = 92.dp)
                .clip(RoundedCornerShape(42.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (showTranslated) "Korean panel text" else "Original panel text",
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomStart),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.34f),
            contentColor = Color.White,
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = if (showTranslated) "Korean overlay preview" else "Original image preview",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))
}
