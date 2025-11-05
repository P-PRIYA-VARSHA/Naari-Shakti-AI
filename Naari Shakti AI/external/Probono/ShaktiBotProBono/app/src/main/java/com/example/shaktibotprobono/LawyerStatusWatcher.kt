package com.example.shaktibotprobono

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LawyerStatusWatcher() {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose { }
        val ref = FirebaseFirestore.getInstance().collection("lawyers").document(uid)
        val reg = ref.addSnapshotListener { snap, _ ->
            val status = snap?.getString("verificationStatus") ?: return@addSnapshotListener
            val reason = snap.getString("rejectionReason").orEmpty()
            when (status) {
                "approved" -> Toast.makeText(context, "Your profile is verified 🎉", Toast.LENGTH_LONG).show()
                "rejected" -> Toast.makeText(context, if (reason.isNotBlank()) "Application rejected: $reason" else "Application rejected", Toast.LENGTH_LONG).show()
            }
        }
        onDispose { reg.remove() }
    }
}


