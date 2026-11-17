package com.example.driver.domain.repository

import com.example.driver.domain.model.User
import com.example.driver.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    fun observeAuthState(): Flow<User?>
    suspend fun login(email: String, pass: String): Result<User>
    suspend fun register(
        email: String,
        pass: String,
        name: String,
        phone: String,
        role: UserRole,
        vehicleModel: String = "",
        vehiclePlate: String = "",
        vehicleColor: String = ""
    ): Result<User>
    suspend fun getUserProfile(uid: String): Result<User>
    suspend fun logout()
}
