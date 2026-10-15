package factor.hamrah.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import factor.hamrah.app.db.AppDatabase
import factor.hamrah.app.model.BackupData
import factor.hamrah.app.model.BusinessSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Locale

object BackupManager {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun createBackupData(database: AppDatabase): BackupData = withContext(Dispatchers.IO) {
        val customers = database.customerDao().getAllCustomersDirect()
        val bankAccounts = database.bankAccountDao().getAllBankAccountsDirect()
        val invoices = database.invoiceDao().getAllInvoicesDirect()
        val invoiceItems = database.invoiceDao().getAllInvoiceItemsDirect()
        val settings = database.businessSettingsDao().getSettingsDirect()

        val today = PersianDateHelper.getTodayJalali().formatFormatted()

        BackupData(
            version = 1,
            appName = "Factor Hamrah",
            exportDateShamsi = today,
            exportTimestamp = System.currentTimeMillis(),
            customers = customers,
            bankAccounts = bankAccounts,
            invoices = invoices,
            invoiceItems = invoiceItems,
            settings = settings
        )
    }

    suspend fun exportBackupToJsonString(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val backupData = createBackupData(database)
        adapter.toJson(backupData)
    }

    fun getDefaultBackupDir(context: Context): File {
        val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Factor Hamrah")
        try {
            if (documentsDir.exists() || documentsDir.mkdirs()) {
                return documentsDir
            }
        } catch (_: Exception) {}

        val candidates = listOf(
            documentsDir,
            File(Environment.getExternalStorageDirectory(), "Factor Hamrah"),
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Factor Hamrah"),
            File(context.filesDir, "Factor Hamrah")
        )
        for (dir in candidates) {
            try {
                if (dir.exists() || dir.mkdirs()) {
                    return dir
                }
            } catch (_: Exception) {}
        }
        return File(context.filesDir, "Factor Hamrah").apply { mkdirs() }
    }

    fun getReadableFolderName(context: Context, uriString: String? = null): String {
        if (uriString.isNullOrBlank()) {
            return "Internal Storage/Documents/Factor Hamrah"
        }
        return try {
            val uri = Uri.parse(uriString)
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val decoded = Uri.decode(docId)
            val parts = decoded.split(":")
            val rawPath = if (parts.size > 1) parts[1] else parts[0]
            val folderPath = rawPath.trim('/')
            if (folderPath.isNotBlank()) {
                "Internal Storage/" + folderPath
            } else {
                "Internal Storage/Documents/Factor Hamrah"
            }
        } catch (_: Exception) {
            "Internal Storage/Custom Folder"
        }
    }

    fun openFolderInFileManager(context: Context, uriString: String?): Boolean {
        // 1. Try with user-selected SAF Tree Uri
        if (!uriString.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(uriString)
                val docId = try { DocumentsContract.getTreeDocumentId(treeUri) } catch (_: Exception) { null }
                val docUri = if (docId != null) {
                    try {
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    } catch (_: Exception) {
                        treeUri
                    }
                } else {
                    treeUri
                }

                val intents = listOf(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(docUri, DocumentsContract.Document.MIME_TYPE_DIR)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(treeUri, DocumentsContract.Document.MIME_TYPE_DIR)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(docUri, "resource/folder")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    Intent(Intent.ACTION_VIEW).apply {
                        data = docUri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )

                for (intent in intents) {
                    try {
                        context.startActivity(intent)
                        return true
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        // 2. Default backup directory
        val defaultDir = getDefaultBackupDir(context)
        if (!defaultDir.exists()) {
            try { defaultDir.mkdirs() } catch (_: Exception) {}
        }

        val defaultDocUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FFactor%20Hamrah")
        val defaultTreeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FFactor%20Hamrah")

        val fallbackIntents = mutableListOf<Intent>(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(defaultDocUri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(defaultTreeUri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        try {
            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                defaultDir
            )
            fallbackIntents.add(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileProviderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            fallbackIntents.add(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileProviderUri, "resource/folder")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {}

        for (intent in fallbackIntents) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }

        // 3. System storage volume / DocumentsUI fallback (Android Q+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
                val intent = storageManager?.primaryStorageVolume?.createOpenDocumentTreeIntent()
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        // 4. General document tree browser fallback
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (_: Exception) {}

        return false
    }

    fun generateBackupFileName(): String {
        val today = PersianDateHelper.getTodayJalali()
        val datePart = String.format(Locale.US, "%04d_%02d_%02d", today.year, today.month, today.day)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val timePart = String.format(Locale.US, "%02d_%02d_%02d", hour, minute, second)
        return "Factor_Hamrah_Backup_${datePart}_${timePart}.json"
    }

    suspend fun exportBackupToFile(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val json = exportBackupToJsonString(database)
        val backupDir = getDefaultBackupDir(context)
        val fileName = generateBackupFileName()
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

    suspend fun exportBackupToTreeUri(context: Context, database: AppDatabase, treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = exportBackupToJsonString(database)
            val fileName = generateBackupFileName()
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val newDocUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocUri,
                "application/json",
                fileName
            ) ?: return@withContext false

            context.contentResolver.openOutputStream(newDocUri)?.use { out ->
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

            if (backupData.bankAccounts.isNotEmpty()) {
                database.bankAccountDao().deleteAll()
                database.bankAccountDao().insertAll(backupData.bankAccounts)
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

            val totalRestored = backupData.customers.size + backupData.bankAccounts.size + backupData.invoices.size
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
            putExtra(Intent.EXTRA_SUBJECT, "فایل پشتیبان فاکتور همراه")
            putExtra(Intent.EXTRA_TEXT, "فایل پشتیبان دیتابیس فاکتورها، مشتریان و حساب‌های بانکی اپلیکیشن فاکتور همراه (JSON)")
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
