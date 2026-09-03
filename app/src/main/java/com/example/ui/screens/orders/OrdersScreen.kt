package com.example.ui.screens.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.GuestOrder
import com.example.data.model.OrderItem
import androidx.compose.material.icons.filled.GridView
import com.example.ui.components.OrderItemsGridDialog
import com.example.ui.components.MainTab
import com.example.ui.components.SharedTabStrip
import com.example.ui.components.TopHeaderBar
import com.example.ui.screens.menu.CustomizationBottomSheet
import com.example.ui.theme.*
import com.example.util.CurrencyConfig

@Composable
fun OrdersScreen(
    initialTableId: String?,
    onNavigateToMenu: (tableId: String, guestId: Int) -> Unit,
    onNavigateToFinalize: (orderId: String) -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onLogoutClick: (() -> Unit)? = null,
    viewModel: OrdersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var tableDropdownExpanded by remember { mutableStateOf(false) }
    var showGridPopup by remember { mutableStateOf(false) }
    var gridPopupInitialGuest by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(initialTableId) {
        if (initialTableId != null) {
            viewModel.setTableId(initialTableId)
        } else if (uiState.tableId == null) {
            val firstTable = uiState.tablesList.firstOrNull()
            if (firstTable != null) viewModel.setTableId(firstTable.id)
        } else {
            viewModel.refreshOrder()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshOrder()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = { TopHeaderBar(onLogoutClick = onLogoutClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundGray,
        bottomBar = {
            val order = uiState.order
            val allItems = order?.guests?.flatMap { it.items }.orEmpty()
            val hasPending = allItems.any { it.status.equals("pending", ignoreCase = true) }
            val hasReady = allItems.any { it.status.equals("ready", ignoreCase = true) }
            val showServed = hasReady && !hasPending
            val kotEnabled = !uiState.isSendingKot && hasPending
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("orders_bottom_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Button: KOT (pending) or Served (when ready items waiting)
                    Button(
                        onClick = {
                            if (showServed) viewModel.markServed() else viewModel.sendKot()
                        },
                        enabled = if (showServed) hasReady else kotEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showServed) GreenConfirm else PinkPrimary,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("kot_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                uiState.isSendingKot -> "Sending…"
                                showServed -> "Served"
                                else -> "KOT"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Right Button: Finalize Order (White + Pink Border)
                    OutlinedButton(
                        onClick = {
                            viewModel.validateAndFinalize { orderId ->
                                onNavigateToFinalize(orderId)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(2.dp, PinkPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("finalize_order_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Finalize Order",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Shared 3-tab strip
            SharedTabStrip(
                selectedTab = MainTab.ORDERS,
                onTabSelected = onTabSelected
            )

            val order = uiState.order

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PinkPrimary)
                }
            } else if (order == null) {
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
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Table Selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please select a table from the Tables screen to start or view an order.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onTabSelected(MainTab.TABLES) },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go to Tables")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Row 1: Table Dropdown | Guests [-] N [+]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Table Dropdown
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, PinkPrimary, RoundedCornerShape(8.dp))
                                .clickable { tableDropdownExpanded = true }
                                .padding(horizontal = 12.dp)
                                .testTag("table_dropdown"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val rawTable = order.tableNumber ?: uiState.tableId ?: ""
                                val displayTable = if (rawTable.startsWith("Table", ignoreCase = true)) rawTable else "Table $rawTable"
                                Text(
                                    text = displayTable,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = PinkPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = tableDropdownExpanded,
                                onDismissRequest = { tableDropdownExpanded = false }
                            ) {
                                uiState.tablesList.forEach { tbl ->
                                    val itemLabel = if (tbl.tableNumber.startsWith("Table", ignoreCase = true)) tbl.tableNumber else "Table ${tbl.tableNumber}"
                                    DropdownMenuItem(
                                        text = { Text(itemLabel) },
                                        onClick = {
                                            tableDropdownExpanded = false
                                            viewModel.setTableId(tbl.id)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Guests Counter [-] N [+]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(44.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, PinkPrimary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp)
                                .testTag("guest_counter_stepper")
                        ) {
                            Text(
                                text = "Guests: ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )

                            IconButton(
                                onClick = { viewModel.updateGuestCount(-1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Guest", tint = PinkPrimary)
                            }

                            Text(
                                text = "${order.guestCount}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = { viewModel.updateGuestCount(1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Guest", tint = PinkPrimary)
                            }
                        }
                    }

                    // Title: Order Items (X Items)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order Items (${order.totalItems} Items)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.testTag("order_items_title")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total: ${CurrencyConfig.format(order.grandTotal)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PinkPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // ⊞ Grid View Button (Opens Grid Popup)
                            IconButton(
                                onClick = {
                                    gridPopupInitialGuest = null
                                    showGridPopup = true
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(PinkLightBg, CircleShape)
                                    .testTag("preview_grid_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "View Items Grid Popup",
                                    tint = PinkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (showGridPopup) {
                        OrderItemsGridDialog(
                            order = order,
                            initialGuestFilter = gridPopupInitialGuest,
                            onQtyChange = { itemId, newQty ->
                                viewModel.updateItemQty(itemId, newQty)
                            },
                            onKotClick = {
                                viewModel.sendKot()
                            },
                            onDismissRequest = { showGridPopup = false }
                        )
                    }

                    val editMenuItem = uiState.editMenuItem
                    val editItem = uiState.editItem
                    if (editMenuItem != null && editItem != null && !uiState.isEditLoading) {
                        CustomizationBottomSheet(
                            item = editMenuItem,
                            customization = uiState.editCustomization,
                            isEditMode = true,
                            initialQuantity = editItem.quantity,
                            initialSpice = editItem.spiceLevel,
                            initialMeat = editItem.meatWellness,
                            initialAllergies = editItem.allergies.orEmpty(),
                            initialAddOns = editItem.addOns.orEmpty(),
                            initialToppings = editItem.toppings.orEmpty(),
                            initialNoOnion = editItem.onionFlag == true,
                            initialNoGarlic = editItem.garlicFlag == true,
                            initialSpecialInstructions = editItem.specialInstructions,
                            onDismiss = { viewModel.closeEditItem() },
                            onAddCustomAllergy = { name, onResult ->
                                viewModel.addCustomAllergyForEdit(name, onResult)
                            },
                            onConfirmAdd = { qty, spice, meat, allergies, addOns, toppings, noOnion, noGarlic, instructions ->
                                viewModel.saveEditItem(
                                    qty, spice, meat, allergies, addOns, toppings,
                                    noOnion, noGarlic, instructions
                                )
                            }
                        )
                    }

                    // Guest Chips Row: "All" + Guest 1..N
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("guest_chips_row")
                    ) {
                        item {
                            val isSelected = uiState.selectedGuestFilter == -1
                            GuestChip(
                                label = "All",
                                isSelected = isSelected,
                                isServed = false,
                                onClick = { viewModel.selectGuestFilter(-1) }
                            )
                        }

                        items(order.guests.filter { it.guestId != 0 }) { guest ->
                            val isSelected = uiState.selectedGuestFilter == guest.guestId
                            val isServed = guest.items.isNotEmpty() && guest.items.all { it.status == "ready" || it.status == "served" }
                            val label = "Guest ${guest.guestId}"
                            GuestChip(
                                label = label,
                                isSelected = isSelected,
                                isServed = isServed,
                                onClick = { viewModel.selectGuestFilter(guest.guestId) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Guest Accordion List (with All Guests / Table Common Card at top)
                    val filteredGuests = remember(order.guests, uiState.selectedGuestFilter) {
                        if (uiState.selectedGuestFilter == -1) {
                            val commonGuest = order.guests.find { it.guestId == 0 } 
                                ?: GuestOrder(guestId = 0, guestName = "Table Items (All Guests)", items = emptyList())
                            val individualGuests = order.guests.filter { it.guestId != 0 }
                            listOf(commonGuest) + individualGuests
                        } else {
                            order.guests.filter { it.guestId == uiState.selectedGuestFilter }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("guest_accordion_list")
                    ) {
                        items(filteredGuests) { guest ->
                            GuestAccordionCard(
                                guest = guest,
                                onAddItemsClick = {
                                    onNavigateToMenu(order.tableId ?: "1", guest.guestId)
                                },
                                onQtyChange = { itemId, newQty ->
                                    viewModel.updateItemQty(itemId, newQty)
                                },
                                onItemClick = { item ->
                                    viewModel.openEditItem(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuestChip(
    label: String,
    isSelected: Boolean,
    isServed: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> PinkLightBg
        isServed -> GreenServed
        else -> Color.White
    }

    val borderColor = if (isSelected) PinkPrimary else Color.LightGray

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PinkPrimary else TextDark
        )
    }
}

@Composable
fun GuestAccordionCard(
    guest: GuestOrder,
    onAddItemsClick: () -> Unit,
    onQtyChange: (itemId: String, newQty: Int) -> Unit,
    onItemClick: (OrderItem) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val isCommonGuest = guest.guestId == 0
    val displayName = if (isCommonGuest) "Table Items (All Guests)" else (guest.guestName ?: "Guest ${guest.guestId}")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp, 
                if (isCommonGuest) PinkPrimary.copy(alpha = 0.5f) else Color(0xFFE0E0E0), 
                RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCommonGuest) Color(0xFFFFF7F9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Guest Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCommonGuest) PinkLightBg.copy(alpha = 0.6f) else Color(0xFFFAFAFA))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCommonGuest) PinkPrimary else TextDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${guest.items.size} items)",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "+" button on header -> opens Menu for this guest / table
                    IconButton(
                        onClick = onAddItemsClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (isCommonGuest) PinkPrimary else PinkLightBg, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add, 
                            contentDescription = "Add Item", 
                            tint = if (isCommonGuest) Color.White else PinkPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Accordion",
                        tint = TextDark
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (guest.items.isEmpty()) {
                        val emptyMsg = if (isCommonGuest) {
                            "No common table items yet. Tap '+' to add items for all guests (e.g. Water, Roti, Salad)."
                        } else {
                            "No items added for Guest ${guest.guestId}. Tap '+' to add menu items."
                        }
                        Text(
                            text = emptyMsg,
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        guest.items.forEach { item ->
                            OrderItemCard(
                                item = item,
                                onQtyChange = { newQty -> onQtyChange(item.id, newQty) },
                                onItemClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(
    item: OrderItem,
    onQtyChange: (Int) -> Unit,
    onItemClick: () -> Unit
) {
    val statusLower = item.status?.lowercase()?.trim().orEmpty()
    // KOT+ cancel keeps the row (qty unchanged) and sets status=cancelled — do not expect qty 0
    val isCancelled = statusLower == "cancelled" || statusLower == "canceled"
    val isReady = statusLower == "ready" || statusLower == "served"
    val cardBg = when {
        isCancelled -> Color(0xFFFFEBEE)
        isReady -> GreenReadyTint.copy(alpha = 0.3f)
        else -> Color.White
    }
    val borderColor = when {
        isCancelled -> Color(0xFFEF9A9A)
        isReady -> GreenReadyTint
        else -> Color(0xFFEEEEEE)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = !isCancelled) { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Veg / Non-Veg Indicator Square
                    val isVeg = item.vegType?.lowercase() == "veg"
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, if (isVeg) GreenVeg else RedVegNonVeg, RoundedCornerShape(2.dp))
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isVeg) GreenVeg else RedVegNonVeg, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = item.productName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCancelled) TextMuted else TextDark,
                            textDecoration = if (isCancelled) TextDecoration.LineThrough else null
                        )
                        Text(
                            text = CurrencyConfig.format(item.price),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCancelled) TextMuted else PinkPrimary
                        )
                        if (isCancelled) {
                            Text(
                                text = "CANCELLED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else if (statusLower == "kot") {
                            Text(
                                text = "KOT Sent",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (isCancelled) {
                    // After KOT, cancel keeps qty in DB — show locked, not 0
                    Text(
                        text = "×${item.quantity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                    )
                } else {
                    // Pink Qty Stepper (- / qty / +)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(PinkLightBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { onQtyChange(item.quantity - 1) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PinkPrimary)
                        }

                        Text(
                            text = "${item.quantity}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = { onQtyChange(item.quantity + 1) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PinkPrimary)
                        }
                    }
                }
            }

            // Customization Chips (Spice, Garlic/Onion, Allergies, Addons, Toppings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!item.spiceLevel.isNull_or_blank()) {
                    CustomChip(label = "Spice: ${item.spiceLevel}", color = Color(0xFFFFE0B2), textColor = Color(0xFFE65100))
                }
                if (item.onionFlag == true) {
                    CustomChip(label = "No Onion", color = Color(0xFFF8BBD0), textColor = PinkDark)
                }
                if (item.garlicFlag == true) {
                    CustomChip(label = "No Garlic", color = Color(0xFFF8BBD0), textColor = PinkDark)
                }
                item.allergies?.forEach { allergy ->
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFFCDD2), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(10.dp), tint = RedVegNonVeg)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = allergy, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RedVegNonVeg)
                    }
                }
            }

            if (!item.specialInstructions.isNull_or_blank()) {
                Text(
                    text = "Note: ${item.specialInstructions}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CustomChip(label: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
