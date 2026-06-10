package com.planner.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.CategoryStat
import com.planner.tracker.data.DailyCategoryStat
import com.planner.tracker.data.DailyStat
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.CardBackground
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import com.planner.tracker.ui.theme.categoryColorFromHex
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    categories: List<CategoryEntity>,
    currentYear: Int,
    currentMonth: Int,
    selectedDate: Long,
    dailyEntries: List<Entry>,
    monthlyStats: List<CategoryStat>,
    dailyStats: List<CategoryStat>,
    weeklyStats: List<CategoryStat>,
    weeklyDailyStats: List<DailyStat>,
    weeklyDailyCategoryStats: List<DailyCategoryStat>,
    monthlyDailyStats: List<DailyStat>,
    monthlyDailyCategoryMap: Map<Long, List<String>>,
    onMonthChange: (Int, Int) -> Unit,
    onDateSelected: (Long) -> Unit,
    onNavigateToGoals: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var year by remember(currentYear) { mutableIntStateOf(currentYear) }
    var month by remember(currentMonth) { mutableIntStateOf(currentMonth) }
    var selectedDay by remember(selectedDate) { mutableLongStateOf(selectedDate) }
    val tabs = listOf("일간", "주간", "월간")

    val categoryMap = remember(categories) { categories.associateBy { it.name } }

    val stats = when (selectedTab) {
        0 -> dailyStats
        1 -> weeklyStats
        else -> monthlyStats
    }

        Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                val cal = remember { Calendar.getInstance() }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { cal.timeInMillis = selectedDay; cal.add(Calendar.DAY_OF_MONTH, -1); selectedDay = cal.timeInMillis; onDateSelected(selectedDay) }) { Icon(Icons.Default.ChevronLeft, "이전 날", tint = MaterialTheme.colorScheme.onBackground) }
                    Text(text = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN).format(Date(selectedDay)), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { cal.timeInMillis = selectedDay; cal.add(Calendar.DAY_OF_MONTH, 1); selectedDay = cal.timeInMillis; onDateSelected(selectedDay) }) { Icon(Icons.Default.ChevronRight, "다음 날", tint = MaterialTheme.colorScheme.onBackground) }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 12시간 시계 항상 표시
                if (dailyEntries.isNotEmpty()) {
                    DailyClockView(entries = dailyEntries, categoryMap = categoryMap)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            1 -> {
                val cal = remember { Calendar.getInstance() }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { cal.timeInMillis = selectedDay; cal.add(Calendar.WEEK_OF_YEAR, -1); selectedDay = cal.timeInMillis; onDateSelected(selectedDay) }) { Icon(Icons.Default.ChevronLeft, "이전 주", tint = MaterialTheme.colorScheme.onBackground) }
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
                    IconButton(onClick = { cal.timeInMillis = selectedDay; cal.add(Calendar.WEEK_OF_YEAR, 1); selectedDay = cal.timeInMillis; onDateSelected(selectedDay) }) { Icon(Icons.Default.ChevronRight, "다음 주", tint = MaterialTheme.colorScheme.onBackground) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            2 -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { month--; if (month < 1) { month = 12; year-- }; onMonthChange(year, month) }) { Icon(Icons.Default.ChevronLeft, "이전 달", tint = MaterialTheme.colorScheme.onBackground) }
                    Text(text = "${year}년 ${month}월", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { month++; if (month > 12) { month = 1; year++ }; onMonthChange(year, month) }) { Icon(Icons.Default.ChevronRight, "다음 달", tint = MaterialTheme.colorScheme.onBackground) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                MonthCalendar(year = year, month = month, dailyCategoryMap = monthlyDailyCategoryMap, categoryMap = categoryMap, onDateClick = { millis -> selectedDay = millis; onDateSelected(millis) })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (stats.isEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "통계 데이터가 없습니다", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "기록 탭에서 활동을 추가하면\n여기에 통계가 표시됩니다", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val totalMinutes = stats.sumOf { it.total }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "총 ${totalMinutes}분 (${totalMinutes / 60}시간 ${totalMinutes % 60}분)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))

                    val strokeWidth = 40f
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val canvasSize = size
                        val padding = strokeWidth / 2f
                        var startAngle = -90f
                        stats.forEach { stat ->
                            val sweepAngle = (stat.total.toFloat() / totalMinutes) * 360f
                            val catColor = categoryMap[stat.category]?.let { categoryColorFromHex(it.colorHex) } ?: Accent
                            drawArc(color = catColor, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt), size = Size(canvasSize.width - strokeWidth, canvasSize.height - strokeWidth), topLeft = Offset(padding, padding))
                            startAngle += sweepAngle
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    stats.forEach { stat ->
                        val catInfo = categoryMap[stat.category]
                        val color = if (catInfo != null) categoryColorFromHex(catInfo.colorHex) else Accent
                        val displayName = catInfo?.displayName ?: stat.category
                        val pct = if (totalMinutes > 0) (stat.total.toFloat() / totalMinutes * 100) else 0f
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = displayName, modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.onBackground)
                            Text(text = "${stat.total}분", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = String.format("%.1f%%", pct), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }

            if (selectedTab == 0 && dailyEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "상세 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val tf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                        dailyEntries.forEach { entry ->
                            val catInfo = categoryMap[entry.category]
                            val color = if (catInfo != null) categoryColorFromHex(catInfo.colorHex) else Accent
                            val displayName = catInfo?.displayName ?: entry.category
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = displayName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                    if (entry.startTime > 0 && entry.endTime > 0) {
                                        Text(text = "${tf.format(Date(entry.startTime))} - ${tf.format(Date(entry.endTime))}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text(text = "${entry.minutes}분", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                if (entry.note.isNotBlank()) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedTab == 1 && weeklyDailyCategoryStats.isNotEmpty()) {
                val dayFormat = remember { SimpleDateFormat("E", Locale.KOREAN) }
                val dailyTotals = weeklyDailyStats.associate { it.date to it.total }
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "일별 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        val grouped = weeklyDailyCategoryStats.groupBy { it.date }.mapValues { (_, stats) ->
                            stats.sortedBy { it.category }
                        }
                        grouped.forEach { (date, catStats) ->
                            val dayTotal = dailyTotals[date] ?: catStats.sumOf { it.total }
                            val dayName = dayFormat.format(Date(date))
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = dayName, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.width(40.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(4.dp)).background(Accent.copy(alpha = 0.12f))) {
                                    Row(Modifier.fillMaxSize()) {
                                        catStats.forEach { stat ->
                                            val barCatInfo = categoryMap[stat.category]
                                            val barColor = if (barCatInfo != null) categoryColorFromHex(barCatInfo.colorHex) else Accent
                                            Box(
                                                Modifier.fillMaxHeight().weight(stat.total.toFloat() / dayTotal)
                                                    .background(barColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (stat.total >= 15) Text("${stat.total}분", color = MaterialTheme.colorScheme.onBackground, fontSize = MaterialTheme.typography.labelSmall.fontSize, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${dayTotal}분", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun timeToAngle(hourOfDay: Int, minute: Int): Float {
    val h = hourOfDay % 12
    return (h * 30 + minute * 0.5f) - 90f
}

@Composable
private fun DailyClockView(
    entries: List<Entry>,
    categoryMap: Map<String, CategoryEntity>
) {
    val entriesWithTime = entries.filter { it.startTime > 0 && it.endTime > 0 }
    val cal = remember { Calendar.getInstance() }

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "12시간 시계", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(modifier = Modifier.size(260.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = minOf(cx, cy) - 16f
                val innerRadius = radius * 0.78f

                drawCircle(color = Accent.copy(alpha = 0.08f), radius = radius, center = Offset(cx, cy))

                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30 - 90).toDouble())
                    val outerX = cx + (radius * cos(angle)).toFloat()
                    val outerY = cy + (radius * sin(angle)).toFloat()
                    val innerX = cx + (radius * 0.88f * cos(angle)).toFloat()
                    val innerY = cy + (radius * 0.88f * sin(angle)).toFloat()
                    drawLine(
                        color = Accent.copy(alpha = 0.4f),
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = if (i % 3 == 0) 3f else 1.5f
                    )
                }

                entriesWithTime.forEach { entry ->
                    cal.timeInMillis = entry.startTime
                    val startHour = cal.get(Calendar.HOUR_OF_DAY)
                    val startMin = cal.get(Calendar.MINUTE)
                    cal.timeInMillis = entry.endTime
                    val endHour = cal.get(Calendar.HOUR_OF_DAY)
                    val endMin = cal.get(Calendar.MINUTE)

                    val startAngle = timeToAngle(startHour, startMin)
                    val endAngle = timeToAngle(endHour, endMin)
                    val sweep = if (endAngle > startAngle) endAngle - startAngle else (360f - startAngle + endAngle)

                    val catInfo = categoryMap[entry.category]
                    val color = if (catInfo != null) categoryColorFromHex(catInfo.colorHex) else Accent

                    drawArc(
                        color = color.copy(alpha = 0.7f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = radius * 0.22f, cap = StrokeCap.Round),
                        topLeft = Offset(cx - innerRadius, cy - innerRadius),
                        size = Size(innerRadius * 2, innerRadius * 2)
                    )
                }

                drawCircle(color = Accent.copy(alpha = 0.15f), radius = 6f, center = Offset(cx, cy))
            }

            Spacer(modifier = Modifier.height(12.dp))

            entriesWithTime.take(6).forEach { entry ->
                val catInfo = categoryMap[entry.category]
                val color = if (catInfo != null) categoryColorFromHex(catInfo.colorHex) else Accent
                val displayName = catInfo?.displayName ?: entry.category
                cal.timeInMillis = entry.startTime
                val sh = cal.get(Calendar.HOUR_OF_DAY)
                val sm = cal.get(Calendar.MINUTE)
                cal.timeInMillis = entry.endTime
                val eh = cal.get(Calendar.HOUR_OF_DAY)
                val em = cal.get(Calendar.MINUTE)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text(text = displayName, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(text = "${sh}:${sm.toString().padStart(2, '0')}-${eh}:${em.toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (entriesWithTime.size > 6) {
                Text(text = "...외 ${entriesWithTime.size - 6}개", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    year: Int,
    month: Int,
    dailyCategoryMap: Map<Long, List<String>>,
    categoryMap: Map<String, CategoryEntity>,
    onDateClick: (Long) -> Unit
) {
    val cal = remember { Calendar.getInstance() }
    val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")

    cal.set(year, month - 1, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                dayNames.forEach { name ->
                    Text(text = name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            var day = 1
            for (week in 0 until 6) {
                if (day > daysInMonth) break
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
                            val categories = dailyCategoryMap[dayMillis]

                            val primaryCatColor = if (categories != null && categories.isNotEmpty()) {
                                val firstCatInfo = categoryMap[categories.first()]
                                if (firstCatInfo != null) categoryColorFromHex(firstCatInfo.colorHex) else Accent
                            } else null

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .then(
                                        if (primaryCatColor != null) Modifier
                                            .background(primaryCatColor.copy(alpha = 0.15f), CircleShape)
                                            .border(1.5.dp, primaryCatColor.copy(alpha = 0.4f), CircleShape)
                                        else Modifier
                                    )
                                    .clip(CircleShape)
                                    .clickable { onDateClick(dayMillis) },
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentDay.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (primaryCatColor != null) primaryCatColor else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (categories != null) FontWeight.Bold else FontWeight.Normal
                                )
                                if (categories != null && categories.size > 1) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        categories.take(3).forEach { catName ->
                                            val catInfo = categoryMap[catName]
                                            val dotColor = if (catInfo != null) categoryColorFromHex(catInfo.colorHex) else Accent
                                            Box(Modifier.size(4.dp).clip(CircleShape).background(dotColor))
                                        }
                                    }
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
