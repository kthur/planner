package com.planner.tracker.wear.presentation

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import kotlinx.coroutines.delay

private val NavyBackground = Color(0xFF1A1A2E)
private val CardBackground = Color(0xFF1E2A4A)
private val AccentColor = Color(0xFFE94560)

fun parseCategoryColor(colorHex: String?): Color {
    if (colorHex.isNullOrEmpty()) return Color.White
    return try {
        val hex = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.White
    }
}

@Composable
fun rememberTickSeconds(startElapsed: Long, isTracking: Boolean): Long {
    var seconds by remember(startElapsed, isTracking) {
        mutableStateOf(
            if (isTracking && startElapsed > 0) {
                (SystemClock.elapsedRealtime() - startElapsed) / 1000
            } else {
                0L
            }
        )
    }

    LaunchedEffect(startElapsed, isTracking) {
        if (isTracking && startElapsed > 0) {
            while (true) {
                seconds = (SystemClock.elapsedRealtime() - startElapsed) / 1000
                delay(500)
            }
        } else {
            seconds = 0L
        }
    }
    return seconds.coerceAtLeast(0L)
}

@Composable
fun WearApp(viewModel: WearViewModel = viewModel()) {
    val navController = rememberSwipeDismissableNavController()

    Scaffold(
        modifier = Modifier.background(NavyBackground),
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToSelectCategory = { action ->
                        navController.navigate("select_category/$action")
                    }
                )
            }
            composable("select_category/{action}") { backStackEntry ->
                val action = backStackEntry.arguments?.getString("action") ?: "track"
                CategorySelectScreen(
                    viewModel = viewModel,
                    onCategorySelected = { categoryName, displayName, colorHex ->
                        if (action == "track") {
                            navController.navigate("config_timer/$categoryName/$displayName/$colorHex")
                        } else {
                            navController.navigate("quick_log/$categoryName/$displayName/$colorHex")
                        }
                    }
                )
            }
            composable("config_timer/{catName}/{catDisplay}/{colorHex}") { backStackEntry ->
                val catName = backStackEntry.arguments?.getString("catName") ?: ""
                val catDisplay = backStackEntry.arguments?.getString("catDisplay") ?: ""
                val colorHex = backStackEntry.arguments?.getString("colorHex") ?: "FFFFFF"
                TimerConfigScreen(
                    categoryName = catName,
                    categoryDisplay = catDisplay,
                    colorHex = colorHex,
                    onStart = { durationMins ->
                        viewModel.startTracking(
                            categories = setOf(catName),
                            categoryDisplays = catDisplay,
                            note = "",
                            timerMinutes = durationMins
                        )
                        navController.popBackStack("main", false)
                    }
                )
            }
            composable("quick_log/{catName}/{catDisplay}/{colorHex}") { backStackEntry ->
                val catName = backStackEntry.arguments?.getString("catName") ?: ""
                val catDisplay = backStackEntry.arguments?.getString("catDisplay") ?: ""
                val colorHex = backStackEntry.arguments?.getString("colorHex") ?: "FFFFFF"
                QuickLogScreen(
                    categoryName = catName,
                    categoryDisplay = catDisplay,
                    colorHex = colorHex,
                    onSave = { mins ->
                        viewModel.quickLog(catName, mins, "Watch Quick Log")
                        navController.popBackStack("main", false)
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: WearViewModel,
    onNavigateToSelectCategory: (String) -> Unit
) {
    val isTracking by viewModel.isTracking.collectAsState()
    val startElapsed by viewModel.startElapsed.collectAsState()
    val targetSec by viewModel.targetSec.collectAsState()
    val categoryDisplays by viewModel.categoryDisplays.collectAsState()
    val activeCategories by viewModel.activeCategories.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val note by viewModel.note.collectAsState()

    val elapsedSeconds = rememberTickSeconds(startElapsed, isTracking)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isTracking) {
            val primaryCategory = activeCategories.firstOrNull() ?: ""
            val matchedCatObj = categoriesList.find { it.name == primaryCategory }
            val catColor = parseCategoryColor(matchedCatObj?.colorHex ?: "E94560")

            if (targetSec > 0) {
                val progress = (elapsedSeconds.toFloat() / targetSec).coerceIn(0f, 1f)
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    indicatorColor = catColor,
                    trackColor = Color.DarkGray.copy(alpha = 0.5f)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 4.dp,
                    indicatorColor = catColor,
                    trackColor = Color.Transparent
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    text = categoryDisplays.ifEmpty { "측정 중" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = catColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                val hrs = elapsedSeconds / 3600
                val mins = (elapsedSeconds % 3600) / 60
                val secs = elapsedSeconds % 60
                val timeStr = if (hrs > 0) {
                    String.format("%d:%02d:%02d", hrs, mins, secs)
                } else {
                    String.format("%02d:%02d", mins, secs)
                }

                Text(
                    text = timeStr,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                if (targetSec > 0) {
                    val remaining = (targetSec - elapsedSeconds).coerceAtLeast(0)
                    Text(
                        text = "남음: ${remaining / 60}분",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                } else if (note.isNotEmpty()) {
                    Text(
                        text = note,
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.stopTracking() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentColor),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Button(
                        onClick = { viewModel.cancelTracking() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Planner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "시간 기록 플래너",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Chip(
                    onClick = { onNavigateToSelectCategory("track") },
                    label = { Text("시간 측정 시작", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start") },
                    colors = ChipDefaults.chipColors(backgroundColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Chip(
                    onClick = { onNavigateToSelectCategory("quick_log") },
                    label = { Text("간편 시간 기록", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color.DarkGray.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CategorySelectScreen(
    viewModel: WearViewModel,
    onCategorySelected: (String, String, String) -> Unit
) {
    val categories by viewModel.categories.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "카테고리 선택",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (categories.isEmpty()) {
            item {
                Text(
                    text = "동기화된 카테고리가\n없습니다. 모바일 앱을\n확인해 주세요.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(categories) { category ->
                val catColor = parseCategoryColor(category.colorHex)
                Chip(
                    onClick = { onCategorySelected(category.name, category.displayName, category.colorHex) },
                    label = {
                        Text(
                            text = category.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = CardBackground
                    ),
                    border = ChipDefaults.chipBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TimerConfigScreen(
    categoryName: String,
    categoryDisplay: String,
    colorHex: String,
    onStart: (String) -> Unit
) {
    val catColor = parseCategoryColor(colorHex)
    val durations = listOf(
        "0" to "제한 없음",
        "15" to "15분",
        "30" to "30분",
        "60" to "1시간",
        "90" to "1.5시간",
        "120" to "2시간"
    )

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "[$categoryDisplay] 목표 시간 설정",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = catColor,
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        items(durations) { (valStr, displayStr) ->
            Chip(
                onClick = { onStart(valStr) },
                label = { Text(displayStr, fontWeight = FontWeight.Medium) },
                colors = ChipDefaults.chipColors(backgroundColor = CardBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun QuickLogScreen(
    categoryName: String,
    categoryDisplay: String,
    colorHex: String,
    onSave: (Int) -> Unit
) {
    val catColor = parseCategoryColor(colorHex)
    val options = listOf(
        15 to "15분 기록",
        30 to "30분 기록",
        45 to "45분 기록",
        60 to "1시간 기록",
        90 to "1.5시간 기록",
        120 to "2시간 기록"
    )

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "[$categoryDisplay] 기록할 시간",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = catColor,
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        items(options) { (mins, displayStr) ->
            Chip(
                onClick = { onSave(mins) },
                label = { Text(displayStr, fontWeight = FontWeight.Medium) },
                colors = ChipDefaults.chipColors(backgroundColor = CardBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }
    }
}
