package com.example.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.BankAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {

    @Query("SELECT * FROM bank_accounts ORDER BY isDefault DESC, id DESC")
    fun getAllBankAccounts(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE bankName LIKE '%' || :query || '%' OR accountHolder LIKE '%' || :query || '%' OR cardNumber LIKE '%' || :query || '%' OR accountNumber LIKE '%' || :query || '%' ORDER BY isDefault DESC, id DESC")
    fun searchBankAccounts(query: String): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    fun getBankAccountById(id: Long): Flow<BankAccount?>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getBankAccountByIdDirect(id: Long): BankAccount?

    @Query("SELECT * FROM bank_accounts ORDER BY isDefault DESC, id DESC")
    suspend fun getAllBankAccountsDirect(): List<BankAccount>

    @Query("SELECT * FROM bank_accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBankAccountDirect(): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<BankAccount>)

    @Update
    suspend fun updateBankAccount(account: BankAccount)

    @Delete
    suspend fun deleteBankAccount(account: BankAccount)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteBankAccountById(id: Long)

    @Query("UPDATE bank_accounts SET isDefault = 0")
    suspend fun resetAllDefaults()

    @Query("UPDATE bank_accounts SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultBankAccount(id: Long)

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bank_accounts")
    fun getBankAccountsCount(): Flow<Int>
}
