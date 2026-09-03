package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MenuCacheDao {

    @Query("SELECT * FROM sma_categories ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllCategories(): List<MenuCategoryEntity>

    @Query("DELETE FROM sma_categories")
    suspend fun deleteAllCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<MenuCategoryEntity>)

    @Transaction
    suspend fun replaceAllCategories(items: List<MenuCategoryEntity>) {
        deleteAllCategories()
        if (items.isNotEmpty()) {
            insertCategories(items)
        }
    }

    @Query(
        """
        SELECT * FROM sma_products
        WHERE (:categoryId = '' OR category_id = :categoryId)
          AND (:vegType = '' OR :vegType = 'all' OR LOWER(veg_type) = LOWER(:vegType))
          AND (:search = '' OR name LIKE '%' || :search || '%')
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getItemsFiltered(
        categoryId: String,
        vegType: String,
        search: String
    ): List<MenuItemEntity>

    @Query("SELECT COUNT(*) FROM sma_products")
    suspend fun getItemCount(): Int

    @Query("DELETE FROM sma_products")
    suspend fun deleteAllItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MenuItemEntity>)

    @Transaction
    suspend fun replaceAllItems(items: List<MenuItemEntity>) {
        deleteAllItems()
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }

    @Query("SELECT * FROM product_customizations WHERE product_id = :productId LIMIT 1")
    suspend fun getCustomization(productId: String): ProductCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomization(entity: ProductCustomizationEntity)

    @Query("SELECT * FROM menu_sync_meta WHERE `key` = :key LIMIT 1")
    suspend fun getMeta(key: String): MenuSyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(entity: MenuSyncMetaEntity)

    @Query("SELECT * FROM sma_res_sections ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllSections(): List<SectionEntity>

    @Query("DELETE FROM sma_res_sections")
    suspend fun deleteAllSections()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(items: List<SectionEntity>)

    @Transaction
    suspend fun replaceAllSections(items: List<SectionEntity>) {
        deleteAllSections()
        if (items.isNotEmpty()) {
            insertSections(items)
        }
    }

    @Query("SELECT * FROM sma_res_subsections WHERE section_id = :sectionId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getSubsections(sectionId: String): List<SubsectionEntity>

    @Query("DELETE FROM sma_res_subsections WHERE section_id = :sectionId")
    suspend fun deleteSubsectionsForSection(sectionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubsections(items: List<SubsectionEntity>)

    @Transaction
    suspend fun replaceSubsectionsForSection(sectionId: String, items: List<SubsectionEntity>) {
        deleteSubsectionsForSection(sectionId)
        if (items.isNotEmpty()) {
            insertSubsections(items)
        }
    }

    @Query(
        """
        SELECT * FROM sma_res_tables
        WHERE section_id = :sectionId
          AND (:subsectionId = '' OR subsection_id = :subsectionId)
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getTables(sectionId: String, subsectionId: String): List<TableItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTables(items: List<TableItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTable(item: TableItemEntity)
}
