package com.pickop.app

import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.random.Random

object MapHelper {
    
    fun initOsmdroid(context: Context) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    fun setupMap(map: MapView) {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(5.0)
        // Default to India
        map.controller.setCenter(GeoPoint(20.5937, 78.9629))
    }

    fun enableMyLocation(map: MapView, context: Context) {
        val provider = GpsMyLocationProvider(context)
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER)
        
        val locationOverlay = MyLocationNewOverlay(provider, map)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        
        locationOverlay.runOnFirstFix {
            map.post {
                val loc = locationOverlay.myLocation
                if (loc != null) {
                    map.controller.animateTo(loc)
                    map.controller.setZoom(15.0)
                    showNearbyCabs(map, loc.latitude, loc.longitude)
                }
            }
        }
        
        map.overlays.add(locationOverlay)
        map.invalidate()
    }

    fun showNearbyCabs(map: MapView, centerLat: Double, centerLon: Double) {
        map.overlays.removeAll { it is Marker && it.title == "Available Cab" }
        val random = Random
        for (i in 1..6) {
            val latOffset = (random.nextDouble() - 0.5) * 0.03
            val lonOffset = (random.nextDouble() - 0.5) * 0.03
            val cab = Marker(map)
            cab.position = GeoPoint(centerLat + latOffset, centerLon + lonOffset)
            cab.title = "Available Cab"
            cab.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            try {
                cab.icon = ContextCompat.getDrawable(map.context, R.drawable.ic_car_top_view)
            } catch (e: Exception) {}
            map.overlays.add(cab)
        }
        map.invalidate()
    }
    
    fun createCabMarker(map: MapView, lat: Double, lon: Double): Marker {
        val marker = Marker(map)
        marker.position = GeoPoint(lat, lon)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.title = "Your Cab"
        try {
            marker.icon = ContextCompat.getDrawable(map.context, R.drawable.ic_car_top_view)
        } catch (e: Exception) {}
        map.overlays.add(marker)
        map.invalidate()
        return marker
    }

    fun drawRoute(
        map: MapView,
        routePoints: List<Pair<Double, Double>>,
        pickupLat: Double?, pickupLon: Double?,
        dropoffLat: Double?, dropoffLon: Double?
    ) {
        map.overlays.clear()

        val geoPoints = routePoints.map { GeoPoint(it.first, it.second) }
        
        if (geoPoints.isNotEmpty()) {
            val line = Polyline()
            line.setPoints(geoPoints)
            line.outlinePaint.color = Color.parseColor("#1A73E8") // Blue route
            line.outlinePaint.strokeWidth = 10f
            map.overlays.add(line)
        }

        // Add Pickup Marker
        var pPoint: GeoPoint? = null
        if (pickupLat != null && pickupLon != null) {
            pPoint = GeoPoint(pickupLat, pickupLon)
            val marker = Marker(map)
            marker.position = pPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Pickup"
            map.overlays.add(marker)
        } else if (geoPoints.isNotEmpty()) {
            pPoint = geoPoints.first()
        }

        // Add Dropoff Marker
        var dPoint: GeoPoint? = null
        if (dropoffLat != null && dropoffLon != null) {
            dPoint = GeoPoint(dropoffLat, dropoffLon)
            val marker = Marker(map)
            marker.position = dPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Drop-off"
            map.overlays.add(marker)
        } else if (geoPoints.isNotEmpty()) {
            dPoint = geoPoints.last()
        }

        map.invalidate()

        // Zoom to fit bounds
        if (pPoint != null && dPoint != null) {
            val boundingBox = BoundingBox.fromGeoPoints(listOf(pPoint, dPoint) + geoPoints)
            map.post {
                map.zoomToBoundingBox(boundingBox, true, 100)
            }
        }
    }
}
