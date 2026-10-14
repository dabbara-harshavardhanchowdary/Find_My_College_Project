package com.findmycollege.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    viewModel: CollegeViewModel,
    onNavigateToColleges: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val collegesList by viewModel.collegesList.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedCollegeIds.collectAsState()
    val comparisonSlots by viewModel.comparisonSlots.collectAsState()
    val selectedCollegeForDetails by viewModel.selectedCollegeForDetails.collectAsState()

    val currentUser = FirebaseAuth.getInstance().currentUser
    var heroSearchQuery by remember { mutableStateOf("") }

    val top3Colleges = remember(collegesList) {
        collegesList.take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎓", fontSize = 24.sp)
                Column {
                    Text(
                        text = "Find My College",
                        color = Blue400,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Smart Admissions & Insights",
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }
            }

            // Right Action Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bookmarks Quick View Button
                IconButton(
                    onClick = onNavigateToSaved,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Slate800, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate700, RoundedCornerShape(12.dp))
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Text("🔖", fontSize = 16.sp)
                        if (bookmarkedIds.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${bookmarkedIds.size}",
                                    color = Slate950,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Profile Avatar Button
                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Blue600.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, Blue500.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = currentUser?.displayName?.firstOrNull()?.toString()?.uppercase() ?: "S",
                        color = Blue400,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // HERO BANNER SECTION
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Slate900, Color(0xFF1E1B4B).copy(alpha = 0.6f), Slate900)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.dp, Slate800, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Blue600.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Official Engineering colleges verified from NIRF.",
                        color = Blue400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Discover & Compare Top Engineering Colleges",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    lineHeight = 26.sp
                )

                Text(
                    text = "Search verified institutions across India. Analyze NIRF rankings, NAAC ratings, cutoffs, fee structures, and compare colleges.",
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Hero Search Input
                OutlinedTextField(
                    value = heroSearchQuery,
                    onValueChange = {
                        heroSearchQuery = it
                        viewModel.onSearchQueryChange(it)
                    },
                    placeholder = { Text("Quick search college, city, or state...", color = Slate400, fontSize = 11.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    trailingIcon = {
                        if (heroSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                heroSearchQuery = ""
                                viewModel.onSearchQueryChange("")
                            }) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.onSearchQueryChange(heroSearchQuery)
                            onNavigateToColleges()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("Search Directory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = onNavigateToColleges,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("🏛️ Explore All →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // QUICK STATS OVERVIEW CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickStatCard(
                icon = "📊",
                title = "${if (collegesList.isNotEmpty()) collegesList.size else 100}+ Verified",
                subtitle = "Indexed institutions",
                modifier = Modifier.weight(1f)
            )

            QuickStatCard(
                icon = "🏆",
                title = "NIRF Verified",
                subtitle = "Official rank metrics",
                modifier = Modifier.weight(1f)
            )

            QuickStatCard(
                icon = "⚖️",
                title = "Compare 5",
                subtitle = "Side-by-side specs",
                modifier = Modifier.weight(1f)
            )
        }

        // TOP 3 HIGHLIGHTS
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏆 Top Ranked Engineering Colleges",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Nationwide highest ranking engineering institutions",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }

                TextButton(onClick = onNavigateToColleges) {
                    Text("View All 100 →", color = Blue400, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            top3Colleges.forEach { college ->
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

        // QUICK ACTION SHORTCUTS (COMPARE & BOOKMARKS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Slate800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .border(1.dp, Slate700, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToCompare() }
                    .padding(14.dp)
            ) {
                Column {
                    Text("⚖️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Compare Tool", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Compare up to 5 colleges side by side", color = Slate400, fontSize = 10.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Slate800.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .border(1.dp, Slate700, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToSaved() }
                    .padding(14.dp)
            ) {
                Column {
                    Text("🔖", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Saved Shortlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Access your bookmarked institutions", color = Slate400, fontSize = 10.sp)
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

@Composable
fun QuickStatCard(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Slate800.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
            .border(1.dp, Slate700.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
            Text(subtitle, color = Slate400, fontSize = 9.sp, maxLines = 1)
        }
    }
}
