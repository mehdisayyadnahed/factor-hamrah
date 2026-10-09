package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class InvoiceType(val titlePersian: String, val badgeColorHex: Long) {
    SALES("فاکتور فروش", 0xFF0D9488),
    PROFORMA("پیش‌فاکتور", 0xFFD97706);

    companion object {
        fun fromString(type: String?): InvoiceType {
            return try {
                if (type != null) valueOf(type) else SALES
            } catch (e: Exception) {
                SALES
            }
        }
    }
}

@Entity(tableName = "invoices")
@JsonClass(generateAdapter = true)
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val invoiceType: String = InvoiceType.PROFORMA.name, // PROFORMA or SALES

    // Customer / Buyer Details
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",

    // Date & Meta
    val issueDateShamsi: String,
    val issueTimestamp: Long = System.currentTimeMillis(),

    // Seller Details
    val sellerName: String = "",
    val sellerPhone: String = "",
    val sellerAddress: String = "",
    val sellerEconomicCode: String = "",

    // Calculations
    val subtotal: Long = 0L,
    val vatPercent: Double = 0.0,
    val vatAmount: Long = 0L,
    val discountPercent: Double = 0.0,
    val discountAmount: Long = 0L,
    val finalTotal: Long = 0L,
    val currency: String = "تومان",

    // Bank Settlement Details
    val bankAccountId: Long? = null,
    val bankName: String = "",
    val bankBranch: String = "",
    val bankAccountNumber: String = "",
    val bankIban: String = "",
    val bankCardNumber: String = "",
    val bankAccountHolder: String = "",
    val bankDepositNotes: String = "",

    // Notes
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
