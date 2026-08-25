package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    val cardBg = when (table.status.lowercase()) {
        "occupied" -> StatusOccupiedBg
        "reserved" -> StatusReservedBg
        "order-placed" -> StatusOrderPlacedBg
        "ready" -> StatusReadyBg
        "free" -> StatusFreeBg
        "served" -> StatusServedBg
        else -> StatusAvailableBg // available
    }

    val numberColor = when (table.status.lowercase()) {
        "available", "occupied" -> PinkPrimary
        "order-placed", "served" -> Color.White
        else -> TextDark
    }

    val cardShape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .height(110.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = if (table.status.lowercase() == "available") 1.dp else 0.dp,
                color = if (table.status.lowercase() == "available") PinkPrimary.copy(alpha = 0.5f) else Color.Transparent,
                shape = cardShape
            )
            .clickable { onClick() }
            .testTag("table_card_${table.id}"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Background huge faded table number
            Text(
                text = table.tableNumber,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = numberColor.copy(alpha = 0.2f),
                modifier = Modifier.align(Alignment.Center)
            )

            // Top Row: Time or Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!table.occupiedTime.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = TextDark.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = table.occupiedTime!!,
                            fontSize = 10.sp,
                            color = TextDark.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Status Pill
                val pillText = when (table.status.lowercase()) {
                    "available" -> "Available"
                    "occupied" -> "Occupied"
                    "reserved" -> "Reserved"
                    "order-placed" -> "Order Placed"
                    "ready" -> "Ready"
                    "free" -> "Free"
                    "served" -> "Served"
                    else -> table.status.uppercase()
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (table.status.lowercase()) {
                        "available" -> PinkLightBg
                        "free" -> Color.White
                        else -> Color.White.copy(alpha = 0.85f)
                    },
                    modifier = Modifier.clickable(enabled = table.status.lowercase() == "free" || table.status.lowercase() == "reserved") {
                        onFreeClick?.invoke()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (table.status.lowercase() == "free") {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = PinkPrimary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = pillText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (table.status.lowercase() == "available") PinkPrimary else TextDark
                        )
                    }
                }
            }

            // Center Content: Table Number & "+" icon
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = table.tableNumber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = numberColor
                )

                if (table.status.lowercase() in listOf("occupied", "order-placed", "served")) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        modifier = Modifier.size(18.dp),
                        tint = if (table.status.lowercase() == "occupied") PinkPrimary else Color.White
                    )
                } else if (table.status.lowercase() == "ready") {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Ready Served",
                        modifier = Modifier.size(18.dp),
                        tint = TextDark
                    )
                } else if (table.status.lowercase() == "reserved") {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Reserved",
                        modifier = Modifier.size(18.dp),
                        tint = TextDark
                    )
                }
            }

            // Bottom Right: Guest count
            if (table.guestsCount != null && table.guestsCount > 0) {
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Guests",
                        modifier = Modifier.size(12.dp),
                        tint = if (table.status.lowercase() in listOf("order-placed", "served")) Color.White else TextDark
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = table.guestsCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (table.status.lowercase() in listOf("order-placed", "served")) Color.White else TextDark
                    )
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
