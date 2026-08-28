package com.example.ui.screens.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TableItem
import com.example.data.repository.RestaurantRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TablesUiState(
    val isLoading: Boolean = false,
    val sectionId: String = "1",
    val sectionName: String = "Main Dining",
    val subsectionId: String? = "101",
    val subsectionName: String? = "Hall A",
    val tables: List<TableItem> = emptyList(),
    val confirmDialogTable: TableItem? = null,
    val reserveDialogTable: TableItem? = null,
    val snackbarMessage: String? = null
)

class TablesViewModel(
    private val repository: RestaurantRepository = RestaurantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(3000)
                loadTables(silent = true)
            }
        }
    }

    fun setSectionContext(secId: String, secName: String, subId: String?, subName: String?) {
        _uiState.value = _uiState.value.copy(
            sectionId = secId,
            sectionName = secName,
            subsectionId = subId,
            subsectionName = subName
        )
        loadTables(silent = false)
    }

    fun loadTables(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent && _uiState.value.tables.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            val secId = _uiState.value.sectionId
            val subId = _uiState.value.subsectionId
            val result = repository.fetchTables(secId, subId)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tables = list
                )
            }.onFailure {
                if (!silent) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun showFreeConfirmDialog(table: TableItem) {
        _uiState.value = _uiState.value.copy(confirmDialogTable = table)
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(confirmDialogTable = null, reserveDialogTable = null)
    }

    fun showReserveDialog(table: TableItem) {
        _uiState.value = _uiState.value.copy(reserveDialogTable = table)
    }

    fun reserveTable(
        tableId: String,
        customerName: String,
        reservedUntil: String,
        reservedNote: String? = null,
        updateExisting: Boolean = false
    ) {
        viewModelScope.launch {
            val result = repository.reserveTable(
                tableId = tableId,
                reservedBy = customerName,
                reservedUntil = reservedUntil,
                reservedNote = reservedNote,
                updateExisting = updateExisting
            )
            result.onSuccess {
                dismissConfirmDialog()
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = if (updateExisting) {
                        "Reservation updated successfully"
                    } else {
                        "Table reserved successfully"
                    }
                )
                loadTables(silent = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Failed to reserve table"
                )
            }
        }
    }

    fun unreserveTable(tableId: String) {
        viewModelScope.launch {
            val result = repository.unreserveTable(tableId)
            result.onSuccess {
                dismissConfirmDialog()
                _uiState.value = _uiState.value.copy(snackbarMessage = "Table reservation cancelled")
                loadTables(silent = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Failed to cancel reservation"
                )
            }
        }
    }

    fun freeTable(tableId: String) {
        viewModelScope.launch {
            repository.freeTable(tableId)
            dismissConfirmDialog()
            _uiState.value = _uiState.value.copy(snackbarMessage = "Table freed successfully")
            loadTables(silent = true)
        }
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
