package com.example.secureshe.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(context: Context) {

    private val prefs: SharedPreferences = initEncryptedPrefsSafely(context)

    private fun initEncryptedPrefsSafely(context: Context): SharedPreferences {
        val mainName = "secure_pin_prefs"
        val keysetName = "${mainName}__androidx_security_crypto_encrypted_prefs__"
        // 1) Try normal encrypted prefs
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                mainName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.e("EncryptionHelper", "Failed to init EncryptedSharedPreferences (first attempt)", e)
        }

        // 2) If failed, delete possibly corrupted stores and retry once
        try {
            context.deleteSharedPreferences(mainName)
            // Keyset may be stored under a separate file name; attempt delete as well
            context.deleteSharedPreferences(keysetName)
        } catch (e: Throwable) {
            Log.w("EncryptionHelper", "Failed to delete corrupted encrypted prefs", e)
        }
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                mainName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.e("EncryptionHelper", "Failed to init EncryptedSharedPreferences after reset; falling back", e)
        }

        // 3) Last-resort fallback to plain SharedPreferences to avoid crashing app
        //    (still scoped to the same name but unencrypted). This prevents crash loops.
        return context.getSharedPreferences("${mainName}_fallback", Context.MODE_PRIVATE)
    }

    fun encryptPin(pin: String): String {
        // Store securely if possible; API kept intact
        prefs.edit().putString("encrypted_pin", pin).apply()
        return pin
    }

    fun decryptPin(): String? {
        return prefs.getString("encrypted_pin", null)
    }

    fun storeEncryptedPin(pin: String) {
        prefs.edit().putString("encrypted_pin", pin).apply()
    }

    fun clearEncryptedPin() {
        prefs.edit().remove("encrypted_pin").apply()
    }
}