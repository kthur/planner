package com.planner.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.ui.theme.categoryColorFromHex

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    categories: List<CategoryEntity>,
    selected: Set<String>,
    onSelectChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val color = categoryColorFromHex(category.colorHex)
            val isSelected = selected.contains(category.name)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = if (isSelected) 1f else 0.2f))
                    .clickable {
                        val newSelected = if (isSelected) {
                            selected - category.name
                        } else {
                            selected + category.name
                        }
                        onSelectChange(newSelected)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category.displayName,
                    color = if (isSelected) Color.White else color
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    categories: List<CategoryEntity>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val color = categoryColorFromHex(category.colorHex)
            val isSelected = category.name == selected
            val badge = when (category.entryType) {
                "COUNT" -> "#"
                "BOTH" -> "#/"
                else -> ""
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = if (isSelected) 1f else 0.2f))
                    .clickable { onSelect(category.name) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.displayName,
                        color = if (isSelected) Color.White else color
                    )
                    if (badge.isNotEmpty()) {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else color.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
