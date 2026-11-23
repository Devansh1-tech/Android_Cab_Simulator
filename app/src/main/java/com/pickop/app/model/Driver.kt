package com.pickop.app.model

/**
 * Simulated driver assigned to a booking.
 */
data class Driver(
    val name: String,
    val vehicleNumber: String,
    val rating: Float
)

/**
 * Pool of fake drivers for simulation.
 */
object DriverPool {
    private val drivers = listOf(
        Driver("Ramesh Kumar",   "MP07 AB 1234", 4.5f),
        Driver("Sunil Verma",    "MP07 CD 5678", 4.2f),
        Driver("Amit Singh",     "MP07 EF 9012", 4.8f),
        Driver("Deepak Sharma",  "MP07 GH 3456", 4.1f),
        Driver("Vijay Patel",    "MP07 IJ 7890", 4.6f),
        Driver("Raju Yadav",     "MP07 KL 2345", 3.9f)
    )

    fun random(): Driver = drivers.random()
}
