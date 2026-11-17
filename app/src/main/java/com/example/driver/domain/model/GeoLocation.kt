package com.example.driver.domain.model

data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressName: String = "",
    val geohash: String = ""
)
