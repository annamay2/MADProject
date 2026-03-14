package com.example.madprojectactivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val uid: String = "",
    val name: String = "",
    val email: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)
