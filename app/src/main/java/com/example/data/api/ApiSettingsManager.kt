package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

object ApiSettingsManager {
    private const val PREF_NAME = "elintom_api_settings"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_IS_LOGGED_IN = "is_admin_logged_in"
    private const val KEY_SETUP_COMPLETE = "setup_complete"

    private var prefs: SharedPreferences? = null

    var baseUrl: String = "http://devdinein.elintpos.in/ordertakingapi/"
        private set

    var apiKey: String = "YOUR_X_API_KEY"
        private set

    var isAdminLoggedIn: Boolean = false
        private set

    /** True only after user saved API URL+key via Settings (fresh install = false). */
    var isSetupComplete: Boolean = false
        private set

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val defaultBaseUrl = try { BuildConfig.BASE_URL } catch (e: Exception) { "" }
            val defaultApiKey = try { BuildConfig.X_API_KEY } catch (e: Exception) { "" }

            val savedUrl = prefs?.getString(KEY_BASE_URL, null)
            val savedKey = prefs?.getString(KEY_API_KEY, null)

            baseUrl = if (!savedUrl.isNullOrBlank()) ApiClient.sanitizeBaseUrl(savedUrl) else if (defaultBaseUrl.isNotBlank()) ApiClient.sanitizeBaseUrl(defaultBaseUrl) else "http://devdinein.elintpos.in/ordertakingapi/"
            apiKey = if (!savedKey.isNullOrBlank()) savedKey else if (defaultApiKey.isNotBlank()) defaultApiKey else "YOUR_X_API_KEY"
            isAdminLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
            isSetupComplete = prefs?.getBoolean(KEY_SETUP_COMPLETE, false) ?: false

            // Migrate: older installs that already saved a URL count as configured
            if (!isSetupComplete && !savedUrl.isNullOrBlank()) {
                isSetupComplete = true
                prefs?.edit()?.putBoolean(KEY_SETUP_COMPLETE, true)?.apply()
            }

            ApiClient.updateConfig(baseUrl, apiKey)
        }
    }

    fun saveSettings(context: Context, newBaseUrl: String, newApiKey: String, markSetupComplete: Boolean = true) {
        init(context)
        val cleanUrl = ApiClient.sanitizeBaseUrl(newBaseUrl)
        baseUrl = cleanUrl
        apiKey = newApiKey.trim()

        val editor = prefs?.edit()
            ?.putString(KEY_BASE_URL, baseUrl)
            ?.putString(KEY_API_KEY, apiKey)
        if (markSetupComplete) {
            isSetupComplete = true
            editor?.putBoolean(KEY_SETUP_COMPLETE, true)
        }
        editor?.apply()

        ApiClient.updateConfig(baseUrl, apiKey)
    }

    fun setAdminLoggedIn(context: Context, loggedIn: Boolean) {
        init(context)
        isAdminLoggedIn = loggedIn
        prefs?.edit()?.putBoolean(KEY_IS_LOGGED_IN, loggedIn)?.apply()
    }

    fun clearSetupForTesting(context: Context) {
        init(context)
        isSetupComplete = false
        prefs?.edit()?.putBoolean(KEY_SETUP_COMPLETE, false)?.apply()
    }
}
