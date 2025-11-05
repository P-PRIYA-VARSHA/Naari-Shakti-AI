package com.example.secureshe.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val pin: String = "",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Parcelize
data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = ""
) : Parcelable

data class SOSVideo(
    val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val localPath: String = "",
    val cloudPath: String = "",
    val isUploaded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) 