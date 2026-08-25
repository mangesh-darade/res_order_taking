package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TableItem
import com.example.ui.theme.*

@Composable
fun TableCard(
    table: TableItem,
    onClick: () -> Unit,
    onFreeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusLower = table.status.lowercase()

    // Background color based on table status
    val cardBg = when (statusLower) {
        "occupied" -> Color(0xFFA2E5FF)       // Light Blue from reference image
        "order-placed" -> Color(0xFFFFCC80)   // Warm Amber / Yellow
        "served" -> Color(0xFF81D4FA)         // Served Light Cyan
        "ready" -> Color(0xFFC8E6C9)          // Ready Light Green
        "reserved" -> Color(0xFFE1BEE7)       // Reserved Light Purple
        "free" -> Color(0xFFE0E0E0)           // Free Light Gray
        else -> Color(0xFFF1F5F9)             // Available Soft Off-White
    }

    val isAvailable = statusLower == "available"
    val isOccupiedOrActive = statusLower in listOf("occupied", "order-placed", "served", "ready")

    val tableTextColor = when {
        statusLower == "occupied" -> Color(0xFFB392C9)    // Pastel Purple from reference image
        statusLower == "order-placed" -> Color(0xFFD97706)
        statusLower == "served" -> Color(0xFF0284C7)
        statusLower == "ready" -> Color(0xFF16A34A)
        statusLower == "reserved" -> Color(0xFF9333EA)
        else -> PinkPrimary
    }

    val displayTime = formatDisplayTime(table.occupiedTime)
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = modifier
            .height(95.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = if (isAvailable) 1.5.dp else 0.dp,
                color = if (isAvailable) PinkPrimary.copy(alpha = 0.35f) else Color.Transparent,
                shape = cardShape
            )
            .clickable { onClick() }
            .testTag("table_card_${table.id}"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ================= LEFT SECTION (Time & Large Table Number with +) =================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Occupied Time (or Status label)
                if (!displayTime.isNullOrBlank()) {
                    Text(
                        text = displayTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                } else {
                    Text(
                        text = if (isAvailable) "Available" else table.status.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailable) PinkPrimary else Color.White.copy(alpha = 0.9f)
                    )
                }

                // Center: Big Table Number with centered '+' (e.g. T1 instead of Table 1)
                val shortTableNumber = table.tableNumber
                    .replace(Regex("(?i)table\\s*[-_]?"), "T")
                    .trim()

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortTableNumber,
                        fontSize = if (shortTableNumber.length > 3) 28.sp else 34.sp,
                        fontWeight = FontWeight.Black,
                        color = tableTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (isOccupiedOrActive) {
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ================= VERTICAL DIVIDER LINE =================
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0x33B392C9))
            )

            // ================= RIGHT SECTION (Person Icon & Guest Count) =================
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Guests",
                    tint = if (isAvailable) PinkPrimary.copy(alpha = 0.6f) else Color.White,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                val guests = if (table.guestsCount != null && table.guestsCount > 0) {
                    table.guestsCount.toString()
                } else if (isAvailable) {
                    "0"
                } else {
                    "2"
                }

                Text(
                    text = guests,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAvailable) PinkPrimary.copy(alpha = 0.8f) else Color.White
                )
            }
        }
    }
}

/**
 * Format "2026-08-25 12:39:59" or "12:39:59" to clean "12:39"
 */
private fun formatDisplayTime(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val trimmed = raw.trim()
        val timePart = if (trimmed.contains(" ")) {
            trimmed.split(" ")[1]
        } else {
            trimmed
        }
        val segments = timePart.split(":")
        if (segments.size >= 2) {
            "${segments[0]}:${segments[1]}"
        } else {
            timePart
        }
    } catch (e: Exception) {
        raw
    }
}
