package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sma_categories")
data class MenuCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "image") val icon: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
