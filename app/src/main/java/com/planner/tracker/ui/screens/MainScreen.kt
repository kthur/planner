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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.planner.tracker.ui.components.DatePickerDialogScreen
import com.planner.tracker.ui.components.EntryCard
import com.planner.tracker.ui.components.TimePickerDialogScreen
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
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
    onDateSelected: (Long) -> Unit,
    onAddEntry: (String, Int, String, Long, Long) -> Unit,
    onDeleteEntry: (Entry) -> Unit,
    onUpdateEntry: (Entry) -> Unit,
    isTracking: Boolean = false,
    elapsedSeconds: Long = 0,
    alarmTriggered: Boolean = false,
    onStartTracking: (String) -> Unit = {},
    onStopTrackingAndSave: (String, String) -> Pair<Long, Long>? = { _, _ -> null },
    onCancelTracking: () -> Unit = {},
    onClearAlarm: () -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var note by remember { mutableStateOf("") }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var manualStartH by remember { mutableStateOf("") }
    var manualStartM by remember { mutableStateOf("") }
    var manualEndH by remember { mutableStateOf("") }
    var manualEndM by remember { mutableStateOf("") }
    var editingEntry by remember { mutableStateOf<Entry?>(null) }
    var deleteConfirmEntry by remember { mutableStateOf<Entry?>(null) }
    var timerMinutes by remember { mutableStateOf("") }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    val dateFormat = remember { SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN) }
    val cal = remember { Calendar.getInstance() }

    LaunchedEffect(alarmTriggered) {
        if (alarmTriggered) showAlarmDialog = true
    }

    fun calcMinutesFromTimestamps(): Int {
        if (startTime > 0 && endTime > 0 && endTime > startTime) {
            return ((endTime - startTime) / 60000).toInt()
        }
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

    if (showStartTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = startTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                manualStartH = h.toString()
                manualStartM = m.toString()
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
                manualEndH = h.toString()
                manualEndM = m.toString()
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

    editingEntry?.let { entry ->
        val editCat = remember(entry) { mutableStateOf(entry.category) }
        val editNote = remember(entry) { mutableStateOf(entry.note) }
        val editStartH = remember(entry) { mutableStateOf(
            if (entry.startTime > 0) {
                Calendar.getInstance().apply { timeInMillis = entry.startTime }.get(Calendar.HOUR_OF_DAY).toString()
            } else ""
        ) }
        val editStartM = remember(entry) { mutableStateOf(
            if (entry.startTime > 0) {
                Calendar.getInstance().apply { timeInMillis = entry.startTime }.get(Calendar.MINUTE).toString()
            } else ""
        ) }
        val editEndH = remember(entry) { mutableStateOf(
            if (entry.endTime > 0) {
                Calendar.getInstance().apply { timeInMillis = entry.endTime }.get(Calendar.HOUR_OF_DAY).toString()
            } else ""
        ) }
        val editEndM = remember(entry) { mutableStateOf(
            if (entry.endTime > 0) {
                Calendar.getInstance().apply { timeInMillis = entry.endTime }.get(Calendar.MINUTE).toString()
            } else ""
        ) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("항목 수정") },
            text = {
                Column {
                    CategorySelector(categories = categories, selected = editCat.value, onSelect = { editCat.value = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = editStartH.value, onValueChange = { editStartH.value = it }, label = { Text("시작 시") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        Text(":", color = TextSecondary)
                        OutlinedTextField(value = editStartM.value, onValueChange = { editStartM.value = it }, label = { Text("분") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = editEndH.value, onValueChange = { editEndH.value = it }, label = { Text("종료 시") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        Text(":", color = TextSecondary)
                        OutlinedTextField(value = editEndM.value, onValueChange = { editEndM.value = it }, label = { Text("분") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editNote.value, onValueChange = { editNote.value = it }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sh = editStartH.value.toIntOrNull() ?: 0; val sm = editStartM.value.toIntOrNull() ?: 0
                    val eh = editEndH.value.toIntOrNull() ?: 0; val em = editEndM.value.toIntOrNull() ?: 0
                    cal.timeInMillis = entry.date
                    cal.set(Calendar.HOUR_OF_DAY, sh)
                    cal.set(Calendar.MINUTE, sm)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val sTime = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, eh)
                    cal.set(Calendar.MINUTE, em)
                    val eTime = cal.timeInMillis
                    val mins = if (eTime > sTime) ((eTime - sTime) / 60000).toInt() else 0
                    onUpdateEntry(entry.copy(category = editCat.value, note = editNote.value, minutes = mins, startTime = sTime, endTime = eTime))
                    editingEntry = null
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { editingEntry = null }) { Text("취소") } }
        )
    }

    val categoryInfoMap = remember(categories) { categories.associateBy { it.name } }

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

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
            ) {
                Text("기록 추가", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                CategorySelector(categories = categories, selected = selectedCategory, onSelect = { selectedCategory = it })
                Spacer(modifier = Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { inputMode = false }, colors = ButtonDefaults.textButtonColors(contentColor = if (!inputMode) Accent else TextSecondary)) {
                        Text("실시간 측정", fontWeight = if (!inputMode) FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(onClick = { inputMode = true }, colors = ButtonDefaults.textButtonColors(contentColor = if (inputMode) Accent else TextSecondary)) {
                        Text("직접 입력", fontWeight = if (inputMode) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (inputMode) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = manualStartH, onValueChange = { manualStartH = it }, label = { Text("시작 시") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        Text(":", color = TextSecondary)
                        OutlinedTextField(value = manualStartM, onValueChange = { manualStartM = it }, label = { Text("분") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showStartTimePicker = true }) { Icon(Icons.Default.CalendarMonth, contentDescription = "시작 시간 선택", tint = Accent) }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = manualEndH, onValueChange = { manualEndH = it }, label = { Text("종료 시") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        Text(":", color = TextSecondary)
                        OutlinedTextField(value = manualEndM, onValueChange = { manualEndM = it }, label = { Text("분") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showEndTimePicker = true }) { Icon(Icons.Default.CalendarMonth, contentDescription = "종료 시간 선택", tint = Accent) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val minutes = calcMinutesFromTimestamps()
                    Text(text = if (minutes > 0) "소요 시간: ${minutes / 60}시간 ${minutes % 60}분" else "시간을 입력하세요", color = if (minutes > 0) TextPrimary else TextSecondary, fontWeight = if (minutes > 0) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                } else {
                    val h = elapsedSeconds / 3600; val m = (elapsedSeconds % 3600) / 60; val s = elapsedSeconds % 60
                    Text(text = "⏱ ${String.format("%02d:%02d:%02d", h, m, s)}", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center)

                    if (!isTracking) {
                        Button(onClick = {
                            val now = Calendar.getInstance()
                            manualStartH = now.get(Calendar.HOUR_OF_DAY).toString()
                            manualStartM = now.get(Calendar.MINUTE).toString()
                            cal.timeInMillis = System.currentTimeMillis()
                            cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            startTime = cal.timeInMillis
                            onStartTracking(timerMinutes)
                        }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("시작")
                        }
                    }

                    if (isTracking) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = timerMinutes, onValueChange = { timerMinutes = it.filter { c -> c.isDigit() } }, label = { Text("알림 (분)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, enabled = false)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val timestamps = onStopTrackingAndSave(selectedCategory, note)
                            if (timestamps != null) {
                                val (sTime, eTime) = timestamps
                                val now = Calendar.getInstance().apply { timeInMillis = eTime }
                                manualEndH = now.get(Calendar.HOUR_OF_DAY).toString()
                                manualEndM = now.get(Calendar.MINUTE).toString()
                                note = ""; showBottomSheet = false
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("종료 및 저장")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("메모 (선택사항)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                if (inputMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val minutes = calcMinutesFromTimestamps()
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
                            manualStartH = ""; manualStartM = ""; manualEndH = ""; manualEndM = ""; note = ""; showBottomSheet = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) {
                        Text("저장", color = TextPrimary)
                    }
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showBottomSheet = true; inputMode = false; timerMinutes = ""
                },
                containerColor = Accent
            ) {
                Icon(Icons.Default.Add, contentDescription = "추가", tint = TextPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = dateFormat.format(Date(selectedDate)), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, contentDescription = "날짜 선택", tint = Accent) }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val totalDayMinutes = entries.sumOf { it.minutes }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("기록된 항목", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (totalDayMinutes > 0) Text(text = "총 ${totalDayMinutes}분 (${totalDayMinutes / 60}시간 ${totalDayMinutes % 60}분)", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("검색...", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                val filteredEntries = remember(entries, searchQuery) {
                    if (searchQuery.isBlank()) entries
                    else entries.filter { e ->
                        val disp = categoryInfoMap[e.category]?.displayName ?: e.category
                        e.note.contains(searchQuery, ignoreCase = true) || disp.contains(searchQuery, ignoreCase = true)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredEntries.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (searchQuery.isNotBlank()) {
                                Text(text = "검색 결과가 없습니다", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text("📝", style = MaterialTheme.typography.displaySmall)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "아직 기록이 없습니다", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "하단의 + 버튼을 눌러\n새로운 항목을 추가해보세요", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredEntries, key = { it.id }) { entry ->
                            val dismissState = rememberDismissState(
                                confirmValueChange = {
                                    if (it == DismissValue.DismissedToStart) { deleteConfirmEntry = entry }; false
                                }
                            )
                            SwipeToDismiss(
                                state = dismissState,
                                directions = setOf(DismissDirection.EndToStart),
                                background = {
                                    Box(Modifier.fillMaxSize().background(Accent.copy(alpha = 0.2f)).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                        Text("삭제", color = Accent, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissContent = {
                                    EntryCard(entry = entry, categoryInfo = categoryInfoMap, onDelete = { deleteConfirmEntry = entry }, onEdit = { editingEntry = entry })
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
