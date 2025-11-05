package com.example.shaktibotprobono

data class Admin(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "admin"
)

data class AdminLawyer(
    val id: String = "",
    val firebaseUserId: String = "",
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
    val verificationStatus: String = "pending", // pending, approved, rejected
    val rejectionReason: String? = null,
    val registrationDate: String = "",
    val documents: List<String> = listOf(),
    val photos: List<String> = listOf()
)