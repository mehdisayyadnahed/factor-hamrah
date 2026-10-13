package factor.hamrah.app.worker

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import factor.hamrah.app.db.AppDatabase
import factor.hamrah.app.utils.BackupManager
import factor.hamrah.app.utils.PersianDateHelper
import java.io.File
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(context)
            val settings = database.businessSettingsDao().getSettingsDirect()

            if (settings == null || !settings.autoBackupEnabled) {
                return Result.success()
            }

            // Perform automatic backup to dedicated folder (custom SAF tree or default Factor Hamrah folder)
            val success = if (!settings.backupFolderUri.isNullOrBlank()) {
                try {
                    BackupManager.exportBackupToTreeUri(context, database, Uri.parse(settings.backupFolderUri))
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                false
            }

            if (!success) {
                BackupManager.exportBackupToFile(context, database)
            }

            // Update last backup timestamp in settings
            database.businessSettingsDao().saveSettings(
                settings.copy(lastBackupTimestamp = System.currentTimeMillis())
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

object AutoBackupScheduler {
    private const val WORK_NAME = "PeriodicInvoiceAutoBackup"

    fun schedule(context: Context, interval: String) {
        val workManager = WorkManager.getInstance(context)

        val repeatIntervalHours = when (interval.uppercase()) {
            "WEEKLY" -> 24L * 7L
            "MONTHLY" -> 24L * 30L
            else -> 24L // DAILY
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            repeatIntervalHours, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME)
    }
}
