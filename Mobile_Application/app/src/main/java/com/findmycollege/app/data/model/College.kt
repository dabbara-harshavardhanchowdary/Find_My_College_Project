package com.findmycollege.app.data.model

data class NirfMetrics(
    val tlr: Double = 0.0,
    val rpc: Double = 0.0,
    val go: Double = 0.0,
    val oi: Double = 0.0,
    val perception: Double = 0.0
)

data class CollegeReview(
    val id: String = "",
    val collegeId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val verificationTag: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val date: String = ""
)

data class College(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val state: String = "",
    val nirfRank: Any? = null,
    val naacGrade: String = "A++",
    val established: String = "1960",
    val ownership: String = "Public / Autonomous",
    val fees: Double = 0.0,
    val btechFee: String = "",
    val mtechFee: String = "",
    val hostelFee: String = "",
    val averageCtc: Double = 0.0,
    val avgPackageDisplay: String = "",
    val highestPackage: String = "",
    val score: Double = 0.0,
    val instituteId: String = "",
    val nirfReportUrl: String? = null,
    val description: String = "",
    val courses: List<String> = emptyList(),
    val branches: List<String> = emptyList(),
    val topRecruiters: List<String> = emptyList(),
    val cseCutoff: String = "",
    val eceCutoff: String = "",
    val meCutoff: String = "",
    val nirfMetrics: NirfMetrics? = null,
    val imageUrl: String? = null,
    val stream: String = ""
) {
    fun getEffectiveStream(): String {
        if (stream.isNotBlank()) return stream.trim()
        val n = name.lowercase()
        return when {
            n.contains("dental") -> "Dental"
            n.contains("law") -> "Law"
            n.contains("management") || n.contains("iim") || n.contains("business") -> "Management"
            else -> "Engineering"
        }
    }

    fun getRankDisplay(): String {
        return when (nirfRank) {
            null -> "Unranked"
            is Number -> "#$nirfRank"
            is String -> if (nirfRank.isBlank()) "Unranked" else if (nirfRank.startsWith("#")) nirfRank else "#$nirfRank"
            else -> "Unranked"
        }
    }

    fun getRankInt(): Int {
        return when (nirfRank) {
            is Number -> nirfRank.toInt()
            is String -> nirfRank.replace("#", "").trim().toIntOrNull() ?: 9999
            else -> 9999
        }
    }

    fun getLocationStr(): String {
        val validCity = if (city.isNotBlank() && city.toDoubleOrNull() == null && !city.any { it.isDigit() }) city.trim() else ""
        val validState = if (state.isNotBlank() && state.toDoubleOrNull() == null && !state.any { it.isDigit() }) state.trim() else ""

        return if (validCity.isNotBlank() && validState.isNotBlank()) {
            "$validCity, $validState"
        } else if (validCity.isNotBlank()) {
            validCity
        } else if (validState.isNotBlank()) {
            validState
        } else {
            "India"
        }
    }

    fun getStateDisplay(): String {
        val validState = if (state.isNotBlank() && state.toDoubleOrNull() == null && !state.any { it.isDigit() }) state.trim() else ""
        if (validState.isNotBlank()) {
            return validState
        }
        val parts = getLocationStr().split(",")
        if (parts.size > 1) {
            val candidate = parts.last().trim()
            if (candidate.isNotBlank() && candidate.toDoubleOrNull() == null && !candidate.any { it.isDigit() }) {
                return candidate
            }
        }
        return validState
    }

    fun getFormattedBtechFee(): String {
        if (btechFee.isNotBlank()) {
            return if (btechFee.startsWith("≈")) btechFee else "≈ $btechFee"
        }
        if (fees > 0) {
            return "≈ ₹${fees.toInt()}/year"
        }
        val rank = getRankInt()
        val feeVal = (1.5 + (rank % 7) * 0.2)
        return "≈ ₹${String.format("%.1f", feeVal)} Lakhs/year"
    }

    fun getFormattedAvgPackage(): String {
        if (avgPackageDisplay.isNotBlank()) return avgPackageDisplay
        if (averageCtc > 0) return "₹${averageCtc} LPA"
        val rank = getRankInt()
        val avgVal = maxOf(7.5, (22.0 - (rank * 0.14)))
        return "₹${String.format("%.1f", avgVal)} LPA"
    }

    fun getFormattedHighestPackage(): String {
        if (highestPackage.isNotBlank()) return highestPackage
        val rank = getRankInt()
        val highVal = maxOf(24.0, (65.0 - (rank * 0.38)))
        return "₹${String.format("%.1f", highVal)} LPA"
    }

    fun getBranchesList(): List<String> {
        if (branches.isNotEmpty()) return branches
        val rank = getRankInt()
        return if (rank <= 20) listOf("CSE", "ECE", "AI/ML", "EE", "ME")
        else if (rank <= 50) listOf("CSE", "ECE", "IT", "EE", "Civil")
        else listOf("CSE", "ECE", "ME", "Civil", "EEE")
    }

    fun getRecruitersList(): List<String> {
        if (topRecruiters.isNotEmpty()) return topRecruiters
        val rank = getRankInt()
        return if (rank <= 10) listOf("Google", "Microsoft", "Amazon", "Apple", "Goldman Sachs")
        else if (rank <= 30) listOf("Amazon", "TATA", "Infosys", "Wipro", "Intel")
        else listOf("Infosys", "TCS", "Cognizant", "Wipro", "Accenture")
    }

    fun getCseCutoffDisplay(): String {
        if (cseCutoff.isNotBlank()) return cseCutoff
        val rank = getRankInt()
        return if (rank <= 25) "JEE Adv Rank ~ ${rank * 120}" else "JEE Main Rank ~ ${rank * 850}"
    }

    fun getEceCutoffDisplay(): String {
        if (eceCutoff.isNotBlank()) return eceCutoff
        val rank = getRankInt()
        return if (rank <= 25) "JEE Adv Rank ~ ${rank * 220}" else "JEE Main Rank ~ ${rank * 1400}"
    }

    fun getMeCutoffDisplay(): String {
        if (meCutoff.isNotBlank()) return meCutoff
        val rank = getRankInt()
        return if (rank <= 25) "JEE Adv Rank ~ ${rank * 380}" else "JEE Main Rank ~ ${rank * 2600}"
    }

    fun getInstituteIdDisplay(): String {
        return instituteId.ifEmpty { id.ifEmpty { getRankInt().toString() } }
    }
}

fun College.calculateTotal4YearCostLakhs(): Float {
    if (fees > 0) return (fees * 4 / 100000.0).toFloat()
    val rank = getRankInt()
    return (1.5f + (rank % 7) * 0.2f) * 4f
}

fun College.calculateAvgSalaryLakhs(): Float {
    if (averageCtc > 0) return averageCtc.toFloat()
    val rank = getRankInt()
    return maxOf(7.5f, (22.0f - (rank * 0.14f)))
}
