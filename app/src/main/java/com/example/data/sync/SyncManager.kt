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

    private val db = AppDatabase.getDatabase(context)
    private val dao: PendingSyncDao = db.pendingSyncDao()
    private val networkMonitor = NetworkMonitor(context)
    private val api get() = ApiClient.service
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** localOrderId → serverOrderId */
    private val orderIdMap = mutableMapOf<String, String>()
    /** localItemId → serverItemId */
    private val itemIdMap = mutableMapOf<String, String>()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val pendingCountFlow = dao.getPendingCountFlow()

    init {
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
        }
    }

    fun resolveOrderId(id: String): String {
        if (id.isBlank()) return id
        return orderIdMap[id] ?: id
    }

    suspend fun clearPendingQueue() {
        withContext(Dispatchers.IO) {
            dao.clearAll()
        }
    }

    fun syncPendingActions() {
        scope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            try {
                val pendingList = dao.getAllPendingActions()
                for (item in pendingList) {
                    val success = processItem(item)
                    if (success) {
                        dao.deletePendingAction(item)
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun remapPayloadIds(payload: Map<String, Any?>): Map<String, Any?> {
        val out = payload.toMutableMap()
        (out["orderId"] as? String)?.let { out["orderId"] = resolveOrderId(it) }
        (out["itemId"] as? String)?.let { local ->
            out["itemId"] = itemIdMap[local] ?: local
        }
        return out
    }

    private suspend fun rewriteQueuedOrderIds(oldOrderId: String, newOrderId: String) {
        if (oldOrderId.isBlank() || newOrderId.isBlank() || oldOrderId == newOrderId) return
        orderIdMap[oldOrderId] = newOrderId
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

    private suspend fun processItem(item: PendingSyncEntity): Boolean {
        return try {
            @Suppress("UNCHECKED_CAST")
            val payload = remapPayloadIds(
                (mapAdapter.fromJson(item.payloadJson) as? Map<String, Any?>) ?: return true
            )

            when (item.actionType) {
                "CREATE_ORDER" -> {
                    val tableId = payload["tableId"] as? String ?: return true
                    val guestCount = (payload["guestCount"] as? Number)?.toInt() ?: 1
                    val localOrderId = (payload["localOrderId"] as? String)
                        ?: item.orderId
                    val res = api.createOrder(tableId, guestCount)
                    val ok = res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                    if (ok) {
                        val serverId = res.body()?.data?.orderId
                        if (!serverId.isNullOrBlank()) {
                            rewriteQueuedOrderIds(localOrderId, serverId)
                        }
                    }
                    ok
                }
                "ADD_ITEM" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return true)
                    val guestId = (payload["guestId"] as? Number)?.toInt() ?: 0
                    val productId = payload["productId"] as? String ?: return true
                    val quantity = (payload["quantity"] as? Number)?.toInt() ?: 1
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
                        onionFlag = (payload["onionFlag"] as? Number)?.toInt() ?: 0,
                        garlicFlag = (payload["garlicFlag"] as? Number)?.toInt() ?: 0,
                        specialInstructions = payload["specialInstructions"] as? String
                    )
                    val ok = res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                    if (ok && !localItemId.isNullOrBlank()) {
                        val serverItem = res.body()?.data?.guests
                            ?.flatMap { it.items }
                            ?.lastOrNull { it.productId == productId }
                        if (serverItem != null && serverItem.id.isNotBlank()) {
                            itemIdMap[localItemId] = serverItem.id
                        }
                        val serverOrderId = res.body()?.data?.orderId
                        if (!serverOrderId.isNullOrBlank() && serverOrderId != orderId) {
                            rewriteQueuedOrderIds(orderId, serverOrderId)
                        }
                    }
                    ok
                }
                "UPDATE_QTY" -> {
                    val itemId = itemIdMap[payload["itemId"] as? String]
                        ?: (payload["itemId"] as? String)
                        ?: return true
                    val newQty = (payload["newQty"] as? Number)?.toInt() ?: 1
                    val res = if (newQty <= 0) api.deleteItem(itemId) else api.updateItem(itemId, newQty)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "SEND_KOT" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return true)
                    val res = api.updateKotStatus(orderId)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "FINALIZE_ORDER" -> {
                    val orderId = resolveOrderId(payload["orderId"] as? String ?: return true)
                    val res = api.finalizeOrder(orderId)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "FREE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return true
                    val res = api.freeTable(tableId)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "RESERVE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return true
                    val reservedBy = payload["reservedBy"] as? String ?: return true
                    val reservedUntil = payload["reservedUntil"] as? String ?: return true
                    val reservedNote = payload["reservedNote"] as? String
                    val updateExisting = (payload["updateExisting"] as? Number)?.toInt() ?: 0
                    val res = api.reserveTable(
                        tableId = tableId,
                        reservedBy = reservedBy,
                        reservedUntil = reservedUntil,
                        reservedNote = reservedNote,
                        updateExisting = updateExisting
                    )
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "UNRESERVE_TABLE" -> {
                    val tableId = payload["tableId"] as? String ?: return true
                    val res = api.unreserveTable(tableId)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                else -> true
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
