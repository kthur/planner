package com.planner.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.components.CategorySelector
import com.planner.tracker.ui.components.TimePickerDialogScreen
import com.planner.tracker.ui.components.EntryCard
import com.planner.tracker.ui.theme.Accent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    categories: List<CategoryEntity>,
    selectedDate: Long,
    entries: List<Entry>,
    onAddEntry: (String, Int, String, Long, Long, String) -> Unit,
    onDeleteEntry: (Entry) -> Unit,
    onUpdateEntry: (Entry) -> Unit,
    isTracking: Boolean = false,
    elapsedSeconds: Long = 0,
    alarmTriggered: Boolean = false,
    restoredNote: String = "",
    restoredCategories: Set<String> = emptySet(),
    onStartTracking: (Set<String>, String, String, String) -> Unit = { _, _, _, _ -> },
    onStopTrackingAndSave: (Set<String>, String) -> Pair<Long, Long>? = { _, _ -> null },
    onCancelTracking: () -> Unit = {},
    onClearAlarm: () -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onCategoriesChange: (Set<String>, String) -> Unit = { _, _ -> }
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var selectedCategories by remember(categories) { mutableStateOf(categories.firstOrNull()?.name?.let { setOf(it) } ?: emptySet()) }
    var note by remember { mutableStateOf("") }

    val categoryInfoMap = remember(categories) { categories.associateBy { it.name } }
val categoryDisplayStr = remember(selectedCategories, categoryInfoMap) {
    selectedCategories.mapNotNull { categoryInfoMap[it]?.displayName }.joinToString(", ")
}

    LaunchedEffect(restoredCategories) {
        if (restoredCategories.isNotEmpty()) {
            selectedCategories = restoredCategories
        }
    }

    LaunchedEffect(restoredNote) {
        if (restoredNote.isNotEmpty()) {
            note = restoredNote
        }
    }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var durationH by remember { mutableStateOf("") }
    var durationM by remember { mutableStateOf("") }
    var directInputSubMode by remember { mutableStateOf(0) } // 0: 소요시간 입력, 1: 기간 설정, 2: 이벤트 기록
    var eventTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showEventTimePicker by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<Entry?>(null) }
    var editStartTime by remember(editingEntry) { mutableLongStateOf(editingEntry?.startTime ?: 0L) }
    var editEndTime by remember(editingEntry) { mutableLongStateOf(editingEntry?.endTime ?: 0L) }
    var showEditStartTimePicker by remember { mutableStateOf(false) }
    var showEditEndTimePicker by remember { mutableStateOf(false) }
    var deleteConfirmEntry by remember { mutableStateOf<Entry?>(null) }
    var timerMinutes by remember { mutableStateOf("") }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val cal = remember { Calendar.getInstance() }

    // 입력 패널 시간 초기화
    LaunchedEffect(Unit) {
        val nowCal = Calendar.getInstance()
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val currentMin = nowCal.get(Calendar.MINUTE)
        cal.timeInMillis = selectedDate
        cal.set(Calendar.HOUR_OF_DAY, currentHour)
        cal.set(Calendar.MINUTE, currentMin)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        endTime = cal.timeInMillis
        eventTime = cal.timeInMillis
        cal.add(Calendar.HOUR, -1)
        startTime = cal.timeInMillis
    }

    LaunchedEffect(alarmTriggered) {
        if (alarmTriggered) showAlarmDialog = true
    }

    fun calcMinutesFromTimestamps(): Int {
        if (inputMode && directInputSubMode == 0) {
            val dh = durationH.toIntOrNull() ?: 0
            val dm = durationM.toIntOrNull() ?: 0
            return dh * 60 + dm
        }
        if (startTime > 0 && endTime > 0 && endTime > startTime) {
            return ((endTime - startTime) / 60000).toInt()
        }
        return 0
    }

    if (showStartTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = startTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                cal.timeInMillis = startTime
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startTime = cal.timeInMillis
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = endTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                cal.timeInMillis = endTime
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                endTime = cal.timeInMillis
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }

    if (showEventTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = eventTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                cal.timeInMillis = eventTime
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                eventTime = cal.timeInMillis
                showEventTimePicker = false
            },
            onDismiss = { showEventTimePicker = false }
        )
    }

    if (showEditStartTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = editStartTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                cal.timeInMillis = editStartTime
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                editStartTime = cal.timeInMillis
                showEditStartTimePicker = false
            },
            onDismiss = { showEditStartTimePicker = false }
        )
    }

    if (showEditEndTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = editEndTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                cal.timeInMillis = editEndTime
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                editEndTime = cal.timeInMillis
                showEditEndTimePicker = false
            },
            onDismiss = { showEditEndTimePicker = false }
        )
    }

    editingEntry?.let { entry ->
        val editCat = remember(entry) { mutableStateOf(entry.category) }
        val editNote = remember(entry) { mutableStateOf(entry.note) }
        val isEvent = entry.entryType == "EVENT"

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("항목 수정") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CategorySelector(categories = categories, selected = editCat.value, onSelect = { editCat.value = it })
                    Spacer(modifier = Modifier.height(8.dp))

                    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    if (isEvent) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showEditStartTimePicker = true },
                                modifier = Modifier.width(180.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("발생 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(timeFormat.format(Date(editStartTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showEditStartTimePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("시작 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(timeFormat.format(Date(editStartTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showEditEndTimePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("종료 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(timeFormat.format(Date(editEndTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editNote.value, onValueChange = { editNote.value = it }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isEvent) {
                        onUpdateEntry(entry.copy(category = editCat.value, note = editNote.value, minutes = 0, startTime = editStartTime, endTime = 0L))
                    } else {
                        val mins = if (editEndTime > editStartTime) ((editEndTime - editStartTime) / 60000).toInt() else 0
                        onUpdateEntry(entry.copy(category = editCat.value, note = editNote.value, minutes = mins, startTime = editStartTime, endTime = editEndTime))
                    }
                    editingEntry = null
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { editingEntry = null }) { Text("취소") } }
        )
    }



    deleteConfirmEntry?.let { entry ->
        val catDisplay = categoryInfoMap[entry.category]?.displayName ?: entry.category
        AlertDialog(
            onDismissRequest = { deleteConfirmEntry = null },
            title = { Text("항목 삭제") },
            text = { Text("\"${catDisplay}\" 항목을 삭제하시겠습니까?") },
            confirmButton = { TextButton(onClick = { onDeleteEntry(entry); deleteConfirmEntry = null }) { Text("삭제", color = Accent) } },
            dismissButton = { TextButton(onClick = { deleteConfirmEntry = null }) { Text("취소") } }
        )
    }

    if (alarmTriggered && showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false; onClearAlarm() },
            title = { Text("⏰ 타이머 완료") },
            text = { Text("설정한 ${timerMinutes}분이 지났습니다!") },
            confirmButton = { TextButton(onClick = { showAlarmDialog = false; onClearAlarm() }) { Text("확인") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // ── 상단 고정 입력/측정 패널 ──
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isTracking) {
                    // 측정 중 대시보드
                    val displaySeconds = if (timerMinutes.toIntOrNull() ?: 0 > 0) {
                        val totalSecs = (timerMinutes.toIntOrNull() ?: 0) * 60
                        maxOf(0L, totalSecs - elapsedSeconds)
                    } else {
                        elapsedSeconds
                    }
                    val bH = displaySeconds / 3600; val bM = (displaySeconds % 3600) / 60; val bS = displaySeconds % 60
                    Text(text = "⏱ 측정 중", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%02d:%02d:%02d", bH, bM, bS),
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text("메모 (선택사항)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val timestamps = onStopTrackingAndSave(selectedCategories, note)
                            if (timestamps != null) { note = "" }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("종료 및 저장")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onCancelTracking() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("측정 취소", color = Accent)
                    }
                } else {
                    // 기록 추가 입력 패널
                    CategorySelector(categories = categories, selected = selectedCategories, onSelectChange = { selectedCategories = it })
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { inputMode = false }, colors = ButtonDefaults.textButtonColors(contentColor = if (!inputMode) Accent else MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text("실시간 측정", fontWeight = if (!inputMode) FontWeight.Bold else FontWeight.Normal)
                        }
                        TextButton(onClick = { inputMode = true }, colors = ButtonDefaults.textButtonColors(contentColor = if (inputMode) Accent else MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text("직접 입력", fontWeight = if (inputMode) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (inputMode) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { directInputSubMode = 0 }, colors = ButtonDefaults.textButtonColors(contentColor = if (directInputSubMode == 0) Accent else MaterialTheme.colorScheme.onSurfaceVariant)) {
                                Text("소요 시간 입력", fontWeight = if (directInputSubMode == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                            TextButton(onClick = { directInputSubMode = 1 }, colors = ButtonDefaults.textButtonColors(contentColor = if (directInputSubMode == 1) Accent else MaterialTheme.colorScheme.onSurfaceVariant)) {
                                Text("기간 설정", fontWeight = if (directInputSubMode == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                            TextButton(onClick = { directInputSubMode = 2 }, colors = ButtonDefaults.textButtonColors(contentColor = if (directInputSubMode == 2) Accent else MaterialTheme.colorScheme.onSurfaceVariant)) {
                                Text("이벤트 기록", fontWeight = if (directInputSubMode == 2) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (directInputSubMode == 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = durationH, onValueChange = { durationH = it }, label = { Text("시간") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = durationM, onValueChange = { durationM = it }, label = { Text("분") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                            }
                        } else if (directInputSubMode == 1) {
                            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showStartTimePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("시작 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(timeFormat.format(Date(startTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { showEndTimePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("종료 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(timeFormat.format(Date(endTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                                    }
                                }
                            }
                        } else {
                            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showEventTimePicker = true },
                                    modifier = Modifier.width(180.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("발생 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(timeFormat.format(Date(eventTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (directInputSubMode != 2) {
                            val minutes = calcMinutesFromTimestamps()
                            Text(text = if (minutes > 0) "소요 시간: ${minutes / 60}시간 ${minutes % 60}분" else "시간을 입력하세요", color = if (minutes > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (minutes > 0) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("메모 (선택사항)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            if (selectedCategories.isNotEmpty()) {
                                selectedCategories.forEach { cat ->
                                    if (directInputSubMode == 2) {
                                        onAddEntry(cat, 0, note, eventTime, 0L, "EVENT")
                                    } else {
                                        val mins = calcMinutesFromTimestamps()
                                        if (mins > 0) {
                                            if (directInputSubMode == 0) {
                                                onAddEntry(cat, mins, note, 0L, 0L, "DURATION")
                                            } else {
                                                onAddEntry(cat, mins, note, startTime, endTime, "DURATION")
                                            }
                                        }
                                    }
                                }
                                if (directInputSubMode == 2) {
                                    note = ""
                                } else {
                                    val mins = calcMinutesFromTimestamps()
                                    if (mins > 0) {
                                        durationH = ""; durationM = ""; note = ""
                                    }
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) {
                            Text("저장")
                        }
                    } else {
                        // 실시간 측정 모드
                        val h = elapsedSeconds / 3600; val m = (elapsedSeconds % 3600) / 60; val s = elapsedSeconds % 60
                        Text(text = "⏱ ${String.format("%02d:%02d:%02d", h, m, s)}", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = timerMinutes, onValueChange = { timerMinutes = it.filter { c -> c.isDigit() } }, label = { Text("알림 타이머 설정 (분)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, enabled = !isTracking)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("메모 (선택사항)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            val now = Calendar.getInstance()
                            cal.timeInMillis = System.currentTimeMillis()
                            cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            startTime = cal.timeInMillis
                            onStartTracking(selectedCategories, categoryDisplayStr, note, timerMinutes)
                        }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("시작")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 기록 목록 ──
        val totalDayMinutes = entries.sumOf { it.minutes }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("기록된 항목", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (totalDayMinutes > 0) Text(text = "총 ${totalDayMinutes}분 (${totalDayMinutes / 60}시간 ${totalDayMinutes % 60}분)", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("검색...", color = MaterialTheme.colorScheme.onSurfaceVariant) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        val filteredEntries = remember(entries, searchQuery) {
            if (searchQuery.isBlank()) entries
            else entries.filter { e ->
                val disp = categoryInfoMap[e.category]?.displayName ?: e.category
                e.note.contains(searchQuery, ignoreCase = true) || disp.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filteredEntries.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (searchQuery.isNotBlank()) {
                        Text(text = "검색 결과가 없습니다", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("📝", style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "아직 기록이 없습니다", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "위의 패널에서\n새로운 항목을 추가해보세요", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredEntries, key = { it.id }) { entry ->
                    EntryCard(entry = entry, categoryInfo = categoryInfoMap, onDelete = { deleteConfirmEntry = entry }, onEdit = { editingEntry = entry })
                }
            }
        }
    }
}
