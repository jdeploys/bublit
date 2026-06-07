package com.bublit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bublit.app.domain.ImageCandidate
import com.bublit.app.web.WebPageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLoadingScreen(
    url: String,
    status: String,
    onBackClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onImagesExtracted: (List<ImageCandidate>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Extracting images",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExtractionStatusCard(status = status)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            WebPageLoader(
                url = url,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                onStatusChange = onStatusChange,
                onImagesExtracted = onImagesExtracted,
            )
        }
    }
}
