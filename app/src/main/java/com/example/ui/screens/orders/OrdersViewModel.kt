package com.example.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.OrderBootstrap
import com.example.data.model.TableItem
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrdersUiState(
    val isLoading: Boolean = false,
    val tableId: String? = null,
    val tablesList: List<TableItem> = emptyList(),
    val order: OrderBootstrap? = null,
    val selectedGuestFilter: Int = 0, // 0 = All, 1..N = Guest 1..N
    val snackbarMessage: String? = null,
    val isFinalized: Boolean = false
)

class OrdersViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadTablesList()
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
        _uiState.value = _uiState.value.copy(tableId = tableId, selectedGuestFilter = 0)
        if (tableId != null) {
            loadOrderData(tableId)
        }
    }

    fun refreshOrder() {
        val tId = _uiState.value.tableId
        if (tId != null) {
            loadOrderData(tId)
        }
    }

    fun loadOrderData(tableId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, snackbarMessage = null)
            val result = repository.fetchOrderBootstrap(tableId = tableId)
            result.onSuccess { order ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    order = order
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectGuestFilter(guestId: Int) {
        _uiState.value = _uiState.value.copy(selectedGuestFilter = guestId)
    }

    fun updateGuestCount(delta: Int) {
        val currentOrder = _uiState.value.order ?: return
        val orderId = currentOrder.orderId ?: return
        viewModelScope.launch {
            val result = repository.updateGuestCount(orderId, delta)
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
}
