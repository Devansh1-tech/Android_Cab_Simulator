package com.pickop.app.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// ── Retrofit DTOs ──

data class PhotonSearchResponse(
    @SerializedName("features") val features: List<PhotonFeature>?
)

data class PhotonFeature(
    @SerializedName("geometry") val geometry: PhotonGeometry?
)

data class PhotonGeometry(
    @SerializedName("coordinates") val coordinates: List<Double>? // [longitude, latitude]
)

data class OsrmResponse(
    @SerializedName("code") val code: String?,
    @SerializedName("routes") val routes: List<OsrmRouteItem>?
)

data class OsrmRouteItem(
    @SerializedName("distance") val distance: Double?, // meters
    @SerializedName("duration") val duration: Double?, // seconds
    @SerializedName("geometry") val geometry: OsrmGeometry?
)

data class OsrmGeometry(
    @SerializedName("coordinates") val coordinates: List<List<Double>>? // [[lon, lat], [lon, lat]]
)

// ── Retrofit Interfaces ──

interface PhotonApiService {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 1
    ): PhotonSearchResponse
}

interface OsrmApiService {
    @GET("route/v1/driving/{coords}?overview=full&geometries=geojson")
    suspend fun getRoute(
        @Path("coords") coords: String
    ): OsrmResponse
}

// ── Singleton Retrofit Client ──

object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val photonService: PhotonApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotonApiService::class.java)
    }

    private val osrmService: OsrmApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OsrmApiService::class.java)
    }

    /**
     * Uses Retrofit to fetch real route distance (km), estimated duration (mins),
     * and the route geometry points (list of Pair(lat, lon)).
     */
    suspend fun fetchRouteData(
        pickup: String,
        dropoff: String,
        fallbackDistanceKm: Double
    ): RouteResult {
        val result = RouteResult(
            distanceKm = fallbackDistanceKm,
            etaMin = (fallbackDistanceKm * 2.5).toInt().coerceAtLeast(2),
            routePoints = emptyList(),
            pickupLat = null, pickupLon = null,
            dropoffLat = null, dropoffLon = null
        )

        return try {
            val pickupResp = photonService.search(pickup)
            val dropoffResp = photonService.search(dropoff)

            val pCoord = pickupResp.features?.firstOrNull()?.geometry?.coordinates
            val dCoord = dropoffResp.features?.firstOrNull()?.geometry?.coordinates

            if (pCoord != null && dCoord != null && pCoord.size >= 2 && dCoord.size >= 2) {
                result.pickupLat = pCoord[1]
                result.pickupLon = pCoord[0]
                result.dropoffLat = dCoord[1]
                result.dropoffLon = dCoord[0]

                // OSRM coordinates format: lon,lat;lon,lat
                val coords = "${pCoord[0]},${pCoord[1]};${dCoord[0]},${dCoord[1]}"
                val routeResp = osrmService.getRoute(coords)
                val route = routeResp.routes?.firstOrNull()
                if (route?.distance != null && route.duration != null) {
                    result.distanceKm = (route.distance / 100.0).roundToInt() / 10.0
                    result.distanceKm = result.distanceKm.coerceAtLeast(1.0)
                    result.etaMin = ((route.duration / 60.0).roundToInt()).coerceAtLeast(1)
                    
                    val points = route.geometry?.coordinates?.mapNotNull { c ->
                        if (c.size >= 2) Pair(c[1], c[0]) else null // Convert to Pair(lat, lon)
                    } ?: emptyList()
                    result.routePoints = points
                }
                
                // Fallback to straight line if OSRM failed
                if (result.routePoints.isEmpty()) {
                    result.routePoints = listOf(Pair(result.pickupLat!!, result.pickupLon!!), Pair(result.dropoffLat!!, result.dropoffLon!!))
                }
            } else {
                // Fallback coordinates if location search failed (e.g. New Delhi to Noida)
                result.pickupLat = 28.6139
                result.pickupLon = 77.2090
                result.dropoffLat = 28.5355
                result.dropoffLon = 77.3910
                result.routePoints = listOf(Pair(28.6139, 77.2090), Pair(28.5355, 77.3910))
            }
            result
        } catch (e: Exception) {
            // Hardcode fallback on exception
            result.pickupLat = 28.6139
            result.pickupLon = 77.2090
            result.dropoffLat = 28.5355
            result.dropoffLon = 77.3910
            result.routePoints = listOf(Pair(28.6139, 77.2090), Pair(28.5355, 77.3910))
            result
        }
    }
}

data class RouteResult(
    var distanceKm: Double,
    var etaMin: Int,
    var routePoints: List<Pair<Double, Double>>,
    var pickupLat: Double?,
    var pickupLon: Double?,
    var dropoffLat: Double?,
    var dropoffLon: Double?
)
