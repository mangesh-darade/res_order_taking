package com.example.data.api

import com.example.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface RestaurantApiService {

    @GET("sections")
    suspend fun getSections(): Response<ApiResponse<List<Section>>>

    @FormUrlEncoded
    @POST("subsections")
    suspend fun getSubsections(
        @Field("section_id") sectionId: String
    ): Response<ApiResponse<List<Subsection>>>

    @FormUrlEncoded
    @POST("tables")
    suspend fun getTables(
        @Field("section_id") sectionId: String,
        @Field("subsection_id") subsectionId: String? = null
    ): Response<ApiResponse<List<TableItem>>>

    @GET("table_statuses")
    suspend fun getTableStatuses(): Response<ApiResponse<List<TableStatusInfo>>>

    @FormUrlEncoded
    @POST("order_status")
    suspend fun getOrderStatus(
        @Field("table_id") tableId: String? = null,
        @Field("order_id") orderId: String? = null
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("order_bootstrap")
    suspend fun getOrderBootstrap(
        @Field("table_id") tableId: String? = null,
        @Field("order_id") orderId: String? = null
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("create_order")
    suspend fun createOrder(
        @Field("table_id") tableId: String,
        @Field("guest_count") guestCount: Int
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("increase_guest")
    suspend fun increaseGuest(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("decrease_guest")
    suspend fun decreaseGuest(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<OrderBootstrap>>

    @GET("menu_categories")
    suspend fun getMenuCategories(): Response<ApiResponse<List<MenuCategory>>>

    @FormUrlEncoded
    @POST("menu_items")
    suspend fun getMenuItems(
        @Field("category_id") categoryId: String? = null,
        @Field("meal_type") mealType: String? = null,
        @Field("search") search: String? = null
    ): Response<ApiResponse<List<MenuItem>>>

    @FormUrlEncoded
    @POST("product_customizations")
    suspend fun getProductCustomizations(
        @Field("product_id") productId: String
    ): Response<ApiResponse<ProductCustomization>>

    @FormUrlEncoded
    @POST("product_details")
    suspend fun getProductDetails(
        @Field("product_id") productId: String
    ): Response<ApiResponse<MenuItem>>

    @FormUrlEncoded
    @POST("add_allergy")
    suspend fun addAllergy(
        @Field("name") name: String
    ): Response<ApiResponse<Map<String, String>>>

    @FormUrlEncoded
    @POST("add_item")
    suspend fun addItem(
        @Field("order_id") orderId: String,
        @Field("guest_id") guestId: Int,
        @Field("product_id") productId: String,
        @Field("quantity") quantity: Int,
        @Field("spice_level") spiceLevel: String? = null,
        @Field("meat_wellness") meatWellness: String? = null,
        @Field("allergies") allergies: String? = null,
        @Field("custom_allergies") customAllergies: String? = null,
        @Field("add_ons") addOns: String? = null,
        @Field("toppings") toppings: String? = null,
        @Field("onion_flag") onionFlag: Int = 0,
        @Field("garlic_flag") garlicFlag: Int = 0,
        @Field("special_instructions") specialInstructions: String? = null
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("update_item")
    suspend fun updateItem(
        @Field("item_id") itemId: String,
        @Field("quantity") quantity: Int,
        @Field("spice_level") spiceLevel: String? = null,
        @Field("meat_wellness") meatWellness: String? = null,
        @Field("allergies") allergies: String? = null,
        @Field("custom_allergies") customAllergies: String? = null,
        @Field("add_ons") addOns: String? = null,
        @Field("toppings") toppings: String? = null,
        @Field("onion_flag") onionFlag: Int = 0,
        @Field("garlic_flag") garlicFlag: Int = 0,
        @Field("special_instructions") specialInstructions: String? = null
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("delete_item")
    suspend fun deleteItem(
        @Field("item_id") itemId: String
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("reserve_table")
    suspend fun reserveTable(
        @Field("table_id") tableId: String,
        @Field("reserved_by") reservedBy: String,
        @Field("reserved_until") reservedUntil: String? = null,
        @Field("reserved_note") reservedNote: String? = null
    ): Response<ApiResponse<Map<String, String>>>

    @FormUrlEncoded
    @POST("unreserve_table")
    suspend fun unreserveTable(
        @Field("table_id") tableId: String
    ): Response<ApiResponse<Map<String, String>>>

    @FormUrlEncoded
    @POST("free_table")
    suspend fun freeTable(
        @Field("table_id") tableId: String
    ): Response<ApiResponse<Map<String, String>>>

    @FormUrlEncoded
    @POST("mark_occupied")
    suspend fun markOccupied(
        @Field("table_id") tableId: String
    ): Response<ApiResponse<Map<String, String>>>

    @FormUrlEncoded
    @POST("mark_free")
    suspend fun markFree(
        @Field("table_id") tableId: String
    ): Response<ApiResponse<Map<String, String>>>

    @FormUrlEncoded
    @POST("update_kot_status")
    suspend fun updateKotStatus(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("mark_ready")
    suspend fun markReady(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("mark_served")
    suspend fun markServed(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("finalize_prepare")
    suspend fun finalizePrepare(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<OrderBootstrap>>

    @FormUrlEncoded
    @POST("finalize_order")
    suspend fun finalizeOrder(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<FinalizeOrderResponse>>

    @FormUrlEncoded
    @POST("complete_and_free")
    suspend fun completeAndFree(
        @Field("order_id") orderId: String
    ): Response<ApiResponse<Map<String, String>>>

    @GET("branding")
    suspend fun getBranding(): Response<ApiResponse<BrandingInfo>>

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("identity") identity: String,
        @Field("password") password: String,
        @Field("username") username: String? = null,
        @Field("role") role: String? = null
    ): Response<ApiResponse<LoginUser>>

    @FormUrlEncoded
    @POST("forgot_password")
    suspend fun forgotPassword(
        @Field("identity") identity: String
    ): Response<ApiResponse<Map<String, String>>>

    @GET("register_info")
    suspend fun getRegisterInfo(): Response<ApiResponse<RegisterInfo>>
}

