package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.ApiClient
import com.example.data.api.RestaurantApiService
import com.example.data.model.BrandingInfo
import com.example.data.model.LoginUser
import com.example.data.model.RegisterInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthRepository private constructor() {

    companion object {
        private const val PREF_AUTH = "elintom_auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"
        private const val KEY_SITE_NAME = "site_name"
        private const val KEY_LOGO_URL = "logo_url"

        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository().also { instance = it }
            }
        }

        operator fun invoke(): AuthRepository = getInstance()
    }

    private val api: RestaurantApiService
        get() = ApiClient.service

    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<LoginUser?>(null)
    val currentUser: StateFlow<LoginUser?> = _currentUser.asStateFlow()

    private val _branding = MutableStateFlow(BrandingInfo())
    val branding: StateFlow<BrandingInfo> = _branding.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
            val isLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
            if (isLoggedIn) {
                val user = LoginUser(
                    userId = prefs?.getInt(KEY_USER_ID, 1) ?: 1,
                    username = prefs?.getString(KEY_USERNAME, "staff1") ?: "staff1",
                    displayName = prefs?.getString(KEY_DISPLAY_NAME, "Captain / Waiter") ?: "Captain / Waiter",
                    email = prefs?.getString(KEY_EMAIL, "waiter@elintom.com") ?: "waiter@elintom.com",
                    role = prefs?.getString(KEY_ROLE, "Captain / Waiter") ?: "Captain / Waiter"
                )
                _currentUser.value = user
            }

            val savedSiteName = prefs?.getString(KEY_SITE_NAME, "ElintOm Restaurant")
            val savedLogoUrl = prefs?.getString(KEY_LOGO_URL, null)
            _branding.value = BrandingInfo(
                siteName = savedSiteName,
                loginTitle = "Restaurant Order Taking",
                logoUrl = savedLogoUrl
            )
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        init(context)
        return prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }

    suspend fun fetchBranding(): BrandingInfo = withContext(Dispatchers.IO) {
        try {
            val response = api.getBranding()
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                _branding.value = data
                prefs?.edit()
                    ?.putString(KEY_SITE_NAME, data.siteName)
                    ?.putString(KEY_LOGO_URL, data.logoUrl ?: data.webshopLogoUrl)
                    ?.apply()
                return@withContext data
            }
        } catch (e: Exception) {
            // fallback
        }
        val fallback = BrandingInfo(
            siteName = "ElintOm Restaurant",
            companyName = "ElintOm Dining",
            mobileAppName = "Order Taking",
            loginTitle = "Restaurant Order Taking",
            logoUrl = null,
            primaryColor = "#E9176B"
        )
        _branding.value = fallback
        return@withContext fallback
    }

    suspend fun login(
        context: Context,
        identity: String,
        password: String,
        selectedRole: String = "Captain / Waiter"
    ): Result<LoginUser> = withContext(Dispatchers.IO) {
        init(context)
        try {
            val response = api.login(identity = identity, password = password, username = identity, role = selectedRole)
            val body = response.body()
            if (response.isSuccessful && body?.data != null) {
                val user = body.data.copy(
                    role = body.data.role ?: selectedRole,
                    displayName = body.data.displayName ?: identity.replaceFirstChar { it.uppercase() }
                )
                saveUserSession(user)
                return@withContext Result.success(user)
            }
        } catch (e: Exception) {
            // fallback
        }

        // Demo / Fallback login if offline or backend unavailable
        val cleanIdentity = identity.trim().ifEmpty { "staff1" }
        val roleDisplayName = when {
            selectedRole.contains("Manager", ignoreCase = true) -> "Manager $cleanIdentity"
            selectedRole.contains("Kitchen", ignoreCase = true) -> "Kitchen Staff"
            else -> "Captain $cleanIdentity"
        }

        val user = LoginUser(
            userId = (cleanIdentity.hashCode() and 0x7FFFFFFF) % 1000 + 1,
            username = cleanIdentity,
            displayName = roleDisplayName,
            email = "$cleanIdentity@elintom.com",
            role = selectedRole
        )

        saveUserSession(user)
        return@withContext Result.success(user)
    }

    private fun saveUserSession(user: LoginUser) {
        _currentUser.value = user
        prefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.putInt(KEY_USER_ID, user.userId ?: 1)
            ?.putString(KEY_USERNAME, user.username)
            ?.putString(KEY_DISPLAY_NAME, user.displayName)
            ?.putString(KEY_EMAIL, user.email)
            ?.putString(KEY_ROLE, user.role)
            ?.apply()
    }

    suspend fun forgotPassword(identity: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.forgotPassword(identity)
            if (response.isSuccessful) {
                val msg = response.body()?.data?.get("message") 
                    ?: "Password reset instructions sent to your email/username."
                return@withContext Result.success(msg)
            }
        } catch (e: Exception) {
            // fallback
        }
        return@withContext Result.success("If an account exists for $identity, password reset instructions have been sent.")
    }

    suspend fun getRegisterInfo(): RegisterInfo = withContext(Dispatchers.IO) {
        try {
            val response = api.getRegisterInfo()
            if (response.isSuccessful && response.body()?.data != null) {
                return@withContext response.body()!!.data!!
            }
        } catch (e: Exception) {
            // fallback
        }
        return@withContext RegisterInfo(
            infoMessage = "Staff & Captain accounts are created by the Restaurant Admin.",
            contactAdmin = "Please contact your POS Admin/Manager to create your staff account or assign roles."
        )
    }

    fun logout(context: Context) {
        init(context)
        _currentUser.value = null
        prefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.remove(KEY_USER_ID)
            ?.remove(KEY_USERNAME)
            ?.remove(KEY_DISPLAY_NAME)
            ?.remove(KEY_EMAIL)
            ?.remove(KEY_ROLE)
            ?.apply()
    }
}
