package com.planner.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.categoryColorFromHex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EntryCard(
    entry: Entry,
    categoryInfo: Map<String, CategoryEntity>,
    onIncrement: (() -> Unit)? = null,
    onDecrement: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cat = categoryInfo[entry.category]
    val displayName = cat?.displayName ?: entry.category
    val color = if (cat != null) categoryColorFromHex(cat.colorHex) else Accent
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val isCount = entry.count > 0 || entry.entryType == "COUNT"
    val isEvent = entry.entryType == "EVENT"

    val typeLabel = when {
        isCount -> "#"
        isEvent -> "📍"
        else -> "⏱"
    }
    val typeBg = when {
        isCount -> color.copy(alpha = 0.08f)
        isEvent -> Color(0xFFFFF3E0).copy(alpha = 0.15f)
        else -> color.copy(alpha = 0.05f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f)
                else if (isCount) color.copy(alpha = 0.06f)
                else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 타입 배지 (좌측 세로 바)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (onToggleSelect != null) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) color else color.copy(alpha = 0.25f))
                        .clickable { onToggleSelect() }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // 타이틀 + 서브정보
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeBg)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(text = typeLabel, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = displayName, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                if (isEvent) {
                    val eventFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    Text(text = eventFmt.format(Date(entry.startTime)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                } else if (entry.startTime > 0 && entry.endTime > 0) {
                    Text(text = "${timeFormat.format(Date(entry.startTime))} - ${timeFormat.format(Date(entry.endTime))}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                }
                if (entry.note.isNotBlank()) {
                    Text(text = entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, maxLines = 1)
                }

                if (isSelected && !entry.photoUri.isNullOrEmpty()) {
                    val context = LocalContext.current
                    val photoFile = remember(entry.photoUri) {
                        java.io.File(java.io.File(context.filesDir, "photos"), entry.photoUri)
                    }
                    if (photoFile.exists()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = photoFile,
                            contentDescription = "첨부 사진",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            if (!isSelected && !entry.photoUri.isNullOrEmpty()) {
                val context = LocalContext.current
                val photoFile = remember(entry.photoUri) {
                    java.io.File(java.io.File(context.filesDir, "photos"), entry.photoUri)
                }
                if (photoFile.exists()) {
                    AsyncImage(
                        model = photoFile,
                        contentDescription = "첨부 사진",
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 값 표시 (+/- 버튼)
            if (isCount) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDecrement != null) {
                        IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "감소", tint = color, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "${entry.count}회", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (onIncrement != null) {
                        IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "증가", tint = color, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else if (isEvent) {
                Text(text = "체크", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "${entry.minutes}분", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

        }
    }
}
