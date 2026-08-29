package com.example.ui.screens.tables

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TableItem
import com.example.ui.components.MainTab
import com.example.ui.components.SharedTabStrip
import com.example.ui.components.TableCard
import com.example.ui.components.TopHeaderBar
import com.example.ui.theme.BackgroundGray
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
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

            val titleText = if (!subsectionName.isNullOrBlank()) {
                "$sectionName • $subsectionName"
            } else {
                "$sectionName • Ground Floor"
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleText.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.testTag("tables_section_title")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(0xFF22C55E)
                        ) {}
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF16A34A)
                        )
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
                                    // free → Mark Available / Reserve
                                    // available|reserved → reserve dialog
                                    // active (occupied+) → transfer / merge / free
                                    when (table.status.lowercase()) {
                                        "free" -> viewModel.showFreeActionsDialog(table)
                                        "available" -> viewModel.showReserveDialog(table)
                                        "reserved" -> viewModel.showReserveDialog(table)
                                        else -> viewModel.showOpsDialog(table)
                                    }
                                },
                                onFreeClick = { viewModel.showFreeConfirmDialog(table) }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statuses = uiState.tables.map { it.status.lowercase() }
                    val totalCount = uiState.tables.size
                    val availableCount = statuses.count { it == "available" }
                    val occupiedCount = statuses.count { it == "occupied" }
                    // Order Placed + Served share pink on cards — one legend chip
                    val pinkCount = statuses.count {
                        it in listOf("order-placed", "placed", "kitchen", "kot_sent", "served")
                    }
                    val readyCount = statuses.count { it == "ready" || it == "order-ready" }
                    val reservedCount = statuses.count { it == "reserved" }
                    val freeCount = statuses.count { it == "free" }

                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colors match TableCard: white / blue / pink / green / yellow / red
                        StatusLegendChip(
                            color = Color.White,
                            count = availableCount,
                            total = totalCount,
                            bordered = true
                        )
                        StatusLegendChip(
                            color = Color(0xFFA2E5FF),
                            count = occupiedCount,
                            total = totalCount
                        )
                        StatusLegendChip(
                            color = Color(0xFFFF7EB6),
                            count = pinkCount,
                            total = totalCount
                        )
                        StatusLegendChip(
                            color = Color(0xFFC8E6C9),
                            count = readyCount,
                            total = totalCount
                        )
                        StatusLegendChip(
                            color = Color(0xFFFFFF99),
                            count = reservedCount,
                            total = totalCount
                        )
                        StatusLegendChip(
                            color = Color(0xFFFFCDD2),
                            count = freeCount,
                            total = totalCount
                        )
                    }

                    Button(
                        onClick = { viewModel.loadTables() },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "REFRESH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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

    if (uiState.pickTargetMode != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelPickTarget() },
            title = {
                Text(
                    text = if (uiState.pickTargetMode == "merge") "Pick merge target" else "Pick transfer target",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (uiState.pickTargetMode == "merge") {
                        "Tap any other table on the floor to merge into it."
                    } else {
                        "Tap an empty/available table to move this order."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelPickTarget() }) {
                    Text("Cancel pick")
                }
            }
        )
    }

    if (uiState.reserveDialogTable != null) {
        ReserveTableDialog(
            table = uiState.reserveDialogTable!!,
            onDismiss = { viewModel.dismissConfirmDialog() },
            onReserve = { by, until, note, isEdit ->
                viewModel.reserveTable(
                    tableId = uiState.reserveDialogTable!!.id,
                    customerName = by,
                    reservedUntil = until,
                    reservedNote = note,
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
    onReserve: (reservedBy: String, reservedUntil: String, reservedNote: String?, isEdit: Boolean) -> Unit,
    onUnreserve: () -> Unit,
    onStartOrder: () -> Unit
) {
    val isReserved = table.status.lowercase() == "reserved"
    var editMode by remember(table.id) { mutableStateOf(!isReserved) }

    var reservedBy by remember(table.id) {
        mutableStateOf(if (isReserved) table.reservedBy.orEmpty() else "")
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

    val untilDisplay = formatMillisForField(reservedUntilMillis)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startOfDayUtcMillis(reservedUntilMillis),
        yearRange = IntRange(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.YEAR) + 2
        )
    )
    val initialCal = remember(reservedUntilMillis) {
        Calendar.getInstance().apply { timeInMillis = reservedUntilMillis }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCal.get(Calendar.MINUTE),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    editMode && isReserved -> "Edit Reservation ${table.tableNumber}"
                    isReserved && !editMode -> "Reserved Table ${table.tableNumber}"
                    else -> "Reserve Table ${table.tableNumber}"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isReserved && !editMode) {
                    Text(
                        text = "Table ${table.tableNumber} is currently reserved.",
                        fontSize = 14.sp
                    )
                    DetailRow("Reserved By", table.reservedBy ?: "—")
                    DetailRow("Until", formatReservedUntilDisplay(table.reservedUntil) ?: "—")
                    if (!table.reservedNote.isNullOrBlank()) {
                        DetailRow("Note", table.reservedNote)
                    }
                    Text(
                        text = "Start order, edit details, or cancel reservation.",
                        fontSize = 13.sp,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                } else {
                    OutlinedTextField(
                        value = reservedBy,
                        onValueChange = {
                            reservedBy = it
                            formError = null
                        },
                        label = { Text("Reserved By *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reserve_by_field")
                    )

                    OutlinedTextField(
                        value = untilDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Until *") },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                                }
                                IconButton(onClick = { showTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Pick time")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("reserve_until_field")
                    )

                    OutlinedTextField(
                        value = reservedNote,
                        onValueChange = { reservedNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    if (formError != null) {
                        Text(
                            text = formError!!,
                            color = Color(0xFFB71C1C),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isReserved && !editMode) {
                Button(
                    onClick = onStartOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("Start Order")
                }
            } else {
                Button(
                    onClick = {
                        val by = reservedBy.trim()
                        val normalizedUntil = formatMillisForApi(reservedUntilMillis)
                        when {
                            by.isEmpty() -> formError = "Reserved By is required"
                            reservedUntilMillis <= System.currentTimeMillis() ->
                                formError = "Reserved Until must be in the future"
                            else -> {
                                formError = null
                                onReserve(
                                    by,
                                    normalizedUntil,
                                    reservedNote.trim().ifEmpty { null },
                                    isReserved && editMode
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    modifier = Modifier.testTag("reserve_submit_button")
                ) {
                    Text(if (isReserved && editMode) "Save Changes" else "Reserve")
                }
            }
        },
        dismissButton = {
            if (isReserved && !editMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        editMode = true
                        reservedBy = table.reservedBy.orEmpty()
                        reservedUntilMillis =
                            parseReservedUntilMillis(table.reservedUntil) ?: defaultReservedUntilMillis()
                        reservedNote = table.reservedNote.orEmpty()
                        formError = null
                    }) {
                        Text("Edit")
                    }
                    OutlinedButton(onClick = onUnreserve) {
                        Text("Cancel Reservation")
                    }
                }
            } else if (isReserved && editMode) {
                OutlinedButton(onClick = {
                    editMode = false
                    formError = null
                }) {
                    Text("Back")
                }
            } else {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        reservedUntilMillis = mergeDateKeepingTime(selected, reservedUntilMillis)
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
            title = { Text("Select time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reservedUntilMillis = mergeTimeKeepingDate(
                        reservedUntilMillis,
                        timePickerState.hour,
                        timePickerState.minute
                    )
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

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark.copy(alpha = 0.55f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark
        )
    }
}

/**
 * Footer status color chip — colors align with TableCard backgrounds.
 */
@Composable
private fun StatusLegendChip(
    color: Color,
    count: Int,
    total: Int,
    bordered: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(12.dp)
                .then(
                    if (bordered) Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(3.dp))
                    else Modifier
                ),
            shape = RoundedCornerShape(3.dp),
            color = color
        ) {}
        Text(
            text = "$count/$total",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
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
