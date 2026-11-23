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
    val createdAt: Long = System.currentTimeMillis()
)
