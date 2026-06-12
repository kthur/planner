package com.planner.tracker.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.planner.tracker.data.CalendarSyncManager

@Composable
fun CalendarSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE) }

    // Load initial values
    var selectedAccount by remember { mutableStateOf(prefs.getString("calendar_account", "") ?: "") }
    var selectedCalendarId by remember { mutableLongStateOf(prefs.getLong("calendar_id", -1L)) }
    var selectedCalendarName by remember { mutableStateOf(prefs.getString("calendar_name", "") ?: "") }

    var accounts by remember { mutableStateOf(emptyList<String>()) }
    var calendars by remember { mutableStateOf(emptyList<Pair<Long, String>>()) }

    var accountExpanded by remember { mutableStateOf(false) }
    var calendarExpanded by remember { mutableStateOf(false) }

    var isCreatingNewCalendar by remember { mutableStateOf(false) }
    var newCalendarName by remember { mutableStateOf("Planner") }

    // Load accounts
    LaunchedEffect(Unit) {
        accounts = CalendarSyncManager.getGoogleAccounts(context)
        if (selectedAccount.isEmpty() && accounts.isNotEmpty()) {
            selectedAccount = accounts.first()
        }
    }

    // Load calendars when account changes
    LaunchedEffect(selectedAccount) {
        if (selectedAccount.isNotEmpty()) {
            calendars = CalendarSyncManager.getCalendarsForAccount(context, selectedAccount)
            // If the selected calendar is not in the new list, reset it
            if (calendars.none { it.first == selectedCalendarId }) {
                if (calendars.isNotEmpty()) {
                    selectedCalendarId = calendars.first().first
                    selectedCalendarName = calendars.first().second
                } else {
                    selectedCalendarId = -1L
                    selectedCalendarName = ""
                }
            }
        } else {
            calendars = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("구글 캘린더 연동 설정") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (accounts.isEmpty()) {
                    Text(
                        text = "기기에 등록된 구글 계정을 찾을 수 없습니다. 캘린더 권한이 허용되어 있는지 확인해 주세요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // 1. Account Selector
                    Text("연동할 구글 계정", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedAccount,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountExpanded = true },
                            enabled = false, // Disable manual typing, click handled via Box clickable
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // Invisible overlay to capture clicks since enabled = false in OutlinedTextField prevents clicking it directly
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { accountExpanded = true }
                        )
                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account) },
                                    onClick = {
                                        selectedAccount = account
                                        accountExpanded = false
                                        isCreatingNewCalendar = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Calendar Selector
                    Text("저장할 캘린더 선택", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val displayText = if (isCreatingNewCalendar) {
                            "+ 새로운 캘린더 생성"
                        } else if (selectedCalendarId != -1L) {
                            selectedCalendarName
                        } else {
                            "선택된 캘린더 없음"
                        }
                        
                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { calendarExpanded = true },
                            enabled = false,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { calendarExpanded = true }
                        )
                        DropdownMenu(
                            expanded = calendarExpanded,
                            onDismissRequest = { calendarExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            calendars.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedCalendarId = id
                                        selectedCalendarName = name
                                        isCreatingNewCalendar = false
                                        calendarExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ 새로운 캘린더 생성...") },
                                onClick = {
                                    isCreatingNewCalendar = true
                                    calendarExpanded = false
                                }
                            )
                        }
                    }

                    // 3. Custom Calendar Creator Form
                    if (isCreatingNewCalendar) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("새로운 캘린더 이름", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newCalendarName,
                            onValueChange = { newCalendarName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newCalendarName.isNotBlank() && selectedAccount.isNotEmpty()) {
                                    val newId = CalendarSyncManager.createCustomCalendar(
                                        context,
                                        selectedAccount,
                                        newCalendarName.trim()
                                    )
                                    if (newId != null) {
                                        calendars = CalendarSyncManager.getCalendarsForAccount(context, selectedAccount)
                                        selectedCalendarId = newId
                                        selectedCalendarName = newCalendarName.trim()
                                        isCreatingNewCalendar = false
                                        Toast.makeText(context, "새로운 캘린더가 생성되었습니다.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "캘린더 생성 실패. 권한을 확인해주세요.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("캘린더 생성 및 선택")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedAccount.isNotEmpty() && selectedCalendarId != -1L && !isCreatingNewCalendar) {
                        prefs.edit().apply {
                            putString("calendar_account", selectedAccount)
                            putLong("calendar_id", selectedCalendarId)
                            putString("calendar_name", selectedCalendarName)
                        }.apply()
                        Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else if (isCreatingNewCalendar) {
                        Toast.makeText(context, "캘린더 생성을 완료하거나 기존 캘린더를 선택해주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "계정과 캘린더를 선택해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
