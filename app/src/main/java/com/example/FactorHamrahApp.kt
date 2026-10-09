package com.example

import android.app.Application
import com.example.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FactorHamrahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-warm the Room database on background IO thread immediately during app cold start
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(this@FactorHamrahApp).openHelper.readableDatabase
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
