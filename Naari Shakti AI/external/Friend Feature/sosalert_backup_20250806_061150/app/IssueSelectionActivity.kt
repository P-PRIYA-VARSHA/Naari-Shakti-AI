package com.example.sosalert

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class IssueSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_issue_selection)

        val issueSpinner: Spinner = findViewById(R.id.issueSpinner)
        val issues: Array<String> = arrayOf(
            "Domestic Violence",
            "Cyberbullying",
            "Stalking",
            "Workplace Harassment",
            "Custom"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, issues)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        issueSpinner.adapter = adapter

        val nextButton: Button = findViewById(R.id.nextButton)
        nextButton.setOnClickListener {
            val selectedIssue = issueSpinner.selectedItem.toString()
            val intent = Intent(this, ChatbotActivity::class.java)
            intent.putExtra("issue", selectedIssue)
            startActivity(intent)
        }
    }
}
