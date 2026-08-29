package com.example.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CustomizationOption
import com.example.data.model.MenuItem
import com.example.data.model.OrderBootstrap
import com.example.data.model.OrderItem
import com.example.data.model.ProductCustomization
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
    val isFinalized: Boolean = false,
    val isSendingKot: Boolean = false,
    val editItem: OrderItem? = null,
    val editMenuItem: MenuItem? = null,
    val editCustomization: ProductCustomization? = null,
    val isEditLoading: Boolean = false
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
                if (tId != null && !_uiState.value.isSendingKot) {
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
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Could not update item"
                )
            }
        }
    }

    /** Tap pending/kot item → reopen customize sheet in edit mode. */
    fun openEditItem(item: OrderItem) {
        val st = item.status?.lowercase()?.trim().orEmpty()
        if (st in listOf("cancelled", "canceled")) {
            return
        }
        if (st in listOf("ready", "served")) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Item already $st — cancel and re-add to change customization."
            )
            return
        }
        viewModelScope.launch {
            val menuItem = MenuItem(
                id = item.productId,
                name = item.productName,
                price = item.price,
                vegType = item.vegType
            )
            _uiState.value = _uiState.value.copy(
                editItem = item,
                editMenuItem = menuItem,
                editCustomization = null,
                isEditLoading = true
            )
            val result = repository.fetchProductCustomizations(item.productId)
            result.onSuccess { custom ->
                _uiState.value = _uiState.value.copy(
                    editCustomization = custom,
                    isEditLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isEditLoading = false,
                    snackbarMessage = "Could not load customization options"
                )
            }
        }
    }

    fun closeEditItem() {
        _uiState.value = _uiState.value.copy(
            editItem = null,
            editMenuItem = null,
            editCustomization = null,
            isEditLoading = false
        )
    }

    fun addCustomAllergyForEdit(
        name: String,
        onResult: (Result<CustomizationOption>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addAllergy(name)
            result.onSuccess { option ->
                val current = _uiState.value.editCustomization
                if (current != null) {
                    val existing = current.allergies.orEmpty()
                    val already = existing.any { it.name.equals(option.name, ignoreCase = true) }
                    val updatedList = if (already) existing else existing + option
                    _uiState.value = _uiState.value.copy(
                        editCustomization = current.copy(allergies = updatedList)
                    )
                }
            }
            onResult(result)
        }
    }

    fun saveEditItem(
        qty: Int,
        spice: String?,
        meat: String?,
        allergies: List<String>,
        addOns: List<String>,
        toppings: List<String>,
        noOnion: Boolean,
        noGarlic: Boolean,
        specialInstructions: String?
    ) {
        val order = _uiState.value.order ?: return
        val orderId = order.orderId ?: return
        val item = _uiState.value.editItem ?: return
        viewModelScope.launch {
            val result = repository.updateItemDetails(
                orderId = orderId,
                itemId = item.id,
                quantity = qty,
                spiceLevel = spice,
                meatWellness = meat,
                allergies = allergies,
                addOns = addOns,
                toppings = toppings,
                onionFlag = noOnion,
                garlicFlag = noGarlic,
                specialInstructions = specialInstructions
            )
            result.onSuccess { updated ->
                _uiState.value = _uiState.value.copy(
                    order = updated,
                    editItem = null,
                    editMenuItem = null,
                    editCustomization = null,
                    isEditLoading = false,
                    snackbarMessage = "Item updated"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Could not update item"
                )
            }
        }
    }

    fun sendKot() {
        val currentOrder = _uiState.value.order ?: return
        val orderId = currentOrder.orderId ?: return
        if (_uiState.value.isSendingKot) {
            return
        }
        val items = currentOrder.guests.flatMap { it.items }
        val hasPending = items.any { it.status.equals("pending", ignoreCase = true) }
        if (currentOrder.totalItems == 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Cannot send KOT: Order is empty")
            return
        }
        if (!hasPending) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "No pending items to send. KOT already up to date.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingKot = true)
            val result = repository.sendKot(orderId)
            result.onSuccess { updated ->
                _uiState.value = _uiState.value.copy(
                    order = updated,
                    isSendingKot = false,
                    snackbarMessage = "KOT Sent to Kitchen!"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSendingKot = false,
                    snackbarMessage = "Failed to send KOT. Please retry."
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
        val items = order.guests.flatMap { it.items }
        val hasPending = items.any { it.status.equals("pending", ignoreCase = true) }
        if (hasPending || order.status.equals("active", ignoreCase = true)) {
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
