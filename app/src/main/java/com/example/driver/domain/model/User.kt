package com.example.driver.domain.model

enum class UserRole {
    PASAJERO,
    CONDUCTOR,
    ADMIN
}

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.PASAJERO,
    val photoUrl: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
