package com.example.data.sync

import android.content.Context
import com.example.data.api.ApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.PendingSyncDao
import com.example.data.local.PendingSyncEntity
import com.squareup.moshi.Moshi
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
                val adapter = moshi.adapter(Map::class.java)
                val json = adapter.toJson(payloadMap)
                val entity = PendingSyncEntity(
                    orderId = orderId ?: "",
                    actionType = actionType,
                    payloadJson = json
                )
                dao.insertPendingAction(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                        // Stop sequential execution on first error to preserve order sequence
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

    private suspend fun processItem(item: PendingSyncEntity): Boolean {
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val payload = mapAdapter.fromJson(item.payloadJson) as? Map<String, Any?> ?: return true

            when (item.actionType) {
                "CREATE_ORDER" -> {
                    val tableId = payload["tableId"] as? String ?: return true
                    val guestCount = (payload["guestCount"] as? Number)?.toInt() ?: 1
                    val res = api.createOrder(tableId, guestCount)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "ADD_ITEM" -> {
                    val orderId = payload["orderId"] as? String ?: return true
                    val guestId = (payload["guestId"] as? Number)?.toInt() ?: 0
                    val productId = payload["productId"] as? String ?: return true
                    val quantity = (payload["quantity"] as? Number)?.toInt() ?: 1
                    val spiceLevel = payload["spiceLevel"] as? String
                    val meatWellness = payload["meatWellness"] as? String
                    val allergies = payload["allergies"] as? String
                    val customAllergies = payload["customAllergies"] as? String
                    val addOns = payload["addOns"] as? String
                    val toppings = payload["toppings"] as? String
                    val onionFlag = (payload["onionFlag"] as? Number)?.toInt() ?: 0
                    val garlicFlag = (payload["garlicFlag"] as? Number)?.toInt() ?: 0
                    val specialInstructions = payload["specialInstructions"] as? String

                    val res = api.addItem(
                        orderId = orderId,
                        guestId = guestId,
                        productId = productId,
                        quantity = quantity,
                        spiceLevel = spiceLevel,
                        meatWellness = meatWellness,
                        allergies = allergies,
                        customAllergies = customAllergies,
                        addOns = addOns,
                        toppings = toppings,
                        onionFlag = onionFlag,
                        garlicFlag = garlicFlag,
                        specialInstructions = specialInstructions
                    )
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "UPDATE_QTY" -> {
                    val itemId = payload["itemId"] as? String ?: return true
                    val newQty = (payload["newQty"] as? Number)?.toInt() ?: 1
                    val res = if (newQty <= 0) api.deleteItem(itemId) else api.updateItem(itemId, newQty)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "SEND_KOT" -> {
                    val orderId = payload["orderId"] as? String ?: return true
                    val res = api.updateKotStatus(orderId)
                    res.isSuccessful && res.body()?.response?.status == "SUCCESS"
                }
                "FINALIZE_ORDER" -> {
                    val orderId = payload["orderId"] as? String ?: return true
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
