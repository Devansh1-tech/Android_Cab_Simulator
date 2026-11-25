package com.pickop.app.model

/**
 * Represents a single booking / trip stored in SQLite.
 */
data class Booking(
    val id: Long = 0,
    val pickup: String,
    val dropoff: String,
    val cabType: String,
    val driverName: String,
    val vehicleNumber: String,
    val fare: Double,
    val distanceKm: Double,
    val otp: String,
    val status: String,         // BOOKED, IN_PROGRESS, COMPLETED, CANCELLED, EXPIRED
    val createdAt: Long = System.currentTimeMillis(),
    val pickupLat: Double = 0.0,
    val pickupLon: Double = 0.0,
    val dropLat: Double = 0.0,
    val dropLon: Double = 0.0,
    val durationMins: Int = 0,
    val baseFare: Double = 0.0,
    val surgeMultiplier: Double = 1.0,
    val paymentMode: String = "CASH",
    val driverRating: Int = 0
)
