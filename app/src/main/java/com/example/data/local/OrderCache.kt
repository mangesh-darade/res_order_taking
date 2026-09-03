package com.example.data.local

import android.content.Context
import com.example.data.model.OrderBootstrap
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class OrderCache(context: Context) {

    private val dao = AppDatabase.getDatabase(context.applicationContext).orderCacheDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val orderAdapter = moshi.adapter(OrderBootstrap::class.java)

    suspend fun getActiveOrders(): List<OrderBootstrap> {
        return dao.getAllOrders()
            .mapNotNull { decode(it.payloadJson) }
            .filter { isActiveStatus(it.status) }
    }

    suspend fun getOrder(orderId: String): OrderBootstrap? {
        val row = dao.getById(orderId) ?: return null
        return decode(row.payloadJson)
    }

    suspend fun getOrderByTableId(tableId: String): OrderBootstrap? {
        val row = dao.getActiveByTableId(tableId) ?: return null
        return decode(row.payloadJson)
    }

    suspend fun saveOrder(order: OrderBootstrap) {
        val orderId = order.orderId ?: return
        val json = orderAdapter.toJson(order) ?: return
        dao.upsert(
            ResOrderEntity(
                id = orderId,
                resTablesId = order.tableId,
                guestCount = order.guestCount,
                status = order.status,
                paymentStatus = if (order.status.equals("finalized", true)) "Paid" else "Unpaid",
                payloadJson = json
            )
        )
    }

    suspend fun deleteOrder(orderId: String) {
        if (orderId.isBlank()) return
        dao.deleteById(orderId)
    }

    suspend fun deleteOrdersForTable(tableId: String) {
        if (tableId.isBlank()) return
        dao.deleteByTableId(tableId)
    }

    suspend fun remapOrderId(oldId: String, newId: String) {
        if (oldId.isBlank() || newId.isBlank() || oldId == newId) return
        val row = dao.getById(oldId) ?: return
        val order = decode(row.payloadJson)?.copy(orderId = newId) ?: return
        dao.deleteById(oldId)
        saveOrder(order)
    }

    private fun decode(json: String): OrderBootstrap? {
        return try {
            orderAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun isActiveStatus(status: String): Boolean {
            return status.lowercase() !in INACTIVE_STATUSES
        }

        private val INACTIVE_STATUSES = setOf("finalized", "completed", "cancelled")
    }
}
