package com.pickop.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pickop.app.api.ApiClient
import com.pickop.app.databinding.ActivityBookingBinding
import com.pickop.app.db.BookingDbHelper
import com.pickop.app.model.Booking
import com.pickop.app.model.DriverPool
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingActivity : AppCompatActivity() {

    private lateinit var b: ActivityBookingBinding
    private lateinit var db: BookingDbHelper

    private var bookingId: Long = -1
    private var timer: CountDownTimer? = null
    private var slotExpired = false
    
    private var carMarker: Marker? = null
    private var approachTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = BookingDbHelper(this)

        val pickup = intent.getStringExtra("pickup") ?: ""
        val dropoff = intent.getStringExtra("dropoff") ?: ""
        val cabName = intent.getStringExtra("cab_name") ?: "Mini"
        val fare = intent.getDoubleExtra("fare", 0.0)
        val distanceKm = intent.getDoubleExtra("distance_km", 5.0)

        // Assign a random driver
        val driver = DriverPool.random()

        // Generate 4-digit OTP
        val otp = "%04d".format((1000..9999).random())

        // Save booking to SQLite
        val booking = Booking(
            pickup = pickup,
            dropoff = dropoff,
            cabType = cabName,
            driverName = driver.name,
            vehicleNumber = driver.vehicleNumber,
            fare = fare,
            distanceKm = distanceKm,
            otp = otp,
            status = "BOOKED"
        )
        bookingId = db.insertBooking(booking)

        // ── Populate UI ──
        MapHelper.setupMap(b.mapView)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val routeResult = ApiClient.fetchRouteData(pickup, dropoff, distanceKm)
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    MapHelper.drawRoute(
                        b.mapView, routeResult.routePoints,
                        routeResult.pickupLat, routeResult.pickupLon,
                        routeResult.dropoffLat, routeResult.dropoffLon
                    )
                    
                    if (routeResult.pickupLat != null && routeResult.pickupLon != null) {
                        simulateDriverArriving(routeResult.pickupLat!!, routeResult.pickupLon!!)
                    }
                }
            }
        }

        b.tvOtp.text = otp
        b.tvDriverName.text = driver.name
        b.tvVehicleInfo.text = "$cabName  •  ${driver.vehicleNumber}"
        b.tvDriverRating.text = "⭐ %.1f rating".format(driver.rating)
        b.tvFareBreakdown.text = "Route: $pickup → $dropoff\nDistance: %.1f km".format(distanceKm)
        b.tvTotalFare.text = "₹%.0f".format(fare)

        // ── 3-minute slot hold countdown ──
        startSlotTimer()

        // ── Start Ride ──
        b.btnStartRide.setOnClickListener {
            if (slotExpired) {
                Toast.makeText(this, "Slot expired! Book again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            approachTimer?.cancel()
            timer?.cancel()
            db.updateStatus(bookingId, "IN_PROGRESS")

            val intent = Intent(this, RideActivity::class.java).apply {
                putExtra("booking_id", bookingId)
            }
            startActivity(intent)
            finish()
        }

        // ── Cancel ──
        b.btnCancel.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cancel Booking?")
                .setMessage("Are you sure you want to cancel this ride?")
                .setPositiveButton("Yes, Cancel") { _, _ ->
                    approachTimer?.cancel()
                    timer?.cancel()
                    db.updateStatus(bookingId, "CANCELLED")
                    Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun simulateDriverArriving(pickupLat: Double, pickupLon: Double) {
        // Driver starts roughly 1.5km away
        val startLat = pickupLat - 0.015
        val startLon = pickupLon - 0.015
        
        carMarker = MapHelper.createCabMarker(b.mapView, startLat, startLon)
        
        val totalTime = 180000L // 3 minutes simulated driving
        val interval = 1000L
        val steps = (totalTime / interval).toInt()
        
        val latStep = (pickupLat - startLat) / steps
        val lonStep = (pickupLon - startLon) / steps
        
        approachTimer = object : CountDownTimer(totalTime, interval) {
            var currentStep = 0
            override fun onTick(ms: Long) {
                currentStep++
                val curLat = startLat + latStep * currentStep
                val curLon = startLon + lonStep * currentStep
                val nextLat = startLat + latStep * (currentStep + 1)
                val nextLon = startLon + lonStep * (currentStep + 1)
                
                val newPos = GeoPoint(curLat, curLon)
                val nextPos = GeoPoint(nextLat, nextLon)
                
                carMarker?.position = newPos
                
                // Calculate direction and rotate
                val bearing = newPos.bearingTo(nextPos).toFloat()
                carMarker?.rotation = bearing
                b.mapView.mapOrientation = 360f - bearing
                
                b.mapView.controller.animateTo(newPos)
                b.mapView.controller.setZoom(17.5)
                b.mapView.invalidate()
            }
            override fun onFinish() {}
        }.start()
    }

    private fun startSlotTimer() {
        val totalMs = 3 * 60 * 1000L    // 3 minutes

        timer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt()
                val m = sec / 60
                val s = sec % 60
                b.tvTimer.text = "%d:%02d".format(m, s)
                b.progressTimer.progress = sec
            }

            override fun onFinish() {
                slotExpired = true
                b.tvTimer.text = "0:00"
                b.tvTimer.setTextColor(getColor(R.color.surge_red))
                b.progressTimer.progress = 0
                b.btnStartRide.isEnabled = false
                b.btnStartRide.alpha = 0.5f
                db.updateStatus(bookingId, "EXPIRED")
                Toast.makeText(this@BookingActivity,
                    "Slot expired! Your 3-minute hold is over.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    override fun onDestroy() {
        approachTimer?.cancel()
        timer?.cancel()
        super.onDestroy()
    }
}
