package com.example.sosalert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class KnowYourRightsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_know_your_rights)

        val issue = intent.getStringExtra("issue") ?: "General"
        val rightsTextView: TextView = findViewById(R.id.rightsTextView)

        val rightsInfo = getRightsInfo(issue)
        rightsTextView.text = rightsInfo

        setupActionButtons(issue)
    }

    private fun getRightsInfo(issue: String): String {
        return when (issue) {
            "Criminal Law" -> """
                📌 **Your Rights - Criminal Law**
                
                🛡️ **Right to Legal Representation**
                ✅ Right to free legal aid if you cannot afford a lawyer
                ✅ Right to be informed of charges against you
                ✅ Right to remain silent and not incriminate yourself
                ✅ Right to a fair and speedy trial
                
                🛡️ **Right to Protection**
                ✅ Right to file FIR at any police station
                ✅ Right to get a copy of the FIR
                ✅ Right to protection from police harassment
                ✅ Right to medical examination if injured
                
                🛡️ **Right to Justice**
                ✅ Right to appeal against conviction
                ✅ Right to compensation for wrongful arrest
                ✅ Right to protection under Juvenile Justice Act (if minor)
                ✅ Right to witness protection if needed
                
                📞 **Emergency Contacts:**
                - Police: 100
                - Women Helpline: 1091
                - Child Helpline: 1098
                - Legal Aid: 1516
            """.trimIndent()

            "Civil Law" -> """
                📌 **Your Rights - Civil Law**
                
                🛡️ **Right to Legal Remedy**
                ✅ Right to file suit in appropriate court
                ✅ Right to seek injunction and stay orders
                ✅ Right to claim damages and compensation
                ✅ Right to appeal against court decisions
                
                🛡️ **Right to Fair Process**
                ✅ Right to be heard before any adverse action
                ✅ Right to legal representation
                ✅ Right to cross-examine witnesses
                ✅ Right to present evidence
                
                🛡️ **Right to Property**
                ✅ Right to peaceful possession of property
                ✅ Right to seek eviction of unauthorized occupants
                ✅ Right to claim rent and damages
                ✅ Right to partition of joint property
                
                📞 **Resources:**
                - Court Directory: [Your state's court website]
                - Legal Aid: https://nalsa.gov.in
                - Property Registration: [State registration office]
            """.trimIndent()

            "Family Law" -> """
                📌 **Your Rights - Family Law**
                
                🛡️ **Right to Protection**
                ✅ Protection under Domestic Violence Act, 2005
                ✅ Right to residence in shared household
                ✅ Right to maintenance and monetary relief
                ✅ Right to protection orders against abuser
                
                🛡️ **Right to Divorce**
                ✅ Right to file for divorce on various grounds
                ✅ Right to claim alimony and maintenance
                ✅ Right to custody of children (best interest)
                ✅ Right to visitation rights
                
                🛡️ **Right to Inheritance**
                ✅ Equal inheritance rights under Hindu Succession Act
                ✅ Right to claim maintenance from ancestral property
                ✅ Right to challenge unfair wills
                ✅ Right to claim family pension
                
                📞 **Emergency Contacts:**
                - Domestic Violence Helpline: 181
                - Women Helpline: 1091
                - Child Helpline: 1098
                - Family Court: [Local family court]
            """.trimIndent()

            "Employment Law" -> """
                📌 **Your Rights - Employment Law**
                
                🛡️ **Right to Fair Treatment**
                ✅ Protection against discrimination and harassment
                ✅ Right to equal pay for equal work
                ✅ Right to safe working conditions
                ✅ Right to reasonable working hours
                
                🛡️ **Right to Benefits**
                ✅ Right to minimum wages
                ✅ Right to paid leave and holidays
                ✅ Right to social security benefits
                ✅ Right to gratuity and provident fund
                
                🛡️ **Right to Redressal**
                ✅ Right to file complaint with Labor Commissioner
                ✅ Right to approach Industrial Tribunal
                ✅ Right to reinstatement if wrongfully terminated
                ✅ Right to compensation for workplace injuries
                
                📞 **Resources:**
                - Labor Commissioner: [State labor office]
                - ESIC: https://www.esic.gov.in
                - EPFO: https://www.epfindia.gov.in
                - Employment Tribunal: [Local tribunal]
            """.trimIndent()

            "Consumer Law" -> """
                📌 **Your Rights - Consumer Law**
                
                🛡️ **Right to Information**
                ✅ Right to know product details and pricing
                ✅ Right to accurate advertising and labeling
                ✅ Right to clear terms and conditions
                ✅ Right to product safety information
                
                🛡️ **Right to Redressal**
                ✅ Right to file complaint with Consumer Forum
                ✅ Right to replacement or refund
                ✅ Right to compensation for damages
                ✅ Right to class action lawsuits
                
                🛡️ **Right to Protection**
                ✅ Protection against unfair trade practices
                ✅ Right to cooling-off period for online purchases
                ✅ Protection against defective products
                ✅ Right to data privacy and protection
                
                📞 **Resources:**
                - Consumer Helpline: 1800-11-4000
                - National Consumer Helpline: 1915
                - Consumer Forum: [Local consumer court]
                - Better Business Bureau: https://www.bbb.org
            """.trimIndent()

            else -> """
                📌 **Your General Legal Rights**
                
                🛡️ **Constitutional Rights**
                ✅ Right to equality and non-discrimination
                ✅ Right to freedom of speech and expression
                ✅ Right to life and personal liberty
                ✅ Right to free legal aid
                
                🛡️ **Right to Justice**
                ✅ Right to approach any court for justice
                ✅ Right to file PIL for public interest
                ✅ Right to approach Human Rights Commission
                ✅ Right to seek compensation for violations
                
                🛡️ **Right to Information**
                ✅ Right to file RTI application
                ✅ Right to access government documents
                ✅ Right to know about government decisions
                ✅ Right to transparency in governance
                
                📞 **Emergency Contacts:**
                - Police: 100
                - Ambulance: 102
                - Fire: 101
                - Women Helpline: 1091
                - Child Helpline: 1098
                - Senior Citizen Helpline: 14567
            """.trimIndent()
        }
    }

    private fun setupActionButtons(issue: String) {
        // Contact Legal Aid Button
        findViewById<Button>(R.id.contactLegalAidBtn)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:1800-LEGAL-AID")
            startActivity(intent)
        }

        // Download Rights Guide Button
        findViewById<Button>(R.id.downloadRightsBtn)?.setOnClickListener {
            // In a real app, this would download a PDF guide
            android.widget.Toast.makeText(this, "Rights guide downloaded", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Emergency Contacts Button
        findViewById<Button>(R.id.emergencyContactsBtn)?.setOnClickListener {
            showEmergencyContacts(issue)
        }
    }

    private fun showEmergencyContacts(issue: String) {
        val contacts = when (issue) {
            "Criminal Law" -> """
                🚨 **Emergency Contacts - Criminal Law**
                
                Police: 100
                Women Helpline: 1091
                Child Helpline: 1098
                Legal Aid: 1516
                Anti-Corruption: 1064
                Cyber Crime: 1930
            """.trimIndent()
            
            "Family Law" -> """
                🚨 **Emergency Contacts - Family Law**
                
                Domestic Violence Helpline: 181
                Women Helpline: 1091
                Child Helpline: 1098
                Senior Citizen Helpline: 14567
                Family Court: [Local number]
                Legal Aid: 1516
            """.trimIndent()
            
            "Employment Law" -> """
                🚨 **Emergency Contacts - Employment Law**
                
                Labor Commissioner: [State number]
                ESIC Helpline: 1800-11-2526
                EPFO Helpline: 1800-11-8055
                Women at Workplace: 1091
                Legal Aid: 1516
            """.trimIndent()
            
            else -> """
                🚨 **General Emergency Contacts**
                
                Police: 100
                Ambulance: 102
                Fire: 101
                Women Helpline: 1091
                Child Helpline: 1098
                Senior Citizen: 14567
                Legal Aid: 1516
            """.trimIndent()
        }

        android.widget.Toast.makeText(this, contacts, android.widget.Toast.LENGTH_LONG).show()
    }
} 