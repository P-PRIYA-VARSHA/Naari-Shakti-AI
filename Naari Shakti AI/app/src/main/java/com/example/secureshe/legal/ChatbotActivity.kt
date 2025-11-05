package com.example.secureshe.legal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.provider.MediaStore
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

class ChatbotActivity : AppCompatActivity() {
    private lateinit var chatContainer: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var sendBtn: Button
    private lateinit var micButton: ImageButton

    private val REQUEST_CODE_SPEECH_INPUT = 100

    private var selectedCategory: String = ""
    private var currentQuestion = 0
    private val answers = mutableListOf<String>()
    private var draft: String = ""
    private var filingGuidance: String = ""

    private val baseQuestions = listOf(
        "What is your full name?",
        "Which city and state are you from?",
        "What specific legal issue are you facing?",
        "When did this incident occur? (date/time)",
        "Please describe the incident in detail.",
        "Do you have any evidence or witnesses?",
        "Have you contacted any authorities already?",
        "What outcome are you seeking?"
    )

    private val jurisdictionQuestions = listOf(
        "What is your state of residence?",
        "Which district/city court has jurisdiction?",
        "Are you filing in state or federal court?",
        "What is the statute of limitations for your case?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        selectedCategory = intent.getStringExtra("selected_category") ?: "General Legal Issue"

        chatContainer = findViewById(R.id.chatMessagesContainer)
        inputField = findViewById(R.id.userInputEditText)
        sendBtn = findViewById(R.id.sendButton)
        scrollView = findViewById(R.id.chatScrollView)
        micButton = findViewById(R.id.micButton)

        addWelcomeMessage()
        askQuestion()

        sendBtn.setOnClickListener {
            val userInput = inputField.text.toString().trim()
            if (userInput.isNotEmpty()) {
                addMessage("👤 You: $userInput")
                answers.add(userInput)
                inputField.setText("")
                currentQuestion++

                if (currentQuestion < getQuestionsForCategory().size) {
                    askQuestion()
                } else {
                    generateComplaintDraft()
                    generateFilingGuidance()
                    saveToFirebase()
                    addActionButtons()
                }
            }
        }

        micButton.setOnClickListener {
            promptSpeechInput()
        }
    }

    private fun addWelcomeMessage() {
        addMessage("🤖 Legal Assistant: Welcome to the Legal Help Center!")
        addMessage("🤖 Legal Assistant: I'm here to help you with your $selectedCategory issue.")
        addMessage("🤖 Legal Assistant: I'll ask you some questions to understand your situation better and generate the appropriate legal documents.")
    }

    private fun getQuestionsForCategory(): List<String> {
        return when (selectedCategory) {
            "Criminal Law" -> baseQuestions + listOf(
                "Were you arrested or charged?",
                "What is the specific criminal offense?",
                "Do you have a lawyer?"
            )
            "Civil Law" -> baseQuestions + listOf(
                "What is the estimated monetary value of your claim?",
                "Who is the opposing party?",
                "Do you have any contracts or agreements?"
            )
            "Family Law" -> baseQuestions + listOf(
                "Are there children involved?",
                "What is the current marital status?",
                "Is there a history of domestic violence?"
            )
            "Employment Law" -> baseQuestions + listOf(
                "What is your job title and employer?",
                "How long have you been employed?",
                "What specific employment law violation occurred?"
            )
            "Consumer Law" -> baseQuestions + listOf(
                "What product or service is involved?",
                "What is the company name?",
                "What is the monetary value of your claim?"
            )
            else -> baseQuestions + jurisdictionQuestions
        }
    }

    private fun askQuestion() {
        val questions = getQuestionsForCategory()
        if (currentQuestion < questions.size) {
            addMessage("🤖 Bot: ${questions[currentQuestion]}")
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
        } else if (message.startsWith("📝") || message.startsWith("📋")) {
            textView.setBackgroundColor(getColor(android.R.color.holo_green_light))
            textView.setTextColor(getColor(android.R.color.black))
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

    // Launcher to let user choose save location/name
    private val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val content = buildDocumentContent()
        if (content.isBlank()) {
            Toast.makeText(this, "Nothing to save yet", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        try {
            contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            Toast.makeText(this, "Document saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildDocumentContent(): String {
        val sb = StringBuilder()
        if (draft.isNotBlank()) {
            sb.appendLine("===== Legal Document Draft =====")
            sb.appendLine(draft)
            sb.appendLine()
        }
        if (filingGuidance.isNotBlank()) {
            sb.appendLine("===== Filing Guidance =====")
            sb.appendLine(filingGuidance)
            sb.appendLine()
        }
        if (sb.isEmpty()) {
            sb.append("No content available yet. Please complete the questions to generate a draft.")
        }
        return sb.toString()
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

    private fun saveTextToDownloads(fileName: String, content: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                // Pre-Android 10 fallback: attempt direct write to public Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val outFile = java.io.File(downloadsDir, fileName)
                java.io.FileOutputStream(outFile).use { fos ->
                    fos.write(content.toByteArray(Charsets.UTF_8))
                    fos.flush()
                }
                true
            }
        } catch (e: Exception) {
            try { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() } catch (_: Throwable) {}
            false
        }
    }

    private fun generateComplaintDraft() {
        val questions = getQuestionsForCategory()
        val name = answers.getOrNull(0) ?: "Unknown"
        val location = answers.getOrNull(1) ?: "Unknown"
        val issue = answers.getOrNull(2) ?: "Legal Issue"
        val time = answers.getOrNull(3) ?: "Unknown"
        val description = answers.getOrNull(4) ?: "No description provided"

        draft = when (selectedCategory) {
            "Criminal Law" -> generateCriminalComplaint(name, location, issue, time, description)
            "Civil Law" -> generateCivilComplaint(name, location, issue, time, description)
            "Family Law" -> generateFamilyComplaint(name, location, issue, time, description)
            "Employment Law" -> generateEmploymentComplaint(name, location, issue, time, description)
            "Consumer Law" -> generateConsumerComplaint(name, location, issue, time, description)
            else -> generateGeneralComplaint(name, location, issue, time, description)
        }

        addMessage("📝 Legal Document Draft Generated:")
        addMessage(draft)
    }

    private fun generateCriminalComplaint(name: String, location: String, issue: String, time: String, description: String): String {
        return """
            📋 FIRST INFORMATION REPORT (FIR)
            
            To,
            The Officer-in-Charge,
            Police Station,
            $location.

            Subject: Complaint regarding $issue

            Dear Sir/Madam,

            I, $name, resident of $location, am writing to file a complaint regarding the following criminal offense:

            Incident Details:
            - Type of Offense: $issue
            - Date/Time: $time
            - Description: $description

            I request you to kindly register an FIR and take necessary legal action regarding this matter.

            Thank you.

            Yours sincerely,  
            $name
            Contact: [Your contact number]
        """.trimIndent()
    }

    private fun generateCivilComplaint(name: String, location: String, issue: String, time: String, description: String): String {
        return """
            📋 CIVIL COMPLAINT DRAFT
            
            IN THE COURT OF [JURISDICTION]
            
            Plaintiff: $name
            Address: $location
            
            Defendant: [Defendant Name]
            Address: [Defendant Address]

            COMPLAINT

            The plaintiff respectfully states:

            1. This is a civil action for $issue.
            2. The incident occurred on $time.
            3. Details: $description

            WHEREFORE, the plaintiff requests:
            - [Specific relief sought]
            - Costs of this action
            - Any other relief the court deems just and proper.

            Respectfully submitted,
            $name
            Date: ${java.time.LocalDate.now()}
        """.trimIndent()
    }

    private fun generateFamilyComplaint(name: String, location: String, issue: String, time: String, description: String): String {
        return """
            📋 FAMILY COURT PETITION
            
            IN THE FAMILY COURT OF [JURISDICTION]
            
            Petitioner: $name
            Address: $location
            
            Respondent: [Respondent Name]
            Address: [Respondent Address]

            PETITION FOR $issue

            The petitioner respectfully states:

            1. This petition is filed for $issue.
            2. The relevant events occurred on $time.
            3. Grounds: $description

            PRAYER:
            - Grant the relief of $issue
            - Any other relief deemed fit

            Respectfully submitted,
            $name
            Date: ${java.time.LocalDate.now()}
        """.trimIndent()
    }

    private fun generateEmploymentComplaint(name: String, location: String, issue: String, time: String, description: String): String {
        return """
            📋 EMPLOYMENT DISCRIMINATION COMPLAINT
            
            To,
            Equal Employment Opportunity Commission
            [Regional Office Address]

            Subject: Employment Discrimination Complaint

            Dear Sir/Madam,

            I, $name, resident of $location, am filing a complaint regarding employment discrimination:

            Employer: [Employer Name]
            Position: [Your Position]
            Incident Date: $time
            Issue: $issue
            Description: $description

            I request an investigation into this matter.

            Respectfully,
            $name
            Contact: [Your contact information]
        """.trimIndent()
    }

    private fun generateConsumerComplaint(name: String, location: String, issue: String, time: String, description: String): String {
        return """
            📋 CONSUMER COMPLAINT
            
            To,
            Consumer Protection Authority
            [Address]

            Subject: Consumer Complaint against [Company Name]

            Dear Sir/Madam,

            I, $name, resident of $location, am filing a consumer complaint:

            Company: [Company Name]
            Product/Service: $issue
            Date of Issue: $time
            Problem: $description

            I request appropriate action and compensation.

            Thank you.

            Sincerely,
            $name
            Contact: [Your contact information]
        """.trimIndent()
    }

    private fun generateGeneralComplaint(name: String, location: String, issue: String, time: String, description: String): String {
        return """
            📋 LEGAL COMPLAINT DRAFT
            
            To,
            The Appropriate Authority,
            $location.

            Subject: Complaint regarding $issue

            Dear Sir/Madam,

            I, $name, resident of $location, am writing to file a complaint regarding:

            Issue: $issue
            Date/Time: $time
            Description: $description

            I request you to kindly take necessary legal action regarding this matter.

            Thank you.

            Yours sincerely,  
            $name
            Contact: [Your contact number]
        """.trimIndent()
    }

    private fun generateFilingGuidance() {
        filingGuidance = when (selectedCategory) {
            "Criminal Law" -> """
                📋 FILING GUIDANCE - CRIMINAL LAW
                
                1. File FIR at nearest police station
                2. Get FIR copy with number
                3. Contact local legal aid services
                4. Consider hiring a criminal defense attorney
                5. Keep all evidence and documentation
                
                Official Links:
                - National Legal Services Authority: https://nalsa.gov.in
                - State Legal Services Authority: [Your state's website]
            """.trimIndent()
            "Civil Law" -> """
                📋 FILING GUIDANCE - CIVIL LAW
                
                1. Consult with a civil attorney
                2. File complaint in appropriate court
                3. Pay court fees
                4. Serve notice to defendant
                5. Attend court hearings
                
                Official Links:
                - Court website: [Your state's court website]
                - Legal aid: https://legal-aid.org
            """.trimIndent()
            "Family Law" -> """
                📋 FILING GUIDANCE - FAMILY LAW
                
                1. File petition in family court
                2. Consider mediation first
                3. Gather all relevant documents
                4. Hire family law attorney
                5. Attend counseling if required
                
                Official Links:
                - Family Court: [Your district's family court]
                - Mediation services: [Local mediation center]
            """.trimIndent()
            "Employment Law" -> """
                📋 FILING GUIDANCE - EMPLOYMENT LAW
                
                1. File with EEOC (if discrimination)
                2. Contact state labor board
                3. Document all incidents
                4. Consider union representation
                5. Consult employment attorney
                
                Official Links:
                - EEOC: https://www.eeoc.gov
                - State Labor Board: [Your state's website]
            """.trimIndent()
            "Consumer Law" -> """
                📋 FILING GUIDANCE - CONSUMER LAW
                
                1. File complaint with consumer protection authority
                2. Contact Better Business Bureau
                3. Consider small claims court
                4. Document all communications
                5. Keep receipts and evidence
                
                Official Links:
                - Consumer Protection: [Your state's consumer protection]
                - BBB: https://www.bbb.org
            """.trimIndent()
            else -> """
                📋 GENERAL FILING GUIDANCE
                
                1. Identify the appropriate court/authority
                2. Gather all relevant documents
                3. Consult with an attorney
                4. File within statute of limitations
                5. Keep copies of all filings
                
                Official Links:
                - Court Directory: [Your state's court directory]
                - Legal Aid: [Local legal aid services]
            """.trimIndent()
        }

        addMessage("📋 Filing Guidance:")
        addMessage(filingGuidance)
    }

    private fun saveToFirebase() {
        val complaint = hashMapOf<String, Any>(
            "category" to selectedCategory,
            "name" to (answers.getOrNull(0) ?: ""),
            "location" to (answers.getOrNull(1) ?: ""),
            "issue" to (answers.getOrNull(2) ?: ""),
            "time" to (answers.getOrNull(3) ?: ""),
            "description" to (answers.getOrNull(4) ?: ""),
            "draft" to draft,
            "filingGuidance" to filingGuidance,
            "timestamp" to System.currentTimeMillis()
        )

        val dbRef = FirebaseDatabase.getInstance().getReference("legal_complaints")
        val complaintId = dbRef.push().key ?: System.currentTimeMillis().toString()

        dbRef.child(complaintId).setValue(complaint)
            .addOnSuccessListener {
                addMessage("✅ Your legal complaint was saved securely to our database.")
            }
            .addOnFailureListener { error ->
                addMessage("❌ Error saving complaint: ${error.message}")
            }
    }

    private fun addActionButtons() {
        addMessage("🤖 Bot: Here are your next steps:")

        val rightsButton = Button(this).apply {
            text = "📚 Know Your Rights"
            setOnClickListener {
                val intent = Intent(this@ChatbotActivity, KnowYourRightsActivity::class.java)
                intent.putExtra("issue", selectedCategory)
                startActivity(intent)
            }
        }
        chatContainer.addView(rightsButton)

        val filingGuidanceButton = Button(this).apply {
            text = "📋 Filing Guidance & Portals"
            setOnClickListener {
                val intent = Intent(this@ChatbotActivity, FilingGuidanceActivity::class.java)
                intent.putExtra("category", selectedCategory)
                startActivity(intent)
            }
        }
        chatContainer.addView(filingGuidanceButton)

        val downloadButton = Button(this).apply {
            text = "📄 Download Document"
            setOnClickListener {
                val safeCategory = selectedCategory.replace(" ", "_")
                val fileName = "Legal_Document_${safeCategory}_${java.time.LocalDate.now()}.txt"
                try {
                    createDocLauncher.launch(fileName)
                } catch (e: Exception) {
                    Toast.makeText(this@ChatbotActivity, "Unable to start download: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        chatContainer.addView(downloadButton)

        val saveToDownloadsButton = Button(this).apply {
            text = "⬇️ Save to Downloads"
            setOnClickListener {
                val content = buildDocumentContent()
                if (content.isBlank()) {
                    Toast.makeText(this@ChatbotActivity, "Nothing to save yet", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val safeCategory = selectedCategory.replace(" ", "_")
                val fileName = "Legal_Document_${safeCategory}_${java.time.LocalDate.now()}.txt"
                val ok = saveTextToDownloads(fileName, content)
                if (ok) {
                    Toast.makeText(this@ChatbotActivity, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ChatbotActivity, "Save failed. Use 'Download Document' to choose a folder.", Toast.LENGTH_LONG).show()
                }
            }
        }
        chatContainer.addView(saveToDownloadsButton)

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
}


