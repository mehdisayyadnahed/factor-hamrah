package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.BusinessSettings
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceType
import com.example.model.InvoiceWithItems
import com.example.repository.InvoiceRepository
import com.example.repository.SettingsRepository
import com.example.utils.PdfInvoiceGenerator
import com.example.utils.PersianDateHelper
import com.example.utils.PersianNumberHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val invoiceRepository = InvoiceRepository(db.invoiceDao())
    private val settingsRepository = SettingsRepository(db.businessSettingsDao())
    private val bankAccountRepository = com.example.repository.BankAccountRepository(db.bankAccountDao())

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<String?>(null) // null = ALL, "SALES", "PROFORMA"
    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()

    val invoicesList: StateFlow<List<InvoiceWithItems>> = combine(
        _searchQuery,
        _selectedTypeFilter
    ) { query, type ->
        Pair(query, type)
    }.flatMapLatest { (query, type) ->
        invoiceRepository.searchInvoices(query, type)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentInvoices: StateFlow<List<InvoiceWithItems>> = invoiceRepository.allInvoices
        .map { it.take(5) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val invoicesCount: StateFlow<Int> = invoiceRepository.invoicesCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val proformaCount: StateFlow<Int> = invoiceRepository.proformaCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalSalesSum: StateFlow<Long?> = invoiceRepository.totalSalesSum
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // Current Invoice Editing State
    private val _currentInvoice = MutableStateFlow<Invoice>(createDefaultInvoice())
    val currentInvoice: StateFlow<Invoice> = _currentInvoice.asStateFlow()

    private val _currentItems = MutableStateFlow<List<InvoiceItem>>(emptyList())
    val currentItems: StateFlow<List<InvoiceItem>> = _currentItems.asStateFlow()

    private val _lastGeneratedPdf = MutableStateFlow<File?>(null)
    val lastGeneratedPdf: StateFlow<File?> = _lastGeneratedPdf.asStateFlow()

    private fun createDefaultInvoice(): Invoice {
        return Invoice(
            invoiceNumber = "",
            invoiceType = InvoiceType.PROFORMA.name,
            customerName = "",
            issueDateShamsi = PersianDateHelper.getTodayJalali().formatFormatted(),
            currency = "تومان"
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTypeFilterChanged(type: String?) {
        _selectedTypeFilter.value = type
    }

    fun initNewInvoice(defaultSettings: BusinessSettings) {
        viewModelScope.launch {
            val nextNumber = invoiceRepository.generateNextInvoiceNumber()
            val defaultAccount = bankAccountRepository.getDefaultBankAccountDirect()
            _currentInvoice.value = Invoice(
                invoiceNumber = nextNumber,
                invoiceType = InvoiceType.PROFORMA.name,
                customerName = "",
                customerPhone = "",
                customerAddress = "",
                sellerName = defaultSettings.businessName,
                sellerPhone = defaultSettings.businessPhone,
                sellerAddress = defaultSettings.businessAddress,
                sellerEconomicCode = defaultSettings.economicCode,
                bankAccountId = defaultAccount?.id,
                bankName = defaultAccount?.bankName ?: "",
                bankBranch = defaultAccount?.branchName ?: "",
                bankAccountNumber = defaultAccount?.accountNumber ?: "",
                bankIban = defaultAccount?.iban ?: "",
                bankCardNumber = defaultAccount?.cardNumber ?: "",
                bankAccountHolder = defaultAccount?.accountHolder ?: "",
                bankDepositNotes = defaultAccount?.depositNotes ?: "",
                issueDateShamsi = PersianDateHelper.getTodayJalali().formatFormatted(),
                currency = defaultSettings.currency,
                notes = defaultSettings.defaultFooterNote
            )
            _currentItems.value = emptyList()
            _lastGeneratedPdf.value = null
        }
    }

    fun loadInvoiceForEdit(invoiceWithItems: InvoiceWithItems) {
        _currentInvoice.value = invoiceWithItems.invoice
        _currentItems.value = invoiceWithItems.items
        _lastGeneratedPdf.value = null
    }

    fun updateInvoiceState(transform: (Invoice) -> Invoice) {
        val updated = transform(_currentInvoice.value)
        recalculateAndSet(updated, _currentItems.value)
    }

    fun addItem(item: InvoiceItem) {
        val list = _currentItems.value.toMutableList()
        list.add(item)
        recalculateAndSet(_currentInvoice.value, list)
    }

    fun updateItem(index: Int, item: InvoiceItem) {
        val list = _currentItems.value.toMutableList()
        if (index in list.indices) {
            list[index] = item
            recalculateAndSet(_currentInvoice.value, list)
        }
    }

    fun removeItem(index: Int) {
        val list = _currentItems.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            recalculateAndSet(_currentInvoice.value, list)
        }
    }

    fun updateVat(percent: Double, manualAmount: Long? = null) {
        val inv = _currentInvoice.value
        val subtotal = calculateSubtotal(_currentItems.value)
        val vatAmount = manualAmount ?: ((subtotal * percent) / 100.0).toLong()
        val finalTot = (subtotal + vatAmount - inv.discountAmount).coerceAtLeast(0L)

        _currentInvoice.value = inv.copy(
            vatPercent = percent,
            vatAmount = vatAmount,
            finalTotal = finalTot
        )
    }

    fun updateDiscount(percent: Double, manualAmount: Long? = null) {
        val inv = _currentInvoice.value
        val subtotal = calculateSubtotal(_currentItems.value)
        val discountAmount = manualAmount ?: ((subtotal * percent) / 100.0).toLong()
        val finalTot = (subtotal + inv.vatAmount - discountAmount).coerceAtLeast(0L)

        _currentInvoice.value = inv.copy(
            discountPercent = percent,
            discountAmount = discountAmount,
            finalTotal = finalTot
        )
    }

    private fun calculateSubtotal(items: List<InvoiceItem>): Long {
        return items.sumOf { (it.quantity * it.unitPrice).toLong() }
    }

    private fun recalculateAndSet(invoice: Invoice, items: List<InvoiceItem>) {
        val subtotal = calculateSubtotal(items)
        val vatAmount = if (invoice.vatPercent > 0) ((subtotal * invoice.vatPercent) / 100.0).toLong() else invoice.vatAmount
        val discountAmount = if (invoice.discountPercent > 0) ((subtotal * invoice.discountPercent) / 100.0).toLong() else invoice.discountAmount
        val finalTot = (subtotal + vatAmount - discountAmount).coerceAtLeast(0L)

        _currentItems.value = items
        _currentInvoice.value = invoice.copy(
            subtotal = subtotal,
            vatAmount = vatAmount,
            discountAmount = discountAmount,
            finalTotal = finalTot,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun saveCurrentInvoice(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val invoice = _currentInvoice.value
            val items = _currentItems.value
            val savedId = invoiceRepository.saveFullInvoice(invoice, items)
            onSaved(savedId)
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(id)
        }
    }

    fun duplicateInvoice(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val nextNum = invoiceRepository.generateNextInvoiceNumber()
            val newInvoice = invoiceWithItems.invoice.copy(
                id = 0,
                invoiceNumber = nextNum,
                issueDateShamsi = PersianDateHelper.getTodayJalali().formatFormatted(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newItems = invoiceWithItems.items.map { it.copy(id = 0, invoiceId = 0) }
            invoiceRepository.saveFullInvoice(newInvoice, newItems)
        }
    }

    fun generatePdfForCurrentInvoice(
        context: Context,
        settings: BusinessSettings,
        onGenerated: (File) -> Unit = {}
    ) {
        val fullData = InvoiceWithItems(
            invoice = _currentInvoice.value,
            items = _currentItems.value
        )
        val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, fullData, settings)
        _lastGeneratedPdf.value = pdfFile
        onGenerated(pdfFile)
    }

    fun generateAndSharePdf(
        context: Context,
        invoiceWithItems: InvoiceWithItems,
        settings: BusinessSettings
    ) {
        val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, invoiceWithItems, settings)
        val title = "${if (invoiceWithItems.invoice.invoiceType == InvoiceType.PROFORMA.name) "پیش‌فاکتور" else "فاکتور"} شماره ${invoiceWithItems.invoice.invoiceNumber}"
        PdfInvoiceGenerator.shareInvoicePdf(context, pdfFile, title)
    }

    fun generateAndViewPdf(
        context: Context,
        invoiceWithItems: InvoiceWithItems,
        settings: BusinessSettings
    ) {
        val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, invoiceWithItems, settings)
        PdfInvoiceGenerator.viewInvoicePdf(context, pdfFile)
    }
}
