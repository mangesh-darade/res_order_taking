package com.example.ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.MenuItem
import com.example.data.model.ProductCustomization
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    tableId: String,
    guestId: Int,
    onBackToOrders: () -> Unit,
    viewModel: MenuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tableId, guestId) {
        viewModel.initialize(tableId, guestId)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            // Pink header WITH back arrow + title "Menu"
            TopAppBar(
                title = {
                    Text(
                        text = "Menu - Guest $guestId (Table ${uiState.tableId})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToOrders,
                        modifier = Modifier.testTag("menu_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Orders",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PinkPrimary),
                modifier = Modifier.testTag("menu_top_bar")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundGray
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Search field: pink border, 50dp, 8dp radius, search icon left
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search menu items...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PinkPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = PinkPrimary.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("menu_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter row: Veg (green border) | Non-veg (red border) | Clear All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterPill(
                    label = "Veg",
                    borderColor = GreenVeg,
                    isSelected = uiState.selectedMealType == "veg",
                    onClick = { viewModel.setMealType("veg") }
                )

                FilterPill(
                    label = "Non-Veg",
                    borderColor = RedVegNonVeg,
                    isSelected = uiState.selectedMealType == "non-veg",
                    onClick = { viewModel.setMealType("non-veg") }
                )

                if (uiState.selectedMealType != "all" || uiState.selectedCategory != null || uiState.searchQuery.isNotEmpty()) {
                    TextButton(onClick = {
                        viewModel.setSearchQuery("")
                        viewModel.setMealType("all")
                        viewModel.selectCategory(null)
                    }) {
                        Text("Clear All", color = PinkPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal category chips: "All Items" + categories (Active: #333 fill, white text)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("category_chips_row")
            ) {
                item {
                    val isSelected = uiState.selectedCategory == null
                    CategoryChip(
                        label = "All Items",
                        isSelected = isSelected,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }

                items(uiState.categories) { cat ->
                    val isSelected = uiState.selectedCategory == cat.id
                    CategoryChip(
                        label = cat.name,
                        isSelected = isSelected,
                        onClick = { viewModel.selectCategory(cat.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vertical list of menu cards
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PinkPrimary)
                }
            } else if (uiState.menuItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items found", color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    modifier = Modifier.fillMaxSize().testTag("menu_items_list")
                ) {
                    items(uiState.menuItems) { menuItem ->
                        MenuItemCard(
                            item = menuItem,
                            onAddClick = { viewModel.openCustomizationSheet(menuItem) }
                        )
                    }
                }
            }
        }
    }

    // Add Item Customization Sheet / Modal
    if (uiState.customDialogItem != null) {
        val item = uiState.customDialogItem!!
        CustomizationBottomSheet(
            item = item,
            customization = uiState.customizationData,
            onDismiss = { viewModel.closeCustomizationSheet() },
            onConfirmAdd = { qty, spice, meat, allergies, addOns, toppings, noOnion, noGarlic, instructions ->
                viewModel.addItemToOrder(item, qty, spice, meat, allergies, addOns, toppings, noOnion, noGarlic, instructions)
            }
        )
    }
}

@Composable
fun FilterPill(
    label: String,
    borderColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) borderColor.copy(alpha = 0.15f) else Color.White)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(borderColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) borderColor else TextDark
            )
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFF333333) else Color.White
    val textColor = if (isSelected) Color.White else TextDark

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, if (isSelected) Color(0xFF333333) else Color.LightGray, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [80x80 thumb]
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // [veg box + name + price]
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isVeg = item.vegType?.lowercase() == "veg"
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(1.dp, if (isVeg) GreenVeg else RedVegNonVeg, RoundedCornerShape(2.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isVeg) GreenVeg else RedVegNonVeg, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description!!,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Text(
                    text = "$${String.format("%.2f", item.price)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PinkPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ADD: outline pink #E91E63, uppercase
            OutlinedButton(
                onClick = onAddClick,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PinkAccent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_item_button_${item.id}")
            ) {
                Text(
                    text = "ADD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PinkAccent
                )
            }
        }
    }
}

@Composable
fun CustomizationBottomSheet(
    item: MenuItem,
    customization: ProductCustomization?,
    onDismiss: () -> Unit,
    onConfirmAdd: (
        qty: Int,
        spice: String?,
        meat: String?,
        allergies: List<String>,
        addOns: List<String>,
        toppings: List<String>,
        noOnion: Boolean,
        noGarlic: Boolean,
        specialInstructions: String?
    ) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    var selectedSpice by remember { mutableStateOf<String?>(customization?.spiceLevels?.firstOrNull()) }
    var selectedMeat by remember { mutableStateOf<String?>(customization?.meatWellness?.firstOrNull()) }
    val selectedAllergies = remember { mutableStateListOf<String>() }
    val selectedAddOns = remember { mutableStateListOf<String>() }
    val selectedToppings = remember { mutableStateListOf<String>() }
    var noOnion by remember { mutableStateOf(false) }
    var noGarlic by remember { mutableStateOf(false) }
    var specialInstructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize ${item.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 1. Toppings checklist chips
                val toppingsList = customization?.toppings ?: emptyList()
                if (toppingsList.isNotEmpty()) {
                    Text("Toppings:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        toppingsList.forEach { top ->
                            val isChecked = selectedToppings.contains(top.name)
                            CustomSelectableChip(
                                label = "${top.name} (+$${top.price})",
                                isSelected = isChecked,
                                onClick = {
                                    if (isChecked) selectedToppings.remove(top.name) else selectedToppings.add(top.name)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Add-ons checklist chips
                val addOnsList = customization?.addOns ?: emptyList()
                if (addOnsList.isNotEmpty()) {
                    Text("Add-ons:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        addOnsList.forEach { addOn ->
                            val isChecked = selectedAddOns.contains(addOn.name)
                            CustomSelectableChip(
                                label = "${addOn.name} (+$${addOn.price})",
                                isSelected = isChecked,
                                onClick = {
                                    if (isChecked) selectedAddOns.remove(addOn.name) else selectedAddOns.add(addOn.name)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. Cooking options: Spice Levels
                val spices = customization?.spiceLevels ?: listOf("Mild", "Medium", "Hot")
                Text("Spice Level:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    spices.forEach { sp ->
                        val isSelected = selectedSpice == sp
                        CustomSelectableChip(
                            label = sp,
                            isSelected = isSelected,
                            onClick = { selectedSpice = sp }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Allergies chips & No Onion/No Garlic
                Text("Dietary & Allergies:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CustomSelectableChip(
                        label = "No Onion",
                        isSelected = noOnion,
                        onClick = { noOnion = !noOnion }
                    )
                    CustomSelectableChip(
                        label = "No Garlic",
                        isSelected = noGarlic,
                        onClick = { noGarlic = !noGarlic }
                    )
                    val allergiesList = customization?.allergies ?: emptyList()
                    allergiesList.forEach { alg ->
                        val isChecked = selectedAllergies.contains(alg.name)
                        CustomSelectableChip(
                            label = alg.name,
                            isSelected = isChecked,
                            onClick = {
                                if (isChecked) selectedAllergies.remove(alg.name) else selectedAllergies.add(alg.name)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = specialInstructions,
                    onValueChange = { specialInstructions = it },
                    label = { Text("Special Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Footer: qty stepper + pink "Add to Order"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(PinkLightBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        IconButton(onClick = { if (quantity > 1) quantity-- }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PinkPrimary)
                        }
                        Text(
                            text = "$quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { quantity++ }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PinkPrimary)
                        }
                    }

                    Button(
                        onClick = {
                            onConfirmAdd(
                                quantity,
                                selectedSpice,
                                selectedMeat,
                                selectedAllergies.toList(),
                                selectedAddOns.toList(),
                                selectedToppings.toList(),
                                noOnion,
                                noGarlic,
                                specialInstructions.ifEmpty { null }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add to Order", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun CustomSelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PinkLightBg else Color(0xFFF0F0F0),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) PinkPrimary else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PinkPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PinkPrimary else TextDark
            )
        }
    }
}

@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
