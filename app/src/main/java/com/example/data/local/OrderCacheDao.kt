package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OrderCacheDao {

    @Query("SELECT * FROM sma_res_orders ORDER BY updated_at DESC")
    suspend fun getAllOrders(): List<ResOrderEntity>

    @Query("SELECT * FROM sma_res_orders WHERE id = :orderId LIMIT 1")
    suspend fun getById(orderId: String): ResOrderEntity?

    @Query(
        """
        SELECT * FROM sma_res_orders
        WHERE res_tables_id = :tableId
          AND LOWER(status) NOT IN ('finalized', 'completed', 'cancelled')
        ORDER BY updated_at DESC
        LIMIT 1
        """
    )
    suspend fun getActiveByTableId(tableId: String): ResOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ResOrderEntity)

    @Query("DELETE FROM sma_res_orders WHERE id = :orderId")
    suspend fun deleteById(orderId: String)

    @Query("DELETE FROM sma_res_orders WHERE res_tables_id = :tableId")
    suspend fun deleteByTableId(tableId: String)

    @Query("UPDATE sma_res_orders SET id = :newId WHERE id = :oldId")
    suspend fun remapOrderId(oldId: String, newId: String)
}
