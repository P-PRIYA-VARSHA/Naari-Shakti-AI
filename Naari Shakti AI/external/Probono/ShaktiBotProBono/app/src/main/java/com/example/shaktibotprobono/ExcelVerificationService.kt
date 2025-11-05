package com.example.shaktibotprobono

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import java.io.InputStreamReader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp

data class ExcelAdvocate(
    val name: String,
    val enrollmentNumber: String?,
    val state: String?,
    val additionalInfo: Map<String, String> = emptyMap()
)

data class ExcelVerificationResult(
    val isVerified: Boolean,
    val confidence: Float, // 0.0 to 1.0
    val matchedAdvocate: ExcelAdvocate?,
    val reason: String,
    val totalAdvocatesInDatabase: Int
)

class ExcelVerificationService(private val context: Context) {
    
    companion object {
        private const val TAG = "ExcelVerification"
    }
    
    private var cachedAdvocates: List<ExcelAdvocate>? = null
    // Fast lookups to improve exact matching before fuzzy search
    private var indexByEnrollment: Map<String, ExcelAdvocate> = emptyMap()
    private var indexByName: Map<String, List<ExcelAdvocate>> = emptyMap()
    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance("probono"))
    private val auth = FirebaseAuth.getInstance(FirebaseApp.getInstance("probono"))
    
    /**
     * Save verification result to Firestore for admin viewing
     */
    suspend fun saveVerificationResultToFirestore(
        lawyerId: String,
        result: ExcelVerificationResult
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 Saving verification result to Firestore for lawyer: $lawyerId")
            
            val verificationData = mapOf(
                "excelIsVerified" to result.isVerified,
                "excelConfidence" to result.confidence,
                "excelMatchedName" to (result.matchedAdvocate?.name ?: ""),
                "excelMatchedEnrollment" to (result.matchedAdvocate?.enrollmentNumber ?: ""),
                "excelReason" to result.reason,
                "excelVerificationTimestamp" to com.google.firebase.Timestamp.now(),
                "excelTotalAdvocatesInDatabase" to result.totalAdvocatesInDatabase
            )
            
            // Save to the lawyer's document
            db.collection("lawyers")
                .document(lawyerId)
                .update(verificationData)
                .await()
            
            Log.d(TAG, "✅ Verification result saved to Firestore successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save verification result to Firestore: ${e.message}", e)
        }
    }
    
    /**
     * Load advocates from embedded data
     */
    suspend fun loadAdvocatesFromEmbeddedData(): List<ExcelAdvocate> = withContext(Dispatchers.IO) {
        // Return cached data if available
        if (cachedAdvocates != null) {
            Log.d(TAG, "📋 Returning cached data (${cachedAdvocates!!.size} advocates)")
            return@withContext cachedAdvocates!!
        }
        
        // Try CSV from assets first. Fallback to embedded list if missing.
        Log.d(TAG, "🔄 Loading advocate data (CSV from assets if available)")
        val advocates = try {
            loadFromCsv(context, "advocates.csv").also {
                Log.d(TAG, "✅ Loaded ${it.size} advocates from CSV assets")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ CSV not found or invalid: ${e.message}. Falling back to embedded data")
            getEmbeddedAdvocates().also {
                Log.d(TAG, "✅ Loaded ${it.size} advocates from embedded data")
            }
        }
        
        // Update cache
        cachedAdvocates = advocates
        // Build fast lookup indexes for exact matching
        buildIndexes(advocates)
        
        advocates
    }

    /**
     * Load advocates from a CSV in app assets. Expected header: name,enrollment[,state]
     */
    private fun loadFromCsv(context: Context, assetName: String): List<ExcelAdvocate> {
        val input = context.assets.open(assetName)
        val reader = BufferedReader(InputStreamReader(input))
        val lines = reader.readLines()
        if (lines.isEmpty()) return emptyList()
        val result = mutableListOf<ExcelAdvocate>()
        // Skip header
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            // Simple CSV split — if names can contain commas, switch to a CSV parser library
            val parts = line.split(',')
            if (parts.size >= 2) {
                val name = parts[0].trim()
                val enrollment = parts[1].trim().ifEmpty { null }
                val state = parts.getOrNull(2)?.trim()
                if (name.isNotEmpty()) result += ExcelAdvocate(name, enrollment, state)
            }
        }
        return result
    }
    
    /**
     * Get embedded advocate data - Andhra Pradesh advocates
     */
    private fun getEmbeddedAdvocates(): List<ExcelAdvocate> {
        return listOf(
            ExcelAdvocate(
                name = "A Rajesh Kumar",
                enrollmentNumber = "AP/03207/2015",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Abdul Ahad Ashraf",
                enrollmentNumber = "AP/00107/2020",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Abdul Azeez",
                enrollmentNumber = "AP/653/2022",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Abdul Jameer Shaik",
                enrollmentNumber = "AP/2247/2015",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Abdul Kareem",
                enrollmentNumber = "AP/2443/2005",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Abhisagar Bezawada",
                enrollmentNumber = "AP/1113/2021",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "ABISHEK Reddy",
                enrollmentNumber = "AP/25543/2022",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Adapaka Ramanarao",
                enrollmentNumber = "AP/952/2011",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "ADARI SHANMUKHA SAI KRISHNA",
                enrollmentNumber = "AP/00976/2017",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Addagabottu Sudhakara Rao",
                enrollmentNumber = "AP/1977/2002",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Adimulam Balanjeneyulu",
                enrollmentNumber = "AP/4453/1999",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Adv Dr Veeram Muralidharareddy",
                enrollmentNumber = "AP/15062/2007",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Ajay Kumar Bodala",
                enrollmentNumber = "AP/01396/2019",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "AKKILI SANJEEVA KUMAR",
                enrollmentNumber = "AP/468/2021",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Akkinapalli Venkateswararao",
                enrollmentNumber = "APS/02524/2015",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Akula SrinivasaRao",
                enrollmentNumber = "RAP/01657/2002",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "ALLAM BHASKARA RAO",
                enrollmentNumber = "2520/2004",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "AMAJALA UMA V SWAMY",
                enrollmentNumber = "AP/2333/2008",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Amarjeet Kumar",
                enrollmentNumber = "AP/05426/2024",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "AmarNath J",
                enrollmentNumber = "AP/00259/2020",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "AMARNATH REDDY G",
                enrollmentNumber = "AP/712/1995",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Amathi Suneetha",
                enrollmentNumber = "AP/1559/2010",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Ambedkar Dunna",
                enrollmentNumber = "AP/1177/2016",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Anamika Dubey",
                enrollmentNumber = "AP/02463/2008",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Ananda Kumar Vejandla",
                enrollmentNumber = "AP/00681/2022",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Ananthu V V Brahmanandam",
                enrollmentNumber = "AP/4422/1999",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Anchi Sreekanth",
                enrollmentNumber = "AP/01280/2021",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "ANDE SUBRAMANYAM",
                enrollmentNumber = "APE/176/2009",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Angad Yadav",
                enrollmentNumber = "AP/07709/2019",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Anil Kumar Singh",
                enrollmentNumber = "AP/4519/1999",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Anshuman Singh",
                enrollmentNumber = "AP/14146/2001",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "APPALANAIDU KANNURU",
                enrollmentNumber = "AP/1097/2009",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "APPALARAJU KOLIMALI",
                enrollmentNumber = "AP/1393/2021",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "ARCHANA K M",
                enrollmentNumber = "AP/01305/2010",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "ARUN BODDU",
                enrollmentNumber = "AP/197/2019",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Aruna Kumari Dasari",
                enrollmentNumber = "AP/90/2011",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Asadi Kondamma",
                enrollmentNumber = "AP/270/1996",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Ashok Kumar Sharma",
                enrollmentNumber = "AP/1523/1993",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "ASHRAF",
                enrollmentNumber = "AP/2961/2010",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Atluri Bala Subbaiah",
                enrollmentNumber = "86/2008",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "AVL Prameela",
                enrollmentNumber = "APS/02125/2006",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Azmathunnisa",
                enrollmentNumber = "AP/802/1987",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "B Murali Mohan",
                enrollmentNumber = "AP/00669/2003",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "B S Kumar",
                enrollmentNumber = "AP/641/1994",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "B SUJANA",
                enrollmentNumber = "APS/1597/1994",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Babu Rao Gaddam",
                enrollmentNumber = "AP/183/1991",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Bagadi Tulasidas",
                enrollmentNumber = "APS/03487/2001",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Baggani Raghava",
                enrollmentNumber = "AP/02630/2022",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "BAKESWERBABU",
                enrollmentNumber = "AP/818/2007",
                state = "Andhra Pradesh"
            ),
            ExcelAdvocate(
                name = "Bakka Venkata Ramana",
                enrollmentNumber = "AP/00262/2000",
                state = "Andhra Pradesh"
            )
        )
    }
    
    private fun buildIndexes(advocates: List<ExcelAdvocate>) {
        try {
            indexByEnrollment = advocates
                .filter { it.enrollmentNumber != null }
                .associateBy { normalizeEnrollment(it.enrollmentNumber!!) }
            indexByName = advocates
                .groupBy { normalizeName(it.name) }
            Log.d(TAG, "⚙️ Built indexes: byEnrollment=${indexByEnrollment.size}, byName=${indexByName.size}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed building indexes: ${e.message}")
            indexByEnrollment = emptyMap()
            indexByName = emptyMap()
        }
    }
    
    /**
     * Verify a lawyer against the Excel database
     */
    suspend fun verifyLawyer(uploadedName: String, uploadedEnrollmentNumber: String?): ExcelVerificationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Verifying lawyer: $uploadedName (Enrollment: $uploadedEnrollmentNumber)")
            
            val advocates = cachedAdvocates ?: emptyList()
            if (advocates.isEmpty()) {
                return@withContext ExcelVerificationResult(
                    isVerified = false,
                    confidence = 0.0f,
                    matchedAdvocate = null,
                    reason = "No advocates found in database",
                    totalAdvocatesInDatabase = 0
                )
            }
            
            var bestMatch: ExcelAdvocate? = null
            var bestConfidence = 0.0f
            
            // 0) Exact-match fast path using indexes
            if (!uploadedEnrollmentNumber.isNullOrBlank()) {
                val key = normalizeEnrollment(uploadedEnrollmentNumber)
                indexByEnrollment[key]?.let { adv ->
                    val nameConf = calculateNameSimilarity(uploadedName, adv.name)
                    // If enrollment matches exactly, treat as exact overall; still factor name a bit for transparency
                    bestMatch = adv
                    bestConfidence = if (nameConf >= 0.8f) 0.99f else 0.95f
                    Log.d(TAG, "🎯 Exact enrollment index hit: ${adv.name} ${adv.enrollmentNumber} (nameConf=${(nameConf*100).toInt()}%)")
                }
            } else {
                // If no enrollment provided, try exact name index
                val nameKey = normalizeName(uploadedName)
                indexByName[nameKey]?.firstOrNull()?.let { adv ->
                    bestMatch = adv
                    bestConfidence = 0.95f
                    Log.d(TAG, "🎯 Exact name index hit: ${adv.name} ${adv.enrollmentNumber}")
                }
            }
            
            // 1) If fast path did not yield a strong result, try to find exact enrollment number match (fuzzy-aware)
            if (uploadedEnrollmentNumber != null) {
                for (advocate in advocates) {
                    if (advocate.enrollmentNumber != null) {
                        val enrollmentConfidence = calculateEnrollmentSimilarity(uploadedEnrollmentNumber, advocate.enrollmentNumber)
                        if (enrollmentConfidence >= 0.95f) { // Exact or very close enrollment match
                            val nameConfidence = calculateNameSimilarity(uploadedName, advocate.name)
                            val overallConfidence = (nameConfidence + enrollmentConfidence) / 2.0f
                            
                            Log.d(TAG, "🎯 Found enrollment match: ${advocate.name} (${advocate.enrollmentNumber}) - Name: ${(nameConfidence * 100).toInt()}%, Overall: ${(overallConfidence * 100).toInt()}%")
                            
                            if (overallConfidence > bestConfidence) {
                                bestConfidence = overallConfidence
                                bestMatch = advocate
                            }
                        }
                    }
                }
            }
            
            // 2) If no good enrollment match found, try name-based matching
            if (bestMatch == null || bestConfidence < 0.7f) {
                Log.d(TAG, "🔍 No good enrollment match found, trying name-based matching...")
                
                for (advocate in advocates) {
                    val nameConfidence = calculateNameSimilarity(uploadedName, advocate.name)
                    val enrollmentConfidence = if (uploadedEnrollmentNumber != null && advocate.enrollmentNumber != null) {
                        calculateEnrollmentSimilarity(uploadedEnrollmentNumber, advocate.enrollmentNumber)
                    } else {
                        0.0f
                    }
                    
                    // Calculate overall confidence - focus on name and enrollment number only
                    val overallConfidence = if (uploadedEnrollmentNumber != null) {
                        // If we have both name and enrollment, weight them equally
                        (nameConfidence + enrollmentConfidence) / 2.0f
                    } else {
                        // If we only have name, use name confidence
                        nameConfidence
                    }
                    
                    if (overallConfidence > bestConfidence) {
                        bestConfidence = overallConfidence
                        bestMatch = advocate
                    }
                }
            }
            
            // Lower threshold for better matching - 70% instead of 80%
            val isVerified = bestConfidence >= 0.7f // 70% confidence threshold
            
            val reason = when {
                bestConfidence >= 0.95f -> "Exact match found in database"
                bestConfidence >= 0.85f -> "Very high confidence match found"
                bestConfidence >= 0.7f -> "High confidence match found"
                bestConfidence >= 0.5f -> "Partial match found, manual review recommended"
                else -> "No significant match found in database"
            }
            
            Log.d(TAG, "✅ Verification result: $isVerified (confidence: ${(bestConfidence * 100).toInt()}%)")
            Log.d(TAG, "📋 Best match: ${bestMatch?.name} (${bestMatch?.enrollmentNumber})")
            
            ExcelVerificationResult(
                isVerified = isVerified,
                confidence = bestConfidence,
                matchedAdvocate = bestMatch,
                reason = reason,
                totalAdvocatesInDatabase = advocates.size
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verifying lawyer: ${e.message}", e)
            ExcelVerificationResult(
                isVerified = false,
                confidence = 0.0f,
                matchedAdvocate = null,
                reason = "Verification failed: ${e.message}",
                totalAdvocatesInDatabase = 0
            )
        }
    }
    
    private fun calculateNameSimilarity(name1: String, name2: String): Float {
        val normalized1 = normalizeName(name1)
        val normalized2 = normalizeName(name2)
        
        // Check for exact match first
        if (normalized1 == normalized2) return 1.0f
        
        // Check if one name contains the other (for partial matches)
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            return 0.95f
        }
        
        // Simple similarity calculation using Jaccard similarity
        val words1 = normalized1.split("\\s+".toRegex()).filter { it.isNotEmpty() }.toSet()
        val words2 = normalized2.split("\\s+".toRegex()).filter { it.isNotEmpty() }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0f
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) intersection.toFloat() / union else 0.0f
    }
    
    private fun calculateEnrollmentSimilarity(enrollment1: String, enrollment2: String): Float {
        val normalized1 = normalizeEnrollment(enrollment1)
        val normalized2 = normalizeEnrollment(enrollment2)
        
        Log.d(TAG, "🔍 Comparing enrollments: '$enrollment1' -> '$normalized1' vs '$enrollment2' -> '$normalized2'")
        
        // Fast exact match
        if (normalized1 == normalized2) {
            Log.d(TAG, "✅ Exact enrollment match found!")
            return 1.0f
        }

        // Parse structured parts (prefix, serial, year)
        val p1 = parseEnrollmentParts(enrollment1)
        val p2 = parseEnrollmentParts(enrollment2)

        if (p1 != null && p2 != null) {
            val prefixEqual = p1.prefix == p2.prefix
            val serialEqual = p1.serial == p2.serial
            val yearEqual = p1.year == p2.year

            if (serialEqual && yearEqual) return 0.98f
            if (serialEqual && prefixEqual) return 0.92f
            if (serialEqual) return 0.88f
            if (yearEqual && prefixEqual) return 0.82f
            if (yearEqual) return 0.78f
        }

        // Fallback: sequence-based similarity on digits-only using longest common substring
        val digits1 = normalized1.filter { it.isDigit() }
        val digits2 = normalized2.filter { it.isDigit() }
        val lcs = longestCommonSubstring(digits1, digits2)
        val denom = maxOf(digits1.length, digits2.length).coerceAtLeast(1)
        val seqSim = lcs.toFloat() / denom
        Log.d(TAG, "🔍 LCS digit similarity: ${(seqSim * 100).toInt()}% (lcs=$lcs, len1=${digits1.length}, len2=${digits2.length})")
        return seqSim * 0.8f // keep conservative weight
    }

    private data class EnrollmentParts(
        val prefix: String?,
        val serial: String,
        val year: String?
    )

    private fun parseEnrollmentParts(rawInput: String): EnrollmentParts? {
        return try {
            val raw = rawInput.uppercase().trim()
            val norm = raw
                .replace("APS", "AP")
                .replace("RAP", "AP")
                .replace("APE", "AP")
            val regex = Regex("^([A-Z]{1,4})?[^0-9]*([0-9]{1,6})[^0-9]*([0-9]{4})?")
            val m = regex.find(norm) ?: return null
            val prefix = m.groupValues.getOrNull(1)?.ifBlank { null }
            val serial = m.groupValues.getOrNull(2)?.trim() ?: return null
            val year = m.groupValues.getOrNull(3)?.ifBlank { null }
            EnrollmentParts(prefix, serial, year)
        } catch (e: Exception) {
            null
        }
    }

    private fun longestCommonSubstring(a: String, b: String): Int {
        if (a.isEmpty() || b.isEmpty()) return 0
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        var maxLen = 0
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                if (a[i - 1] == b[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                    if (dp[i][j] > maxLen) maxLen = dp[i][j]
                }
            }
        }
        return maxLen
    }

    private fun normalizeName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("[^a-z]"), " ") // keep letters, normalize everything else to space
            .replace(Regex("\\s+"), " ") // collapse spaces
            .trim()
    }

    private fun normalizeEnrollment(enrollment: String): String {
        // Uppercase and trim
        val raw = enrollment.uppercase().trim()
        Log.d(TAG, "🔄 Normalizing enrollment: '$enrollment' -> '$raw'")
        // Normalize common prefixes then strip non-alphanumerics so that AP/1234-2000 == AP12342000
        val replaced = raw
            .replace("APS", "AP")
            .replace("RAP", "AP")
            .replace("APE", "AP")
        val normalized = replaced.replace(Regex("[^A-Z0-9]"), "")
        Log.d(TAG, "✅ Normalized enrollment: '$normalized'")
        return normalized
    }
    
    /**
     * Clear cache (useful for testing)
     */
    fun clearCache() {
        cachedAdvocates = null
        Log.d(TAG, "🗑️ Cache cleared")
    }
    
    /**
     * Force reload advocates from CSV (useful when CSV is updated)
     */
    suspend fun forceReloadAdvocates(): List<ExcelAdvocate> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Force reloading advocates from CSV...")
        clearCache()
        loadAdvocatesFromEmbeddedData()
    }
    
    /**
     * Get cache status
     */
    fun getCacheStatus(): String {
        return if (cachedAdvocates != null) {
            "Cache: ${cachedAdvocates!!.size} advocates loaded"
        } else {
            "Cache: Empty"
        }
    }
} 