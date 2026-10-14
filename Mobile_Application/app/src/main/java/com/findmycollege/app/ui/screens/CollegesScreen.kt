package com.findmycollege.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findmycollege.app.data.model.College
import com.findmycollege.app.ui.components.CollegeCardItem
import com.findmycollege.app.ui.dialogs.CollegeDetailDialog
import com.findmycollege.app.ui.theme.*
import com.findmycollege.app.ui.viewmodel.CollegeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegesScreen(viewModel: CollegeViewModel) {
    val collegesList by viewModel.collegesList.collectAsState()
    val filteredList by viewModel.filteredList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val selectedStream by viewModel.selectedStream.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCollegeForDetails by viewModel.selectedCollegeForDetails.collectAsState()
    var expandedStateDropdown by remember { mutableStateOf(false) }
    val statesList by viewModel.statesList.collectAsState()

    val streamOptions = listOf("All", "Dental", "Engineering", "Law", "Management")

    val stateList = remember(statesList, collegesList) {
        if (statesList.size > 1) {
            statesList
        } else {
            val extracted = collegesList.mapNotNull { college ->
                val s = college.getStateDisplay().ifBlank { college.state.trim() }
                if (s.isNotBlank() && s.toIntOrNull() == null) s else null
            }
            .filter { it.isNotBlank() && it.toIntOrNull() == null }
            .distinct()
            .sorted()

            listOf("All States") + extracted
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp)
    ) {
        // Search & State Filter Row
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search by college name, city, or state...", color = Slate400, fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Slate400)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate950,
                    unfocusedContainerColor = Slate950,
                    focusedBorderColor = Blue500,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Horizontal Scrollable Stream Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(streamOptions) { stream ->
                    val isSelected = stream.equals(selectedStream, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onStreamFilterChange(stream) },
                        label = {
                            Text(
                                text = stream,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue600,
                            selectedLabelColor = Color.White,
                            containerColor = Slate950,
                            labelColor = Slate300
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Slate700,
                            selectedBorderColor = Blue500
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // State Filter Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedStateDropdown,
                onExpandedChange = { expandedStateDropdown = !expandedStateDropdown }
            ) {
                OutlinedTextField(
                    value = selectedState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter by State", color = Slate400, fontSize = 10.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStateDropdown) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedStateDropdown,
                    onDismissRequest = { expandedStateDropdown = false },
                    modifier = Modifier.background(Slate950)
                ) {
                    stateList.forEach { stateName ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stateName,
                                    color = if (stateName == selectedState) Blue400 else Color.White,
                                    fontWeight = if (stateName == selectedState) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                viewModel.onStateFilterChange(stateName)
                                expandedStateDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Directory Title & Count Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectedStream == "All") "NIRF Colleges Directory" else "NIRF $selectedStream Colleges Directory",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Browse and search full list of indexed institutions",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            Surface(
                color = Blue600.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Showing ${filteredList.size} of ${collegesList.size}",
                    color = Blue400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Blue400)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Fetching College Data...", color = Slate400, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredList, key = { it.getInstituteIdDisplay() }) { college ->
                    val isBookmarked = viewModel.isBookmarked(college)
                    val isCompared = viewModel.isInComparison(college)

                    CollegeCardItem(
                        college = college,
                        isBookmarked = isBookmarked,
                        isCompared = isCompared,
                        onToggleBookmark = { viewModel.toggleBookmark(college) },
                        onToggleCompare = { viewModel.toggleComparison(college) },
                        onViewDetails = { viewModel.selectCollegeForDetails(college) }
                    )
                }
            }
        }
    }

    // Detail Modal Dialog if selected
    selectedCollegeForDetails?.let { college ->
        val isBookmarked = viewModel.isBookmarked(college)
        CollegeDetailDialog(
            college = college,
            isBookmarked = isBookmarked,
            onToggleBookmark = { viewModel.toggleBookmark(college) },
            onDismiss = { viewModel.selectCollegeForDetails(null) }
        )
    }
}