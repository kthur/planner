package com.planner.tracker.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.Entry
import com.planner.tracker.ui.theme.Accent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditDialog(
    entry: Entry,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Entry) -> Unit
) {
    var editCat by remember(entry) { mutableStateOf(entry.category) }
    var editNote by remember(entry) { mutableStateOf(entry.note) }
    val isEvent = entry.entryType == "EVENT"

    var editStartTime by remember(entry) { mutableLongStateOf(if (entry.startTime > 0) entry.startTime else System.currentTimeMillis()) }
    var editEndTime by remember(entry) { mutableLongStateOf(if (entry.endTime > 0) entry.endTime else System.currentTimeMillis()) }
    var showEditStartTimePicker by remember { mutableStateOf(false) }
    var showEditEndTimePicker by remember { mutableStateOf(false) }
    val cal = remember { Calendar.getInstance() }

    val context = LocalContext.current
    var editPhotoUri by remember(entry) { mutableStateOf(entry.photoUri) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var tempPhotoPath by rememberSaveable { mutableStateOf("") }
    val newPhotosCreated = remember { mutableStateListOf<String>() }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempPhotoPath.isNotEmpty()) {
            val tempFile = java.io.File(tempPhotoPath)
            if (tempFile.exists()) {
                val photosDir = java.io.File(context.filesDir, "photos")
                val finalFile = java.io.File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
                if (tempFile.renameTo(finalFile)) {
                    editPhotoUri = finalFile.name
                    newPhotosCreated.add(finalFile.name)
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val photosDir = java.io.File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()
            val finalFile = java.io.File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(finalFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                editPhotoUri = finalFile.name
                newPhotosCreated.add(finalFile.name)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val dismissAction = {
        newPhotosCreated.forEach { filename ->
            val file = java.io.File(java.io.File(context.filesDir, "photos"), filename)
            if (file.exists()) file.delete()
        }
        onDismiss()
    }

    if (showPhotoDialog) {
        PhotoActionDialog(
            onDismiss = { showPhotoDialog = false },
            onTakePhoto = {
                val photosDir = java.io.File(context.filesDir, "photos")
                if (!photosDir.exists()) photosDir.mkdirs()
                val tempFile = java.io.File(photosDir, "temp_${System.currentTimeMillis()}.jpg")
                tempPhotoPath = tempFile.absolutePath
                val authority = "${context.packageName}.fileprovider"
                val providerUri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
                cameraLauncher.launch(providerUri)
            },
            onPickPhoto = {
                galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }

    if (showEditStartTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = editStartTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                cal.timeInMillis = editStartTime
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                editStartTime = cal.timeInMillis
                showEditStartTimePicker = false
            },
            onDismiss = { showEditStartTimePicker = false }
        )
    }

    if (showEditEndTimePicker) {
        val now = Calendar.getInstance().apply { timeInMillis = editEndTime }
        TimePickerDialogScreen(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            currentMinute = now.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                cal.timeInMillis = editEndTime
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                editEndTime = cal.timeInMillis
                showEditEndTimePicker = false
            },
            onDismiss = { showEditEndTimePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("항목 수정") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CategorySelector(categories = categories, selected = editCat, onSelect = { editCat = it })
                Spacer(modifier = Modifier.height(8.dp))

                val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                if (isEvent) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showEditStartTimePicker = true },
                            modifier = Modifier.width(180.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("발생 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(timeFormat.format(Date(editStartTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showEditStartTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("시작 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(timeFormat.format(Date(editStartTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showEditEndTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("종료 시간", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(timeFormat.format(Date(editEndTime)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Accent)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editNote, onValueChange = { editNote = it }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(modifier = Modifier.height(12.dp))
                Text("사진 첨부", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))

                if (!editPhotoUri.isNullOrEmpty()) {
                    val photoFile = remember(editPhotoUri) {
                        java.io.File(java.io.File(context.filesDir, "photos"), editPhotoUri!!)
                    }
                    if (photoFile.exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = photoFile,
                                contentDescription = "첨부 사진",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            androidx.compose.material3.IconButton(
                                onClick = { editPhotoUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "사진 삭제",
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        editPhotoUri = null
                    }
                } else {
                    OutlinedButton(
                        onClick = { showPhotoDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "사진 추가"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("사진 추가")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isEvent) {
                    onConfirm(entry.copy(category = editCat, note = editNote, minutes = 0, startTime = editStartTime, endTime = 0L, photoUri = editPhotoUri))
                } else {
                    val mins = if (editEndTime > editStartTime) ((editEndTime - editStartTime) / 60000).toInt() else 0
                    onConfirm(entry.copy(category = editCat, note = editNote, minutes = mins, startTime = editStartTime, endTime = editEndTime, photoUri = editPhotoUri))
                }
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = dismissAction) { Text("취소") } }
    )
}
