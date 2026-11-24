package com.planner.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.Template
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.categoryColorFromHex

@Composable
fun TemplateDialog(
    templates: List<Template>,
    categories: List<CategoryEntity>,
    onSelect: (Template) -> Unit,
    onAdd: (String, String, String, Int) -> Unit,
    onDelete: (Template) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "") }
    var newNote by remember { mutableStateOf("") }
    var newMinutes by remember { mutableStateOf("") }

    val categoryMap = remember(categories) { categories.associateBy { it.name } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📋 기록 템플릿") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (!showAddForm) {
                    if (templates.isEmpty()) {
                        Text(
                            "저장된 템플릿이 없습니다.\n자주 쓰는 기록을 템플릿으로 저장해보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(templates, key = { it.id }) { tmpl ->
                                val cat = categoryMap[tmpl.category]
                                val color = if (cat != null) categoryColorFromHex(cat.colorHex) else Accent
                                val displayName = cat?.displayName ?: tmpl.category
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().clickable { onSelect(tmpl) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tmpl.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                "$displayName · ${tmpl.durationMinutes}분" + (if (tmpl.note.isNotEmpty()) " · ${tmpl.note}" else ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = color
                                            )
                                        }
                                        IconButton(onClick = { onDelete(tmpl) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showAddForm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("새 템플릿 추가")
                    }
                } else {
                    // 추가 폼
                    Text("새 템플릿", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("템플릿 이름 (예: 아침 운동)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CategorySelector(
                        categories = categories,
                        selected = newCategory,
                        onSelect = { newCategory = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMinutes,
                        onValueChange = { newMinutes = it },
                        label = { Text("기본 시간 (분)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text("기본 메모 (선택사항)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showAddForm = false }, modifier = Modifier.weight(1f)) { Text("취소") }
                        Button(
                            onClick = {
                                if (newName.isNotBlank() && newCategory.isNotBlank()) {
                                    onAdd(newName.trim(), newCategory, newNote.trim(), newMinutes.toIntOrNull() ?: 0)
                                    showAddForm = false
                                    newName = ""; newNote = ""; newMinutes = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            modifier = Modifier.weight(1f)
                        ) { Text("저장") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}
