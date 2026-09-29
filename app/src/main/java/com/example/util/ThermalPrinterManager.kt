package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.example.data.model.DivisionPrinterConfig
import com.example.data.model.OrderItem
import com.example.data.model.PrinterConnectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermalPrinterManager {

    private const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"

    // ESC/POS Commands
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val ESC_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11)
    private val ESC_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    private val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Cut full/partial with feed
    private val ESC_BUZZER = byteArrayOf(0x1B, 0x42, 0x02, 0x02, 0x07) // Beep 2 times + BEL

    /**
     * Send byte array directly over Network (LAN / WiFi / Ethernet IP:Port).
     */
    suspend fun sendBytesOverNetwork(
        ip: String,
        port: Int,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 4000) // 4 sec timeout
                val stream = socket.getOutputStream()
                stream.write(bytes)
                stream.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get list of connected USB Printer device names.
     */
    fun getAvailableUsbPrinters(context: Context): List<String> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        val names = mutableListOf<String>()
        for ((name, device) in usbManager.deviceList) {
            val isPrinter = device.deviceClass == UsbConstants.USB_CLASS_PER_INTERFACE ||
                    (0 until device.interfaceCount).any { i ->
                        device.getInterface(i).interfaceClass == 7 // 7 is USB Printer Class
                    }
            val label = if (device.productName.isNullOrBlank()) device.deviceName else "${device.productName} ($name)"
            names.add(label)
        }
        return names
    }

    /**
     * Send byte array directly to a USB thermal printer.
     */
    suspend fun sendBytesOverUsb(
        context: Context,
        targetDeviceName: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return@withContext Result.failure(Exception("USB Manager not available"))

        val device = usbManager.deviceList.values.firstOrNull { d ->
            val label = if (d.productName.isNullOrBlank()) d.deviceName else "${d.productName} (${d.deviceName})"
            label.contains(targetDeviceName, ignoreCase = true) || d.deviceName.equals(targetDeviceName, ignoreCase = true)
        } ?: return@withContext Result.failure(Exception("USB printer not found or disconnected"))

        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
            usbManager.requestPermission(device, permissionIntent)
            return@withContext Result.failure(Exception("USB Permission required. Please approve the prompt and try again."))
        }

        try {
            val connection = usbManager.openDevice(device)
                ?: return@withContext Result.failure(Exception("Cannot open connection to USB device"))

            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                connection.claimInterface(usbInterface, true)
                for (j in 0 until usbInterface.endpointCount) {
                    val endpoint = usbInterface.getEndpoint(j)
                    if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                        connection.bulkTransfer(endpoint, bytes, bytes.size, 5000)
                        connection.releaseInterface(usbInterface)
                        connection.close()
                        return@withContext Result.success(Unit)
                    }
                }
                connection.releaseInterface(usbInterface)
            }
            connection.close()
            Result.failure(Exception("No bulk output endpoint found on USB printer"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Print a test receipt on the configured printer.
     */
    suspend fun testPrint(
        context: Context,
        config: DivisionPrinterConfig
    ): Result<String> {
        val out = ByteArrayOutputStream()
        val width = if (config.paperSize == "58mm") 32 else 48
        val line = "-".repeat(width)

        out.write(ESC_INIT)

        // Set Line Spacing (ESC 3 n)
        val ls = config.lineSpacing.coerceIn(16, 64)
        out.write(byteArrayOf(0x1B, 0x33, ls.toByte()))

        // Sound Buzzer if enabled
        if (config.soundBuzzer) {
            out.write(ESC_BUZZER)
        }

        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_BOLD_ON)
        out.write(ESC_DOUBLE_SIZE)
        out.write("*** TEST PRINT ***\n".toByteArray(Charsets.US_ASCII))
        out.write(ESC_NORMAL_SIZE)
        out.write(ESC_BOLD_OFF)
        out.write("${config.divisionName}\n".toByteArray(Charsets.US_ASCII))
        out.write("$line\n".toByteArray(Charsets.US_ASCII))

        out.write(ESC_ALIGN_LEFT)
        out.write("Connection  : ${config.connectionType.name}\n".toByteArray(Charsets.US_ASCII))
        if (config.connectionType == PrinterConnectionType.NETWORK) {
            out.write("IP Address  : ${config.ipAddress}:${config.port}\n".toByteArray(Charsets.US_ASCII))
        } else {
            out.write("USB Device  : ${config.usbDeviceName.ifBlank { "Default USB" }}\n".toByteArray(Charsets.US_ASCII))
        }
        out.write("Paper Size  : ${config.paperSize}\n".toByteArray(Charsets.US_ASCII))
        out.write("Auto-Cut    : ${if (config.autoCut) "Enabled" else "Disabled"}\n".toByteArray(Charsets.US_ASCII))
        out.write("Line Spacing: ${config.lineSpacing} dots\n".toByteArray(Charsets.US_ASCII))
        out.write("Buzzer/Beep : ${if (config.soundBuzzer) "Enabled" else "Disabled"}\n".toByteArray(Charsets.US_ASCII))
        out.write("Copies      : ${config.printCopies}\n".toByteArray(Charsets.US_ASCII))
        val dateStr = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        out.write("Date & Time : $dateStr\n".toByteArray(Charsets.US_ASCII))
        out.write("$line\n".toByteArray(Charsets.US_ASCII))

        out.write(ESC_ALIGN_CENTER)
        out.write("Status: PRINTER READY!\n".toByteArray(Charsets.US_ASCII))

        // Feed blank lines before cut
        val feedCount = config.feedLinesBeforeCut.coerceIn(1, 8)
        out.write("\n".repeat(feedCount).toByteArray(Charsets.US_ASCII))

        // Auto Cut if enabled
        if (config.autoCut) {
            out.write(ESC_CUT_PAPER)
        }

        val bytes = out.toByteArray()
        val result = if (config.connectionType == PrinterConnectionType.NETWORK) {
            sendBytesOverNetwork(config.ipAddress, config.port, bytes)
        } else {
            sendBytesOverUsb(context, config.usbDeviceName, bytes)
        }

        return if (result.isSuccess) {
            Result.success("Test print sent successfully to ${config.divisionName}!")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to send test print"))
        }
    }

    /**
     * Print KOT Slip for a specific division station.
     */
    suspend fun printKotSlip(
        context: Context,
        config: DivisionPrinterConfig,
        orderId: String,
        tableName: String,
        items: List<OrderItem>,
        kotToken: String = "",
        orderType: String = "Dine in",
        waiterName: String = ""
    ): Result<String> {
        if (!config.isEnabled || items.isEmpty()) {
            return Result.success("Skipped (Disabled or No Items)")
        }

        val width = if (config.paperSize == "58mm") 32 else 48
        val line = "-".repeat(width)
        val copies = config.printCopies.coerceIn(1, 4)

        val fullStream = ByteArrayOutputStream()

        for (copyNum in 1..copies) {
            val out = ByteArrayOutputStream()
            out.write(ESC_INIT)

            // Set Line Spacing (ESC 3 n)
            val ls = config.lineSpacing.coerceIn(16, 64)
            out.write(byteArrayOf(0x1B, 0x33, ls.toByte()))

            // Kitchen Sound Buzzer
            if (config.soundBuzzer) {
                out.write(ESC_BUZZER)
            }

            out.write(ESC_ALIGN_CENTER)

            // Prominent Order Type Badge (PARCEL / TAKEAWAY vs DINE-IN)
            if (config.showOrderType) {
                val isParcel = orderType.contains("parcel", ignoreCase = true) ||
                        orderType.contains("takeaway", ignoreCase = true) ||
                        orderType.contains("take away", ignoreCase = true) ||
                        orderType.contains("delivery", ignoreCase = true)

                out.write(ESC_BOLD_ON)
                out.write(ESC_DOUBLE_SIZE)
                if (isParcel) {
                    out.write("*** [ PARCEL / TAKEAWAY ] ***\n".toByteArray(Charsets.US_ASCII))
                } else {
                    out.write("[ DINE-IN ]\n".toByteArray(Charsets.US_ASCII))
                }
                out.write(ESC_NORMAL_SIZE)
                out.write(ESC_BOLD_OFF)
            }

            // KOT Station Header
            out.write(ESC_BOLD_ON)
            out.write("K.O.T. [${config.divisionName.uppercase()}]\n".toByteArray(Charsets.US_ASCII))
            if (copies > 1) {
                out.write("(Copy $copyNum of $copies)\n".toByteArray(Charsets.US_ASCII))
            }
            out.write(ESC_BOLD_OFF)

            val timeStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())
            out.write("Order: $orderId | Table: $tableName\n".toByteArray(Charsets.US_ASCII))
            if (kotToken.isNotBlank()) {
                out.write("Token: $kotToken | Time: $timeStr\n".toByteArray(Charsets.US_ASCII))
            } else {
                out.write("Time: $timeStr\n".toByteArray(Charsets.US_ASCII))
            }

            if (config.showWaiterName && waiterName.isNotBlank()) {
                out.write("Waiter/Captain: $waiterName\n".toByteArray(Charsets.US_ASCII))
            }

            out.write("$line\n".toByteArray(Charsets.US_ASCII))

            // Items Header
            out.write(ESC_ALIGN_LEFT)
            out.write(ESC_BOLD_ON)
            if (width == 32) {
                out.write(String.format(Locale.US, "%-4s %-20s %5s\n", "QTY", "ITEM", "STAT").toByteArray(Charsets.US_ASCII))
            } else {
                out.write(String.format(Locale.US, "%-5s %-32s %8s\n", "QTY", "ITEM DESCRIPTION", "STATUS").toByteArray(Charsets.US_ASCII))
            }
            out.write(ESC_BOLD_OFF)
            out.write("$line\n".toByteArray(Charsets.US_ASCII))

            // Print each item for this station
            for (item in items) {
                val qtyStr = "${item.quantity}x"
                val name = if (item.productName.length > (width - 12)) item.productName.take(width - 12) else item.productName
                out.write(ESC_BOLD_ON)
                if (width == 32) {
                    out.write(String.format(Locale.US, "%-4s %-26s\n", qtyStr, name).toByteArray(Charsets.US_ASCII))
                } else {
                    out.write(String.format(Locale.US, "%-5s %-41s\n", qtyStr, name).toByteArray(Charsets.US_ASCII))
                }
                out.write(ESC_BOLD_OFF)

                // Add special instructions / spice level
                if (!item.specialInstructions.isNullOrBlank()) {
                    out.write("     Note: ${item.specialInstructions}\n".toByteArray(Charsets.US_ASCII))
                }
                if (!item.spiceLevel.isNullOrBlank()) {
                    out.write("     Spice: ${item.spiceLevel}\n".toByteArray(Charsets.US_ASCII))
                }
            }

            out.write("$line\n".toByteArray(Charsets.US_ASCII))

            // Feed blank lines before cut
            val feedCount = config.feedLinesBeforeCut.coerceIn(1, 8)
            out.write("\n".repeat(feedCount).toByteArray(Charsets.US_ASCII))

            // Auto-Cut
            if (config.autoCut) {
                out.write(ESC_CUT_PAPER)
            }

            fullStream.write(out.toByteArray())
        }

        val bytes = fullStream.toByteArray()
        val result = if (config.connectionType == PrinterConnectionType.NETWORK) {
            sendBytesOverNetwork(config.ipAddress, config.port, bytes)
        } else {
            sendBytesOverUsb(context, config.usbDeviceName, bytes)
        }

        return if (result.isSuccess) {
            Result.success("KOT printed on ${config.divisionName} ($copies copy)")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("KOT Print Failed"))
        }
    }
}
