package com.findmycollege.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findmycollege.app.data.model.College
import com.findmycollege.app.ui.components.CollegeCardItem
import com.findmycollege.app.ui.dialogs.CollegeDetailDialog
import com.findmycollege.app.ui.theme.*
import com.findmycollege.app.ui.viewmodel.CollegeViewModel

@Composable
fun BookmarksScreen(viewModel: CollegeViewModel) {
    val bookmarkedColleges by viewModel.bookmarkedColleges.collectAsState()
    val selectedCollegeForDetails by viewModel.selectedCollegeForDetails.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔖 Bookmarked Colleges",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Saved to your student profile",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Text(
                    text = "${bookmarkedColleges.size} Bookmarked",
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (bookmarkedColleges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp)
                    .background(Slate800.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔖", fontSize = 36.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = "No colleges bookmarked yet",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Tap the bookmark icon on any college card or detail modal to save it here for quick access.",
                        color = Slate400,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(bookmarkedColleges, key = { it.getInstituteIdDisplay() }) { college ->
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
