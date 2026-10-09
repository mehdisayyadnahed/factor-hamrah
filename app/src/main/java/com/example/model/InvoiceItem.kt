package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
@JsonClass(generateAdapter = true)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long = 0,
    val rowNumber: Int = 1,
    val itemCode: String = "",
    val itemName: String,
    val quantity: Double = 1.0,
    val unitPrice: Long = 0L,
    val totalPrice: Long = 0L // quantity * unitPrice
)
