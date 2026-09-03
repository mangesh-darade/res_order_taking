package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_customizations")
data class ProductCustomizationEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
