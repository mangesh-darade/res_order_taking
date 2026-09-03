package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(entity: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync_queue WHERE status = 'PENDING' ORDER BY id ASC")
    suspend fun getAllPendingActions(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync_queue ORDER BY id ASC")
    suspend fun getAllActions(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync_queue ORDER BY id ASC")
    fun getAllActionsFlow(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync_queue WHERE status IN ('FAILED', 'CONFLICT') ORDER BY updated_at DESC")
    suspend fun getFailedActions(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync_queue WHERE status IN ('FAILED', 'CONFLICT') ORDER BY updated_at DESC")
    fun getFailedActionsFlow(): Flow<List<PendingSyncEntity>>

    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE status = 'PENDING' OR status = 'SYNCING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE status = 'PENDING' OR status = 'SYNCING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE status IN ('FAILED', 'CONFLICT')")
    fun getFailedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync_queue WHERE status IN ('FAILED', 'CONFLICT')")
    suspend fun getFailedCount(): Int

    @Query("SELECT * FROM pending_sync_queue WHERE id = :id LIMIT 1")
    suspend fun getActionById(id: Long): PendingSyncEntity?

    @Delete
    suspend fun deletePendingAction(entity: PendingSyncEntity)

    @Update
    suspend fun updatePendingAction(entity: PendingSyncEntity)

    @Query("UPDATE pending_sync_queue SET status = :status, last_error_message = :errorMsg, updated_at = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMsg: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE pending_sync_queue SET status = 'PENDING', retry_count = 0, last_error_message = null, updated_at = :now WHERE status IN ('FAILED', 'CONFLICT')")
    suspend fun retryAllFailed(now: Long = System.currentTimeMillis())

    @Query("UPDATE pending_sync_queue SET status = 'PENDING', retry_count = 0, last_error_message = null, updated_at = :now WHERE id = :id")
    suspend fun retryAction(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM pending_sync_queue WHERE order_id = :orderId ORDER BY id ASC")
    suspend fun getPendingByOrderId(orderId: String): List<PendingSyncEntity>

    @Query("UPDATE pending_sync_queue SET order_id = :newOrderId WHERE order_id = :oldOrderId")
    suspend fun remapOrderId(oldOrderId: String, newOrderId: String)

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync_queue WHERE status IN ('FAILED', 'CONFLICT')")
    suspend fun clearFailedActions()

    @Query("DELETE FROM pending_sync_queue")
    suspend fun clearAll()
}
