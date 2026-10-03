package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.Customer
import com.example.model.InvoiceType
import com.example.model.InvoiceWithItems
import com.example.repository.CustomerRepository
import com.example.repository.InvoiceRepository
import com.example.utils.BackupManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CustomerSortOrder(val title: String) {
    DEFAULT("پیش‌فرض (جدیدترین)"),
    SALES_TOTAL_DESC("بیشترین مجموع مبلغ فاکتورهای فروش صادر شده"),
    SALES_TOTAL_ASC("کمترین مجموع مبلغ فاکتورهای فروش صادر شده"),
    SALES_COUNT_DESC("بیشترین تعداد فاکتورهای فروش صادر شده"),
    SALES_COUNT_ASC("کمترین تعداد فاکتورهای فروش صادر شده"),
    PROFORMA_TOTAL_DESC("بیشترین مجموع مبلغ پیش‌فاکتورهای فروش صادر شده"),
    PROFORMA_TOTAL_ASC("کمترین مجموع مبلغ پیش‌فاکتورهای فروش صادر شده"),
    PROFORMA_COUNT_DESC("بیشترین تعداد پیش‌فاکتورهای فروش صادر شده"),
    PROFORMA_COUNT_ASC("کمترین تعداد پیش‌فاکتورهای فروش صادر شده")
}

data class CustomerStats(
    val salesTotal: Long = 0L,
    val salesCount: Int = 0,
    val proformaTotal: Long = 0L,
    val proformaCount: Int = 0
)

data class CustomerWithStats(
    val customer: Customer,
    val stats: CustomerStats
)

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val customerRepository = CustomerRepository(db.customerDao())
    private val invoiceRepository = InvoiceRepository(db.invoiceDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(CustomerSortOrder.DEFAULT)
    val sortOrder: StateFlow<CustomerSortOrder> = _sortOrder.asStateFlow()

    val customersWithStatsList: StateFlow<List<CustomerWithStats>> = combine(
        _searchQuery.flatMapLatest { query -> customerRepository.searchCustomers(query) },
        invoiceRepository.allInvoices,
        _sortOrder
    ) { customers, invoices, sortOrder ->
        val invoicesByCustomerId = invoices.filter { it.invoice.customerId != null }.groupBy { it.invoice.customerId }

        val list = customers.map { customer ->
            val directInvoices = invoicesByCustomerId[customer.id] ?: emptyList()
            val fallbackInvoices = if (directInvoices.isEmpty()) {
                invoices.filter { inv ->
                    inv.invoice.customerId == null && (
                        (inv.invoice.customerName.isNotBlank() && inv.invoice.customerName.trim().equals(customer.name.trim(), ignoreCase = true)) ||
                        (customer.phone.isNotBlank() && inv.invoice.customerPhone.trim() == customer.phone.trim())
                    )
                }
            } else emptyList()

            val allInvoices = directInvoices + fallbackInvoices
            val sales = allInvoices.filter { it.invoice.invoiceType == InvoiceType.SALES.name }
            val proforma = allInvoices.filter { it.invoice.invoiceType == InvoiceType.PROFORMA.name }

            val stats = CustomerStats(
                salesTotal = sales.sumOf { it.invoice.finalTotal },
                salesCount = sales.size,
                proformaTotal = proforma.sumOf { it.invoice.finalTotal },
                proformaCount = proforma.size
            )
            CustomerWithStats(customer = customer, stats = stats)
        }

        when (sortOrder) {
            CustomerSortOrder.DEFAULT -> list
            CustomerSortOrder.SALES_TOTAL_DESC -> list.sortedWith(compareByDescending<CustomerWithStats> { it.stats.salesTotal }.thenByDescending { it.customer.id })
            CustomerSortOrder.SALES_TOTAL_ASC -> list.sortedWith(compareBy<CustomerWithStats> { it.stats.salesTotal }.thenByDescending { it.customer.id })
            CustomerSortOrder.SALES_COUNT_DESC -> list.sortedWith(compareByDescending<CustomerWithStats> { it.stats.salesCount }.thenByDescending { it.customer.id })
            CustomerSortOrder.SALES_COUNT_ASC -> list.sortedWith(compareBy<CustomerWithStats> { it.stats.salesCount }.thenByDescending { it.customer.id })
            CustomerSortOrder.PROFORMA_TOTAL_DESC -> list.sortedWith(compareByDescending<CustomerWithStats> { it.stats.proformaTotal }.thenByDescending { it.customer.id })
            CustomerSortOrder.PROFORMA_TOTAL_ASC -> list.sortedWith(compareBy<CustomerWithStats> { it.stats.proformaTotal }.thenByDescending { it.customer.id })
            CustomerSortOrder.PROFORMA_COUNT_DESC -> list.sortedWith(compareByDescending<CustomerWithStats> { it.stats.proformaCount }.thenByDescending { it.customer.id })
            CustomerSortOrder.PROFORMA_COUNT_ASC -> list.sortedWith(compareBy<CustomerWithStats> { it.stats.proformaCount }.thenByDescending { it.customer.id })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customersList: StateFlow<List<Customer>> = customersWithStatsList
        .map { list -> list.map { it.customer } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customersCount: StateFlow<Int> = customerRepository.customersCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Selected customer for details screen
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    val selectedCustomerInvoices: StateFlow<List<InvoiceWithItems>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) invoiceRepository.getInvoicesByCustomerId(id)
            else MutableStateFlow(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: CustomerSortOrder) {
        _sortOrder.value = order
    }

    fun selectCustomer(id: Long?) {
        _selectedCustomerId.value = id
    }

    fun saveCustomer(customer: Customer, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = customerRepository.saveCustomer(customer)
            onComplete(id)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            customerRepository.deleteCustomer(customer)
        }
    }

    fun deleteCustomerById(id: Long) {
        viewModelScope.launch {
            customerRepository.deleteCustomerById(id)
        }
    }

    fun exportAndShareCustomerPhoneList(context: Context) {
        viewModelScope.launch {
            val content = customerRepository.exportCustomerPhoneNumbersTxt()
            BackupManager.shareTextContent(context, content, "لیست شماره تماس مشتریان")
        }
    }
}
