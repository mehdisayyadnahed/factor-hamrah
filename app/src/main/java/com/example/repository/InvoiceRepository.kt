package com.example.repository

import com.example.db.InvoiceDao
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val invoiceDao: InvoiceDao) {

    val allInvoices: Flow<List<InvoiceWithItems>> = invoiceDao.getAllInvoicesWithItems()
    val invoicesCount: Flow<Int> = invoiceDao.getInvoicesCount()
    val proformaCount: Flow<Int> = invoiceDao.getProformaCount()
    val totalSalesSum: Flow<Long?> = invoiceDao.getTotalSalesSum()

    fun getInvoicesByCustomerId(customerId: Long): Flow<List<InvoiceWithItems>> {
        return invoiceDao.getInvoicesByCustomerId(customerId)
    }

    fun getInvoiceWithItemsById(id: Long): Flow<InvoiceWithItems?> {
        return invoiceDao.getInvoiceWithItemsById(id)
    }

    suspend fun getInvoiceWithItemsByIdDirect(id: Long): InvoiceWithItems? {
        return invoiceDao.getInvoiceWithItemsByIdDirect(id)
    }

    fun searchInvoices(query: String, type: String?): Flow<List<InvoiceWithItems>> {
        return invoiceDao.searchInvoices(query.trim(), type)
    }

    suspend fun saveFullInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        return invoiceDao.saveFullInvoice(invoice, items)
    }

    suspend fun deleteInvoice(id: Long) {
        invoiceDao.deleteInvoiceById(id)
    }

    suspend fun generateNextInvoiceNumber(): String {
        val allNumbers = invoiceDao.getAllInvoiceNumbers()
        if (allNumbers.isEmpty()) {
            return "1001"
        }

        // 1. Find the highest integer invoice number present across all invoices
        var maxNumericValue: Long? = null
        for (numStr in allNumbers) {
            val cleaned = com.example.utils.PersianNumberHelper.toEnglishDigits(numStr.trim())
            // check pure integer
            val parsedLong = cleaned.toLongOrNull()
            if (parsedLong != null) {
                if (maxNumericValue == null || parsedLong > maxNumericValue) {
                    maxNumericValue = parsedLong
                }
            } else {
                // If it contains prefix/suffix, try extracting trailing digits
                val match = Regex("""(\d+)$""").find(cleaned)
                if (match != null) {
                    val extracted = match.value.toLongOrNull()
                    if (extracted != null && (maxNumericValue == null || extracted > maxNumericValue)) {
                        maxNumericValue = extracted
                    }
                }
            }
        }

        if (maxNumericValue != null) {
            return (maxNumericValue + 1).toString()
        }

        // 2. If no numeric pattern found, inspect the last saved invoice
        val lastInvoice = invoiceDao.getLastInvoice()
        if (lastInvoice != null) {
            val englishNum = com.example.utils.PersianNumberHelper.toEnglishDigits(lastInvoice.invoiceNumber.trim())
            val match = Regex("""(\d+)""").findAll(englishNum).lastOrNull()
            if (match != null) {
                val digits = match.value
                val incremented = (digits.toLongOrNull() ?: 1000L) + 1
                return englishNum.replaceRange(match.range, incremented.toString())
            }
        }

        val maxId = invoiceDao.getMaxInvoiceId() ?: 0L
        return (maxId + 1001L).toString()
    }
}
