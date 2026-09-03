package com.example.ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CustomizationOption
import com.example.data.model.MenuItem
import com.example.data.model.ProductCustomization
import com.example.ui.theme.*
import com.example.util.CurrencyConfig

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
                    val titleText = if (guestId == 0) {
                        "Menu - Table ${uiState.tableId} (All Guests)"
                    } else {
                        "Menu - Guest $guestId (Table ${uiState.tableId})"
                    }
                    Text(
                        text = titleText,
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
            onAddCustomAllergy = { name, onResult ->
                viewModel.addCustomAllergy(name, onResult)
            },
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
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                    text = CurrencyConfig.format(item.price),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PinkPrimary
                )
                if (item.stockWarning == true) {
                    Text(
                        text = if (item.inStock == false) "Out of stock" else "Low/Zero stock",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.inStock == false) Color(0xFFC62828) else Color(0xFFD97706)
                    )
                } else if (!item.station.isNullOrBlank()) {
                    Text(
                        text = item.station!!,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ADD: outline pink #E91E63, uppercase — blocked when strict stock + out of stock
            OutlinedButton(
                onClick = onAddClick,
                enabled = item.inStock != false,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (item.inStock == false) Color.Gray else PinkAccent
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_item_button_${item.id}")
            ) {
                Text(
                    text = "ADD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.inStock == false) Color.Gray else PinkAccent
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationBottomSheet(
    item: MenuItem,
    customization: ProductCustomization?,
    onDismiss: () -> Unit,
    isEditMode: Boolean = false,
    initialQuantity: Int = 1,
    initialSpice: String? = null,
    initialMeat: String? = null,
    initialAllergies: List<String> = emptyList(),
    initialAddOns: List<String> = emptyList(),
    initialToppings: List<String> = emptyList(),
    initialNoOnion: Boolean = false,
    initialNoGarlic: Boolean = false,
    initialSpecialInstructions: String? = null,
    onAddCustomAllergy: (name: String, onResult: (Result<CustomizationOption>) -> Unit) -> Unit = { _, _ -> },
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
    // Lists from API product_customizations — sections only when data exists
    val toppingsList = customization?.toppings.orEmpty()
    val addOnsList = customization?.addOns.orEmpty()
    val allergiesFromApi = customization?.allergies.orEmpty()
    val spices = customization?.spiceLevels.orEmpty()
    val isVegItem = !item.vegType.equals("non-veg", ignoreCase = true)
    // Meat wellness only for non-veg (API also returns [] for veg)
    val meatOptions = if (isVegItem) emptyList() else customization?.meatWellness.orEmpty()

    var quantity by remember(isEditMode, initialQuantity) {
        mutableStateOf(if (isEditMode) initialQuantity.coerceAtLeast(1) else 1)
    }
    var selectedSpice by remember(spices, isEditMode, initialSpice) {
        mutableStateOf(
            if (isEditMode) {
                initialSpice?.takeIf { it.isNotBlank() }
                    ?: spices.firstOrNull { it.equals(initialSpice, ignoreCase = true) }
                    ?: spices.firstOrNull()
            } else spices.firstOrNull()
        )
    }
    var selectedMeat by remember(meatOptions, isEditMode, initialMeat) {
        mutableStateOf(
            if (isEditMode) {
                initialMeat?.takeIf { it.isNotBlank() }
                    ?: meatOptions.firstOrNull { it.equals(initialMeat, ignoreCase = true) }
                    ?: meatOptions.firstOrNull()
            } else meatOptions.firstOrNull()
        )
    }
    val selectedAllergies = remember { mutableStateListOf<String>() }
    val selectedAddOns = remember { mutableStateListOf<String>() }
    val selectedToppings = remember { mutableStateListOf<String>() }
    var noOnion by remember(isEditMode, initialNoOnion) { mutableStateOf(if (isEditMode) initialNoOnion else false) }
    var noGarlic by remember(isEditMode, initialNoGarlic) { mutableStateOf(if (isEditMode) initialNoGarlic else false) }
    var specialInstructions by remember(isEditMode, initialSpecialInstructions) {
        mutableStateOf(if (isEditMode) initialSpecialInstructions.orEmpty() else "")
    }

    // Prefill selections when editing an existing order item
    LaunchedEffect(isEditMode, initialAllergies, initialAddOns, initialToppings) {
        if (isEditMode) {
            selectedAllergies.clear()
            selectedAllergies.addAll(initialAllergies)
            selectedAddOns.clear()
            selectedAddOns.addAll(initialAddOns)
            selectedToppings.clear()
            selectedToppings.addAll(initialToppings)
        }
    }

    // Local allergy chips (API + newly added custom)
    val allergiesList = remember { mutableStateListOf<CustomizationOption>() }
    LaunchedEffect(allergiesFromApi) {
        allergiesList.clear()
        allergiesList.addAll(allergiesFromApi)
    }

    var showAddAllergyDialog by remember { mutableStateOf(false) }
    var newAllergyName by remember { mutableStateOf("") }
    var allergyError by remember { mutableStateOf<String?>(null) }
    var allergySaving by remember { mutableStateOf(false) }

    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    val scrollMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = maxDialogHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "Edit ${item.name}" else "Customize ${item.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = scrollMaxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (toppingsList.isNotEmpty()) {
                        CustomizationSection(title = "Toppings") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                toppingsList.forEach { top ->
                                    val isChecked = selectedToppings.any {
                                        it.equals(top.name, ignoreCase = true) || it == top.id
                                    }
                                    CustomSelectableChip(
                                        label = "${top.name} (+${CurrencyConfig.format(top.price)})",
                                        isSelected = isChecked,
                                        onClick = {
                                            if (isChecked) {
                                                selectedToppings.removeAll {
                                                    it.equals(top.name, ignoreCase = true) || it == top.id
                                                }
                                            } else selectedToppings.add(top.name)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (addOnsList.isNotEmpty()) {
                        CustomizationSection(title = "Add-ons") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                addOnsList.forEach { addOn ->
                                    val isChecked = selectedAddOns.any {
                                        it.equals(addOn.name, ignoreCase = true) || it == addOn.id
                                    }
                                    CustomSelectableChip(
                                        label = "${addOn.name} (+${CurrencyConfig.format(addOn.price)})",
                                        isSelected = isChecked,
                                        onClick = {
                                            if (isChecked) {
                                                selectedAddOns.removeAll {
                                                    it.equals(addOn.name, ignoreCase = true) || it == addOn.id
                                                }
                                            } else selectedAddOns.add(addOn.name)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (spices.isNotEmpty()) {
                        CustomizationSection(title = "Spice Level") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                spices.forEach { sp ->
                                    CustomSelectableChip(
                                        label = sp,
                                        isSelected = selectedSpice == sp,
                                        onClick = { selectedSpice = sp }
                                    )
                                }
                            }
                        }
                    }

                    if (meatOptions.isNotEmpty()) {
                        CustomizationSection(title = "Meat Wellness") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                meatOptions.forEach { mw ->
                                    CustomSelectableChip(
                                        label = mw,
                                        isSelected = selectedMeat == mw,
                                        onClick = { selectedMeat = mw }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Dietary & Allergies",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            // + adds custom allergy to DB master + selects it
                            IconButton(
                                onClick = {
                                    newAllergyName = ""
                                    allergyError = null
                                    showAddAllergyDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add custom allergy",
                                    tint = PinkPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                            allergiesList.forEach { alg ->
                                val isChecked = selectedAllergies.any {
                                    it.equals(alg.name, ignoreCase = true) || it == alg.id
                                }
                                CustomSelectableChip(
                                    label = alg.name,
                                    isSelected = isChecked,
                                    onClick = {
                                        if (isChecked) {
                                            selectedAllergies.removeAll {
                                                it.equals(alg.name, ignoreCase = true) || it == alg.id
                                            }
                                        } else selectedAllergies.add(alg.name)
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        label = { Text("Special Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PinkPrimary)
                        }
                        Text(
                            text = "$quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PinkPrimary)
                        }
                    }

                    Button(
                        onClick = {
                            onConfirmAdd(
                                quantity,
                                selectedSpice,
                                if (isVegItem) null else selectedMeat,
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
                        Text(
                            if (isEditMode) "Save Changes" else "Add to Order",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showAddAllergyDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!allergySaving) showAddAllergyDialog = false
            },
            title = { Text("Add custom allergy", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newAllergyName,
                        onValueChange = {
                            newAllergyName = it
                            allergyError = null
                        },
                        label = { Text("Allergy name") },
                        singleLine = true,
                        enabled = !allergySaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!allergyError.isNullOrBlank()) {
                        Text(
                            text = allergyError!!,
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newAllergyName.trim()
                        if (trimmed.isEmpty()) {
                            allergyError = "Enter allergy name"
                            return@Button
                        }
                        allergySaving = true
                        onAddCustomAllergy(trimmed) { result ->
                            allergySaving = false
                            result.onSuccess { option ->
                                if (allergiesList.none { it.name.equals(option.name, ignoreCase = true) }) {
                                    allergiesList.add(option)
                                }
                                if (!selectedAllergies.any { it.equals(option.name, ignoreCase = true) }) {
                                    selectedAllergies.add(option.name)
                                }
                                showAddAllergyDialog = false
                                newAllergyName = ""
                            }.onFailure { e ->
                                allergyError = e.message ?: "Could not save allergy"
                            }
                        }
                    },
                    enabled = !allergySaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text(if (allergySaving) "Saving…" else "Add", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddAllergyDialog = false },
                    enabled = !allergySaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomizationSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
