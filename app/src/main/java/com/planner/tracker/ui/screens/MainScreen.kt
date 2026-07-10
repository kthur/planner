package com.planner.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import com.planner.tracker.ui.components.PhotoActionDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.components.CategorySelector
import com.planner.tracker.ui.components.TimePickerDialogScreen
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.categoryColorFromHex
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    categories: List<CategoryEntity>,
    selectedDate: Long,
    onAddEntry: (String, Int, String, Long, Long, String, Int, String?) -> Unit,
    isTracking: Boolean = false,
    elapsedSeconds: Long = 0,
    alarmTriggered: Boolean = false,
    restoredNote: String = "",
    restoredCategories: Set<String> = emptySet(),
    restoredPhotoUri: String? = null,
    onStartTracking: (Set<String>, String, String, String, String?) -> Unit = { _, _, _, _, _ -> },
    onStopTrackingAndSave: (Set<String>, String, String?) -> Pair<Long, Long>? = { _, _, _ -> null },
    onCancelTracking: () -> Unit = {},
    onClearAlarm: () -> Unit = {}
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var note by remember { mutableStateOf("") }

    val categoryInfoMap = remember(categories) { categories.associateBy { it.name } }
    val categoryDisplayStr = remember(selectedCategory, categoryInfoMap) {
        categoryInfoMap[selectedCategory]?.displayName ?: selectedCategory
    }

    LaunchedEffect(restoredCategories) {
        if (restoredCategories.isNotEmpty()) {
            selectedCategory = restoredCategories.firstOrNull() ?: selectedCategory
        }
    }

    LaunchedEffect(restoredNote) {
        if (restoredNote.isNotEmpty()) {
            note = restoredNote
        }
    }

    val context = LocalContext.current
    var selectedPhotoName by remember { mutableStateOf<String?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var tempPhotoPath by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(restoredPhotoUri) {
        if (restoredPhotoUri != null) {
            selectedPhotoName = restoredPhotoUri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempPhotoPath.isNotEmpty()) {
            val tempFile = java.io.File(tempPhotoPath)
            if (tempFile.exists()) {
                val photosDir = java.io.File(context.filesDir, "photos")
                val finalFile = java.io.File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
                if (tempFile.renameTo(finalFile)) {
                    selectedPhotoName = finalFile.name
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val photosDir = java.io.File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()
            val finalFile = java.io.File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(finalFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                selectedPhotoName = finalFile.name
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val clearSelectedPhoto = {
        selectedPhotoName?.let { filename ->
            val file = java.io.File(java.io.File(context.filesDir, "photos"), filename)
            if (file.exists()) file.delete()
        }
        selectedPhotoName = null
    }

    if (showPhotoDialog) {
        PhotoActionDialog(
            onDismiss = { showPhotoDialog = false },
            onTakePhoto = {
                val photosDir = java.io.File(context.filesDir, "photos")
                if (!photosDir.exists()) photosDir.mkdirs()
                val tempFile = java.io.File(photosDir, "temp_${System.currentTimeMillis()}.jpg")
                tempPhotoPath = tempFile.absolutePath
                val authority = "${context.packageName}.fileprovider"
                val providerUri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
                cameraLauncher.launch(providerUri)
            },
            onPickPhoto = {
                galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var durationH by remember { mutableStateOf("") }
    var durationM by remember { mutableStateOf("") }
    var directInputSubMode by remember { mutableStateOf(0) } // 0: 소요시간, 1: 기간, 2: 이벤트, 3: 횟수
    var eventTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showEventTimePicker by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableStateOf("") }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf(false) }
    var countValue by remember { mutableStateOf("1") }
    val cal = remember { Calendar.getInstance() }

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

    LaunchedEffect(selectedCategory, categoryInfoMap) {
        val catType = categoryInfoMap[selectedCategory]?.entryType ?: "DURATION"
        when (catType) {
            "COUNT" -> { inputMode = true; directInputSubMode = 3 }
            "DURATION" -> { inputMode = false; directInputSubMode = 0 }
            "BOTH" -> { inputMode = false; directInputSubMode = 0 }
        }
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

    if (alarmTriggered && showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false; onClearAlarm() },
            title = { Text("⏰ 타이머 완료") },
            text = { Text("설정한 ${timerMinutes}분이 지났습니다!") },
            confirmButton = { TextButton(onClick = { showAlarmDialog = false; onClearAlarm() }) { Text("확인") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
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
                    Text(text = "⏱ [$categoryDisplayStr] 측정 중", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showPhotoDialog = true }) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "사진 추가", tint = Accent)
                            }
                        }
                    )
                    if (selectedPhotoName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val file = remember(selectedPhotoName) {
                                java.io.File(java.io.File(context.filesDir, "photos"), selectedPhotoName!!)
                            }
                            if (file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "첨부 사진",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("사진 첨부됨", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { clearSelectedPhoto() }) {
                                    Icon(Icons.Default.Close, contentDescription = "삭제", tint = Accent)
                                }
                            } else {
                                selectedPhotoName = null
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val timestamps = onStopTrackingAndSave(if (selectedCategory.isNotEmpty()) setOf(selectedCategory) else emptySet(), note, selectedPhotoName)
                            if (timestamps != null) { note = ""; selectedPhotoName = null }
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
                        onClick = { onCancelTracking(); clearSelectedPhoto() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("측정 취소", color = Accent)
                    }
                } else {
                    // 기록 추가 입력 패널
                    CategorySelector(categories = categories, selected = selectedCategory, onSelect = { selectedCategory = it })
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { directInputSubMode = 0 }, colors = ButtonDefaults.textButtonColors(contentColor = if (directInputSubMode == 0) Accent else MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text("소요 시간", fontWeight = if (directInputSubMode == 0) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { directInputSubMode = 1 }, colors = ButtonDefaults.textButtonColors(contentColor = if (directInputSubMode == 1) Accent else MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text("기간", fontWeight = if (directInputSubMode == 1) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { directInputSubMode = 2 }, colors = ButtonDefaults.textButtonColors(contentColor = if (directInputSubMode == 2) Accent else MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text("이벤트", fontWeight = if (directInputSubMode == 2) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { directInputSubMode = 3 }, colors = ButtonDefaults.textButtonColors(contentColor = if (directInputSubMode == 3) Accent else MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text("횟수", fontWeight = if (directInputSubMode == 3) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (directInputSubMode == 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = durationH, onValueChange = { durationH = it }, label = { Text("시간") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = durationM, onValueChange = { durationM = it }, label = { Text("분") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                            }
                        } else if (directInputSubMode == 3) {
                            val count = countValue.toIntOrNull() ?: 1
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = {
                                    val v = (countValue.toIntOrNull() ?: 1) - 1
                                    if (v >= 0) countValue = v.toString()
                                }) {
                                    Icon(Icons.Default.Remove, "감소", tint = Accent)
                                }
                                Text(text = if (count > 0) "${count}회" else "0회", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, color = Accent, modifier = Modifier.padding(horizontal = 24.dp))
                                IconButton(onClick = {
                                    countValue = ((countValue.toIntOrNull() ?: 0) + 1).toString()
                                }) {
                                    Icon(Icons.Default.Add, "증가", tint = Accent)
                                }
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
                        if (directInputSubMode != 2 && directInputSubMode != 3) {
                            val minutes = calcMinutesFromTimestamps()
                            Text(text = if (minutes > 0) "소요 시간: ${minutes / 60}시간 ${minutes % 60}분" else "시간을 입력하세요", color = if (minutes > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (minutes > 0) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = note, onValueChange = { note = it },
                            label = { Text("메모 (선택사항)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showPhotoDialog = true }) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "사진 추가", tint = Accent)
                                }
                            }
                        )
                        if (selectedPhotoName != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val file = remember(selectedPhotoName) {
                                    java.io.File(java.io.File(context.filesDir, "photos"), selectedPhotoName!!)
                                }
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "첨부 사진",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("사진 첨부됨", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { clearSelectedPhoto() }) {
                                        Icon(Icons.Default.Close, contentDescription = "삭제", tint = Accent)
                                    }
                                } else {
                                    selectedPhotoName = null
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            val cat = selectedCategory
                            if (cat.isNotEmpty()) {
                                if (directInputSubMode == 2) {
                                    onAddEntry(cat, 0, note, eventTime, 0L, "EVENT", 0, selectedPhotoName)
                                    note = ""; selectedPhotoName = null
                                } else if (directInputSubMode == 3) {
                                    val cnt = countValue.toIntOrNull() ?: 1
                                    if (cnt > 0) {
                                        onAddEntry(cat, 0, note, 0L, 0L, "COUNT", cnt, selectedPhotoName)
                                        note = ""; countValue = "1"; selectedPhotoName = null
                                    }
                                } else {
                                    val mins = calcMinutesFromTimestamps()
                                    if (mins > 0) {
                                        if (directInputSubMode == 0) {
                                            onAddEntry(cat, mins, note, 0L, 0L, "DURATION", 0, selectedPhotoName)
                                        } else {
                                            onAddEntry(cat, mins, note, startTime, endTime, "DURATION", 0, selectedPhotoName)
                                        }
                                        durationH = ""; durationM = ""; note = ""; selectedPhotoName = null
                                    }
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) {
                            Text("저장")
                        }
                    } else {
                        // 실시간 측정 모드
                        val h = elapsedSeconds / 3600; val m = (elapsedSeconds % 3600) / 60; val s = elapsedSeconds % 60
                        val timerProgress = if (timerMinutes.toIntOrNull() ?: 0 > 0) elapsedSeconds.toFloat() / ((timerMinutes.toIntOrNull() ?: 1) * 60) else 0f
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            if ((timerMinutes.toIntOrNull() ?: 0) > 0) {
                                Canvas(modifier = Modifier.size(140.dp)) {
                                    val stroke = 8f
                                    val sz = this.size
                                    drawArc(color = Accent.copy(alpha = 0.15f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round), size = Size(sz.width - stroke, sz.height - stroke), topLeft = Offset(stroke / 2f, stroke / 2f))
                                    drawArc(color = Accent, startAngle = -90f, sweepAngle = (timerProgress.coerceIn(0f, 1f) * 360f), useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round), size = Size(sz.width - stroke, sz.height - stroke), topLeft = Offset(stroke / 2f, stroke / 2f))
                                }
                            }
                            Text(text = "⏱ ${String.format("%02d:%02d:%02d", h, m, s)}", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = timerMinutes, onValueChange = { timerMinutes = it.filter { c -> c.isDigit() } }, label = { Text("알림 타이머 설정 (분)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, enabled = !isTracking)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = note, onValueChange = { note = it },
                            label = { Text("메모 (선택사항)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showPhotoDialog = true }) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "사진 추가", tint = Accent)
                                }
                            }
                        )
                        if (selectedPhotoName != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val file = remember(selectedPhotoName) {
                                    java.io.File(java.io.File(context.filesDir, "photos"), selectedPhotoName!!)
                                }
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "첨부 사진",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("사진 첨부됨", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { clearSelectedPhoto() }) {
                                        Icon(Icons.Default.Close, contentDescription = "삭제", tint = Accent)
                                    }
                                } else {
                                    selectedPhotoName = null
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            val now = Calendar.getInstance()
                            cal.timeInMillis = System.currentTimeMillis()
                            cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            startTime = cal.timeInMillis
                            onStartTracking(if (selectedCategory.isNotEmpty()) setOf(selectedCategory) else emptySet(), categoryDisplayStr, note, timerMinutes, selectedPhotoName)
                        }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("시작")
                        }
                    }
                }
            }
        }
    }
}
