package com.findmycollege.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findmycollege.app.data.model.College
import com.findmycollege.app.ui.theme.*

@Composable
fun CollegeCardItem(
    college: College,
    isBookmarked: Boolean,
    isCompared: Boolean = false,
    onToggleBookmark: () -> Unit,
    onToggleCompare: () -> Unit = {},
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.9f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.8f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Bookmark Icon Button in Top Right
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .background(
                        if (isBookmarked) Color(0xFFF59E0B).copy(alpha = 0.2f) else Slate900.copy(alpha = 0.8f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        if (isBookmarked) Color(0xFFF59E0B).copy(alpha = 0.5f) else Slate700.copy(alpha = 0.8f),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Text(text = "🔖", fontSize = 16.sp)
            }

            Column(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {
                // Image Header Banner Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Slate900, Color(0xFF1E1B4B).copy(alpha = 0.6f), Slate900)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(1.dp, Slate700.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏛️", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // NIRF Rank Badge
                    Surface(
                        color = Blue600.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "NIRF ${college.getRankDisplay()}",
                            color = Blue400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // NAAC Accreditation Badge
                    Surface(
                        color = Emerald500.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "NAAC: ${college.naacGrade}",
                            color = Emerald400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Stream Badge
                    if (college.stream.isNotBlank()) {
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = college.stream,
                                color = Color(0xFFA5B4FC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Score Badge
                    if (college.score > 0) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "⭐ ${String.format(java.util.Locale.US, "%.2f", college.score)}",
                                color = Color(0xFFFCD34D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title & Location
                Text(
                    text = college.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "📍 ${college.getLocationStr()} • ID: ${college.getInstituteIdDisplay()}",
                    color = Slate400,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // Stats Dark Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .border(1.dp, Slate800, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tuition Fee
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💰 Tuition Fee:", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(college.getFormattedBtechFee(), color = Blue400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Package Stats Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Slate900, RoundedCornerShape(8.dp))
                                .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Avg Package", color = Slate400, fontSize = 9.sp)
                                Text("📈 ${college.getFormattedAvgPackage()}", color = Blue400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Slate900, RoundedCornerShape(8.dp))
                                .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Highest Package", color = Slate400, fontSize = 9.sp)
                                Text("🚀 ${college.getFormattedHighestPackage()}", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Branches List
                    Column {
                        Text("🎓 Courses / Branches:", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            college.getBranchesList().take(4).forEach { branch ->
                                Surface(
                                    color = Slate800,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = branch,
                                        color = Slate300,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Top Recruiters
                    Column {
                        Text("🏢 Top Recruiters:", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            college.getRecruitersList().take(3).forEach { rec ->
                                Surface(
                                    color = Color(0xFF1E1B4B).copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3730A3).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = rec,
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // NIRF Audited Metrics Row
                    college.nirfMetrics?.let { m ->
                        if (m.tlr > 0 || m.rpc > 0 || m.go > 0 || m.oi > 0 || m.perception > 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate900, RoundedCornerShape(8.dp))
                                    .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                Text("🏛️ NIRF Audited Metrics:", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TLR: ${m.tlr}", color = Blue400, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    Text("RPC: ${m.rpc}", color = Emerald400, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    Text("GO: ${m.go}", color = Color(0xFF818CF8), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    Text("OI: ${m.oi}", color = Color(0xFFFBBF24), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    Text("PR: ${m.perception}", color = Color(0xFFC084FC), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compare Toggle Button
                    Button(
                        onClick = onToggleCompare,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompared) Emerald500.copy(alpha = 0.2f) else Blue600
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCompared) Emerald500.copy(alpha = 0.5f) else Blue500.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isCompared) "✓ In Compare" else "⚖️ + Add to Compare",
                            color = if (isCompared) Emerald400 else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // View Details Text Link Button
                    TextButton(onClick = onViewDetails) {
                        Text(
                            text = "View Details →",
                            color = Blue400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
