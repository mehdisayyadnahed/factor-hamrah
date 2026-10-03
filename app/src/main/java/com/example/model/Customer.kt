package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "customers")
@JsonClass(generateAdapter = true)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val notes: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
