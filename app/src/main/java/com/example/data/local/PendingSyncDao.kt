package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(entity: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync_queue ORDER BY id ASC")
    suspend fun getAllPendingActions(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync_queue")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync_queue")
    suspend fun getPendingCount(): Int

    @Delete
    suspend fun deletePendingAction(entity: PendingSyncEntity)

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync_queue")
    suspend fun clearAll()
}
