package com.pickop.app

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pickop.app.api.ApiClient
import com.pickop.app.databinding.ActivityRideBinding
import com.pickop.app.db.BookingDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class RideActivity : AppCompatActivity() {

    private lateinit var b: ActivityRideBinding
    private lateinit var db: BookingDbHelper
    private var bookingId: Long = -1
    private var rideTimer: CountDownTimer? = null
    private var elapsedSeconds = 0
    
    private var carMarker: Marker? = null
    private var routePoints: List<Pair<Double, Double>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRideBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = BookingDbHelper(this)
        bookingId = intent.getLongExtra("booking_id", -1)

        val booking = db.getBooking(bookingId)
        if (booking == null) {
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        b.tvRidePickup.text = "📍 From: ${booking.pickup}"
        b.tvRideDropoff.text = "📍 To: ${booking.dropoff}"
        b.tvRideDriver.text = "${booking.driverName}  •  ${booking.vehicleNumber}"
        b.tvRideFare.text = "₹%.0f".format(booking.fare)
        b.tvRideStatus.text = "Your ${booking.cabType} ride is in progress"
        
        MapHelper.setupMap(b.mapView)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val routeResult = ApiClient.fetchRouteData(booking.pickup, booking.dropoff, booking.distanceKm)
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    routePoints = routeResult.routePoints
                    MapHelper.drawRoute(
                        b.mapView, routePoints,
                        routeResult.pickupLat, routeResult.pickupLon,
                        routeResult.dropoffLat, routeResult.dropoffLon
                    )
                    if (routePoints.isNotEmpty()) {
                        carMarker = MapHelper.createCabMarker(
                            b.mapView,
                            routePoints.first().first,
                            routePoints.first().second
                        )
                    }
                }
            }
        }

        // Simple elapsed timer to show ride is "happening"
        rideTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(ms: Long) {
                elapsedSeconds++
                val m = elapsedSeconds / 60
                val s = elapsedSeconds % 60
                b.tvElapsed.text = "Ride time: %d:%02d".format(m, s)

                // Animate cab marker along the route
                if (routePoints.isNotEmpty() && carMarker != null) {
                    val simulatedDuration = 180f // 3 minutes total for demo
                    val progress = (elapsedSeconds / simulatedDuration).coerceIn(0f, 1f)
                    val targetIndex = (progress * (routePoints.size - 1)).toInt()
                    val point = routePoints[targetIndex]
                    
                    val newPos = GeoPoint(point.first, point.second)
                    
                    if (carMarker?.position != null && targetIndex < routePoints.size - 1) {
                        val nextPoint = routePoints[targetIndex + 1]
                        val nextPos = GeoPoint(nextPoint.first, nextPoint.second)
                        
                        if (newPos.latitude != nextPos.latitude || newPos.longitude != nextPos.longitude) {
                            val bearing = newPos.bearingTo(nextPos).toFloat()
                            carMarker?.rotation = bearing
                            
                            // Rotate map so direction of travel is always UP
                            b.mapView.mapOrientation = 360f - bearing
                        }
                    }

                    carMarker?.position = newPos
                    // Ensure cab is visible in map view during move
                    b.mapView.controller.animateTo(carMarker?.position)
                    b.mapView.controller.setZoom(18.0) // closer zoom for navigation
                    b.mapView.invalidate()
                }
            }
            override fun onFinish() {}
        }.start()

        b.btnCompleteRide.setOnClickListener {
            rideTimer?.cancel()
            db.updateStatus(bookingId, "COMPLETED")
            Toast.makeText(this, "Ride completed! ₹%.0f".format(booking.fare), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        rideTimer?.cancel()
        super.onDestroy()
    }
}
