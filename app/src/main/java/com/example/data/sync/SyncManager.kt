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
import kotlinx.coroutines.flow.Flow
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

    val pendingCountFlow: Flow<Int> = dao.getPendingCountFlow()
    val failedCountFlow: Flow<Int> = dao.getFailedCountFlow()
    val allActionsFlow: Flow<List<PendingSyncEntity>> = dao.getAllActionsFlow()
    val failedActionsFlow: Flow<List<PendingSyncEntity>> = dao.getFailedActionsFlow()

    /** Fired after each successfully synced queue item. */
    private var onActionSyncedListener: (suspend (SyncedAction) -> Unit)? = null

    /** Fired when network returns and pending queue drain pass finishes. */
    private var onNetworkSyncCompleteListener: (suspend () -> Unit)? = null

    fun setOnActionSyncedListener(listener: suspend (SyncedAction) -> Unit) {
        onActionSyncedListener = listener
    }

    fun setOnNetworkSyncCompleteListener(listener: suspend () -> Unit) {
        onNetworkSyncCompleteListener = listener
    }

    suspend fun getPendingCount(): Int = dao.getPendingCount()
    suspend fun getFailedCount(): Int = dao.getFailedCount()

    init {
        loadIdMaps()
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    runPendingSyncPass()
                }
            }
        }
        // Initialize periodic background worker
        try {
            SyncWorker.schedulePeriodicSync(appContext)
        } catch (_: Exception) {}
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
                    payloadJson = json,
                    status = "PENDING"
                )
                dao.insertPendingAction(entity)
                // Schedule WorkManager to guarantee sync even if app is immediately killed
                SyncWorker.scheduleImmediateSync(appContext)
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

    suspend fun retryAllFailed() {
        withContext(Dispatchers.IO) {
            dao.retryAllFailed()
            syncPendingActions()
        }
    }

    suspend fun retryAction(id: Long) {
        withContext(Dispatchers.IO) {
            dao.retryAction(id)
            syncPendingActions()
        }
    }

    suspend fun deleteAction(id: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteById(id)
        }
    }

    suspend fun clearFailedActions() {
        withContext(Dispatchers.IO) {
            dao.clearFailedActions()
        }
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
     * Resolve Table conflict by reassigning to a new table and retrying.
     */
    suspend fun reassignConflictTable(actionId: Long, newTableId: String) {
        withContext(Dispatchers.IO) {
            val item = dao.getActionById(actionId) ?: return@withContext
            try {
                @Suppress("UNCHECKED_CAST")
                val payload = (mapAdapter.fromJson(item.payloadJson) as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf()
                payload["tableId"] = newTableId
                val updatedJson = mapAdapter.toJson(payload)
                val updated = item.copy(
                    orderId = "TABLE-$newTableId",
                    payloadJson = updatedJson,
                    status = "PENDING",
                    retryCount = 0,
                    lastErrorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
                dao.updatePendingAction(updated)
                syncPendingActions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * True when this table/order still has queued offline actions not yet synced.
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
            runPendingSyncPass()
        }
    }

    /**
     * Direct execution pass used by WorkManager or Foreground triggers.
     * Returns true if all pending items synced successfully.
     */
    suspend fun runPendingSyncPassDirect(): Boolean {
        return runPendingSyncPass()
    }

    private suspend fun runPendingSyncPass(): Boolean = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext false
        _isSyncing.value = true
        var allSuccess = true
        try {
            prepareQueueForSync()
            val pendingList = dao.getAllPendingActions()
            for (item in pendingList) {
                dao.updateStatus(item.id, "SYNCING", null)
                val result = processItem(item)
                when (result) {
                    is SyncItemResult.Success -> {
                        notifyActionSynced(item)
                        dao.deletePendingAction(item)
                    }
                    is SyncItemResult.Conflict -> {
                        allSuccess = false
                        dao.updateStatus(item.id, "CONFLICT", result.message)
                    }
                    is SyncItemResult.RetryLater -> {
                        allSuccess = false
                        val nextRetry = item.retryCount + 1
                        if (nextRetry >= MAX_RETRIES) {
                            // Don't drop silently! Mark as FAILED for manual review/retry
                            dao.updateStatus(item.id, "FAILED", result.message ?: "Failed after $MAX_RETRIES retries")
                        } else {
                            val next = item.copy(
                                status = "PENDING",
                                retryCount = nextRetry,
                                lastErrorMessage = result.message,
                                updatedAt = System.currentTimeMillis()
                            )
                            dao.updatePendingAction(next)
                        }
                        break // Pause current loop to avoid spamming server while unreachable
                    }
                    is SyncItemResult.Drop -> {
                        dao.deletePendingAction(item)
                    }
                }
            }
            onNetworkSyncCompleteListener?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
            allSuccess = false
        } finally {
            _isSyncing.value = false
        }
        allSuccess
    }

    private suspend fun notifyActionSynced(item: PendingSyncEntity) {
        val payload = try {
            @Suppress("UNCHECKED_CAST")
            mapAdapter.fromJson(item.payloadJson) as? Map<String, Any?>
        } catch (_: Exception) {
            null
        } ?: emptyMap()
        val tableId = payload["tableId"]?.toString()
            ?: item.orderId.removePrefix("TABLE-").takeIf { item.orderId.startsWith("TABLE-") }
        onActionSyncedListener?.invoke(
            SyncedAction(
                actionType = item.actionType,
                orderId = resolveOrderId(item.orderId),
                tableId = tableId,
                payload = payload
            )
        )
    }

    /**
     * Long-offline hygiene:
     * - coalesce UPDATE_QTY for same itemId (keep latest only)
     * - mark actions older than MAX_AGE_MS as FAILED instead of deleting silently
     */
    private suspend fun prepareQueueForSync() {
        val now = System.currentTimeMillis()
        val all = dao.getAllPendingActions()
        val latestQtyByItem = mutableMapOf<String, Long>()
        for (row in all) {
            if (now - row.createdAt > MAX_AGE_MS) {
                dao.updateStatus(row.id, "FAILED", "Action expired after 7 days offline")
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

    private suspend fun processItem(item: PendingSyncEntity): SyncItemResult {
        return try {
            @Suppress("UNCHECKED_CAST")
            val payload = remapPayloadIds(
                (mapAdapter.fromJson(item.payloadJson) as? Map<String, Any?>) ?: return SyncItemResult.Drop("Invalid JSON payload")
            )

            when (item.actionType) {
                "CREATE_ORDER" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncItemResult.Drop("Missing tableId")
                    val guestCount = payloadInt(payload["guestCount"], 1)
                    val localOrderId = (payload["localOrderId"] as? String) ?: item.orderId
                    val res = api.createOrder(tableId, guestCount)
                    val ok = res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                    if (ok) {
                        val serverId = res.body()?.data?.orderId
                        if (!serverId.isNullOrBlank()) {
                            rewriteQueuedOrderIds(localOrderId, serverId)
                        }
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        val code = res.code()
                        if (code == 409 || err.contains("occupied", true) || err.contains("already in use", true) || err.contains("table busy", true)) {
                            SyncItemResult.Conflict("Table $tableId is already occupied on the server. Reassign table to proceed.")
                        } else {
                            SyncItemResult.RetryLater(err.ifBlank { "HTTP $code failed" })
                        }
                    }
                }
                "ADD_ITEM" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return SyncItemResult.Drop("Missing orderId"))
                    val guestId = payloadInt(payload["guestId"], 0)
                    val productId = payload["productId"] as? String ?: return SyncItemResult.Drop("Missing productId")
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
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        when {
                            err.contains("required", true) || err.contains("not found", true) -> SyncItemResult.Drop(err)
                            err.contains("out of stock", true) -> SyncItemResult.Drop("Product out of stock on server")
                            else -> SyncItemResult.RetryLater(err.ifBlank { "Add item failed" })
                        }
                    }
                }
                "UPDATE_QTY" -> {
                    val rawItemId = payload["itemId"] as? String ?: return SyncItemResult.Drop("Missing itemId")
                    val itemId = resolveItemId(rawItemId)
                    if (itemId.startsWith("ITEM-") && itemId == rawItemId && !itemIdMap.containsKey(rawItemId)) {
                        return SyncItemResult.RetryLater("Waiting for server item ID mapping")
                    }
                    val newQty = payloadInt(payload["newQty"], 1)
                    val res = if (newQty <= 0) api.deleteItem(itemId) else api.updateItem(itemId, newQty)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        if (err.contains("not found", true) || err.contains("cancelled", true)) {
                            SyncItemResult.Drop(err)
                        } else {
                            SyncItemResult.RetryLater(err.ifBlank { "Update quantity failed" })
                        }
                    }
                }
                "UPDATE_ITEM" -> {
                    val rawItemId = payload["itemId"] as? String ?: return SyncItemResult.Drop("Missing itemId")
                    val itemId = resolveItemId(rawItemId)
                    if (itemId.startsWith("ITEM-") && itemId == rawItemId && !itemIdMap.containsKey(rawItemId)) {
                        return SyncItemResult.RetryLater("Waiting for server item ID mapping")
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
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        when {
                            err.contains("not found", true) || err.contains("cancelled", true) -> SyncItemResult.Drop(err)
                            err.contains("already", true) || err.contains("ready", true) || err.contains("served", true) -> SyncItemResult.Drop(err)
                            else -> SyncItemResult.RetryLater(err.ifBlank { "Update item customization failed" })
                        }
                    }
                }
                "SEND_KOT" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return SyncItemResult.Drop("Missing orderId"))
                    val res = api.updateKotStatus(orderId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        if (err.contains("no pending", true) || err.contains("already", true)
                            || err.contains("idempotent", true) || err.isBlank()
                        ) {
                            SyncItemResult.Success
                        } else if (err.contains("not found", true)) {
                            SyncItemResult.Drop(err)
                        } else {
                            SyncItemResult.RetryLater(err.ifBlank { "Send KOT failed" })
                        }
                    }
                }
                "FINALIZE_ORDER" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return SyncItemResult.Drop("Missing orderId"))
                    val res = api.finalizeOrder(orderId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        if (err.contains("already", true) || err.contains("completed", true)
                            || err.contains("finalized", true) || err.contains("not found", true)
                        ) {
                            SyncItemResult.Drop(err)
                        } else {
                            SyncItemResult.RetryLater(err.ifBlank { "Finalize order failed" })
                        }
                    }
                }
                "FREE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncItemResult.Drop("Missing tableId")
                    val res = api.freeTable(tableId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        if (err.contains("already", true) || err.contains("available", true)
                            || err.contains("not found", true)
                        ) {
                            SyncItemResult.Drop(err)
                        } else {
                            SyncItemResult.RetryLater(err.ifBlank { "Free table failed" })
                        }
                    }
                }
                "RESERVE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncItemResult.Drop("Missing tableId")
                    val reservedBy = payload["reservedBy"] as? String ?: return SyncItemResult.Drop("Missing reservedBy")
                    val reservedUntil = payload["reservedUntil"] as? String ?: return SyncItemResult.Drop("Missing reservedUntil")
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
                        SyncItemResult.Success
                    } else {
                        val err = res.body()?.response?.error.orEmpty()
                        val code = res.code()
                        if (code == 409 || err.contains("occupied", true) || err.contains("already reserved", true)) {
                            SyncItemResult.Conflict("Table $tableId is already reserved/occupied on the server")
                        } else {
                            SyncItemResult.RetryLater(err.ifBlank { "Reserve table failed" })
                        }
                    }
                }
                "UNRESERVE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return SyncItemResult.Drop("Missing tableId")
                    val res = api.unreserveTable(tableId)
                    if (res.isSuccessful && res.body()?.response?.status == "SUCCESS") {
                        SyncItemResult.Success
                    } else {
                        SyncItemResult.Drop("Unreserve table dropped")
                    }
                }
                else -> SyncItemResult.Drop("Unknown action type")
            }
        } catch (e: Exception) {
            SyncItemResult.RetryLater(e.localizedMessage ?: "Unknown network error")
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

    private sealed class SyncItemResult {
        object Success : SyncItemResult()
        data class RetryLater(val message: String?) : SyncItemResult()
        data class Conflict(val message: String) : SyncItemResult()
        data class Drop(val reason: String) : SyncItemResult()
    }

    companion object {
        private const val PREFS_NAME = "ordertaking_sync_maps"
        private const val KEY_ORDER_MAP = "order_id_map"
        private const val KEY_ITEM_MAP = "item_id_map"
        private const val MAX_RETRIES = 8
        /** Drop queued actions older than 7 days (stale long-offline). Order JSON kept in sma_res_orders. */
        private const val MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

data class SyncedAction(
    val actionType: String,
    val orderId: String?,
    val tableId: String?,
    val payload: Map<String, Any?>
)
