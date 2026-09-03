package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PendingSyncEntity::class,
        MenuCategoryEntity::class,
        MenuItemEntity::class,
        ProductCustomizationEntity::class,
        MenuSyncMetaEntity::class,
        SectionEntity::class,
        SubsectionEntity::class,
        TableItemEntity::class,
        ResOrderEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingSyncDao(): PendingSyncDao

    abstract fun menuCacheDao(): MenuCacheDao

    abstract fun orderCacheDao(): OrderCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS menu_categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        icon TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS menu_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        categoryId TEXT,
                        price REAL NOT NULL,
                        vegType TEXT,
                        imageUrl TEXT,
                        description TEXT,
                        stockQty REAL,
                        inStock INTEGER NOT NULL,
                        stockWarning INTEGER NOT NULL,
                        station TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_menu_items_categoryId ON menu_items(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_menu_items_vegType ON menu_items(vegType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_menu_items_name ON menu_items(name)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS product_customizations (
                        productId TEXT NOT NULL PRIMARY KEY,
                        payloadJson TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS menu_sync_meta (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        longValue INTEGER NOT NULL,
                        stringValue TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS floor_sections (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        subsectionsCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS floor_subsections (
                        id TEXT NOT NULL PRIMARY KEY,
                        sectionId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_floor_subsections_sectionId ON floor_subsections(sectionId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS floor_tables (
                        id TEXT NOT NULL PRIMARY KEY,
                        tableNumber TEXT NOT NULL,
                        sectionId TEXT,
                        subsectionId TEXT,
                        status TEXT NOT NULL,
                        guestsCount INTEGER NOT NULL,
                        occupiedTime TEXT,
                        orderId TEXT,
                        reservedBy TEXT,
                        reservedUntil TEXT,
                        reservedNote TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_floor_tables_sectionId ON floor_tables(sectionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_floor_tables_subsectionId ON floor_tables(subsectionId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS menu_categories")
                db.execSQL("DROP TABLE IF EXISTS menu_items")
                db.execSQL("DROP TABLE IF EXISTS product_customizations")
                db.execSQL("DROP TABLE IF EXISTS menu_sync_meta")
                db.execSQL("DROP TABLE IF EXISTS pending_sync_queue")
                db.execSQL("DROP TABLE IF EXISTS floor_sections")
                db.execSQL("DROP TABLE IF EXISTS floor_subsections")
                db.execSQL("DROP TABLE IF EXISTS floor_tables")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sma_categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        image TEXT,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sma_products (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        category_id TEXT,
                        price REAL NOT NULL,
                        veg_type TEXT,
                        image TEXT,
                        details TEXT,
                        stock_qty REAL,
                        in_stock INTEGER NOT NULL,
                        stock_warning INTEGER NOT NULL,
                        station TEXT,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sma_products_category_id ON sma_products(category_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sma_products_veg_type ON sma_products(veg_type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sma_products_name ON sma_products(name)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS product_customizations (
                        product_id TEXT NOT NULL PRIMARY KEY,
                        payload_json TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS menu_sync_meta (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        long_value INTEGER NOT NULL,
                        string_value TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        order_id TEXT NOT NULL,
                        action_type TEXT NOT NULL,
                        payload_json TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        retry_count INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sma_res_sections (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        subsections_count INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sma_res_subsections (
                        id TEXT NOT NULL PRIMARY KEY,
                        section_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sma_res_subsections_section_id ON sma_res_subsections(section_id)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sma_res_tables (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        section_id TEXT,
                        subsection_id TEXT,
                        status TEXT NOT NULL,
                        guest_count INTEGER NOT NULL,
                        occupied_time TEXT,
                        order_id TEXT,
                        reserved_by TEXT,
                        reserved_until TEXT,
                        reserved_note TEXT,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sma_res_tables_section_id ON sma_res_tables(section_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sma_res_tables_subsection_id ON sma_res_tables(subsection_id)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sma_res_orders (
                        id TEXT NOT NULL PRIMARY KEY,
                        res_tables_id TEXT,
                        guest_count INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        payment_status TEXT NOT NULL,
                        payload_json TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sma_res_orders_res_tables_id ON sma_res_orders(res_tables_id)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_sync_queue ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE pending_sync_queue ADD COLUMN last_error_message TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE pending_sync_queue ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant_pos.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
