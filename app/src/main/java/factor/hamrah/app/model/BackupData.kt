package factor.hamrah.app.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val appName: String = "Factor Hamrah",
    val exportDateShamsi: String,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val customers: List<Customer> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val settings: BusinessSettings? = null
)
