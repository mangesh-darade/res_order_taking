package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sma_res_sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "subsections_count") val subsectionsCount: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
