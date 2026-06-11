package com.planner.tracker.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.Goal
import com.planner.tracker.ui.components.CategoryManageDialog
import com.planner.tracker.ui.components.CategorySelector
import com.planner.tracker.ui.components.DatePickerDialogScreen
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.categoryColorFromHex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalsScreen(
    categories: List<CategoryEntity>,
    currentYear: Int,
    currentMonth: Int,
    goals: List<Goal>,
    categoryProgress: Map<String, Pair<Int, Int>>,
    onUpsertGoal: (Goal) -> Unit,
    onDeleteGoal: (Long) -> Unit
) {
    val categoryMap = remember(categories) { categories.associateBy { it.name } }

    var showDialog by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var description by remember { mutableStateOf("") }
    var targetMinutes by remember { mutableStateOf("") }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var deadlineMillis by remember { mutableLongStateOf(0L) }
    var deleteConfirmGoalId by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    fun openCreateDialog() {
        editingGoal = null
        description = ""
        targetMinutes = ""
        selectedCategory = categories.firstOrNull()?.name ?: ""
        deadlineMillis = 0L
        showDialog = true
    }

    fun openEditDialog(goal: Goal) {
        editingGoal = goal
        description = goal.description
        selectedCategory = goal.category
        targetMinutes = if (goal.targetMinutes > 0) goal.targetMinutes.toString() else ""
        deadlineMillis = goal.deadline
        showDialog = true
    }

    if (showDeadlinePicker) {
        DatePickerDialogScreen(
            currentDate = if (deadlineMillis > 0) deadlineMillis else System.currentTimeMillis(),
            onDateSelected = { date ->
                deadlineMillis = date
                showDeadlinePicker = false
            },
            onDismiss = { showDeadlinePicker = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openCreateDialog() },
                containerColor = Accent
            ) {
                Icon(Icons.Default.Add, contentDescription = "목표 추가", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "카테고리별 진행 상황과 세부 목표를 관리하세요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (goals.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎯", style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "목표가 없습니다",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "우측 하단 + 버튼으로\n카테고리별 목표를 추가해보세요",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(goals, key = { it.id }) { goal ->
                    val catInfo = categoryMap[goal.category]
                    val color = if (catInfo != null) categoryColorFromHex(catInfo.colorHex) else Accent
                    val displayName = catInfo?.displayName ?: goal.category
                    val progress = categoryProgress[goal.category]
                    val currentMinutes = progress?.first ?: 0
                    val targetMin = goal.targetMinutes
                    val pct = if (targetMin > 0) (currentMinutes.toFloat() / targetMin).coerceIn(0f, 1f) else 0f

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = goal.isCompleted,
                                    onCheckedChange = { onUpsertGoal(goal.copy(isCompleted = it)) },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = displayName,
                                    color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else color,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (goal.description.isNotBlank()) {
                                    Text(
                                        text = "- ${goal.description}",
                                        color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { openEditDialog(goal) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "수정", tint = Accent, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { deleteConfirmGoalId = goal.id }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Accent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            if (targetMin > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = pct,
                                    color = color,
                                    trackColor = color.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${currentMinutes}분 / ${targetMin}분 (${(pct * 100).toInt()}%)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (goal.deadline > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "기한: ${dateFormat.format(Date(goal.deadline))}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val yearMonth = "${currentYear}-${String.format("%02d", currentMonth)}"
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingGoal != null) "목표 수정" else "새 목표") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CategorySelector(
                        categories = categories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("목표 내용 (예: 매일 30분 운동)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetMinutes,
                        onValueChange = { targetMinutes = it },
                        label = { Text("목표 시간 (분, 선택사항)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (deadlineMillis > 0) "기한: ${dateFormat.format(Date(deadlineMillis))}" else "기한 없음",
                            color = if (deadlineMillis > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showDeadlinePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "기한 선택", tint = Accent)
                        }
                        if (deadlineMillis > 0) {
                            TextButton(onClick = { deadlineMillis = 0L }) {
                                Text("초기화", color = Accent)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val goal = (editingGoal ?: Goal(yearMonth = yearMonth, category = selectedCategory)).copy(
                        category = selectedCategory,
                        description = description,
                        targetMinutes = targetMinutes.toIntOrNull() ?: 0,
                        deadline = deadlineMillis
                    )
                    onUpsertGoal(goal)
                    showDialog = false
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    deleteConfirmGoalId?.let { goalId ->
        AlertDialog(
            onDismissRequest = { deleteConfirmGoalId = null },
            title = { Text("목표 삭제") },
            text = { Text("이 목표를 정말 삭제하시겠습니까?") },
            confirmButton = { TextButton(onClick = { onDeleteGoal(goalId); deleteConfirmGoalId = null }) { Text("삭제", color = Accent) } },
            dismissButton = { TextButton(onClick = { deleteConfirmGoalId = null }) { Text("취소") } }
        )
    }

}
