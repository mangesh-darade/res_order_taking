package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sma_res_orders",
    indices = [Index(value = ["res_tables_id"])]
)
data class ResOrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "res_tables_id") val resTablesId: String?,
    @ColumnInfo(name = "guest_count") val guestCount: Int,
    val status: String,
    @ColumnInfo(name = "payment_status") val paymentStatus: String = "Unpaid",
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
