package com.findmycollege.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findmycollege.app.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "ℹ️ About Find My College",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = "Learn more about our platform and official NIRF evaluation methodology",
                color = Slate400,
                fontSize = 11.sp
            )
        }

        // Platform Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Empowering Students with Data-Driven Admissions",
                    color = Blue400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Find My College is designed to help students discover verified NIRF-ranked institutions across India. Access real-time college records, admission cutoffs, rankings, and placement statistics to make informed choices.",
                    color = Slate300,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // NIRF Methodology Framework Section Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏆 ", fontSize = 20.sp)
            Text(
                text = "NIRF Ranking Methodology & Parameters",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }

        // Parameter 1: TLR
        MethodologyCard(
            title = "Teaching, Learning & Resources (TLR)",
            titleColor = Blue400,
            icon = "📚",
            metrics = listOf(
                "Student Strength including Doctoral Students (SS)",
                "Faculty-student ratio with emphasis on permanent faculty (FSR)",
                "Combined metric for Faculty with PhD (or equivalent) and Experience (FQE)",
                "Financial Resources and their Utilisation (FRU)",
                "Online Education (OE)",
                "Multiple Entry/Exit, Indian Knowledge System and Regional languages (MIR)"
            )
        )

        // Parameter 2: RP
        MethodologyCard(
            title = "Research and Professional Practice (RP)",
            titleColor = Emerald400,
            icon = "🔬",
            metrics = listOf(
                "Combined metric for Publications (PU)",
                "Combined metric for Quality of Publications (QP)",
                "IPR and Patents: Published and Granted (IPR)",
                "Footprint of Projects and Professional Practice (FPPP)",
                "Publications & Citations in SDGs (PSDGs)"
            )
        )

        // Parameter 3: GO
        MethodologyCard(
            title = "Graduation Outcomes (GO)",
            titleColor = Color(0xFF818CF8), // Indigo
            icon = "🎓",
            metrics = listOf(
                "Metric for University Examinations (GUE)",
                "Metric for Number of Ph.D. Students Graduated (GPHD)"
            )
        )

        // Parameter 4: OI
        MethodologyCard(
            title = "Outreach and Inclusivity (OI)",
            titleColor = Color(0xFFFBBF24), // Amber
            icon = "🌐",
            metrics = listOf(
                "Percentage of Students from Other States/Countries (Region Diversity RD)",
                "Percentage of Women (Women Diversity WD)",
                "Economically and Socially Challenged Students (ESCS)",
                "Facilities for Physically Challenged Students (PCS)",
                "Perception (PR) Ranking"
            )
        )

        // Parameter 5: Peer Perception
        MethodologyCard(
            title = "Peer Perception",
            titleColor = Color(0xFFC084FC), // Purple
            icon = "👥",
            metrics = listOf(
                "Academic Peers and Employers (PR)"
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Official Data Note
        Card(
            colors = CardDefaults.cardColors(containerColor = Blue600.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Blue400.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("📌", fontSize = 18.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "OFFICIAL DATA NOTE",
                        color = Blue400,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Note: All ranking data, metrics, and institutional reports on this platform are subject to and sourced directly from NIRF, Ministry of Education, Government of India.",
                        color = Slate300,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MethodologyCard(
    title: String,
    titleColor: Color,
    icon: String,
    metrics: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate950),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$icon ", fontSize = 16.sp)
                Text(text = title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            metrics.forEach { metric ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = metric, color = Slate300, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}
