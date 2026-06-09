package com.planner.tracker.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.planner.tracker.data.Category
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.components.CategorySelector
import com.planner.tracker.ui.components.DatePickerDialogScreen
import com.planner.tracker.ui.components.EntryCard
import com.planner.tracker.ui.components.TimePickerDialogScreen
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.CardBackground
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    selectedDate: Long,
    entries: List<Entry>,
    onDateSelected: (Long) -> Unit,
    onAddEntry: (Category, Int, String, Long, Long) -> Unit,
    onDeleteEntry: (Entry) -> Unit,
    onUpdateEntry: (Entry) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(Category.HEALTH) }
    var note by remember { mutableStateOf("") }
    var isTracking by remember { mutableStateOf(false) }
    var trackingStartedAt by remember { mutableLongStateOf(0L) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
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
    var inputMode by remember { mutableStateOf(true) } // true = direct input, false = real-time tracking
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    var alarmTriggered by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN) }
    val cal = remember { Calendar.getInstance() }
    val ctx = LocalContext.current

    LaunchedEffect(isTracking, alarmTriggered) {
        if (isTracking) {
            val targetSec = timerMinutes.toIntOrNull()?.times(60) ?: 0
            alarmTriggered = false
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - trackingStartedAt) / 1000
                if (targetSec > 0 && elapsedSeconds >= targetSec && !alarmTriggered) {
                    alarmTriggered = true
                    showAlarmDialog = true
                    sendTimerNotification(ctx)
                }
                delay(1000)
            }
        }
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
                    CategorySelector(
                        selected = editCat.value,
                        onSelect = { editCat.value = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editStartH.value,
                            onValueChange = { editStartH.value = it },
                            label = { Text("시작 시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", color = TextSecondary)
                        OutlinedTextField(
                            value = editStartM.value,
                            onValueChange = { editStartM.value = it },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editEndH.value,
                            onValueChange = { editEndH.value = it },
                            label = { Text("종료 시") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", color = TextSecondary)
                        OutlinedTextField(
                            value = editEndM.value,
                            onValueChange = { editEndM.value = it },
                            label = { Text("분") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editNote.value,
                        onValueChange = { editNote.value = it },
                        label = { Text("메모") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sh = editStartH.value.toIntOrNull() ?: 0
                    val sm = editStartM.value.toIntOrNull() ?: 0
                    val eh = editEndH.value.toIntOrNull() ?: 0
                    val em = editEndM.value.toIntOrNull() ?: 0
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
                    onUpdateEntry(
                        entry.copy(
                            category = editCat.value,
                            note = editNote.value,
                            minutes = mins,
                            startTime = sTime,
                            endTime = eTime
                        )
                    )
                    editingEntry = null
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text("취소")
                }
            }
        )
    }

    deleteConfirmEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteConfirmEntry = null },
            title = { Text("항목 삭제") },
            text = { Text("\"${entry.category.displayName}\" 항목을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEntry(entry)
                    deleteConfirmEntry = null
                }) {
                    Text("삭제", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmEntry = null }) {
                    Text("취소")
                }
            }
        )
    }

    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            title = { Text("⏰ 타이머 완료") },
            text = { Text("설정한 ${timerMinutes}분이 지났습니다!") },
            confirmButton = {
                TextButton(onClick = { showAlarmDialog = false }) {
                    Text("확인")
                }
            }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { inputMode = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (inputMode) Accent else TextSecondary
                        )
                    ) {
                        Text("직접 입력", fontWeight = if (inputMode) FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(
                        onClick = { inputMode = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (!inputMode) Accent else TextSecondary
                        )
                    ) {
                        Text("실시간 측정", fontWeight = if (!inputMode) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (inputMode) {
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
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showStartTimePicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "시작 시간 선택",
                                tint = Accent
                            )
                        }
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
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showEndTimePicker = true }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "종료 시간 선택",
                                tint = Accent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val minutes = calcMinutesFromTimestamps()
                    val displayH = minutes / 60
                    val displayM = minutes % 60
                    Text(
                        text = if (minutes > 0) "소요 시간: ${displayH}시간 ${displayM}분" else "시간을 입력하세요",
                        color = if (minutes > 0) TextPrimary else TextSecondary,
                        fontWeight = if (minutes > 0) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    val h = elapsedSeconds / 3600
                    val m = (elapsedSeconds % 3600) / 60
                    val s = elapsedSeconds % 60
                    Text(
                        text = "⏱ ${String.format("%02d:%02d:%02d", h, m, s)}",
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )

                    if (!isTracking) {
                        Button(
                            onClick = {
                                val now = Calendar.getInstance()
                                manualStartH = now.get(Calendar.HOUR_OF_DAY).toString()
                                manualStartM = now.get(Calendar.MINUTE).toString()
                                cal.timeInMillis = System.currentTimeMillis()
                                cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                cal.set(Calendar.SECOND, now.get(Calendar.SECOND))
                                cal.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
                                startTime = cal.timeInMillis
                                trackingStartedAt = cal.timeInMillis
                                elapsedSeconds = 0
                                isTracking = true
                                showTrackingNotification(ctx)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("시작")
                        }
                    }

                    if (isTracking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = timerMinutes,
                                onValueChange = { timerMinutes = it.filter { c -> c.isDigit() } },
                                label = { Text("알림 (분)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = !isTracking
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val now = Calendar.getInstance()
                                manualEndH = now.get(Calendar.HOUR_OF_DAY).toString()
                                manualEndM = now.get(Calendar.MINUTE).toString()
                                cal.timeInMillis = System.currentTimeMillis()
                                cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                cal.set(Calendar.SECOND, now.get(Calendar.SECOND))
                                cal.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
                                endTime = cal.timeInMillis
                                isTracking = false
                                alarmTriggered = false
                                cancelTrackingNotification(ctx)
                                // auto-save
                                val minutes = ((endTime - startTime) / 60000).toInt()
                                if (minutes > 0) {
                                    onAddEntry(selectedCategory, minutes, note, startTime, endTime)
                                    note = ""
                                    showBottomSheet = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("종료 및 저장")
                        }
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

                if (inputMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
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
                                manualStartH = ""
                                manualStartM = ""
                                manualEndH = ""
                                manualEndM = ""
                                note = ""
                                showBottomSheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                    showBottomSheet = true
                    inputMode = !isTracking
                    timerMinutes = ""
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

            Column(modifier = Modifier.weight(1f)) {
                val totalDayMinutes = entries.sumOf { it.minutes }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "기록된 항목",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalDayMinutes > 0) {
                        Text(
                            text = "총 ${totalDayMinutes}분 (${totalDayMinutes / 60}시간 ${totalDayMinutes % 60}분)",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("검색...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                val filteredEntries = remember(entries, searchQuery) {
                    if (searchQuery.isBlank()) entries
                    else entries.filter { e ->
                        e.note.contains(searchQuery, ignoreCase = true) ||
                        e.category.displayName.contains(searchQuery, ignoreCase = true)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "검색 결과가 없습니다",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    text = "📝",
                                    style = MaterialTheme.typography.displaySmall
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "아직 기록이 없습니다",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "하단의 + 버튼을 눌러\n새로운 항목을 추가해보세요",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredEntries, key = { it.id }) { entry ->
                            val dismissState = rememberDismissState(
                                confirmValueChange = {
                                    if (it == DismissValue.DismissedToStart) {
                                        deleteConfirmEntry = entry
                                    }
                                    false
                                }
                            )
                            SwipeToDismiss(
                                state = dismissState,
                                directions = setOf(DismissDirection.EndToStart),
                                background = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Accent.copy(alpha = 0.2f))
                                            .padding(end = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text("삭제", color = Accent, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissContent = {
                                    EntryCard(
                                        entry = entry,
                                        onDelete = { deleteConfirmEntry = entry },
                                        onEdit = { editingEntry = entry }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sendTimerNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "timer_alarm")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("타이머 완료")
        .setContentText("설정한 시간이 되었습니다!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(1001, notification)
}

private fun showTrackingNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "timer_alarm")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("⏱ 트래킹 중")
        .setContentText("현재 시간을 측정하고 있습니다")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
    manager.notify(1002, notification)
}

private fun cancelTrackingNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(1002)
}
