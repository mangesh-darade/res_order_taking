package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
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
import androidx.compose.ui.text.style.TextOverflow
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

    val cardBg = when (statusLower) {
        "occupied" -> StatusOccupiedBg
        "reserved" -> StatusReservedBg
        "order-placed" -> StatusOrderPlacedBg
        "ready" -> StatusReadyBg
        "free" -> StatusFreeBg
        "served" -> StatusServedBg
        else -> StatusAvailableBg // available
    }

    val isDarkBackground = statusLower in listOf("order-placed", "served")
    val titleColor = when {
        isDarkBackground -> Color.White
        statusLower == "available" -> PinkPrimary
        statusLower == "occupied" -> PinkPrimary
        else -> TextDark
    }

    val cardShape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .height(115.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = if (statusLower == "available") 1.5.dp else 0.dp,
                color = if (statusLower == "available") PinkPrimary.copy(alpha = 0.4f) else Color.Transparent,
                shape = cardShape
            )
            .clickable { onClick() }
            .testTag("table_card_${table.id}"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP ROW: Occupied Time (Left) & Status Pill (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!table.occupiedTime.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = if (isDarkBackground) Color.White.copy(alpha = 0.85f) else TextDark.copy(alpha = 0.7f)
                        )
                        Text(
                            text = table.occupiedTime!!,
                            fontSize = 9.sp,
                            color = if (isDarkBackground) Color.White.copy(alpha = 0.9f) else TextDark.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Status Pill Badge
                val pillText = when (statusLower) {
                    "available" -> "Available"
                    "occupied" -> "Occupied"
                    "reserved" -> "Reserved"
                    "order-placed" -> "Placed"
                    "ready" -> "Ready"
                    "free" -> "Free"
                    "served" -> "Served"
                    else -> table.status.uppercase()
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (statusLower) {
                        "available" -> PinkLightBg
                        "free" -> Color.White
                        else -> Color.White.copy(alpha = 0.9f)
                    },
                    modifier = Modifier.clickable(enabled = statusLower == "free" || statusLower == "reserved") {
                        onFreeClick?.invoke()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (statusLower == "free") {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(9.dp),
                                tint = PinkPrimary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = pillText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (statusLower == "available") PinkPrimary else TextDark
                        )
                    }
                }
            }

            // 2. CENTER: Clean, Bold Table Name (No overlapping duplicate texts)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = table.tableNumber,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. BOTTOM ROW: Status Hint / Guest Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (statusLower in listOf("occupied", "order-placed", "served")) {
                    Text(
                        text = "+ Add Item",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkBackground) Color.White.copy(alpha = 0.9f) else PinkPrimary
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (table.guestsCount != null && table.guestsCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Guests",
                            modifier = Modifier.size(11.dp),
                            tint = if (isDarkBackground) Color.White else TextDark.copy(alpha = 0.75f)
                        )
                        Text(
                            text = table.guestsCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkBackground) Color.White else TextDark
                        )
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

