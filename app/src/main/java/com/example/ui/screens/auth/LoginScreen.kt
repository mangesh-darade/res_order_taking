package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.api.ApiSettingsManager
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.PinkLightBg
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToRegisterInfo: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ApiSettingsManager.init(context)
        viewModel.loadBranding(context)
    }

    val setupComplete = uiState.isSetupComplete

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Top right settings button for API configuration
        IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .testTag("login_settings_button")
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Server Settings",
                        tint = PinkPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!setupComplete) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("setup_required_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Server setup required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the settings gear (top right) → login with admin credentials → set API URL & X-API-KEY → TEST API → SAVE. Then you can login or create a staff account.",
                            fontSize = 12.sp,
                            color = TextDark,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Card Container
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Header
                    val logoUrl = uiState.branding.logoUrl ?: uiState.branding.webshopLogoUrl
                    if (!logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = "POS Logo",
                            modifier = Modifier
                                .height(64.dp)
                                .padding(bottom = 8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(PinkPrimary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = PinkPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = uiState.branding.siteName ?: "ElintOm Restaurant",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )

                    Text(
                        text = uiState.branding.loginTitle ?: "Restaurant Order Taking",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(bottom = 16.dp))

                    if (!uiState.isSetupComplete) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .testTag("setup_required_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Server setup required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the gear (⚙) → login with admin credentials → set API URL & key → TEST → SAVE. Then you can register / login.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFBF360C),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Error Message Banner
                    uiState.errorMessage?.let { error ->
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    fontSize = 12.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Identity Field
                    OutlinedTextField(
                        value = uiState.identity,
                        onValueChange = viewModel::onIdentityChanged,
                        enabled = uiState.isSetupComplete,
                        label = { Text("Username / Email / Mobile", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PinkPrimary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        enabled = uiState.isSetupComplete,
                        label = { Text("Password", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PinkPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisibility) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login(context) {
                                    Toast.makeText(context, "Welcome to ${uiState.branding.siteName}!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkPrimary,
                            focusedLabelColor = PinkPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(context) {
                                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }
                        },
                        enabled = !uiState.isLoading && uiState.isSetupComplete,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LOG IN AS CAPTAIN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismissRequest = { showSettingsDialog = false },
                onSetupSaved = {
                    showSettingsDialog = false
                    viewModel.refreshSetupState(context)
                    Toast.makeText(context, "Server configured. You can log in now.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
