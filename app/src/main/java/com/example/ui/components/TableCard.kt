package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableCard(
    table: TableItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFreeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusLower = table.status.lowercase()

    // Background color based on table status
    val cardBg = when (statusLower) {
        "occupied" -> Color(0xFFA2E5FF)
        "order-placed", "placed", "kitchen", "kot_sent" -> Color(0xFFFFCC80)
        "ready" -> Color(0xFFC8E6C9)
        "served" -> Color(0xFFFF7EB6)
        "reserved" -> Color(0xFFFFFF99) // Exact Yellow per specification for Reserved
        "free" -> Color(0xFFFFCDD2)    // Exact Red tint for Free
        else -> Color(0xFFF8FAFC)
    }

    val isAvailable = statusLower == "available"

    val tableTextColor = when {
        statusLower == "occupied" -> Color(0xFF0284C7)
        statusLower in listOf("order-placed", "placed", "kitchen", "kot_sent") -> Color(0xFFD97706)
        statusLower == "ready" -> Color(0xFF16A34A)
        statusLower == "served" -> Color(0xFFBE185D)
        statusLower == "reserved" -> Color(0xFF856404) // Dark Gold/Yellow text
        statusLower == "free" -> Color(0xFFB71C1C)
        else -> PinkPrimary
    }

    val displayTime = formatDisplayTime(table.occupiedTime)
    val reservedUntilShort = if (statusLower == "reserved") {
        formatReservedUntilShort(table.reservedUntil)
    } else {
        null
    }
    val headerLabel = when {
        statusLower == "reserved" && !reservedUntilShort.isNullOrBlank() -> "Until $reservedUntilShort"
        !displayTime.isNullOrBlank() -> displayTime
        isAvailable -> "Available"
        else -> table.status.uppercase()
    }
    val cardShape = RoundedCornerShape(14.dp)

    Card(
        modifier = modifier
            .height(108.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 1.5.dp,
                color = if (isAvailable) PinkPrimary.copy(alpha = 0.4f) else tableTextColor.copy(alpha = 0.5f),
                shape = cardShape
            )
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick?.invoke() }
            )
            .testTag("table_card_${table.id}"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Status Name & Guests Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAvailable) PinkPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = headerLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAvailable) PinkPrimary else tableTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Guests",
                        tint = tableTextColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = (table.guestsCount ?: 0).toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tableTextColor
                    )
                }
            }

            // Center Table Name Text Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = table.tableNumber,
                        fontSize = when {
                            table.tableNumber.length > 7 -> 14.sp
                            table.tableNumber.length > 4 -> 17.sp
                            else -> 22.sp
                        },
                        fontWeight = FontWeight.Black,
                        color = tableTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (statusLower == "reserved") {
                        if (!table.reservedBy.isNullOrBlank()) {
                            Text(
                                text = table.reservedBy,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tableTextColor.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                        if (!reservedUntilShort.isNullOrBlank()) {
                            Text(
                                text = reservedUntilShort,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = tableTextColor.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
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

/** Same day → "6:30 PM"; other day → "28 Aug 6:30 PM" */
private fun formatReservedUntilShort(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val cleaned = raw.trim().replace('T', ' ').let {
            if (Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$""").matches(it)) "$it:00" else it
        }
        val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        inFmt.isLenient = false
        val parsed = inFmt.parse(cleaned) ?: return formatDisplayTime(raw)
        val cal = java.util.Calendar.getInstance().apply { time = parsed }
        val today = java.util.Calendar.getInstance()
        val sameDay = cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
        val outFmt = if (sameDay) {
            java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
        } else {
            java.text.SimpleDateFormat("d MMM h:mm a", java.util.Locale.US)
        }
        outFmt.format(parsed)
    } catch (_: Exception) {
        formatDisplayTime(raw)
    }
}
