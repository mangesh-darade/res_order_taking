package com.example.data.sync

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.PendingSyncDao
import com.example.data.local.PendingSyncEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val dao: PendingSyncDao = db.pendingSyncDao()
    private val networkMonitor = NetworkMonitor(appContext)
    private val api get() = ApiClient.service
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** localOrderId → serverOrderId (persisted — survives process death) */
    private val orderIdMap = mutableMapOf<String, String>()
    /** localItemId → serverItemId */
    private val itemIdMap = mutableMapOf<String, String>()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val pendingCountFlow = dao.getPendingCountFlow()

    init {
        loadIdMaps()
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    syncPendingActions()
                }
            }
        }
    }

    suspend fun enqueueAction(orderId: String?, actionType: String, payloadMap: Map<String, Any?>) {
        withContext(Dispatchers.IO) {
            try {
                val resolvedOrderId = resolveOrderId(orderId ?: "")
                val resolvedPayload = remapPayloadIds(payloadMap)
                val json = mapAdapter.toJson(resolvedPayload)
                val entity = PendingSyncEntity(
                    orderId = resolvedOrderId,
                    actionType = actionType,
                    payloadJson = json
                )
                dao.insertPendingAction(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun mapOrderId(localId: String, serverId: String) {
        if (localId.isNotBlank() && serverId.isNotBlank() && localId != serverId) {
            orderIdMap[localId] = serverId
            persistIdMaps()
        }
    }

    fun mapItemId(localId: String, serverId: String) {
        if (localId.isNotBlank() && serverId.isNotBlank() && localId != serverId) {
            itemIdMap[localId] = serverId
            persistIdMaps()
        }
    }

    fun resolveOrderId(id: String): String {
        if (id.isBlank()) return id
        return orderIdMap[id] ?: id
    }

    fun resolveItemId(id: String): String {
        if (id.isBlank()) return id
        return itemIdMap[id] ?: id
    }

    suspend fun clearPendingQueue() {
        withContext(Dispatchers.IO) {
            dao.clearAll()
            orderIdMap.clear()
            itemIdMap.clear()
            persistIdMaps()
        }
    }

    /**
     * True when this table/order still has queued offline actions not yet synced.
     * Used to avoid polling overwrite wiping local items before sync finishes.
     */
    suspend fun hasPendingForTable(tableId: String?, orderId: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            if (tableId.isNullOrBlank() && orderId.isNullOrBlank()) {
                return@withContext false
            }
            val resolvedOrderId = orderId?.let { resolveOrderId(it) }.orEmpty()
            val pendingList = dao.getAllPendingActions()
            for (item in pendingList) {
                val rowOrderId = resolveOrderId(item.orderId)
                if (resolvedOrderId.isNotBlank()) {
                    if (rowOrderId == resolvedOrderId || item.orderId == orderId) {
                        return@withContext true
                    }
                }
                if (!tableId.isNullOrBlank()) {
                    if (item.orderId == "TABLE-$tableId") {
                        return@withContext true
                    }
                    if (item.orderId.contains("-$tableId-")) {
                        return@withContext true
                    }
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val payload = mapAdapter.fromJson(item.payloadJson) as? Map<String, Any?>
                        val payloadTableId = payload?.get("tableId")?.toString()
                        if (payloadTableId == tableId) {
                            return@withContext true
                        }
                        val payloadOrderId = payload?.get("orderId")?.toString()
                        if (!payloadOrderId.isNullOrBlank() && resolvedOrderId.isNotBlank()
                            && resolveOrderId(payloadOrderId) == resolvedOrderId
                        ) {
                            return@withContext true
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            false
        }
    }

    fun syncPendingActions() {
        scope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            try {
                prepareQueueForSync()
                val pendingList = dao.getAllPendingActions()
                for (item in pendingList) {
                    val result = processItem(item)
                    when (result) {
                        SyncResult.SUCCESS -> dao.deletePendingAction(item)
                        SyncResult.RETRY_LATER -> {
                            val next = item.copy(retryCount = item.retryCount + 1)
                            dao.updatePendingAction(next)
                            if (next.retryCount >= MAX_RETRIES) {
                                dao.deletePendingAction(next)
                                continue
                            }
                            // Dependency not ready — stop this pass so order stays FIFO
                            break
                        }
                        SyncResult.DROP -> dao.deletePendingAction(item)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Long-offline hygiene:
     * - drop actions older than MAX_AGE_MS
     * - coalesce UPDATE_QTY for same itemId (keep latest only)
     */
    private suspend fun prepareQueueForSync() {
        val now = System.currentTimeMillis()
        val all = dao.getAllPendingActions()
        val latestQtyByItem = mutableMapOf<String, Long>()
        for (row in all) {
            if (now - row.createdAt > MAX_AGE_MS) {
                dao.deletePendingAction(row)
                continue
            }
            if (row.actionType == "UPDATE_QTY") {
                val payload = try {
                    @Suppress("UNCHECKED_CAST")
                    mapAdapter.fromJson(row.payloadJson) as? Map<String, Any?>
                } catch (_: Exception) {
                    null
                }
                val itemId = (payload?.get("itemId") as? String).orEmpty()
                if (itemId.isNotBlank()) {
                    val prev = latestQtyByItem[itemId]
                    if (prev != null && prev < row.id) {
                        dao.deleteById(prev)
                    } else if (prev != null && prev > row.id) {
                        dao.deletePendingAction(row)
                        continue
                    }
                    latestQtyByItem[itemId] = row.id
                }
            }
        }
    }

    private fun remapPayloadIds(payload: Map<String, Any?>): Map<String, Any?> {
        val out = payload.toMutableMap()
        (out["orderId"] as? String)?.let { out["orderId"] = resolveOrderId(it) }
        (out["itemId"] as? String)?.let { local ->
            out["itemId"] = resolveItemId(local)
        }
        return out
    }

    private suspend fun rewriteQueuedOrderIds(oldOrderId: String, newOrderId: String) {
        if (oldOrderId.isBlank() || newOrderId.isBlank() || oldOrderId == newOrderId) return
        orderIdMap[oldOrderId] = newOrderId
        persistIdMaps()
        val queued = dao.getPendingByOrderId(oldOrderId)
        for (row in queued) {
            val payload = try {
                @Suppress("UNCHECKED_CAST")
                (mapAdapter.fromJson(row.payloadJson) as? Map<String, Any?>)?.toMutableMap()
                    ?: mutableMapOf()
            } catch (_: Exception) {
                mutableMapOf()
            }
            payload["orderId"] = newOrderId
            val updated = row.copy(
                orderId = newOrderId,
                payloadJson = mapAdapter.toJson(payload)
            )
            dao.updatePendingAction(updated)
        }
        dao.remapOrderId(oldOrderId, newOrderId)
    }

    private suspend fun processItem(item: PendingSyncEntity): SyncResult {
        return try {
            @Suppress("UNCHECKED_CAST")
            val payload = remapPayloadIds(
                (mapAdapter.fromJson(item.payloadJson) as? Map<String, Any?>) ?: return SyncResult.DROP
            )

            when (item.actionType) {
                "CREATE_ORDER" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncResult.DROP
                    val guestCount = payloadInt(payload["guestCount"], 1)
                    val localOrderId = (payload["localOrderId"] as? String) ?: item.orderId
                    val res = api.createOrder(tableId, guestCount)
                    val ok = res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                    if (ok) {
                        val serverId = res.body()?.data?.orderId
                        if (!serverId.isNullOrBlank()) {
                            rewriteQueuedOrderIds(localOrderId, serverId)
                        }
                        SyncResult.SUCCESS
                    } else {
                        SyncResult.RETRY_LATER
                    }
                }
                "ADD_ITEM" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return SyncResult.DROP)
                    // Still a local ORD- id and no map → wait for CREATE_ORDER
                    if (orderId.startsWith("ORD-") && !orderIdMap.containsKey(orderId) &&
                        orderIdMap.values.none { it == orderId }
                    ) {
                        // May already be server id shaped differently; only wait if looks offline-local
                        if (orderId.contains("-") && orderId.removePrefix("ORD-").toIntOrNull() == null) {
                            // table-based local id — try API anyway
                        }
                    }
                    val guestId = payloadInt(payload["guestId"], 0)
                    val productId = payload["productId"] as? String ?: return SyncResult.DROP
                    val quantity = payloadInt(payload["quantity"], 1)
                    val localItemId = payload["localItemId"] as? String
                    val res = api.addItem(
                        orderId = orderId,
                        guestId = guestId,
                        productId = productId,
                        quantity = quantity,
                        spiceLevel = payload["spiceLevel"] as? String,
                        meatWellness = payload["meatWellness"] as? String,
                        allergies = payload["allergies"] as? String,
                        customAllergies = payload["customAllergies"] as? String,
                        addOns = payload["addOns"] as? String,
                        toppings = payload["toppings"] as? String,
                        onionFlag = payloadInt(payload["onionFlag"], 0),
                        garlicFlag = payloadInt(payload["garlicFlag"], 0),
                        specialInstructions = payload["specialInstructions"] as? String
                    )
                    val ok = res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                    if (ok) {
                        if (!localItemId.isNullOrBlank()) {
                            val serverItem = res.body()?.data?.guests
                                ?.flatMap { it.items }
                                ?.lastOrNull { it.productId == productId }
                            if (serverItem != null && serverItem.id.isNotBlank()) {
                                mapItemId(localItemId, serverItem.id)
                            }
                        }
                        val serverOrderId = res.body()?.data?.orderId
                        if (!serverOrderId.isNullOrBlank() && serverOrderId != orderId) {
                            rewriteQueuedOrderIds(orderId, serverOrderId)
                        }
                        SyncResult.SUCCESS
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        when {
                            err.contains("required", true) || err.contains("not found", true) -> SyncResult.DROP
                            err.contains("out of stock", true) -> SyncResult.DROP
                            else -> SyncResult.RETRY_LATER
                        }
                    }
                }
                "UPDATE_QTY" -> {
                    val rawItemId = payload["itemId"] as? String ?: return SyncResult.DROP
                    val itemId = resolveItemId(rawItemId)
                    // Still offline-local id → wait until ADD_ITEM maps it
                    if (itemId.startsWith("ITEM-") && itemId == rawItemId && !itemIdMap.containsKey(rawItemId)) {
                        return SyncResult.RETRY_LATER
                    }
                    val newQty = payloadInt(payload["newQty"], 1)
                    val res = if (newQty <= 0) api.deleteItem(itemId) else api.updateItem(itemId, newQty)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncResult.SUCCESS
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        if (err.contains("not found", true) || err.contains("cancelled", true)) {
                            SyncResult.DROP
                        } else {
                            SyncResult.RETRY_LATER
                        }
                    }
                }
                "UPDATE_ITEM" -> {
                    val rawItemId = payload["itemId"] as? String ?: return SyncResult.DROP
                    val itemId = resolveItemId(rawItemId)
                    if (itemId.startsWith("ITEM-") && itemId == rawItemId && !itemIdMap.containsKey(rawItemId)) {
                        return SyncResult.RETRY_LATER
                    }
                    val res = api.updateItem(
                        itemId = itemId,
                        quantity = payloadInt(payload["quantity"], 1).coerceAtLeast(1),
                        spiceLevel = payload["spiceLevel"] as? String,
                        meatWellness = payload["meatWellness"] as? String,
                        allergies = payload["allergies"] as? String,
                        addOns = payload["addOns"] as? String,
                        toppings = payload["toppings"] as? String,
                        onionFlag = payloadInt(payload["onionFlag"], 0),
                        garlicFlag = payloadInt(payload["garlicFlag"], 0),
                        specialInstructions = payload["specialInstructions"] as? String
                    )
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncResult.SUCCESS
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        when {
                            err.contains("not found", true) || err.contains("cancelled", true) -> SyncResult.DROP
                            err.contains("already", true) || err.contains("ready", true) || err.contains("served", true) -> SyncResult.DROP
                            else -> SyncResult.RETRY_LATER
                        }
                    }
                }
                "SEND_KOT" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return SyncResult.DROP)
                    val res = api.updateKotStatus(orderId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncResult.SUCCESS
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        // Already sent / no pending — treat as done (multi-device / double tap)
                        if (err.contains("no pending", true) || err.contains("already", true)
                            || err.contains("idempotent", true) || err.isBlank()
                        ) {
                            SyncResult.SUCCESS
                        } else if (err.contains("not found", true)) {
                            SyncResult.DROP
                        } else {
                            SyncResult.RETRY_LATER
                        }
                    }
                }
                "FINALIZE_ORDER" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return SyncResult.DROP)
                    val res = api.finalizeOrder(orderId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncResult.SUCCESS
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        // Another device already billed / completed
                        if (err.contains("already", true) || err.contains("completed", true)
                            || err.contains("finalized", true) || err.contains("not found", true)
                        ) {
                            SyncResult.DROP
                        } else {
                            SyncResult.RETRY_LATER
                        }
                    }
                }
                "FREE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncResult.DROP
                    val res = api.freeTable(tableId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncResult.SUCCESS
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        if (err.contains("already", true) || err.contains("available", true)
                            || err.contains("not found", true)
                        ) {
                            SyncResult.DROP
                        } else {
                            SyncResult.RETRY_LATER
                        }
                    }
                }
                "RESERVE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncResult.DROP
                    val reservedBy = payload["reservedBy"] as? String ?: return SyncResult.DROP
                    val reservedUntil = payload["reservedUntil"] as? String ?: return SyncResult.DROP
                    val reservedNote = payload["reservedNote"] as? String
                    val updateExisting = payloadInt(payload["updateExisting"], 0)
                    val res = api.reserveTable(
                        tableId = tableId,
                        reservedBy = reservedBy,
                        reservedUntil = reservedUntil,
                        reservedNote = reservedNote,
                        updateExisting = updateExisting
                    )
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncResult.SUCCESS
                    } else {
                        SyncResult.RETRY_LATER
                    }
                }
                "UNRESERVE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncResult.DROP
                    val res = api.unreserveTable(tableId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncResult.SUCCESS
                    } else {
                        SyncResult.DROP
                    }
                }
                else -> SyncResult.DROP
            }
        } catch (e: Exception) {
            SyncResult.RETRY_LATER
        }
    }

    private fun payloadInt(value: Any?, default: Int): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }

    private fun loadIdMaps() {
        try {
            val orderJson = prefs.getString(KEY_ORDER_MAP, null)
            if (!orderJson.isNullOrBlank()) {
                @Suppress("UNCHECKED_CAST")
                val map = mapAdapter.fromJson(orderJson) as? Map<String, Any?>
                map?.forEach { (k, v) ->
                    val s = v?.toString()
                    if (!s.isNullOrBlank()) orderIdMap[k] = s
                }
            }
            val itemJson = prefs.getString(KEY_ITEM_MAP, null)
            if (!itemJson.isNullOrBlank()) {
                @Suppress("UNCHECKED_CAST")
                val map = mapAdapter.fromJson(itemJson) as? Map<String, Any?>
                map?.forEach { (k, v) ->
                    val s = v?.toString()
                    if (!s.isNullOrBlank()) itemIdMap[k] = s
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun persistIdMaps() {
        try {
            prefs.edit()
                .putString(KEY_ORDER_MAP, mapAdapter.toJson(orderIdMap.toMap()))
                .putString(KEY_ITEM_MAP, mapAdapter.toJson(itemIdMap.toMap()))
                .apply()
        } catch (_: Exception) {
        }
    }

    private enum class SyncResult {
        SUCCESS,
        RETRY_LATER,
        DROP
    }

    companion object {
        private const val PREFS_NAME = "ordertaking_sync_maps"
        private const val KEY_ORDER_MAP = "order_id_map"
        private const val KEY_ITEM_MAP = "item_id_map"
        private const val MAX_RETRIES = 8
        /** Drop queued actions older than 72h (stale long-offline). */
        private const val MAX_AGE_MS = 72L * 60L * 60L * 1000L

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
