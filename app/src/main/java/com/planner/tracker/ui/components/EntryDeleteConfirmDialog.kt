package com.planner.tracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.theme.Accent

@Composable
fun EntryDeleteConfirmDialog(
    entry: Entry,
    categoryInfoMap: Map<String, CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val catDisplay = categoryInfoMap[entry.category]?.displayName ?: entry.category
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("항목 삭제") },
        text = { Text("\"${catDisplay}\" 항목을 삭제하시겠습니까?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("삭제", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
