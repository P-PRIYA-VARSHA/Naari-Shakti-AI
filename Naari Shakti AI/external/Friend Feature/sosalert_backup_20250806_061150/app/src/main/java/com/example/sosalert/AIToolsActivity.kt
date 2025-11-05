package com.example.sosalert

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AIToolsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_tools)

        setupToolCards()
    }

    private fun setupToolCards() {
        // AI Chatbot
        findViewById<CardView>(R.id.cardAIChatbot).setOnClickListener {
            startActivity(Intent(this, EnhancedChatbotActivity::class.java))
        }

        // Document Generator → open guided document drafting flow
        findViewById<CardView>(R.id.cardDocumentGenerator).setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java).apply {
                // Optional: preselect a general category so questions tailor drafting
                putExtra("selected_category", "General Legal Issue")
            }
            startActivity(intent)
        }

        // Removed Coming Soon tiles to declutter UI

        // Rights Checker
        findViewById<CardView>(R.id.cardRightsChecker).setOnClickListener {
            startActivity(Intent(this, KnowYourRightsActivity::class.java))
        }

        // Filing Guidance
        findViewById<CardView>(R.id.cardFilingGuidance).setOnClickListener {
            startActivity(Intent(this, FilingGuidanceActivity::class.java))
        }

        // Legal Research Assistant (route to EnhancedChatbot for now)
        findViewById<CardView>(R.id.cardLegalResearch).setOnClickListener {
            startActivity(Intent(this, EnhancedChatbotActivity::class.java))
        }

        // Case Analysis (route to EnhancedChatbot for now)
        findViewById<CardView>(R.id.cardCaseAnalysis).setOnClickListener {
            startActivity(Intent(this, EnhancedChatbotActivity::class.java))
        }

        // Contract Review (route to EnhancedChatbot for now)
        findViewById<CardView>(R.id.cardContractReview).setOnClickListener {
            startActivity(Intent(this, EnhancedChatbotActivity::class.java))
        }

        // Legal Form Filler (route to ChatbotActivity for guided forms)
        findViewById<CardView>(R.id.cardFormFiller).setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    private fun showComingSoon(featureName: String) {
        Toast.makeText(this, "$featureName - Coming Soon!", Toast.LENGTH_SHORT).show()
    }
} 