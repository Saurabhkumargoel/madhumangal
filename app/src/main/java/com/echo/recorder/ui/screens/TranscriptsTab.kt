package com.echo.recorder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.echo.recorder.data.TranscriptionWithAudio
import com.echo.recorder.ui.components.AudioPlayerCard
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import com.echo.recorder.data.UserPreferences

@Composable
fun TranscriptsTab(
    transcriptions: List<TranscriptionWithAudio>,
    transcribingIds: Set<Long> = emptySet(),
    processingAiIds: Set<Long> = emptySet(),
    defaultSttModel: String = UserPreferences.STT_MODEL_VOSK_HINDI,
    defaultAiModel: String = UserPreferences.AI_MODEL_LOCAL_TINY,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeleteTranscription: (Long) -> Unit,
    onRetranscribe: (Long) -> Unit = {},
    onRetranscribeWithModel: (Long, String) -> Unit = { _, _ -> },
    onReprocessAiWithModel: (Long, String) -> Unit = { _, _ -> },
    onUpdateText: (Long, String) -> Unit = { _, _ -> },
    onExportMedia: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Recordings & Media",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "All audio chunks, speech transcriptions & saved markdown files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onExportMedia,
                modifier = Modifier
                    .size(42.dp)
                    .background(PrimaryBlue.copy(alpha = 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.DriveFileMove,
                    contentDescription = "Export All Media",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search transcriptions...", color = TextMuted) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PrimaryBlue)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (transcriptions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isBlank()) "No transcriptions yet" else "No matching transcriptions found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Start listening continuously to generate automatic transcriptions." else "Try searching for a different keyword.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = transcriptions,
                    key = { it.id }
                ) { item ->
                    AudioPlayerCard(
                        transcription = item,
                        isTranscribing = transcribingIds.contains(item.id),
                        isProcessingAi = processingAiIds.contains(item.id),
                        defaultSttModel = defaultSttModel,
                        defaultAiModel = defaultAiModel,
                        onDelete = onDeleteTranscription,
                        onRetranscribe = onRetranscribe,
                        onRetranscribeWithModel = onRetranscribeWithModel,
                        onReprocessAiWithModel = onReprocessAiWithModel,
                        onUpdateText = onUpdateText
                    )
                }
            }
        }
    }
}
