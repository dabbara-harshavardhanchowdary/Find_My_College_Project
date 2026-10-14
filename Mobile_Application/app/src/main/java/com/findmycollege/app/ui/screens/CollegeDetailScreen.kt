package com.findmycollege.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.findmycollege.app.data.model.College

@Composable
fun CollegeDetailScreen(college: College) {
    val context = LocalContext.current
    val m = college.nirfMetrics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = college.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = "${college.city}, ${college.state}", color = Color.Gray)
        Text(text = "Tuition Fee: ≈ ₹${college.fees.toInt()} / year", fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏛️ Official NIRF Parameter Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                MetricBar("Teaching, Learning & Resources (TLR - 30%)", m?.tlr ?: 0.0)
                MetricBar("Research and Professional Practice (RPC - 30%)", m?.rpc ?: 0.0)
                MetricBar("Graduation Outcomes (GO - 20%)", m?.go ?: 0.0)
                MetricBar("Outreach and Inclusivity (OI - 10%)", m?.oi ?: 0.0)
                MetricBar("Peer Perception (PR - 10%)", m?.perception ?: 0.0)
            }
        }

        if (!college.nirfReportUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(college.nirfReportUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📄 View Official Signed NIRF Dossier (PDF)")
            }
        }
    }
}

@Composable
fun MetricBar(name: String, score: Double) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = name, style = MaterialTheme.typography.bodySmall)
            Text(text = "$score / 100", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = (score / 100.0).toFloat().coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
    }
}
