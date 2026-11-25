package com.pickop.app.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pickop.app.model.Booking

class BookingDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "pickop.db"
        private const val DB_VERSION = 2
        private const val TABLE = "bookings"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pickup TEXT NOT NULL,
                dropoff TEXT NOT NULL,
                cab_type TEXT NOT NULL,
                driver_name TEXT NOT NULL,
                vehicle_number TEXT NOT NULL,
                fare REAL NOT NULL,
                distance_km REAL NOT NULL,
                otp TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                pickup_lat REAL DEFAULT 0.0,
                pickup_lon REAL DEFAULT 0.0,
                drop_lat REAL DEFAULT 0.0,
                drop_lon REAL DEFAULT 0.0,
                duration_mins INTEGER DEFAULT 0,
                base_fare REAL DEFAULT 0.0,
                surge_multiplier REAL DEFAULT 1.0,
                payment_mode TEXT DEFAULT 'CASH',
                driver_rating INTEGER DEFAULT 0
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    // ── Insert ──

    fun insertBooking(booking: Booking): Long {
        val cv = ContentValues().apply {
            put("pickup", booking.pickup)
            put("dropoff", booking.dropoff)
            put("cab_type", booking.cabType)
            put("driver_name", booking.driverName)
            put("vehicle_number", booking.vehicleNumber)
            put("fare", booking.fare)
            put("distance_km", booking.distanceKm)
            put("otp", booking.otp)
            put("status", booking.status)
            put("created_at", booking.createdAt)
            put("pickup_lat", booking.pickupLat)
            put("pickup_lon", booking.pickupLon)
            put("drop_lat", booking.dropLat)
            put("drop_lon", booking.dropLon)
            put("duration_mins", booking.durationMins)
            put("base_fare", booking.baseFare)
            put("surge_multiplier", booking.surgeMultiplier)
            put("payment_mode", booking.paymentMode)
            put("driver_rating", booking.driverRating)
        }
        return writableDatabase.insert(TABLE, null, cv)
    }

    // ── Update status ──

    fun updateStatus(id: Long, status: String) {
        val cv = ContentValues().apply { put("status", status) }
        writableDatabase.update(TABLE, cv, "id = ?", arrayOf(id.toString()))
    }

    // ── Get one ──

    fun getBooking(id: Long): Booking? {
        val cursor = readableDatabase.query(
            TABLE, null, "id = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToBooking(it) else null
        }
    }

    // ── Get all (newest first) ──

    fun getAllBookings(): List<Booking> {
        val list = mutableListOf<Booking>()
        val cursor = readableDatabase.query(
            TABLE, null, null, null,
            null, null, "created_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToBooking(it))
            }
        }
        return list
    }

    // ── Get by status ──

    fun getBookingsByStatus(status: String): List<Booking> {
        val list = mutableListOf<Booking>()
        val cursor = readableDatabase.query(
            TABLE, null, "status = ?", arrayOf(status),
            null, null, "created_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToBooking(it))
            }
        }
        return list
    }

    // ── Delete ride ──

    fun deleteRide(id: Long) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    private fun cursorToBooking(c: Cursor): Booking {
        val pickupLatIdx = c.getColumnIndex("pickup_lat")
        val pickupLonIdx = c.getColumnIndex("pickup_lon")
        val dropLatIdx = c.getColumnIndex("drop_lat")
        val dropLonIdx = c.getColumnIndex("drop_lon")
        val durationIdx = c.getColumnIndex("duration_mins")
        val baseFareIdx = c.getColumnIndex("base_fare")
        val surgeIdx = c.getColumnIndex("surge_multiplier")
        val paymentIdx = c.getColumnIndex("payment_mode")
        val ratingIdx = c.getColumnIndex("driver_rating")

        return Booking(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            pickup = c.getString(c.getColumnIndexOrThrow("pickup")),
            dropoff = c.getString(c.getColumnIndexOrThrow("dropoff")),
            cabType = c.getString(c.getColumnIndexOrThrow("cab_type")),
            driverName = c.getString(c.getColumnIndexOrThrow("driver_name")),
            vehicleNumber = c.getString(c.getColumnIndexOrThrow("vehicle_number")),
            fare = c.getDouble(c.getColumnIndexOrThrow("fare")),
            distanceKm = c.getDouble(c.getColumnIndexOrThrow("distance_km")),
            otp = c.getString(c.getColumnIndexOrThrow("otp")),
            status = c.getString(c.getColumnIndexOrThrow("status")),
            createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
            pickupLat = if (pickupLatIdx != -1) c.getDouble(pickupLatIdx) else 0.0,
            pickupLon = if (pickupLonIdx != -1) c.getDouble(pickupLonIdx) else 0.0,
            dropLat = if (dropLatIdx != -1) c.getDouble(dropLatIdx) else 0.0,
            dropLon = if (dropLonIdx != -1) c.getDouble(dropLonIdx) else 0.0,
            durationMins = if (durationIdx != -1) c.getInt(durationIdx) else 0,
            baseFare = if (baseFareIdx != -1) c.getDouble(baseFareIdx) else 0.0,
            surgeMultiplier = if (surgeIdx != -1) c.getDouble(surgeIdx) else 1.0,
            paymentMode = if (paymentIdx != -1) c.getString(paymentIdx) else "CASH",
            driverRating = if (ratingIdx != -1) c.getInt(ratingIdx) else 0
        )
    }
}
