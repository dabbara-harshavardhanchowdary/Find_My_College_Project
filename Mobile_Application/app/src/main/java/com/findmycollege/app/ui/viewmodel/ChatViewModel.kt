package com.findmycollege.app.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmycollege.app.data.repository.CollegeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = "Just now"
)

/**
 * ChatViewModel for FindMyCollege AI Admission Counselor
 */
class ChatViewModel : ViewModel() {

    private val repository = CollegeRepository()

    val messages = mutableStateListOf<ChatMessage>(
        ChatMessage(
            text = "👋 Hello! I am your FindMyCollege AI Counselor.\nAsk me about NIRF ranks, admission cutoffs, tuition fees, placement packages, or ROI calculations!",
            isUser = false
        )
    )

    val isThinking = mutableStateOf(false)

    fun sendMessage(userQuery: String) {
        val trimmed = userQuery.trim()
        if (trimmed.isEmpty() || isThinking.value) return

        messages.add(ChatMessage(text = trimmed, isUser = true))
        isThinking.value = true

        viewModelScope.launch {
            val colleges = repository.getColleges()
            delay(800) // Simulate processing
            val responseText = generateGroundedAiResponse(trimmed, colleges)
            messages.add(ChatMessage(text = responseText, isUser = false))
            isThinking.value = false
        }
    }

    private fun generateGroundedAiResponse(query: String, colleges: List<com.findmycollege.app.data.model.College>): String {
        val q = query.lowercase().trim()

        if (q.contains("top 5 cse") || q.contains("top cse") || q.contains("best cse")) {
            val top5 = colleges.take(5)
            val sb = StringBuilder("🏆 Top 5 NIRF Engineering Institutes for Computer Science (CSE):\n\n")
            top5.forEachIndexed { idx, col ->
                sb.append("${idx + 1}. ${col.name} (NIRF ${col.getRankDisplay()})\n📍 Location: ${col.city} | Avg Pkg: ₹18.5 - 24.0 LPA\n\n")
            }
            sb.append("💡 Tip: Use the Compare tool to evaluate cutoffs side-by-side!")
            return sb.toString()
        }

        if (q.contains("best roi") || q.contains("roi") || q.contains("return on investment")) {
            return """
                📊 Best Return on Investment (ROI) Engineering Institutions:

                1. IIT Madras (NIRF #1) — 4-Yr Fee: ~₹8.8 Lakhs | Avg Pkg: ₹22.0 LPA (ROI: 250%)
                2. Saveetha Engineering College (SIMATS) — 4-Yr Fee: ~₹6.5 Lakhs | Avg Pkg: ₹16.8 LPA (ROI: 258%)
                3. IIT Bombay (NIRF #3) — 4-Yr Fee: ~₹9.2 Lakhs | Avg Pkg: ₹23.5 LPA (ROI: 255%)

                💡 ROI = (Average Salary / Total 4-Year Cost) × 100%. Top public & autonomous institutions deliver payback within ~1.5 - 2 years!
            """.trimIndent()
        }

        if (q.contains("placement") || q.contains("highest package") || q.contains("salary")) {
            return """
                📈 Top Institutions by Placement Packages (2026 Index):

                • IIT Madras: Highest: ₹52.5 LPA | Avg: ₹22.0 LPA
                • IIT Delhi: Highest: ₹50.0 LPA | Avg: ₹21.5 LPA
                • IIT Bombay: Highest: ₹54.0 LPA | Avg: ₹23.5 LPA
                • Saveetha Engineering College (SIMATS): Highest: ₹44.0 LPA | Avg: ₹16.8 LPA

                🏢 Key Recruiters: Google, Microsoft, Amazon, Apple, Goldman Sachs, Intel, TCS.
            """.trimIndent()
        }

        if (q.contains("cutoff") || q.contains("jee") || q.contains("rank needed")) {
            return """
                🎯 Estimated General Category JEE Cutoff Ranks:

                • Top 1-10 NIRF (IITs): JEE Advanced Rank under 2,500 for CSE/ECE.
                • Top 10-30 NIRF (NITs/Top Autonomous): JEE Main Rank between 2,500 - 15,000.
                • Top 30-100 NIRF Institutions: JEE Main / State Entrance Ranks between 15,000 - 45,000.

                🔍 Search any college on the Home screen to inspect branch-wise cutoffs!
            """.trimIndent()
        }

        val match = colleges.find { it.name.lowercase().contains(q) || it.city.lowercase().contains(q) }
        if (match != null) {
            return """
                🏛️ ${match.name}
                📍 Location: ${match.getLocationStr()}
                🏆 NIRF Rank: ${match.getRankDisplay()}
                💰 Tuition Fee: ${match.getFormattedBtechFee()}
                📈 Highest Package: ${match.getFormattedHighestPackage()}
                🎓 Branches: CSE, ECE, AI/ML, Electrical, Mechanical

                Click on the college card to view verified student reviews & cutoffs!
            """.trimIndent()
        }

        return """
            Thank you for your question! As your FindMyCollege AI Counselor, I analyzed your prompt regarding engineering admissions.

            Our platform features 100 verified NIRF institutions with complete data for:
            • 🏆 NIRF Rankings & NAAC Accreditations
            • 💰 Annual Tuition & Hostel Fees
            • 📈 Placement Averages & Highest Packages
            • 📊 4-Year Return on Investment (ROI)

            Ask me: "Top 5 CSE Colleges", "Which college has the best ROI?", or search any campus!
        """.trimIndent()
    }
}
