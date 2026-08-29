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

    @Update
    suspend fun updatePendingAction(entity: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync_queue WHERE orderId = :orderId ORDER BY id ASC")
    suspend fun getPendingByOrderId(orderId: String): List<PendingSyncEntity>

    @Query("UPDATE pending_sync_queue SET orderId = :newOrderId WHERE orderId = :oldOrderId")
    suspend fun remapOrderId(oldOrderId: String, newOrderId: String)

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync_queue")
    suspend fun clearAll()
}
