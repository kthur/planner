package com.planner.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import com.planner.tracker.ui.theme.categoryColorFromHex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EntryCard(
    entry: Entry,
    categoryInfo: Map<String, CategoryEntity>,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onToggleSelect != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) color else color.copy(alpha = 0.3f))
                        .then(Modifier.padding(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                if (entry.entryType == "EVENT") {
                    val eventTimeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    Text(
                        text = "📍 ${eventTimeFormat.format(Date(entry.startTime))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (entry.startTime > 0 && entry.endTime > 0) {
                    Text(
                        text = "${timeFormat.format(Date(entry.startTime))} - ${timeFormat.format(Date(entry.endTime))}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))

            if (entry.count > 0) {
                if (onDecrement != null) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "감소", tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = "${entry.count}회",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                if (onIncrement != null) {
                    IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "증가", tint = color, modifier = Modifier.size(18.dp))
                    }
                }
            } else if (entry.entryType == "EVENT") {
                Text(text = "체크", color = Accent, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = "${entry.minutes}분",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "수정", tint = Accent, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}
