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
    val sectionId: String = "",
    val sectionName: String = "",
    val subsectionId: String? = null,
    val subsectionName: String? = null,
    val tables: List<TableItem> = emptyList(),
    val confirmDialogTable: TableItem? = null,
    val reserveDialogTable: TableItem? = null,
    val opsDialogTable: TableItem? = null,
    val freeActionsDialogTable: TableItem? = null,
    val pickTargetMode: String? = null, // "transfer" | "merge"
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
                if (!repository.isOnline()) continue
                if (_uiState.value.pickTargetMode == null &&
                    _uiState.value.opsDialogTable == null &&
                    _uiState.value.freeActionsDialogTable == null
                ) {
                    loadTables(silent = true)
                }
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
            var secId = _uiState.value.sectionId
            val subId = _uiState.value.subsectionId
            if (secId.isBlank()) {
                val secList = repository.sections.value
                if (secList.isNotEmpty()) {
                    secId = secList.first().id
                    _uiState.value = _uiState.value.copy(
                        sectionId = secId,
                        sectionName = secList.first().name
                    )
                }
            }
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
        _uiState.value = _uiState.value.copy(
            confirmDialogTable = table,
            opsDialogTable = null,
            freeActionsDialogTable = null
        )
    }

    fun showOpsDialog(table: TableItem) {
        _uiState.value = _uiState.value.copy(opsDialogTable = table, pickTargetMode = null)
    }

    fun showFreeActionsDialog(table: TableItem) {
        _uiState.value = _uiState.value.copy(freeActionsDialogTable = table)
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            confirmDialogTable = null,
            reserveDialogTable = null,
            opsDialogTable = null,
            freeActionsDialogTable = null,
            pickTargetMode = null
        )
    }

    fun showReserveDialog(table: TableItem) {
        _uiState.value = _uiState.value.copy(
            reserveDialogTable = table,
            freeActionsDialogTable = null
        )
    }

    fun startPickTarget(mode: String) {
        _uiState.value = _uiState.value.copy(pickTargetMode = mode, opsDialogTable = null)
    }

    fun onTablePickedAsTarget(target: TableItem) {
        val mode = _uiState.value.pickTargetMode ?: return
        val fromId = pendingSourceTableId ?: return
        if (target.id == fromId) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Pick a different table")
            return
        }
        when (mode) {
            "transfer" -> transferTable(fromId, target.id)
            "merge" -> mergeTables(fromId, target.id)
        }
    }

    private var pendingSourceTableId: String? = null

    fun beginTransfer(from: TableItem) {
        pendingSourceTableId = from.id
        _uiState.value = _uiState.value.copy(
            opsDialogTable = null,
            pickTargetMode = "transfer",
            snackbarMessage = "Select target table to transfer ${from.tableNumber}"
        )
    }

    fun beginMerge(from: TableItem) {
        pendingSourceTableId = from.id
        _uiState.value = _uiState.value.copy(
            opsDialogTable = null,
            pickTargetMode = "merge",
            snackbarMessage = "Select target table to merge ${from.tableNumber} into"
        )
    }

    fun cancelPickTarget() {
        pendingSourceTableId = null
        _uiState.value = _uiState.value.copy(pickTargetMode = null)
    }

    fun transferTable(fromTableId: String, toTableId: String) {
        viewModelScope.launch {
            val result = repository.transferTable(fromTableId, toTableId)
            result.onSuccess {
                pendingSourceTableId = null
                _uiState.value = _uiState.value.copy(
                    pickTargetMode = null,
                    snackbarMessage = it.message ?: "Table transferred"
                )
                loadTables(silent = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Transfer failed"
                )
            }
        }
    }

    fun mergeTables(fromTableId: String, toTableId: String) {
        viewModelScope.launch {
            val result = repository.mergeTables(fromTableId, toTableId)
            result.onSuccess {
                pendingSourceTableId = null
                _uiState.value = _uiState.value.copy(
                    pickTargetMode = null,
                    snackbarMessage = it.message ?: "Tables merged"
                )
                loadTables(silent = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Merge failed"
                )
            }
        }
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

    fun markAvailable(tableId: String) {
        viewModelScope.launch {
            val result = repository.markAvailable(tableId)
            result.onSuccess {
                dismissConfirmDialog()
                _uiState.value = _uiState.value.copy(snackbarMessage = "Table marked available")
                loadTables(silent = true)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Failed to mark available"
                )
            }
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
