package com.planner.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.Category
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.components.CategorySelector
import com.planner.tracker.ui.components.DatePickerDialogScreen
import com.planner.tracker.ui.components.EntryCard
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.CardBackground
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    selectedDate: Long,
    entries: List<Entry>,
    onDateSelected: (Long) -> Unit,
    onAddEntry: (Category, Int, String, Long, Long) -> Unit,
    onDeleteEntry: (Entry) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(Category.HEALTH) }
    var note by remember { mutableStateOf("") }
    var isTracking by remember { mutableStateOf(false) }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var manualStartH by remember { mutableStateOf("") }
    var manualStartM by remember { mutableStateOf("") }
    var manualEndH by remember { mutableStateOf("") }
    var manualEndM by remember { mutableStateOf("") }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN) }

    val cal = remember { Calendar.getInstance() }

    fun calcMinutes(): Int {
        val sh = manualStartH.toIntOrNull() ?: return 0
        val sm = manualStartM.toIntOrNull() ?: return 0
        val eh = manualEndH.toIntOrNull() ?: return 0
        val em = manualEndM.toIntOrNull() ?: return 0
        return (eh * 60 + em) - (sh * 60 + sm)
    }

    if (showDatePicker) {
        DatePickerDialogScreen(
            currentDate = selectedDate,
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val minutes = calcMinutes()
                    if (minutes > 0) {
                        cal.timeInMillis = System.currentTimeMillis()
                        cal.set(Calendar.HOUR_OF_DAY, manualStartH.toIntOrNull() ?: 0)
                        cal.set(Calendar.MINUTE, manualStartM.toIntOrNull() ?: 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val sTime = cal.timeInMillis
                        cal.set(Calendar.HOUR_OF_DAY, manualEndH.toIntOrNull() ?: 0)
                        cal.set(Calendar.MINUTE, manualEndM.toIntOrNull() ?: 0)
                        val eTime = cal.timeInMillis
                        onAddEntry(selectedCategory, minutes, note, sTime, eTime)
                        manualStartH = ""
                        manualStartM = ""
                        manualEndH = ""
                        manualEndM = ""
                        note = ""
                    }
                },
                containerColor = Accent
            ) {
                Icon(Icons.Default.Add, contentDescription = "추가", tint = TextPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dateFormat.format(Date(selectedDate)),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "날짜 선택",
                        tint = Accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "기록 추가",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CategorySelector(
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualStartH,
                            onValueChange = { manualStartH = it },
                            label = { Text("시작 시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", color = TextSecondary)
                        OutlinedTextField(
                            value = manualStartM,
                            onValueChange = { manualStartM = it },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualEndH,
                            onValueChange = { manualEndH = it },
                            label = { Text("종료 시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", color = TextSecondary)
                        OutlinedTextField(
                            value = manualEndM,
                            onValueChange = { manualEndM = it },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val minutes = calcMinutes()
                    val displayH = minutes / 60
                    val displayM = minutes % 60
                    Text(
                        text = if (minutes > 0) "소요 시간: ${displayH}시간 ${displayM}분" else "시간을 입력하세요",
                        color = if (minutes > 0) TextPrimary else TextSecondary,
                        fontWeight = if (minutes > 0) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val now = Calendar.getInstance()
                                manualStartH = now.get(Calendar.HOUR_OF_DAY).toString()
                                manualStartM = now.get(Calendar.MINUTE).toString()
                                isTracking = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("지금 시작")
                        }
                        Button(
                            onClick = {
                                val now = Calendar.getInstance()
                                manualEndH = now.get(Calendar.HOUR_OF_DAY).toString()
                                manualEndM = now.get(Calendar.MINUTE).toString()
                                isTracking = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("지금 종료")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("메모 (선택사항)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "기록된 항목",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Text(
                    text = "이 날짜에 기록된 항목이 없습니다.",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onDelete = { onDeleteEntry(entry) }
                    )
                }
            }
        }
    }
}
