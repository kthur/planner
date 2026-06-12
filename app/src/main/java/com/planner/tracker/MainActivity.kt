package com.planner.tracker

import android.net.Uri
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.screens.GoalsScreen
import com.planner.tracker.ui.screens.MainScreen
import com.planner.tracker.ui.screens.StatsScreen
import com.planner.tracker.ui.components.CategoryManageDialog
import com.planner.tracker.ui.components.DatePickerDialogScreen
import com.planner.tracker.ui.components.CalendarSettingsDialog
import com.planner.tracker.ui.components.EntryEditDialog
import com.planner.tracker.ui.components.EntryDeleteConfirmDialog
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.PlannerTheme
import com.planner.tracker.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        calendarPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR
            )
        )

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            PlannerTheme(darkTheme = isDarkMode) {
                PlannerUI(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

private fun ComponentActivity.createNotificationChannels() {
    val timerChannel = NotificationChannel(
        "timer_alarm",
        "타이머 알림",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        enableVibration(true)
        description = "타이머가 완료되면 알림"
    }
    val trackingChannel = NotificationChannel(
        "tracking_channel",
        "트래킹",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "실시간 측정 중일 때 상태 표시"
    }
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(timerChannel)
    manager.createNotificationChannel(trackingChannel)
}

data class NavItem(val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("기록", Icons.Default.EditNote),
    NavItem("통계", Icons.Default.BarChart),
    NavItem("목표", Icons.Default.Flag)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerUI(isDarkMode: Boolean, onToggleDarkMode: () -> Unit) {
    val viewModel: PlannerViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val entries by viewModel.entriesForSelectedDate.collectAsState()
    val yearly by viewModel.currentYear.collectAsState()
    val monthly by viewModel.currentMonth.collectAsState()
    val stats by viewModel.monthlyStats.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val weeklyDailyStats by viewModel.weeklyDailyStats.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val categoryProgress by viewModel.categoryProgress.collectAsState()
    val monthlyDailyStats by viewModel.monthlyDailyStats.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val alarmTriggered by viewModel.alarmTriggered.collectAsState()
    val restoredNote by viewModel.restoredNote.collectAsState()
    val restoredCategories by viewModel.restoredCategories.collectAsState()
    val weeklyDailyCategoryStats by viewModel.weeklyDailyCategoryStats.collectAsState()
    val monthlyDailyCategoryMap by viewModel.monthlyDailyCategoryMap.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val ctx = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDataAsJson { json ->
                try {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    Toast.makeText(ctx, "내보내기 완료", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "내보내기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                viewModel.importDataFromJson(json) { success, msg ->
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(ctx, "불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showCategoryManageDialog by rememberSaveable { mutableStateOf(false) }
    var showCalendarSettingsDialog by rememberSaveable { mutableStateOf(false) }

    var selectedStatsEntry by remember { mutableStateOf<Entry?>(null) }
    var showStatsEditDialog by remember { mutableStateOf(false) }
    var showStatsDeleteConfirm by remember { mutableStateOf(false) }

    if (showCalendarSettingsDialog) {
        CalendarSettingsDialog(
            onDismiss = { showCalendarSettingsDialog = false }
        )
    }

    LaunchedEffect(selectedDate) {
        selectedStatsEntry = null
    }

    if (showStatsEditDialog) {
        selectedStatsEntry?.let { entry ->
            EntryEditDialog(
                entry = entry,
                categories = categories,
                onDismiss = { showStatsEditDialog = false },
                onConfirm = { edited ->
                    viewModel.updateEntry(edited)
                    selectedStatsEntry = null
                    showStatsEditDialog = false
                }
            )
        }
    }

    if (showStatsDeleteConfirm) {
        selectedStatsEntry?.let { entry ->
            val categoryInfoMap = remember(categories) { categories.associateBy { it.name } }
            EntryDeleteConfirmDialog(
                entry = entry,
                categoryInfoMap = categoryInfoMap,
                onDismiss = { showStatsDeleteConfirm = false },
                onConfirm = {
                    viewModel.deleteEntry(entry)
                    selectedStatsEntry = null
                    showStatsDeleteConfirm = false
                }
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialogScreen(
            currentDate = selectedDate,
            onDateSelected = { date -> viewModel.setSelectedDate(date); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showCategoryManageDialog) {
        CategoryManageDialog(
            categories = categories,
            onDismiss = { showCategoryManageDialog = false },
            onAdd = { display, hex, entryType -> viewModel.addCategory(java.util.UUID.randomUUID().toString(), display, hex, entryType) },
            onUpdate = { name, display, hex, entryType -> viewModel.updateCategory(name, display, hex, entryType) },
            onDelete = { name -> viewModel.deleteCategory(name) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (selectedTab) {
                        0 -> {
                            val dateFormat = remember { SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN) }
                            val cal = remember { Calendar.getInstance() }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    cal.timeInMillis = selectedDate
                                    cal.add(Calendar.DAY_OF_MONTH, -1)
                                    viewModel.setSelectedDate(cal.timeInMillis)
                                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 날", tint = MaterialTheme.colorScheme.onBackground) }
                                Text(
                                    text = dateFormat.format(Date(selectedDate)),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.clickable { viewModel.setSelectedDate(System.currentTimeMillis()) }
                                )
                                IconButton(onClick = {
                                    cal.timeInMillis = selectedDate
                                    cal.add(Calendar.DAY_OF_MONTH, 1)
                                    viewModel.setSelectedDate(cal.timeInMillis)
                                }) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 날", tint = MaterialTheme.colorScheme.onBackground) }
                            }
                        }
                        1 -> Text("통계", style = MaterialTheme.typography.titleLarge)
                        2 -> Text("목표", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    when (selectedTab) {
                        0 -> {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "날짜 선택", tint = Accent)
                            }
                            IconButton(onClick = { showCalendarSettingsDialog = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "캘린더 연동 설정", tint = Accent)
                            }
                        }
                        1 -> {
                            if (selectedStatsEntry != null) {
                                IconButton(onClick = { showStatsEditDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "수정", tint = Accent)
                                }
                                IconButton(onClick = { showStatsDeleteConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Accent)
                                }
                                IconButton(onClick = { selectedStatsEntry = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "선택 취소", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            } else {
                                TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("복원", color = Accent) }
                                IconButton(onClick = { exportLauncher.launch("planner_backup_${System.currentTimeMillis()}.json") }) { Icon(Icons.Default.Share, contentDescription = "내보내기", tint = Accent) }
                            }
                        }
                        2 -> {
                            TextButton(onClick = { showCategoryManageDialog = true }) {
                                Text("카테고리 관리", color = Accent)
                            }
                        }
                    }
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "라이트 모드" else "다크 모드",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index; selectedStatsEntry = null },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> MainScreen(
                    categories = categories,
                    selectedDate = selectedDate,
                    entries = entries,
                    onAddEntry = { cat, min, note, s, e, type, count -> viewModel.addEntry(cat, min, note, s, e, type, count) },
                    onUpdateEntry = { viewModel.updateEntry(it) },
                    isTracking = isTracking,
                    elapsedSeconds = elapsedSeconds,
                    alarmTriggered = alarmTriggered,
                    restoredNote = restoredNote,
                    restoredCategories = restoredCategories,
                    onStartTracking = { cats, displays, note, timerMinutes -> viewModel.startTracking(cats, displays, note, timerMinutes) },
                    onStopTrackingAndSave = { cats, note -> viewModel.stopTrackingAndSave(cats, note) },
                    onCancelTracking = { viewModel.cancelTracking() },
                    onClearAlarm = { viewModel.clearAlarmTriggered() },
                    onBatchDelete = { entries -> entries.forEach { viewModel.deleteEntry(it) } }
                )
                1 -> StatsScreen(
                    categories = categories,
                    currentYear = yearly,
                    currentMonth = monthly,
                    selectedDate = selectedDate,
                    dailyEntries = entries,
                    monthlyStats = stats,
                    dailyStats = dailyStats,
                    weeklyStats = weeklyStats,
                    weeklyDailyStats = weeklyDailyStats,
                    weeklyDailyCategoryStats = weeklyDailyCategoryStats,
                    monthlyDailyStats = monthlyDailyStats,
                    monthlyDailyCategoryMap = monthlyDailyCategoryMap,
                    onMonthChange = { y, m -> viewModel.setCurrentMonth(y, m) },
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    onNavigateToGoals = { selectedTab = 2 },
                    selectedEntry = selectedStatsEntry,
                    onSelectEntry = { selectedStatsEntry = it }
                )
                2 -> GoalsScreen(
                    categories = categories,
                    currentYear = yearly,
                    currentMonth = monthly,
                    goals = goals,
                    categoryProgress = categoryProgress,
                    onUpsertGoal = { viewModel.upsertGoal(it) },
                    onDeleteGoal = { viewModel.deleteGoal(it) }
                )
            }
        }
    }
}
