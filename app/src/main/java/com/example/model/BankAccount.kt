package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "bank_accounts")
@JsonClass(generateAdapter = true)
data class BankAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String = "",
    val branchName: String = "",
    val accountNumber: String = "",
    val iban: String = "", // شماره شبا
    val cardNumber: String = "", // شماره کارت ۱۶ رقمی
    val accountHolder: String = "", // نام صاحب حساب
    val depositNotes: String = "", // توضیحات واریز
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
