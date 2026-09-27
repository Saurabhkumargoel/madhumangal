package com.echo.recorder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.NoteTask
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import java.util.Locale

enum class TaskSortOption(val label: String) {
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
    onToggleCompletion: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    isRecording: Boolean = false,
    elapsedSeconds: Long = 0L,
    onToggleRecording: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedMainTab by remember { mutableIntStateOf(0) }
    var selectedSort by remember { mutableStateOf(TaskSortOption.PRIORITY_DESC) }
    var selectedStatus by remember { mutableStateOf(TaskStatusFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var selectedCategoryTag by remember { mutableStateOf("Work") }

    // Map of group collapse state (id -> Boolean, true = expanded)
    val expandedGroups = remember {
        mutableStateMapOf(
            "urgent" to true,
            "quick_wins" to true,
            "work" to true,
            "shopping" to true,
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

    // Helper to filter and sort task lists
    fun processTaskList(rawList: List<NoteTask>): List<NoteTask> {
        var filtered = rawList

        // Status filter
        filtered = when (selectedStatus) {
            TaskStatusFilter.ALL -> filtered
            TaskStatusFilter.PENDING -> filtered.filter { !it.isCompleted }
            TaskStatusFilter.COMPLETED -> filtered.filter { it.isCompleted }
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

    // Pre-defined groups for Tab 0 ("All")
    val taskGroups = listOf(
        TaskGroupDefinition(
            id = "urgent",
            title = "Urgent & High Priority",
            icon = "🚨",
            accentColor = ActiveRed,
            taskFilter = { (it.priority == NoteTask.PRIORITY_HIGH || it.categoryGroup.equals("Urgent", ignoreCase = true)) }
        ),
        TaskGroupDefinition(
            id = "quick_wins",
            title = "Low-Hanging Fruits (Easy Wins)",
            icon = "⚡",
            accentColor = Color(0xFFF59E0B),
            taskFilter = { it.isLowHangingFruit }
        ),
        TaskGroupDefinition(
            id = "work",
            title = "Work & Career",
            icon = "💼",
            accentColor = PrimaryBlue,
            taskFilter = { it.categoryGroup.equals("Work", ignoreCase = true) }
        ),
        TaskGroupDefinition(
            id = "shopping",
            title = "Shopping & Errands",
            icon = "🛒",
            accentColor = Color(0xFF10B981),
            taskFilter = { it.categoryGroup.equals("Shopping", ignoreCase = true) }
        ),
        TaskGroupDefinition(
            id = "personal",
            title = "Personal & Family",
            icon = "🏡",
            accentColor = AccentPurple,
            taskFilter = { it.categoryGroup.equals("Personal", ignoreCase = true) }
        ),
        TaskGroupDefinition(
            id = "ideas",
            title = "Ideas & Notes",
            icon = "💡",
            accentColor = Color(0xFF06B6D4),
            taskFilter = { it.categoryGroup.equals("Ideas", ignoreCase = true) || it.categoryGroup.equals("General", ignoreCase = true) }
        )
    )

    val mainTabs = listOf("All (Grouped)", "🚨 Priority", "⚡ Easy Wins", "🏷️ By Category")
    val categoryTags = listOf("Work", "Shopping", "Personal", "Urgent", "Ideas")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── 1. Top Header Row: Title on Left, Live Record Toggle on Right ──────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Task Dashboard",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$totalCount extracted task${if (totalCount != 1) "s" else ""} • $pendingCount pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dashboard Top-Right Recorder Toggle
            if (isRecording) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ActiveRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ActiveRed),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔴 $timerStr",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ActiveRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onToggleRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Record", tint = DarkBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 2. Smart Overview & AI Summary Card ───────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 4-Column Stat Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "$totalCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "TOTAL", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    }

                    // Urgent / High
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "$urgentCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (urgentCount > 0) ActiveRed else TextPrimary)
                        Text(text = "URGENT", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (urgentCount > 0) ActiveRed else TextMuted)
                    }

                    // Quick Wins
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "$easyWinsCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (easyWinsCount > 0) Color(0xFFF59E0B) else TextPrimary)
                        Text(text = "EASY WINS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (easyWinsCount > 0) Color(0xFFF59E0B) else TextMuted)
                    }

                    // Completed
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "$completedCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (completedCount > 0) Color(0xFF10B981) else TextPrimary)
                        Text(text = "COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (completedCount > 0) Color(0xFF10B981) else TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = PrimaryBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Smart AI Summary Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (totalCount == 0) {
                                "🎙️ Speak or import an audio file to extract actionable Hindi/Hinglish tasks."
                            } else if (urgentCount > 0 && easyWinsCount > 0) {
                                "🔥 Attention: $urgentCount urgent item${if (urgentCount > 1) "s" else ""} pending • ⚡ $easyWinsCount quick win${if (easyWinsCount > 1) "s" else ""} ready to tackle in <5 mins!"
                            } else if (urgentCount > 0) {
                                "🔥 Focus: $urgentCount urgent task${if (urgentCount > 1) "s" else ""} require priority resolution."
                            } else if (easyWinsCount > 0) {
                                "⚡ High Momentum: $easyWinsCount low-hanging fruit task${if (easyWinsCount > 1) "s" else ""} ready to complete quickly."
                            } else if (pendingCount == 0) {
                                "🎉 All tasks finished! You're completely caught up."
                            } else {
                                "📋 $pendingCount active task${if (pendingCount > 1) "s" else ""} across work, shopping, and personal categories."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    height = 2.5.dp
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

        Spacer(modifier = Modifier.height(10.dp))

        // ── 4. Interactive Sorting & Filtering Controls Bar ───────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status filter chips (All, Pending, Done)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskStatusFilter.values().forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = DarkBackground,
                            containerColor = SurfaceDark,
                            labelColor = TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            // Right side: Sort Button & Search Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Search Toggle Button
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) searchQuery = ""
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchExpanded || searchQuery.isNotBlank()) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { isSortMenuOpen = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isSortMenuOpen,
                        onDismissRequest = { isSortMenuOpen = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        TaskSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
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

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter tasks by keyword...", color = TextMuted, fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(48.dp),
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

        Spacer(modifier = Modifier.height(8.dp))

        // ── 5. Main Tab Content ───────────────────────────────────────────────
        when (selectedMainTab) {
            // TAB 0: All (Grouped Overview with Collapsible Group Cards & Smart Summaries)
            0 -> {
                // Header with Expand/Collapse All
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TASK GROUPS (${taskGroups.size})",
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
                            text = if (allExpanded) "Collapse All" else "Expand All",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }

                if (tasks.isEmpty()) {
                    EmptyTasksPlaceholder()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(taskGroups, key = { it.id }) { group ->
                            val rawGroupTasks = tasks.filter(group.taskFilter)
                            val processedGroupTasks = processTaskList(rawGroupTasks)
                            val isExpanded = expandedGroups[group.id] ?: true

                            TaskGroupCard(
                                group = group,
                                rawTasks = rawGroupTasks,
                                displayedTasks = processedGroupTasks,
                                isExpanded = isExpanded,
                                onToggleExpand = { expandedGroups[group.id] = !isExpanded },
                                onToggleCompletion = onToggleCompletion,
                                onDeleteTask = onDeleteTask
                            )
                        }
                    }
                }
            }

            // TAB 1: Priority & Urgent (High → Med → Low)
            1 -> {
                val priorityTasks = processTaskList(tasks.sortedWith(
                    compareBy<NoteTask> { it.isCompleted }
                        .thenByDescending { it.priority }
                        .thenByDescending { it.timestampMillis }
                ))

                if (priorityTasks.isEmpty()) {
                    EmptyTasksPlaceholder(message = "No priority tasks matching current filter")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(priorityTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggleCompletion = onToggleCompletion,
                                onDeleteTask = onDeleteTask
                            )
                        }
                    }
                }
            }

            // TAB 2: Easy Wins (Low-Hanging Fruits)
            2 -> {
                val easyTasks = processTaskList(lowHangingFruits)

                if (easyTasks.isEmpty()) {
                    EmptyTasksPlaceholder(message = "No quick wins detected yet")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(easyTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggleCompletion = onToggleCompletion,
                                onDeleteTask = onDeleteTask
                            )
                        }
                    }
                }
            }

            // TAB 3: By Category Tags
            3 -> {
                Column {
                    // Category Chips Bar
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(categoryTags) { cat ->
                            val count = tasks.count { it.categoryGroup.equals(cat, ignoreCase = true) }
                            FilterChip(
                                selected = selectedCategoryTag == cat,
                                onClick = { selectedCategoryTag = cat },
                                label = { Text("$cat ($count)", fontSize = 12.sp) },
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

                    val catTasks = processTaskList(tasks.filter { it.categoryGroup.equals(selectedCategoryTag, ignoreCase = true) })

                    if (catTasks.isEmpty()) {
                        EmptyTasksPlaceholder(message = "No tasks found under $selectedCategoryTag")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(catTasks, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    onToggleCompletion = onToggleCompletion,
                                    onDeleteTask = onDeleteTask
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
 * Collapsible Card representing one intelligent group of tasks (e.g. Urgent, Work, Shopping)
 * Features a smart summary preview line when collapsed or expanded.
 */
@Composable
fun TaskGroupCard(
    group: TaskGroupDefinition,
    rawTasks: List<NoteTask>,
    displayedTasks: List<NoteTask>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    val totalInGroup = rawTasks.size
    val pendingInGroup = rawTasks.count { !it.isCompleted }
    val completedInGroup = totalInGroup - pendingInGroup

    // Generate smart group summary line
    val smartSummary = remember(rawTasks) {
        if (rawTasks.isEmpty()) {
            "No active tasks in this group"
        } else {
            val highCount = rawTasks.count { it.priority == NoteTask.PRIORITY_HIGH && !it.isCompleted }
            val firstPending = rawTasks.firstOrNull { !it.isCompleted }?.title ?: rawTasks.first().title
            buildString {
                if (highCount > 0) append("$highCount high priority • ")
                append("$pendingInGroup pending")
                if (completedInGroup > 0) append(" • $completedInGroup done")
                append(" • \"${firstPending.take(35)}${if (firstPending.length > 35) "..." else ""}\"")
            }
        }
    }

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
            // Group Header Row (Tappable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = group.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Group Count Badge
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

            // Smart Summary Line (Always visible so user has context at a glance)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = smartSummary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Collapsible Task List
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (displayedTasks.isEmpty()) {
                        Text(
                            text = if (rawTasks.isEmpty()) "No tasks in this group yet." else "No tasks match the active filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedTasks.forEach { task ->
                                TaskCard(
                                    task = task,
                                    onToggleCompletion = onToggleCompletion,
                                    onDeleteTask = onDeleteTask
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
 * High-precision task card item with priority indicator, category tag, and completion toggle
 */
@Composable
fun TaskCard(
    task: NoteTask,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    val priorityColor = when (task.priority) {
        NoteTask.PRIORITY_HIGH -> ActiveRed
        NoteTask.PRIORITY_MEDIUM -> Color(0xFFF59E0B)
        else -> PrimaryBlue
    }

    val priorityText = when (task.priority) {
        NoteTask.PRIORITY_HIGH -> "HIGH"
        NoteTask.PRIORITY_MEDIUM -> "MED"
        else -> "LOW"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (task.priority == NoteTask.PRIORITY_HIGH && !task.isCompleted) ActiveRed.copy(alpha = 0.5f) else CardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            IconButton(
                onClick = { onToggleCompletion(task.id, !task.isCompleted) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) PrimaryBlue else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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

                if (task.description.isNotBlank() && task.description != task.title) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tag Chips Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = priorityText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            fontSize = 9.sp
                        )
                    }

                    // Category Tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AccentPurple.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = task.categoryGroup,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPurple,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                            fontSize = 9.sp
                        )
                    }

                    // Quick Win Tag
                    if (task.isLowHangingFruit) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "⚡ QUICK WIN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Delete Task Button
            IconButton(
                onClick = { onDeleteTask(task.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTasksPlaceholder(
    message: String = "No tasks or action items found"
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Record continuous audio or import a file to synthesize tasks automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
