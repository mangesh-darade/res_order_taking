package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GuestOrder
import com.example.data.model.OrderBootstrap
import com.example.data.model.OrderItem
import com.example.ui.theme.*

data class GridItemDisplay(
    val item: OrderItem,
    val guestId: Int,
    val guestLabel: String
)

@Composable
fun OrderItemsGridDialog(
    order: OrderBootstrap,
    initialGuestFilter: Int? = null,
    onQtyChange: (itemId: String, newQty: Int) -> Unit,
    onKotClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    // -1 = All Items, 0 = Table Items, 1..N = Specific Guest
    var selectedFilter by remember { mutableStateOf(initialGuestFilter ?: -1) }

    // Flatten all items with guest association
    val allGridItems = remember(order.guests) {
        val list = mutableListOf<GridItemDisplay>()
        order.guests.forEach { guest ->
            val label = if (guest.guestId == 0) "Table Items" else (guest.guestName ?: "Guest ${guest.guestId}")
            guest.items.forEach { itm ->
                list.add(GridItemDisplay(item = itm, guestId = guest.guestId, guestLabel = label))
            }
        }
        list
    }

    val filteredItems = remember(allGridItems, selectedFilter) {
        if (selectedFilter == -1) {
            allGridItems
        } else {
            allGridItems.filter { it.guestId == selectedFilter }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("order_items_grid_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. Header Bar: Title + Table Badge + Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(PinkLightBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid View",
                                tint = PinkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Order Items Grid",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Table: ${order.tableNumber ?: "Table ${order.tableId ?: "1"}"} • ${allGridItems.size} Total Items",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEEEEEE), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Filter Tabs: [ All Items ] [ Table Items ] [ Guest 1 ] [ Guest 2 ] ...
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterTabChip(
                            label = "All Items (${allGridItems.size})",
                            isSelected = selectedFilter == -1,
                            onClick = { selectedFilter = -1 }
                        )
                    }

                    // Individual Guests (GuestId > 0)
                    val individualGuests = order.guests.filter { it.guestId > 0 }
                    items(individualGuests) { g ->
                        val count = allGridItems.count { it.guestId == g.guestId }
                        FilterTabChip(
                            label = "${g.guestName ?: "Guest ${g.guestId}"} ($count)",
                            isSelected = selectedFilter == g.guestId,
                            onClick = { selectedFilter = g.guestId }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(10.dp))

                // 3. Main Content: 2-Column Responsive Grid of Item Cards
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Fastfood,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No items added in this category yet.",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredItems) { gridItem ->
                            GridItemCard(
                                gridItem = gridItem,
                                onQtyChange = { newQty ->
                                    onQtyChange(gridItem.item.id, newQty)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(10.dp))

                // 4. Bottom Summary Bar & Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Grand Total",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Text(
                            text = com.example.util.CurrencyConfig.format(order.grandTotal),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PinkPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBDBDBD)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                        ) {
                            Text("Close", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onKotClick()
                                onDismissRequest()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send KOT", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PinkPrimary else PinkLightBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else PinkPrimary
        )
    }
}

@Composable
fun GridItemCard(
    gridItem: GridItemDisplay,
    onQtyChange: (Int) -> Unit
) {
    val item = gridItem.item
    val isReady = item.status == "ready" || item.status == "served"
    val isVeg = item.vegType?.lowercase() == "veg"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (isReady) GreenReadyTint else Color(0xFFE0E0E0),
                RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) GreenReadyTint.copy(alpha = 0.2f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Top Row: Veg Badge + Guest Assignment Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Veg / NonVeg Square
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .border(1.5.dp, if (isVeg) GreenVeg else RedVegNonVeg, RoundedCornerShape(2.dp))
                        .padding(2.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isVeg) GreenVeg else RedVegNonVeg, CircleShape)
                    )
                }

                // Guest Badge (e.g. Table Items vs Guest 1)
                Box(
                    modifier = Modifier
                        .background(
                            if (gridItem.guestId == 0) Color(0xFFFFF0F5) else Color(0xFFF0F4F8),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = gridItem.guestLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gridItem.guestId == 0) PinkPrimary else Color(0xFF1976D2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dish Name
            Text(
                text = item.productName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Price & Subtotal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${com.example.util.CurrencyConfig.format(item.price)} each",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Text(
                    text = com.example.util.CurrencyConfig.format(item.price * item.quantity),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PinkPrimary
                )
            }

            // Customization Badges: Spice / Allergies / Notes
            val hasCustomizations = !item.spiceLevel.isNullOrBlank() ||
                    item.onionFlag == true ||
                    item.garlicFlag == true ||
                    !item.allergies.isNullOrEmpty() ||
                    !item.specialInstructions.isNullOrBlank()

            if (hasCustomizations) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!item.spiceLevel.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFE0B2), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("🌶️ ${item.spiceLevel}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                    }
                    if (item.onionFlag == true) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF8BBD0), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("No Onion", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PinkDark)
                        }
                    }
                    if (item.garlicFlag == true) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF8BBD0), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("No Garlic", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PinkDark)
                        }
                    }
                }

                if (!item.specialInstructions.isNullOrBlank()) {
                    Text(
                        text = "Note: ${item.specialInstructions}",
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Row: Status Badge + Quantity Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                val statusText = when (item.status?.lowercase()) {
                    "kot" -> "KOT Sent"
                    "ready" -> "Ready"
                    "served" -> "Served"
                    else -> "Pending"
                }
                val statusColor = when (item.status?.lowercase()) {
                    "kot" -> Color(0xFFE65100)
                    "ready" -> Color(0xFF0288D1)
                    "served" -> GreenServed
                    else -> Color(0xFF757575)
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // Quantity Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(PinkLightBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    IconButton(
                        onClick = { onQtyChange(item.quantity - 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PinkPrimary, modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = "${item.quantity}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PinkPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { onQtyChange(item.quantity + 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = PinkPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
