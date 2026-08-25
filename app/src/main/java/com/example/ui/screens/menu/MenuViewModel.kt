package com.example.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.ProductCustomization
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MenuUiState(
    val isLoading: Boolean = false,
    val tableId: String = "1",
    val guestId: Int = 1,
    val orderId: String = "",
    val searchQuery: String = "",
    val selectedMealType: String = "all", // "all", "veg", "non-veg"
    val selectedCategory: String? = null,
    val categories: List<MenuCategory> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val customDialogItem: MenuItem? = null,
    val customizationData: ProductCustomization? = null,
    val snackbarMessage: String? = null
)

class MenuViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    fun initialize(tableId: String, guestId: Int) {
        _uiState.value = _uiState.value.copy(tableId = tableId, guestId = guestId)
        viewModelScope.launch {
            repository.fetchBranding()
        }
        loadCategories()
        loadOrderContext(tableId)
    }

    private fun loadOrderContext(tableId: String) {
        viewModelScope.launch {
            val result = repository.fetchOrderBootstrap(tableId = tableId)
            result.onSuccess { order ->
                _uiState.value = _uiState.value.copy(orderId = order.orderId ?: "")
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val result = repository.fetchMenuCategories()
            result.onSuccess { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
                loadMenuItems()
            }
        }
    }

    fun loadMenuItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.fetchMenuItems(
                categoryId = _uiState.value.selectedCategory,
                mealType = _uiState.value.selectedMealType,
                search = _uiState.value.searchQuery
            )
            result.onSuccess { items ->
                _uiState.value = _uiState.value.copy(isLoading = false, menuItems = items)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadMenuItems()
    }

    fun setMealType(mealType: String) {
        _uiState.value = _uiState.value.copy(selectedMealType = mealType)
        loadMenuItems()
    }

    fun selectCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = categoryId)
        loadMenuItems()
    }

    fun openCustomizationSheet(item: MenuItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(customDialogItem = item, isLoading = true)
            val result = repository.fetchProductCustomizations(item.id)
            result.onSuccess { custom ->
                _uiState.value = _uiState.value.copy(isLoading = false, customizationData = custom)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun closeCustomizationSheet() {
        _uiState.value = _uiState.value.copy(customDialogItem = null, customizationData = null)
    }

    fun addItemToOrder(
        item: MenuItem,
        qty: Int,
        spiceLevel: String?,
        meatWellness: String?,
        allergies: List<String>,
        addOns: List<String>,
        toppings: List<String>,
        noOnion: Boolean,
        noGarlic: Boolean,
        specialInstructions: String?
    ) {
        viewModelScope.launch {
            val orderId = _uiState.value.orderId.ifEmpty { "ORD-${_uiState.value.tableId}" }
            val result = repository.addItemToOrder(
                orderId = orderId,
                guestId = _uiState.value.guestId,
                productId = item.id,
                quantity = qty,
                spiceLevel = spiceLevel,
                meatWellness = meatWellness,
                allergies = allergies,
                customAllergies = null,
                addOns = addOns,
                toppings = toppings,
                onionFlag = noOnion,
                garlicFlag = noGarlic,
                specialInstructions = specialInstructions
            )

            result.onSuccess { bootstrap ->
                closeCustomizationSheet()
                val msg = if (_uiState.value.guestId == 0) {
                    "Added ${item.name} x$qty for All Guests (Table)"
                } else {
                    "Added ${item.name} x$qty to Guest ${_uiState.value.guestId}"
                }
                _uiState.value = _uiState.value.copy(
                    orderId = bootstrap.orderId ?: _uiState.value.orderId,
                    snackbarMessage = msg
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = err.localizedMessage ?: "Failed to add item"
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
