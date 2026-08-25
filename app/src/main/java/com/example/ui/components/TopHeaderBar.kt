package com.example.ui.components

import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TopHeaderBar(
    onLogoutClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentUser by AuthRepository.getInstance().currentUser.collectAsState()
    val branding by AuthRepository.getInstance().branding.collectAsState()

    val todayDate = remember {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        formatter.format(Date())
    }

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
            Column(verticalArrangement = Arrangement.Center) {
                if (currentUser != null) {
                    Text(
                        text = currentUser?.displayName ?: "Staff Member",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentUser?.role ?: "Captain",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
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

            // Center: App / Site brand title
            Text(
                text = (branding.siteName ?: "ELINTOM").uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.testTag("app_brand_title")
            )

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

                    DropdownMenuItem(
                        text = { Text("Server & API Settings", fontSize = 13.sp, color = TextDark) },
                        onClick = {
                            menuExpanded = false
                            showSettingsDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = PinkPrimary)
                        },
                        modifier = Modifier.testTag("menu_api_settings")
                    )

                    if (currentUser != null && onLogoutClick != null) {
                        HorizontalDivider(color = Color(0xFFEEEEEE))

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

    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = { showSettingsDialog = false }
        )
    }
}


