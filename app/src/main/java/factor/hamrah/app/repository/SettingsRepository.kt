package factor.hamrah.app.repository

import factor.hamrah.app.db.BusinessSettingsDao
import factor.hamrah.app.model.BusinessSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val businessSettingsDao: BusinessSettingsDao) {

    val settings: Flow<BusinessSettings?> = businessSettingsDao.getSettings()

    suspend fun getSettingsDirect(): BusinessSettings {
        return businessSettingsDao.getSettingsDirect() ?: BusinessSettings()
    }

    suspend fun updateSettings(settings: BusinessSettings) {
        businessSettingsDao.saveSettings(settings)
    }
}
