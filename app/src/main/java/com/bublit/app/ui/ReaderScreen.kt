package com.bublit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    sourceUrl: String,
    extractionStatus: String,
    readerMode: ReaderMode,
    showTranslated: Boolean,
    items: List<ReaderImageItem>,
    focusedIndex: Int,
    onBackClick: () -> Unit,
    onReaderModeChange: (ReaderMode) -> Unit,
    onShowTranslatedChange: (Boolean) -> Unit,
    onPreviousImageClick: () -> Unit,
    onNextImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleItems = when (readerMode) {
        ReaderMode.Continuous -> items
        ReaderMode.Focus -> items.getOrNull(focusedIndex)?.let(::listOf).orEmpty()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reader",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = sourceUrl,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExtractionStatusCard(status = extractionStatus)
                    ReaderModeSelector(
                        selectedMode = readerMode,
                        onModeSelected = onReaderModeChange,
                    )
                    TranslationToggle(
                        showTranslated = showTranslated,
                        onShowTranslatedChange = onShowTranslatedChange,
                    )
                    if (readerMode == ReaderMode.Focus) {
                        FocusControls(
                            focusedIndex = focusedIndex,
                            itemCount = items.size,
                            onPreviousImageClick = onPreviousImageClick,
                            onNextImageClick = onNextImageClick,
                        )
                    }
                }
            }

            items(
                items = visibleItems,
                key = { it.id },
            ) { item ->
                ReaderImageCard(
                    item = item,
                    showTranslated = showTranslated,
                )
            }
        }
    }
}

@Composable
private fun FocusControls(
    focusedIndex: Int,
    itemCount: Int,
    onPreviousImageClick: () -> Unit,
    onNextImageClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onPreviousImageClick,
            enabled = focusedIndex > 0,
            modifier = Modifier.weight(1f),
        ) {
            Text("Previous")
        }
        Button(
            onClick = onNextImageClick,
            enabled = focusedIndex < itemCount - 1,
            modifier = Modifier.weight(1f),
        ) {
            Text("Next ${focusedIndex + 1}/$itemCount")
        }
    }
}
