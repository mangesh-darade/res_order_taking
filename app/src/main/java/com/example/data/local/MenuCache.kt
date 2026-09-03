package com.example.data.local

import android.content.Context
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.ProductCustomization
import com.example.data.model.Section
import com.example.data.model.Subsection
import com.example.data.model.TableItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Offline-first menu catalog on disk (Room).
 * API refresh when online; reads always prefer local DB.
 */
class MenuCache(context: Context) {

    private val dao = AppDatabase.getDatabase(context.applicationContext).menuCacheDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val customizationAdapter = moshi.adapter(ProductCustomization::class.java)

    suspend fun getCategories(): List<MenuCategory> {
        return dao.getAllCategories().map { it.toModel() }
    }

    suspend fun saveCategories(categories: List<MenuCategory>) {
        if (categories.isEmpty()) return
        dao.replaceAllCategories(categories.map { it.toEntity() })
    }

    suspend fun getItems(
        categoryId: String? = null,
        mealType: String? = null,
        search: String? = null
    ): List<MenuItem> {
        val cat = categoryId.orEmpty()
        val veg = when {
            mealType.isNullOrBlank() || mealType.equals("all", ignoreCase = true) -> ""
            else -> mealType
        }
        val q = search?.trim().orEmpty()
        return dao.getItemsFiltered(cat, veg, q).map { it.toModel() }
    }

    suspend fun replaceAllItems(items: List<MenuItem>) {
        dao.replaceAllItems(items.map { it.toEntity() })
    }

    suspend fun upsertItems(items: List<MenuItem>) {
        if (items.isEmpty()) return
        dao.insertItems(items.map { it.toEntity() })
    }

    suspend fun getItemCount(): Int = dao.getItemCount()

    suspend fun getCustomization(productId: String): ProductCustomization? {
        val row = dao.getCustomization(productId) ?: return null
        return try {
            customizationAdapter.fromJson(row.payloadJson)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCustomization(customization: ProductCustomization) {
        val json = customizationAdapter.toJson(customization) ?: return
        dao.upsertCustomization(
            ProductCustomizationEntity(
                productId = customization.productId,
                payloadJson = json
            )
        )
    }

    suspend fun getLastCatalogSyncAt(): Long {
        return dao.getMeta(KEY_CATALOG_SYNC)?.longValue ?: 0L
    }

    suspend fun getLastFloorSyncAt(): Long {
        return dao.getMeta(KEY_FLOOR_SYNC)?.longValue ?: 0L
    }

    suspend fun markCatalogSynced() {
        dao.upsertMeta(MenuSyncMetaEntity(key = KEY_CATALOG_SYNC, longValue = System.currentTimeMillis()))
    }

    suspend fun getSections(): List<Section> {
        return dao.getAllSections().map { it.toModel() }
    }

    suspend fun saveSections(sections: List<Section>) {
        if (sections.isEmpty()) return
        dao.replaceAllSections(sections.map { it.toEntity() })
    }

    suspend fun getSubsections(sectionId: String): List<Subsection> {
        return dao.getSubsections(sectionId).map { it.toModel() }
    }

    suspend fun saveSubsections(sectionId: String, subsections: List<Subsection>) {
        dao.replaceSubsectionsForSection(sectionId, subsections.map { it.toEntity() })
    }

    suspend fun getTables(sectionId: String, subsectionId: String? = null): List<TableItem> {
        return dao.getTables(sectionId, subsectionId.orEmpty()).map { it.toModel() }
    }

    suspend fun upsertTables(tables: List<TableItem>) {
        if (tables.isEmpty()) return
        dao.upsertTables(tables.map { it.toEntity() })
    }

    suspend fun upsertTable(table: TableItem) {
        dao.upsertTable(table.toEntity())
    }

    suspend fun markFloorPlanSynced() {
        dao.upsertMeta(MenuSyncMetaEntity(key = KEY_FLOOR_SYNC, longValue = System.currentTimeMillis()))
    }

    companion object {
        private const val KEY_CATALOG_SYNC = "catalog_sync_at"
        private const val KEY_FLOOR_SYNC = "floor_sync_at"
    }
}

private fun MenuCategoryEntity.toModel() = MenuCategory(id = id, name = name, icon = icon)

private fun MenuCategory.toEntity() = MenuCategoryEntity(id = id, name = name, icon = icon)

private fun MenuItemEntity.toModel() = MenuItem(
    id = id,
    name = name,
    categoryId = categoryId,
    price = price,
    vegType = vegType,
    imageUrl = imageUrl,
    description = description,
    stockQty = stockQty,
    inStock = inStock,
    stockWarning = stockWarning,
    station = station
)

private fun MenuItem.toEntity() = MenuItemEntity(
    id = id,
    name = name,
    categoryId = categoryId,
    price = price,
    vegType = vegType,
    imageUrl = imageUrl,
    description = description,
    stockQty = stockQty,
    inStock = inStock ?: true,
    stockWarning = stockWarning ?: false,
    station = station
)

private fun SectionEntity.toModel() = Section(
    id = id,
    name = name,
    subsectionsCount = subsectionsCount
)

private fun Section.toEntity() = SectionEntity(
    id = id,
    name = name,
    subsectionsCount = subsectionsCount ?: 0
)

private fun SubsectionEntity.toModel() = Subsection(
    id = id,
    sectionId = sectionId,
    name = name
)

private fun Subsection.toEntity() = SubsectionEntity(
    id = id,
    sectionId = sectionId,
    name = name
)

private fun TableItemEntity.toModel() = TableItem(
    id = id,
    tableNumber = tableNumber,
    sectionId = sectionId,
    subsectionId = subsectionId,
    status = status,
    guestsCount = guestsCount,
    occupiedTime = occupiedTime,
    orderId = orderId,
    reservedBy = reservedBy,
    reservedUntil = reservedUntil,
    reservedNote = reservedNote
)

private fun TableItem.toEntity() = TableItemEntity(
    id = id,
    tableNumber = tableNumber,
    sectionId = sectionId,
    subsectionId = subsectionId,
    status = status,
    guestsCount = guestsCount ?: 0,
    occupiedTime = occupiedTime,
    orderId = orderId,
    reservedBy = reservedBy,
    reservedUntil = reservedUntil,
    reservedNote = reservedNote
)
