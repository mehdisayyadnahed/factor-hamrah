package factor.hamrah.app

import android.app.Application
import factor.hamrah.app.db.AppDatabase
import factor.hamrah.app.worker.AutoBackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FactorHamrahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-warm the Room database on background IO thread immediately during app cold start
        // and ensure AutoBackup is scheduled if enabled in business settings
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(this@FactorHamrahApp)
                database.openHelper.readableDatabase
                val settings = database.businessSettingsDao().getSettingsDirect()
                if (settings?.autoBackupEnabled == true) {
                    AutoBackupScheduler.schedule(this@FactorHamrahApp, settings.autoBackupInterval)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
