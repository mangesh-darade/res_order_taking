package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sma_products",
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["veg_type"]),
        Index(value = ["name"])
    ]
)
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    val price: Double,
    @ColumnInfo(name = "veg_type") val vegType: String?,
    @ColumnInfo(name = "image") val imageUrl: String?,
    @ColumnInfo(name = "details") val description: String?,
    @ColumnInfo(name = "stock_qty") val stockQty: Double?,
    @ColumnInfo(name = "in_stock") val inStock: Boolean,
    @ColumnInfo(name = "stock_warning") val stockWarning: Boolean,
    val station: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
