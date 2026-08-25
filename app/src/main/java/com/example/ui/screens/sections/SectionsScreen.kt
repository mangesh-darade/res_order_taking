package com.example.ui.screens.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MainTab
import com.example.ui.components.SharedTabStrip
import com.example.ui.components.TopHeaderBar
import com.example.ui.theme.BackgroundGray
import com.example.ui.theme.PinkPrimary
import com.example.ui.theme.TextDark

@Composable
fun SectionsScreen(
    onNavigateToTables: (sectionId: String, sectionName: String, subsectionId: String?, subsectionName: String?) -> Unit,
    onTabSelected: (MainTab) -> Unit,
    onLogoutClick: (() -> Unit)? = null,
    viewModel: SectionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Auto-navigate if selected section has no subsections
    LaunchedEffect(uiState.selectedSection, uiState.subsections, uiState.isLoading) {
        val selected = uiState.selectedSection
        if (!uiState.isLoading && selected != null && (selected.subsectionsCount == 0 || uiState.subsections.isEmpty())) {
            // Uncomment if immediate auto-redirect is desired, or let user tap section card below
        }
    }

    Scaffold(
        topBar = { TopHeaderBar(onLogoutClick = onLogoutClick) },
        containerColor = BackgroundGray
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Shared 3-tab strip
            SharedTabStrip(
                selectedTab = MainTab.SECTIONS,
                onTabSelected = onTabSelected
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Label "Select Section:"
                Text(
                    text = "Select Section:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("select_section_label")
                )

                // Wide pink-border dropdown (~42dp tall, large text)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(width = 2.dp, color = PinkPrimary, shape = RoundedCornerShape(8.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 16.dp)
                        .testTag("section_dropdown_selector"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.selectedSection?.name ?: "Select Section",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = PinkPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color.White)
                    ) {
                        uiState.sections.forEach { sec ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sec.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextDark
                                    )
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    viewModel.selectSection(sec)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PinkPrimary)
                    }
                } else {
                    val currentSec = uiState.selectedSection
                    val subList = uiState.subsections

                    if (subList.isNotEmpty()) {
                        Text(
                            text = "Subsections in ${currentSec?.name}:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(subList) { sub ->
                                Card(
                                    modifier = Modifier
                                        .height(110.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(width = 2.dp, color = PinkPrimary, shape = RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (currentSec != null) {
                                                onNavigateToTables(currentSec.id, currentSec.name, sub.id, sub.name)
                                            }
                                        }
                                        .testTag("subsection_card_${sub.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sub.name,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PinkPrimary,
                                            modifier = Modifier.testTag("subsection_name_${sub.id}")
                                        )
                                    }
                                }
                            }
                        }
                    } else if (currentSec != null) {
                        // Section has no subsections -> Single card to view tables directly
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .height(140.dp)
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(width = 2.dp, color = PinkPrimary, shape = RoundedCornerShape(12.dp))
                                    .clickable {
                                        onNavigateToTables(currentSec.id, currentSec.name, null, null)
                                    }
                                    .testTag("section_direct_card_${currentSec.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentSec.name,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PinkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap to view tables",
                                        fontSize = 14.sp,
                                        color = TextDark.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
