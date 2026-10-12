package com.example.ui.screens.tables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TableItem
import com.example.data.model.TableReservation
import com.example.ui.components.MainTab
import com.example.ui.components.SharedTabStrip
import com.example.ui.components.TableCard
import com.example.ui.components.TopHeaderBar
import com.example.ui.theme.BackgroundGray
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TablesScreen(
    sectionId: String,
    sectionName: String,
    subsectionId: String?,
    subsectionName: String?,
    onNavigateToOrders: (tableId: String) -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onLogoutClick: (() -> Unit)? = null,
    viewModel: TablesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sectionId, sectionName, subsectionId, subsectionName) {
        viewModel.setSectionContext(sectionId, sectionName, subsectionId, subsectionName)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        topBar = { TopHeaderBar(onLogoutClick = onLogoutClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundGray
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SharedTabStrip(
                selectedTab = MainTab.TABLES,
                onTabSelected = onTabSelected
            )

            // Search Bar & Filter Controls Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search table, status, reserved by...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = PinkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = PinkPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("tables_search_bar")
                    )

                    // Section & Subsection Dropdowns Row (only shown if more than 1 option exists)
                    val showSectionDropdown = uiState.availableSections.size > 1
                    val showSubsectionDropdown = uiState.availableSubsections.size > 1

                    if (showSectionDropdown || showSubsectionDropdown) {
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Section Selector (only when multiple sections exist)
                            if (showSectionDropdown) {
                                var sectionMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .clickable { sectionMenuExpanded = true },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (uiState.sectionName.isNotBlank()) uiState.sectionName else "All Sections",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextDark,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = TextDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = sectionMenuExpanded,
                                        onDismissRequest = { sectionMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Sections", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                sectionMenuExpanded = false
                                                viewModel.selectSectionFilter(null)
                                            }
                                        )
                                        uiState.availableSections.forEach { sec ->
                                            DropdownMenuItem(
                                                text = { Text(sec.name) },
                                                onClick = {
                                                    sectionMenuExpanded = false
                                                    viewModel.selectSectionFilter(sec)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Subsection Selector (only when multiple subsections exist for this section)
                            if (showSubsectionDropdown) {
                                var subsectionMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .clickable { subsectionMenuExpanded = true },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (!uiState.subsectionName.isNullOrBlank()) uiState.subsectionName!! else "All Subsections",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextDark,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = TextDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = subsectionMenuExpanded,
                                        onDismissRequest = { subsectionMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Subsections", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                subsectionMenuExpanded = false
                                                viewModel.selectSubsectionFilter(null)
                                            }
                                        )
                                        uiState.availableSubsections.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub.name) },
                                                onClick = {
                                                    subsectionMenuExpanded = false
                                                    viewModel.selectSubsectionFilter(sub)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status Filter Chips Row (Matching TableCard Status Colors)
                    val allCount = uiState.allTables.size
                    val availCount = uiState.allTables.count { it.status.lowercase() == "available" }
                    val occCount = uiState.allTables.count { it.status.lowercase() in listOf("occupied", "order-placed", "placed", "kitchen", "kot_sent", "ready", "served") }
                    val resCount = uiState.allTables.count { it.status.lowercase() == "reserved" }
                    val freeCount = uiState.allTables.count { it.status.lowercase() in listOf("free", "billed") }

                    data class StatusFilterOption(
                        val key: String,
                        val label: String,
                        val bgColor: Color,
                        val textColor: Color,
                        val borderColor: Color,
                        val selectedBorderColor: Color
                    )

                    val filterOptions = listOf(
                        StatusFilterOption(
                            key = "ALL",
                            label = "All ($allCount)",
                            bgColor = Color(0xFFF1F5F9),
                            textColor = Color(0xFF334155),
                            borderColor = Color(0xFFCBD5E1),
                            selectedBorderColor = Color(0xFF475569)
                        ),
                        StatusFilterOption(
                            key = "AVAILABLE",
                            label = "Available ($availCount)",
                            bgColor = Color(0xFFFFFFFF),
                            textColor = PinkPrimary,
                            borderColor = PinkPrimary.copy(alpha = 0.45f),
                            selectedBorderColor = PinkPrimary
                        ),
                        StatusFilterOption(
                            key = "OCCUPIED",
                            label = "Occupied ($occCount)",
                            bgColor = Color(0xFFA2E5FF),
                            textColor = Color(0xFF0284C7),
                            borderColor = Color(0xFF7DD3FC),
                            selectedBorderColor = Color(0xFF0284C7)
                        ),
                        StatusFilterOption(
                            key = "RESERVED",
                            label = "Reserved ($resCount)",
                            bgColor = Color(0xFFFFFF99),
                            textColor = Color(0xFF856404),
                            borderColor = Color(0xFFFDE047),
                            selectedBorderColor = Color(0xFFB45309)
                        ),
                        StatusFilterOption(
                            key = "FREE",
                            label = "Billed ($freeCount)",
                            bgColor = Color(0xFFFFCDD2),
                            textColor = Color(0xFFB71C1C),
                            borderColor = Color(0xFFFCA5A5),
                            selectedBorderColor = Color(0xFFB71C1C)
                        )
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filterOptions) { opt ->
                            val isSelected = uiState.statusFilter == opt.key
                            Surface(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clickable { viewModel.onStatusFilterChanged(opt.key) },
                                shape = RoundedCornerShape(14.dp),
                                color = opt.bgColor,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) opt.selectedBorderColor else opt.borderColor
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = opt.textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.hasAssignedTables) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF7ED),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color(0xFFC2410C),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (uiState.isFilterByAssigned) "Assigned Tables (${uiState.assignedCount})" else "All Tables (${uiState.allTables.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                        }

                        AssistChip(
                            onClick = { viewModel.toggleAssignedFilter() },
                            label = {
                                Text(
                                    text = if (uiState.isFilterByAssigned) "SHOW ALL" else "MY TABLES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (uiState.isFilterByAssigned) Color.White else Color(0xFFEA580C),
                                labelColor = if (uiState.isFilterByAssigned) Color(0xFFEA580C) else Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA580C)),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Non-blocking pick banner — tables stay tappable (do NOT use AlertDialog here)
            if (uiState.pickTargetMode != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (uiState.pickTargetMode == "merge") Color(0xFFE0F2FE) else Color(0xFFFCE7F3),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.pickTargetMode == "merge") {
                                    "Pick merge target"
                                } else {
                                    "Pick transfer target"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Text(
                                text = if (uiState.pickTargetMode == "merge") {
                                    "Tap another table to merge into."
                                } else {
                                    "Tap an empty/available table to move the order."
                                },
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        TextButton(onClick = { viewModel.cancelPickTarget() }) {
                            Text("Cancel", color = PinkPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PinkPrimary)
                    }
                } else if (uiState.tables.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            val isFilterActive = uiState.searchQuery.isNotBlank() || uiState.statusFilter != "ALL"
                            Text(
                                text = if (isFilterActive) "No Matching Tables" else if (uiState.hasAssignedTables && uiState.isFilterByAssigned) "No Assigned Tables Here" else "No Tables Found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isFilterActive) {
                                    "No tables match the current search or status filter."
                                } else if (uiState.hasAssignedTables && uiState.isFilterByAssigned) {
                                    "You have no assigned tables in this section.\nTap below to view all tables."
                                } else {
                                    "No tables found in this section.\nPlease configure sections and tables in ElintOm POS."
                                },
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (isFilterActive) {
                                Button(
                                    onClick = { viewModel.resetAllFilters() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reset Filters")
                                }
                            } else if (uiState.hasAssignedTables && uiState.isFilterByAssigned) {
                                Button(
                                    onClick = { viewModel.toggleAssignedFilter() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("SHOW ALL TABLES")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.loadTables(silent = false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Refresh Tables")
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tables_grid")
                    ) {
                        items(uiState.tables) { table ->
                            TableCard(
                                table = table,
                                onClick = {
                                    when {
                                        uiState.pickTargetMode != null -> viewModel.onTablePickedAsTarget(table)
                                        table.status.lowercase() == "reserved" -> viewModel.showReserveDialog(table)
                                        else -> onNavigateToOrders(table.id)
                                    }
                                },
                                onLongClick = {
                                    // While picking target, ignore long-press
                                    if (uiState.pickTargetMode == null) {
                                        when (table.status.lowercase()) {
                                            "free" -> viewModel.showFreeActionsDialog(table)
                                            "available" -> viewModel.showReserveDialog(table)
                                            "reserved" -> viewModel.showReserveDialog(table)
                                            else -> viewModel.showOpsDialog(table)
                                        }
                                    }
                                },
                                onFreeClick = { viewModel.showFreeConfirmDialog(table) },
                                onViewReservations = { viewModel.showReservationsDialog(table) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.confirmDialogTable != null) {
        val table = uiState.confirmDialogTable!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = { Text(text = "Free Table ${table.tableNumber}?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to mark table ${table.tableNumber} as Free (red)? Open orders will be closed.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.freeTable(table.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissConfirmDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.opsDialogTable != null) {
        val table = uiState.opsDialogTable!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = { Text(text = "Table ${table.tableNumber}", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Transfer moves the order. Merge combines bills into another table.") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.beginTransfer(table) },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Transfer") }
                    Button(
                        onClick = { viewModel.beginMerge(table) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Merge into…") }
                    OutlinedButton(
                        onClick = {
                            viewModel.dismissConfirmDialog()
                            viewModel.showFreeConfirmDialog(table)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Free table") }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
                    Text("Close")
                }
            }
        )
    }

    if (uiState.freeActionsDialogTable != null) {
        // Free (red) after cleaning → Mark Available (white) or Reserve
        val table = uiState.freeActionsDialogTable!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = { Text(text = "Table ${table.tableNumber} (Free)", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Cleaning done? Mark Available to seat new guests.") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.markAvailable(table.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Mark Available") }
                    OutlinedButton(
                        onClick = {
                            viewModel.dismissConfirmDialog()
                            viewModel.showReserveDialog(table)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reserve") }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
                    Text("Close")
                }
            }
        )
    }

    // Pick mode uses top banner only — no blocking dialog (tables must stay tappable)

    if (uiState.reservationsDialogTable != null) {
        TableReservationsDialog(
            table = uiState.reservationsDialogTable!!,
            reservations = uiState.tableReservations,
            isLoading = uiState.isLoadingReservations,
            onDismiss = { viewModel.dismissReservationsDialog() },
            onBookNewSlot = {
                val table = uiState.reservationsDialogTable!!
                viewModel.showReserveDialog(table)
            },
            onCancelReservation = { resId ->
                val tableId = uiState.reservationsDialogTable!!.id
                viewModel.cancelReservation(resId, tableId)
            }
        )
    }

    if (uiState.reserveDialogTable != null) {
        ReserveTableDialog(
            table = uiState.reserveDialogTable!!,
            onDismiss = { viewModel.dismissConfirmDialog() },
            onReserve = { by, from, until, note, phone, guests, isEdit ->
                viewModel.reserveTable(
                    tableId = uiState.reserveDialogTable!!.id,
                    customerName = by,
                    reservedFrom = from,
                    reservedUntil = until,
                    reservedNote = note,
                    customerPhone = phone,
                    guestCount = guests,
                    updateExisting = isEdit
                )
            },
            onUnreserve = { viewModel.unreserveTable(uiState.reserveDialogTable!!.id) },
            onStartOrder = {
                val id = uiState.reserveDialogTable!!.id
                viewModel.dismissConfirmDialog()
                onNavigateToOrders(id)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReserveTableDialog(
    table: TableItem,
    onDismiss: () -> Unit,
    onReserve: (reservedBy: String, reservedFrom: String, reservedUntil: String, reservedNote: String?, phone: String?, guests: Int, isEdit: Boolean) -> Unit,
    onUnreserve: () -> Unit,
    onStartOrder: () -> Unit
) {
    val isReserved = table.status.lowercase() == "reserved"
    var editMode by remember(table.id) { mutableStateOf(!isReserved) }
    var isBookingNewSlot by remember(table.id) { mutableStateOf(false) }

    var reservedBy by remember(table.id) {
        mutableStateOf(if (isReserved) table.reservedBy.orEmpty() else "")
    }
    var customerPhone by remember(table.id) { mutableStateOf("") }
    var guestCountStr by remember(table.id) {
        mutableStateOf((table.guestsCount?.takeIf { it > 0 } ?: 2).toString())
    }
    var reservedFromMillis by remember(table.id) {
        mutableStateOf(System.currentTimeMillis())
    }
    var reservedUntilMillis by remember(table.id) {
        mutableStateOf(parseReservedUntilMillis(table.reservedUntil) ?: defaultReservedUntilMillis())
    }
    var reservedNote by remember(table.id) {
        mutableStateOf(if (isReserved) table.reservedNote.orEmpty() else "")
    }
    var formError by remember(table.id) { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickingForFrom by remember { mutableStateOf(false) }

    val fromDisplay = formatMillisForField(reservedFromMillis)
    val untilDisplay = formatMillisForField(reservedUntilMillis)

    val activeMillis = if (pickingForFrom) reservedFromMillis else reservedUntilMillis
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startOfDayUtcMillis(activeMillis),
        yearRange = IntRange(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.YEAR) + 2
        )
    )
    val initialCal = remember(activeMillis) {
        Calendar.getInstance().apply { timeInMillis = activeMillis }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCal.get(Calendar.MINUTE),
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Table ${table.tableNumber}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (isReserved) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (isBookingNewSlot) "ADVANCE" else "RESERVED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isReserved && !editMode) {
                    // Active Reservation Detail Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = PinkPrimary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = PinkPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Customer Name",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = table.reservedBy?.ifBlank { "Guest" } ?: "Guest",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${table.guestsCount?.takeIf { it > 0 } ?: 1} Guests",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Reserved Until",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = formatReservedUntilDisplay(table.reservedUntil) ?: "—",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            if (!table.reservedNote.isNullOrBlank()) {
                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = table.reservedNote,
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons in View Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartOrder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Order / Seat Guest",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                isBookingNewSlot = true
                                editMode = true
                                reservedBy = ""
                                customerPhone = ""
                                guestCountStr = (table.guestsCount?.takeIf { it > 0 } ?: 2).toString()
                                val nextStart = parseReservedUntilMillis(table.reservedUntil) ?: System.currentTimeMillis()
                                reservedFromMillis = nextStart
                                reservedUntilMillis = nextStart + TimeUnit.HOURS.toMillis(2)
                                reservedNote = ""
                                formError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFDCFCE7),
                                contentColor = Color(0xFF15803D)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Book Next Slot (Advance Booking)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editMode = true
                                    isBookingNewSlot = false
                                    reservedBy = table.reservedBy.orEmpty()
                                    reservedUntilMillis = parseReservedUntilMillis(table.reservedUntil) ?: defaultReservedUntilMillis()
                                    reservedNote = table.reservedNote.orEmpty()
                                    formError = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = onUnreserve,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cancel Booking",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    // Form Mode
                    if (isReserved) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isBookingNewSlot = false
                                        reservedBy = table.reservedBy.orEmpty()
                                        reservedUntilMillis = parseReservedUntilMillis(table.reservedUntil) ?: defaultReservedUntilMillis()
                                        reservedNote = table.reservedNote.orEmpty()
                                        formError = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isBookingNewSlot) Color.White else Color.Transparent,
                                        contentColor = if (!isBookingNewSlot) Color(0xFF0F172A) else Color(0xFF64748B)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (!isBookingNewSlot) 1.dp else 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("Edit Current", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        isBookingNewSlot = true
                                        reservedBy = ""
                                        customerPhone = ""
                                        guestCountStr = (table.guestsCount?.takeIf { it > 0 } ?: 2).toString()
                                        val nextStart = parseReservedUntilMillis(table.reservedUntil) ?: System.currentTimeMillis()
                                        reservedFromMillis = nextStart
                                        reservedUntilMillis = nextStart + TimeUnit.HOURS.toMillis(2)
                                        reservedNote = ""
                                        formError = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBookingNewSlot) Color(0xFF16A34A) else Color.Transparent,
                                        contentColor = if (isBookingNewSlot) Color.White else Color(0xFF64748B)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isBookingNewSlot) 1.dp else 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("+ Next Customer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reservedBy,
                        onValueChange = {
                            reservedBy = it
                            formError = null
                        },
                        label = { Text(if (isBookingNewSlot) "Next Customer Name *" else "Customer Name *") },
                        placeholder = { Text(if (isBookingNewSlot) "e.g. Ramesh Patil" else "e.g. Customer Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reserve_by_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Phone") },
                            placeholder = { Text("Mobile No.") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.1f)
                        )
                        OutlinedTextField(
                            value = guestCountStr,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) guestCountStr = it },
                            label = { Text("Guests") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    OutlinedTextField(
                        value = fromDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From (Date & Time) *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    pickingForFrom = true
                                    showDatePicker = true
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Date", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    pickingForFrom = true
                                    showTimePicker = true
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickingForFrom = true
                                showDatePicker = true
                            }
                    )

                    OutlinedTextField(
                        value = untilDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Until (Date & Time) *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    pickingForFrom = false
                                    showDatePicker = true
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Date", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    pickingForFrom = false
                                    showTimePicker = true
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickingForFrom = false
                                showDatePicker = true
                            }
                            .testTag("reserve_until_field")
                    )

                    OutlinedTextField(
                        value = reservedNote,
                        onValueChange = { reservedNote = it },
                        label = { Text("Note (optional)") },
                        placeholder = { Text("e.g. Birthday celebration, window seat") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    if (formError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formError!!,
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val by = reservedBy.trim()
                                val normalizedFrom = formatMillisForApi(reservedFromMillis)
                                val normalizedUntil = formatMillisForApi(reservedUntilMillis)
                                val guests = guestCountStr.toIntOrNull() ?: 1
                                val isEditAction = isReserved && editMode && !isBookingNewSlot
                                when {
                                    by.isEmpty() -> formError = "Customer Name is required"
                                    reservedUntilMillis <= reservedFromMillis ->
                                        formError = "Reserved Until must be after From time"
                                    else -> {
                                        formError = null
                                        onReserve(
                                            by,
                                            normalizedFrom,
                                            normalizedUntil,
                                            reservedNote.trim().ifEmpty { null },
                                            customerPhone.trim().ifEmpty { null },
                                            guests,
                                            isEditAction
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBookingNewSlot) Color(0xFF16A34A) else Color(0xFFD97706)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("reserve_submit_button")
                        ) {
                            Text(
                                text = when {
                                    isBookingNewSlot -> "Confirm Advance Booking"
                                    isReserved && editMode -> "Save Changes"
                                    else -> "Reserve Table"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (isReserved) {
                                    editMode = false
                                    isBookingNewSlot = false
                                    formError = null
                                } else {
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Text(if (isReserved) "Back" else "Cancel", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        if (pickingForFrom) {
                            reservedFromMillis = mergeDateKeepingTime(selected, reservedFromMillis)
                        } else {
                            reservedUntilMillis = mergeDateKeepingTime(selected, reservedUntilMillis)
                        }
                        formError = null
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (pickingForFrom) "Select start time" else "Select end time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pickingForFrom) {
                        reservedFromMillis = mergeTimeKeepingDate(
                            reservedFromMillis,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    } else {
                        reservedUntilMillis = mergeTimeKeepingDate(
                            reservedUntilMillis,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    }
                    formError = null
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

private fun defaultReservedUntilMillis(): Long =
    System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2)

private fun parseReservedUntilMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    return try {
        val cleaned = raw.trim().replace('T', ' ').let {
            if (Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$""").matches(it)) "$it:00" else it
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.isLenient = false
        sdf.parse(cleaned)?.time
    } catch (_: Exception) {
        null
    }
}

private fun formatMillisForField(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    return sdf.format(java.util.Date(millis))
}

private fun formatMillisForApi(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return sdf.format(java.util.Date(millis))
}

/** DatePicker returns UTC midnight millis — keep local time-of-day from currentUntil. */
private fun mergeDateKeepingTime(selectedUtcMidnight: Long, currentUntil: Long): Long {
    val timeCal = Calendar.getInstance().apply { timeInMillis = currentUntil }
    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedUtcMidnight
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfDayUtcMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(Calendar.YEAR, local.get(Calendar.YEAR))
    utc.set(Calendar.MONTH, local.get(Calendar.MONTH))
    utc.set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
    return utc.timeInMillis
}

private fun mergeTimeKeepingDate(currentUntil: Long, hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = currentUntil
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatReservedUntilDisplay(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val outFmt = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
        val parsed = inFmt.parse(raw.trim().replace('T', ' ').let {
            if (Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$""").matches(it)) "$it:00" else it
        })
        if (parsed != null) outFmt.format(parsed) else raw
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun TableReservationsDialog(
    table: TableItem,
    reservations: List<TableReservation>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onBookNewSlot: () -> Unit,
    onCancelReservation: (reservationId: String) -> Unit
) {
    var reservationToCancel by remember { mutableStateOf<TableReservation?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Table ${table.tableNumber} Bookings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PinkPrimary)
                }
            } else if (reservations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No upcoming reservations",
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "This table has no active or future reservations.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reservations) { res ->
                        val isSeated = res.status == 2
                        val statusBg = if (isSeated) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                        val statusText = if (isSeated) Color(0xFF16A34A) else Color(0xFFD97706)
                        val statusLabel = if (isSeated) "SEATED" else "CONFIRMED"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (!isSeated) {
                                        TextButton(
                                            onClick = { reservationToCancel = res },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formatReservationSlot(res.reservedFrom, res.reservedTo),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = res.customerName.ifBlank { "Guest" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark
                                        )
                                    }
                                    Text(
                                        text = "${res.guestCount ?: 1} Guests",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                }

                                if (!res.customerPhone.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = res.customerPhone,
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                if (!res.notes.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = res.notes,
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onBookNewSlot,
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Book Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (reservationToCancel != null) {
        val target = reservationToCancel!!
        AlertDialog(
            onDismissRequest = { reservationToCancel = null },
            title = { Text("Cancel Reservation?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to cancel the reservation for ${target.customerName} on ${formatReservationSlot(target.reservedFrom, target.reservedTo)}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        reservationToCancel = null
                        onCancelReservation(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToCancel = null }) {
                    Text("No, Keep")
                }
            }
        )
    }
}

private fun formatReservationSlot(fromRaw: String, toRaw: String): String {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val outDateFmt = java.text.SimpleDateFormat("d MMM, hh:mm a", Locale.US)
        val outTimeFmt = java.text.SimpleDateFormat("hh:mm a", Locale.US)

        val fClean = fromRaw.trim().replace('T', ' ').let { if (it.length == 16) "$it:00" else it }
        val tClean = toRaw.trim().replace('T', ' ').let { if (it.length == 16) "$it:00" else it }

        val fDate = parser.parse(fClean)
        val tDate = parser.parse(tClean)

        if (fDate != null && tDate != null) {
            "${outDateFmt.format(fDate)} - ${outTimeFmt.format(tDate)}"
        } else {
            "$fromRaw - $toRaw"
        }
    } catch (_: Exception) {
        "$fromRaw - $toRaw"
    }
}

