package com.planner.tracker.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.CloudService
import com.planner.tracker.ui.theme.Accent
import com.planner.tracker.ui.theme.categoryColorFromHex
import com.planner.tracker.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShareScreen(
    viewModel: PlannerViewModel,
    categories: List<CategoryEntity>
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUserState.collectAsState()
    val profile by viewModel.userProfileState.collectAsState()
    val feed by viewModel.sharedFeed.collectAsState()
    val categoriesMap = remember(categories) { categories.associateBy { it.name } }

    var isSubmitting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (currentUser == null) {
            // 1. Auth view (Login/Signup)
            ShareAuthCard(
                onLogin = { email, password ->
                    isSubmitting = true
                    viewModel.loginUser(email, password) { success, msg ->
                        isSubmitting = false
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onRegister = { email, password, name ->
                    isSubmitting = true
                    viewModel.registerUser(email, password, name) { success, msg ->
                        isSubmitting = false
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else if (profile == null) {
            // Loading profile
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (profile?.groupId.isNullOrEmpty()) {
            // 2. Group Selection (Create or Join)
            ShareGroupCard(
                onJoinGroup = { code ->
                    isSubmitting = true
                    viewModel.joinGroup(code) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            Toast.makeText(context, "그룹 가입에 성공했습니다: $msg", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onCreateGroup = { groupName ->
                    isSubmitting = true
                    viewModel.createGroup(groupName) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            Toast.makeText(context, "그룹을 생성했습니다: 초대코드 $msg", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onLogout = {
                    viewModel.logoutUser()
                }
            )
        } else {
            // 3. Shared Feed View
            val groupId = profile?.groupId ?: ""
            val displayName = profile?.displayName ?: currentUser?.email ?: "멤버"

            Column(modifier = Modifier.fillMaxSize()) {
                // Group Header
                GroupHeader(
                    groupCode = groupId,
                    userName = displayName,
                    onLogout = { viewModel.logoutUser() },
                    onLeaveGroup = {
                        isSubmitting = true
                        viewModel.leaveGroup(groupId) { success, msg ->
                            isSubmitting = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRefresh = { viewModel.syncUnsyncedEntries() }
                )

                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                // Feed List
                if (feed.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "피드 비어있음",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "아직 공유된 시간 기록이 없습니다.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "오늘의 기록을 작성하여 첫 피드를 남겨보세요!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(feed) { entry ->
                            FeedItemCard(entry, categoriesMap)
                        }
                    }
                }
            }
        }

        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        }
    }
}

@Composable
fun ShareAuthCard(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "멤버 공유 공간",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "로그인하여 친구들과 오늘 하루를 공유하세요",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Accent
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("로그인", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("회원가입", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("이메일 주소") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        focusedLabelColor = Accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedTab == 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("사용할 닉네임") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            focusedLabelColor = Accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("비밀번호") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        focusedLabelColor = Accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                            if (selectedTab == 0) {
                                onLogin(email.trim(), password.trim())
                            } else {
                                if (displayName.trim().isNotEmpty()) {
                                    onRegister(email.trim(), password.trim(), displayName.trim())
                                } else {
                                    onRegister(email.trim(), password.trim(), "멤버")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (selectedTab == 0) "로그인" else "가입하고 시작하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ShareGroupCard(
    onJoinGroup: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onLogout: () -> Unit
) {
    var groupCode by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var isCreatingGroup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "공유 그룹 설정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "기존 그룹에 가입하거나 새 공유 그룹을 만드세요",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(visible = !isCreatingGroup) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = groupCode,
                            onValueChange = { groupCode = it },
                            label = { Text("6자리 초대 코드 입력 (예: GP-XXXXXX)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                focusedLabelColor = Accent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (groupCode.trim().isNotEmpty()) {
                                    onJoinGroup(groupCode.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("그룹 가입 완료", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "또는 새 그룹을 만들고 싶으신가요?",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "새 그룹 만들기",
                                fontSize = 13.sp,
                                color = Accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isCreatingGroup = true }
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isCreatingGroup) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("새 그룹 이름 (예: 우리 가족, 스터디룸)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                focusedLabelColor = Accent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (groupName.trim().isNotEmpty()) {
                                    onCreateGroup(groupName.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("새 그룹 생성 완료", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "초대 코드를 가지고 계신가요?",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "그룹 가입하기",
                                fontSize = 13.sp,
                                color = Accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isCreatingGroup = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                TextButtonHelper(
                    text = "로그아웃",
                    icon = Icons.Default.ExitToApp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun GroupHeader(
    groupCode: String,
    userName: String,
    onLogout: () -> Unit,
    onLeaveGroup: () -> Unit,
    onRefresh: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "공유 공간",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "초대 코드: $groupCode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "복사",
                        tint = Accent,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(groupCode))
                                Toast
                                    .makeText(context, "초대 코드가 복사되었습니다.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "동기화", tint = Accent)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onLeaveGroup) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "그룹 탈퇴", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Accent.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = Accent)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$userName 님으로 접속 중",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "로그아웃",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.clickable { onLogout() }
            )
        }
    }
}

@Composable
fun FeedItemCard(
    entry: CloudService.SharedEntry,
    categoriesMap: Map<String, CategoryEntity>
) {
    val dateFormat = remember { SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN) }
    val entryDateStr = remember(entry.date) { dateFormat.format(Date(entry.date)) }

    val cat = categoriesMap[entry.category]
    val displayName = cat?.displayName ?: entry.category
    val color = if (cat != null) categoryColorFromHex(cat.colorHex) else Accent

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Writer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.2f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = entry.userName.take(1).uppercase()
                    Text(
                        text = initial,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = entry.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = entryDateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category & Time Spent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${entry.minutes / 60}시간 ${entry.minutes % 60}분 기록",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (entry.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = entry.note,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Image Preview (if present)
            if (!entry.photoUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = entry.photoUrl,
                    contentDescription = "공유 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
fun TextButtonHelper(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
    }
}
