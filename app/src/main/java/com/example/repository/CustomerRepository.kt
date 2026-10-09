package com.example.repository

import com.example.db.CustomerDao
import com.example.model.Customer
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val customerDao: CustomerDao) {

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val customersCount: Flow<Int> = customerDao.getCustomersCount()

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return if (query.isBlank()) {
            customerDao.getAllCustomers()
        } else {
            customerDao.searchCustomers(query.trim())
        }
    }

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdDirect(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    suspend fun getAllCustomersDirect(): List<Customer> = customerDao.getAllCustomersDirect()

    suspend fun saveCustomer(customer: Customer): Long {
        return if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun deleteCustomerById(id: Long) {
        customerDao.deleteCustomerById(id)
    }

    suspend fun exportCustomerPhoneNumbersTxt(): String {
        val list = customerDao.getAllCustomersDirect()
        val sb = StringBuilder()
        sb.append("==== لیست شماره تلفن مشتریان ====\n")
        sb.append("تعداد کل: ${list.size}\n\n")
        list.forEachIndexed { index, customer ->
            sb.append("${index + 1}. ${customer.name} : ${customer.phone}\n")
            if (customer.notes.isNotBlank()) {
                sb.append("   (توضیحات: ${customer.notes})\n")
            }
        }
        return sb.toString()
    }
}
