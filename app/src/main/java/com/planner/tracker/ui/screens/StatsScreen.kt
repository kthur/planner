package com.planner.tracker.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planner.tracker.data.Category
import com.planner.tracker.data.CategoryStat
import com.planner.tracker.ui.theme.CardBackground
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import com.planner.tracker.ui.theme.categoryColor

@Composable
fun StatsScreen(
    currentYear: Int,
    currentMonth: Int,
    stats: List<CategoryStat>,
    onMonthChange: (Int, Int) -> Unit,
    onNavigateToGoals: () -> Unit
) {
    var year by remember(currentYear) { mutableIntStateOf(currentYear) }
    var month by remember(currentMonth) { mutableIntStateOf(currentMonth) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "월간 통계",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        if (stats.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "이 달에 기록된 데이터가 없습니다.\n항목을 추가하고 통계를 확인하세요.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
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
