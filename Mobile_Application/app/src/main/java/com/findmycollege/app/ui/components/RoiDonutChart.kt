package com.findmycollege.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.findmycollege.app.data.model.College
import com.findmycollege.app.data.model.calculateAvgSalaryLakhs
import com.findmycollege.app.data.model.calculateTotal4YearCostLakhs
import com.findmycollege.app.ui.theme.Slate300
import com.findmycollege.app.ui.theme.Slate400
import com.findmycollege.app.ui.theme.Slate800
import com.findmycollege.app.ui.theme.Slate950
import java.util.Locale

/**
 * Native Jetpack Compose Canvas ROI Donut / Pie Chart.
 * Draws Cost segment in red/orange and Return segment in green with central ROI overlay.
 */
@Composable
fun RoiDonutChart(
    totalCost: Float,
    avgSalary: Float,
    modifier: Modifier = Modifier
) {
    val validCost = if (totalCost <= 0f) 1f else totalCost
    val validSalary = if (avgSalary <= 0f) 0.1f else avgSalary
    val totalSum = validCost + validSalary

    val costSweepAngle = (validCost / totalSum) * 360f
    val salarySweepAngle = (validSalary / totalSum) * 360f

    val roiRatio = if (totalCost > 0f) avgSalary / totalCost else 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(90.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Draw Cost Arc (Red/Orange segment)
            drawArc(
                color = Color(0xFFFF6B6B),
                startAngle = -90f,
                sweepAngle = costSweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw Return Arc (Green segment)
            drawArc(
                color = Color(0xFF2EC4B6),
                startAngle = -90f + costSweepAngle,
                sweepAngle = salarySweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center ROI ratio overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${String.format(Locale.US, "%.1f", roiRatio)}x",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Text(
                text = "ROI",
                color = Slate400,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
            )
        }
    }
}

/**
 * Reusable Card container for ROI Donut Chart with header, legend row, and payback chip.
 */
@Composable
fun RoiAnalysisCard(
    college: College,
    modifier: Modifier = Modifier
) {
    val totalCost = college.calculateTotal4YearCostLakhs()
    val avgSalary = college.calculateAvgSalaryLakhs()
    val paybackYears = if (avgSalary > 0f) String.format(Locale.US, "%.1f", totalCost / avgSalary) else "N/A"

    Surface(
        color = Slate950,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Payback Chip Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 ROI Analysis",
                    color = Slate300,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Payback: $paybackYears Yrs",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Native Canvas Donut Chart
            RoiDonutChart(
                totalCost = totalCost,
                avgSalary = avgSalary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Legend Row below chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Red dot: Total Investment (₹)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFFFF6B6B), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Cost: ₹${String.format(Locale.US, "%.1f", totalCost)}L",
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                // Green dot: Avg First Year CTC (₹)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF2EC4B6), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CTC: ₹${String.format(Locale.US, "%.1f", avgSalary)}L",
                        color = Color(0xFF2EC4B6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
