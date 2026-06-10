package com.planner.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.CardBackground
import com.planner.tracker.ui.theme.TextPrimary
import com.planner.tracker.ui.theme.TextSecondary
import com.planner.tracker.ui.theme.categoryColorFromHex

private val presetColors = listOf(
    "FF4CAF50", "FF2196F3", "FFFF9800", "FF9C27B0", "FFF44336", "FF00BCD4",
    "FFFF5722", "FF607D8B", "FF795548", "FF009688", "FFE91E63", "FF3F51B5"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryManageDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit,
    onUpdate: (String, String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var editTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editDisplay by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf(presetColors.first()) }
    var isAdding by remember { mutableStateOf(false) }

    fun startAdd() {
        isAdding = true
        editTarget = null
        editName = ""
        editDisplay = ""
        editColor = presetColors.first()
    }

    fun startEdit(cat: CategoryEntity) {
        isAdding = false
        editTarget = cat
        editName = cat.name
        editDisplay = cat.displayName
        editColor = cat.colorHex
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카테고리 관리") },
        text = {
            Column {
                categories.forEach { cat ->
                    val isDefault = cat.isDefault
                    val color = categoryColorFromHex(cat.colorHex)
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.size(8.dp))
                            Text(text = cat.displayName, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                            if (!isDefault) {
                                IconButton(onClick = { startEdit(cat) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "수정", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDelete(cat.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (editTarget != null || isAdding) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("영문 이름 (예: NEW_CAT)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = isAdding
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDisplay,
                        onValueChange = { editDisplay = it },
                        label = { Text("표시 이름 (예: 새 카테고리)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("색상 선택", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetColors.forEach { hex ->
                            val isSelected = hex == editColor
                            val c = categoryColorFromHex(hex)
                            IconButton(
                                onClick = { editColor = hex },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(if (isSelected) 28.dp else 24.dp)
                                        .clip(CircleShape)
                                        .background(c),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editTarget = null; isAdding = false }) {
                            Text("취소")
                        }
                        TextButton(
                            onClick = {
                                if (editName.isNotBlank() && editDisplay.isNotBlank()) {
                                    if (isAdding) onAdd(editName.uppercase(), editDisplay, editColor)
                                    else editTarget?.let { onUpdate(it.name, editDisplay, editColor) }
                                    editTarget = null; isAdding = false
                                }
                            }
                        ) {
                            Text("저장")
                        }
                    }
                } else {
                    TextButton(
                        onClick = { startAdd() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("새 카테고리 추가")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}
