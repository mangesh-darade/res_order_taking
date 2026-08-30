package com.example.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BrandingInfo
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SplashUiState(
    val isLoading: Boolean = true,
    val branding: BrandingInfo = BrandingInfo(),
    val isLoggedIn: Boolean = false,
    val isSetupComplete: Boolean = false
)

class SplashViewModel : ViewModel() {
    private val authRepo = AuthRepository.getInstance()

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /**
     * @param onResult (isLoggedIn, isSetupComplete)
     */
    fun checkAuthAndLoadBranding(context: Context, onResult: (isLoggedIn: Boolean, isSetupComplete: Boolean) -> Unit) {
        viewModelScope.launch {
            authRepo.init(context)
            val setupDone = authRepo.isSetupComplete(context)
            val loggedIn = authRepo.isLoggedIn(context)
            val branding = if (setupDone) {
                authRepo.fetchBranding()
            } else {
                BrandingInfo(
                    siteName = "Order Taking",
                    loginTitle = "Configure server first",
                    primaryColor = "#E9176B"
                )
            }

            _uiState.value = SplashUiState(
                isLoading = false,
                branding = branding,
                isLoggedIn = loggedIn,
                isSetupComplete = setupDone
            )

            delay(700)
            onResult(loggedIn && setupDone, setupDone)
        }
    }
}
