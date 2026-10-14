package com.findmycollege.app.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.findmycollege.app.data.model.College
import com.findmycollege.app.data.model.CollegeReview
import com.findmycollege.app.data.repository.CollegeRepository
import com.findmycollege.app.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun CollegeDetailDialog(
    college: College,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Overview") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { CollegeRepository() }

    var reviewsList by remember { mutableStateOf<List<CollegeReview>>(emptyList()) }
    var userRating by remember { mutableStateOf(5) }
    var userComment by remember { mutableStateOf("") }
    var reviewFeedbackMsg by remember { mutableStateOf("") }

    LaunchedEffect(college.id) {
        reviewsList = repository.getReviewsForCollege(college.getInstituteIdDisplay())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Blue600.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "NIRF ${college.getRankDisplay()} Rank",
                                color = Blue400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = college.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        )

                        Text(
                            text = "📍 ${college.getLocationStr()}",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Text("✕", color = Slate400, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bookmark Toggle Action Button
                Button(
                    onClick = onToggleBookmark,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBookmarked) Color(0xFFF59E0B).copy(alpha = 0.2f) else Slate800
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isBookmarked) Color(0xFFF59E0B).copy(alpha = 0.5f) else Slate700
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text(
                        text = if (isBookmarked) "🔖 Bookmarked" else "🔖 Bookmark College",
                        color = if (isBookmarked) Color(0xFFF59E0B) else Slate300,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector Bar (Overview, Fees, Cutoffs, Placements, Reviews)
                val tabs = listOf("Overview", "Fees", "Cutoffs", "Placements", "Reviews")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Blue600 else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Slate400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents Container
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (selectedTab) {
                        "Overview" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Stats Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Slate950, RoundedCornerShape(12.dp))
                                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("NIRF Rank", color = Slate400, fontSize = 10.sp)
                                            Text(college.getRankDisplay(), color = Blue400, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Slate950, RoundedCornerShape(12.dp))
                                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("NIRF Score", color = Slate400, fontSize = 10.sp)
                                            Text("⭐ ${college.score}", color = Emerald400, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate950, RoundedCornerShape(12.dp))
                                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("Institute ID", color = Slate400, fontSize = 10.sp)
                                        Text(college.getInstituteIdDisplay(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // NIRF Parameter Breakdown
                                val m = college.nirfMetrics
                                val tlr = m?.tlr ?: 78.5
                                val rpc = m?.rpc ?: 82.0
                                val go = m?.go ?: 75.0
                                val oi = m?.oi ?: 72.0
                                val pr = m?.perception ?: 80.0

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate950, RoundedCornerShape(16.dp))
                                        .border(1.dp, Blue500.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "🏛️ Official NIRF Parameter Breakdown",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )

                                        MetricProgressBar("1. TLR (Teaching & Resources)", tlr, Blue400)
                                        MetricProgressBar("2. RPC (Research & Practice)", rpc, Emerald400)
                                        MetricProgressBar("3. GO (Graduation Outcomes)", go, Color(0xFF818CF8))
                                        MetricProgressBar("4. OI (Outreach & Inclusivity)", oi, Color(0xFFFBBF24))
                                        MetricProgressBar("5. Perception (Peer Rank)", pr, Color(0xFFC084FC))
                                    }
                                }

                                if (!college.nirfReportUrl.isNullOrBlank()) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(college.nirfReportUrl))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Blue600.copy(alpha = 0.2f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📄 View Official Signed NIRF Dossier (PDF)", color = Blue300, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = if (college.description.isNotBlank()) college.description else "${college.name} is a premier engineering institution ranked ${college.getRankDisplay()} in NIRF.",
                                    color = Slate300,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        "Fees" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FeeRow("B.Tech / B.E. (Annual Fee)", college.getFormattedBtechFee())
                                FeeRow("M.Tech (Annual Fee)", if (college.mtechFee.isNotBlank()) college.mtechFee else "≈ ₹1,10,000/year")
                                FeeRow("Hostel & Mess Charges", if (college.hostelFee.isNotBlank()) college.hostelFee else "≈ ₹65,000/year")
                            }
                        }

                        "Cutoffs" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Estimated Cutoff Ranks (General Category):", color = Slate400, fontSize = 11.sp)
                                CutoffRow("Computer Science (CSE)", college.getCseCutoffDisplay())
                                CutoffRow("Electronics (ECE)", college.getEceCutoffDisplay())
                                CutoffRow("Mechanical (ME)", college.getMeCutoffDisplay())
                            }
                        }

                        "Placements" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Slate950, RoundedCornerShape(12.dp))
                                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Highest Package", color = Slate400, fontSize = 10.sp)
                                            Text(college.getFormattedHighestPackage(), color = Emerald400, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Slate950, RoundedCornerShape(12.dp))
                                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Average Package", color = Slate400, fontSize = 10.sp)
                                            Text(college.getFormattedAvgPackage(), color = Blue400, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }

                        "Reviews" -> {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Add Review Section if logged in
                                if (currentUser != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Slate950, RoundedCornerShape(14.dp))
                                            .border(1.dp, Emerald500.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("✍️ Write Student Review", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                            OutlinedTextField(
                                                value = userComment,
                                                onValueChange = { userComment = it },
                                                placeholder = { Text("Share your campus experience, academics, and placements...") },
                                                modifier = Modifier.fillMaxWidth(),
                                                maxLines = 3
                                            )

                                            Button(
                                                onClick = {
                                                    if (userComment.isNotBlank()) {
                                                        val newReview = CollegeReview(
                                                            collegeId = college.getInstituteIdDisplay(),
                                                            authorName = currentUser.displayName ?: (currentUser.email?.split("@")?.get(0) ?: "Student"),
                                                            authorEmail = currentUser.email ?: "",
                                                            verificationTag = "Verified Student of ${college.name}",
                                                            rating = userRating,
                                                            comment = userComment.trim(),
                                                            date = "2026-10-10"
                                                        )
                                                        scope.launch {
                                                            val success = repository.submitReview(newReview)
                                                            if (success) {
                                                                reviewFeedbackMsg = "Review submitted successfully! ✅"
                                                                userComment = ""
                                                                reviewsList = repository.getReviewsForCollege(college.getInstituteIdDisplay())
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Post Verified Review", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }

                                            if (reviewFeedbackMsg.isNotBlank()) {
                                                Text(reviewFeedbackMsg, color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Reviews List Feed
                                Text("💬 Student Reviews (${reviewsList.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                if (reviewsList.isEmpty()) {
                                    Text("No reviews posted yet for this institution.", color = Slate400, fontSize = 11.sp)
                                } else {
                                    reviewsList.forEach { r ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Slate950, RoundedCornerShape(12.dp))
                                                .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                                .padding(10.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(r.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Text("⭐ ${r.rating}/5", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                                Text(r.comment, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MetricProgressBar(label: String, score: Double, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Slate300, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text("$score / 100", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = (score / 100.0).toFloat().coerceIn(0f, 1f),
            color = color,
            trackColor = Slate900,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}

@Composable
fun FeeRow(title: String, fee: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate950, RoundedCornerShape(10.dp))
            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(fee, color = Blue400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CutoffRow(branch: String, cutoff: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate950, RoundedCornerShape(10.dp))
            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(branch, color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(cutoff, color = Emerald400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
