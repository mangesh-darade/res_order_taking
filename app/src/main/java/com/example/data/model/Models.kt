package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResponseMeta(
    @Json(name = "status") val status: String? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "response") val response: ResponseMeta? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class Section(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "subsections_count") val subsectionsCount: Int? = 0
)

@JsonClass(generateAdapter = true)
data class Subsection(
    @Json(name = "id") val id: String,
    @Json(name = "section_id") val sectionId: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class TableItem(
    @Json(name = "id") val id: String,
    @Json(name = "table_number") val tableNumber: String,
    @Json(name = "section_id") val sectionId: String? = null,
    @Json(name = "subsection_id") val subsectionId: String? = null,
    @Json(name = "status") val status: String = "available", // available, occupied, reserved, order-placed, ready, free, served
    @Json(name = "guests_count") val guestsCount: Int? = 0,
    @Json(name = "occupied_time") val occupiedTime: String? = null,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "reserved_by") val reservedBy: String? = null,
    @Json(name = "reserved_until") val reservedUntil: String? = null,
    @Json(name = "reserved_note") val reservedNote: String? = null
)

@JsonClass(generateAdapter = true)
data class TableStatusInfo(
    @Json(name = "key") val key: String,
    @Json(name = "label") val label: String,
    @Json(name = "color") val color: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "product_id") val productId: String = "",
    @Json(name = "product_name") val productName: String = "",
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "veg_type") val vegType: String? = "veg", // "veg", "non-veg"
    @Json(name = "spice_level") val spiceLevel: String? = null,
    @Json(name = "meat_wellness") val meatWellness: String? = null,
    @Json(name = "allergies") val allergies: List<String>? = emptyList(),
    @Json(name = "custom_allergies") val customAllergies: String? = null,
    @Json(name = "add_ons") val addOns: List<String>? = emptyList(),
    @Json(name = "toppings") val toppings: List<String>? = emptyList(),
    @Json(name = "onion_flag") val onionFlag: Boolean? = false, // true = No Onion
    @Json(name = "garlic_flag") val garlicFlag: Boolean? = false, // true = No Garlic
    @Json(name = "special_instructions") val specialInstructions: String? = null,
    @Json(name = "status") val status: String? = "pending" // "pending", "kot", "ready", "served"
)

@JsonClass(generateAdapter = true)
data class GuestOrder(
    @Json(name = "guest_id") val guestId: Int,
    @Json(name = "guest_name") val guestName: String? = "Guest",
    @Json(name = "items") val items: List<OrderItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OrderBootstrap(
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "table_id") val tableId: String? = null,
    @Json(name = "table_number") val tableNumber: String? = null,
    @Json(name = "section_name") val sectionName: String? = null,
    @Json(name = "subsection_name") val subsectionName: String? = null,
    @Json(name = "guest_count") val guestCount: Int = 1,
    @Json(name = "status") val status: String = "active", // "active", "kot_sent", "ready", "served", "finalized"
    @Json(name = "guests") val guests: List<GuestOrder> = emptyList(),
    @Json(name = "total_items") val totalItems: Int = 0,
    @Json(name = "grand_total") val grandTotal: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class MenuCategory(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "icon") val icon: String? = null
)

@JsonClass(generateAdapter = true)
data class MenuItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "veg_type") val vegType: String? = "veg", // "veg", "non-veg"
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "stock_qty") val stockQty: Double? = null,
    @Json(name = "in_stock") val inStock: Boolean? = true,
    @Json(name = "stock_warning") val stockWarning: Boolean? = false,
    @Json(name = "station") val station: String? = null
)

@JsonClass(generateAdapter = true)
data class CustomizationOption(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class ProductCustomization(
    @Json(name = "product_id") val productId: String,
    @Json(name = "add_ons") val addOns: List<CustomizationOption>? = emptyList(),
    @Json(name = "toppings") val toppings: List<CustomizationOption>? = emptyList(),
    @Json(name = "allergies") val allergies: List<CustomizationOption>? = emptyList(),
    @Json(name = "meat_wellness") val meatWellness: List<String>? = emptyList(),
    @Json(name = "spice_levels") val spiceLevels: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class FinalizeOrderResponse(
    @Json(name = "sale_id") val saleId: String? = null,
    @Json(name = "invoice_url") val invoiceUrl: String? = null,
    @Json(name = "grand_total") val grandTotal: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class BrandingInfo(
    @Json(name = "site_name") val siteName: String? = "Darbar Restaurant",
    @Json(name = "company_name") val companyName: String? = "Darbar Restaurant",
    @Json(name = "mobile_app_name") val mobileAppName: String? = "Order Taking",
    @Json(name = "login_title") val loginTitle: String? = "Restaurant Order Taking",
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "webshop_logo_url") val webshopLogoUrl: String? = null,
    @Json(name = "primary_color") val primaryColor: String? = "#E9176B",
    @Json(name = "default_currency") val defaultCurrency: String? = "INR",
    @Json(name = "currency_symbol") val currencySymbol: String? = "Rs",
    @Json(name = "display_symbol") val displaySymbol: Int? = 1,
    @Json(name = "timezone") val timezone: String? = "Asia/Kolkata",
    @Json(name = "decimals") val decimals: Int? = 2,
    @Json(name = "qty_decimals") val qtyDecimals: Int? = 3,
    @Json(name = "decimals_sep") val decimalsSep: String? = ".",
    @Json(name = "thousands_sep") val thousandsSep: String? = ",",
    @Json(name = "server_time") val serverTime: String? = null,
    @Json(name = "pos_type") val posType: String? = "restaurant",
    /** POS Settings.overselling: 1 = allow (soft), 0 = hard-block out of stock. */
    @Json(name = "overselling") val overselling: Int? = 1,
    /** Derived: 1 when overselling=0. */
    @Json(name = "strict_stock") val strictStock: Int? = 0
)

@JsonClass(generateAdapter = true)
data class LoginUser(
    @Json(name = "user_id") val userId: Int? = 1,
    @Json(name = "username") val username: String? = "waiter1",
    @Json(name = "email") val email: String? = "waiter@elintom.com",
    @Json(name = "first_name") val firstName: String? = "Staff",
    @Json(name = "last_name") val lastName: String? = "Member",
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "display_name") val displayName: String? = "Captain / Waiter",
    @Json(name = "role") val role: String? = "Captain", // Captain, Manager, Kitchen
    @Json(name = "company_id") val companyId: Int? = null
)

@JsonClass(generateAdapter = true)
data class RegisterOption(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "code") val code: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterInfo(
    @Json(name = "info_message") val infoMessage: String? = "Staff & Captain accounts are created by the Restaurant Admin.",
    @Json(name = "contact_admin") val contactAdmin: String? = "Please contact your POS Admin/Manager to get login credentials.",
    @Json(name = "groups") val groups: List<RegisterOption>? = emptyList(),
    @Json(name = "warehouses") val warehouses: List<RegisterOption>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TableMoveResponse(
    @Json(name = "message") val message: String? = null,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "from_table_id") val fromTableId: String? = null,
    @Json(name = "to_table_id") val toTableId: String? = null,
    @Json(name = "order") val order: OrderBootstrap? = null
)

