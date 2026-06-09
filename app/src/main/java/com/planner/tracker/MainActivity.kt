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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.planner.tracker.ui.screens.GoalsScreen
import com.planner.tracker.ui.screens.MainScreen
import com.planner.tracker.ui.screens.StatsScreen
import com.planner.tracker.ui.theme.PlannerTheme
import com.planner.tracker.viewmodel.PlannerViewModel

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

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
    val weeklyDailyCategoryStats by viewModel.weeklyDailyCategoryStats.collectAsState()
    val monthlyDailyCategoryMap by viewModel.monthlyDailyCategoryMap.collectAsState()
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

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        alwaysShowLabel = true
                    )
                }
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = if (isDarkMode) "라이트 모드" else "다크 모드",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> MainScreen(
                    selectedDate = selectedDate,
                    entries = entries,
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    onAddEntry = { cat, min, note, s, e -> viewModel.addEntry(cat, min, note, s, e) },
                    onDeleteEntry = { viewModel.deleteEntry(it) },
                    onUpdateEntry = { viewModel.updateEntry(it) },
                    isTracking = isTracking,
                    elapsedSeconds = elapsedSeconds,
                    alarmTriggered = alarmTriggered,
                    onStartTracking = { viewModel.startTracking(it) },
                    onStopTrackingAndSave = { cat, note -> viewModel.stopTrackingAndSave(cat, note) },
                    onCancelTracking = { viewModel.cancelTracking() },
                    onClearAlarm = { viewModel.clearAlarmTriggered() }
                )
                1 -> StatsScreen(
                    currentYear = yearly,
                    currentMonth = monthly,
                    selectedDate = selectedDate,
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
                    onExport = { exportLauncher.launch("planner_backup_${System.currentTimeMillis()}.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) }
                )
                2 -> GoalsScreen(
                    currentYear = yearly,
                    currentMonth = monthly,
                    goals = goals,
                    categoryProgress = categoryProgress,
                    onUpsertGoal = { viewModel.upsertGoal(it) },
                    onDeleteGoal = { viewModel.deleteGoal(it) },
                    onNavigateBack = { selectedTab = 0 }
                )
            }
        }
    }
}
