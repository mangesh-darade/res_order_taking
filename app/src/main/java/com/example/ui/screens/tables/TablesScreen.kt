package com.example.ui.screens.tables

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.MainTab
import com.example.ui.components.SharedTabStrip
import com.example.ui.components.TableCard
import com.example.ui.components.TopHeaderBar
import com.example.ui.theme.BackgroundGray
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark

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
            // Shared 3-tab strip
            SharedTabStrip(
                selectedTab = MainTab.TABLES,
                onTabSelected = onTabSelected
            )

            // Section Context Bar with LIVE badge
            val titleText = if (!subsectionName.isNull_or_blank()) {
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
                        columns = GridCells.Adaptive(minSize = 105.dp),
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
                                onClick = { onNavigateToOrders(table.id) },
                                onFreeClick = { viewModel.showFreeConfirmDialog(table) }
                            )
                        }
                    }
                }
            }

            // Bottom Footer with Legend and Refresh Button
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
                    // Legend
                    val availableCount = uiState.tables.count { it.status.lowercase() == "available" }
                    val occupiedCount = uiState.tables.count { it.status.lowercase() == "occupied" || it.status.lowercase() == "order-placed" }
                    val totalCount = uiState.tables.size

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                modifier = Modifier
                                    .size(12.dp)
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(3.dp)),
                                shape = RoundedCornerShape(3.dp),
                                color = Color.White
                            ) {}
                            Text(
                                text = "$availableCount/$totalCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = RoundedCornerShape(3.dp),
                                color = com.example.ui.theme.StatusOccupiedBg
                            ) {}
                            Text(
                                text = "$occupiedCount/$totalCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }

                    // Refresh Button
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

    // Free Table Confirm Dialog
    if (uiState.confirmDialogTable != null) {
        val table = uiState.confirmDialogTable!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = { Text(text = "Free Table ${table.tableNumber}?", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to mark table ${table.tableNumber} as free/available?") },
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
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
