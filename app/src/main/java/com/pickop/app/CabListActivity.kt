package com.pickop.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pickop.app.adapter.CabAdapter
import com.pickop.app.api.ApiClient
import com.pickop.app.databinding.ActivityCabListBinding
import com.pickop.app.model.CabTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CabListActivity : AppCompatActivity() {

    private lateinit var b: ActivityCabListBinding
    private var distanceKm: Double = 5.0
    private var etaMin: Int = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCabListBinding.inflate(layoutInflater)
        setContentView(b.root)

        val pickup = intent.getStringExtra("pickup") ?: ""
        val dropoff = intent.getStringExtra("dropoff") ?: ""

        // Initial estimate: 1 km per 3 chars, min 2 km, max 25 km
        distanceKm = (dropoff.length.toDouble() / 3.0).coerceIn(2.0, 25.0)
        etaMin = (distanceKm * 2.5).toInt()

        b.tvRoutePickup.text = pickup
        b.tvRouteDropoff.text = dropoff
        b.tvRouteInfo.text = "%.1f km · ~%d min".format(distanceKm, etaMin)

        MapHelper.setupMap(b.mapView)

        b.btnBack.setOnClickListener { finish() }
        b.rvCabs.layoutManager = LinearLayoutManager(this)

        setupAdapter(pickup, dropoff)

        // Asynchronously fetch real road distance via Retrofit (OSRM + Photon)
        lifecycleScope.launch(Dispatchers.IO) {
            val routeResult = ApiClient.fetchRouteData(pickup, dropoff, distanceKm)
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    distanceKm = routeResult.distanceKm
                    etaMin = routeResult.etaMin
                    b.tvRouteInfo.text = "%.1f km · ~%d min (Live Route)".format(distanceKm, etaMin)
                    setupAdapter(pickup, dropoff)
                    
                    MapHelper.drawRoute(
                        b.mapView,
                        routeResult.routePoints,
                        routeResult.pickupLat, routeResult.pickupLon,
                        routeResult.dropoffLat, routeResult.dropoffLon
                    )
                    
                    if (routeResult.pickupLat != null && routeResult.pickupLon != null) {
                        MapHelper.showNearbyCabs(b.mapView, routeResult.pickupLat!!, routeResult.pickupLon!!)
                    }
                }
            }
        }
    }

    private fun setupAdapter(pickup: String, dropoff: String) {
        b.rvCabs.adapter = CabAdapter(CabTypes.all, distanceKm) { cab, fare ->
            val intent = Intent(this, BookingActivity::class.java).apply {
                putExtra("pickup", pickup)
                putExtra("dropoff", dropoff)
                putExtra("cab_name", cab.name)
                putExtra("cab_icon", cab.icon)
                putExtra("fare", fare)
                putExtra("distance_km", distanceKm)
            }
            startActivity(intent)
            finish()
        }
    }
}
