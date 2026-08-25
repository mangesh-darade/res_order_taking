package com.example.ui.screens.finalize

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import com.example.ui.components.InvoiceDialog
import com.example.ui.components.TopHeaderBar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizeScreen(
    orderId: String,
    onBackToOrder: () -> Unit,
    onNavigateToTables: () -> Unit,
    viewModel: FinalizeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInvoiceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        viewModel.loadAndFinalize(orderId, onNavigateToTables)
    }

    Scaffold(
        topBar = { TopHeaderBar() },
        containerColor = BackgroundGray,
        bottomBar = {
            val order = uiState.order
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("finalize_bottom_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back to Order
                    OutlinedButton(
                        onClick = onBackToOrder,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(2.dp, PinkPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("back_to_order_button")
                    ) {
                        Text(
                            text = "Back to Order",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary
                        )
                    }

                    // Pink Total Amount box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PinkPrimary)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Amount", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            Text(
                                text = "$${String.format("%.2f", order?.grandTotal ?: uiState.finalizeResult?.grandTotal ?: 0.0)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Green success banner: ✓ Order Confirmed (fades after ~5s)
            AnimatedVisibility(visible = uiState.showSuccessBanner) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("success_banner"),
                    colors = CardDefaults.cardColors(containerColor = GreenConfirm),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "✓ Order Confirmed",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Auto return in ${uiState.autoNavigateTimer}s",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val order = uiState.order

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PinkPrimary)
                }
            } else if (order != null) {
                // Table + Guests Display Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Table: ${order.tableNumber ?: "T-1"}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Guests: ${order.guestCount}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PinkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Section: ${order.sectionName ?: "Main"} ${order.subsectionName?.let { "($it)" } ?: ""}",
                            fontSize = 14.sp,
                            color = TextMuted
                        )

                        if (!uiState.finalizeResult?.saleId.isNull_or_blank()) {
                            Text(
                                text = "Invoice / Sale ID: ${uiState.finalizeResult?.saleId}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showInvoiceDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("view_invoice_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIEW & PRINT SALES INVOICE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (showInvoiceDialog) {
                    InvoiceDialog(
                        order = order,
                        saleId = uiState.finalizeResult?.saleId,
                        onDismissRequest = { showInvoiceDialog = false }
                    )
                }

                Text(
                    text = "Order Details (Read-Only)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Read-only order items
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(order.guests) { guest ->
                        if (guest.items.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Guest ${guest.guestId}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PinkPrimary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    guest.items.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${item.quantity}x ${item.productName}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextDark
                                            )
                                            Text(
                                                text = "$${String.format("%.2f", item.price * item.quantity)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
