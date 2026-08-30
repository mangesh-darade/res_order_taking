package com.example.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiSettingsManager
import com.example.data.model.BrandingInfo
import com.example.data.model.LoginUser
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val identity: String = "",
    val password: String = "",
    val selectedRole: String = "Captain / Waiter",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val branding: BrandingInfo = BrandingInfo(),
    val loggedInUser: LoginUser? = null,
    val isSetupComplete: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val authRepo = AuthRepository.getInstance()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun loadBranding(context: Context) {
        viewModelScope.launch {
            authRepo.init(context)
            val setup = ApiSettingsManager.isSetupComplete
            val branding = if (setup) {
                authRepo.fetchBranding()
            } else {
                BrandingInfo(
                    siteName = "Order Taking",
                    loginTitle = "Configure API server to continue",
                    primaryColor = "#E9176B"
                )
            }
            _uiState.value = _uiState.value.copy(branding = branding, isSetupComplete = setup)
        }
    }

    fun refreshSetupState(context: Context) {
        authRepo.init(context)
        _uiState.value = _uiState.value.copy(isSetupComplete = ApiSettingsManager.isSetupComplete)
        if (ApiSettingsManager.isSetupComplete) {
            loadBranding(context)
        }
    }

    fun onIdentityChanged(value: String) {
        _uiState.value = _uiState.value.copy(identity = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onRoleSelected(role: String) {
        _uiState.value = _uiState.value.copy(selectedRole = role)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun login(context: Context, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.identity.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter username, email, or identity.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepo.login(
                context = context,
                identity = state.identity,
                password = state.password,
                selectedRole = state.selectedRole
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loggedInUser = user
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Login failed. Please check credentials."
                    )
                }
            )
        }
    }
}
