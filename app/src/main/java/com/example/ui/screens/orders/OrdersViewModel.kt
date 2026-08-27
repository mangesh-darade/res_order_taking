package com.example.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.OrderBootstrap
import com.example.data.model.TableItem
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OrdersUiState(
    val isLoading: Boolean = false,
    val tableId: String? = null,
    val tablesList: List<TableItem> = emptyList(),
    val order: OrderBootstrap? = null,
    val selectedGuestFilter: Int = -1, // -1 = All, 0 = Table Items, 1..N = Guest 1..N
    val snackbarMessage: String? = null,
    val isFinalized: Boolean = false
)

class OrdersViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            repository.fetchBranding()
        }
        loadTablesList()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(3000)
                val tId = _uiState.value.tableId
                if (tId != null) {
                    loadOrderData(tId, silent = true)
                }
            }
        }
    }

    fun loadTablesList() {
        viewModelScope.launch {
            val result = repository.fetchTables("1")
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(tablesList = list)
            }
        }
    }

    fun setTableId(tableId: String?) {
        _uiState.value = _uiState.value.copy(tableId = tableId, selectedGuestFilter = -1)
        if (tableId != null) {
            loadOrderData(tableId, silent = false)
        }
    }

    fun refreshOrder() {
        val tId = _uiState.value.tableId
        if (tId != null) {
            loadOrderData(tId, silent = false)
        }
    }

    fun loadOrderData(tableId: String, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent && _uiState.value.order == null) {
                _uiState.value = _uiState.value.copy(isLoading = true, snackbarMessage = null)
            }
            val result = repository.fetchOrderBootstrap(tableId = tableId)
            result.onSuccess { order ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    order = order
                )
            }.onFailure {
                if (!silent) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun selectGuestFilter(guestId: Int) {
        _uiState.value = _uiState.value.copy(selectedGuestFilter = guestId)
    }

    fun updateGuestCount(delta: Int) {
        val currentOrder = _uiState.value.order
        val orderId = currentOrder?.orderId ?: ""
        val tableId = _uiState.value.tableId ?: currentOrder?.tableId ?: "1"
        viewModelScope.launch {
            val result = repository.updateGuestCount(orderId, delta, tableId)
            result.onSuccess { updated ->
                _uiState.value = _uiState.value.copy(order = updated)
            }
        }
    }

    fun updateItemQty(itemId: String, newQty: Int) {
        val currentOrder = _uiState.value.order ?: return
        val orderId = currentOrder.orderId ?: return
        viewModelScope.launch {
            val result = repository.updateItemQuantity(orderId, itemId, newQty)
            result.onSuccess { updated ->
                _uiState.value = _uiState.value.copy(order = updated)
            }
        }
    }

    fun sendKot() {
        val currentOrder = _uiState.value.order ?: return
        val orderId = currentOrder.orderId ?: return
        if (currentOrder.totalItems == 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Cannot send KOT: Order is empty")
            return
        }
        viewModelScope.launch {
            val result = repository.sendKot(orderId)
            result.onSuccess { updated ->
                _uiState.value = _uiState.value.copy(
                    order = updated,
                    snackbarMessage = "KOT Sent to Kitchen!"
                )
            }
        }
    }

    fun markServed() {
        val currentOrder = _uiState.value.order ?: return
        val orderId = currentOrder.orderId ?: return
        viewModelScope.launch {
            val result = repository.markOrderServed(orderId)
            result.onSuccess { updated ->
                _uiState.value = _uiState.value.copy(
                    order = updated,
                    snackbarMessage = "Order Marked as Served!"
                )
            }
        }
    }

    fun validateAndFinalize(onSuccess: (orderId: String) -> Unit) {
        val order = _uiState.value.order
        if (order == null || order.totalItems == 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Add items first before finalizing order")
            return
        }
        if (order.status == "active" || order.guests.flatMap { it.items }.any { it.status == "pending" }) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Send KOT first before finalizing order")
            return
        }
        onSuccess(order.orderId ?: "")
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
