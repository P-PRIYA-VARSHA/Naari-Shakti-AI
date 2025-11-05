package com.example.shaktibotprobono

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
    }
}


