package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sma_res_subsections",
    indices = [Index(value = ["section_id"])]
)
data class SubsectionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "section_id") val sectionId: String,
    val name: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
