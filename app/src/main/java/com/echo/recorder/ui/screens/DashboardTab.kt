@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.echo.recorder.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.echo.recorder.data.NoteTask
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import com.echo.recorder.util.AppStrings
import java.util.Locale

enum class TaskSortOption(val label: String) {
    CUSTOM("Custom Order (Draggable)"),
    PRIORITY_DESC("Priority (High → Low)"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    TITLE_ASC("Title (A → Z)"),
    STATUS("Pending First")
}

enum class TaskStatusFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Done")
}

enum class DueHorizonFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    NO_DUE("No Date")
}

data class TaskGroupDefinition(
    val id: String,
    val title: String,
    val icon: String,
    val accentColor: Color,
    val taskFilter: (NoteTask) -> Boolean
)

@Composable
fun DashboardTab(
    tasks: List<NoteTask>,
    lowHangingFruits: List<NoteTask>,
    userPreferences: UserPreferences,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onCreateTask: (String, String, Int, Boolean, String, Long?, Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
    onAddSubtask: (Long, String) -> Unit = { _, _ -> },
    onMoveTaskToCategory: (Long, String) -> Unit = { _, _ -> },
    onTogglePinCategory: (String) -> Unit = {},
    onAddCustomCategory: (String) -> Unit = {},
    onDeleteCustomCategory: (String) -> Unit = {},
    onExportDashboard: (String) -> Unit = {},
    onOpenAiAsk: () -> Unit = {},
    onEditTask: (Long, String, String, Int, Boolean, String) -> Unit = { _, _, _, _, _, _ -> },
    onReorderTasks: (Int, Int, List<NoteTask>) -> Unit = { _, _, _ -> },
    onReorderGroups: (Int, Int, List<String>) -> Unit = { _, _, _ -> },
    onReorderTasksBulk: ((Set<Long>, Boolean, List<NoteTask>) -> Unit)? = null,
    onDeleteTasksBulk: ((Set<Long>) -> Unit)? = null,
    onImportTasksJson: ((String, (Boolean, String) -> Unit) -> Unit)? = null,
    isRecording: Boolean = false,
    elapsedSeconds: Long = 0L,
    onToggleRecording: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi = userPreferences.appLanguage == "hi"
    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 } }

    var selectedMainTab by remember { mutableIntStateOf(0) }
    var selectedSort by remember { mutableStateOf(TaskSortOption.CUSTOM) }
    var selectedStatus by remember { mutableStateOf(TaskStatusFilter.ALL) }
    var selectedHorizon by remember { mutableStateOf(DueHorizonFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var selectedCategoryTag by remember { mutableStateOf("Shopping") }

    // Dialog States
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<NoteTask?>(null) }
    var subtaskParentTask by remember { mutableStateOf<NoteTask?>(null) }
    var moveTargetTask by remember { mutableStateOf<NoteTask?>(null) }

    // Selection Mode & Confirmations
    var isSelectionModeActive by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var taskToDeleteConfirm by remember { mutableStateOf<NoteTask?>(null) }
    var categoryToDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    // Reorder mode & Floating Action Button states
    var isReorderModeActive by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = isSelectionModeActive || isReorderModeActive) {
        if (isSelectionModeActive) {
            isSelectionModeActive = false
            selectedTaskIds = emptySet()
        } else {
            isReorderModeActive = false
        }
    }

    // ── Selection Mode Helper: Cascading Parent-Child Selection & Individual Child Deselection ──
    fun toggleTaskSelection(taskId: Long) {
        val isCurrentlySelected = taskId in selectedTaskIds
        if (isCurrentlySelected) {
            val descendants = mutableSetOf<Long>()
            fun collectDescendants(pid: Long) {
                tasks.filter { it.parentId == pid }.forEach { child ->
                    descendants.add(child.id)
                    collectDescendants(child.id)
                }
            }
            collectDescendants(taskId)
            selectedTaskIds = selectedTaskIds - taskId - descendants
            if (selectedTaskIds.isEmpty()) {
                isSelectionModeActive = false
            }
        } else {
            val toAdd = mutableSetOf(taskId)
            fun collectDescendants(pid: Long) {
                tasks.filter { it.parentId == pid }.forEach { child ->
                    toAdd.add(child.id)
                    collectDescendants(child.id)
                }
            }
            collectDescendants(taskId)
            selectedTaskIds = selectedTaskIds + toAdd
            isSelectionModeActive = true
        }
    }

    // ── Bulk Move Helpers: Check if items are already packed at boundaries ──
    fun canBulkMoveUp(selectedIds: Set<Long>, siblings: List<NoteTask>): Boolean {
        if (selectedIds.isEmpty()) return false
        val indices = siblings.mapIndexedNotNull { idx, task -> if (task.id in selectedIds) idx else null }
        if (indices.size != selectedIds.size) return false
        val isPackedAtTop = indices.mapIndexed { i, idx -> idx == i }.all { it }
        return !isPackedAtTop
    }

    fun canBulkMoveDown(selectedIds: Set<Long>, siblings: List<NoteTask>): Boolean {
        if (selectedIds.isEmpty()) return false
        val indices = siblings.mapIndexedNotNull { idx, task -> if (task.id in selectedIds) idx else null }
        if (indices.size != selectedIds.size) return false
        val n = siblings.size
        val k = indices.size
        val isPackedAtBottom = indices.mapIndexed { i, idx -> idx == n - k + i }.all { it }
        return !isPackedAtBottom
    }

    // ── Export Selected Tasks (.md, .txt, .json, .html, copy) ──
    fun exportSelectedTasks(format: String) {
        val selectedList = tasks.filter { it.id in selectedTaskIds }
        if (selectedList.isEmpty()) return
        val topSelected = selectedList.filter { it.parentId == null || it.parentId !in selectedTaskIds }

        when (format) {
            "copy" -> {
                val sb = StringBuilder()
                topSelected.forEach { parent ->
                    sb.append(if (parent.isCompleted) "[x] " else "[ ] ").append(parent.title).append("\n")
                    if (parent.description.isNotBlank()) sb.append("    ").append(parent.description).append("\n")
                    selectedList.filter { it.parentId == parent.id }.forEach { sub ->
                        sb.append("    - ").append(if (sub.isCompleted) "[x] " else "[ ] ").append(sub.title).append("\n")
                    }
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Selected Tasks", sb.toString()))
                Toast.makeText(context, if (isHindi) "${selectedList.size} कार्य कॉपी किए गए!" else "Copied ${selectedList.size} task(s)!", Toast.LENGTH_SHORT).show()
            }
            "md" -> {
                val sb = StringBuilder("# Echo Tasks Export\n\n")
                topSelected.forEach { parent ->
                    sb.append("- [").append(if (parent.isCompleted) "x" else " ").append("] **").append(parent.title).append("**")
                    if (parent.categoryGroup.isNotBlank()) sb.append(" `").append(parent.categoryGroup).append("`")
                    sb.append("\n")
                    if (parent.description.isNotBlank()) sb.append("  > ").append(parent.description).append("\n")
                    selectedList.filter { it.parentId == parent.id }.forEach { sub ->
                        sb.append("  - [").append(if (sub.isCompleted) "x" else " ").append("] ").append(sub.title).append("\n")
                    }
                }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                    type = "text/markdown"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export Tasks (.md)"))
            }
            "txt" -> {
                val sb = StringBuilder("Echo Tasks Export\n=================\n\n")
                topSelected.forEach { parent ->
                    sb.append(if (parent.isCompleted) "[x] " else "[ ] ").append(parent.title)
                    sb.append(" (Priority: ").append(when (parent.priority) { 3 -> "High"; 1 -> "Low"; else -> "Medium" })
                    sb.append(", Category: ").append(parent.categoryGroup).append(")\n")
                    if (parent.description.isNotBlank()) sb.append("    Note: ").append(parent.description).append("\n")
                    selectedList.filter { it.parentId == parent.id }.forEach { sub ->
                        sb.append("    - ").append(if (sub.isCompleted) "[x] " else "[ ] ").append(sub.title).append("\n")
                    }
                }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export Tasks (.txt)"))
            }
            "json" -> {
                val arr = JSONArray()
                topSelected.forEach { parent ->
                    val obj = JSONObject().apply {
                        put("id", parent.id)
                        put("title", parent.title)
                        put("description", parent.description)
                        put("category", parent.categoryGroup)
                        put("priority", parent.priority)
                        put("isCompleted", parent.isCompleted)
                        put("isLowHangingFruit", parent.isLowHangingFruit)
                        val subArr = JSONArray()
                        selectedList.filter { it.parentId == parent.id }.forEach { sub ->
                            subArr.put(JSONObject().apply {
                                put("id", sub.id)
                                put("title", sub.title)
                                put("isCompleted", sub.isCompleted)
                            })
                        }
                        if (subArr.length() > 0) put("subtasks", subArr)
                    }
                    arr.put(obj)
                }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, arr.toString(2))
                    type = "application/json"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export Tasks (.json)"))
            }
            "html" -> {
                val sb = StringBuilder()
                sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Echo Tasks Export</title>")
                sb.append("<style>body{font-family:sans-serif;background:#0F172A;color:#F8FAFC;padding:24px;}")
                sb.append(".task{background:#1E293B;padding:12px;margin-bottom:8px;border-radius:8px;}")
                sb.append(".done{text-decoration:line-through;color:#94A3B8;}")
                sb.append("</style></head><body><h1>Echo Tasks Export</h1><ul>")
                topSelected.forEach { parent ->
                    sb.append("<li class='task ").append(if (parent.isCompleted) "done" else "").append("'>")
                    sb.append("<strong>").append(parent.title).append("</strong>")
                    if (parent.description.isNotBlank()) sb.append("<p>").append(parent.description).append("</p>")
                    val subs = selectedList.filter { it.parentId == parent.id }
                    if (subs.isNotEmpty()) {
                        sb.append("<ul>")
                        subs.forEach { sub ->
                            sb.append("<li class='").append(if (sub.isCompleted) "done" else "").append("'>").append(sub.title).append("</li>")
                        }
                        sb.append("</ul>")
                    }
                    sb.append("</li>")
                }
                sb.append("</ul></body></html>")
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                    type = "text/html"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export Tasks (.html)"))
            }
        }
    }

    // Expand/Collapse state map for group cards
    val expandedGroups = remember {
        mutableStateMapOf(
            "urgent" to true,
            "quick_wins" to true,
            "shopping" to true,
            "work" to true,
            "personal" to true,
            "ideas" to true
        )
    }

    val timerMinutes = elapsedSeconds / 60
    val timerSeconds = elapsedSeconds % 60
    val timerStr = String.format(Locale.US, "%02d:%02d", timerMinutes, timerSeconds)

    // Calculate smart summary stats
    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val pendingCount = totalCount - completedCount
    val urgentCount = tasks.count { (it.priority == NoteTask.PRIORITY_HIGH || it.categoryGroup.equals("Urgent", ignoreCase = true)) && !it.isCompleted }
    val easyWinsCount = tasks.count { it.isLowHangingFruit && !it.isCompleted }
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    // Master list of all available categories (Built-in + Custom + Dynamic from tasks)
    val baseCategories = listOf("Shopping", "Work", "Personal", "Urgent", "Ideas")
    val allCategories = remember(userPreferences.customCategories, tasks) {
        val set = linkedSetOf<String>()
        // Pinned categories first!
        userPreferences.pinnedCategories.forEach { set.add(it) }
        baseCategories.forEach { set.add(it) }
        userPreferences.customCategories.forEach { set.add(it) }
        tasks.map { it.categoryGroup }.forEach { if (it.isNotBlank()) set.add(it) }
        set.toList()
    }

    // Helper to filter and sort top-level task lists
    fun processTaskList(rawList: List<NoteTask>): List<NoteTask> {
        var filtered = rawList

        // Status filter
        filtered = when (selectedStatus) {
            TaskStatusFilter.ALL -> filtered
            TaskStatusFilter.PENDING -> filtered.filter { !it.isCompleted }
            TaskStatusFilter.COMPLETED -> filtered.filter { it.isCompleted }
        }

        // Horizon filter
        filtered = when (selectedHorizon) {
            DueHorizonFilter.ALL -> filtered
            DueHorizonFilter.TODAY -> filtered.filter {
                it.dueTimeText.contains("Today", ignoreCase = true) || it.dueTimeText.contains("Evening", ignoreCase = true)
            }
            DueHorizonFilter.UPCOMING -> filtered.filter {
                it.dueTimeText.isNotBlank() && !it.dueTimeText.contains("Today", ignoreCase = true)
            }
            DueHorizonFilter.NO_DUE -> filtered.filter { it.dueTimeText.isBlank() }
        }

        // Search filter
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase(Locale.ROOT)
            filtered = filtered.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                it.description.lowercase(Locale.ROOT).contains(q) ||
                it.categoryGroup.lowercase(Locale.ROOT).contains(q)
            }
        }

        // Sorting
        return when (selectedSort) {
            TaskSortOption.CUSTOM -> filtered.sortedWith(
                compareBy<NoteTask> { it.orderIndex }
                    .thenByDescending { it.timestampMillis }
            )
            TaskSortOption.PRIORITY_DESC -> filtered.sortedWith(
                compareBy<NoteTask> { it.isCompleted }
                    .thenByDescending { it.priority }
                    .thenByDescending { it.timestampMillis }
            )
            TaskSortOption.DATE_DESC -> filtered.sortedByDescending { it.timestampMillis }
            TaskSortOption.DATE_ASC -> filtered.sortedBy { it.timestampMillis }
            TaskSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase(Locale.ROOT) }
            TaskSortOption.STATUS -> filtered.sortedWith(
                compareBy<NoteTask> { it.isCompleted }
                    .thenByDescending { it.priority }
            )
        }
    }

    // Dynamic Task Group Definitions (including pinned & custom groups)
    val taskGroups = remember(userPreferences.pinnedCategories, userPreferences.customCategories, isHindi) {
        val list = mutableListOf<TaskGroupDefinition>()

        // 1. Pinned Custom Categories
        userPreferences.pinnedCategories.forEach { pinnedCat ->
            if (pinnedCat !in listOf("Urgent", "Shopping", "Work", "Personal", "Ideas")) {
                list.add(
                    TaskGroupDefinition(
                        id = "pinned_$pinnedCat",
                        title = "📌 $pinnedCat",
                        icon = "⭐",
                        accentColor = AccentPurple,
                        taskFilter = { it.categoryGroup.equals(pinnedCat, ignoreCase = true) }
                    )
                )
            }
        }

        // 2. Built-in Core Groups (Localized)
        list.add(
            TaskGroupDefinition(
                id = "urgent",
                title = if (isHindi) AppStrings.get("cat_urgent", true) else "Urgent & High Priority",
                icon = "🚨",
                accentColor = ActiveRed,
                taskFilter = { (it.priority == NoteTask.PRIORITY_HIGH || it.categoryGroup.equals("Urgent", ignoreCase = true) || it.categoryGroup.contains("ज़रूरी", ignoreCase = true) || it.categoryGroup.contains("आवश्यक", ignoreCase = true)) }
            )
        )
        list.add(
            TaskGroupDefinition(
                id = "quick_wins",
                title = if (isHindi) AppStrings.get("cat_quick_wins", true) else "Low-Hanging Fruits (Easy Wins)",
                icon = "⚡",
                accentColor = Color(0xFFF59E0B),
                taskFilter = { it.isLowHangingFruit }
            )
        )
        list.add(
            TaskGroupDefinition(
                id = "shopping",
                title = if (isHindi) AppStrings.get("cat_shopping", true) else "Shopping & Vegetables",
                icon = "🛒",
                accentColor = Color(0xFF10B981),
                taskFilter = { it.categoryGroup.equals("Shopping", ignoreCase = true) || it.categoryGroup.contains("सब्जी", ignoreCase = true) || it.categoryGroup.contains("खरीदारी", ignoreCase = true) }
            )
        )
        list.add(
            TaskGroupDefinition(
                id = "work",
                title = if (isHindi) AppStrings.get("cat_work", true) else "Work & Projects",
                icon = "💼",
                accentColor = PrimaryBlue,
                taskFilter = { it.categoryGroup.equals("Work", ignoreCase = true) || it.categoryGroup.contains("काम", ignoreCase = true) || it.categoryGroup.contains("कार्य", ignoreCase = true) }
            )
        )
        list.add(
            TaskGroupDefinition(
                id = "personal",
                title = if (isHindi) AppStrings.get("cat_personal", true) else "Personal & Family",
                icon = "🏡",
                accentColor = AccentPurple,
                taskFilter = { it.categoryGroup.equals("Personal", ignoreCase = true) || it.categoryGroup.contains("परिवार", ignoreCase = true) || it.categoryGroup.contains("व्यक्तिगत", ignoreCase = true) }
            )
        )
        list.add(
            TaskGroupDefinition(
                id = "ideas",
                title = if (isHindi) AppStrings.get("cat_ideas", true) else "Ideas & Notes",
                icon = "💡",
                accentColor = Color(0xFF06B6D4),
                taskFilter = { it.categoryGroup.equals("Ideas", ignoreCase = true) || it.categoryGroup.equals("General", ignoreCase = true) || it.categoryGroup.contains("विचार", ignoreCase = true) || it.categoryGroup.contains("नोट्स", ignoreCase = true) }
            )
        )

        // 3. Other Custom Categories
        userPreferences.customCategories.forEach { customCat ->
            if (list.none { it.id == "pinned_$customCat" || it.id.equals(customCat, ignoreCase = true) }) {
                list.add(
                    TaskGroupDefinition(
                        id = "custom_$customCat",
                        title = customCat,
                        icon = "🏷️",
                        accentColor = PrimaryBlue,
                        taskFilter = { it.categoryGroup.equals(customCat, ignoreCase = true) }
                    )
                )
            }
        }
        list
    }

    val mainTabs = if (isHindi) {
        listOf(AppStrings.get("tab_grouped", true), AppStrings.get("tab_priority", true), AppStrings.get("tab_easy_wins", true), AppStrings.get("tab_categories", true))
    } else {
        listOf("All (Grouped)", "🚨 Priority", "⚡ Easy Wins", "🏷️ By Category")
    }

    // Filter only top-level tasks for master list (subtasks are rendered under parents)
    val topLevelTasks = tasks.filter { it.parentId == null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // ── 1. Top Header Row: Title & Action Buttons ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title (Collapses cleanly without redundant task count line)
                Text(
                    text = AppStrings.get("dashboard_title", isHindi),
                    style = if (isScrolled) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Top Right Action Buttons: Export Menu and Recorder Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Export Menu
                    var isExportMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { isExportMenuOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStrings.get("export", isHindi), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        DropdownMenu(
                            expanded = isExportMenuOpen,
                            onDismissRequest = { isExportMenuOpen = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as Markdown (.md)", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    isExportMenuOpen = false
                                    onExportDashboard("md")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Webpage (.html)", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    isExportMenuOpen = false
                                    onExportDashboard("html")
                                }
                            )
                        }
                    }

                    // Recorder Pill
                    if (isRecording) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ActiveRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ActiveRed),
                            modifier = Modifier.clickable { onToggleRecording() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔴 $timerStr", color = ActiveRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 2. Smart Overview with Circular Progress Ring (Smooth Shrink) ─
            AnimatedVisibility(
                visible = !isScrolled,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Circular Progress Indicator with % in center
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(62.dp)) {
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 6.dp,
                                color = PrimaryBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Remaining Progress Items Appended in Same Row
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) "$completedCount/$totalCount पूर्ण" else "$completedCount/$totalCount Done",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completedCount > 0) Color(0xFF10B981) else TextPrimary
                                )
                                // Consistent Urgent count shown on expanded!
                                Text(
                                    text = if (isHindi) "🚨 $urgentCount जरूरी" else "🚨 $urgentCount Urgent",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (urgentCount > 0) ActiveRed else TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isHindi) "⚡ $easyWinsCount त्वरित कार्य" else "⚡ $easyWinsCount Quick Wins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (easyWinsCount > 0) Color(0xFFF59E0B) else TextMuted
                                )
                                Text(
                                    text = if (isHindi) "⏳ $pendingCount लंबित" else "⏳ $pendingCount Pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isScrolled,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(bottom = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 3.dp,
                                color = PrimaryBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        val urgentText = if (urgentCount > 0) (if (isHindi) " • 🚨 $urgentCount जरूरी" else " • 🚨 $urgentCount urgent") else ""
                        Text(
                            text = if (isHindi) "${(progressFraction * 100).toInt()}% • $completedCount/$totalCount पूर्ण$urgentText • $pendingCount लंबित"
                                   else "${(progressFraction * 100).toInt()}% • $completedCount/$totalCount done$urgentText • $pendingCount pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Reorder Mode Active Banner
            AnimatedVisibility(visible = isReorderModeActive) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentPurple.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { isReorderModeActive = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHindi) "↕️ पुन: व्यवस्थित मोड • समाप्त करने के लिए टैप करें" else "↕️ Reorder Mode • Tap here or back when done",
                            color = AccentPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHindi) "पूर्ण ✓" else "Done ✓",
                            color = AccentPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 3. Top Navigation Tabs Row ────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedMainTab,
            containerColor = DarkBackground,
            contentColor = PrimaryBlue,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                    color = PrimaryBlue,
                    height = 2.dp
                )
            },
            divider = {}
        ) {
            mainTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedMainTab == index,
                    onClick = { selectedMainTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedMainTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedMainTab == index) PrimaryBlue else TextMuted,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 4. Filtering, Timing Horizons & Search Bar ────────────────────────
        Column {
            // Row 1: Status Chips + Search & Sort Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status filter chips (All, Pending, Done)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TaskStatusFilter.values().forEach { status ->
                        val statusLabel = if (isHindi) {
                            when (status) {
                                TaskStatusFilter.ALL -> "सभी"
                                TaskStatusFilter.PENDING -> "लंबित"
                                TaskStatusFilter.COMPLETED -> "पूर्ण"
                            }
                        } else status.label
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(statusLabel, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = DarkBackground,
                                containerColor = SurfaceDark,
                                labelColor = TextMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Right: Search Toggle & Sort Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HintIconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) searchQuery = ""
                        },
                        hint = if (isHindi) "खोजें" else "Search Tasks",
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchExpanded || searchQuery.isNotBlank()) PrimaryBlue else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Import Tasks JSON button
                    HintIconButton(
                        onClick = { showImportJsonDialog = true },
                        hint = if (isHindi) "कार्य आयात करें (JSON)" else "Import Tasks (JSON)",
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import Tasks (JSON)", tint = AccentPurple, modifier = Modifier.size(16.dp))
                    }

                    Box {
                        HintIconButton(
                            onClick = { isSortMenuOpen = true },
                            hint = if (isHindi) "कार्यों को छांटें" else "Sort Tasks",
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            TaskSortOption.values().forEach { option ->
                                val sortLabel = if (isHindi) {
                                    when (option) {
                                        TaskSortOption.CUSTOM -> "कस्टम क्रम (खींचें)"
                                        TaskSortOption.PRIORITY_DESC -> "प्राथमिकता (उच्च → निम्न)"
                                        TaskSortOption.DATE_DESC -> "नवीनतम पहले"
                                        TaskSortOption.DATE_ASC -> "पुरातन पहले"
                                        TaskSortOption.TITLE_ASC -> "शीर्षक (अ → ज्ञ)"
                                        TaskSortOption.STATUS -> "लंबित पहले"
                                    }
                                } else option.label
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortLabel,
                                            fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSort == option) PrimaryBlue else TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        selectedSort = option
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Row 2: Timing Horizon Filter Chips ("All Time", "Today", "Upcoming", "No Date")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DueHorizonFilter.values().forEach { horizon ->
                    val horizonLabel = if (isHindi) {
                        when (horizon) {
                            DueHorizonFilter.ALL -> "सभी समय"
                            DueHorizonFilter.TODAY -> "आज"
                            DueHorizonFilter.UPCOMING -> "आगामी"
                            DueHorizonFilter.NO_DUE -> "बिना तारीख"
                        }
                    } else horizon.label
                    val isSelected = selectedHorizon == horizon
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) AccentPurple.copy(alpha = 0.25f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AccentPurple else CardBorder.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.clickable { selectedHorizon = horizon }
                    ) {
                        Text(
                            text = horizonLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AccentPurple else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(AppStrings.get("search_tasks_hint", isHindi), color = TextMuted, fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                singleLine = true
            )
        }

        // ── Selection Mode Action Bar (Export .md/.txt/.json/.html/copy, Bulk Move, Delete) ──
        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HintIconButton(
                                onClick = {
                                    isSelectionModeActive = false
                                    selectedTaskIds = emptySet()
                                },
                                hint = if (isHindi) "चयन मोड बंद करें" else "Exit Selection Mode",
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Exit Selection", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "${selectedTaskIds.size} चयनित" else "${selectedTaskIds.size} Selected",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                fontSize = 14.sp
                            )
                        }

                        // Bulk Move Arrows (only if same level & same parent!)
                        val selectedList = tasks.filter { it.id in selectedTaskIds }
                        val firstPid = selectedList.firstOrNull()?.parentId
                        val areSameParent = selectedList.isNotEmpty() && selectedList.all { it.parentId == firstPid }
                        val siblingList = if (areSameParent) {
                            if (firstPid == null) {
                                tasks.filter { it.parentId == null }
                            } else {
                                tasks.filter { it.parentId == firstPid }
                            }
                        } else emptyList()

                        val canMoveUp = areSameParent && canBulkMoveUp(selectedTaskIds, siblingList)
                        val canMoveDown = areSameParent && canBulkMoveDown(selectedTaskIds, siblingList)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (areSameParent && siblingList.size > 1) {
                                // Bulk Move Up Arrow
                                HintIconButton(
                                    onClick = {
                                        if (canMoveUp) {
                                            selectedSort = TaskSortOption.CUSTOM
                                            onReorderTasksBulk?.invoke(selectedTaskIds, true, siblingList)
                                        }
                                    },
                                    enabled = canMoveUp,
                                    hint = if (isHindi) "चयनित कार्यों को ऊपर ले जाएं" else "Bulk Move Up",
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Bulk Move Up",
                                        tint = if (canMoveUp) PrimaryBlue else TextMuted.copy(alpha = 0.25f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Bulk Move Down Arrow
                                HintIconButton(
                                    onClick = {
                                        if (canMoveDown) {
                                            selectedSort = TaskSortOption.CUSTOM
                                            onReorderTasksBulk?.invoke(selectedTaskIds, false, siblingList)
                                        }
                                    },
                                    enabled = canMoveDown,
                                    hint = if (isHindi) "चयनित कार्यों को नीचे ले जाएं" else "Bulk Move Down",
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Bulk Move Down",
                                        tint = if (canMoveDown) PrimaryBlue else TextMuted.copy(alpha = 0.25f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Delete Selected button
                            HintIconButton(
                                onClick = { showBulkDeleteConfirm = true },
                                hint = if (isHindi) "चयनित कार्य हटाएं" else "Delete Selected Tasks",
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = ActiveRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Export Actions: Copy, .md, .txt, .json, .html
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportSelectedTasks("copy") },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(if (isHindi) "कॉपी" else "Copy", fontSize = 11.sp, color = PrimaryBlue)
                        }
                        OutlinedButton(
                            onClick = { exportSelectedTasks("md") },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(".md", fontSize = 11.sp, color = Color(0xFF10B981))
                        }
                        OutlinedButton(
                            onClick = { exportSelectedTasks("txt") },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(".txt", fontSize = 11.sp, color = PrimaryBlue)
                        }
                        OutlinedButton(
                            onClick = { exportSelectedTasks("json") },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(".json", fontSize = 11.sp, color = AccentPurple)
                        }
                        OutlinedButton(
                            onClick = { exportSelectedTasks("html") },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(".html", fontSize = 11.sp, color = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 5. Main Tab Content ───────────────────────────────────────────────
        when (selectedMainTab) {
            // TAB 0: All (Grouped Overview with Collapsible Cards & Subtasks)
            0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "कार्य समूह (${taskGroups.size})" else "TASK GROUPS (${taskGroups.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )

                    val allExpanded = taskGroups.all { expandedGroups[it.id] != false }
                    TextButton(
                        onClick = {
                            val target = !allExpanded
                            taskGroups.forEach { expandedGroups[it.id] = target }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (allExpanded) AppStrings.get("collapse_all", isHindi) else AppStrings.get("expand_all", isHindi),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }

                if (topLevelTasks.isEmpty()) {
                    EmptyTasksPlaceholder(isHindi = isHindi)
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = if (isScrolled) 4.dp else 0.dp, bottom = 90.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            taskGroups.forEach { group ->
                                val rawGroupTasks = topLevelTasks.filter(group.taskFilter)
                                val processedGroupTasks = processTaskList(rawGroupTasks)
                                val isExpanded = expandedGroups[group.id] ?: true

                                if (rawGroupTasks.isEmpty()) {
                                    item(key = "empty_${group.id}") {
                                        Box(modifier = Modifier.animateItemPlacement()) {
                                            CompactEmptyGroupCard(
                                                group = group,
                                                isReorderMode = isReorderModeActive,
                                                onEnterReorderMode = { isReorderModeActive = true },
                                                onMoveUp = {
                                                    selectedSort = TaskSortOption.CUSTOM
                                                    val idx = taskGroups.indexOf(group)
                                                    if (idx > 0) {
                                                        val names = taskGroups.map { it.title.removePrefix("📌 ") }
                                                        onReorderGroups(idx, idx - 1, names)
                                                    }
                                                },
                                                onMoveDown = {
                                                    selectedSort = TaskSortOption.CUSTOM
                                                    val idx = taskGroups.indexOf(group)
                                                    if (idx < taskGroups.size - 1) {
                                                        val names = taskGroups.map { it.title.removePrefix("📌 ") }
                                                        onReorderGroups(idx, idx + 1, names)
                                                    }
                                                },
                                                onDeleteGroup = {
                                                    val catName = group.title.removePrefix("📌 ").trim()
                                                    onDeleteCustomCategory(catName)
                                                },
                                                isCustomCategory = userPreferences.customCategories.contains(group.title.removePrefix("📌 ").trim()),
                                                onAddGroupTask = {
                                                    selectedCategoryTag = group.title.removePrefix("📌 ").trim()
                                                    showAddTaskDialog = true
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    @OptIn(ExperimentalFoundationApi::class)
                                    stickyHeader(key = "header_${group.id}") {
                                        StickyGroupHeader(
                                            group = group,
                                            totalCount = rawGroupTasks.size,
                                            pendingCount = rawGroupTasks.count { !it.isCompleted },
                                            isExpanded = isExpanded,
                                            onToggleExpand = { expandedGroups[group.id] = !isExpanded },
                                            onAddGroupTask = {
                                                selectedCategoryTag = group.title.removePrefix("📌 ").trim()
                                                showAddTaskDialog = true
                                            },
                                            onMoveUp = {
                                                selectedSort = TaskSortOption.CUSTOM
                                                val idx = taskGroups.indexOf(group)
                                                if (idx > 0) {
                                                    val names = taskGroups.map { it.title.removePrefix("📌 ") }
                                                    onReorderGroups(idx, idx - 1, names)
                                                }
                                            },
                                            onMoveDown = {
                                                selectedSort = TaskSortOption.CUSTOM
                                                val idx = taskGroups.indexOf(group)
                                                if (idx < taskGroups.size - 1) {
                                                    val names = taskGroups.map { it.title.removePrefix("📌 ") }
                                                    onReorderGroups(idx, idx + 1, names)
                                                }
                                            }
                                        )
                                    }

                                    if (isExpanded) {
                                        items(processedGroupTasks, key = { "${group.id}_task_${it.id}" }) { task ->
                                            Box(modifier = Modifier.animateItemPlacement()) {
                                                NestedTaskCard(
                                                    task = task,
                                                    allTasks = tasks,
                                                    isSelectionMode = isSelectionModeActive,
                                                    isSelected = task.id in selectedTaskIds,
                                                    selectedTaskIds = selectedTaskIds,
                                                    onToggleSelection = { toggleTaskSelection(it) },
                                                    isReorderMode = isReorderModeActive,
                                                    onEnterReorderMode = { isReorderModeActive = true },
                                                    onToggleCompletion = onToggleCompletion,
                                                    onDeleteTask = { taskToDeleteConfirm = it },
                                                    onAddSubtask = { node -> subtaskParentTask = node },
                                                    onMoveTask = { moveTargetTask = task },
                                                    onEditTask = { taskToEdit = it },
                                                    onMoveUp = {
                                                        selectedSort = TaskSortOption.CUSTOM
                                                        val idx = processedGroupTasks.indexOf(task)
                                                        if (idx > 0) {
                                                            onReorderTasks(idx, idx - 1, processedGroupTasks)
                                                        }
                                                    },
                                                    onMoveDown = {
                                                        selectedSort = TaskSortOption.CUSTOM
                                                        val idx = processedGroupTasks.indexOf(task)
                                                        if (idx < processedGroupTasks.size - 1) {
                                                            onReorderTasks(idx, idx + 1, processedGroupTasks)
                                                        }
                                                    },
                                                    onReorderSubtasks = { from, to, list ->
                                                        selectedSort = TaskSortOption.CUSTOM
                                                        onReorderTasks(from, to, list)
                                                    },
                                                    isHindi = isHindi
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── VS Code Sticky Scroll Stack Overlay (Level 0: Category Group, Level 1: Active Parent Task) ──
                        val activeGroupInfo by remember(taskGroups, topLevelTasks, tasks) {
                            derivedStateOf {
                                val visible = listState.layoutInfo.visibleItemsInfo
                                if (visible.isEmpty() || !isScrolled) null
                                else {
                                    val firstVisible = visible.firstOrNull()
                                    val key = firstVisible?.key?.toString() ?: ""
                                    val group = taskGroups.firstOrNull { g ->
                                        key.startsWith("header_${g.id}") ||
                                        key.startsWith("empty_${g.id}") ||
                                        topLevelTasks.filter(g.taskFilter).any { t -> key == "${g.id}_task_${t.id}" || key == "task_${t.id}" || key == "${t.id}" }
                                    } ?: taskGroups.firstOrNull { g ->
                                        val groupTasks = topLevelTasks.filter(g.taskFilter)
                                        groupTasks.any { gt -> tasks.any { sub -> sub.parentId == gt.id && (key.contains("subtask_${sub.id}") || key == "${sub.id}") } }
                                    }

                                    val task = topLevelTasks.firstOrNull { t ->
                                        key.contains("_task_${t.id}") || key == "task_${t.id}" || key == "${t.id}" ||
                                        tasks.filter { it.parentId == t.id }.any { sub -> key.contains("subtask_${sub.id}") || key == "${sub.id}" }
                                    }
                                    Pair(group, task)
                                }
                            }
                        }

                        val activeGroup = activeGroupInfo?.first
                        val activeTask = activeGroupInfo?.second

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isScrolled && activeGroup != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .zIndex(30f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBackground)
                                    .padding(bottom = 4.dp)
                            ) {
                                activeGroup?.let { group ->
                                    val rawGroupTasks = topLevelTasks.filter(group.taskFilter)
                                    VsCodeStickyGroupBar(
                                        group = group,
                                        totalCount = rawGroupTasks.size,
                                        pendingCount = rawGroupTasks.count { !it.isCompleted },
                                        isExpanded = expandedGroups[group.id] ?: true,
                                        onToggleExpand = {
                                            val curr = expandedGroups[group.id] ?: true
                                            expandedGroups[group.id] = !curr
                                        },
                                        onAddGroupTask = {
                                            selectedCategoryTag = group.title.removePrefix("📌 ").trim()
                                            showAddTaskDialog = true
                                        },
                                        onMoveUp = {
                                            selectedSort = TaskSortOption.CUSTOM
                                            val idx = taskGroups.indexOf(group)
                                            if (idx > 0) {
                                                val names = taskGroups.map { it.title.removePrefix("📌 ") }
                                                onReorderGroups(idx, idx - 1, names)
                                            }
                                        },
                                        onMoveDown = {
                                            selectedSort = TaskSortOption.CUSTOM
                                            val idx = taskGroups.indexOf(group)
                                            if (idx < taskGroups.size - 1) {
                                                val names = taskGroups.map { it.title.removePrefix("📌 ") }
                                                onReorderGroups(idx, idx + 1, names)
                                            }
                                        }
                                    )
                                }

                                // Stacked Level 1: Active Parent Task (pins under category when child subtasks scroll)
                                if (activeTask != null && tasks.any { it.parentId == activeTask.id }) {
                                    val directSubtasks = tasks.filter { it.parentId == activeTask.id }
                                    val doneSubtasks = directSubtasks.count { it.isCompleted }
                                    VsCodeStickyTaskBar(
                                        task = activeTask,
                                        completedCount = doneSubtasks,
                                        totalCount = directSubtasks.size,
                                        onToggleComplete = { onToggleCompletion(activeTask.id, !activeTask.isCompleted) },
                                        onAddSubtask = { subtaskParentTask = activeTask }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 1: Priority & Urgent
            1 -> {
                val priorityTasks = processTaskList(topLevelTasks.sortedWith(
                    compareBy<NoteTask> { it.isCompleted }
                        .thenByDescending { it.priority }
                        .thenByDescending { it.timestampMillis }
                ))

                if (priorityTasks.isEmpty()) {
                    EmptyTasksPlaceholder(
                        message = if (isHindi) "वर्तमान फ़िल्टर से कोई जरूरी कार्य नहीं मिला" else "No priority tasks matching current filter",
                        isHindi = isHindi
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(priorityTasks, key = { it.id }) { task ->
                            Box(modifier = Modifier.animateItemPlacement()) {
                                NestedTaskCard(
                                    task = task,
                                    allTasks = tasks,
                                    isSelectionMode = isSelectionModeActive,
                                    isSelected = task.id in selectedTaskIds,
                                    selectedTaskIds = selectedTaskIds,
                                    onToggleSelection = { toggleTaskSelection(it) },
                                    isHindi = isHindi,
                                    isReorderMode = isReorderModeActive,
                                    onEnterReorderMode = { isReorderModeActive = true },
                                    onToggleCompletion = onToggleCompletion,
                                    onDeleteTask = { taskToDeleteConfirm = it },
                                    onAddSubtask = { node -> subtaskParentTask = node },
                                    onMoveTask = { moveTargetTask = task },
                                    onEditTask = { taskToEdit = it },
                                    onMoveUp = {
                                        selectedSort = TaskSortOption.CUSTOM
                                        val idx = priorityTasks.indexOf(task)
                                        if (idx > 0) {
                                            onReorderTasks(idx, idx - 1, priorityTasks)
                                        }
                                    },
                                    onMoveDown = {
                                        selectedSort = TaskSortOption.CUSTOM
                                        val idx = priorityTasks.indexOf(task)
                                        if (idx < priorityTasks.size - 1) {
                                            onReorderTasks(idx, idx + 1, priorityTasks)
                                        }
                                    },
                                    onReorderSubtasks = { from, to, list ->
                                        selectedSort = TaskSortOption.CUSTOM
                                        onReorderTasks(from, to, list)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // TAB 2: Easy Wins (Low-Hanging Fruits)
            2 -> {
                val easyTasks = processTaskList(topLevelTasks.filter { it.isLowHangingFruit })

                if (easyTasks.isEmpty()) {
                    EmptyTasksPlaceholder(
                        message = if (isHindi) "अभी तक कोई त्वरित कार्य नहीं मिला" else "No quick wins detected yet",
                        isHindi = isHindi
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(easyTasks, key = { it.id }) { task ->
                            Box(modifier = Modifier.animateItemPlacement()) {
                                NestedTaskCard(
                                    task = task,
                                    allTasks = tasks,
                                    isSelectionMode = isSelectionModeActive,
                                    isSelected = task.id in selectedTaskIds,
                                    selectedTaskIds = selectedTaskIds,
                                    onToggleSelection = { toggleTaskSelection(it) },
                                    isHindi = isHindi,
                                    isReorderMode = isReorderModeActive,
                                    onEnterReorderMode = { isReorderModeActive = true },
                                    onToggleCompletion = onToggleCompletion,
                                    onDeleteTask = { taskToDeleteConfirm = it },
                                    onAddSubtask = { node -> subtaskParentTask = node },
                                    onMoveTask = { moveTargetTask = task },
                                    onEditTask = { taskToEdit = it },
                                    onMoveUp = {
                                        selectedSort = TaskSortOption.CUSTOM
                                        val idx = easyTasks.indexOf(task)
                                        if (idx > 0) {
                                            onReorderTasks(idx, idx - 1, easyTasks)
                                        }
                                    },
                                    onMoveDown = {
                                        selectedSort = TaskSortOption.CUSTOM
                                        val idx = easyTasks.indexOf(task)
                                        if (idx < easyTasks.size - 1) {
                                            onReorderTasks(idx, idx + 1, easyTasks)
                                        }
                                    },
                                    onReorderSubtasks = { from, to, list ->
                                        selectedSort = TaskSortOption.CUSTOM
                                        onReorderTasks(from, to, list)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // TAB 3: By Category Tags (CRUD + Pinning + Rearranging)
            3 -> {
                Column {
                    // Category Chips Bar with + Add Category & Pinned Indicators
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        // + Add Category Chip
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.clickable { showAddCategoryDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Category", tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isHindi) "नई श्रेणी" else "New Category", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        items(allCategories) { cat ->
                            val isPinned = userPreferences.pinnedCategories.contains(cat)
                            val count = topLevelTasks.count { it.categoryGroup.equals(cat, ignoreCase = true) }

                            FilterChip(
                                selected = selectedCategoryTag == cat,
                                onClick = { selectedCategoryTag = cat },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isPinned) {
                                             Text("📌 ", fontSize = 10.sp)
                                        }
                                        Text("$cat ($count)", fontSize = 11.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = DarkBackground,
                                    containerColor = SurfaceDark,
                                    labelColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Category Action Toolbar (Pin/Unpin, Delete if custom)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isCurrentPinned = userPreferences.pinnedCategories.contains(selectedCategoryTag)
                        val isCustom = userPreferences.customCategories.contains(selectedCategoryTag)

                        Text(
                            text = if (isHindi) "श्रेणी: $selectedCategoryTag" else "Category: $selectedCategoryTag",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Pin Toggle
                            OutlinedButton(
                                onClick = { onTogglePinCategory(selectedCategoryTag) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isCurrentPinned) AccentPurple else TextMuted)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isCurrentPinned) (if (isHindi) "अनपिन करें" else "Unpin") else (if (isHindi) "पिन करें" else "Pin to Top"), fontSize = 10.sp, color = if (isCurrentPinned) AccentPurple else TextMuted)
                            }

                            // Delete Custom Category (prompts confirmation)
                            if (isCustom) {
                                OutlinedButton(
                                    onClick = {
                                        categoryToDeleteConfirm = selectedCategoryTag
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = ActiveRed)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isHindi) "हटाएं" else "Delete", fontSize = 10.sp, color = ActiveRed)
                                }
                            }
                        }
                    }

                    val catTasks = processTaskList(topLevelTasks.filter { it.categoryGroup.equals(selectedCategoryTag, ignoreCase = true) })

                    if (catTasks.isEmpty()) {
                        EmptyTasksPlaceholder(
                            message = if (isHindi) "$selectedCategoryTag में कोई कार्य नहीं मिला" else "No tasks found under $selectedCategoryTag",
                            isHindi = isHindi
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(catTasks, key = { it.id }) { task ->
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    NestedTaskCard(
                                        task = task,
                                        allTasks = tasks,
                                        isSelectionMode = isSelectionModeActive,
                                        isSelected = task.id in selectedTaskIds,
                                        selectedTaskIds = selectedTaskIds,
                                        onToggleSelection = { toggleTaskSelection(it) },
                                        isHindi = isHindi,
                                        isReorderMode = isReorderModeActive,
                                        onEnterReorderMode = { isReorderModeActive = true },
                                        onToggleCompletion = onToggleCompletion,
                                        onDeleteTask = { taskToDeleteConfirm = it },
                                        onAddSubtask = { node -> subtaskParentTask = node },
                                        onMoveTask = { moveTargetTask = task },
                                        onEditTask = { taskToEdit = it },
                                        onMoveUp = {
                                            selectedSort = TaskSortOption.CUSTOM
                                            val idx = catTasks.indexOf(task)
                                            if (idx > 0) {
                                                onReorderTasks(idx, idx - 1, catTasks)
                                            }
                                        },
                                        onMoveDown = {
                                            selectedSort = TaskSortOption.CUSTOM
                                            val idx = catTasks.indexOf(task)
                                            if (idx < catTasks.size - 1) {
                                                onReorderTasks(idx, idx + 1, catTasks)
                                            }
                                        },
                                        onReorderSubtasks = { from, to, list ->
                                            selectedSort = TaskSortOption.CUSTOM
                                            onReorderTasks(from, to, list)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Floating Action Button (Bottom-Right Expandable Speed Dial) ───────
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 16.dp)
                .zIndex(15f)
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Add Group
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentPurple,
                        shadowElevation = 8.dp,
                        modifier = Modifier.clickable {
                            isFabExpanded = false
                            showAddGroupDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get("add_group", isHindi),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Option 2: Add Task
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryBlue,
                        shadowElevation = 8.dp,
                        modifier = Modifier.clickable {
                            isFabExpanded = false
                            showAddTaskDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get("add_task", isHindi),
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Main Floating Button
            Surface(
                shape = CircleShape,
                color = PrimaryBlue,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(56.dp)
                    .combinedClickable(
                        onClick = { isFabExpanded = !isFabExpanded },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, if (isHindi) "त्वरित कार्य (नया कार्य या समूह जोड़ें)" else "Quick Actions (Add Task or Group)", Toast.LENGTH_SHORT).show()
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Quick Actions",
                        tint = DarkBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

    // ── Dialog: Add Task Manually ─────────────────────────────────────────────
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(allCategories.firstOrNull() ?: "Shopping") }
        var priority by remember { mutableIntStateOf(NoteTask.PRIORITY_MEDIUM) }
        var isQuickWin by remember { mutableStateOf(false) }
        var dueText by remember { mutableStateOf("") }
        var autoDone by remember { mutableStateOf(true) }

        val corporateTextFieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF0F172A),
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = Color(0xFFF8FAFC),
            unfocusedTextColor = Color(0xFFF8FAFC),
            focusedPlaceholderColor = TextMuted.copy(alpha = 0.35f),
            unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.35f),
            focusedLabelColor = PrimaryBlue,
            unfocusedLabelColor = TextMuted
        )

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text(if (isHindi) "➕ नया कार्य जोड़ें" else "➕ Add New Task", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi) "कार्य शीर्षक" else "Task Title",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        placeholder = {
                            Text(
                                text = if (isHindi) "उदा. सब्जियां लाएं..." else "e.g. Buy vegetables...",
                                color = TextMuted.copy(alpha = 0.45f),
                                fontStyle = FontStyle.Italic
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = corporateTextFieldColors
                    )

                    // Category Selector
                    Text(if (isHindi) "श्रेणी:" else "Category:", fontSize = 12.sp, color = TextMuted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allCategories) { cat ->
                            FilterChip(
                                selected = selectedCat == cat,
                                onClick = { selectedCat = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Priority Selector
                    Text(if (isHindi) "प्राथमिकता:" else "Priority:", fontSize = 12.sp, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple(NoteTask.PRIORITY_HIGH, if (isHindi) "उच्च" else "High", ActiveRed),
                            Triple(NoteTask.PRIORITY_MEDIUM, if (isHindi) "मध्यम" else "Medium", Color(0xFFF59E0B)),
                            Triple(NoteTask.PRIORITY_LOW, if (isHindi) "कम" else "Low", PrimaryBlue)
                        ).forEach { (p, label, color) ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(label, fontSize = 11.sp, color = if (priority == p) DarkBackground else color) }
                            )
                        }
                    }

                    // Due Date / Timing Horizon Preset
                    Text(if (isHindi) "समय सीमा:" else "Timing Horizon:", fontSize = 12.sp, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple("Today", if (isHindi) "आज" else "Today", "Today"),
                            Triple("Tomorrow", if (isHindi) "कल" else "Tomorrow", "Tomorrow"),
                            Triple("Next Week", if (isHindi) "अगले सप्ताह" else "Next Week", "Next Week"),
                            Triple("", if (isHindi) "कोई नहीं" else "None", "")
                        ).forEach { (valKey, displayLabel, _) ->
                            FilterChip(
                                selected = dueText == valKey,
                                onClick = { dueText = valKey },
                                label = { Text(displayLabel, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Auto-complete Parent Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isHindi) "उप-कार्य पूर्ण होने पर स्वतः पूर्ण करें" else "Auto-complete on subtasks done", fontSize = 12.sp, color = TextPrimary)
                        Switch(
                            checked = autoDone,
                            onCheckedChange = { autoDone = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onCreateTask(taskTitle, selectedCat, priority, isQuickWin, dueText, null, autoDone)
                            showAddTaskDialog = false
                        }
                    }
                ) {
                    Text(AppStrings.get("add_task", isHindi))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Dialog: Add Subtask ───────────────────────────────────────────────────
    if (subtaskParentTask != null) {
        var subtaskTitle by remember { mutableStateOf("") }
        val parent = subtaskParentTask!!

        val corporateTextFieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF0F172A),
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = Color(0xFFF8FAFC),
            unfocusedTextColor = Color(0xFFF8FAFC),
            focusedPlaceholderColor = TextMuted.copy(alpha = 0.35f),
            unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.35f),
            focusedLabelColor = PrimaryBlue,
            unfocusedLabelColor = TextMuted
        )

        AlertDialog(
            onDismissRequest = { subtaskParentTask = null },
            title = { Text(if (isHindi) "➕ \"${parent.title.take(25)}\" में उप-कार्य जोड़ें" else "➕ Add Subtask to \"${parent.title.take(25)}\"", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(if (isHindi) "आइटम जोड़ें (उदा. आलू, टमाटर, बैंगन) अपनी चेकबॉक्स के साथ:" else "Add an item (e.g. आलू, टमाटर, बैंगन) with its own independent checkbox:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "उप-कार्य का शीर्षक" else "Subtask Title",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = subtaskTitle,
                        onValueChange = { subtaskTitle = it },
                        placeholder = {
                            Text(
                                text = if (isHindi) "उदा. 1 किलो आलू..." else "e.g. 1kg potatoes...",
                                color = TextMuted.copy(alpha = 0.45f),
                                fontStyle = FontStyle.Italic
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = corporateTextFieldColors
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subtaskTitle.isNotBlank()) {
                            onAddSubtask(parent.id, subtaskTitle)
                            subtaskParentTask = null
                        }
                    }
                ) {
                    Text(AppStrings.get("add_subtask", isHindi))
                }
            },
            dismissButton = {
                TextButton(onClick = { subtaskParentTask = null }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Dialog: Add Custom Category ──────────────────────────────────────────
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }

        val corporateTextFieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF0F172A),
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = Color(0xFFF8FAFC),
            unfocusedTextColor = Color(0xFFF8FAFC),
            focusedPlaceholderColor = TextMuted,
            unfocusedPlaceholderColor = TextMuted,
            focusedLabelColor = PrimaryBlue,
            unfocusedLabelColor = TextMuted
        )

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(AppStrings.get("create_category", isHindi), fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(if (isHindi) "नया श्रेणी नाम दर्ज करें (उदा. फिटनेस, किराना, वित्त):" else "Enter a new category name (e.g. Fitness, Groceries, Finance):", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "श्रेणी का नाम" else "Category Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = {
                            Text(
                                text = if (isHindi) "उदा. फिटनेस, किराना, वित्त..." else "e.g. Fitness, Groceries, Finance...",
                                color = TextMuted.copy(alpha = 0.45f),
                                fontStyle = FontStyle.Italic
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = corporateTextFieldColors
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onAddCustomCategory(newCatName)
                            selectedCategoryTag = newCatName.trim()
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text(AppStrings.get("create", isHindi))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Dialog: Move Task to Category ─────────────────────────────────────────
    if (moveTargetTask != null) {
        val target = moveTargetTask!!
        AlertDialog(
            onDismissRequest = { moveTargetTask = null },
            title = { Text(if (isHindi) "📦 कार्य स्थानांतरित करें: \"${target.title.take(25)}\"" else "📦 Move \"${target.title.take(25)}\"", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(if (isHindi) "लक्षित श्रेणी चुनें:" else "Select target category:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allCategories) { cat ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (target.categoryGroup == cat) PrimaryBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMoveTaskToCategory(target.id, cat)
                                        moveTargetTask = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cat, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveTargetTask = null }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Dialog: Add Group ─────────────────────────────────────────────────────
    if (showAddGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text(AppStrings.get("add_group", isHindi), fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = if (isHindi) "समूह / श्रेणी का नाम" else "Group / Category Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = {
                            Text(
                                text = if (isHindi) "उदा. नई श्रेणी..." else "e.g. New Category...",
                                color = TextMuted.copy(alpha = 0.45f),
                                fontStyle = FontStyle.Italic
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onAddCustomCategory(groupName.trim())
                            showAddGroupDialog = false
                        }
                    }
                ) {
                    Text(AppStrings.get("create", isHindi))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Dialog: Edit Task ─────────────────────────────────────────────────────
    if (taskToEdit != null) {
        val editing = taskToEdit!!
        var editTitle by remember { mutableStateOf(editing.title) }
        var editCat by remember { mutableStateOf(editing.categoryGroup) }
        var editPrio by remember { mutableIntStateOf(editing.priority) }
        var editFruit by remember { mutableStateOf(editing.isLowHangingFruit) }
        var editDue by remember { mutableStateOf(editing.dueTimeText) }

        AlertDialog(
            onDismissRequest = { taskToEdit = null },
            title = { Text(AppStrings.get("edit_task", isHindi), fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = AppStrings.get("title", isHindi),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        placeholder = { Text(if (isHindi) "कार्य का शीर्षक..." else "Task title...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "समय सीमा (उदा. आज)" else "Timing Horizon (e.g. Today)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    OutlinedTextField(
                        value = editDue,
                        onValueChange = { editDue = it },
                        placeholder = { Text(if (isHindi) "उदा. आज, कल..." else "e.g. Today, Tomorrow...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isHindi) "त्वरित कार्य (आसान जीत):" else "Quick Win (Easy Fruit):", fontSize = 12.sp, color = TextPrimary)
                        Switch(checked = editFruit, onCheckedChange = { editFruit = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            onEditTask(editing.id, editTitle, editCat, editPrio, editFruit, editDue)
                            taskToEdit = null
                        }
                    }
                ) {
                    Text(AppStrings.get("save", isHindi))
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToEdit = null }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Confirmation Dialog: Delete Task ───────────────────────────────────────
    if (taskToDeleteConfirm != null) {
        val task = taskToDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { taskToDeleteConfirm = null },
            title = { Text(if (isHindi) "कार्य हटाएं?" else "Delete Task?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    text = if (isHindi) "क्या आप वाकई \"${task.title}\" को हटाना चाहते हैं? यह क्रिया पूर्ववत नहीं की जा सकती।"
                           else "Are you sure you want to delete \"${task.title}\"? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(task.id)
                        taskToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDeleteConfirm = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Confirmation Dialog: Delete Category ──────────────────────────────────
    if (categoryToDeleteConfirm != null) {
        val catName = categoryToDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { categoryToDeleteConfirm = null },
            title = { Text(if (isHindi) "श्रेणी हटाएं?" else "Delete Category?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    text = if (isHindi) "क्या आप वाकई \"$catName\" श्रेणी को हटाना चाहते हैं?"
                           else "Are you sure you want to delete category \"$catName\"?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomCategory(catName)
                        if (selectedCategoryTag == catName) selectedCategoryTag = "Shopping"
                        categoryToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDeleteConfirm = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Confirmation Dialog: Bulk Delete Tasks ────────────────────────────────
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text(if (isHindi) "चयनित कार्य हटाएं?" else "Delete Selected Tasks?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    text = if (isHindi) "क्या आप वाकई ${selectedTaskIds.size} चयनित कार्यों को हटाना चाहते हैं?"
                           else "Are you sure you want to delete ${selectedTaskIds.size} selected task(s)?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTasksBulk?.invoke(selectedTaskIds)
                        selectedTaskIds = emptySet()
                        isSelectionModeActive = false
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Dialog: Import Tasks JSON ─────────────────────────────────────────────
    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text(if (isHindi) "📥 कार्य आयात करें (JSON)" else "📥 Import Tasks (JSON)", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = if (isHindi) "कार्य या उप-कार्य युक्त JSON पेस्ट करें:" else "Paste JSON containing tasks and subtasks:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("[{\"title\": \"Task 1\", \"subtasks\": [\"Sub 1\"]}]", fontSize = 11.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            onImportTasksJson?.invoke(importJsonText) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    importJsonText = ""
                                    showImportJsonDialog = false
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isHindi) "आयात करें" else "Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }
    }
}

/**
 * Compact One-Liner Card for Empty Task Groups.
 * Preserves screen space by not expanding when empty!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactEmptyGroupCard(
    group: TaskGroupDefinition,
    isReorderMode: Boolean = false,
    onEnterReorderMode: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onDeleteGroup: () -> Unit = {},
    isCustomCategory: Boolean = false,
    onAddGroupTask: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isHamburgerPressed by remember { mutableStateOf(false) }
    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .combinedClickable(
                onClick = onAddGroupTask,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnterReorderMode()
                }
            ),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isReorderMode) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .scale(if (isHamburgerPressed) 1.25f else 1.0f)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isHamburgerPressed = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        verticalDragAccumulator = 0f
                                    },
                                    onDragEnd = {
                                        isHamburgerPressed = false
                                        verticalDragAccumulator = 0f
                                    },
                                    onDragCancel = {
                                        isHamburgerPressed = false
                                        verticalDragAccumulator = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        verticalDragAccumulator += dragAmount
                                        val threshold = 32f
                                        if (verticalDragAccumulator <= -threshold) {
                                            onMoveUp()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            verticalDragAccumulator = 0f
                                        } else if (verticalDragAccumulator >= threshold) {
                                            onMoveDown()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            verticalDragAccumulator = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Reorder Handle",
                            tint = if (isHamburgerPressed) AccentPurple else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(text = group.icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(0)",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isReorderMode) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = PrimaryBlue, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = PrimaryBlue, modifier = Modifier.size(15.dp))
                    }
                    if (isCustomCategory) {
                        IconButton(onClick = onDeleteGroup, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = ActiveRed, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                IconButton(onClick = onAddGroupTask, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to group",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * VS Code Sticky Scroll Group Bar:
 * Scope Level 0 - Pins the parent category at the top of the canvas while scrolling child items.
 * Styled with elevated backdrop so child items scroll cleanly beneath it without leaving canvas.
 */
@Composable
fun VsCodeStickyGroupBar(
    group: TaskGroupDefinition,
    totalCount: Int,
    pendingCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddGroupTask: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (pendingCount > 0) group.accentColor.copy(alpha = 0.7f) else CardBorder
        ),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Group Reorder Handle",
                    tint = group.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = group.icon, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = group.accentColor.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "$pendingCount/$totalCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = group.accentColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Group Up", tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Group Down", tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                IconButton(onClick = onAddGroupTask, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", tint = PrimaryBlue, modifier = Modifier.size(15.dp))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * VS Code Sticky Scroll Task Bar:
 * Scope Level 1 - Pins parent task immediately under the Category bar when scrolling inside its subtasks.
 */
@Composable
fun VsCodeStickyTaskBar(
    task: NoteTask,
    completedCount: Int,
    totalCount: Int,
    onToggleComplete: () -> Unit,
    onAddSubtask: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.45f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.SubdirectoryArrowRight,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onToggleComplete, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (totalCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PrimaryBlue.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$completedCount/$totalCount",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onAddSubtask, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Subtask", tint = PrimaryBlue, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/**
 * Sticky Category Header: Pins at top while scrolling its child tasks (VS Code Breadcrumbs style).
 * Retains all menu and action buttons.
 */
@Composable
fun StickyGroupHeader(
    group: TaskGroupDefinition,
    totalCount: Int,
    pendingCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddGroupTask: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (pendingCount > 0) group.accentColor.copy(alpha = 0.5f) else CardBorder
        ),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Move Group",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = group.icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = group.accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "$pendingCount/$totalCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = group.accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onAddGroupTask, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task to Group", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Collapsible Card representing one intelligent group of tasks
 */
@Composable
fun TaskGroupCard(
    group: TaskGroupDefinition,
    rawTasks: List<NoteTask>,
    displayedTasks: List<NoteTask>,
    allTasks: List<NoteTask>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onDeleteTask: (NoteTask) -> Unit,
    onAddSubtask: (NoteTask) -> Unit,
    onMoveTask: (NoteTask) -> Unit,
    isHindi: Boolean = false
) {
    val totalInGroup = rawTasks.size
    val pendingInGroup = rawTasks.count { !it.isCompleted }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (pendingInGroup > 0) group.accentColor.copy(alpha = 0.6f) else CardBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = group.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = group.accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$totalInGroup",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = group.accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (displayedTasks.isEmpty()) {
                        Text(
                            text = if (rawTasks.isEmpty()) (if (isHindi) "इस समूह में अभी कोई कार्य नहीं है।" else "No tasks in this group yet.")
                                   else (if (isHindi) "कोई कार्य सक्रिय फ़िल्टर से मेल नहीं खाता।" else "No tasks match the active filter."),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedTasks.forEach { task ->
                                NestedTaskCard(
                                    task = task,
                                    allTasks = allTasks,
                                    isHindi = isHindi,
                                    onToggleCompletion = onToggleCompletion,
                                    onDeleteTask = onDeleteTask,
                                    onAddSubtask = onAddSubtask,
                                    onMoveTask = { onMoveTask(task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recursive arbitrary-depth subtask tree renderer with CheckBox, selection support,
 * Up/Down arrows, and delete button positioned on the RIGHT side.
 * Reordering is strictly bounded to the same parent task!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NestedSubtaskTree(
    parentId: Long,
    allTasks: List<NoteTask>,
    depth: Int = 1,
    isSelectionMode: Boolean = false,
    selectedTaskIds: Set<Long> = emptySet(),
    onToggleSelection: (Long) -> Unit = {},
    isReorderMode: Boolean = false,
    onEnterReorderMode: () -> Unit = {},
    onEditTask: (NoteTask) -> Unit = {},
    onToggleCompletion: (Long, Boolean) -> Unit,
    onDeleteTask: (NoteTask) -> Unit,
    onAddSubtask: (NoteTask) -> Unit,
    onReorderSubtasks: (Int, Int, List<NoteTask>) -> Unit = { _, _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val children = allTasks.filter { it.parentId == parentId }.sortedBy { it.orderIndex }
    if (children.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (14 * depth.coerceAtMost(3)).dp)
    ) {
        children.forEach { sub ->
            val grandChildren = allTasks.filter { it.parentId == sub.id }
            val completedGrandCount = grandChildren.count { it.isCompleted }
            val subIdx = children.indexOf(sub)
            var isHamburgerPressed by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelection(sub.id)
                            } else {
                                onEditTask(sub)
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleSelection(sub.id)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection Checkbox
                if (isSelectionMode) {
                    Checkbox(
                        checked = sub.id in selectedTaskIds,
                        onCheckedChange = { onToggleSelection(sub.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryBlue,
                            uncheckedColor = TextMuted,
                            checkmarkColor = DarkBackground
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }

                // LEFT SIDE: Subdirectory arrow, Checkbox, Title
                Icon(
                    imageVector = Icons.Default.SubdirectoryArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))

                IconButton(
                    onClick = { onToggleCompletion(sub.id, !sub.isCompleted) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (sub.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Toggle Subtask",
                        tint = if (sub.isCompleted) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = sub.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (sub.isCompleted) TextMuted else TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (grandChildren.isNotEmpty()) {
                    Text(
                        text = "($completedGrandCount/${grandChildren.size})",
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Add nested subtask (+) at this level
                IconButton(
                    onClick = { onAddSubtask(sub) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add nested subtask", tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                }

                // RIGHT SIDE: Reorder handles & delete button ONLY visible when in Reorder Mode!
                if (isReorderMode) {
                    // Move Up (generous 36dp touch target)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = subIdx > 0) {
                                if (subIdx > 0) {
                                    onReorderSubtasks(subIdx, subIdx - 1, children)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Subtask Up",
                            tint = if (subIdx > 0) PrimaryBlue else TextMuted.copy(alpha = 0.2f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Move Down (generous 36dp touch target)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = subIdx < children.size - 1) {
                                if (subIdx < children.size - 1) {
                                    onReorderSubtasks(subIdx, subIdx + 1, children)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Subtask Down",
                            tint = if (subIdx < children.size - 1) PrimaryBlue else TextMuted.copy(alpha = 0.2f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Hamburger Drag Handle (vibrates and drags vertically to reorder subtask)
                    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(if (isHamburgerPressed) 1.25f else 1.0f)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isHamburgerPressed = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        verticalDragAccumulator = 0f
                                    },
                                    onDragEnd = {
                                        isHamburgerPressed = false
                                        verticalDragAccumulator = 0f
                                    },
                                    onDragCancel = {
                                        isHamburgerPressed = false
                                        verticalDragAccumulator = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        verticalDragAccumulator += dragAmount
                                        val threshold = 32f
                                        if (verticalDragAccumulator <= -threshold && subIdx > 0) {
                                            onReorderSubtasks(subIdx, subIdx - 1, children)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            verticalDragAccumulator = 0f
                                        } else if (verticalDragAccumulator >= threshold && subIdx < children.size - 1) {
                                            onReorderSubtasks(subIdx, subIdx + 1, children)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            verticalDragAccumulator = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Reorder Handle",
                            tint = if (isHamburgerPressed) AccentPurple else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete button (visible ONLY in Reorder Mode)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onDeleteTask(sub) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Subtask",
                            tint = ActiveRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            NestedSubtaskTree(
                parentId = sub.id,
                allTasks = allTasks,
                depth = depth + 1,
                isSelectionMode = isSelectionMode,
                selectedTaskIds = selectedTaskIds,
                onToggleSelection = onToggleSelection,
                isReorderMode = isReorderMode,
                onEnterReorderMode = onEnterReorderMode,
                onEditTask = onEditTask,
                onToggleCompletion = onToggleCompletion,
                onDeleteTask = onDeleteTask,
                onAddSubtask = onAddSubtask,
                onReorderSubtasks = onReorderSubtasks
            )
        }
    }
}

/**
 * Nested Task Card:
 * - When in Selection Mode, tapping only toggles selection (never edits).
 * - Touch & hold (long press) activates Selection Mode and toggles selection.
 * - Checkboxes used for task completion instead of radio buttons.
 * - Hamburger prefix (≡) and Move Up/Down arrows are hidden until Reorder Mode is active.
 * - Generous 36dp touch hit targets for arrows and handles.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NestedTaskCard(
    task: NoteTask,
    allTasks: List<NoteTask>,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    selectedTaskIds: Set<Long> = emptySet(),
    onToggleSelection: (Long) -> Unit = {},
    isReorderMode: Boolean = false,
    onEnterReorderMode: () -> Unit = {},
    onToggleCompletion: (Long, Boolean) -> Unit,
    onDeleteTask: (NoteTask) -> Unit,
    onAddSubtask: (NoteTask) -> Unit,
    onMoveTask: () -> Unit,
    onEditTask: (NoteTask) -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onReorderSubtasks: (Int, Int, List<NoteTask>) -> Unit = { _, _, _ -> },
    isHindi: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var isMenuOpen by remember { mutableStateOf(false) }
    var isHamburgerPressed by remember { mutableStateOf(false) }

    val directSubtasks = allTasks.filter { it.parentId == task.id }
    val completedSubtasksCount = directSubtasks.count { it.isCompleted }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PrimaryBlue else if (task.priority == NoteTask.PRIORITY_HIGH && !task.isCompleted) ActiveRed.copy(alpha = 0.5f) else CardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection(task.id)
                    } else {
                        onEditTask(task)
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleSelection(task.id)
                }
            )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection Checkbox
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection(task.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryBlue,
                            uncheckedColor = TextMuted,
                            checkmarkColor = DarkBackground
                        ),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // If in Reorder mode, show Drag handle (≡) at start with pop-out, vibration & vertical drag reordering!
                if (isReorderMode) {
                    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(if (isHamburgerPressed) 1.25f else 1.0f)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isHamburgerPressed = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        verticalDragAccumulator = 0f
                                    },
                                    onDragEnd = {
                                        isHamburgerPressed = false
                                        verticalDragAccumulator = 0f
                                    },
                                    onDragCancel = {
                                        isHamburgerPressed = false
                                        verticalDragAccumulator = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        verticalDragAccumulator += dragAmount
                                        val threshold = 32f
                                        if (verticalDragAccumulator <= -threshold) {
                                            onMoveUp()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            verticalDragAccumulator = 0f
                                        } else if (verticalDragAccumulator >= threshold) {
                                            onMoveDown()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            verticalDragAccumulator = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drag Handle",
                            tint = if (isHamburgerPressed) AccentPurple else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                // Checkbox
                IconButton(
                    onClick = { onToggleCompletion(task.id, !task.isCompleted) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (task.dueTimeText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "⏳ ${task.dueTimeText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = task.categoryGroup,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPurple,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }

                        if (directSubtasks.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$completedSubtasksCount/${directSubtasks.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Add Subtask (+) Icon button
                IconButton(
                    onClick = { onAddSubtask(task) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subtask",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Move Up & Down ONLY in Reorder Mode (generous 36dp touch target)
                if (isReorderMode) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onMoveUp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onMoveDown),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    }
                }

                // 3-dot Menu
                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("edit_task", isHindi), fontSize = 12.sp, color = TextPrimary) },
                            onClick = {
                                isMenuOpen = false
                                onEditTask(task)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("add_subtask", isHindi), fontSize = 12.sp, color = TextPrimary) },
                            onClick = {
                                isMenuOpen = false
                                onAddSubtask(task)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("move_task", isHindi), fontSize = 12.sp, color = TextPrimary) },
                            onClick = {
                                isMenuOpen = false
                                onMoveTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("delete_task", isHindi), fontSize = 12.sp, color = ActiveRed) },
                            onClick = {
                                isMenuOpen = false
                                onDeleteTask(task)
                            }
                        )
                    }
                }
            }

            // Subtask List (Indented under Parent with arbitrary depth)
            if (directSubtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                NestedSubtaskTree(
                    parentId = task.id,
                    allTasks = allTasks,
                    depth = 1,
                    isSelectionMode = isSelectionMode,
                    selectedTaskIds = selectedTaskIds,
                    onToggleSelection = onToggleSelection,
                    isReorderMode = isReorderMode,
                    onEnterReorderMode = onEnterReorderMode,
                    onEditTask = onEditTask,
                    onToggleCompletion = onToggleCompletion,
                    onDeleteTask = onDeleteTask,
                    onAddSubtask = onAddSubtask,
                    onReorderSubtasks = onReorderSubtasks
                )
            }
        }
    }
}

@Composable
fun EmptyTasksPlaceholder(
    message: String? = null,
    isHindi: Boolean = false
) {
    val displayMessage = message ?: if (isHindi) "कोई कार्य या एक्शन आइटम नहीं मिला" else "No tasks or action items found"
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayMessage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isHindi) "ऑडियो रिकॉर्ड करें या नया कार्य जोड़ने के लिए '➕ कार्य' दबाएं।" else "Record continuous audio or tap '➕ Task' to add action items.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

/**
 * Reusable IconButton that provides Haptic feedback and a helpful Toast hint
 * when touched and held (long pressed).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HintIconButton(
    onClick: () -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

