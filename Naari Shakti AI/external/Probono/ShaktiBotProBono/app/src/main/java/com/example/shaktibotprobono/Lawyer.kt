package com.example.shaktibotprobono

import com.google.firebase.firestore.DocumentId

data class Lawyer(
    @DocumentId
    val id: String? = null,
    val userId: String? = null,
    val name: String = "",
    val barId: String = "",
    val email: String = "",
    val contactNumber: String = "",
    val state: String = "",
    val specialization: String = "",
    val languages: List<String> = listOf(),
    val gender: String = "",
    val casesHandled: String = "",
    val verified: Boolean = false,
    val availability: Boolean = true,
    val emailVerified: Boolean = false,
    val registrationDate: String = ""
)
