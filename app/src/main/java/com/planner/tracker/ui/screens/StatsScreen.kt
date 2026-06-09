package com.planner.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planner.tracker.data.Category
import com.planner.tracker.data.CategoryStat
import com.planner.tracker.data.DailyStat
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.CardBackground
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import com.planner.tracker.ui.theme.categoryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    currentYear: Int,
    currentMonth: Int,
    selectedDate: Long,
    monthlyStats: List<CategoryStat>,
    dailyStats: List<CategoryStat>,
    weeklyStats: List<CategoryStat>,
    weeklyDailyStats: List<DailyStat>,
    monthlyDailyStats: List<DailyStat>,
    onMonthChange: (Int, Int) -> Unit,
    onDateSelected: (Long) -> Unit,
    onNavigateToGoals: () -> Unit,
    onExport: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var year by remember(currentYear) { mutableIntStateOf(currentYear) }
    var month by remember(currentMonth) { mutableIntStateOf(currentMonth) }
    var selectedDay by remember(selectedDate) { mutableLongStateOf(selectedDate) }
    val tabs = listOf("일간", "주간", "월간")

    val stats = when (selectedTab) {
        0 -> dailyStats
        1 -> weeklyStats
        else -> monthlyStats
    }

    val dateSet = remember(monthlyDailyStats) {
        monthlyDailyStats.map { it.date }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "통계",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Share, contentDescription = "내보내기", tint = Accent)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val presets = listOf("오늘", "이번 주", "이번 달")
            presets.forEach { label ->
                val cal = Calendar.getInstance()
                TextButton(
                    onClick = {
                        when (label) {
                            "오늘" -> { onDateSelected(cal.timeInMillis); selectedDay = cal.timeInMillis; selectedTab = 0 }
                            "이번 주" -> { selectedDay = cal.timeInMillis; onDateSelected(cal.timeInMillis); selectedTab = 1 }
                            "이번 달" -> { onMonthChange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1); selectedTab = 2 }
                        }
                    }
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                val cal = remember { Calendar.getInstance() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        cal.timeInMillis = selectedDay
                        cal.add(Calendar.DAY_OF_MONTH, -1)
                        selectedDay = cal.timeInMillis
                        onDateSelected(selectedDay)
                    }) {
                        Icon(Icons.Default.ChevronLeft, "이전 날", tint = TextPrimary)
                    }
                    Text(
                        text = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN).format(Date(selectedDay)),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(onClick = {
                        cal.timeInMillis = selectedDay
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        selectedDay = cal.timeInMillis
                        onDateSelected(selectedDay)
                    }) {
                        Icon(Icons.Default.ChevronRight, "다음 날", tint = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            1 -> {
                val cal = remember { Calendar.getInstance() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        cal.timeInMillis = selectedDay
                        cal.add(Calendar.WEEK_OF_YEAR, -1)
                        selectedDay = cal.timeInMillis
                        onDateSelected(selectedDay)
                    }) {
                        Icon(Icons.Default.ChevronLeft, "이전 주", tint = TextPrimary)
                    }
                    Text(
                        text = {
                            cal.timeInMillis = selectedDay
                            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            val start = cal.timeInMillis
                            cal.add(Calendar.DAY_OF_WEEK, 6)
                            val end = cal.timeInMillis
                            val fmt = SimpleDateFormat("M/d", Locale.KOREAN)
                            "${fmt.format(Date(start))} - ${fmt.format(Date(end))}"
                        }(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(onClick = {
                        cal.timeInMillis = selectedDay
                        cal.add(Calendar.WEEK_OF_YEAR, 1)
                        selectedDay = cal.timeInMillis
                        onDateSelected(selectedDay)
                    }) {
                        Icon(Icons.Default.ChevronRight, "다음 주", tint = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            2 -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        month--
                        if (month < 1) { month = 12; year-- }
                        onMonthChange(year, month)
                    }) {
                        Icon(Icons.Default.ChevronLeft, "이전 달", tint = TextPrimary)
                    }
                    Text(
                        text = "${year}년 ${month}월",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(onClick = {
                        month++
                        if (month > 12) { month = 1; year++ }
                        onMonthChange(year, month)
                    }) {
                        Icon(Icons.Default.ChevronRight, "다음 달", tint = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                MonthCalendar(
                    year = year,
                    month = month,
                    dateSet = dateSet,
                    onDateClick = { millis ->
                        selectedDay = millis
                        onDateSelected(millis)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (stats.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "통계 데이터가 없습니다",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "기록 탭에서 활동을 추가하면\n여기에 통계가 표시됩니다",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
        } else {
            val totalMinutes = stats.sumOf { it.total }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "총 ${totalMinutes}분 (${totalMinutes / 60}시간 ${totalMinutes % 60}분)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val strokeWidth = 40f
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val canvasSize = size
                        val padding = strokeWidth / 2f
                        var startAngle = -90f
                        stats.forEach { stat ->
                            val sweepAngle = (stat.total.toFloat() / totalMinutes) * 360f
                            drawArc(
                                color = categoryColor(stat.category),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = Size(canvasSize.width - strokeWidth, canvasSize.height - strokeWidth),
                                topLeft = Offset(padding, padding)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    stats.forEach { stat ->
                        val color = categoryColor(stat.category)
                        val pct = if (totalMinutes > 0) (stat.total.toFloat() / totalMinutes * 100) else 0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stat.category.displayName,
                                modifier = Modifier.width(80.dp),
                                color = TextPrimary
                            )
                            Text(
                                text = "${stat.total}분",
                                modifier = Modifier.weight(1f),
                                color = TextSecondary
                            )
                            Text(
                                text = String.format("%.1f%%", pct),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(60.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 1 && weeklyDailyStats.isNotEmpty()) {
                val dayFormat = remember { SimpleDateFormat("E", Locale.KOREAN) }
                val maxDaily = weeklyDailyStats.maxOf { it.total }.coerceAtLeast(1)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "일별 기록",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        weeklyDailyStats.forEach { daily ->
                            val pct = daily.total.toFloat() / maxDaily
                            val dayName = dayFormat.format(Date(daily.date))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = dayName,
                                    color = TextPrimary,
                                    modifier = Modifier.width(40.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Accent.copy(alpha = 0.15f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(pct)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Accent)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${daily.total}분",
                                    color = TextSecondary,
                                    modifier = Modifier.width(50.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "카테고리별 상세",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    stats.forEach { stat ->
                        val color = categoryColor(stat.category)
                        val pct = if (totalMinutes > 0) stat.total.toFloat() / totalMinutes else 0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stat.category.displayName,
                                modifier = Modifier.width(80.dp),
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = pct,
                                color = color,
                                trackColor = color.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${stat.total}분",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(50.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    year: Int,
    month: Int,
    dateSet: Set<Long>,
    onDateClick: (Long) -> Unit
) {
    val cal = remember { Calendar.getInstance() }
    val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")

    cal.set(year, month - 1, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayNames.forEach { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            var day = 1
            for (week in 0 until 6) {
                if (day > daysInMonth) break
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dow in 0 until 7) {
                        if ((week == 0 && dow < firstDayOfWeek) || day > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val currentDay = day
                            cal.set(year, month - 1, currentDay)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val dayMillis = cal.timeInMillis
                            val hasData = dayMillis in dateSet

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .clickable { onDateClick(dayMillis) },
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentDay.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = if (hasData) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasData) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Accent)
                                    )
                                }
                            }
                            day++
                        }
                    }
                }
            }
        }
    }
}
