package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PendingSyncEntity
import com.example.data.sync.SyncManager
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncStatusDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncManager = remember { SyncManager.getInstance(context) }

    val allActions by syncManager.allActionsFlow.collectAsState(initial = emptyList())
    val isSyncing by syncManager.isSyncing.collectAsState()
    val isOnline by syncManager.isOnline.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, FAILED
    var conflictItemToReassign by remember { mutableStateOf<PendingSyncEntity?>(null) }

    val pendingList = remember(allActions) { allActions.filter { it.status == "PENDING" || it.status == "SYNCING" } }
    val failedList = remember(allActions) { allActions.filter { it.status == "FAILED" || it.status == "CONFLICT" } }

    val displayList = remember(allActions, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> pendingList
            "FAILED" -> failedList
            else -> allActions
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("sync_status_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (failedList.isNotEmpty()) Color(0xFFF44336) else if (!isOnline) Color(0xFFFF9800) else PinkPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (failedList.isNotEmpty()) Icons.Default.Warning else Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Offline Sync Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextDark
                            )
                            Text(
                                text = if (!isOnline) "Offline Mode (Queued locally)" else if (isSyncing) "Syncing with server..." else "Online & Monitoring",
                                fontSize = 12.sp,
                                color = if (!isOnline) Color(0xFFFF9800) else if (isSyncing) PinkPrimary else Color(0xFF4CAF50)
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Tabs & Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${allActions.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Queued (${pendingList.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = PinkPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "FAILED",
                        onClick = { selectedFilter = "FAILED" },
                        label = { Text("Failed / Conflict (${failedList.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFD32F2F)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action List
                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilter == "FAILED") "No failed sync actions!" else "All orders synced with server.",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList, key = { it.id }) { item ->
                            SyncItemRow(
                                item = item,
                                timeStr = timeFormatter.format(Date(item.updatedAt.takeIf { it > 0 } ?: item.createdAt)),
                                onRetry = {
                                    coroutineScope.launch {
                                        syncManager.retryAction(item.id)
                                    }
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        syncManager.deleteAction(item.id)
                                    }
                                },
                                onReassign = {
                                    conflictItemToReassign = item
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (failedList.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    syncManager.clearFailedActions()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                        ) {
                            Text("Clear Failed", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (failedList.isNotEmpty()) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        syncManager.retryAllFailed()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry All Failed", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    syncManager.syncPendingActions()
                                }
                            },
                            enabled = !isSyncing && pendingList.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSyncing) "Syncing..." else "Sync Now", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Conflict Reassign Table Dialog
    conflictItemToReassign?.let { item ->
        ReassignTableDialog(
            item = item,
            onDismiss = { conflictItemToReassign = null },
            onConfirmNewTable = { newTableId ->
                coroutineScope.launch {
                    syncManager.reassignConflictTable(item.id, newTableId)
                    conflictItemToReassign = null
                }
            }
        )
    }
}

@Composable
private fun SyncItemRow(
    item: PendingSyncEntity,
    timeStr: String,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onReassign: () -> Unit
) {
    val isFailed = item.status == "FAILED"
    val isConflict = item.status == "CONFLICT"
    val isSyncing = item.status == "SYNCING"

    val statusBg = when {
        isConflict -> Color(0xFFFFF3E0)
        isFailed -> Color(0xFFFFEBEE)
        isSyncing -> Color(0xFFE3F2FD)
        else -> Color(0xFFF5F5F5)
    }

    val statusColor = when {
        isConflict -> Color(0xFFE65100)
        isFailed -> Color(0xFFD32F2F)
        isSyncing -> Color(0xFF1976D2)
        else -> Color(0xFF757575)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PinkPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.actionType.replace("_", " "),
                            color = PinkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.orderId.ifBlank { "Order" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor,
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.status,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            if (!item.lastErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.lastErrorMessage,
                    fontSize = 11.sp,
                    color = if (isConflict) Color(0xFFE65100) else Color(0xFFD32F2F),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isFailed || isConflict) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isConflict) {
                        TextButton(
                            onClick = onReassign,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reassign Table", fontSize = 11.sp, color = Color(0xFFE65100))
                        }
                    }

                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 11.sp, color = PinkPrimary)
                    }

                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextMuted)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Discard", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReassignTableDialog(
    item: PendingSyncEntity,
    onDismiss: () -> Unit,
    onConfirmNewTable: (String) -> Unit
) {
    var newTableInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reassign Conflicted Table", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "The table originally assigned has a conflict on the server. Enter another free table number to resend this order:",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newTableInput,
                    onValueChange = { newTableInput = it },
                    label = { Text("New Table # (e.g. 5 or T2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTableInput.isNotBlank()) {
                        onConfirmNewTable(newTableInput.trim())
                    }
                },
                enabled = newTableInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text("Reassign & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
