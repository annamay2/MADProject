// data/model/UserProfile.kt
package com.example.madprojectactivity.data.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)
