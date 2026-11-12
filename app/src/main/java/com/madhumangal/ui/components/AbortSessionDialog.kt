package com.madhumangal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhumangal.data.AudioSegment
import com.madhumangal.ui.theme.ActiveRed
import com.madhumangal.ui.theme.CardBorder
import com.madhumangal.ui.theme.PrimaryBlue
import com.madhumangal.ui.theme.SurfaceDark
import com.madhumangal.ui.theme.TextMuted
import com.madhumangal.ui.theme.TextPrimary
import com.madhumangal.ui.theme.TextSecondary
import java.io.File

@Composable
fun AbortSessionDialog(
    segments: List<AudioSegment>,
    defaultSelectionMode: String = "ALL_FILES", // "ALL_FILES" or "LAST_FILE_ONLY"
    onConfirmAbort: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
    isHindi: Boolean = false
) {
    var selectedIds by remember(segments, defaultSelectionMode) {
        mutableStateOf(
            when {
                segments.isEmpty() -> emptySet()
                defaultSelectionMode == "LAST_FILE_ONLY" -> setOf(segments.last().id)
                else -> segments.map { it.id }.toSet()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isHindi) "🚫 रिकॉर्डिंग सत्र निरस्त करें?" else "🚫 Abort Recording Session?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isHindi)
                        "रिकॉर्डिंग तुरंत रोक दी जाएगी। चुनें कि कौन-सी फ़ाइलें हटानी हैं:"
                    else
                        "Recording will stop immediately. Choose which recorded audio files and their notes/transcriptions to delete:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                if (segments.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isHindi)
                                "वर्तमान चक्र में अभी कोई ऑडियो खंड डिस्क पर नहीं लिखा गया है। मेमोरी बफ़र तुरंत साफ़ हो जाएगा।"
                            else
                                "No audio chunks have been saved to disk yet in this session. The live buffer will be discarded cleanly.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    // Quick selection actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Select All
                        OutlinedButton(
                            onClick = { selectedIds = segments.map { it.id }.toSet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(if (isHindi) "सभी" else "All", fontSize = 10.sp, color = PrimaryBlue)
                        }

                        // Deselect All
                        OutlinedButton(
                            onClick = { selectedIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(if (isHindi) "कोई नहीं" else "None", fontSize = 10.sp, color = TextMuted)
                        }

                        // Invert Selection
                        OutlinedButton(
                            onClick = {
                                val all = segments.map { it.id }.toSet()
                                selectedIds = all - selectedIds
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(if (isHindi) "उलटा" else "Invert", fontSize = 10.sp, color = Color(0xFFF59E0B))
                        }

                        // Select Last
                        OutlinedButton(
                            onClick = {
                                selectedIds = segments.lastOrNull()?.let { setOf(it.id) } ?: emptySet()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp),
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(if (isHindi) "अंतिम" else "Last", fontSize = 10.sp, color = PrimaryBlue)
                        }
                    }

                    // Chunks list
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            segments.forEachIndexed { index, seg ->
                                val isChecked = seg.id in selectedIds
                                val fileName = File(seg.filePath).name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (isChecked) selectedIds - seg.id else selectedIds + seg.id
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { chk ->
                                            selectedIds = if (chk) selectedIds + seg.id else selectedIds - seg.id
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = ActiveRed),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "#${index + 1}: $fileName",
                                        fontSize = 11.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChecked) ActiveRed else TextPrimary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${seg.durationSeconds}s",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmAbort(selectedIds) },
                colors = ButtonDefaults.buttonColors(containerColor = ActiveRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (segments.isEmpty()) {
                        if (isHindi) "सत्र बंद करें" else "Abort Session"
                    } else {
                        if (isHindi) "निरस्त करें (${selectedIds.size} हटाएं)" else "Abort & Delete (${selectedIds.size})"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}
