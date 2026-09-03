package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_queue")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "order_id") val orderId: String,
    @ColumnInfo(name = "action_type") val actionType: String, // CREATE_ORDER, ADD_ITEM, UPDATE_QTY, UPDATE_ITEM, SEND_KOT, FINALIZE_ORDER, FREE_TABLE, RESERVE_TABLE, UNRESERVE_TABLE
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "status") val status: String = "PENDING", // PENDING, SYNCING, FAILED, CONFLICT
    @ColumnInfo(name = "last_error_message") val lastErrorMessage: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
