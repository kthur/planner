package com.planner.tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlannerTheme {
                PlannerUI()
            }
        }
    }
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

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) viewModel.refreshTimeRanges()
    }

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
                    onDeleteEntry = { viewModel.deleteEntry(it) }
                )
                1 -> StatsScreen(
                    currentYear = yearly,
                    currentMonth = monthly,
                    monthlyStats = stats,
                    dailyStats = dailyStats,
                    weeklyStats = weeklyStats,
                    weeklyDailyStats = weeklyDailyStats,
                    onMonthChange = { y, m -> viewModel.setCurrentMonth(y, m) },
                    onNavigateToGoals = { selectedTab = 2 },
                    onExport = {
                        val ctx = LocalContext.current
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
