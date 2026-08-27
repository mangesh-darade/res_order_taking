package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_queue")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String,
    val actionType: String, // CREATE_ORDER, ADD_ITEM, UPDATE_QTY, DELETE_ITEM, SEND_KOT, FINALIZE_ORDER, FREE_TABLE
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
