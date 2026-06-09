package com.planner.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PlannerTheme(darkTheme = isDarkMode) {
                PlannerUI()
            }
        }
    }
}

private fun ComponentActivity.createNotificationChannel() {
    val channel = NotificationChannel(
        "timer_alarm",
        "타이머 알림",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        enableVibration(true)
        description = "타이머가 완료되면 알림"
    }
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
}

data class NavItem(val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("기록", Icons.Default.EditNote),
    NavItem("통계", Icons.Default.BarChart),
    NavItem("목표", Icons.Default.Flag)
)

@Composable
fun PlannerUI() {
    val viewModel: PlannerViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isDarkMode by rememberSaveable { mutableStateOf(true) }

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
    val ctx = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        alwaysShowLabel = true
                    )
                }
                IconButton(onClick = { isDarkMode = !isDarkMode }) {
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
                    onUpdateEntry = { viewModel.updateEntry(it) }
                )
                    1 -> StatsScreen(
                        currentYear = yearly,
                        currentMonth = monthly,
                        selectedDate = selectedDate,
                        monthlyStats = stats,
                        dailyStats = dailyStats,
                        weeklyStats = weeklyStats,
                        weeklyDailyStats = weeklyDailyStats,
                        monthlyDailyStats = monthlyDailyStats,
                        onMonthChange = { y, m -> viewModel.setCurrentMonth(y, m) },
                        onDateSelected = { viewModel.setSelectedDate(it) },
                        onNavigateToGoals = { selectedTab = 2 },
                        onExport = {
                            viewModel.exportDataAsJson { json ->
                                try {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        putExtra(Intent.EXTRA_SUBJECT, "Planner Backup")
                                    }
                                    ctx.startActivity(Intent.createChooser(intent, "데이터 내보내기"))
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "내보내기 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
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
