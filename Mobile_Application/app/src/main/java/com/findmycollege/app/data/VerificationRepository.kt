package com.findmycollege.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Verification & Affiliation Result Data Class
 */
data class AffiliationResult(
    val isVerified: Boolean,
    val collegeName: String? = null,
    val verifiedCollegeId: String? = null,
    val badgeText: String = "Standard Account"
)

/**
 * College Review Data Class
 */
data class CollegeReview(
    val id: String,
    val collegeId: String,
    val authorName: String,
    val authorEmail: String,
    val verificationTag: String,
    val rating: Float,
    val comment: String,
    val dateStr: String
)

/**
 * Domain Verification Repository Singleton for Mobile Application
 */
object VerificationRepository {

    private data class DomainMapping(
        val domains: List<String>,
        val collegeId: String,
        val aliases: List<String>,
        val collegeName: String
    )

    private val domainMappings = listOf(
        DomainMapping(
            domains = listOf("saveetha.com", "sse.saveetha.com", "simats.com"),
            collegeId = "saveetha-simats",
            aliases = listOf("saveetha-school-of-engineering", "saveetha-sse", "sse-simats", "saveetha-simats", "simats", "sse", "ir-e-u-0474", "ir-e-c-26911", "saveetha"),
            collegeName = "Saveetha School of Engineering (SIMATS)"
        ),
        DomainMapping(
            domains = listOf("saveetha.ac.in"),
            collegeId = "saveetha-sec",
            aliases = listOf("saveetha-engineering-college", "saveetha-sec", "sec"),
            collegeName = "Saveetha Engineering College (SEC)"
        ),
        DomainMapping(
            domains = listOf("iitm.ac.in", "student.iitm.ac.in"),
            collegeId = "iit-madras",
            aliases = listOf("iit-madras", "iitm"),
            collegeName = "IIT Madras"
        ),
        DomainMapping(
            domains = listOf("vitstudent.ac.in", "vit.ac.in"),
            collegeId = "vit-vellore",
            aliases = listOf("vit-vellore", "vit"),
            collegeName = "VIT Vellore"
        ),
        DomainMapping(
            domains = listOf("srmist.edu.in"),
            collegeId = "srm-ktr",
            aliases = listOf("srm-ktr", "srm"),
            collegeName = "SRM Institute of Science and Technology"
        )
    )

    private val reviewsStore = mutableListOf(
        CollegeReview(
            id = "m_rev_1",
            collegeId = "saveetha-simats",
            authorName = "Rahul Kumar",
            authorEmail = "rahul.sse@saveetha.com",
            verificationTag = "Verified Student of Saveetha School of Engineering (SIMATS)",
            rating = 5.0f,
            comment = "Outstanding research labs, supportive faculty, and top tier placement opportunities in core CS & AI fields.",
            dateStr = "2026-09-15"
        ),
        CollegeReview(
            id = "m_rev_2",
            collegeId = "iit-madras",
            authorName = "Ananya R",
            authorEmail = "ananya@iitm.ac.in",
            verificationTag = "Verified Student of IIT Madras",
            rating = 5.0f,
            comment = "Unmatched academic rigor, world class innovation ecosystem, and campus life.",
            dateStr = "2026-09-28"
        )
    )

    /**
     * Normalize college ID/Name to standard key
     */
    fun normalizeCollegeId(targetIdOrName: String?): String {
        if (targetIdOrName.isNullOrBlank()) return ""
        val q = targetIdOrName.trim().lowercase()

        for (item in domainMappings) {
            if (item.collegeId == q) return item.collegeId
            if (item.collegeName.lowercase().contains(q) || q.contains(item.collegeName.lowercase())) {
                return item.collegeId
            }
            for (alias in item.aliases) {
                if (q == alias || q.contains(alias)) return item.collegeId
            }
        }
        return q
    }

    /**
     * Resolves student domain affiliation for a given email address
     */
    fun getAffiliation(email: String?): AffiliationResult {
        if (email.isNullOrBlank() || !email.contains("@")) {
            return AffiliationResult(isVerified = false, badgeText = "Standard Account")
        }

        val cleanEmail = email.lowercase().trim()
        val domain = cleanEmail.substringAfter("@").lowercase().trim()

        // 1. Saveetha School of Engineering (SIMATS Deemed University)
        if (cleanEmail.contains(".sse@saveetha.com") ||
            cleanEmail.contains("@saveetha.com") ||
            cleanEmail.contains("@sse.saveetha.com") ||
            cleanEmail.contains("@simats.com") ||
            domain == "saveetha.com" ||
            domain == "sse.saveetha.com" ||
            domain == "simats.com") {
            return AffiliationResult(
                isVerified = true,
                collegeName = "Saveetha School of Engineering (SIMATS)",
                verifiedCollegeId = "saveetha-simats",
                badgeText = "🎓 Verified Student of Saveetha School of Engineering (SIMATS)"
            )
        }

        // 2. Saveetha Engineering College (SEC - Autonomous / Anna University)
        if (cleanEmail.contains("@saveetha.ac.in") || domain == "saveetha.ac.in") {
            return AffiliationResult(
                isVerified = true,
                collegeName = "Saveetha Engineering College (SEC)",
                verifiedCollegeId = "saveetha-sec",
                badgeText = "🎓 Verified Student of Saveetha Engineering College (SEC)"
            )
        }

        // 3. General domain mappings
        for (mapping in domainMappings) {
            val isMatch = mapping.domains.any { d -> domain == d || domain.endsWith(".$d") }
            if (isMatch) {
                return AffiliationResult(
                    isVerified = true,
                    collegeName = mapping.collegeName,
                    verifiedCollegeId = mapping.collegeId,
                    badgeText = "🎓 Verified Student of ${mapping.collegeName}"
                )
            }
        }

        return AffiliationResult(isVerified = false, badgeText = "Standard Account")
    }

    /**
     * Checks if the user is authorized to review a target college
     */
    fun canReview(userEmail: String?, collegeId: String?): Boolean {
        if (userEmail.isNullOrBlank() || collegeId.isNullOrBlank()) return false
        val cleanEmail = userEmail.lowercase().trim()
        val cleanTarget = collegeId.lowercase().trim()

        val affiliation = getAffiliation(userEmail)
        if (!affiliation.isVerified) return false

        // Robust matching logic for Saveetha institutions:
        // If user has any Saveetha/SIMATS/SSE email, allow reviewing any Saveetha entry in database
        val isSaveethaUser = cleanEmail.contains("saveetha") || cleanEmail.contains("simats") || cleanEmail.contains("sse")
        val isSaveethaCollege = cleanTarget.contains("saveetha") || cleanTarget.contains("simats") || cleanTarget.contains("sse") ||
                                cleanTarget.contains("ir-e-u-0474") || cleanTarget.contains("ir-e-c-26911")

        if (isSaveethaUser && isSaveethaCollege) {
            return true
        }

        val targetNormalized = normalizeCollegeId(collegeId)
        return affiliation.verifiedCollegeId == targetNormalized
    }

    /**
     * Get reviews for a target college
     */
    fun getReviews(collegeId: String?): List<CollegeReview> {
        val targetNorm = normalizeCollegeId(collegeId)
        val cleanTarget = collegeId?.lowercase()?.trim() ?: ""

        return reviewsStore.filter { r ->
            val rNorm = normalizeCollegeId(r.collegeId)
            rNorm == targetNorm ||
            (cleanTarget.contains("saveetha") && r.collegeId.contains("saveetha"))
        }
    }

    /**
     * Submit a review with strict authorization guard
     */
    fun submitReview(
        userEmail: String?,
        userName: String?,
        collegeId: String,
        rating: Float,
        comment: String
    ): Result<CollegeReview> {
        if (!canReview(userEmail, collegeId)) {
            return Result.failure(
                IllegalStateException("🔒 Authorization Error: Only verified students with an official university email from this institution can submit reviews.")
            )
        }

        val affiliation = getAffiliation(userEmail)
        val name = if (!userName.isNullOrBlank()) userName else userEmail?.substringBefore("@") ?: "Verified Student"
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val newReview = CollegeReview(
            id = "m_rev_${System.currentTimeMillis()}",
            collegeId = normalizeCollegeId(collegeId),
            authorName = name,
            authorEmail = userEmail!!,
            verificationTag = "Verified Student of ${affiliation.collegeName}",
            rating = rating.coerceIn(1.0f, 5.0f),
            comment = comment.trim(),
            dateStr = dateStr
        )

        reviewsStore.add(0, newReview)
        return Result.success(newReview)
    }
}
