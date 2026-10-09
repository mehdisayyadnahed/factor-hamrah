package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.BusinessSettings
import com.example.repository.SettingsRepository
import com.example.utils.BackupManager
import com.example.worker.AutoBackupScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsRepository = SettingsRepository(db.businessSettingsDao())

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded: StateFlow<Boolean> = _isSettingsLoaded.asStateFlow()

    val settings: StateFlow<BusinessSettings> = settingsRepository.settings
        .map { dbSettings ->
            _isSettingsLoaded.value = true
            dbSettings ?: BusinessSettings()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = BusinessSettings()
        )

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    fun updateSettings(updated: BusinessSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(updated)

            // Reschedule or cancel AutoBackup
            if (updated.autoBackupEnabled) {
                AutoBackupScheduler.schedule(getApplication(), updated.autoBackupInterval)
            } else {
                AutoBackupScheduler.cancel(getApplication())
            }
        }
    }

    fun updateLogoUri(uri: String?) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(logoUri = uri))
        }
    }

    fun updateSignatureUri(uri: String?) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(signatureUri = uri))
        }
    }

    /**
     * Persistently saves the chosen logo to the app's internal private storage directory.
     * This avoids losing content:// URI permissions when the application process restarts.
     */
    fun saveLogoFromUri(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                if (inputStream != null) {
                    val logosDir = File(context.filesDir, "business_assets").apply { mkdirs() }
                    val logoFile = File(logosDir, "logo_${System.currentTimeMillis()}.png")
                    logoFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()

                    // Update settings with the local persistent file path
                    val current = settingsRepository.getSettingsDirect()
                    updateSettings(current.copy(logoUri = logoFile.absolutePath))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Persistently saves the chosen seller stamp/signature to the app's internal private storage directory.
     */
    fun saveSignatureFromUri(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                if (inputStream != null) {
                    val assetsDir = File(context.filesDir, "business_assets").apply { mkdirs() }
                    val sigFile = File(assetsDir, "signature_${System.currentTimeMillis()}.png")
                    sigFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()

                    val current = settingsRepository.getSettingsDirect()
                    updateSettings(current.copy(signatureUri = sigFile.absolutePath))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateAppTheme(theme: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(appTheme = theme))
        }
    }

    fun updatePdfTheme(pdfTheme: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(pdfTheme = pdfTheme))
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(currency = currency))
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(autoBackupEnabled = enabled))
        }
    }

    fun updateAutoBackupInterval(interval: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(autoBackupInterval = interval))
        }
    }

    fun updateBackupFolderUri(uri: String?) {
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            updateSettings(current.copy(backupFolderUri = uri))
        }
    }

    fun exportManualBackup(context: Context, onComplete: (File) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val backupFile = BackupManager.exportBackupToFile(context, db)
                _backupStatusMessage.value = "پشتیبان‌گیری با موفقیت انجام شد."
                onComplete(backupFile)
                BackupManager.shareBackupFile(context, backupFile)
            } catch (e: Exception) {
                _backupStatusMessage.value = "خطا در تهیه پشتیبان: ${e.localizedMessage}"
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.restoreBackupFromUri(context, uri, db)
            result.onSuccess { count ->
                val msg = "بازیابی با موفقیت انجام شد ($count رکورد بازگردانده شد)."
                _backupStatusMessage.value = msg
                onResult(true, msg)
            }.onFailure { err ->
                val msg = "خطا در بازیابی اطلاعات: ${err.localizedMessage}"
                _backupStatusMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun clearStatusMessage() {
        _backupStatusMessage.value = null
    }
}
