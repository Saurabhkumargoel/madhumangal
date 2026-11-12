package com.madhumangal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.madhumangal.data.ActionPosition
import com.madhumangal.data.ActionSize
import com.madhumangal.data.BotActionType
import com.madhumangal.data.BotCustomAction
import com.madhumangal.ui.theme.DarkBackground
import com.madhumangal.ui.theme.PrimaryBlue
import com.madhumangal.ui.theme.SurfaceDark
import com.madhumangal.ui.theme.SurfaceVariantDark
import com.madhumangal.ui.theme.TextMuted
import com.madhumangal.ui.theme.TextPrimary
import com.madhumangal.ui.theme.TextSecondary
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MadhuStudioDialog(
    initialActions: List<BotCustomAction>,
    onSaveActions: (List<BotCustomAction>) -> Unit,
    onDismiss: () -> Unit
) {
    var actionsList by remember { mutableStateOf(initialActions) }
    var isAddingNew by remember { mutableStateOf(false) }

    // New action form state
    var editingId by remember { mutableStateOf<String?>(null) }
    var actionLabel by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BotActionType.SPEAK_TIME) }
    var customPrompt by remember { mutableStateOf("") }
    var selectedPosition by remember { mutableStateOf(ActionPosition.QUICK_ACTIONS) }
    var selectedSize by remember { mutableStateOf(ActionSize.MEDIUM) }
    var selectedColor by remember { mutableStateOf("#3B82F6") }
    var selectedIcon by remember { mutableStateOf("Clock") }

    val colorPalette = listOf(
        "#3B82F6", // Blue
        "#8B5CF6", // Purple
        "#10B981", // Emerald
        "#F59E0B", // Amber
        "#EC4899", // Pink
        "#06B6D4"  // Cyan
    )

    val iconOptions = listOf("Clock", "Date", "Battery", "Mic", "Sparkles", "Bolt", "Star")

    fun startAddNew() {
        editingId = null
        actionLabel = ""
        selectedType = BotActionType.SPEAK_TIME
        customPrompt = ""
        selectedPosition = ActionPosition.QUICK_ACTIONS
        selectedSize = ActionSize.MEDIUM
        selectedColor = "#3B82F6"
        selectedIcon = "Clock"
        isAddingNew = true
    }

    fun startEdit(action: BotCustomAction) {
        editingId = action.id
        actionLabel = action.label
        selectedType = action.actionType
        customPrompt = action.promptText
        selectedPosition = action.position
        selectedSize = action.size
        selectedColor = action.colorHex
        selectedIcon = action.iconName
        isAddingNew = true
    }

    fun saveCurrentForm() {
        val label = if (actionLabel.isNotBlank()) actionLabel.trim() else selectedType.title
        val newAction = BotCustomAction(
            id = editingId ?: UUID.randomUUID().toString(),
            label = label,
            actionType = selectedType,
            promptText = customPrompt.trim(),
            position = selectedPosition,
            size = selectedSize,
            colorHex = selectedColor,
            iconName = selectedIcon
        )

        val updated = if (editingId != null) {
            actionsList.map { if (it.id == editingId) newAction else it }
        } else {
            actionsList + newAction
        }
        actionsList = updated
        onSaveActions(updated)
        isAddingNew = false
        editingId = null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(24.dp),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f)),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Madhu Action Studio",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Developer UI & Action Customizer",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isAddingNew) {
                    // Form View
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = if (editingId != null) "Edit Custom Action" else "Create New Bot Action",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Label
                        OutlinedTextField(
                            value = actionLabel,
                            onValueChange = { actionLabel = it },
                            label = { Text("Button Title", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Current Time, Battery...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Type
                        Text("Action Behavior:", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BotActionType.values().forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = {
                                        selectedType = type
                                        if (actionLabel.isBlank()) {
                                            actionLabel = type.title
                                        }
                                    },
                                    label = { Text(type.title, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.25f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        }

                        if (selectedType == BotActionType.ASK_AI_PROMPT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customPrompt,
                                onValueChange = { customPrompt = it },
                                label = { Text("AI Prompt Query", fontSize = 12.sp) },
                                placeholder = { Text("e.g. Give me a 1-sentence motivation", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Screen Placement
                        Text("Screen Position:", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionPosition.values().forEach { pos ->
                                FilterChip(
                                    selected = selectedPosition == pos,
                                    onClick = { selectedPosition = pos },
                                    label = { Text(pos.title.substringBefore(" "), fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.25f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button Size
                        Text("Button Size:", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionSize.values().forEach { size ->
                                FilterChip(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    label = { Text(size.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.25f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Accent Color
                        Text("Accent Color:", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            colorPalette.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColor = hex }
                                        .border(
                                            width = if (selectedColor == hex) 2.dp else 0.dp,
                                            color = if (selectedColor == hex) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColor == hex) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isAddingNew = false }) {
                                Text("Cancel", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { saveCurrentForm() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Save Action", color = Color.White)
                            }
                        }
                    }
                } else {
                    // Actions List View
                    if (actionsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No custom bot actions added yet.", fontSize = 13.sp, color = TextMuted)
                                Text("Tap '+ Add Action' below to build one!", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(actionsList) { action ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val actionColor = try {
                                                Color(android.graphics.Color.parseColor(action.colorHex))
                                            } catch (_: Exception) { PrimaryBlue }

                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(actionColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getActionIcon(action.actionType),
                                                    contentDescription = null,
                                                    tint = actionColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = action.label,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${action.position.title} • ${action.size.name}",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(onClick = { startEdit(action) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    val updated = actionsList.filter { it.id != action.id }
                                                    actionsList = updated
                                                    onSaveActions(updated)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Add Button
                    Button(
                        onClick = { startAddNew() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Custom Action", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun getActionIcon(type: BotActionType): ImageVector {
    return when (type) {
        BotActionType.SPEAK_TIME -> Icons.Default.Schedule
        BotActionType.SPEAK_DATE -> Icons.Default.Today
        BotActionType.SPEAK_BATTERY -> Icons.Default.BatteryStd
        BotActionType.WAKE_BOT -> Icons.Default.Mic
        BotActionType.TOGGLE_RECORD -> Icons.Default.RadioButtonChecked
        BotActionType.ASK_AI_PROMPT -> Icons.Default.Psychology
        BotActionType.CLEAR_TRANSCRIPT -> Icons.Default.Clear
    }
}
