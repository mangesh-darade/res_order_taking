package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_sync_meta")
data class MenuSyncMetaEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "long_value") val longValue: Long = 0L,
    @ColumnInfo(name = "string_value") val stringValue: String? = null
)
