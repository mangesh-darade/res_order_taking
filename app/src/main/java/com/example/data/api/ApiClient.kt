package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var currentBaseUrl: String = ""

    @Volatile
    private var currentApiKey: String = ""

    @Volatile
    private var currentService: RestaurantApiService? = null

    fun sanitizeBaseUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return "http://devdinein.elintpos.in/ordertakingapi/"
        var cleaned = rawUrl.trim()
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "http://$cleaned"
        }
        // Remove trailing slashes
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length - 1)
        }
        // Auto-append ordertakingapi if not already present at the end
        if (!cleaned.endsWith("ordertakingapi", ignoreCase = true) &&
            !cleaned.endsWith("restaurant_order_taking_api", ignoreCase = true) &&
            !cleaned.endsWith("Restaurant_Order_Taking", ignoreCase = true)) {
            cleaned += "/ordertakingapi"
        }
        cleaned += "/"
        return cleaned
    }

    fun updateConfig(baseUrl: String, apiKey: String) {
        currentBaseUrl = sanitizeBaseUrl(baseUrl)
        currentApiKey = apiKey.trim()
        synchronized(this) {
            currentService = buildService()
        }
    }

    fun getBaseUrl(): String {
        return if (currentBaseUrl.isNotBlank()) currentBaseUrl else ApiSettingsManager.baseUrl
    }

    fun getApiKey(): String {
        return if (currentApiKey.isNotBlank()) currentApiKey else ApiSettingsManager.apiKey
    }

    private val authInterceptor = Interceptor { chain ->
        val activeKey = getApiKey()
        val request = chain.request().newBuilder()
            .addHeader("X-API-KEY", activeKey)
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private fun buildService(): RestaurantApiService {
        val activeUrl = sanitizeBaseUrl(getBaseUrl())
        return Retrofit.Builder()
            .baseUrl(activeUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RestaurantApiService::class.java)
    }

    val service: RestaurantApiService
        get() {
            if (currentService == null) {
                synchronized(this) {
                    if (currentService == null) {
                        currentService = buildService()
                    }
                }
            }
            return currentService!!
        }
}

