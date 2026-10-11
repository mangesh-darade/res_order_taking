package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.ui.theme.PinkLightBg
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopHeaderBar(
    onLogoutClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val currentUser by AuthRepository.getInstance().currentUser.collectAsState()
    val branding by AuthRepository.getInstance().branding.collectAsState()

    val todayDate = remember {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        formatter.format(Date())
    }

    val syncManager = remember { com.example.data.sync.SyncManager.getInstance(context) }
    val isOnline by syncManager.isOnline.collectAsState()
    val pendingCount by syncManager.pendingCountFlow.collectAsState(initial = 0)
    val failedCount by syncManager.failedCountFlow.collectAsState(initial = 0)
    val isSyncing by syncManager.isSyncing.collectAsState()
    val offlineRemainingSeconds by syncManager.offlineRemainingSeconds.collectAsState()
    val isGracePeriodExpired by syncManager.isGracePeriodExpired.collectAsState()

    var showSyncDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(PinkPrimary)
            .padding(horizontal = 16.dp)
            .testTag("top_header_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: User Profile badge or Date
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { showSyncDialog = true }
                    .padding(vertical = 4.dp)
            ) {
                if (currentUser != null) {
                    Text(
                        text = currentUser?.displayName ?: "Staff Member",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusDotColor = when {
                            failedCount > 0 -> Color(0xFFF44336)
                            !isOnline -> if (isGracePeriodExpired) Color(0xFFD32F2F) else Color(0xFFFF9800)
                            pendingCount > 0 -> Color(0xFFFFC107)
                            else -> Color(0xFF4CAF50)
                        }
                        Surface(
                            shape = CircleShape,
                            color = statusDotColor,
                            modifier = Modifier.size(7.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusText = when {
                            failedCount > 0 -> "SYNC FAILED ($failedCount)"
                            !isOnline -> {
                                if (isGracePeriodExpired) "OFFLINE LOCKED ($pendingCount)"
                                else if (offlineRemainingSeconds != null) "OFFLINE: ${offlineRemainingSeconds}s ($pendingCount)"
                                else "OFFLINE ($pendingCount)"
                            }
                            pendingCount > 0 -> "SYNCING ($pendingCount)"
                            else -> currentUser?.role ?: "Captain"
                        }
                        Text(
                            text = statusText,
                            color = if (failedCount > 0 || isGracePeriodExpired) Color(0xFFFFCDD2) else Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = if (failedCount > 0 || isGracePeriodExpired) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = todayDate,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("today_date_text")
                    )
                }
            }

            // Center: App / Site brand title & Logo from settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1.5f, fill = false)
                    .clickable { showSyncDialog = true }
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val logoUrl = branding.logoUrl ?: branding.webshopLogoUrl
                if (!logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = branding.siteName ?: "Logo",
                        modifier = Modifier
                            .height(34.dp)
                            .padding(end = 8.dp)
                    )
                }
                val brandTitle = branding.siteName?.ifBlank { null } ?: "ELINTOM"
                Text(
                    text = brandTitle.uppercase(),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("app_brand_title")
                )
                if (failedCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "⚠ $failedCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (pendingCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "⚡ $pendingCount",
                            color = Color.Yellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right: Hamburger / Dropdown Menu
            Box {
                IconButton(
                    onClick = {
                        if (onMenuClick != null) {
                            onMenuClick()
                        } else {
                            menuExpanded = true
                        }
                    },
                    modifier = Modifier.testTag("hamburger_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    if (currentUser != null) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = currentUser?.displayName ?: "Staff User",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Role: ${currentUser?.role ?: "Staff"}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            },
                            onClick = { },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PinkPrimary)
                            }
                        )

                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }

                    // Offline Sync Queue & History Option
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sync Queue & Status", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                if (failedCount > 0) {
                                    Surface(shape = CircleShape, color = Color(0xFFD32F2F), modifier = Modifier.size(18.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$failedCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (pendingCount > 0) {
                                    Surface(shape = CircleShape, color = Color(0xFFFF9800), modifier = Modifier.size(18.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$pendingCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            showSyncDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (failedCount > 0) Icons.Default.Warning else Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (failedCount > 0) Color(0xFFD32F2F) else PinkPrimary
                            )
                        }
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Server & API Settings: only on Login gear (Darade auth) — hidden after login
                    if (currentUser != null && onLogoutClick != null) {
                        DropdownMenuItem(
                            text = { Text("LOGOUT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red) },
                            onClick = {
                                menuExpanded = false
                                AuthRepository.getInstance().logout(context)
                                onLogoutClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                            },
                            modifier = Modifier.testTag("menu_logout_item")
                        )
                    }
                }
            }
        }
    }

    if (showSyncDialog) {
        SyncStatusDialog(onDismissRequest = { showSyncDialog = false })
    }
}


