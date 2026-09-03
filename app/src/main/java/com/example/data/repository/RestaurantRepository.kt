package com.example.data.repository

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.api.RestaurantApiService
import com.example.data.local.MenuCache
import com.example.data.local.OrderCache
import com.example.data.model.*
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncedAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestaurantRepository private constructor() {

    companion object {
        @Volatile
        private var instance: RestaurantRepository? = null

        fun getInstance(): RestaurantRepository {
            return instance ?: synchronized(this) {
                instance ?: RestaurantRepository().also { instance = it }
            }
        }

        operator fun invoke(): RestaurantRepository = getInstance()

        private const val MIN_FLOOR_SYNC_MS = 5L * 60L * 1000L
        private const val MIN_CATALOG_SYNC_MS = 30L * 60L * 1000L
    }

    private val api: RestaurantApiService
        get() = ApiClient.service

    private var syncManager: SyncManager? = null
    private var menuCache: MenuCache? = null
    private var orderCache: OrderCache? = null
    private var syncListenersAttached = false
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initSyncManager(context: Context) {
        initLocalStorage(context)
    }

    fun initLocalStorage(context: Context) {
        val appContext = context.applicationContext
        if (syncManager == null) {
            syncManager = SyncManager.getInstance(appContext)
        }
        if (menuCache == null) {
            menuCache = MenuCache(appContext)
        }
        if (orderCache == null) {
            orderCache = OrderCache(appContext)
        }
        attachSyncListeners()
        repoScope.launch {
            loadPersistedState()
        }
    }

    fun isOnline(): Boolean = syncManager?.isOnline?.value == true

    private fun attachSyncListeners() {
        if (syncListenersAttached) return
        syncListenersAttached = true
        syncManager?.setOnActionSyncedListener { action ->
            handleSyncedAction(action)
        }
        syncManager?.setOnNetworkSyncCompleteListener {
            refreshAfterNetworkSync()
        }
    }

    private suspend fun loadPersistedState() {
        val savedOrders = orderCache?.getActiveOrders().orEmpty()
        if (savedOrders.isNotEmpty()) {
            _orders.value = savedOrders
        }
        val sections = menuCache?.getSections().orEmpty()
        if (sections.isNotEmpty()) {
            _sections.value = sections
        }
        val categories = menuCache?.getCategories().orEmpty()
        if (categories.isNotEmpty()) {
            _categories.value = categories
        }
    }

    private suspend fun handleSyncedAction(action: SyncedAction) {
        when (action.actionType) {
            "CREATE_ORDER" -> {
                val localId = action.payload["localOrderId"]?.toString().orEmpty()
                val serverId = action.orderId.orEmpty()
                if (localId.isNotBlank() && serverId.isNotBlank() && localId != serverId) {
                    orderCache?.remapOrderId(localId, serverId)
                    _orders.value = _orders.value.map { order ->
                        if (order.orderId == localId) order.copy(orderId = serverId) else order
                    }
                }
            }
            "FINALIZE_ORDER" -> {
                action.orderId?.let { orderCache?.deleteOrder(it) }
            }
            "FREE_TABLE" -> {
                action.tableId?.let { orderCache?.deleteOrdersForTable(it) }
            }
        }
    }

    /** After pending queue drains: light refresh — no hammering API. */
    private suspend fun refreshAfterNetworkSync() {
        if (!isOnline()) return
        syncFloorPlanIfOnline(force = false)
        syncMenuCatalogIfOnline(force = false)
        val pending = syncManager?.getPendingCount() ?: 0
        if (pending > 0) return
        for (order in _orders.value) {
            val tableId = order.tableId ?: continue
            if (syncManager?.hasPendingForTable(tableId, order.orderId) == true) continue
            fetchOrderBootstrap(tableId = tableId, orderId = order.orderId)
        }
    }

    suspend fun clearPendingSyncQueue() {
        syncManager?.clearPendingQueue()
    }

    // In-memory state for live API data
    private val _branding = MutableStateFlow(BrandingInfo())
    private val _sections = MutableStateFlow<List<Section>>(emptyList())
    private val _subsections = MutableStateFlow<List<Subsection>>(emptyList())
    private val _tables = MutableStateFlow<List<TableItem>>(emptyList())
    private val _orders = MutableStateFlow<List<OrderBootstrap>>(emptyList())
    private val _categories = MutableStateFlow<List<MenuCategory>>(emptyList())
    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    private val _customizations = MutableStateFlow<Map<String, ProductCustomization>>(emptyMap())

    val branding: StateFlow<BrandingInfo> = _branding.asStateFlow()
    val sections: StateFlow<List<Section>> = _sections.asStateFlow()
    val tables: StateFlow<List<TableItem>> = _tables.asStateFlow()

    suspend fun fetchBranding(): Result<BrandingInfo> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBranding()
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: BrandingInfo()
                _branding.value = data
                com.example.util.CurrencyConfig.updateFromBranding(data)
                return@withContext Result.success(data)
            }
        } catch (e: Exception) {
            // fallback
        }
        com.example.util.CurrencyConfig.updateFromBranding(_branding.value)
        Result.success(_branding.value)
    }

    suspend fun fetchSections(): Result<List<Section>> = withContext(Dispatchers.IO) {
        val cache = menuCache
        val local = cache?.getSections().orEmpty()
        if (local.isNotEmpty()) {
            _sections.value = local
        }
        if (!isOnline()) {
            if (local.isNotEmpty()) return@withContext Result.success(local)
            return@withContext Result.success(_sections.value)
        }
        try {
            val response = api.getSections()
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: emptyList()
                if (data.isNotEmpty()) {
                    cache?.saveSections(data)
                    _sections.value = data
                    return@withContext Result.success(data)
                }
            }
        } catch (_: Exception) {
        }
        if (local.isNotEmpty()) {
            return@withContext Result.success(local)
        }
        Result.success(_sections.value)
    }

    suspend fun fetchSubsections(sectionId: String): Result<List<Subsection>> = withContext(Dispatchers.IO) {
        val cache = menuCache
        val local = cache?.getSubsections(sectionId).orEmpty()
        if (local.isNotEmpty()) {
            _subsections.value = _subsections.value.filter { it.sectionId != sectionId } + local
        }
        if (!isOnline()) {
            if (local.isNotEmpty()) return@withContext Result.success(local)
            return@withContext Result.success(_subsections.value.filter { it.sectionId == sectionId })
        }
        try {
            val response = api.getSubsections(sectionId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: emptyList()
                cache?.saveSubsections(sectionId, data)
                _subsections.value = _subsections.value.filter { it.sectionId != sectionId } + data
                return@withContext Result.success(data)
            }
        } catch (_: Exception) {
        }
        if (local.isNotEmpty()) {
            return@withContext Result.success(local)
        }
        Result.success(_subsections.value.filter { it.sectionId == sectionId })
    }

    suspend fun fetchTables(sectionId: String, subsectionId: String? = null): Result<List<TableItem>> = withContext(Dispatchers.IO) {
        val cache = menuCache
        val local = cache?.getTables(sectionId, subsectionId).orEmpty()
        if (local.isNotEmpty()) {
            val merged = mergeTablesWithMemory(local)
            _tables.value = merged
        }
        if (!isOnline()) {
            if (local.isNotEmpty()) {
                return@withContext Result.success(mergeTablesWithMemory(local))
            }
            val filtered = _tables.value.filter {
                (it.sectionId == null || it.sectionId == sectionId) &&
                    (subsectionId == null || it.subsectionId == null || it.subsectionId == subsectionId)
            }
            return@withContext Result.success(filtered)
        }
        try {
            val response = api.getTables(sectionId, subsectionId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: emptyList()
                if (data.isNotEmpty()) {
                    cache?.upsertTables(data)
                    _tables.value = data
                    return@withContext Result.success(data)
                }
            }
        } catch (_: Exception) {
        }
        if (local.isNotEmpty()) {
            return@withContext Result.success(mergeTablesWithMemory(local))
        }
        val filtered = _tables.value.filter {
            (it.sectionId == null || it.sectionId == sectionId) &&
                (subsectionId == null || it.subsectionId == null || it.subsectionId == subsectionId)
        }
        Result.success(filtered)
    }

    /**
     * Pull sections, subsections and tables into Room (online only).
     */
    suspend fun syncFloorPlanIfOnline(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val cache = menuCache ?: return@withContext Result.failure(Exception("Menu cache not initialized"))
        if (!isOnline()) {
            return@withContext Result.failure(Exception("Offline"))
        }
        if (!force) {
            val last = cache.getLastFloorSyncAt()
            if (System.currentTimeMillis() - last < MIN_FLOOR_SYNC_MS) {
                return@withContext Result.success(Unit)
            }
        }
        try {
            val secResponse = api.getSections()
            if (!secResponse.isSuccessful || secResponse.body()?.response?.status != "SUCCESS") {
                return@withContext Result.failure(Exception("Failed to sync sections"))
            }
            val sections = secResponse.body()?.data ?: emptyList()
            if (sections.isEmpty()) {
                return@withContext Result.success(Unit)
            }
            cache.saveSections(sections)
            _sections.value = sections

            val allTables = mutableListOf<TableItem>()
            for (section in sections) {
                val subResponse = api.getSubsections(section.id)
                if (subResponse.isSuccessful && subResponse.body()?.response?.status == "SUCCESS") {
                    val subs = subResponse.body()?.data ?: emptyList()
                    cache.saveSubsections(section.id, subs)
                    _subsections.value = _subsections.value.filter { it.sectionId != section.id } + subs

                    val tablesNoSub = api.getTables(section.id, null)
                    if (tablesNoSub.isSuccessful && tablesNoSub.body()?.response?.status == "SUCCESS") {
                        tablesNoSub.body()?.data?.let { allTables.addAll(it) }
                    }
                    for (sub in subs) {
                        val tablesSub = api.getTables(section.id, sub.id)
                        if (tablesSub.isSuccessful && tablesSub.body()?.response?.status == "SUCCESS") {
                            tablesSub.body()?.data?.let { allTables.addAll(it) }
                        }
                    }
                }
            }
            if (allTables.isNotEmpty()) {
                cache.upsertTables(allTables.distinctBy { it.id })
                _tables.value = allTables.distinctBy { it.id }
            }
            cache.markFloorPlanSynced()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mergeTablesWithMemory(cached: List<TableItem>): List<TableItem> {
        if (_tables.value.isEmpty()) {
            return cached
        }
        val memoryById = _tables.value.associateBy { it.id }
        return cached.map { row ->
            memoryById[row.id] ?: row
        }
    }

    suspend fun fetchOrderBootstrap(tableId: String?, orderId: String? = null): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        var existingOrder = _orders.value.find {
            (orderId != null && it.orderId == orderId) ||
            (tableId != null && it.tableId == tableId && it.status != "finalized" && it.status != "completed")
        }
        if (existingOrder == null && !orderId.isNullOrBlank()) {
            existingOrder = orderCache?.getOrder(orderId)
        }
        if (existingOrder == null && !tableId.isNullOrBlank()) {
            existingOrder = orderCache?.getOrderByTableId(tableId)
        }
        if (existingOrder != null && _orders.value.none {
                it.orderId == existingOrder?.orderId ||
                    (!tableId.isNullOrBlank() && it.tableId == tableId)
            }
        ) {
            existingOrder = updateLocalOrder(existingOrder)
        }

        val pendingForThis = syncManager?.hasPendingForTable(tableId, orderId ?: existingOrder?.orderId) == true
        if (pendingForThis && existingOrder != null) {
            return@withContext Result.success(existingOrder)
        }

        if (!isOnline()) {
            if (existingOrder != null) {
                return@withContext Result.success(existingOrder)
            }
            if (tableId != null) {
                val fromTable = orderCache?.getOrderByTableId(tableId)
                if (fromTable != null) {
                    return@withContext Result.success(updateLocalOrder(fromTable))
                }
            }
            val resultOrder = existingOrder ?: OrderBootstrap(tableId = tableId ?: "1", tableNumber = "T-1")
            return@withContext Result.success(updateLocalOrder(resultOrder))
        }

        try {
            val response = api.getOrderBootstrap(tableId = tableId, orderId = orderId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let { serverOrder ->
                    val localItemCount = existingOrder?.guests?.flatMap { it.items }?.sumOf { it.quantity } ?: 0
                    val serverItemCount = serverOrder.guests.flatMap { it.items }.sumOf { it.quantity }
                    // Server still empty/stale while offline queue is draining — keep local cart
                    if (existingOrder != null && localItemCount > serverItemCount
                        && syncManager?.hasPendingForTable(tableId, existingOrder.orderId) == true
                    ) {
                        return@withContext Result.success(existingOrder)
                    }
                    val normalized = updateLocalOrder(serverOrder)
                    return@withContext Result.success(normalized)
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        if (existingOrder == null && tableId != null) {
            val table = _tables.value.find { it.id == tableId }
            val newOrderId = "ORD-$tableId-${System.currentTimeMillis() % 10000}"
            val section = _sections.value.find { it.id == table?.sectionId }
            val subsection = _subsections.value.find { it.id == table?.subsectionId }
            val initialGuestCount = maxOf(table?.guestsCount ?: 2, 2)
            val initialGuests = (1..initialGuestCount).map { gId ->
                GuestOrder(guestId = gId, guestName = "Guest $gId", items = emptyList())
            }
            existingOrder = OrderBootstrap(
                orderId = newOrderId,
                tableId = tableId,
                tableNumber = table?.tableNumber ?: "T-$tableId",
                sectionName = section?.name ?: "Main Dining",
                subsectionName = subsection?.name ?: "Hall A",
                guestCount = initialGuestCount,
                status = "active",
                guests = initialGuests,
                totalItems = 0,
                grandTotal = 0.0
            )
            val normalized = updateLocalOrder(existingOrder)
            return@withContext Result.success(normalized)
        }

        val resultOrder = existingOrder ?: OrderBootstrap(tableId = tableId ?: "1", tableNumber = "T-1")
        val normalized = updateLocalOrder(resultOrder)
        Result.success(normalized)
    }

    suspend fun createOrder(tableId: String, guestCount: Int): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        try {
            val response = api.createOrder(tableId, guestCount)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let {
                    val normalized = updateLocalOrder(it)
                    return@withContext Result.success(normalized)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        // Always create a clean fresh order state for new table session
        val table = _tables.value.find { it.id == tableId }
        val newOrderId = "ORD-$tableId-${System.currentTimeMillis() % 10000}"
        val section = _sections.value.find { it.id == table?.sectionId }
        val subsection = _subsections.value.find { it.id == table?.subsectionId }
        val initialGuests = (1..guestCount).map { gId ->
            GuestOrder(guestId = gId, guestName = "Guest $gId")
        }
        val newOrder = OrderBootstrap(
            orderId = newOrderId,
            tableId = tableId,
            tableNumber = table?.tableNumber ?: "T-$tableId",
            sectionName = section?.name ?: "Main Dining",
            subsectionName = subsection?.name ?: "Hall A",
            guestCount = guestCount,
            status = "active",
            guests = initialGuests,
            totalItems = 0,
            grandTotal = 0.0
        )
        // Clear any old order for this table
        _orders.value = _orders.value.filterNot { it.tableId == tableId }
        val normalized = updateLocalOrder(newOrder)
        // Mark table occupied
        updateTableStatus(tableId, "occupied", normalized.guestCount, normalized.orderId)
        syncManager?.enqueueAction(
            normalized.orderId,
            "CREATE_ORDER",
            mapOf(
                "tableId" to tableId,
                "guestCount" to guestCount,
                "localOrderId" to (normalized.orderId ?: newOrderId)
            )
        )
        Result.success(normalized)
    }

    suspend fun updateGuestCount(orderId: String, delta: Int, tableId: String? = null): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        try {
            val response = if (delta > 0) api.increaseGuest(orderId, tableId) else api.decreaseGuest(orderId, tableId)
            val body = response.body()
            if (response.isSuccessful && (body?.response?.status.equals("SUCCESS", ignoreCase = true) || body?.data != null)) {
                body?.data?.let {
                    val normalized = updateLocalOrder(it)
                    return@withContext Result.success(normalized)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        val cleanTableId = tableId ?: orderId.removePrefix("ORD-").split("-").firstOrNull()
        val order = _orders.value.find { 
            (!it.orderId.isNullOrBlank() && it.orderId == orderId) || (cleanTableId != null && it.tableId == cleanTableId)
        } ?: _orders.value.firstOrNull() ?: return@withContext Result.failure(Exception("Order not found"))

        val currentIndividualGuests = order.guests.filter { it.guestId != 0 }
        val currentGuestCount = maxOf(order.guestCount, currentIndividualGuests.size, 1)
        val newCount = (currentGuestCount + delta).coerceAtLeast(1)

        val guests = if (newCount > currentIndividualGuests.size) {
            currentIndividualGuests + (currentIndividualGuests.size + 1..newCount).map {
                GuestOrder(guestId = it, guestName = "Guest $it")
            }
        } else {
            currentIndividualGuests.take(newCount)
        }
        val tableItemsGuest = order.guests.find { it.guestId == 0 }
        val allGuests = if (tableItemsGuest != null) listOf(tableItemsGuest) + guests else guests
        val updated = order.copy(guestCount = newCount, guests = allGuests)
        val normalized = updateLocalOrder(updated)
        Result.success(normalized)
    }

    /**
     * Pull full menu catalog from API into Room (online only).
     * Safe to call on menu open / app start.
     */
    suspend fun syncMenuCatalogIfOnline(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val cache = menuCache ?: return@withContext Result.failure(Exception("Menu cache not initialized"))
        if (!isOnline()) {
            return@withContext Result.failure(Exception("Offline"))
        }
        if (!force) {
            val last = cache.getLastCatalogSyncAt()
            if (System.currentTimeMillis() - last < MIN_CATALOG_SYNC_MS) {
                return@withContext Result.success(Unit)
            }
        }
        try {
            val catResponse = api.getMenuCategories()
            if (catResponse.isSuccessful && catResponse.body()?.response?.status == "SUCCESS") {
                val categories = catResponse.body()?.data ?: emptyList()
                if (categories.isNotEmpty()) {
                    cache.saveCategories(categories)
                    _categories.value = categories
                }
            }
            val itemResponse = api.getMenuItems(categoryId = null, mealType = null, search = null)
            if (itemResponse.isSuccessful && itemResponse.body()?.response?.status == "SUCCESS") {
                val items = itemResponse.body()?.data ?: emptyList()
                if (items.isNotEmpty()) {
                    cache.replaceAllItems(items)
                    _menuItems.value = items
                    cache.markCatalogSynced()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMenuCategories(): Result<List<MenuCategory>> = withContext(Dispatchers.IO) {
        val cache = menuCache
        val local = cache?.getCategories().orEmpty()
        if (local.isNotEmpty()) {
            _categories.value = local
        }
        if (!isOnline()) {
            if (local.isNotEmpty()) return@withContext Result.success(local)
            return@withContext Result.success(_categories.value)
        }
        try {
            val response = api.getMenuCategories()
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: emptyList()
                if (data.isNotEmpty()) {
                    cache?.saveCategories(data)
                    _categories.value = data
                    return@withContext Result.success(data)
                }
            }
        } catch (_: Exception) {
        }
        if (local.isNotEmpty()) {
            return@withContext Result.success(local)
        }
        Result.success(_categories.value)
    }

    suspend fun fetchMenuItems(categoryId: String? = null, mealType: String? = null, search: String? = null): Result<List<MenuItem>> = withContext(Dispatchers.IO) {
        val cache = menuCache
        val local = cache?.getItems(categoryId, mealType, search).orEmpty()
        if (local.isNotEmpty()) {
            _menuItems.value = local
        }
        val isFullCatalogFetch = categoryId.isNullOrBlank()
            && (mealType.isNullOrBlank() || mealType.equals("all", ignoreCase = true))
            && search.isNullOrBlank()
        if (!isOnline()) {
            if (local.isNotEmpty()) return@withContext Result.success(local)
            var list = _menuItems.value
            if (!categoryId.isNullOrBlank()) {
                list = list.filter { it.categoryId == categoryId }
            }
            if (!mealType.isNullOrBlank() && mealType != "all") {
                list = list.filter { it.vegType.equals(mealType, ignoreCase = true) }
            }
            if (!search.isNullOrBlank()) {
                list = list.filter { it.name.contains(search, ignoreCase = true) }
            }
            return@withContext Result.success(list)
        }
        try {
            val response = api.getMenuItems(categoryId, mealType, search)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: emptyList()
                if (data.isNotEmpty()) {
                    if (isFullCatalogFetch) {
                        cache?.replaceAllItems(data)
                        cache?.markCatalogSynced()
                    } else {
                        cache?.upsertItems(data)
                    }
                    _menuItems.value = data
                    return@withContext Result.success(data)
                }
            }
        } catch (_: Exception) {
        }
        if (local.isNotEmpty()) {
            return@withContext Result.success(local)
        }
        var list = _menuItems.value
        if (!categoryId.isNullOrBlank()) {
            list = list.filter { it.categoryId == categoryId }
        }
        if (!mealType.isNullOrBlank() && mealType != "all") {
            list = list.filter { it.vegType.equals(mealType, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            list = list.filter { it.name.contains(search, ignoreCase = true) }
        }
        Result.success(list)
    }

    suspend fun fetchProductCustomizations(productId: String): Result<ProductCustomization> = withContext(Dispatchers.IO) {
        val cache = menuCache
        val localCustomization = cache?.getCustomization(productId)
        if (localCustomization != null) {
            _customizations.value = _customizations.value + (productId to localCustomization)
        }
        try {
            val response = api.getProductCustomizations(productId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let {
                    cache?.saveCustomization(it)
                    _customizations.value = _customizations.value + (productId to it)
                    return@withContext Result.success(it)
                }
            }
        } catch (_: Exception) {
        }
        if (localCustomization != null) {
            return@withContext Result.success(localCustomization)
        }
        val memoryCached = _customizations.value[productId]
        if (memoryCached != null) {
            return@withContext Result.success(memoryCached)
        }
        Result.success(defaultCustomization(productId))
    }

    suspend fun cacheProductCustomization(customization: ProductCustomization) {
        withContext(Dispatchers.IO) {
            menuCache?.saveCustomization(customization)
            _customizations.value = _customizations.value + (customization.productId to customization)
        }
    }

    private fun defaultCustomization(productId: String) = ProductCustomization(
        productId = productId,
        addOns = emptyList(),
        toppings = emptyList(),
        allergies = emptyList(),
        meatWellness = emptyList(),
        spiceLevels = listOf("Mild", "Medium", "Spicy", "Extra Hot")
    )

    suspend fun addAllergy(name: String): Result<CustomizationOption> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(Exception("Allergy name is required"))
        }
        try {
            val response = api.addAllergy(trimmed)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data
                val id = data?.get("id").orEmpty().ifBlank { "custom-${System.currentTimeMillis()}" }
                val savedName = data?.get("name").orEmpty().ifBlank { trimmed }
                return@withContext Result.success(CustomizationOption(id, savedName, 0.0))
            }
            val err = response.body()?.response?.error
            if (!err.isNullOrBlank()) {
                return@withContext Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        Result.failure(Exception("Failed to add allergy"))
    }

    suspend fun addItemToOrder(
        orderId: String,
        guestId: Int,
        productId: String,
        quantity: Int,
        spiceLevel: String?,
        meatWellness: String?,
        allergies: List<String>?,
        customAllergies: String?,
        addOns: List<String>?,
        toppings: List<String>?,
        onionFlag: Boolean,
        garlicFlag: Boolean,
        specialInstructions: String?
    ): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        val productCheck = _menuItems.value.find { it.id == productId }
        // POS Settings.overselling: 0 = strict (block), 1 = soft allow — from branding
        val overselling = _branding.value.overselling ?: 1
        val strict = overselling == 0 || (_branding.value.strictStock ?: 0) == 1
        if (strict && productCheck?.inStock == false) {
            return@withContext Result.failure(Exception("Out of stock"))
        }
        val meatForApi = if (productCheck?.vegType.equals("non-veg", ignoreCase = true)) meatWellness else null
        try {
            val response = api.addItem(
                orderId = orderId,
                guestId = guestId,
                productId = productId,
                quantity = quantity,
                spiceLevel = spiceLevel,
                meatWellness = meatForApi,
                allergies = allergies?.joinToString(","),
                customAllergies = customAllergies,
                addOns = addOns?.joinToString(","),
                toppings = toppings?.joinToString(","),
                onionFlag = if (onionFlag) 1 else 0,
                garlicFlag = if (garlicFlag) 1 else 0,
                specialInstructions = specialInstructions
            )
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let {
                    val normalized = updateLocalOrder(it)
                    return@withContext Result.success(normalized)
                }
            }
            val apiErr = response.body()?.response?.error
                ?: response.errorBody()?.string()?.let { raw ->
                    Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
                }
            if (!apiErr.isNullOrBlank()) {
                return@withContext Result.failure(Exception(apiErr))
            }
        } catch (e: Exception) {
            // fallback offline
        }
        if (strict && (productCheck?.inStock == false || (productCheck?.stockQty != null && productCheck.stockQty < quantity))) {
            return@withContext Result.failure(Exception("Out of stock"))
        }
        // Fallback local memory state modification
        val cleanTableId = orderId.removePrefix("ORD-").split("-").firstOrNull()
        val order = _orders.value.find { 
            it.orderId == orderId || (cleanTableId != null && it.tableId == cleanTableId)
        } ?: _orders.value.firstOrNull() ?: return@withContext Result.failure(Exception("Order not found"))

        val product = _menuItems.value.find { it.id == productId }
        val newItem = OrderItem(
            id = "ITEM-${System.currentTimeMillis() % 10000}",
            productId = productId,
            productName = product?.name ?: "Custom Item",
            price = product?.price ?: 12.00,
            quantity = quantity,
            vegType = product?.vegType ?: "veg",
            spiceLevel = spiceLevel,
            meatWellness = meatForApi,
            allergies = allergies,
            customAllergies = customAllergies,
            addOns = addOns,
            toppings = toppings,
            onionFlag = onionFlag,
            garlicFlag = garlicFlag,
            specialInstructions = specialInstructions,
            status = "pending"
        )

        // Ensure guest with guestId exists in order
        val existingGuest = order.guests.find { it.guestId == guestId }
        val baseGuests = if (existingGuest == null) {
            val name = if (guestId == 0) "Table Items (All Guests)" else "Guest $guestId"
            order.guests + GuestOrder(guestId = guestId, guestName = name, items = emptyList())
        } else {
            order.guests
        }

        val updatedGuests = baseGuests.map { g ->
            if (g.guestId == guestId) {
                g.copy(items = g.items + newItem)
            } else g
        }
        val grandTotal = updatedGuests.flatMap { it.items }.sumOf { it.price * it.quantity }
        val totalItems = updatedGuests.flatMap { it.items }.sumOf { it.quantity }
        val updatedOrder = order.copy(
            guests = updatedGuests,
            grandTotal = grandTotal,
            totalItems = totalItems
        )
        updateLocalOrder(updatedOrder)
        syncManager?.enqueueAction(
            orderId, "ADD_ITEM", mapOf(
                "orderId" to orderId,
                "guestId" to guestId,
                "productId" to productId,
                "quantity" to quantity,
                "localItemId" to newItem.id,
                "spiceLevel" to spiceLevel,
                "meatWellness" to meatForApi,
                "allergies" to allergies?.joinToString(","),
                "customAllergies" to customAllergies,
                "addOns" to addOns?.joinToString(","),
                "toppings" to toppings?.joinToString(","),
                "onionFlag" to if (onionFlag) 1 else 0,
                "garlicFlag" to if (garlicFlag) 1 else 0,
                "specialInstructions" to specialInstructions
            )
        )
        Result.success(updatedOrder)
    }

    suspend fun updateItemQuantity(orderId: String, itemId: String, newQty: Int): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        val order = _orders.value.find { it.orderId == orderId }
        val target = order?.guests?.flatMap { it.items }?.find { it.id == itemId }
        val st = target?.status?.lowercase()?.trim().orEmpty()
        if (st in listOf("cancelled", "canceled")) {
            return@withContext Result.failure(Exception("Item already cancelled"))
        }
        // Ready/Served: allow cancel (qty→0), block qty edits
        if (st in listOf("ready", "served") && newQty > 0) {
            return@withContext Result.failure(
                Exception("Item already $st. Decrease to 0 to cancel with kitchen.")
            )
        }
        try {
            val response = if (newQty <= 0) api.deleteItem(itemId) else api.updateItem(itemId, newQty)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let {
                    val normalized = updateLocalOrder(it)
                    return@withContext Result.success(normalized)
                }
            }
            val err = response.body()?.response?.error
            if (!err.isNullOrBlank()) {
                return@withContext Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            // fallback offline only for pending
        }
        if (st !in listOf("", "pending") && syncManager?.isOnline?.value == true) {
            return@withContext Result.failure(Exception("Could not update item on server"))
        }
        val current = _orders.value.find { it.orderId == orderId }
            ?: return@withContext Result.failure(Exception("Order not found"))
        val updatedGuests = current.guests.map { g ->
            val updatedItems = if (newQty <= 0) {
                g.items.filterNot { it.id == itemId }
            } else {
                g.items.map { item -> if (item.id == itemId) item.copy(quantity = newQty) else item }
            }
            g.copy(items = updatedItems)
        }
        val grandTotal = updatedGuests.flatMap { it.items }.sumOf { it.price * it.quantity }
        val totalItems = updatedGuests.flatMap { it.items }.sumOf { it.quantity }
        val updatedOrder = current.copy(
            guests = updatedGuests,
            grandTotal = grandTotal,
            totalItems = totalItems
        )
        val normalized = updateLocalOrder(updatedOrder)
        syncManager?.enqueueAction(orderId, "UPDATE_QTY", mapOf("itemId" to itemId, "newQty" to newQty))
        Result.success(normalized)
    }

    /** Full customize edit for pending/kot items (spice, meat, allergies, add-ons, toppings, notes). */
    suspend fun updateItemDetails(
        orderId: String,
        itemId: String,
        quantity: Int,
        spiceLevel: String?,
        meatWellness: String?,
        allergies: List<String>?,
        addOns: List<String>?,
        toppings: List<String>?,
        onionFlag: Boolean,
        garlicFlag: Boolean,
        specialInstructions: String?
    ): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        val order = _orders.value.find { it.orderId == orderId }
        val target = order?.guests?.flatMap { it.items }?.find { it.id == itemId }
        val st = target?.status?.lowercase()?.trim().orEmpty()
        if (st in listOf("cancelled", "canceled")) {
            return@withContext Result.failure(Exception("Item already cancelled"))
        }
        if (st in listOf("ready", "served")) {
            return@withContext Result.failure(Exception("Item already $st — cannot edit. Cancel and re-add."))
        }
        try {
            val response = api.updateItem(
                itemId = itemId,
                quantity = quantity.coerceAtLeast(1),
                spiceLevel = spiceLevel.orEmpty(),
                meatWellness = meatWellness.orEmpty(),
                allergies = allergies?.joinToString(",").orEmpty(),
                customAllergies = null,
                addOns = addOns?.joinToString(",").orEmpty(),
                toppings = toppings?.joinToString(",").orEmpty(),
                onionFlag = if (onionFlag) 1 else 0,
                garlicFlag = if (garlicFlag) 1 else 0,
                specialInstructions = specialInstructions.orEmpty()
            )
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let {
                    return@withContext Result.success(updateLocalOrder(it))
                }
            }
            val err = response.body()?.response?.error
            if (!err.isNullOrBlank()) {
                return@withContext Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            // offline local patch
        }
        val current = _orders.value.find { it.orderId == orderId }
            ?: return@withContext Result.failure(Exception("Order not found"))
        val updatedGuests = current.guests.map { g ->
            g.copy(items = g.items.map { item ->
                if (item.id != itemId) item
                else item.copy(
                    quantity = quantity.coerceAtLeast(1),
                    spiceLevel = spiceLevel,
                    meatWellness = meatWellness,
                    allergies = allergies,
                    addOns = addOns,
                    toppings = toppings,
                    onionFlag = onionFlag,
                    garlicFlag = garlicFlag,
                    specialInstructions = specialInstructions
                )
            })
        }
        val grandTotal = updatedGuests.flatMap { it.items }.sumOf { it.price * it.quantity }
        val totalItems = updatedGuests.flatMap { it.items }.sumOf { it.quantity }
        val updatedOrder = current.copy(
            guests = updatedGuests,
            grandTotal = grandTotal,
            totalItems = totalItems
        )
        val normalized = updateLocalOrder(updatedOrder)
        syncManager?.enqueueAction(
            orderId, "UPDATE_ITEM", mapOf(
                "itemId" to itemId,
                "quantity" to quantity.coerceAtLeast(1),
                "spiceLevel" to spiceLevel,
                "meatWellness" to meatWellness,
                "allergies" to allergies?.joinToString(","),
                "addOns" to addOns?.joinToString(","),
                "toppings" to toppings?.joinToString(","),
                "onionFlag" to if (onionFlag) 1 else 0,
                "garlicFlag" to if (garlicFlag) 1 else 0,
                "specialInstructions" to specialInstructions
            )
        )
        Result.success(normalized)
    }

    suspend fun sendKot(orderId: String): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateKotStatus(orderId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let {
                    val normalized = updateLocalOrder(it)
                    return@withContext Result.success(normalized)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        val order = _orders.value.find { it.orderId == orderId }
            ?: return@withContext Result.failure(Exception("Order not found"))
        val hasPending = order.guests.flatMap { it.items }.any {
            it.status.equals("pending", ignoreCase = true)
        }
        if (!hasPending) {
            // Idempotent: nothing to fire
            return@withContext Result.success(updateLocalOrder(order))
        }
        val updatedGuests = order.guests.map { g ->
            g.copy(items = g.items.map { item ->
                if (item.status.equals("pending", ignoreCase = true)) {
                    item.copy(status = "kot")
                } else {
                    item
                }
            })
        }
        val updatedOrder = order.copy(status = "kot_sent", guests = updatedGuests)
        val normalized = updateLocalOrder(updatedOrder)
        if (order.tableId != null) {
            updateTableStatus(order.tableId, "order-placed", normalized.guestCount, orderId)
        }
        syncManager?.enqueueAction(orderId, "SEND_KOT", mapOf("orderId" to orderId))
        Result.success(normalized)
    }

    suspend fun markOrderServed(orderId: String): Result<OrderBootstrap> = withContext(Dispatchers.IO) {
        try {
            val response = api.markServed(orderId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let {
                    val normalized = updateLocalOrder(it)
                    return@withContext Result.success(normalized)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        val order = _orders.value.find { it.orderId == orderId } ?: return@withContext Result.failure(Exception("Order not found"))
        val updatedGuests = order.guests.map { g ->
            g.copy(items = g.items.map { item ->
                if (item.status.equals("ready", ignoreCase = true)) {
                    item.copy(status = "served")
                } else {
                    item
                }
            })
        }
        val updatedOrder = order.copy(status = "served", guests = updatedGuests)
        val normalized = updateLocalOrder(updatedOrder)
        if (order.tableId != null) {
            updateTableStatus(order.tableId, "served", normalized.guestCount, orderId)
        }
        Result.success(normalized)
    }

    suspend fun finalizeOrder(orderId: String): Result<FinalizeOrderResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.finalizeOrder(orderId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                response.body()?.data?.let { data ->
                    orderCache?.deleteOrder(orderId)
                    val order = _orders.value.find { it.orderId == orderId }
                    if (order?.tableId != null) {
                        updateTableStatus(order.tableId, "available", 0, null)
                        _orders.value = _orders.value.filterNot { it.tableId == order.tableId || it.orderId == orderId }
                    } else {
                        _orders.value = _orders.value.filterNot { it.orderId == orderId }
                    }
                    return@withContext Result.success(data)
                }
            }
        } catch (e: Exception) {
            // fallback offline
        }
        val order = _orders.value.find { it.orderId == orderId }
        val grandTotal = order?.grandTotal ?: 0.0
        val saleId = "SALE-${System.currentTimeMillis() % 100000}"
        if (order != null) {
            updateLocalOrder(order.copy(status = "finalized"))
        }
        if (order?.tableId != null) {
            updateTableStatus(order.tableId, "available", 0, null)
            _orders.value = _orders.value.filterNot { it.tableId == order.tableId || it.orderId == orderId }
        } else {
            _orders.value = _orders.value.filterNot { it.orderId == orderId }
        }

        syncManager?.enqueueAction(orderId, "FINALIZE_ORDER", mapOf("orderId" to orderId))

        Result.success(FinalizeOrderResponse(
            saleId = saleId,
            invoiceUrl = "http://localhost/invoice/$saleId.pdf",
            grandTotal = grandTotal
        ))
    }

    suspend fun freeTable(tableId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.freeTable(tableId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                orderCache?.deleteOrdersForTable(tableId)
                _orders.value = _orders.value.filterNot { it.tableId == tableId }
                updateTableStatus(tableId, "free", 0, null)
                return@withContext Result.success(true)
            }
        } catch (e: Exception) {
            // fallback
        }
        _orders.value = _orders.value.filterNot { it.tableId == tableId }
        updateTableStatus(tableId, "free", 0, null)
        syncManager?.enqueueAction("TABLE-$tableId", "FREE_TABLE", mapOf("tableId" to tableId))
        Result.success(true)
    }

    suspend fun markAvailable(tableId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.markAvailable(tableId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                updateTableStatus(tableId, "available", 0, null)
                return@withContext Result.success(true)
            }
            val err = response.body()?.response?.error ?: "Failed to mark available"
            return@withContext Result.failure(Exception(err))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun reserveTable(
        tableId: String,
        reservedBy: String,
        reservedUntil: String,
        reservedNote: String? = null,
        updateExisting: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val applyLocal = {
            updateTableStatus(
                tableId = tableId,
                newStatus = "reserved",
                guests = 0,
                orderId = null,
                reservedBy = reservedBy,
                reservedUntil = reservedUntil,
                reservedNote = reservedNote,
                clearReservation = false
            )
        }
        val enqueueOffline = suspend {
            applyLocal()
            syncManager?.enqueueAction(
                "TABLE-$tableId",
                "RESERVE_TABLE",
                mapOf(
                    "tableId" to tableId,
                    "reservedBy" to reservedBy,
                    "reservedUntil" to reservedUntil,
                    "reservedNote" to reservedNote,
                    "updateExisting" to if (updateExisting) 1 else 0
                )
            )
            Result.success(true)
        }

        if (syncManager?.isOnline?.value == false) {
            return@withContext enqueueOffline()
        }

        try {
            val response = api.reserveTable(
                tableId = tableId,
                reservedBy = reservedBy,
                reservedUntil = reservedUntil,
                reservedNote = reservedNote,
                updateExisting = if (updateExisting) 1 else 0
            )
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                applyLocal()
                return@withContext Result.success(true)
            }
            val err = response.body()?.response?.error
                ?: response.errorBody()?.string()?.let { raw ->
                    Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
                }
                ?: "Failed to reserve table"
            return@withContext Result.failure(Exception(err))
        } catch (e: Exception) {
            enqueueOffline()
        }
    }

    suspend fun unreserveTable(tableId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val applyLocal = {
            updateTableStatus(
                tableId = tableId,
                newStatus = "available",
                guests = 0,
                orderId = null,
                reservedBy = null,
                reservedUntil = null,
                reservedNote = null
            )
        }
        val enqueueOffline = suspend {
            applyLocal()
            syncManager?.enqueueAction(
                "TABLE-$tableId",
                "UNRESERVE_TABLE",
                mapOf("tableId" to tableId)
            )
            Result.success(true)
        }

        if (syncManager?.isOnline?.value == false) {
            return@withContext enqueueOffline()
        }

        try {
            val response = api.unreserveTable(tableId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                applyLocal()
                return@withContext Result.success(true)
            }
            val err = response.body()?.response?.error
                ?: response.errorBody()?.string()?.let { raw ->
                    Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
                }
                ?: "Failed to cancel reservation"
            return@withContext Result.failure(Exception(err))
        } catch (e: Exception) {
            enqueueOffline()
        }
    }

    suspend fun transferTable(fromTableId: String, toTableId: String): Result<TableMoveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.transferTable(fromTableId, toTableId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: TableMoveResponse(message = "Transferred")
                data.order?.let { updateLocalOrder(it) }
                _orders.value = _orders.value.map { o ->
                    if (o.tableId == fromTableId) o.copy(tableId = toTableId) else o
                }
                updateTableStatus(fromTableId, "available", 0, null)
                updateTableStatus(toTableId, "occupied", data.order?.guestCount ?: 0, data.orderId)
                return@withContext Result.success(data)
            }
            val err = response.body()?.response?.error ?: "Transfer failed"
            return@withContext Result.failure(Exception(err))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun mergeTables(fromTableId: String, toTableId: String): Result<TableMoveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.mergeTables(fromTableId, toTableId)
            if (response.isSuccessful && response.body()?.response?.status == "SUCCESS") {
                val data = response.body()?.data ?: TableMoveResponse(message = "Merged")
                data.order?.let { updateLocalOrder(it) }
                _orders.value = _orders.value.filterNot { it.tableId == fromTableId }
                updateTableStatus(fromTableId, "available", 0, null)
                updateTableStatus(toTableId, "occupied", data.order?.guestCount ?: 0, data.orderId)
                return@withContext Result.success(data)
            }
            val err = response.body()?.response?.error ?: "Merge failed"
            return@withContext Result.failure(Exception(err))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    private fun updateLocalOrder(order: OrderBootstrap): OrderBootstrap {
        val individualGuests = order.guests.filter { it.guestId != 0 }
        val normalizedGuestCount = maxOf(order.guestCount, individualGuests.size, 1)

        val tableItemsGuest = order.guests.find { it.guestId == 0 } ?: GuestOrder(guestId = 0, guestName = "Table Items (All Guests)", items = emptyList())
        val guaranteedGuests = (1..normalizedGuestCount).map { gId ->
            individualGuests.find { it.guestId == gId } ?: GuestOrder(guestId = gId, guestName = "Guest $gId")
        }
        val fullGuestsList = listOf(tableItemsGuest) + guaranteedGuests

        val normalizedOrder = order.copy(
            guestCount = normalizedGuestCount,
            guests = fullGuestsList
        )

        if (!normalizedOrder.tableId.isNullOrBlank()) {
            val statusToSet = when (normalizedOrder.status.lowercase()) {
                "kot_sent", "kot sent", "order-placed", "order placed" -> "order-placed"
                "ready", "prepared", "kitchen_ready" -> "ready"
                "served", "food_served" -> "served"
                "finalized", "completed" -> "available"
                else -> "occupied"
            }
            updateTableStatus(normalizedOrder.tableId, statusToSet, normalizedOrder.guestCount, normalizedOrder.orderId)
        }

        val list = _orders.value.toMutableList()
        val index = list.indexOfFirst { 
            (!it.orderId.isNullOrBlank() && it.orderId == normalizedOrder.orderId) || 
            (!it.tableId.isNullOrBlank() && it.tableId == normalizedOrder.tableId)
        }
        if (index >= 0) list[index] = normalizedOrder else list.add(normalizedOrder)
        _orders.value = list
        repoScope.launch {
            orderCache?.saveOrder(normalizedOrder)
        }
        return normalizedOrder
    }

    private fun updateTableStatus(
        tableId: String,
        newStatus: String,
        guests: Int = 0,
        orderId: String? = null,
        reservedBy: String? = null,
        reservedUntil: String? = null,
        reservedNote: String? = null,
        clearReservation: Boolean = newStatus != "reserved"
    ) {
        var updatedRow: TableItem? = null
        _tables.value = _tables.value.map { t ->
            if (t.id == tableId) {
                val next = t.copy(
                    status = newStatus,
                    guestsCount = guests,
                    orderId = orderId,
                    reservedBy = if (clearReservation && newStatus != "reserved") null else (reservedBy ?: t.reservedBy),
                    reservedUntil = if (clearReservation && newStatus != "reserved") null else (reservedUntil ?: t.reservedUntil),
                    reservedNote = if (clearReservation && newStatus != "reserved") null else (reservedNote ?: t.reservedNote)
                )
                updatedRow = next
                next
            } else {
                t
            }
        }
        updatedRow?.let { row ->
            repoScope.launch {
                menuCache?.upsertTable(row)
            }
        }
    }

    // Default Initial Data Providers
    private fun getInitialSections() = listOf(
        Section("1", "Main Dining", 3),
        Section("2", "AC Section", 2),
        Section("3", "Bar & Terrace", 2),
        Section("4", "Outdoor Lawn", 0)
    )

    private fun getInitialSubsections() = listOf(
        Subsection("101", "1", "Hall A"),
        Subsection("102", "1", "Hall B"),
        Subsection("103", "1", "Patio"),
        Subsection("201", "2", "Family AC"),
        Subsection("202", "2", "VIP Lounge"),
        Subsection("301", "3", "Rooftop Terrace"),
        Subsection("302", "3", "Sports Bar")
    )

    private fun getInitialTables() = listOf(
        TableItem("1", "T-01", "1", "101", "available", 0),
        TableItem("2", "T-02", "1", "101", "occupied", 4, "12:30 PM", "ORD-101"),
        TableItem("3", "T-03", "1", "101", "reserved", 0, null, null),
        TableItem("4", "T-04", "1", "101", "order-placed", 2, "01:05 PM", "ORD-102"),
        TableItem("5", "T-05", "1", "101", "ready", 3, "01:15 PM", "ORD-103"),
        TableItem("6", "T-06", "1", "101", "free", 2, "01:25 PM", "ORD-104"),
        TableItem("7", "T-07", "1", "101", "served", 2, "01:30 PM", "ORD-105"),
        TableItem("8", "T-08", "1", "101", "available", 0),
        TableItem("9", "T-09", "1", "101", "available", 0),
        TableItem("10", "T-10", "1", "102", "available", 0),
        TableItem("11", "T-11", "1", "102", "occupied", 6, "12:45 PM", "ORD-106"),
        TableItem("12", "T-12", "2", "201", "available", 0)
    )

    private fun getInitialOrders() = listOf(
        OrderBootstrap(
            orderId = "ORD-101",
            tableId = "2",
            tableNumber = "T-02",
            sectionName = "Main Dining",
            subsectionName = "Hall A",
            guestCount = 4,
            status = "active",
            guests = listOf(
                GuestOrder(1, "Guest 1", listOf(
                    OrderItem("1", "P-101", "Paneer Butter Masala", 12.99, 1, "veg", "Medium", null, listOf("Nut Allergy"), null, listOf("Extra Butter"), listOf("Coriander"), false, false, "Less oily", "pending"),
                    OrderItem("2", "P-103", "Garlic Naan", 3.50, 2, "veg", null, null, emptyList(), null, emptyList(), emptyList(), false, false, null, "pending")
                )),
                GuestOrder(2, "Guest 2", listOf(
                    OrderItem("3", "P-102", "Chicken Tikka Masala", 14.99, 1, "non-veg", "Hot", "Well Done", emptyList(), null, emptyList(), emptyList(), false, true, "No Garlic", "pending")
                )),
                GuestOrder(3, "Guest 3", emptyList()),
                GuestOrder(4, "Guest 4", emptyList())
            ),
            totalItems = 4,
            grandTotal = 34.98
        )
    )

    private fun getInitialCategories() = listOf(
        MenuCategory("cat-1", "Starters & Appetizers"),
        MenuCategory("cat-2", "Main Course"),
        MenuCategory("cat-3", "Breads & Rice"),
        MenuCategory("cat-4", "Beverages"),
        MenuCategory("cat-5", "Desserts")
    )

    private fun getInitialMenuItems() = listOf(
        MenuItem("P-101", "Paneer Butter Masala", "cat-2", 12.99, "veg", "https://images.unsplash.com/photo-1631452180519-c014fe946bc7", "Rich cottage cheese cooked in creamy tomato butter gravy."),
        MenuItem("P-102", "Chicken Tikka Masala", "cat-2", 14.99, "non-veg", "https://images.unsplash.com/photo-1565557623262-b51c2513a641", "Roasted chicken chunks in spiced curry sauce."),
        MenuItem("P-103", "Garlic Naan", "cat-3", 3.50, "veg", "https://images.unsplash.com/photo-1601050690597-df0568f70950", "Traditional oven baked Indian bread garnished with minced garlic."),
        MenuItem("P-104", "Vegetable Spring Rolls", "cat-1", 8.99, "veg", "https://images.unsplash.com/photo-1544025162-d76694265947", "Crispy rolled appetizer packed with seasoned vegetables."),
        MenuItem("P-105", "Mango Lassi", "cat-4", 4.99, "veg", "https://images.unsplash.com/photo-1571006682862-39201f9d45e7", "Refreshing yogurt smoothie blended with ripe mangoes."),
        MenuItem("P-106", "Tandoori Chicken", "cat-1", 16.99, "non-veg", "https://images.unsplash.com/photo-1626777552726-4a6b54c97e46", "Bone-in chicken marinated in yogurt and Indian spices."),
        MenuItem("P-107", "Gulab Jamun", "cat-5", 5.99, "veg", "https://images.unsplash.com/photo-1605197586548-028f01b7a2d4", "Soft milk dumplings soaked in cardamom sugar syrup."),
        MenuItem("P-108", "Mutton Biryani", "cat-2", 18.99, "non-veg", "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8", "Fragrant basmati rice slow cooked with tender mutton pieces.")
    )

    private fun getInitialCustomizations() = mapOf(
        "P-101" to ProductCustomization(
            productId = "P-101",
            addOns = listOf(CustomizationOption("a1", "Extra Cottage Cheese", 2.00), CustomizationOption("a2", "Extra Butter", 1.00)),
            toppings = listOf(CustomizationOption("t1", "Fresh Coriander", 0.50), CustomizationOption("t2", "Grated Cheese", 1.20)),
            allergies = listOf(CustomizationOption("al1", "Nut Allergy", 0.0), CustomizationOption("al2", "Lactose Intolerant", 0.0)),
            meatWellness = emptyList(),
            spiceLevels = listOf("Mild", "Medium", "Hot", "Extra Hot")
        ),
        "P-102" to ProductCustomization(
            productId = "P-102",
            addOns = listOf(CustomizationOption("a1", "Extra Gravy", 1.50)),
            toppings = listOf(CustomizationOption("t1", "Sliced Ginger", 0.50)),
            allergies = listOf(CustomizationOption("al1", "Nut Allergy", 0.0)),
            meatWellness = listOf("Normal", "Well Done", "Juicy"),
            spiceLevels = listOf("Mild", "Medium", "Hot")
        )
    )
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
