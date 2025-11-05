package com.example.secureshe

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

@HiltAndroidApp
class SecureSheApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize external module utils
        try {
            com.example.shaktibotprobono.Utils.init(this)
        } catch (_: Throwable) { }

        // Initialize a secondary FirebaseApp for Probono (external google-services.json)
        try {
            val hasProBono = FirebaseApp.getApps(this).any { it.name == "probono" }
            if (!hasProBono) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("shaktibot-probono")
                    .setApplicationId("1:547760649801:android:2371240742d3dff76ab198")
                    .setApiKey("AIzaSyDxVgMiXTD7iRYgBzpa-xKhQ0Bxbxs54BI")
                    .setStorageBucket("shaktibot-probono.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options, "probono")
            }
        } catch (_: Throwable) { }
    }
}