package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(1) } // 1=identity 2=otp 3=new password
    var identity by remember { mutableStateOf("") }
    var emailMasked by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PinkPrimary)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("forgot_password_screen"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (step) {
                            1 -> "Send OTP to email"
                            2 -> "Enter OTP"
                            else -> "Set new password"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (step) {
                            1 -> "OTP will be sent to your registered email in ElintOm."
                            2 -> "Check email${if (emailMasked.isNotBlank()) " ($emailMasked)" else ""}. OTP valid 10 minutes."
                            else -> "Enter a new password (8–25 characters)."
                        },
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    errorMessage?.let { err ->
                        Text(text = err, fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    when (step) {
                        1 -> {
                            OutlinedTextField(
                                value = identity,
                                onValueChange = { identity = it; errorMessage = null },
                                label = { Text("Username / Email / Phone") },
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = PinkPrimary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (identity.isBlank()) {
                                        errorMessage = "Enter username, email, or phone."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val result = AuthRepository.getInstance().forgotPassword(identity)
                                        isLoading = false
                                        result.fold(
                                            onSuccess = { data ->
                                                emailMasked = data["email_masked"].orEmpty()
                                                Toast.makeText(context, data["message"] ?: "OTP sent", Toast.LENGTH_SHORT).show()
                                                step = 2
                                            },
                                            onFailure = { errorMessage = it.message }
                                        )
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                else Text("SEND OTP", fontWeight = FontWeight.Bold)
                            }
                        }
                        2 -> {
                            OutlinedTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() }; errorMessage = null },
                                label = { Text("6-digit OTP") },
                                leadingIcon = { Icon(Icons.Default.Pin, null, tint = PinkPrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (otp.length != 6) {
                                        errorMessage = "Enter 6-digit OTP."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val result = AuthRepository.getInstance().verifyResetOtp(identity, otp)
                                        isLoading = false
                                        result.fold(
                                            onSuccess = { step = 3 },
                                            onFailure = { errorMessage = it.message }
                                        )
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                else Text("VERIFY OTP", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { step = 1; otp = "" }) {
                                Text("Resend / change identity", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                        else -> {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; errorMessage = null },
                                label = { Text("New password") },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = PinkPrimary) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it; errorMessage = null },
                                label = { Text("Confirm password") },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = PinkPrimary) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (password.length < 8) {
                                        errorMessage = "Password must be at least 8 characters."
                                        return@Button
                                    }
                                    if (password != passwordConfirm) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val result = AuthRepository.getInstance().resetPassword(identity, otp, password, passwordConfirm)
                                        isLoading = false
                                        result.fold(
                                            onSuccess = { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                onBackToLogin()
                                            },
                                            onFailure = { errorMessage = it.message }
                                        )
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                else Text("SAVE & GO TO LOGIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onBackToLogin) {
                        Text("Back to Login", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
