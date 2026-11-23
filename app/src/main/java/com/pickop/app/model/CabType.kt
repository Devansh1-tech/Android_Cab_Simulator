package com.pickop.app.model

/**
 * Available cab types with simple ₹ pricing.
 */
data class CabType(
    val id: Int,
    val name: String,
    val icon: String,       // emoji
    val baseFare: Double,   // ₹ flat
    val perKm: Double,      // ₹ per km
    val etaMinutes: Int     // simulated ETA nearby
)

/**
 * Pre-defined cab options.
 */
object CabTypes {
    val all = listOf(
        CabType(1, "Bike",   "🏍️", 15.0,  5.0, 3),
        CabType(2, "Auto",   "🛺", 25.0, 10.0, 5),
        CabType(3, "Mini",   "🚗", 40.0, 13.0, 7),
        CabType(4, "Sedan",  "🚘", 55.0, 16.0, 10)
    )

    fun fareFor(cab: CabType, distanceKm: Double): Double {
        return cab.baseFare + (cab.perKm * distanceKm)
    }
}
