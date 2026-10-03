package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "business_settings")
@JsonClass(generateAdapter = true)
data class BusinessSettings(
    @PrimaryKey
    val id: Int = 1,
    val businessName: String = "فروشگاه و خدمات بازرگانی",
    val businessPhone: String = "",
    val businessAddress: String = "",
    val economicCode: String = "",
    val logoUri: String? = null,
    val signatureUri: String? = null,
    val currency: String = "تومان",
    val defaultFooterNote: String = "از خرید و حسن اعتماد شما سپاسگزاریم.",

    // App Theme (SYSTEM, LIGHT, DARK)
    val appTheme: String = "SYSTEM",

    // PDF Color Theme (CRIMSON, NAVY, EMERALD, PURPLE, SLATE, AMBER)
    val pdfTheme: String = "CRIMSON",

    // Auto Backup
    val autoBackupEnabled: Boolean = false,
    val autoBackupInterval: String = "DAILY", // DAILY, WEEKLY, MONTHLY
    val backupFolderUri: String? = null,
    val lastBackupTimestamp: Long = 0L
)
