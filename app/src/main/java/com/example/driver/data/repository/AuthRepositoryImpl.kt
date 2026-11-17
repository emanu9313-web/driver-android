package com.example.driver.data.repository

import com.example.driver.domain.model.DriverProfile
import com.example.driver.domain.model.User
import com.example.driver.domain.model.UserRole
import com.example.driver.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                firestore.collection("users").document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val user = snapshot.toObject(UserDto::class.java)?.toDomain()
                            trySend(user)
                        } else {
                            trySend(null)
                        }
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, pass: String): Result<User> = runCatching {
        val authResult = auth.signInWithEmailAndPassword(email, pass).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("UID nulo tras autenticación")
        getUserProfile(uid).getOrThrow()
    }

    override suspend fun register(
        email: String,
        pass: String,
        name: String,
        phone: String,
        role: UserRole,
        vehicleModel: String,
        vehiclePlate: String,
        vehicleColor: String
    ): Result<User> = runCatching {
        require(role != UserRole.ADMIN) {
            "El rol de administrador debe asignarse desde Firebase Console."
        }

        val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("No se pudo crear usuario")

        val user = User(
            uid = uid,
            name = name,
            email = email,
            phone = phone,
            role = role,
            createdAtMillis = System.currentTimeMillis()
        )

        firestore.collection("users").document(uid)
            .set(UserDto.fromDomain(user))
            .await()

        if (role == UserRole.CONDUCTOR) {
            val driverProfile = DriverProfile(
                uid = uid,
                name = name,
                phone = phone,
                vehicleModel = vehicleModel,
                vehiclePlate = vehiclePlate,
                vehicleColor = vehicleColor,
                isOnline = false,
                isApproved = false,
                isSuspended = false
            )
            firestore.collection("drivers").document(uid)
                .set(DriverDto.fromDomain(driverProfile))
                .await()
        }

        user
    }

    override suspend fun getUserProfile(uid: String): Result<User> = runCatching {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val userDto = snapshot.toObject(UserDto::class.java)
            ?: throw IllegalStateException("Perfil de usuario no encontrado")
        userDto.toDomain()
    }

    override suspend fun logout() {
        auth.signOut()
    }
}

// DTOs para mapeo con Firestore
data class UserDto(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = UserRole.PASAJERO.name,
    val photoUrl: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain() = User(
        uid = uid,
        name = name,
        email = email,
        phone = phone,
        role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.PASAJERO },
        photoUrl = photoUrl,
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(user: User) = UserDto(
            uid = user.uid,
            name = user.name,
            email = user.email,
            phone = user.phone,
            role = user.role.name,
            photoUrl = user.photoUrl,
            createdAtMillis = user.createdAtMillis
        )
    }
}
