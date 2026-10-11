package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.AuthRepository
import com.example.data.sync.SyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OfflineGraceOverlay() {
    val context = LocalContext.current
    val syncManager = remember { SyncManager.getInstance(context) }
    val isOnline by syncManager.isOnline.collectAsState()
    val offlineRemainingSeconds by syncManager.offlineRemainingSeconds.collectAsState()
    val isGracePeriodExpired by syncManager.isGracePeriodExpired.collectAsState()
    val justRestoredOnline by syncManager.justRestoredOnline.collectAsState()
    val currentUser by AuthRepository.getInstance().currentUser.collectAsState()

    // Only active when a user/captain is actively logged in
    if (currentUser == null) return

    val coroutineScope = rememberCoroutineScope()
    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionCheckFailed by remember { mutableStateOf(false) }

    // State for user dismissing the 5s warning dialog manually if desired
    var dismissedAlertSecond by remember { mutableStateOf<Int?>(null) }
    val showFiveSecondAlert = !isOnline &&
            !isGracePeriodExpired &&
            offlineRemainingSeconds != null &&
            offlineRemainingSeconds!! in 1..SyncManager.OFFLINE_ALERT_THRESHOLD_SECONDS &&
            dismissedAlertSecond != offlineRemainingSeconds

    // 1. Alert in the last 5 seconds (5..1)
    if (showFiveSecondAlert) {
        val remaining = offlineRemainingSeconds ?: 5
        Dialog(
            onDismissRequest = { dismissedAlertSecond = remaining },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated countdown badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0))
                            .border(3.dp, Color(0xFFFF9800), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$remaining",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE65100)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "इंटरनेट कनेक्शन गेले आहे!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD84315),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Warning: Internet Connection Lost",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "३० सेकंदांपैकी शेवटचे $remaining सेकंद शिल्लक आहेत. ॲप लॉक होण्यापूर्वी कृपया ताबडतोब Wi-Fi किंवा इंटरनेट कनेक्ट करा!",
                        fontSize = 13.sp,
                        color = Color(0xFF37474F),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { dismissedAlertSecond = remaining },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "समजले / Continue ($remaining s)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // 2. Full Lockout Dialog after 30 seconds expired
    if (!isOnline && isGracePeriodExpired) {
        Dialog(
            onDismissRequest = { /* non-dismissible */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Offline Lock",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "इंटरनेट कनेक्शन आवश्यक आहे",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB71C1C),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Internet Connection Required",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFAFAFA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "• ३० सेकंदांची ऑफलाइन मुदत संपली आहे.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF263238)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• डेटा सुरक्षित ठेवण्यासाठी नवीन ऑर्डर्स घेणे तात्पुरते थांबवले आहे.",
                                fontSize = 12.sp,
                                color = Color(0xFF455A64)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✅ मागील ३० सेकंदांतील सर्व डेटा सुरक्षित आहे. इंटरनेट येताच तो आपोआप सिंक व रिस्टोअर होईल.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    if (connectionCheckFailed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "अजूनही इंटरनेट सुरू झालेले नाही. कृपया Wi-Fi चालू करा.",
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCheckingConnection = true
                                connectionCheckFailed = false
                                delay(600)
                                val connected = syncManager.retryConnection()
                                isCheckingConnection = false
                                if (!connected) {
                                    connectionCheckFailed = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isCheckingConnection
                    ) {
                        if (isCheckingConnection) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("तपासत आहे... / Checking...", color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "इंटरनेट पुन्हा तपासा (Retry)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // 3. Online Restored Success Banner (Sliding from top)
    AnimatedVisibility(
        visible = justRestoredOnline,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            color = Color(0xFF2E7D32),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Restored",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "इंटरनेट पूर्ववत झाले! (Internet Restored)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "मागील ३० सेकंदांचा डेटा सिंक झाला असून ॲप Live झाले आहे.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
