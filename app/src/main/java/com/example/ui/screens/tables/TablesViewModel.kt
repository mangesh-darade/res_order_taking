package com.example.ui.screens.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TableItem
import com.example.data.repository.AuthRepository
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
    val allTables: List<TableItem> = emptyList(),
    val isFilterByAssigned: Boolean = true,
    val hasAssignedTables: Boolean = false,
    val assignedCount: Int = 0,
    val searchQuery: String = "",
    val statusFilter: String = "ALL", // "ALL", "AVAILABLE", "OCCUPIED", "RESERVED", "FREE"
    val availableSections: List<com.example.data.model.Section> = emptyList(),
    val availableSubsections: List<com.example.data.model.Subsection> = emptyList(),
    val confirmDialogTable: TableItem? = null,
    val reserveDialogTable: TableItem? = null,
    // Flag to open reservation dialog directly in edit mode when user taps Edit from bookings list
    val isEditReservationMode: Boolean = false,
    val opsDialogTable: TableItem? = null,
    val freeActionsDialogTable: TableItem? = null,
    val pickTargetMode: String? = null, // "transfer" | "merge"
    val reservationsDialogTable: TableItem? = null,
    val tableReservations: List<com.example.data.model.TableReservation> = emptyList(),
    val isLoadingReservations: Boolean = false,
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
        loadAvailableSectionsAndSubsections()
    }

    private fun getAssignedTableIds(): Set<String> {
        val user = AuthRepository.getInstance().currentUser.value
        val assigned = user?.tableAssign
        if (assigned.isNullOrBlank()) return emptySet()
        return assigned.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun isTableAssigned(table: TableItem, assignedIds: Set<String>): Boolean {
        if (assignedIds.isEmpty()) return true
        return assignedIds.contains(table.id) ||
               assignedIds.contains(table.tableNumber) ||
               assignedIds.any { id -> table.tableNumber.equals("T$id", ignoreCase = true) || table.tableNumber.equals(id, ignoreCase = true) }
    }

    private fun computeFilteredTables(
        sourceList: List<TableItem>,
        query: String = _uiState.value.searchQuery,
        statusFilter: String = _uiState.value.statusFilter,
        filterAssigned: Boolean = _uiState.value.isFilterByAssigned
    ): List<TableItem> {
        val assignedIds = getAssignedTableIds()
        var result = if (filterAssigned && assignedIds.isNotEmpty()) {
            sourceList.filter { isTableAssigned(it, assignedIds) }
        } else {
            sourceList
        }

        if (statusFilter != "ALL") {
            result = result.filter { table ->
                val st = table.status.lowercase()
                when (statusFilter) {
                    "AVAILABLE" -> st == "available"
                    "OCCUPIED" -> st in listOf("occupied", "order-placed", "placed", "kitchen", "kot_sent", "ready", "served")
                    "RESERVED" -> st == "reserved"
                    "FREE" -> st in listOf("free", "billed")
                    else -> true
                }
            }
        }

        val q = query.trim().lowercase()
        if (q.isNotEmpty()) {
            result = result.filter { table ->
                table.tableNumber.lowercase().contains(q) ||
                table.reservedBy?.lowercase()?.contains(q) == true ||
                table.status.lowercase().contains(q) ||
                table.statusLabel?.lowercase()?.contains(q) == true
            }
        }

        return result
    }

    fun toggleAssignedFilter() {
        val nextFilter = !_uiState.value.isFilterByAssigned
        val displayed = computeFilteredTables(
            sourceList = _uiState.value.allTables,
            filterAssigned = nextFilter
        )
        _uiState.value = _uiState.value.copy(
            isFilterByAssigned = nextFilter,
            tables = displayed
        )
    }

    fun onSearchQueryChanged(query: String) {
        val displayed = computeFilteredTables(
            sourceList = _uiState.value.allTables,
            query = query
        )
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            tables = displayed
        )
    }

    fun onStatusFilterChanged(status: String) {
        val displayed = computeFilteredTables(
            sourceList = _uiState.value.allTables,
            statusFilter = status
        )
        _uiState.value = _uiState.value.copy(
            statusFilter = status,
            tables = displayed
        )
    }

    fun resetAllFilters() {
        val displayed = computeFilteredTables(
            sourceList = _uiState.value.allTables,
            query = "",
            statusFilter = "ALL"
        )
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            statusFilter = "ALL",
            tables = displayed
        )
    }

    fun loadAvailableSectionsAndSubsections() {
        viewModelScope.launch {
            val secRes = repository.fetchSections()
            secRes.onSuccess { sections ->
                var currentSecId = _uiState.value.sectionId
                var currentSecName = _uiState.value.sectionName
                if (sections.size == 1 && currentSecId.isBlank()) {
                    currentSecId = sections.first().id
                    currentSecName = sections.first().name
                }
                _uiState.value = _uiState.value.copy(
                    availableSections = sections,
                    sectionId = currentSecId,
                    sectionName = currentSecName
                )
                if (currentSecId.isNotBlank()) {
                    val subRes = repository.fetchSubsections(currentSecId)
                    subRes.onSuccess { subs ->
                        var subId = _uiState.value.subsectionId
                        var subName = _uiState.value.subsectionName
                        if (subs.size == 1) {
                            subId = subs.first().id
                            subName = subs.first().name
                        } else if (subs.isEmpty()) {
                            subId = null
                            subName = null
                        }
                        _uiState.value = _uiState.value.copy(
                            availableSubsections = subs,
                            subsectionId = subId,
                            subsectionName = subName
                        )
                    }
                }
            }
        }
    }

    fun selectSectionFilter(sec: com.example.data.model.Section?) {
        val secId = sec?.id.orEmpty()
        val secName = sec?.name.orEmpty()
        _uiState.value = _uiState.value.copy(
            sectionId = secId,
            sectionName = secName,
            subsectionId = null,
            subsectionName = null,
            availableSubsections = emptyList()
        )
        if (secId.isNotBlank()) {
            viewModelScope.launch {
                val subRes = repository.fetchSubsections(secId)
                subRes.onSuccess { subs ->
                    val autoSub = if (subs.size == 1) subs.first() else null
                    _uiState.value = _uiState.value.copy(
                        availableSubsections = subs,
                        subsectionId = autoSub?.id,
                        subsectionName = autoSub?.name
                    )
                    loadTables(silent = false)
                }
            }
        } else {
            loadTables(silent = false)
        }
    }

    fun selectSubsectionFilter(sub: com.example.data.model.Subsection?) {
        _uiState.value = _uiState.value.copy(
            subsectionId = sub?.id,
            subsectionName = sub?.name
        )
        loadTables(silent = false)
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
        loadAvailableSectionsAndSubsections()
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
                val assignedIds = getAssignedTableIds()
                val hasAssigned = assignedIds.isNotEmpty()
                val displayed = computeFilteredTables(sourceList = list)
                val countAssigned = list.count { isTableAssigned(it, assignedIds) }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allTables = list,
                    tables = displayed,
                    hasAssignedTables = hasAssigned,
                    assignedCount = countAssigned
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
            isEditReservationMode = false,
            reservationsDialogTable = null,
            opsDialogTable = null,
            freeActionsDialogTable = null,
            pickTargetMode = null
        )
    }

    // Opens reserve table dialog, optionally pre-entering edit mode for editing an existing reservation
    fun showReserveDialog(table: TableItem, startInEditMode: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            reserveDialogTable = table,
            isEditReservationMode = startInEditMode,
            reservationsDialogTable = null,
            freeActionsDialogTable = null
        )
    }

    fun showReservationsDialog(table: TableItem) {
        _uiState.value = _uiState.value.copy(
            reservationsDialogTable = table,
            reserveDialogTable = null,
            isLoadingReservations = true,
            tableReservations = emptyList()
        )
        loadTableReservations(table.id)
    }

    fun dismissReservationsDialog() {
        _uiState.value = _uiState.value.copy(
            reservationsDialogTable = null,
            tableReservations = emptyList(),
            isLoadingReservations = false
        )
    }

    fun loadTableReservations(tableId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingReservations = true)
            val result = repository.getTableReservations(tableId = tableId)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    tableReservations = list,
                    isLoadingReservations = false
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoadingReservations = false,
                    snackbarMessage = err.message ?: "Failed to load reservations"
                )
            }
        }
    }

    fun cancelReservation(reservationId: String, tableId: String) {
        viewModelScope.launch {
            val result = repository.cancelReservation(reservationId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Reservation cancelled successfully")
                loadTableReservations(tableId)
                loadTables(silent = true)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = err.message ?: "Failed to cancel reservation"
                )
            }
        }
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
        reservedFrom: String? = null,
        reservedUntil: String,
        reservedNote: String? = null,
        customerPhone: String? = null,
        guestCount: Int? = 1,
        reservationId: String? = null,
        updateExisting: Boolean = false
    ) {
        viewModelScope.launch {
            val result = repository.reserveTable(
                tableId = tableId,
                reservedBy = customerName,
                reservedFrom = reservedFrom,
                reservedUntil = reservedUntil,
                reservedNote = reservedNote,
                customerPhone = customerPhone,
                guestCount = guestCount,
                reservationId = reservationId,
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
