package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.example.data.model.RegisterOption
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
    var groups by remember { mutableStateOf<List<RegisterOption>>(emptyList()) }
    var warehouses by remember { mutableStateOf<List<RegisterOption>>(emptyList()) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedWarehouseIds by remember { mutableStateOf(setOf<String>()) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingOptions by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoadingOptions = true
        val info = AuthRepository.getInstance().getRegisterInfo()
        groups = info.groups.orEmpty().filter { !it.id.isNullOrBlank() }
        warehouses = info.warehouses.orEmpty().filter { !it.id.isNullOrBlank() }
        if (selectedGroupId == null && groups.isNotEmpty()) {
            val preferred = groups.firstOrNull {
                val n = it.name.orEmpty().lowercase()
                n.contains("sales") || n.contains("employee") || n.contains("kitchen") || n.contains("manager")
            } ?: groups.first()
            selectedGroupId = preferred.id
        }
        if (selectedWarehouseIds.isEmpty() && warehouses.size == 1) {
            selectedWarehouseIds = setOf(warehouses.first().id!!)
        }
        isLoadingOptions = false
    }

    val selectedGroupLabel = groups.firstOrNull { it.id == selectedGroupId }?.let { opt ->
        opt.description?.takeIf { it.isNotBlank() } ?: opt.name.orEmpty()
    } ?: "Select staff group"

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
                text = "Account is saved in ElintOm users. Group and Location are required (same as admin).",
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

                    Text("Group *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isLoadingOptions) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            color = PinkPrimary
                        )
                    } else {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)) {
                            OutlinedTextField(
                                value = selectedGroupLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { groupMenuExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select group")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PinkPrimary,
                                    focusedLabelColor = PinkPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_group_field")
                                    .clickable { groupMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = groupMenuExpanded,
                                onDismissRequest = { groupMenuExpanded = false }
                            ) {
                                groups.forEach { opt ->
                                    val label = opt.description?.takeIf { it.isNotBlank() } ?: opt.name.orEmpty()
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedGroupId = opt.id
                                            groupMenuExpanded = false
                                            errorMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text("Location * (select at least one)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!isLoadingOptions && warehouses.isEmpty()) {
                        Text(
                            text = "No locations found from API. Configure warehouses in ElintOm.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .testTag("register_warehouse_list")
                        ) {
                            warehouses.forEach { wh ->
                                val id = wh.id ?: return@forEach
                                val checked = selectedWarehouseIds.contains(id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedWarehouseIds = if (checked) {
                                                selectedWarehouseIds - id
                                            } else {
                                                selectedWarehouseIds + id
                                            }
                                            errorMessage = null
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { on ->
                                            selectedWarehouseIds = if (on) {
                                                selectedWarehouseIds + id
                                            } else {
                                                selectedWarehouseIds - id
                                            }
                                            errorMessage = null
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = PinkPrimary)
                                    )
                                    Text(
                                        text = wh.name ?: id,
                                        color = TextDark,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    RegisterField("Password (8–25)", password, KeyboardType.Password, true) { password = it; errorMessage = null }
                    RegisterField("Confirm password", passwordConfirm, KeyboardType.Password, true) { passwordConfirm = it; errorMessage = null }

                    Button(
                        onClick = {
                            if (firstName.isBlank() || lastName.isBlank() || username.isBlank() || email.isBlank()) {
                                errorMessage = "Fill all required fields."
                                return@Button
                            }
                            if (selectedGroupId.isNullOrBlank()) {
                                errorMessage = "Select a staff group."
                                return@Button
                            }
                            if (selectedWarehouseIds.isEmpty()) {
                                errorMessage = "Select at least one location."
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
                                    firstName,
                                    lastName,
                                    username,
                                    email,
                                    phone.ifBlank { null },
                                    password,
                                    passwordConfirm,
                                    selectedGroupId!!,
                                    selectedWarehouseIds.toList()
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
                        enabled = !isLoading && !isLoadingOptions,
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("register_submit_button")
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
