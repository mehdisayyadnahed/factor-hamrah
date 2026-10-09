package com.example.repository

import com.example.db.BusinessSettingsDao
import com.example.model.BusinessSettings
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
