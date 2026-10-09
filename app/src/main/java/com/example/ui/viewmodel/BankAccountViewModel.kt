package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.BankAccount
import com.example.repository.BankAccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BankAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val bankAccountRepository = BankAccountRepository(db.bankAccountDao())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val bankAccountsList: StateFlow<List<BankAccount>> = _searchQuery
        .flatMapLatest { query ->
            bankAccountRepository.searchBankAccounts(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bankAccountsCount: StateFlow<Int> = bankAccountRepository.bankAccountsCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun saveBankAccount(account: BankAccount, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = bankAccountRepository.saveBankAccount(account)
            onComplete?.invoke(id)
        }
    }

    fun setDefaultBankAccount(id: Long) {
        viewModelScope.launch {
            bankAccountRepository.setDefaultBankAccount(id)
        }
    }

    fun deleteBankAccount(account: BankAccount) {
        viewModelScope.launch {
            bankAccountRepository.deleteBankAccount(account)
        }
    }

    fun deleteBankAccountById(id: Long) {
        viewModelScope.launch {
            bankAccountRepository.deleteBankAccountById(id)
        }
    }
}
