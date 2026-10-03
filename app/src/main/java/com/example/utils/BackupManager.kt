package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.db.AppDatabase
import com.example.model.BackupData
import com.example.model.BusinessSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

object BackupManager {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun createBackupData(database: AppDatabase): BackupData = withContext(Dispatchers.IO) {
        val customers = database.customerDao().getAllCustomersDirect()
        val invoices = database.invoiceDao().getAllInvoicesDirect()
        val invoiceItems = database.invoiceDao().getAllInvoiceItemsDirect()
        val settings = database.businessSettingsDao().getSettingsDirect()

        val today = PersianDateHelper.getTodayJalali().formatFormatted()

        BackupData(
            version = 1,
            appName = "InvoiceManagerPersian",
            exportDateShamsi = today,
            exportTimestamp = System.currentTimeMillis(),
            customers = customers,
            invoices = invoices,
            invoiceItems = invoiceItems,
            settings = settings
        )
    }

    suspend fun exportBackupToJsonString(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val backupData = createBackupData(database)
        adapter.toJson(backupData)
    }

    suspend fun exportBackupToFile(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val json = exportBackupToJsonString(database)
        val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
        val fileName = "InvoiceBackup_${PersianDateHelper.getTodayJalali().formatFormatted().replace("/", "_")}_${System.currentTimeMillis()}.json"
        val backupFile = File(backupDir, fileName)

        FileOutputStream(backupFile).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }

        backupFile
    }

    suspend fun exportBackupToUri(context: Context, database: AppDatabase, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = exportBackupToJsonString(database)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackupFromJson(context: Context, jsonString: String, database: AppDatabase): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backupData = adapter.fromJson(jsonString)
                ?: return@withContext Result.failure(Exception("قالب فایل پشتیبان نامعتبر است."))

            // Restore in transaction
            if (backupData.customers.isNotEmpty()) {
                database.customerDao().deleteAll()
                database.customerDao().insertAll(backupData.customers)
            }

            if (backupData.invoices.isNotEmpty()) {
                database.invoiceDao().deleteAllInvoices()
                database.invoiceDao().deleteAllInvoiceItems()

                database.invoiceDao().insertAllInvoices(backupData.invoices)
                if (backupData.invoiceItems.isNotEmpty()) {
                    database.invoiceDao().insertAllInvoiceItems(backupData.invoiceItems)
                }
            }

            backupData.settings?.let {
                database.businessSettingsDao().saveSettings(it)
            }

            val totalRestored = backupData.customers.size + backupData.invoices.size
            Result.success(totalRestored)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromUri(context: Context, uri: Uri, database: AppDatabase): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("امکان باز کردن فایل انتخاب‌شده وجود ندارد."))
            
            val jsonString = InputStreamReader(inputStream, Charsets.UTF_8).use { it.readText() }
            restoreBackupFromJson(context, jsonString, database)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "فایل پشتیبان فاکتورها و مشتریان")
            putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان دیتابیس اپلیکیشن فاکتور ساز (JSON)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "اشتراک‌گذاری فایل پشتیبان")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareTextContent(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
