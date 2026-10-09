package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val appName: String = "InvoiceManagerPersian",
    val exportDateShamsi: String,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val customers: List<Customer> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val settings: BusinessSettings? = null
)
