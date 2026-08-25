package com.example.util

import com.example.data.model.BrandingInfo

object CurrencyConfig {
    var symbol: String = "Rs"
    var decimals: Int = 2
    var decimalsSep: String = "."
    var thousandsSep: String = ","

    fun updateFromBranding(branding: BrandingInfo?) {
        if (branding != null) {
            val s = branding.currencySymbol
            symbol = if (!s.isNullOrBlank()) s else if (branding.defaultCurrency == "INR") "Rs" else "$"
            decimals = branding.decimals ?: 2
            decimalsSep = branding.decimalsSep ?: "."
            thousandsSep = branding.thousandsSep ?: ","
        }
    }

    fun format(amount: Double): String {
        return "$symbol ${String.format("%.${decimals}f", amount)}"
    }

    fun formatWithoutSpace(amount: Double): String {
        return "$symbol${String.format("%.${decimals}f", amount)}"
    }
}
