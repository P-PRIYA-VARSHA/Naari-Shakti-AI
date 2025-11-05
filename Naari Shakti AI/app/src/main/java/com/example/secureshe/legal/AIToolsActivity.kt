package com.example.secureshe.legal

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.secureshe.R
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import com.example.secureshe.MainActivity

class AIToolsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_tools)

        // Open dashboard with drawer when hamburger is clicked
        findViewById<ImageButton>(R.id.menuButton)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("open_drawer", true)
            startActivity(intent)
            // Optional: close this screen to match back behavior
            finish()
        }

        setupToolCards()

        // Make the white strip under footer match system navigation bar height
        val whiteStrip: View? = findViewById(R.id.footerWhiteStrip)
        whiteStrip?.let { strip ->
            ViewCompat.setOnApplyWindowInsetsListener(strip) { v, insets ->
                val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val lp = v.layoutParams
                if (lp != null && lp.height != bottom) {
                    lp.height = bottom
                    v.layoutParams = lp
                }
                insets
            }
        }

        // Ensure returning via system back shows the menu on dashboard
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@AIToolsActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("open_drawer", true)
                startActivity(intent)
                finish()
            }
        })
    }

    private fun setupToolCards() {
        // AI Legal Assistant / Enhanced Chatbot UI
        findViewById<CardView>(R.id.cardAIChatbot).setOnClickListener {
            startActivity(Intent(this, EnhancedChatbotActivity::class.java).putExtra("mode", "assistant"))
        }

        // Legal Research Assistant (route to chatbot for now)
        findViewById<CardView>(R.id.cardLegalResearch).setOnClickListener {
            startActivity(Intent(this, EnhancedChatbotActivity::class.java).putExtra("mode", "research"))
        }

        // Case Analysis and Contract Review removed from UI

        // Legal Form Filler (route to chatbot for guided forms)
        findViewById<CardView>(R.id.cardFormFiller).setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        // Document Generator (route to chatbot)
        findViewById<CardView>(R.id.cardDocumentGenerator).setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        // Rights Checker
        findViewById<CardView>(R.id.cardRightsChecker).setOnClickListener {
            startActivity(Intent(this, KnowYourRightsActivity::class.java))
        }

        // Filing Guidance
        findViewById<CardView>(R.id.cardFilingGuidance).setOnClickListener {
            startActivity(Intent(this, FilingGuidanceActivity::class.java))
        }
    }
}


