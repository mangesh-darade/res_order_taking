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
    val isLoggedIn: Boolean = false
)

class SplashViewModel : ViewModel() {
    private val authRepo = AuthRepository.getInstance()

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    fun checkAuthAndLoadBranding(context: Context, onResult: (isLoggedIn: Boolean) -> Unit) {
        viewModelScope.launch {
            authRepo.init(context)
            val branding = authRepo.fetchBranding()
            val loggedIn = authRepo.isLoggedIn(context)
            
            _uiState.value = SplashUiState(
                isLoading = false,
                branding = branding,
                isLoggedIn = loggedIn
            )
            
            delay(800) // Brief smooth splash experience
            onResult(loggedIn)
        }
    }
}
