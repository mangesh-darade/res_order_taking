package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.ApiClient
import com.example.data.api.ApiSettingsManager
import com.example.ui.theme.PinkLightBg
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Ensure manager is initialized
    LaunchedEffect(Unit) {
        ApiSettingsManager.init(context)
    }

    var isLoggedIn by remember { mutableStateOf(ApiSettingsManager.isAdminLoggedIn) }

    // Login Form State
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Settings Form State
    var baseUrlInput by remember { mutableStateOf(ApiSettingsManager.baseUrl) }
    var apiKeyInput by remember { mutableStateOf(ApiSettingsManager.apiKey) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PinkLightBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isLoggedIn) "API Configuration" else "Admin Login",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                if (!isLoggedIn) {
                    // LOGIN FORM
                    Text(
                        text = "Login as Admin to manage API Base URL & Credentials.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("admin") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_username_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PinkPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password / PIN") },
                        placeholder = { Text("1234 or admin") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PinkPrimary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary
                        )
                    )

                    loginError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val u = username.trim()
                            val p = password.trim()
                            if ((u.equals("admin", ignoreCase = true) || u.isEmpty()) && (p == "1234" || p == "admin" || p == "admin123" || p == "pass")) {
                                ApiSettingsManager.setAdminLoggedIn(context, true)
                                isLoggedIn = true
                                loginError = null
                                Toast.makeText(context, "Logged in as Admin", Toast.LENGTH_SHORT).show()
                            } else {
                                loginError = "Invalid credentials. Try username 'admin' & password '1234'."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_login_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOGIN TO SETTINGS", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Default Login: admin / 1234",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                } else {
                    // API CONFIGURATION FORM
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PinkLightBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = PinkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin Authorized",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                        }

                        TextButton(
                            onClick = {
                                ApiSettingsManager.setAdminLoggedIn(context, false)
                                isLoggedIn = false
                                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("LOGOUT", fontSize = 11.sp, color = PinkPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = {
                            baseUrlInput = it
                            testResult = null
                        },
                        label = { Text("Server Domain / URL") },
                        placeholder = { Text("http://devdinein.elintpos.in/") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_base_url_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = PinkPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter domain only (e.g. http://devdinein.elintpos.in/). 'ordertakingapi/' is appended automatically.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            testResult = null
                        },
                        label = { Text("X-API-KEY") },
                        placeholder = { Text("YOUR_X_API_KEY") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = PinkPrimary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary
                        )
                    )

                    // Quick URL helper buttons
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                baseUrlInput = "http://10.0.2.2/ElintOm_PHP_8.5/ordertakingapi/"
                            },
                            label = { Text("10.0.2.2 (Emulator)", fontSize = 10.sp) }
                        )
                        AssistChip(
                            onClick = {
                                baseUrlInput = "http://devdinein.elintpos.in/ordertakingapi/"
                            },
                            label = { Text("devdinein.elintpos.in", fontSize = 10.sp) }
                        )
                    }

                    // Test Connection Result Banner
                    testResult?.let { (success, msg) ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (success) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) Color(0xFF166534) else Color(0xFF991B1B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (success) Color(0xFF166534) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingConnection = true
                                    testResult = null
                                    // Save temporarily to test
                                    ApiClient.updateConfig(baseUrlInput, apiKeyInput)
                                    try {
                                        val response = ApiClient.service.getSections()
                                        val body = response.body()
                                        if (response.isSuccessful && (body?.response?.status.equals("SUCCESS", ignoreCase = true) || body?.data != null)) {
                                            val count = body?.data?.size ?: 0
                                            testResult = Pair(true, "Connected successfully! $count section(s) loaded.")
                                        } else {
                                            val code = response.code()
                                            testResult = Pair(false, "API returned code $code: ${response.message()}")
                                        }
                                    } catch (e: Exception) {
                                        testResult = Pair(false, "Connection Failed: ${e.localizedMessage ?: "Unknown network error"}")
                                    } finally {
                                        isTestingConnection = false
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PinkPrimary)
                            } else {
                                Text("TEST API", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                            }
                        }

                        Button(
                            onClick = {
                                ApiSettingsManager.saveSettings(context, baseUrlInput, apiKeyInput)
                                Toast.makeText(context, "API Settings Saved & Applied!", Toast.LENGTH_SHORT).show()
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SAVE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
