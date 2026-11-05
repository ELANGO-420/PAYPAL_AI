package com.example.hack

import android.app.Application
import com.google.firebase.FirebaseApp

class HackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
