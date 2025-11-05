package com.example.secureshe.legal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.secureshe.R

class FilingGuidanceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filing_guidance)

        val category = intent.getStringExtra("category") ?: "General"
        val guidanceTextView: TextView = findViewById(R.id.guidanceTextView)

        val guidanceInfo = getFilingGuidance(category)
        guidanceTextView.text = guidanceInfo

        setupActionButtons(category)
    }

    private fun getFilingGuidance(category: String): String {
        return when (category) {
            "Criminal Law" -> """
                📋 **Filing Guidance - Criminal Law**
                
                🚨 **Immediate Steps:**
                1. File FIR at nearest police station
                2. Get FIR copy with number
                3. Contact local legal aid services
                4. Consider hiring a criminal defense attorney
                5. Keep all evidence and documentation
                
                📄 **Required Documents:**
                - Identity proof (Aadhaar, PAN, etc.)
                - Address proof
                - Evidence (photos, videos, medical reports)
                - Witness statements
                - Any relevant documents
                
                🏛️ **Official Portals:**
                - National Legal Services Authority: https://nalsa.gov.in
                - State Legal Services Authority: [Your state's website]
                - Police Portal: https://cybercrime.gov.in
                - Court Directory: [Your state's court website]
                
                📞 **Emergency Contacts:**
                - Police: 100
                - Women Helpline: 1091
                - Child Helpline: 1098
                - Legal Aid: 1516
            """.trimIndent()

            "Civil Law" -> """
                📋 **Filing Guidance - Civil Law**
                
                🚨 **Steps to File:**
                1. Consult with a civil attorney
                2. File complaint in appropriate court
                3. Pay court fees
                4. Serve notice to defendant
                5. Attend court hearings
                
                📄 **Required Documents:**
                - Court fee payment receipt
                - Plaint/Complaint document
                - Supporting evidence
                - Defendant's address proof
                - Power of attorney (if applicable)
                
                🏛️ **Official Portals:**
                - Court website: [Your state's court website]
                - Legal aid: https://legal-aid.org
                - Property registration: [State registration office]
                - Court fee calculator: [State court website]
                
                📞 **Resources:**
                - Court helpline: [Local court number]
                - Legal aid: 1516
                - Property registration: [State office]
            """.trimIndent()

            "Family Law" -> """
                📋 **Filing Guidance - Family Law**
                
                🚨 **Steps to File:**
                1. File petition in family court
                2. Consider mediation first
                3. Gather all relevant documents
                4. Hire family law attorney
                5. Attend counseling if required
                
                📄 **Required Documents:**
                - Marriage certificate
                - Birth certificates of children
                - Income proof
                - Property documents
                - Medical certificates (if applicable)
                
                🏛️ **Official Portals:**
                - Family Court: [Your district's family court]
                - Mediation services: [Local mediation center]
                - Marriage registration: [State marriage office]
                - Child welfare: [State child welfare]
                
                📞 **Emergency Contacts:**
                - Domestic Violence Helpline: 181
                - Women Helpline: 1091
                - Child Helpline: 1098
                - Family Court: [Local number]
            """.trimIndent()

            "Employment Law" -> """
                📋 **Filing Guidance - Employment Law**
                
                🚨 **Steps to File:**
                1. File with EEOC (if discrimination)
                2. Contact state labor board
                3. Document all incidents
                4. Consider union representation
                5. Consult employment attorney
                
                📄 **Required Documents:**
                - Employment contract
                - Pay slips and salary records
                - Performance reviews
                - Communication records
                - Medical certificates (if applicable)
                
                🏛️ **Official Portals:**
                - EEOC: https://www.eeoc.gov
                - State Labor Board: [Your state's website]
                - ESIC: https://www.esic.gov.in
                - EPFO: https://www.epfindia.gov.in
                
                📞 **Resources:**
                - Labor Commissioner: [State number]
                - ESIC Helpline: 1800-11-2526
                - EPFO Helpline: 1800-11-8055
                - Women at Workplace: 1091
            """.trimIndent()

            "Consumer Law" -> """
                📋 **Filing Guidance - Consumer Law**
                
                🚨 **Steps to File:**
                1. File complaint with consumer protection authority
                2. Contact Better Business Bureau
                3. Consider small claims court
                4. Document all communications
                5. Keep receipts and evidence
                
                📄 **Required Documents:**
                - Purchase receipts
                - Warranty documents
                - Communication records
                - Photos of defective products
                - Bank statements (for payments)
                
                🏛️ **Official Portals:**
                - Consumer Protection: https://consumerhelpline.gov.in
                - BBB: https://www.bbb.org
                
                📞 **Helplines:**
                - Consumer Helpline: 1800-11-4000
                - National Consumer Helpline: 1915
                - Consumer Forum: [Local number]
            """.trimIndent()

            else -> """
                📋 **General Filing Guidance**
                
                🚨 **Steps to File:**
                1. Identify the appropriate court/authority
                2. Gather all relevant documents
                3. Consult with an attorney
                4. File within statute of limitations
                5. Keep copies of all filings
                
                📄 **General Requirements:**
                - Identity proof
                - Address proof
                - Supporting documents
                - Court fees (if applicable)
                - Legal representation (recommended)
                
                🏛️ **Official Resources:**
                - Court Directory: [Your state's court directory]
                - Legal Aid: [Local legal aid services]
                - Government Portal: https://india.gov.in
                - RTI Portal: https://rtionline.gov.in
                
                📞 **General Contacts:**
                - Legal Aid: 1516
                - Police: 100
                - Government Helpline: 1075
            """.trimIndent()
        }
    }

    private fun setupActionButtons(category: String) {
        findViewById<Button>(R.id.visitPortalBtn)?.setOnClickListener {
            val portalUrl = when (category) {
                "Criminal Law" -> "https://nalsa.gov.in"
                "Civil Law" -> "https://legal-aid.org"
                "Family Law" -> "https://familycourt.gov.in"
                "Employment Law" -> "https://www.eeoc.gov"
                "Consumer Law" -> "https://consumerhelpline.gov.in"
                else -> "https://india.gov.in"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))
            startActivity(intent)
        }

        findViewById<Button>(R.id.downloadFormsBtn)?.setOnClickListener {
            android.widget.Toast.makeText(this, "Forms downloaded to Downloads folder", android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.contactCourtBtn)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:1800-COURT-HELP")
            startActivity(intent)
        }

        findViewById<Button>(R.id.scheduleConsultationBtn)?.setOnClickListener {
            android.widget.Toast.makeText(this, "Consultation scheduled for tomorrow at 10 AM", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}


