package com.example.secureshe.legal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.secureshe.R
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class AutoChatbotActivity : AppCompatActivity() {
    private lateinit var chatContainer: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var sendBtn: Button
    private lateinit var micButton: ImageButton

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private var conversationContext = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        chatContainer = findViewById(R.id.chatMessagesContainer)
        inputField = findViewById(R.id.userInputEditText)
        sendBtn = findViewById(R.id.sendButton)
        scrollView = findViewById(R.id.chatScrollView)
        micButton = findViewById(R.id.micButton)

        addWelcomeMessage()

        sendBtn.setOnClickListener {
            val userInput = inputField.text.toString().trim()
            if (userInput.isNotEmpty()) {
                addMessage("👤 You: $userInput")
                val response = generateAutomaticResponse(userInput)
                addMessage("🤖 Legal Assistant: $response")
                inputField.setText("")
            }
        }

        micButton.setOnClickListener { promptSpeechInput() }
    }

    private fun addWelcomeMessage() {
        addMessage("🤖 Legal Assistant: Hello! I'm your AI legal assistant. I can help you with:")
        addMessage("🤖 Legal Assistant: • Legal advice and information")
        addMessage("🤖 Legal Assistant: • Document generation")
        addMessage("🤖 Legal Assistant: • Filing guidance")
        addMessage("🤖 Legal Assistant: • Rights information")
        addMessage("🤖 Legal Assistant: Just tell me what legal issue you're facing, and I'll help you!")
    }

    private fun generateAutomaticResponse(userInput: String): String {
        val input = userInput.lowercase()
        updateContext(userInput)
        return when {
            input.contains("assault") || input.contains("attack") -> {
                "I understand you're dealing with an assault case. This is a serious criminal offense. You should immediately file an FIR at your nearest police station. You have the right to legal representation and can seek free legal aid. Would you like me to help you draft a complaint or guide you through the filing process?"
            }
            input.contains("theft") || input.contains("stolen") -> {
                "For theft cases, you need to file an FIR immediately. Document all details including time, location, and any witnesses. Keep any evidence safe. You can also approach the consumer forum if it's related to online fraud. Should I help you prepare the complaint document?"
            }
            input.contains("fraud") || input.contains("scam") -> {
                "Fraud cases can be complex. If it's online fraud, report to cybercrime.gov.in. For financial fraud, contact your bank and file a police complaint. Document all transactions and communications. Would you like guidance on the specific steps for your type of fraud?"
            }
            input.contains("property") || input.contains("land") -> {
                "Property disputes require careful documentation. You'll need to file a civil suit in the appropriate court. Gather all property documents, agreements, and evidence. Consider mediation first as it's faster and cheaper. Would you like help with the legal complaint format?"
            }
            input.contains("contract") || input.contains("agreement") -> {
                "Contract disputes can be resolved through civil courts or arbitration. Review your contract terms carefully. If there's a breach, document all losses. You may need a civil attorney. Should I help you understand your legal options?"
            }
            input.contains("damages") || input.contains("compensation") -> {
                "For claiming damages, you need to prove the loss and establish liability. Document all expenses and losses. File a civil suit in the appropriate court. Consider hiring a civil attorney for complex cases. Would you like guidance on the claim process?"
            }
            input.contains("divorce") || input.contains("separation") -> {
                "Divorce proceedings can be filed in family court. You'll need grounds for divorce and proper documentation. Consider counseling first if possible. For mutual consent divorce, both parties must agree. Would you like information about the divorce process or help with petition drafting?"
            }
            input.contains("custody") || input.contains("child") -> {
                "Child custody decisions are based on the child's best interests. The court considers various factors including the child's age, parents' financial situation, and living conditions. You'll need to file a petition in family court. Should I help you understand the custody process?"
            }
            input.contains("domestic violence") || input.contains("abuse") -> {
                "Domestic violence is a serious crime. You have immediate protection under the Domestic Violence Act. Contact the women's helpline (1091) or domestic violence helpline (181). File an FIR and seek protection orders. You have the right to residence and maintenance. Would you like emergency contact information?"
            }
            input.contains("harassment") || input.contains("discrimination") -> {
                "Workplace harassment and discrimination are illegal. Document all incidents with dates and witnesses. File a complaint with your company's internal committee. You can also approach the labor commissioner or file a complaint with the women's commission. Would you like help with the complaint process?"
            }
            input.contains("termination") || input.contains("fired") -> {
                "Wrongful termination can be challenged. Check if proper notice was given and if you received severance pay. Document all communications and performance reviews. You can approach the labor court for reinstatement or compensation. Should I help you understand your rights?"
            }
            input.contains("salary") || input.contains("wages") -> {
                "For salary or wage disputes, you can approach the labor commissioner or file a complaint with the labor court. Document all salary slips and communications. You have the right to minimum wages and timely payment. Would you like guidance on the complaint process?"
            }
            input.contains("defective") || input.contains("product") -> {
                "For defective products, you can file a complaint with the consumer forum. Keep all receipts and warranty documents. Document the defect with photos. You're entitled to replacement, refund, or compensation. Would you like help with the consumer complaint format?"
            }
            input.contains("service") || input.contains("company") -> {
                "For service-related issues, first try to resolve with the company. If unsuccessful, file a complaint with the consumer forum. Document all communications and keep receipts. You can also approach the consumer helpline (1800-11-4000). Should I help you draft a complaint?"
            }
            input.contains("online") || input.contains("e-commerce") -> {
                "For online fraud or e-commerce issues, report to cybercrime.gov.in and file a police complaint. Contact your bank if money was involved. You can also file a consumer complaint. Document all transactions and communications. Would you like specific guidance for your case?"
            }
            input.contains("rights") || input.contains("legal rights") -> {
                "You have several fundamental rights including right to equality, right to life and liberty, right to free legal aid, and right to approach courts. For specific rights related to your issue, I can provide detailed information. What type of legal rights are you asking about?"
            }
            input.contains("court") || input.contains("filing") -> {
                "Court filing procedures vary by case type. Generally, you'll need to file in the appropriate court with proper documents and fees. Consider hiring a lawyer for complex cases. You can also seek free legal aid. Would you like specific filing guidance for your case?"
            }
            input.contains("lawyer") || input.contains("attorney") -> {
                "You can find lawyers through the bar association, legal aid services, or online directories. For free legal aid, contact the National Legal Services Authority (1516). Many lawyers offer free initial consultations. Would you like help finding legal representation?"
            }
            input.contains("evidence") || input.contains("proof") -> {
                "Evidence is crucial for legal cases. Document everything with photos, videos, and written records. Keep all relevant documents, receipts, and communications. Witness statements can be valuable. Store evidence safely and make copies. Would you like guidance on evidence collection for your specific case?"
            }
            input.contains("police") || input.contains("fir") -> {
                "You can file an FIR at any police station. Get a copy of the FIR with the number. If police refuse to register FIR, you can approach the superintendent or file a complaint with the magistrate. You have the right to free legal aid. Would you like help with the FIR process?"
            }
            input.contains("help") || input.contains("what should i do") -> {
                "I can help you with legal advice, document preparation, filing guidance, and rights information. Tell me your specific legal issue, and I'll provide targeted assistance. You can also access emergency contacts and official portals through this app."
            }
            input.contains("emergency") || input.contains("urgent") -> {
                "For emergencies, contact Police (100), Women Helpline (1091), Child Helpline (1098), or Domestic Violence Helpline (181). If you're in immediate danger, call the police immediately. Would you like me to provide specific emergency contacts for your situation?"
            }
            input.contains("document") || input.contains("complaint") -> {
                "I can help you generate legal documents based on your case. Tell me your specific legal issue, and I'll create the appropriate complaint or petition format. I can also guide you through the filing process and required documents."
            }
            input.contains("money") || input.contains("cost") -> {
                "Legal costs vary by case type. Court fees depend on the claim amount. You can seek free legal aid if you cannot afford a lawyer. Many lawyers offer payment plans. Consumer cases have minimal fees. Would you like information about costs for your specific case?"
            }
            input.contains("time") || input.contains("duration") -> {
                "Legal proceedings can take several months to years depending on the case complexity and court workload. Criminal cases may be faster than civil cases. Alternative dispute resolution like mediation can be quicker. Would you like timeline information for your specific case?"
            }
            else -> {
                "I understand you're seeking legal help. To provide the most accurate assistance, could you please tell me more specifically about your legal issue? For example, is it related to criminal law, civil disputes, family matters, employment issues, or consumer problems? I can then give you targeted guidance and help you with document preparation."
            }
        }
    }

    private fun updateContext(userInput: String) {
        val input = userInput.lowercase()
        when {
            input.contains("criminal") || input.contains("assault") || input.contains("theft") || input.contains("fraud") -> {
                conversationContext["category"] = "Criminal Law"
            }
            input.contains("civil") || input.contains("property") || input.contains("contract") -> {
                conversationContext["category"] = "Civil Law"
            }
            input.contains("family") || input.contains("divorce") || input.contains("custody") -> {
                conversationContext["category"] = "Family Law"
            }
            input.contains("employment") || input.contains("work") || input.contains("job") -> {
                conversationContext["category"] = "Employment Law"
            }
            input.contains("consumer") || input.contains("product") || input.contains("service") -> {
                conversationContext["category"] = "Consumer Law"
            }
        }

        if (input.contains("name") || input.contains("i am") || input.contains("my name")) {
            conversationContext["name"] = "User"
        }
        if (input.contains("location") || input.contains("city") || input.contains("state")) {
            conversationContext["location"] = "User's location"
        }
    }

    private fun addMessage(message: String) {
        val textView = TextView(this)
        textView.text = message
        textView.setPadding(16, 12, 16, 12)
        textView.setTextColor(getColor(android.R.color.black))
        textView.textSize = 16f
        if (message.startsWith("🤖")) {
            textView.setBackgroundColor(getColor(android.R.color.holo_blue_light))
            textView.setTextColor(getColor(android.R.color.white))
        }
        chatContainer.addView(
            textView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!text.isNullOrEmpty()) {
                inputField.setText(text)
            }
        }
    }

    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Your device doesn't support voice input", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addActionButtons() {
        addMessage("🤖 Legal Assistant: Here are some helpful resources:")

        val rightsButton = Button(this).apply {
            text = "📚 Know Your Rights"
            setOnClickListener {
                val intent = Intent(this@AutoChatbotActivity, KnowYourRightsActivity::class.java)
                intent.putExtra("issue", conversationContext["category"] ?: "General")
                startActivity(intent)
            }
        }
        chatContainer.addView(rightsButton)

        val filingGuidanceButton = Button(this).apply {
            text = "📋 Filing Guidance & Portals"
            setOnClickListener {
                val intent = Intent(this@AutoChatbotActivity, FilingGuidanceActivity::class.java)
                intent.putExtra("category", conversationContext["category"] ?: "General")
                startActivity(intent)
            }
        }
        chatContainer.addView(filingGuidanceButton)

        val emergencyButton = Button(this).apply {
            text = "🚨 Emergency Contacts"
            setOnClickListener { showEmergencyContacts() }
        }
        chatContainer.addView(emergencyButton)

        val legalAidButton = Button(this).apply {
            text = "📞 Contact Legal Aid"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:1800-LEGAL-AID")
                startActivity(intent)
            }
        }
        chatContainer.addView(legalAidButton)
    }

    private fun showEmergencyContacts() {
        val contacts = """
            🚨 **Emergency Contacts**
            
            Police: 100
            Ambulance: 102
            Fire: 101
            Women Helpline: 1091
            Child Helpline: 1098
            Domestic Violence: 181
            Senior Citizen: 14567
            Legal Aid: 1516
            Anti-Corruption: 1064
            Cyber Crime: 1930
        """.trimIndent()

        Toast.makeText(this, contacts, Toast.LENGTH_LONG).show()
    }
}


