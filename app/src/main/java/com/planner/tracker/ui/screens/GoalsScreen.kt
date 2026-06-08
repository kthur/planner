package com.planner.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.Category
import com.planner.tracker.data.Goal
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.CardBackground
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import com.planner.tracker.ui.theme.categoryColor

@Composable
fun GoalsScreen(
    currentYear: Int,
    currentMonth: Int,
    goals: List<Goal>,
    categoryProgress: Map<Category, Pair<Int, Int>>,
    onUpsertGoal: (Category, Int) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var targetHours by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${currentYear}년 ${currentMonth}월 목표",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "각 카테고리별로 월 목표 시간을 설정하고 진행 상황을 확인하세요.",
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Category.entries.forEach { category ->
            val color = categoryColor(category)
            val existingGoal = goals.find { it.category == category }
            val progress = categoryProgress[category]
            val currentMinutes = progress?.first ?: 0
            val targetMinutes = progress?.second ?: 0
            val pct = if (targetMinutes > 0) currentMinutes.toFloat() / targetMinutes else 0f

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (existingGoal != null) {
                            OutlinedButton(
                                onClick = { onDeleteGoal(existingGoal.id) },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text("삭제", color = Accent)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                editingCategory = category
                                targetHours = if (existingGoal != null) existingGoal.targetMinutes / 60 else 0
                                showDialog = true
                            }
                        ) {
                            Text(
                                if (existingGoal != null) "수정" else "설정",
                                color = color
                            )
                        }
                    }

                    if (existingGoal != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { pct.coerceIn(0f, 1f) },
                            color = color,
                            trackColor = color.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "현재: ${currentMinutes}분 (${currentMinutes / 60}시간 ${currentMinutes % 60}분)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "목표: ${targetMinutes}분 (${targetMinutes / 60}시간)",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (pct >= 1f) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✅ 목표 달성!",
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "목표가 설정되지 않았습니다.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showDialog && editingCategory != null) {
        val cat = editingCategory!!
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("${cat.displayName} 목표 설정") },
            text = {
                Column {
                    Text("월 목표 시간을 입력하세요.", color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = if (targetHours == 0) "" else targetHours.toString(),
                        onValueChange = { targetHours = it.toIntOrNull() ?: 0 },
                        label = { Text("목표 시간 (시간)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (targetHours > 0) {
                        onUpsertGoal(cat, targetHours * 60)
                        showDialog = false
                    }
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
}
