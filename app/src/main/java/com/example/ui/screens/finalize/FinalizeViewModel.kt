package com.example.ui.screens.finalize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FinalizeOrderResponse
import com.example.data.model.OrderBootstrap
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FinalizeUiState(
    val isLoading: Boolean = false,
    val orderId: String = "",
    val order: OrderBootstrap? = null,
    val finalizeResult: FinalizeOrderResponse? = null,
    val showSuccessBanner: Boolean = true,
    val autoNavigateTimer: Int = 7
)

class FinalizeViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinalizeUiState())
    val uiState: StateFlow<FinalizeUiState> = _uiState.asStateFlow()

    fun loadAndFinalize(orderId: String, onAutoNavigate: () -> Unit) {
        _uiState.value = _uiState.value.copy(orderId = orderId, isLoading = true)
        viewModelScope.launch {
            val orderRes = repository.fetchOrderBootstrap(null, orderId)
            val orderData = orderRes.getOrNull()

            val finRes = repository.finalizeOrder(orderId)
            val finData = finRes.getOrNull()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                order = orderData,
                finalizeResult = finData,
                showSuccessBanner = true
            )

            // Auto-hide green banner after 5s
            launch {
                delay(5000)
                _uiState.value = _uiState.value.copy(showSuccessBanner = false)
            }

            // Auto navigate back to Tables after 7s countdown
            for (sec in 7 downTo 1) {
                _uiState.value = _uiState.value.copy(autoNavigateTimer = sec)
                delay(1000)
            }
            onAutoNavigate()
        }
    }
}
