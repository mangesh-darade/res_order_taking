package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.PrinterSettingsManager
import com.example.data.model.Division
import com.example.data.model.DivisionPrinterConfig
import com.example.data.model.PrinterConnectionType
import com.example.data.repository.RestaurantRepository
import com.example.ui.theme.PinkLightBg
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.util.ThermalPrinterManager
import kotlinx.coroutines.launch

@Composable
fun PrinterSettingsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { RestaurantRepository() }

    var isLoading by remember { mutableStateOf(true) }
    var printerConfigs by remember { mutableStateOf<List<DivisionPrinterConfig>>(emptyList()) }
    var availableUsbPrinters by remember { mutableStateOf<List<String>>(emptyList()) }
    var testingDivisionId by remember { mutableStateOf<String?>(null) }
    var testResultMap by remember { mutableStateOf<Map<String, Pair<Boolean, String>>>(emptyMap()) }

    fun refreshData() {
        coroutineScope.launch {
            isLoading = true
            availableUsbPrinters = ThermalPrinterManager.getAvailableUsbPrinters(context)
            val res = repository.fetchDivisions()
            val divisions = res.getOrDefault(
                listOf(
                    Division("1", "Kitchen Printer", "171001"),
                    Division("2", "Bar Printer", "171002"),
                    Division("3", "Lounge Printer", "171003"),
                    Division("4", "Terrace Printer", "171004"),
                    Division("5", "No Print", "171005")
                )
            )
            printerConfigs = PrinterSettingsManager.syncWithDivisions(context, divisions)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Division Printers (Thermal ESC/POS)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Configure IP/LAN or USB printer per division station",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            IconButton(
                onClick = { refreshData() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PinkPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PinkPrimary, strokeWidth = 2.dp)
            }
        } else if (printerConfigs.isEmpty()) {
            Text(
                text = "No product divisions found.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            printerConfigs.forEachIndexed { index, config ->
                DivisionPrinterCard(
                    config = config,
                    availableUsbPrinters = availableUsbPrinters,
                    isTesting = testingDivisionId == config.divisionId,
                    testResult = testResultMap[config.divisionId],
                    onConfigChanged = { updated ->
                        val list = printerConfigs.toMutableList()
                        list[index] = updated
                        printerConfigs = list
                        PrinterSettingsManager.savePrinterConfig(context, updated)
                    },
                    onTestPrint = { cfg ->
                        coroutineScope.launch {
                            testingDivisionId = cfg.divisionId
                            val result = ThermalPrinterManager.testPrint(context, cfg)
                            val success = result.isSuccess
                            val msg = if (success) {
                                result.getOrNull() ?: "Test slip printed successfully!"
                            } else {
                                result.exceptionOrNull()?.localizedMessage ?: "Failed to connect to printer"
                            }
                            testResultMap = testResultMap + (cfg.divisionId to Pair(success, msg))
                            testingDivisionId = null
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    PrinterSettingsManager.saveAllConfigs(context, printerConfigs)
                    Toast.makeText(context, "All printer configurations saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE ALL PRINTER CONFIGURATIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DivisionPrinterCard(
    config: DivisionPrinterConfig,
    availableUsbPrinters: List<String>,
    isTesting: Boolean,
    testResult: Pair<Boolean, String>?,
    onConfigChanged: (DivisionPrinterConfig) -> Unit,
    onTestPrint: (DivisionPrinterConfig) -> Unit
) {
    var expandedUsbMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (config.isEnabled) Color(0xFFFBFBFB) else Color(0xFFF3F3F3)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (config.isEnabled) PinkPrimary.copy(alpha = 0.3f) else Color(0xFFDDDDDD))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Division Name + Enable Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (config.connectionType == PrinterConnectionType.NETWORK) Icons.Default.Print else Icons.Default.Usb,
                        contentDescription = null,
                        tint = if (config.isEnabled) PinkPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = config.divisionName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (config.isEnabled) TextDark else TextMuted
                    )
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { onConfigChanged(config.copy(isEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PinkPrimary
                    )
                )
            }

            if (config.isEnabled) {
                Spacer(modifier = Modifier.height(10.dp))

                // Connection Type Selector (LAN vs USB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isNet = config.connectionType == PrinterConnectionType.NETWORK
                    FilterChip(
                        selected = isNet,
                        onClick = { onConfigChanged(config.copy(connectionType = PrinterConnectionType.NETWORK)) },
                        label = { Text("LAN / IP (Network)", fontSize = 11.sp, fontWeight = if (isNet) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )

                    val isUsb = config.connectionType == PrinterConnectionType.USB
                    FilterChip(
                        selected = isUsb,
                        onClick = { onConfigChanged(config.copy(connectionType = PrinterConnectionType.USB)) },
                        label = { Text("USB Printer", fontSize = 11.sp, fontWeight = if (isUsb) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = { Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Network IP & Port Fields
                if (config.connectionType == PrinterConnectionType.NETWORK) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = config.ipAddress,
                            onValueChange = { onConfigChanged(config.copy(ipAddress = it.trim())) },
                            label = { Text("Printer IP Address", fontSize = 11.sp) },
                            placeholder = { Text("192.168.1.200") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(2.2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkPrimary,
                                focusedLabelColor = PinkPrimary
                            )
                        )

                        OutlinedTextField(
                            value = config.port.toString(),
                            onValueChange = {
                                val p = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 9100
                                onConfigChanged(config.copy(port = p))
                            },
                            label = { Text("Port", fontSize = 11.sp) },
                            placeholder = { Text("9100") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkPrimary,
                                focusedLabelColor = PinkPrimary
                            )
                        )
                    }
                } else {
                    // USB Device Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (config.usbDeviceName.isNotBlank()) config.usbDeviceName else (availableUsbPrinters.firstOrNull() ?: "No USB printer detected"),
                            onValueChange = { onConfigChanged(config.copy(usbDeviceName = it)) },
                            label = { Text("Connected USB Printer", fontSize = 11.sp) },
                            readOnly = availableUsbPrinters.isNotEmpty(),
                            trailingIcon = {
                                if (availableUsbPrinters.isNotEmpty()) {
                                    IconButton(onClick = { expandedUsbMenu = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select USB")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (availableUsbPrinters.isNotEmpty()) expandedUsbMenu = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkPrimary,
                                focusedLabelColor = PinkPrimary
                            )
                        )

                        DropdownMenu(
                            expanded = expandedUsbMenu,
                            onDismissRequest = { expandedUsbMenu = false }
                        ) {
                            availableUsbPrinters.forEach { dev ->
                                DropdownMenuItem(
                                    text = { Text(dev, fontSize = 12.sp) },
                                    onClick = {
                                        onConfigChanged(config.copy(usbDeviceName = dev))
                                        expandedUsbMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Paper Size Selection (58mm vs 80mm) + Test Print Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Paper:", fontSize = 11.sp, color = TextMuted)
                        listOf("80mm", "58mm").forEach { size ->
                            val sel = config.paperSize == size
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (sel) PinkLightBg else Color.Transparent,
                                modifier = Modifier
                                    .border(1.dp, if (sel) PinkPrimary else Color(0xFFDDDDDD), RoundedCornerShape(6.dp))
                                    .clickable { onConfigChanged(config.copy(paperSize = size)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = size,
                                    fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) PinkPrimary else TextDark
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onTestPrint(config) },
                        enabled = !isTesting,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = PinkPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 11.sp, color = PinkPrimary)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp), tint = PinkPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TEST PRINT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
                        }
                    }
                }

                // Test result feedback
                testResult?.let { (success, msg) ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (success) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (success) Color(0xFF86EFAC) else Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (success) Color(0xFF16A34A) else Color(0xFFDC2626),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = if (success) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }
    }
}
