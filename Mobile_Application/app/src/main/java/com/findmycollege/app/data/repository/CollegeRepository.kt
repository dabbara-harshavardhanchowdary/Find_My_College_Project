package com.findmycollege.app.data.repository

import com.findmycollege.app.data.model.College
import com.findmycollege.app.data.model.CollegeReview
import com.findmycollege.app.data.model.NirfMetrics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CollegeRepository {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        val STREAM_COLLECTIONS = mapOf(
            "Dental" to "dental_colleges",
            "Engineering" to "engineering_colleges",
            "Law" to "law_colleges",
            "Management" to "management_colleges"
        )

        fun getCollectionNameForStream(stream: String): String? {
            val trimmed = stream.trim()
            return STREAM_COLLECTIONS.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.value
        }

        fun getStreamNameForCollection(collectionName: String): String {
            return STREAM_COLLECTIONS.entries.firstOrNull { it.value.equals(collectionName, ignoreCase = true) }?.key ?: "Engineering"
        }
    }

    suspend fun getColleges(stream: String = "All"): List<College> = withContext(Dispatchers.IO) {
        val targetEntries = if (stream.isNotBlank() && !stream.equals("All", ignoreCase = true)) {
            val col = getCollectionNameForStream(stream) ?: "engineering_colleges"
            val sName = getStreamNameForCollection(col)
            listOf(sName to col)
        } else {
            STREAM_COLLECTIONS.toList()
        }

        try {
            val deferred = targetEntries.map { (streamName, collectionName) ->
                async {
                    try {
                        val snapshot = db.collection(collectionName).get().await()
                        snapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            parseCollegeDoc(doc.id, data, defaultStream = streamName)
                        }
                    } catch (e: Exception) {
                        emptyList<College>()
                    }
                }
            }
            val all = deferred.awaitAll().flatten().sortedBy { it.getRankInt() }
            if (all.isNotEmpty()) all else getDefaultColleges(stream)
        } catch (e: Exception) {
            getDefaultColleges(stream)
        }
    }

    fun getCollegesFlow(stream: String = "All"): Flow<List<College>> = callbackFlow {
        val targetEntries = if (stream.isNotBlank() && !stream.equals("All", ignoreCase = true)) {
            val col = getCollectionNameForStream(stream) ?: "engineering_colleges"
            val sName = getStreamNameForCollection(col)
            listOf(sName to col)
        } else {
            STREAM_COLLECTIONS.toList()
        }

        val cache = ConcurrentHashMap<String, List<College>>()
        val listeners = mutableListOf<ListenerRegistration>()

        for ((streamName, colName) in targetEntries) {
            val reg = db.collection(colName).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (!cache.containsKey(colName)) {
                        cache[colName] = emptyList()
                    }
                } else if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        parseCollegeDoc(doc.id, data, defaultStream = streamName)
                    }.sortedBy { it.getRankInt() }
                    cache[colName] = list
                }
                val aggregated = cache.values.flatten().sortedBy { it.getRankInt() }
                trySend(if (aggregated.isNotEmpty()) aggregated else getDefaultColleges(stream))
            }
            listeners.add(reg)
        }

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    private fun isValidLocationName(s: String): Boolean {
        if (s.isBlank()) return false
        if (s.toDoubleOrNull() != null) return false
        if (s.any { it.isDigit() }) return false
        return true
    }

    private fun inferLocationFromName(name: String, id: String): Pair<String, String> {
        val n = "$name $id".lowercase()
        return when {
            n.contains("aligarh") -> Pair("Aligarh", "Uttar Pradesh")
            n.contains("madras") || n.contains("chennai") || n.contains("saveetha") -> Pair("Chennai", "Tamil Nadu")
            n.contains("delhi") || n.contains("jamia") -> Pair("New Delhi", "Delhi")
            n.contains("bombay") || n.contains("mumbai") -> Pair("Mumbai", "Maharashtra")
            n.contains("kanpur") -> Pair("Kanpur", "Uttar Pradesh")
            n.contains("roorkee") -> Pair("Roorkee", "Uttarakhand")
            n.contains("kharagpur") -> Pair("Kharagpur", "West Bengal")
            n.contains("guwahati") -> Pair("Guwahati", "Assam")
            n.contains("hyderabad") || n.contains("sangareddy") -> Pair("Sangareddy", "Telangana")
            n.contains("trichy") || n.contains("tiruchirappalli") -> Pair("Tiruchirappalli", "Tamil Nadu")
            n.contains("jadavpur") || n.contains("kolkata") -> Pair("Kolkata", "West Bengal")
            n.contains("vellore") || n.contains("vit") -> Pair("Vellore", "Tamil Nadu")
            n.contains("surathkal") || n.contains("karnataka") -> Pair("Surathkal", "Karnataka")
            n.contains("indore") -> Pair("Indore", "Madhya Pradesh")
            n.contains("srm") || n.contains("kattankulathur") -> Pair("Kattankulathur", "Tamil Nadu")
            n.contains("pilani") || n.contains("bits") -> Pair("Pilani", "Rajasthan")
            n.contains("varanasi") || n.contains("bhu") -> Pair("Varanasi", "Uttar Pradesh")
            n.contains("gandhinagar") -> Pair("Gandhinagar", "Gujarat")
            n.contains("ropar") || n.contains("punjab") -> Pair("Rupnagar", "Punjab")
            n.contains("patna") || n.contains("bihar") -> Pair("Patna", "Bihar")
            n.contains("mandi") -> Pair("Mandi", "Himachal Pradesh")
            n.contains("jodhpur") -> Pair("Jodhpur", "Rajasthan")
            n.contains("bhubaneswar") -> Pair("Bhubaneswar", "Odisha")
            n.contains("warangal") -> Pair("Warangal", "Telangana")
            n.contains("nagpur") -> Pair("Nagpur", "Maharashtra")
            n.contains("jaipur") -> Pair("Jaipur", "Rajasthan")
            n.contains("kurukshetra") -> Pair("Kurukshetra", "Haryana")
            n.contains("silchar") -> Pair("Silchar", "Assam")
            n.contains("durgapur") -> Pair("Durgapur", "West Bengal")
            n.contains("jalandhar") -> Pair("Jalandhar", "Punjab")
            n.contains("surat") -> Pair("Surat", "Gujarat")
            n.contains("raipur") -> Pair("Raipur", "Chhattisgarh")
            n.contains("prayagraj") || n.contains("allahabad") -> Pair("Prayagraj", "Uttar Pradesh")
            n.contains("goa") -> Pair("Farmagudi", "Goa")
            n.contains("calicut") || n.contains("kozhikode") -> Pair("Kozhikode", "Kerala")
            n.contains("rourkela") -> Pair("Rourkela", "Odisha")
            n.contains("coimbatore") -> Pair("Coimbatore", "Tamil Nadu")
            n.contains("thiruvananthapuram") || n.contains("trivandrum") -> Pair("Thiruvananthapuram", "Kerala")
            n.contains("chandigarh") -> Pair("Chandigarh", "Punjab")
            n.contains("manipal") -> Pair("Manipal", "Karnataka")
            n.contains("bangalore") || n.contains("bengaluru") -> Pair("Bengaluru", "Karnataka")
            n.contains("pune") -> Pair("Pune", "Maharashtra")
            n.contains("ahmedabad") -> Pair("Ahmedabad", "Gujarat")
            n.contains("lucknow") -> Pair("Lucknow", "Uttar Pradesh")
            else -> Pair("India", "India")
        }
    }

    private fun parseCollegeDoc(docId: String, data: Map<String, Any>, defaultStream: String = "Engineering"): College {
        val name = (data["name"] as? String) ?: (data["Name"] as? String) ?: docId
        var cityCandidate = (data["city"] as? String) ?: (data["City"] as? String)
            ?: (data["location"] as? String)?.split(",")?.firstOrNull()?.trim()
            ?: (data["Location"] as? String)?.split(",")?.firstOrNull()?.trim() ?: ""
        var city = if (isValidLocationName(cityCandidate)) cityCandidate.trim() else ""

        var stateCandidate = (data["state"] as? String) ?: (data["State"] as? String)
            ?: (data["location"] as? String)?.split(",")?.lastOrNull()?.trim()
            ?: (data["Location"] as? String)?.split(",")?.lastOrNull()?.trim() ?: ""
        var state = if (isValidLocationName(stateCandidate)) stateCandidate.trim() else ""

        if (city.isBlank() || state.isBlank()) {
            val (inferredCity, inferredState) = inferLocationFromName(name, docId)
            if (city.isBlank()) city = inferredCity
            if (state.isBlank()) state = inferredState
        }

        val rank = data["nirfRank"] ?: data["Rank"] ?: data["rank"]
        val score = (data["score"] as? Number)?.toDouble() ?: (data["Score"] as? Number)?.toDouble() ?: 0.0
        val instId = (data["instituteId"] as? String) ?: (data["Institute ID"] as? String) ?: docId
        val naac = (data["naacGrade"] as? String) ?: (data["accreditation"] as? String) ?: "A++"
        val fees = (data["fees"] as? Number)?.toDouble() ?: (data["tuitionFee"] as? Number)?.toDouble() ?: 0.0
        val btechFeeStr = (data["btechFee"] as? String) ?: ""
        val mtechFeeStr = (data["mtechFee"] as? String) ?: ""
        val hostelFeeStr = (data["hostelFee"] as? String) ?: ""
        val averageCtc = (data["averageCtc"] as? Number)?.toDouble() ?: (data["avgCtc"] as? Number)?.toDouble() ?: 0.0
        val avgPkgStr = (data["avgPackage"] as? String) ?: ""
        val highestPkgStr = (data["highestPackage"] as? String) ?: ""
        val reportUrl = (data["nirfReportUrl"] as? String) ?: (data["reportUrl"] as? String)
        val description = (data["description"] as? String) ?: (data["about"] as? String) ?: ""

        val metricsMap = (data["nirfMetrics"] as? Map<*, *>) ?: (data["metrics"] as? Map<*, *>)
        val metrics = metricsMap?.let {
            NirfMetrics(
                tlr = (it["tlr"] as? Number)?.toDouble() ?: 0.0,
                rpc = (it["rpc"] as? Number)?.toDouble() ?: (it["rp"] as? Number)?.toDouble() ?: 0.0,
                go = (it["go"] as? Number)?.toDouble() ?: 0.0,
                oi = (it["oi"] as? Number)?.toDouble() ?: 0.0,
                perception = (it["perception"] as? Number)?.toDouble() ?: (it["pr"] as? Number)?.toDouble() ?: 0.0
            )
        } ?: NirfMetrics(
            tlr = (data["tlr"] as? Number)?.toDouble() ?: 0.0,
            rpc = (data["rpc"] as? Number)?.toDouble() ?: (data["rp"] as? Number)?.toDouble() ?: 0.0,
            go = (data["go"] as? Number)?.toDouble() ?: 0.0,
            oi = (data["oi"] as? Number)?.toDouble() ?: 0.0,
            perception = (data["perception"] as? Number)?.toDouble() ?: (data["pr"] as? Number)?.toDouble() ?: 0.0
        )

        @Suppress("UNCHECKED_CAST")
        val courses = (data["courses"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val branches = (data["branches"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val recruiters = (data["topRecruiters"] as? List<String>) ?: emptyList()

        val cseCutoff = (data["cseCutoff"] as? String) ?: ""
        val eceCutoff = (data["eceCutoff"] as? String) ?: ""
        val meCutoff = (data["meCutoff"] as? String) ?: ""
        val imageUrl = data["imageUrl"] as? String

        val rawStream = (data["stream"] as? String) ?: (data["Stream"] as? String) ?: ""
        val stream = if (rawStream.isNotBlank()) rawStream.trim() else defaultStream.ifBlank { inferStreamFromName(name) }

        return College(
            id = docId,
            name = name,
            city = city,
            state = state,
            nirfRank = rank,
            naacGrade = naac,
            established = (data["established"] as? String) ?: "1960",
            ownership = (data["ownership"] as? String) ?: "Public / Autonomous",
            fees = fees,
            btechFee = btechFeeStr,
            mtechFee = mtechFeeStr,
            hostelFee = hostelFeeStr,
            averageCtc = averageCtc,
            avgPackageDisplay = avgPkgStr,
            highestPackage = highestPkgStr,
            score = score,
            instituteId = instId,
            nirfReportUrl = reportUrl,
            description = description,
            courses = courses,
            branches = branches,
            topRecruiters = recruiters,
            cseCutoff = cseCutoff,
            eceCutoff = eceCutoff,
            meCutoff = meCutoff,
            nirfMetrics = metrics,
            imageUrl = imageUrl,
            stream = stream
        )
    }

    private fun inferStreamFromName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("dental") -> "Dental"
            lower.contains("law") -> "Law"
            lower.contains("management") || lower.contains("iim") || lower.contains("business") -> "Management"
            lower.contains("technology") || lower.contains("engineering") || lower.contains("iit") || lower.contains("nit") -> "Engineering"
            else -> "Engineering"
        }
    }

    fun getDefaultColleges(stream: String = "All"): List<College> {
        val all = listOf(
            College(
                id = "iit-madras",
                name = "Indian Institute of Technology Madras (IIT Madras)",
                city = "Chennai",
                state = "Tamil Nadu",
                nirfRank = 1,
                naacGrade = "A++",
                established = "1959",
                ownership = "Public / Autonomous",
                fees = 220000.0,
                btechFee = "₹2.2 Lakhs/year",
                averageCtc = 22.0,
                avgPackageDisplay = "₹22.0 LPA",
                highestPackage = "₹1.98 CPA",
                score = 89.79,
                instituteId = "IR-E-U-0456",
                description = "IIT Madras is globally recognized for cutting-edge engineering, research, and technical innovation in Chennai, Tamil Nadu.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "Electrical", "Mechanical", "Aerospace"),
                topRecruiters = listOf("Google", "Microsoft", "Texas Instruments", "Qualcomm", "Apple")
            ),
            College(
                id = "iit-delhi",
                name = "Indian Institute of Technology Delhi (IIT Delhi)",
                city = "New Delhi",
                state = "Delhi",
                nirfRank = 2,
                naacGrade = "A++",
                established = "1961",
                ownership = "Public / Autonomous",
                fees = 225000.0,
                btechFee = "₹2.25 Lakhs/year",
                averageCtc = 20.5,
                avgPackageDisplay = "₹20.5 LPA",
                highestPackage = "₹2.0 CPA",
                score = 87.09,
                instituteId = "IR-E-U-0092",
                description = "IIT Delhi is a premier institute situated in Hauz Khas, New Delhi, renowned for technology & entrepreneurship.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "Math & Computing", "ECE", "Chemical", "Civil"),
                topRecruiters = listOf("Microsoft", "Google", "Goldman Sachs", "McKinsey", "Uber")
            ),
            College(
                id = "iit-bombay",
                name = "Indian Institute of Technology Bombay (IIT Bombay)",
                city = "Mumbai",
                state = "Maharashtra",
                nirfRank = 3,
                naacGrade = "A++",
                established = "1958",
                ownership = "Public / Autonomous",
                fees = 230000.0,
                btechFee = "₹2.3 Lakhs/year",
                averageCtc = 21.8,
                avgPackageDisplay = "₹21.8 LPA",
                highestPackage = "₹3.67 CPA",
                score = 83.40,
                instituteId = "IR-E-U-0306",
                description = "IIT Bombay located in Powai, Mumbai, Maharashtra is famed for its top engineering programs and placement records.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "Electrical", "Mechanical", "Aerospace", "Metallurgy"),
                topRecruiters = listOf("Google", "Jane Street", "Apple", "Sony Japan", "Microsoft")
            ),
            College(
                id = "iit-kanpur",
                name = "Indian Institute of Technology Kanpur (IIT Kanpur)",
                city = "Kanpur",
                state = "Uttar Pradesh",
                nirfRank = 4,
                naacGrade = "A++",
                established = "1959",
                ownership = "Public / Autonomous",
                fees = 215000.0,
                btechFee = "₹2.15 Lakhs/year",
                averageCtc = 19.5,
                avgPackageDisplay = "₹19.5 LPA",
                highestPackage = "₹1.9 CPA",
                score = 82.56,
                instituteId = "IR-E-U-0500",
                description = "IIT Kanpur in Kanpur, Uttar Pradesh pioneered computer science education in India and boasts world-class research facility.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "Electrical", "Aerospace", "Mechanical", "Materials Science"),
                topRecruiters = listOf("Microsoft", "Intel", "Amazon", "Goldman Sachs", "Qualcomm")
            ),
            College(
                id = "iit-roorkee",
                name = "Indian Institute of Technology Roorkee (IIT Roorkee)",
                city = "Roorkee",
                state = "Uttarakhand",
                nirfRank = 5,
                naacGrade = "A++",
                established = "1847",
                ownership = "Public / Autonomous",
                fees = 210000.0,
                btechFee = "₹2.1 Lakhs/year",
                averageCtc = 18.3,
                avgPackageDisplay = "₹18.3 LPA",
                highestPackage = "₹1.3 CPA",
                score = 78.63,
                instituteId = "IR-E-U-0560",
                description = "IIT Roorkee in Uttarakhand is Asia's oldest technical institution, offering heritage and research excellence.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "Civil", "Mechanical", "Data Science"),
                topRecruiters = listOf("Amazon", "Microsoft", "Oracle", "Flipkart", "TATA")
            ),
            College(
                id = "iit-kharagpur",
                name = "Indian Institute of Technology Kharagpur (IIT Kharagpur)",
                city = "Kharagpur",
                state = "West Bengal",
                nirfRank = 6,
                naacGrade = "A++",
                established = "1951",
                ownership = "Public / Autonomous",
                fees = 220000.0,
                btechFee = "₹2.2 Lakhs/year",
                averageCtc = 19.1,
                avgPackageDisplay = "₹19.1 LPA",
                highestPackage = "₹2.6 CPA",
                score = 77.88,
                instituteId = "IR-E-U-0570",
                description = "IIT Kharagpur in West Bengal is the largest IIT campus offering diverse engineering, technology, and management programs.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "Mining", "Industrial", "Agriculture"),
                topRecruiters = listOf("Google", "Honeywell", "Barclays", "Samsung", "Deloitte")
            ),
            College(
                id = "iit-guwahati",
                name = "Indian Institute of Technology Guwahati (IIT Guwahati)",
                city = "Guwahati",
                state = "Assam",
                nirfRank = 7,
                naacGrade = "A++",
                established = "1994",
                ownership = "Public / Autonomous",
                fees = 210000.0,
                btechFee = "₹2.1 Lakhs/year",
                averageCtc = 17.8,
                avgPackageDisplay = "₹17.8 LPA",
                highestPackage = "₹1.2 CPA",
                score = 74.90,
                instituteId = "IR-E-U-0053",
                description = "IIT Guwahati located on the banks of Brahmaputra river in Assam provides world-class educational and scientific opportunities.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "Biosciences", "Design", "Chemical"),
                topRecruiters = listOf("Microsoft", "Uber", "Oracle", "Texas Instruments", "NVIDIA")
            ),
            College(
                id = "iit-hyderabad",
                name = "Indian Institute of Technology Hyderabad (IIT Hyderabad)",
                city = "Sangareddy",
                state = "Telangana",
                nirfRank = 8,
                naacGrade = "A++",
                established = "2008",
                ownership = "Public / Autonomous",
                fees = 220000.0,
                btechFee = "₹2.2 Lakhs/year",
                averageCtc = 20.1,
                avgPackageDisplay = "₹20.1 LPA",
                highestPackage = "₹1.1 CPA",
                score = 74.12,
                instituteId = "IR-E-U-0013",
                description = "IIT Hyderabad in Telangana is celebrated for fast growth, futuristic AI research, and high technology innovation.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "AI", "ECE", "Biomedical", "Materials"),
                topRecruiters = listOf("TSMC", "Microsoft", "Google", "Qualcomm", "Amazon")
            ),
            College(
                id = "nit-trichy",
                name = "National Institute of Technology Tiruchirappalli (NIT Trichy)",
                city = "Tiruchirappalli",
                state = "Tamil Nadu",
                nirfRank = 9,
                naacGrade = "A++",
                established = "1964",
                ownership = "Public / Autonomous",
                fees = 145000.0,
                btechFee = "₹1.45 Lakhs/year",
                averageCtc = 15.6,
                avgPackageDisplay = "₹15.6 LPA",
                highestPackage = "₹52.8 LPA",
                score = 69.71,
                instituteId = "IR-E-U-0473",
                description = "NIT Trichy in Tamil Nadu ranks #1 among all NITs in India with stellar academic curriculum and campus placements.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "EEE", "Mechanical", "Production"),
                topRecruiters = listOf("Microsoft", "Amazon", "TATA Steel", "L&T", "Ford")
            ),
            College(
                id = "jadavpur-university",
                name = "Jadavpur University",
                city = "Kolkata",
                state = "West Bengal",
                nirfRank = 10,
                naacGrade = "A++",
                established = "1955",
                ownership = "Public State University",
                fees = 10000.0,
                btechFee = "₹10,000/year",
                averageCtc = 14.8,
                avgPackageDisplay = "₹14.8 LPA",
                highestPackage = "₹1.4 CPA",
                score = 67.04,
                instituteId = "IR-E-U-0584",
                description = "Jadavpur University in Kolkata, West Bengal offers top-class technical education at extremely subsidized fee with outstanding ROI.",
                courses = listOf("B.E.", "M.E.", "Ph.D"),
                branches = listOf("CSE", "ETCE", "IT", "Electrical", "Mechanical"),
                topRecruiters = listOf("Google", "Texas Instruments", "PwC", "Samsug R&D", "Wells Fargo")
            ),
            College(
                id = "vit-vellore",
                name = "Vellore Institute of Technology (VIT Vellore)",
                city = "Vellore",
                state = "Tamil Nadu",
                nirfRank = 11,
                naacGrade = "A++",
                established = "1984",
                ownership = "Private Deemed University",
                fees = 198000.0,
                btechFee = "₹1.98 Lakhs/year",
                averageCtc = 9.2,
                avgPackageDisplay = "₹9.2 LPA",
                highestPackage = "₹1.02 CPA",
                score = 66.59,
                instituteId = "IR-E-U-0491",
                description = "VIT Vellore in Tamil Nadu is a leading private university offering extensive technological labs and international exposure.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "AI & Robotics", "Biotechnology", "IT"),
                topRecruiters = listOf("Amazon", "Cognizant", "Infosys", "TCS", "Wipro")
            ),
            College(
                id = "nitk-surathkal",
                name = "National Institute of Technology Karnataka (NIT Surathkal)",
                city = "Surathkal",
                state = "Karnataka",
                nirfRank = 12,
                naacGrade = "A++",
                established = "1960",
                ownership = "Public / Autonomous",
                fees = 150000.0,
                btechFee = "₹1.5 Lakhs/year",
                averageCtc = 15.2,
                avgPackageDisplay = "₹15.2 LPA",
                highestPackage = "₹54.0 LPA",
                score = 66.08,
                instituteId = "IR-E-U-0237",
                description = "NIT Surathkal in Mangalore, Karnataka features its own private beach and premier engineering faculties.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "Information Technology", "EEE", "Mechanical"),
                topRecruiters = listOf("Microsoft", "Google", "DE Shaw", "Uber", "Qualcomm")
            ),
            College(
                id = "anna-university",
                name = "Anna University (CEG Campus)",
                city = "Chennai",
                state = "Tamil Nadu",
                nirfRank = 13,
                naacGrade = "A++",
                established = "1978",
                ownership = "Public State University",
                fees = 55000.0,
                btechFee = "₹55,000/year",
                averageCtc = 10.5,
                avgPackageDisplay = "₹10.5 LPA",
                highestPackage = "₹36.0 LPA",
                score = 65.20,
                instituteId = "IR-E-U-0439",
                description = "Anna University CEG Campus in Guindy, Chennai, Tamil Nadu is one of India's premier technical institutions.",
                courses = listOf("B.E.", "B.Tech", "M.E."),
                branches = listOf("CSE", "ECE", "IT", "Mechanical", "Civil"),
                topRecruiters = listOf("TCS", "Infosys", "Bosch", "Caterpillar", "Accenture")
            ),
            College(
                id = "iit-indore",
                name = "Indian Institute of Technology Indore (IIT Indore)",
                city = "Indore",
                state = "Madhya Pradesh",
                nirfRank = 14,
                naacGrade = "A++",
                established = "2009",
                ownership = "Public / Autonomous",
                fees = 220000.0,
                btechFee = "₹2.2 Lakhs/year",
                averageCtc = 18.5,
                avgPackageDisplay = "₹18.5 LPA",
                highestPackage = "₹68.0 LPA",
                score = 64.10,
                instituteId = "IR-E-U-0270",
                description = "IIT Indore in Madhya Pradesh excels in research citations and high industry collaborations.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "ECE", "EE", "Mechanical", "Metallurgy"),
                topRecruiters = listOf("Microsoft", "Amazon", "Flipkart", "Goldman Sachs", "Intel")
            ),
            College(
                id = "saveetha-simats",
                name = "Saveetha School of Engineering (SIMATS)",
                city = "Chennai",
                state = "Tamil Nadu",
                nirfRank = 15,
                naacGrade = "A++",
                established = "2005",
                ownership = "Private Deemed University",
                fees = 180000.0,
                btechFee = "₹1.8 Lakhs/year",
                averageCtc = 8.5,
                avgPackageDisplay = "₹8.5 LPA",
                highestPackage = "₹38.0 LPA",
                score = 63.50,
                instituteId = "IR-E-U-0474",
                description = "Saveetha School of Engineering (SIMATS) in Thandalam, Chennai, Tamil Nadu features state-of-the-art research labs, flexible credit system, and industry-aligned CSE/AI programs.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "AI & ML", "ECE", "Biomedical", "IT"),
                topRecruiters = listOf("Cognizant", "TCS", "Virtusa", "Capgemini", "Accenture")
            ),
            College(
                id = "saveetha-sec",
                name = "Saveetha Engineering College (SEC)",
                city = "Chennai",
                state = "Tamil Nadu",
                nirfRank = 26,
                naacGrade = "A++",
                established = "2001",
                ownership = "Autonomous / Anna University",
                fees = 150000.0,
                btechFee = "₹1.5 Lakhs/year",
                averageCtc = 7.2,
                avgPackageDisplay = "₹7.2 LPA",
                highestPackage = "₹34.0 LPA",
                score = 58.40,
                instituteId = "IR-E-C-26911",
                description = "Saveetha Engineering College (SEC) in Sriperumbudur, Chennai, Tamil Nadu is an autonomous premier institution known for hands-on learning and high placement records.",
                courses = listOf("B.E.", "B.Tech", "M.E."),
                branches = listOf("CSE", "ECE", "EEE", "Mechanical", "Civil"),
                topRecruiters = listOf("Infosys", "Wipro", "HCL", "Mindtree", "TCS")
            ),
            College(
                id = "srm-ktr",
                name = "SRM Institute of Science and Technology",
                city = "Kattankulathur",
                state = "Tamil Nadu",
                nirfRank = 28,
                naacGrade = "A++",
                established = "1985",
                ownership = "Private Deemed University",
                fees = 250000.0,
                btechFee = "₹2.5 Lakhs/year",
                averageCtc = 8.8,
                avgPackageDisplay = "₹8.8 LPA",
                highestPackage = "₹1.1 CPA",
                score = 57.80,
                instituteId = "IR-E-U-0478",
                description = "SRM Institute of Science and Technology in Kattankulathur, Chennai, Tamil Nadu is a top multi-disciplinary university.",
                courses = listOf("B.Tech", "M.Tech", "Ph.D"),
                branches = listOf("CSE", "Software", "ECE", "Civil", "Mechanical"),
                topRecruiters = listOf("Amazon", "Barclays", "TCS Ninja", "Cognizant", "Deloitte")
            ),
            College(
                id = "bits-pilani",
                name = "Birla Institute of Technology and Science (BITS Pilani)",
                city = "Pilani",
                state = "Rajasthan",
                nirfRank = 20,
                naacGrade = "A++",
                established = "1964",
                ownership = "Private Deemed / Institute of Eminence",
                fees = 475000.0,
                btechFee = "₹4.75 Lakhs/year",
                averageCtc = 20.8,
                avgPackageDisplay = "₹20.8 LPA",
                highestPackage = "₹1.3 CPA",
                score = 61.20,
                instituteId = "IR-E-U-0391",
                description = "BITS Pilani in Rajasthan is India's leading non-government institute with zero-attendance policy and Practice School internships.",
                courses = listOf("B.E.", "M.E.", "Ph.D"),
                branches = listOf("Computer Science", "Electronics", "Electrical", "Mechanical", "Chemical"),
                topRecruiters = listOf("Google", "Microsoft", "Uber", "NutaniX", "Texas Instruments"),
                stream = "Engineering"
            ),
            College(
                id = "maulana-azad-dental",
                name = "Maulana Azad Institute of Dental Sciences",
                city = "New Delhi",
                state = "Delhi",
                nirfRank = 1,
                naacGrade = "A+",
                established = "1983",
                ownership = "Public / Government",
                fees = 35000.0,
                btechFee = "₹35,000/year",
                averageCtc = 9.5,
                avgPackageDisplay = "₹9.5 LPA",
                highestPackage = "₹18.0 LPA",
                score = 82.50,
                instituteId = "IR-D-U-0095",
                description = "Maulana Azad Institute of Dental Sciences is India's top ranked institution for dental education and clinical research.",
                courses = listOf("BDS", "MDS"),
                branches = listOf("Orthodontics", "Prosthodontics", "Oral Surgery", "Periodontics"),
                topRecruiters = listOf("Fortis Healthcare", "Max Healthcare", "Apollo Hospitals"),
                stream = "Dental"
            ),
            College(
                id = "nlsiu-bengaluru",
                name = "National Law School of India University (NLSIU)",
                city = "Bengaluru",
                state = "Karnataka",
                nirfRank = 1,
                naacGrade = "A++",
                established = "1987",
                ownership = "Public / Autonomous",
                fees = 320000.0,
                btechFee = "₹3.2 Lakhs/year",
                averageCtc = 18.0,
                avgPackageDisplay = "₹18.0 LPA",
                highestPackage = "₹32.0 LPA",
                score = 85.20,
                instituteId = "IR-L-U-0240",
                description = "National Law School of India University is India's premier national law university located in Bengaluru, Karnataka.",
                courses = listOf("B.A. LL.B (Hons)", "LL.M", "Ph.D"),
                branches = listOf("Corporate Law", "Constitutional Law", "Criminal Law", "Intellectual Property"),
                topRecruiters = listOf("Shardul Amarchand Mangaldas", "Cyril Amarchand", "AZB & Partners", "Trilegal"),
                stream = "Law"
            ),
            College(
                id = "iim-ahmedabad",
                name = "Indian Institute of Management Ahmedabad (IIM Ahmedabad)",
                city = "Ahmedabad",
                state = "Gujarat",
                nirfRank = 1,
                naacGrade = "A++",
                established = "1961",
                ownership = "Public / Autonomous / Institute of National Importance",
                fees = 2500000.0,
                btechFee = "₹12.5 Lakhs/year",
                averageCtc = 34.5,
                avgPackageDisplay = "₹34.5 LPA",
                highestPackage = "₹1.15 CPA",
                score = 89.10,
                instituteId = "IR-M-U-0264",
                description = "IIM Ahmedabad is globally recognized as India's premier management school for postgraduate business programs.",
                courses = listOf("MBA", "PGPX", "Ph.D"),
                branches = listOf("Finance", "Marketing", "Strategy", "Operations", "Consulting"),
                topRecruiters = listOf("McKinsey & Company", "Boston Consulting Group", "Bain & Company", "Goldman Sachs"),
                stream = "Management"
            )
        )
        if (stream.isNotBlank() && !stream.equals("All", ignoreCase = true)) {
            val filtered = all.filter { it.stream.equals(stream.trim(), ignoreCase = true) }
            return if (filtered.isNotEmpty()) filtered else all
        }
        return all
    }

    fun getUserBookmarksFlow(userId: String): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val userRef = db.collection("users").document(userId)
        val listener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptySet())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                val bookmarksList = snapshot.get("bookmarks") as? List<String>
                trySend(bookmarksList?.toSet() ?: emptySet())
            } else {
                trySend(emptySet())
            }
        }
        awaitClose { listener.remove() }
    }

    fun updateUserBookmarkInFirestore(userId: String, collegeId: String, isCurrentlyBookmarked: Boolean) {
        if (userId.isBlank() || collegeId.isBlank()) return
        val userRef = db.collection("users").document(userId)
        val update = if (isCurrentlyBookmarked) {
            FieldValue.arrayRemove(collegeId)
        } else {
            FieldValue.arrayUnion(collegeId)
        }
        userRef.update("bookmarks", update).addOnFailureListener {
            userRef.set(mapOf("bookmarks" to listOf(collegeId)), com.google.firebase.firestore.SetOptions.merge())
        }
    }

    suspend fun getReviewsForCollege(collegeId: String): List<CollegeReview> {
        return try {
            val snapshot = db.collection("colleges").document(collegeId).collection("reviews").get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                CollegeReview(
                    id = doc.id,
                    collegeId = collegeId,
                    authorName = data["authorName"] as? String ?: "Student",
                    authorEmail = data["authorEmail"] as? String ?: "",
                    verificationTag = data["verificationTag"] as? String ?: "Verified Student",
                    rating = (data["rating"] as? Number)?.toInt() ?: 5,
                    comment = data["comment"] as? String ?: "",
                    date = data["date"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun submitReview(review: CollegeReview): Boolean {
        return try {
            val ref = db.collection("colleges").document(review.collegeId).collection("reviews").document()
            val data = mapOf(
                "authorName" to review.authorName,
                "authorEmail" to review.authorEmail,
                "verificationTag" to review.verificationTag,
                "rating" to review.rating,
                "comment" to review.comment,
                "date" to review.date
            )
            ref.set(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
