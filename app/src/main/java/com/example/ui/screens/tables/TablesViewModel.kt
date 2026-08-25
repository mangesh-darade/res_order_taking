package com.example.ui.screens.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TableItem
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TablesUiState(
    val isLoading: Boolean = false,
    val sectionId: String = "1",
    val sectionName: String = "Main Dining",
    val subsectionId: String? = "101",
    val subsectionName: String? = "Hall A",
    val tables: List<TableItem> = emptyList(),
    val confirmDialogTable: TableItem? = null,
    val snackbarMessage: String? = null
)

class TablesViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    fun setSectionContext(secId: String, secName: String, subId: String?, subName: String?) {
        _uiState.value = _uiState.value.copy(
            sectionId = secId,
            sectionName = secName,
            subsectionId = subId,
            subsectionName = subName
        )
        loadTables()
    }

    fun loadTables() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val secId = _uiState.value.sectionId
            val subId = _uiState.value.subsectionId
            val result = repository.fetchTables(secId, subId)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tables = list
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun showFreeConfirmDialog(table: TableItem) {
        _uiState.value = _uiState.value.copy(confirmDialogTable = table)
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(confirmDialogTable = null)
    }

    fun freeTable(tableId: String) {
        viewModelScope.launch {
            repository.freeTable(tableId)
            dismissConfirmDialog()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Table freed successfully")
            loadTables()
        }
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
