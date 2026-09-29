package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Division
import com.example.data.model.DivisionPrinterConfig
import com.example.data.model.PrinterConnectionType
import org.json.JSONArray
import org.json.JSONObject

object PrinterSettingsManager {
    private const val PREF_NAME = "elintom_printer_settings"
    private const val KEY_PRINTER_CONFIGS = "printer_configs_json"

    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        return prefs!!
    }

    /**
     * Get all saved printer configurations.
     */
    fun getPrinterConfigs(context: Context): List<DivisionPrinterConfig> {
        val p = getPrefs(context)
        val jsonStr = p.getString(KEY_PRINTER_CONFIGS, null) ?: return emptyList()
        val list = mutableListOf<DivisionPrinterConfig>()
        try {
            val jsonArr = JSONArray(jsonStr)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    DivisionPrinterConfig(
                        divisionId = obj.optString("division_id", ""),
                        divisionName = obj.optString("division_name", ""),
                        isEnabled = obj.optBoolean("is_enabled", true),
                        connectionType = if (obj.optString("connection_type") == "USB") PrinterConnectionType.USB else PrinterConnectionType.NETWORK,
                        ipAddress = obj.optString("ip_address", "192.168.1.200"),
                        port = obj.optInt("port", 9100),
                        usbDeviceName = obj.optString("usb_device_name", ""),
                        paperSize = obj.optString("paper_size", "80mm"),
                        autoCut = obj.optBoolean("auto_cut", true),
                        feedLinesBeforeCut = obj.optInt("feed_lines_before_cut", 3),
                        lineSpacing = obj.optInt("line_spacing", 32),
                        printCopies = obj.optInt("print_copies", 1),
                        soundBuzzer = obj.optBoolean("sound_buzzer", true),
                        showOrderType = obj.optBoolean("show_order_type", true),
                        showWaiterName = obj.optBoolean("show_waiter_name", true)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Synchronize with active server divisions.
     * Ensures every division from server has a corresponding config entry.
     */
    fun syncWithDivisions(context: Context, divisions: List<Division>): List<DivisionPrinterConfig> {
        val currentConfigs = getPrinterConfigs(context).toMutableList()
        val updated = mutableListOf<DivisionPrinterConfig>()

        for (div in divisions) {
            val existing = currentConfigs.find { it.divisionId == div.id }
            if (existing != null) {
                // Update name in case it changed on server
                updated.add(existing.copy(divisionName = div.name))
            } else {
                // Create default config
                val isNoPrint = div.name.contains("No Print", ignoreCase = true)
                updated.add(
                    DivisionPrinterConfig(
                        divisionId = div.id,
                        divisionName = div.name,
                        isEnabled = !isNoPrint,
                        connectionType = PrinterConnectionType.NETWORK,
                        ipAddress = "192.168.1." + (200 + (div.id.toIntOrNull() ?: 1)),
                        port = 9100,
                        usbDeviceName = "",
                        paperSize = "80mm"
                    )
                )
            }
        }

        saveAllConfigs(context, updated)
        return updated
    }

    /**
     * Save a single division printer config.
     */
    fun savePrinterConfig(context: Context, config: DivisionPrinterConfig) {
        val current = getPrinterConfigs(context).toMutableList()
        val index = current.indexOfFirst { it.divisionId == config.divisionId }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(config)
        }
        saveAllConfigs(context, current)
    }

    /**
     * Save full list of printer configs.
     */
    fun saveAllConfigs(context: Context, configs: List<DivisionPrinterConfig>) {
        val jsonArr = JSONArray()
        for (cfg in configs) {
            val obj = JSONObject().apply {
                put("division_id", cfg.divisionId)
                put("division_name", cfg.divisionName)
                put("is_enabled", cfg.isEnabled)
                put("connection_type", cfg.connectionType.name)
                put("ip_address", cfg.ipAddress)
                put("port", cfg.port)
                put("usb_device_name", cfg.usbDeviceName)
                put("paper_size", cfg.paperSize)
                put("auto_cut", cfg.autoCut)
                put("feed_lines_before_cut", cfg.feedLinesBeforeCut)
                put("line_spacing", cfg.lineSpacing)
                put("print_copies", cfg.printCopies)
                put("sound_buzzer", cfg.soundBuzzer)
                put("show_order_type", cfg.showOrderType)
                put("show_waiter_name", cfg.showWaiterName)
            }
            jsonArr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_PRINTER_CONFIGS, jsonArr.toString()).apply()
    }
}
