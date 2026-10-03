package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoicesWithItems(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY id DESC")
    fun getInvoicesByCustomerId(customerId: Long): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceWithItemsById(id: Long): Flow<InvoiceWithItems?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceWithItemsByIdDirect(id: Long): InvoiceWithItems?

    @Transaction
    @Query("""
        SELECT * FROM invoices 
        WHERE (:type IS NULL OR invoiceType = :type)
        AND (invoiceNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%')
        ORDER BY id DESC
    """)
    fun searchInvoices(query: String, type: String?): Flow<List<InvoiceWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItemsByInvoiceId(invoiceId: Long)

    @Transaction
    suspend fun saveFullInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = if (invoice.id == 0L) {
            insertInvoice(invoice)
        } else {
            updateInvoice(invoice)
            deleteInvoiceItemsByInvoiceId(invoice.id)
            invoice.id
        }

        val updatedItems = items.mapIndexed { index, item ->
            item.copy(
                invoiceId = invoiceId,
                rowNumber = index + 1,
                totalPrice = (item.quantity * item.unitPrice).toLong()
            )
        }
        insertInvoiceItems(updatedItems)
        return invoiceId
    }

    @Query("SELECT MAX(id) FROM invoices")
    suspend fun getMaxInvoiceId(): Long?

    @Query("SELECT invoiceNumber FROM invoices")
    suspend fun getAllInvoiceNumbers(): List<String>

    @Query("SELECT * FROM invoices ORDER BY id DESC LIMIT 1")
    suspend fun getLastInvoice(): Invoice?

    @Query("SELECT * FROM invoices")
    suspend fun getAllInvoicesDirect(): List<Invoice>

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItemsDirect(): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInvoices(invoices: List<Invoice>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()

    @Query("DELETE FROM invoice_items")
    suspend fun deleteAllInvoiceItems()

    @Query("SELECT COUNT(*) FROM invoices")
    fun getInvoicesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM invoices WHERE invoiceType = 'PROFORMA'")
    fun getProformaCount(): Flow<Int>

    @Query("SELECT SUM(finalTotal) FROM invoices WHERE invoiceType = 'SALES'")
    fun getTotalSalesSum(): Flow<Long?>
}
