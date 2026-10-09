package com.example.repository

import com.example.db.BankAccountDao
import com.example.model.BankAccount
import kotlinx.coroutines.flow.Flow

class BankAccountRepository(private val bankAccountDao: BankAccountDao) {

    val allBankAccounts: Flow<List<BankAccount>> = bankAccountDao.getAllBankAccounts()
    val bankAccountsCount: Flow<Int> = bankAccountDao.getBankAccountsCount()

    fun searchBankAccounts(query: String): Flow<List<BankAccount>> {
        return if (query.isBlank()) {
            bankAccountDao.getAllBankAccounts()
        } else {
            bankAccountDao.searchBankAccounts(query.trim())
        }
    }

    fun getBankAccountById(id: Long): Flow<BankAccount?> = bankAccountDao.getBankAccountById(id)

    suspend fun getBankAccountByIdDirect(id: Long): BankAccount? = bankAccountDao.getBankAccountByIdDirect(id)

    suspend fun getAllBankAccountsDirect(): List<BankAccount> = bankAccountDao.getAllBankAccountsDirect()

    suspend fun getDefaultBankAccountDirect(): BankAccount? = bankAccountDao.getDefaultBankAccountDirect()

    suspend fun saveBankAccount(account: BankAccount): Long {
        if (account.isDefault) {
            bankAccountDao.resetAllDefaults()
        }
        return if (account.id == 0L) {
            // If this is the first account, make it default automatically
            val total = bankAccountDao.getAllBankAccountsDirect().size
            val accountToSave = if (total == 0) account.copy(isDefault = true) else account
            bankAccountDao.insertBankAccount(accountToSave)
        } else {
            bankAccountDao.updateBankAccount(account)
            account.id
        }
    }

    suspend fun setDefaultBankAccount(id: Long) {
        bankAccountDao.resetAllDefaults()
        bankAccountDao.setDefaultBankAccount(id)
    }

    suspend fun deleteBankAccount(account: BankAccount) {
        bankAccountDao.deleteBankAccount(account)
    }

    suspend fun deleteBankAccountById(id: Long) {
        bankAccountDao.deleteBankAccountById(id)
    }
}
