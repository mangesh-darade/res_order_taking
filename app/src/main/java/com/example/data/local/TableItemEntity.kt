package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sma_res_tables",
    indices = [
        Index(value = ["section_id"]),
        Index(value = ["subsection_id"])
    ]
)
data class TableItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val tableNumber: String,
    @ColumnInfo(name = "section_id") val sectionId: String?,
    @ColumnInfo(name = "subsection_id") val subsectionId: String?,
    val status: String,
    @ColumnInfo(name = "guest_count") val guestsCount: Int,
    @ColumnInfo(name = "occupied_time") val occupiedTime: String?,
    @ColumnInfo(name = "order_id") val orderId: String?,
    @ColumnInfo(name = "reserved_by") val reservedBy: String?,
    @ColumnInfo(name = "reserved_until") val reservedUntil: String?,
    @ColumnInfo(name = "reserved_note") val reservedNote: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
