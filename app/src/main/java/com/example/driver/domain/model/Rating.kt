package com.example.driver.domain.model

data class Rating(
    val id: String = "",
    val rideId: String = "",
    val passengerId: String = "",
    val driverId: String = "",
    val score: Int = 5,
    val comment: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
