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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

object ThermalPrinterManager {

    private const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"

    /**
     * Get the active IPv4 address of this Android device on local WiFi/LAN.
     */
    fun getLocalDeviceIp(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Fast Parallel Subnet Scanner:
     * Scans all 254 IP addresses on the local subnet for open RAW printer port (default 9100).
     * Finishes in ~1.5 - 2 seconds using concurrent Kotlin coroutines.
     */
    suspend fun scanNetworkPrinters(port: Int = 9100): List<String> = withContext(Dispatchers.IO) {
        val myIp = getLocalDeviceIp() ?: return@withContext emptyList()
        val subnet = myIp.substringBeforeLast(".") // e.g. "192.168.0"
        val foundList = Collections.synchronizedList(mutableListOf<String>())

        coroutineScope {
            (1..254).map { i ->
                async(Dispatchers.IO) {
                    val testIp = "$subnet.$i"
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(testIp, port), 450) // 450ms timeout
                            foundList.add(testIp)
                        }
                    } catch (_: Exception) {}
                }
            }.awaitAll()
        }

        foundList.sortedBy { ip ->
            ip.substringAfterLast(".").toIntOrNull() ?: 0
        }
    }

    // ESC/POS Commands
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val ESC_FONT_A = byteArrayOf(0x1B, 0x4D, 0x00) // Standard Font (12x24)
    private val ESC_FONT_B = byteArrayOf(0x1B, 0x4D, 0x01) // Small / Compressed Font (9x17)
    private val ESC_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11)
    private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1D, 0x21, 0x01) // 2x height, standard width (high visibility in kitchen)
    private val ESC_DOUBLE_WIDTH = byteArrayOf(0x1D, 0x21, 0x10) // standard height, 2x width
    private val ESC_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    private val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Cut full/partial with feed
    private val ESC_BUZZER = byteArrayOf(0x1B, 0x42, 0x02, 0x02, 0x07) // Beep 2 times + BEL

    /**
     * Smart word-wrapping helper that wraps lines cleanly at word boundaries
     * and indents continuation lines so words are never chopped in half.
     */
    fun wrapWords(text: String, maxWidth: Int): List<String> {
        if (text.length <= maxWidth) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLine.length + 1 + word.length <= maxWidth) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder("   ").append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * Format a row with left-aligned and right-aligned text distributed across full width.
     * Eliminates empty right section space and balances 80mm (48 col) / 58mm (32 col) paper.
     */
    fun formatTwoColumns(left: String, right: String, width: Int): String {
        val cleanLeft = left.trim()
        val cleanRight = right.trim()
        val totalLen = cleanLeft.length + cleanRight.length
        return if (totalLen >= width) {
            val maxLeft = (width - cleanRight.length - 1).coerceAtLeast(0)
            cleanLeft.take(maxLeft) + " " + cleanRight + "\n"
        } else {
            val spaces = width - totalLen
            cleanLeft + " ".repeat(spaces) + cleanRight + "\n"
        }
    }

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
        val doubleLine = "=".repeat(width)

        out.write(ESC_INIT)

        // Set Line Spacing (minimum 32 dots for clear breathing room)
        val ls = config.lineSpacing.coerceIn(32, 64)
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
        out.write("${config.divisionName}\n".toByteArray(Charsets.US_ASCII))
        out.write(ESC_BOLD_OFF)
        out.write("$doubleLine\n".toByteArray(Charsets.US_ASCII))

        out.write(ESC_ALIGN_LEFT)
        out.write(formatTwoColumns("Connection: ${config.connectionType.name}", "Paper: ${config.paperSize}", width).toByteArray(Charsets.US_ASCII))
        if (config.connectionType == PrinterConnectionType.NETWORK) {
            out.write(formatTwoColumns("IP Address: ${config.ipAddress}", "Port: ${config.port}", width).toByteArray(Charsets.US_ASCII))
        } else {
            out.write(formatTwoColumns("USB Device: ${config.usbDeviceName.ifBlank { "Default USB" }}", "", width).toByteArray(Charsets.US_ASCII))
        }
        out.write(formatTwoColumns("Auto-Cut: ${if (config.autoCut) "Enabled" else "Disabled"}", "Buzzer: ${if (config.soundBuzzer) "Enabled" else "Disabled"}", width).toByteArray(Charsets.US_ASCII))
        out.write(formatTwoColumns("Line Spacing: ${config.lineSpacing} dots", "Copies: ${config.printCopies}", width).toByteArray(Charsets.US_ASCII))

        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        out.write(formatTwoColumns("Date: $dateStr", "Time: $timeStr", width).toByteArray(Charsets.US_ASCII))
        out.write("$doubleLine\n".toByteArray(Charsets.US_ASCII))

        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_BOLD_ON)
        out.write(ESC_DOUBLE_HEIGHT)
        out.write("STATUS: PRINTER READY!\n".toByteArray(Charsets.US_ASCII))
        out.write(ESC_NORMAL_SIZE)
        out.write(ESC_BOLD_OFF)

        // Feed blank lines before cut (minimum 3 lines for clean cut)
        val feedCount = config.feedLinesBeforeCut.coerceIn(3, 8)
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

        val margin = if (config.paperSize == "58mm") "  " else "    "
        val contentWidth = if (config.paperSize == "58mm") 28 else 40
        val singleLine = margin + "-".repeat(contentWidth) + "\n"
        val doubleLine = margin + "=".repeat(contentWidth) + "\n"
        val copies = config.printCopies.coerceIn(1, 4)

        val fullStream = ByteArrayOutputStream()

        for (copyNum in 1..copies) {
            val out = ByteArrayOutputStream()
            out.write(ESC_INIT)
            out.write(ESC_NORMAL_SIZE)
            out.write(ESC_BOLD_OFF)

            // Set compact default line spacing
            out.write(byteArrayOf(0x1B, 0x32))

            // Kitchen Sound Buzzer
            if (config.soundBuzzer) {
                out.write(ESC_BUZZER)
            }

            out.write(ESC_ALIGN_LEFT)

            val isParcel = orderType.contains("parcel", ignoreCase = true) ||
                    orderType.contains("takeaway", ignoreCase = true) ||
                    orderType.contains("take away", ignoreCase = true) ||
                    orderType.contains("delivery", ignoreCase = true)

            if (isParcel) {
                out.write((margin + formatTwoColumns("", "[ TAKEAWAY ]", contentWidth)).toByteArray(Charsets.US_ASCII))
            }

            if (copies > 1) {
                out.write((margin + "(Copy $copyNum of $copies)\n").toByteArray(Charsets.US_ASCII))
            }

            // Top double separator line
            out.write(doubleLine.toByteArray(Charsets.US_ASCII))

            // 2. Table & KOT header (Bold & prominent!)
            out.write(ESC_BOLD_ON)
            val tableText = "TABLE: $tableName"
            val kotNum = if (kotToken.isNotBlank()) kotToken else orderId
            val cleanKotNum = kotNum.removePrefix("#")
            val kotText = "KOT: #$cleanKotNum"
            out.write((margin + formatTwoColumns(tableText, kotText, contentWidth)).toByteArray(Charsets.US_ASCII))
            out.write(ESC_BOLD_OFF)

            // Captain Name & Time (placed directly beneath KOT / Table)
            val resolvedCaptain = if (waiterName.isNotBlank()) {
                waiterName
            } else {
                try {
                    val auth = com.example.data.repository.AuthRepository.getInstance()
                    auth.init(context)
                    auth.currentUser.value?.displayName ?: auth.currentUser.value?.username ?: ""
                } catch (_: Exception) {
                    ""
                }
            }

            val timeStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())
            val timeText = "Time: $timeStr"
            val captText = if (resolvedCaptain.isNotBlank()) "Captain: $resolvedCaptain" else ""

            if (captText.isNotBlank()) {
                if (timeText.length + captText.length + 1 <= contentWidth) {
                    out.write((margin + formatTwoColumns(timeText, captText, contentWidth)).toByteArray(Charsets.US_ASCII))
                } else {
                    out.write((margin + timeText + "\n").toByteArray(Charsets.US_ASCII))
                    if (captText.length <= contentWidth) {
                        out.write((margin + formatTwoColumns("", captText, contentWidth)).toByteArray(Charsets.US_ASCII))
                    } else {
                        out.write((margin + captText + "\n").toByteArray(Charsets.US_ASCII))
                    }
                }
            } else {
                out.write((margin + timeText + "\n").toByteArray(Charsets.US_ASCII))
            }

            // Double separator line
            out.write(doubleLine.toByteArray(Charsets.US_ASCII))

            // 3. Items Column Header: "ITEM DESCRIPTION" left, "QTY" right (Bold!)
            out.write(ESC_BOLD_ON)
            out.write((margin + formatTwoColumns("ITEM DESCRIPTION", "QTY", contentWidth)).toByteArray(Charsets.US_ASCII))
            out.write(ESC_BOLD_OFF)

            // Single line under column header
            out.write(singleLine.toByteArray(Charsets.US_ASCII))

            // 4. Print each item: Item Name left, "1x" right (Bold item name)
            var totalQty = 0
            val maxLeftLen = if (config.paperSize == "58mm") 22 else 34
            val fontBWidth = if (config.paperSize == "58mm") 38 else 52

            for (item in items) {
                totalQty += item.quantity
                val qtyStr = "${item.quantity}x"

                // Item Name and Qty in Bold
                out.write(ESC_BOLD_ON)
                if (item.productName.length <= maxLeftLen) {
                    out.write((margin + formatTwoColumns(item.productName, qtyStr, contentWidth)).toByteArray(Charsets.US_ASCII))
                } else {
                    val chunks = item.productName.chunked(maxLeftLen)
                    out.write((margin + formatTwoColumns(chunks[0], qtyStr, contentWidth)).toByteArray(Charsets.US_ASCII))
                    for (extraChunk in chunks.drop(1)) {
                        out.write((margin + formatTwoColumns(extraChunk, "", contentWidth)).toByteArray(Charsets.US_ASCII))
                    }
                }
                out.write(ESC_BOLD_OFF)

                // Spice Level and Allergy combined on ONE row in SMALL Font B
                val spiceText = if (!item.spiceLevel.isNullOrBlank()) "Spice: ${item.spiceLevel.trim()}" else ""
                val allergyList = mutableListOf<String>()
                item.allergies?.filter { it.isNotBlank() }?.let { allergyList.addAll(it) }
                if (!item.customAllergies.isNullOrBlank()) {
                    allergyList.add(item.customAllergies.trim())
                }
                if (item.onionFlag == true && !allergyList.any { it.contains("onion", ignoreCase = true) }) {
                    allergyList.add("No Onion")
                }
                if (item.garlicFlag == true && !allergyList.any { it.contains("garlic", ignoreCase = true) }) {
                    allergyList.add("No Garlic")
                }
                val allergyText = if (allergyList.isNotEmpty()) "Allergy: ${allergyList.joinToString(", ")}" else ""

                val spiceAllergyRow = when {
                    spiceText.isNotBlank() && allergyText.isNotBlank() -> ">> $spiceText | $allergyText"
                    spiceText.isNotBlank() -> ">> $spiceText"
                    allergyText.isNotBlank() -> ">> $allergyText"
                    else -> ""
                }

                // Switch to Font B (Small Font) for Spice, Allergy, and Note
                out.write(ESC_FONT_B)

                if (spiceAllergyRow.isNotBlank()) {
                    val lines = wrapWords(spiceAllergyRow, fontBWidth)
                    for (line in lines) {
                        out.write((margin + "$line\n").toByteArray(Charsets.US_ASCII))
                    }
                }

                // Special Instructions / Note on a SEPARATE row (Small Font B)
                if (!item.specialInstructions.isNullOrBlank()) {
                    val noteText = ">> Note: ${item.specialInstructions.trim()}"
                    val lines = wrapWords(noteText, fontBWidth)
                    for (line in lines) {
                        out.write((margin + "$line\n").toByteArray(Charsets.US_ASCII))
                    }
                }

                // Switch back to Font A for next item
                out.write(ESC_FONT_A)
            }

            // Double separator line after items
            out.write(doubleLine.toByteArray(Charsets.US_ASCII))

            // 5. Total Items & Total Qty (Bold!)
            out.write(ESC_BOLD_ON)
            val summaryLeft = "Total Items: ${items.size}"
            val summaryRight = "Total Qty: $totalQty"
            out.write((margin + formatTwoColumns(summaryLeft, summaryRight, contentWidth)).toByteArray(Charsets.US_ASCII))
            out.write(ESC_BOLD_OFF)

            // Double separator line after summary
            out.write(doubleLine.toByteArray(Charsets.US_ASCII))

            // 6. Tight compact feed before cut (removes excess white space above and below)
            out.write("\n".toByteArray(Charsets.US_ASCII))

            // Auto-Cut: GS V 66 0 feeds exactly to cutter blade and cuts with zero extra white paper
            if (config.autoCut) {
                out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
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
