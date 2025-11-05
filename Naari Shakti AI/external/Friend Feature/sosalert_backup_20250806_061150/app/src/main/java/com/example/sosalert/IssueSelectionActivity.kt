package com.example.sosalert

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class IssueSelectionActivity : AppCompatActivity() {
    
    private var selectedCategory: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_issue_selection)

        setupCategoryCards()
        setupChatButton()
    }
    
    private fun setupCategoryCards() {
        val categories = mapOf(
            R.id.criminalLawCard to "Criminal Law",
            R.id.civilLawCard to "Civil Law", 
            R.id.familyLawCard to "Family Law",
            R.id.employmentLawCard to "Employment Law",
            R.id.consumerLawCard to "Consumer Law",
            R.id.otherLawCard to "Other Legal Issues"
        )
        
        categories.forEach { (cardId, categoryName) ->
            findViewById<MaterialCardView>(cardId).setOnClickListener {
                selectedCategory = categoryName
                highlightSelectedCard(cardId)
                Toast.makeText(this, "Selected: $categoryName", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun highlightSelectedCard(selectedCardId: Int) {
        // Reset all cards to default state
        val allCards = listOf(
            R.id.criminalLawCard,
            R.id.civilLawCard,
            R.id.familyLawCard,
            R.id.employmentLawCard,
            R.id.consumerLawCard,
            R.id.otherLawCard
        )
        
        allCards.forEach { cardId ->
            val card = findViewById<MaterialCardView>(cardId)
            card.strokeWidth = 0
            card.strokeColor = 0
        }
        
        // Highlight selected card
        val selectedCard = findViewById<MaterialCardView>(selectedCardId)
        selectedCard.strokeWidth = 4
        selectedCard.strokeColor = getColor(android.R.color.holo_blue_dark)
    }
    
    private fun setupChatButton() {
        findViewById<Button>(R.id.chatWithAIBtn).setOnClickListener {
            if (selectedCategory.isEmpty()) {
                Toast.makeText(this, "Please select a legal category first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val intent = Intent(this, ChatbotActivity::class.java).apply {
                putExtra("selected_category", selectedCategory)
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.autoChatbotBtn).setOnClickListener {
            val intent = Intent(this, AutoChatbotActivity::class.java)
            startActivity(intent)
        }
    }
} 