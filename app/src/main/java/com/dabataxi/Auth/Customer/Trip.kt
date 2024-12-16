package com.dabataxi.Auth.Customer

data class Trip(
    val tripId: String = "",
    val userId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val startLocation: LocationData? = null,
    val endLocation: LocationData? = null,
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val origin: String = "",
    val destination: String = "",
    val fare: Double = 0.0,
    val route: List<LocationData> = emptyList()
)
