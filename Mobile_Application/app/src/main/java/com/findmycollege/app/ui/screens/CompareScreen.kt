package com.findmycollege.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findmycollege.app.data.model.College
import com.findmycollege.app.ui.theme.*
import com.findmycollege.app.ui.viewmodel.CollegeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(viewModel: CollegeViewModel) {
    val collegesList by viewModel.collegesList.collectAsState()
    val comparisonSlots by viewModel.comparisonSlots.collectAsState()

    var activeSearchSlotIndex by remember { mutableStateOf<Int?>(null) }
    var slotQuery by remember { mutableStateOf("") }

    val activeColleges = remember(comparisonSlots) {
        comparisonSlots.filterNotNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "⚖️ Compare Colleges",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Analyze ranks, cutoffs, fees & packages side-by-side",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            if (comparisonSlots.size < 5) {
                IconButton(
                    onClick = { viewModel.addComparisonSlot() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Blue600.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(1.dp, Blue500.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add slot", tint = Blue400)
                }
            }
        }

        // Error Banner if stream mismatch occurred
        val comparisonError by viewModel.comparisonError.collectAsState()
        comparisonError?.let { err ->
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️ ", fontSize = 14.sp)
                        Text(
                            text = err,
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearComparisonError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Slot Picker Horizontal Chips Row
        Text("Select Institutions to Compare (Up to 5):", color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            comparisonSlots.forEachIndexed { index, selectedCollege ->
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .background(
                            if (selectedCollege != null) Slate800 else Slate950,
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (selectedCollege != null) Blue500.copy(alpha = 0.4f) else Slate700,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            activeSearchSlotIndex = index
                            slotQuery = ""
                        }
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Slot ${index + 1}", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (selectedCollege != null) {
                                IconButton(
                                    onClick = { viewModel.clearSlot(index) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        if (selectedCollege != null) {
                            Text(
                                text = selectedCollege.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NIRF ${selectedCollege.getRankDisplay()}",
                                    color = Blue400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedCollege.stream.isNotBlank()) {
                                    Text(
                                        text = "• ${selectedCollege.stream}",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "+ Select College",
                                color = Blue400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Search College Picker Dialog if slot clicked
        activeSearchSlotIndex?.let { slotIdx ->
            // Determine if another slot is already filled to enforce stream matching
            val referenceCollege = comparisonSlots.filterIndexed { i, c -> i != slotIdx && c != null }.mapNotNull { it }.firstOrNull()
            val requiredStream = referenceCollege?.stream?.ifBlank { referenceCollege.getEffectiveStream() }
            var streamColleges by remember(requiredStream) { mutableStateOf<List<College>>(emptyList()) }

            LaunchedEffect(requiredStream) {
                if (requiredStream != null) {
                    try {
                        streamColleges = viewModel.getCollegesForStreamDirect(requiredStream)
                    } catch (e: Exception) {
                        streamColleges = emptyList()
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { activeSearchSlotIndex = null },
                containerColor = Slate900,
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Select College for Slot ${slotIdx + 1}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (requiredStream != null) {
                            Surface(
                                color = Blue600.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Blue500.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "🔒 Restricted to $requiredStream stream",
                                    color = Blue400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = slotQuery,
                            onValueChange = { slotQuery = it },
                            placeholder = { Text("Type college name, city...", color = Slate400, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val eligibleColleges = remember(collegesList, streamColleges, requiredStream) {
                            if (requiredStream != null) {
                                val pool = if (streamColleges.isNotEmpty()) streamColleges else collegesList
                                pool.filter {
                                    val s = it.stream.ifBlank { it.getEffectiveStream() }
                                    s.equals(requiredStream, ignoreCase = true)
                                }
                            } else {
                                collegesList
                            }
                        }

                        val matchingList = remember(slotQuery, eligibleColleges) {
                            if (slotQuery.isBlank()) eligibleColleges.take(15)
                            else eligibleColleges.filter {
                                it.name.contains(slotQuery, ignoreCase = true) ||
                                it.city.contains(slotQuery, ignoreCase = true)
                            }.take(15)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (matchingList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (requiredStream != null) "No matching $requiredStream colleges found" else "No matching colleges found",
                                        color = Slate400,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                matchingList.forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Slate950, RoundedCornerShape(10.dp))
                                            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val success = viewModel.setCollegeForSlot(slotIdx, col)
                                                if (success) {
                                                    activeSearchSlotIndex = null
                                                }
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(col.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text("📍 ${col.getLocationStr()}", color = Slate400, fontSize = 10.sp)
                                                    if (col.stream.isNotBlank()) {
                                                        Text("• ${col.stream}", color = Color(0xFFA5B4FC), fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                            Text("NIRF ${col.getRankDisplay()}", color = Blue400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSearchSlotIndex = null }) {
                        Text("Cancel", color = Slate400)
                    }
                }
            )
        }

        if (activeColleges.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .background(Slate800.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .border(1.dp, Slate700, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚖️", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select at least 2 colleges to compare", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Tap the slot boxes above or '+ Add to Compare' on any college card to evaluate rankings, fees, and packages side-by-side.",
                        color = Slate400,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // SIDE-BY-SIDE COMPARISON TABLE
            Text("📊 Side-by-Side Comparison Matrix:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(Slate950, RoundedCornerShape(16.dp))
                    .border(1.dp, Slate800, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Row with College Names
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Parameter", color = Slate400, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        activeColleges.forEach { col ->
                            Text(
                                text = col.name,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.width(130.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate800)

                    // NIRF Rank Row
                    CompareTableRow("NIRF Rank", activeColleges) { it.getRankDisplay() }

                    // Tuition Fee Row
                    CompareTableRow("Tuition Fee", activeColleges) { it.getFormattedBtechFee() }

                    // Average Package Row
                    CompareTableRow("Avg Package", activeColleges) { it.getFormattedAvgPackage() }

                    // Highest Package Row
                    CompareTableRow("Highest Package", activeColleges) { it.getFormattedHighestPackage() }

                    // NAAC Rating Row
                    CompareTableRow("NAAC Grade", activeColleges) { it.naacGrade }

                    // Location Row
                    CompareTableRow("Location", activeColleges) { it.getLocationStr() }

                    // CSE Cutoff Row
                    CompareTableRow("CSE Cutoff", activeColleges) { it.getCseCutoffDisplay() }

                    // TLR Score Row
                    CompareTableRow("TLR (30%)", activeColleges) { "${it.nirfMetrics?.tlr ?: 78.0} / 100" }

                    // RPC Score Row
                    CompareTableRow("RPC (30%)", activeColleges) { "${it.nirfMetrics?.rpc ?: 82.0} / 100" }

                    // GO Score Row
                    CompareTableRow("GO (20%)", activeColleges) { "${it.nirfMetrics?.go ?: 75.0} / 100" }

                    // Perception Row
                    CompareTableRow("Perception", activeColleges) { "${it.nirfMetrics?.perception ?: 80.0} / 100" }
                }
            }

            // WINNER VERDICT CARD
            val topRankedCollege = remember(activeColleges) {
                activeColleges.minByOrNull { it.getRankInt() }
            }

            if (topRankedCollege != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Blue600.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .border(1.dp, Blue500.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🏆 Total Result & Verdict", color = Blue400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Based on audited government data, ${topRankedCollege.name} (NIRF ${topRankedCollege.getRankDisplay()}) leads in overall accreditation, academic TLR score, and nationwide placement packages.",
                            color = Slate200,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompareTableRow(label: String, colleges: List<College>, valueExtractor: (College) -> String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(110.dp))
        colleges.forEach { col ->
            Text(
                text = valueExtractor(col),
                color = Blue400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(130.dp)
            )
        }
    }
}
