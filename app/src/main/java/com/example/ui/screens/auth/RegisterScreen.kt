package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.data.repository.AuthRepository
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onRegistered: () -> Unit = onBackToLogin
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Staff Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PinkPrimary)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("register_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Account is saved in ElintOm users (using your configured API URL).",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            errorMessage?.let {
                Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Register", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(12.dp))

                    RegisterField("First name", firstName) { firstName = it; errorMessage = null }
                    RegisterField("Last name", lastName) { lastName = it; errorMessage = null }
                    RegisterField("Username", username) { username = it; errorMessage = null }
                    RegisterField("Email", email, KeyboardType.Email) { email = it; errorMessage = null }
                    RegisterField("Phone (optional)", phone, KeyboardType.Phone) { phone = it; errorMessage = null }
                    RegisterField("Password (8–25)", password, KeyboardType.Password, true) { password = it; errorMessage = null }
                    RegisterField("Confirm password", passwordConfirm, KeyboardType.Password, true) { passwordConfirm = it; errorMessage = null }

                    Button(
                        onClick = {
                            if (firstName.isBlank() || lastName.isBlank() || username.isBlank() || email.isBlank()) {
                                errorMessage = "Fill all required fields."
                                return@Button
                            }
                            if (password.length < 8) {
                                errorMessage = "Password must be at least 8 characters."
                                return@Button
                            }
                            if (password != passwordConfirm) {
                                errorMessage = "Passwords do not match."
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val result = AuthRepository.getInstance().register(
                                    firstName, lastName, username, email, phone.ifBlank { null }, password, passwordConfirm
                                )
                                isLoading = false
                                result.fold(
                                    onSuccess = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        onRegistered()
                                    },
                                    onFailure = { errorMessage = it.message }
                                )
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        else Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterField(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    passwordField: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (passwordField) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}
