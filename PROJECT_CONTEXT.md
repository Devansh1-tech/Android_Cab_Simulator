# PROJECT CONTEXT: RideSim (Uber / Rapido Style Cab & Bike Booking Simulation)

## 1. Project Overview
Build an Android ride-hailing simulation app in **Kotlin** and **Jetpack Compose** modelled after **Uber** and **Rapido** in India. The app integrates the **Google Maps Platform** for real map rendering, location search, and real road routing, paired with a **local Coroutines-based Simulation Engine** that spawns virtual drivers (Bikes, Autos, Minis, Sedans), animates them smoothly along real Google Maps road polylines, calculates dynamic ₹ fares with surge pricing, and manages the full end-to-end ride lifecycle (including 4-digit OTP verification).

---

## 2. Tech Stack & Libraries (Strict Requirements)
* **Language:** Kotlin (100% Kotlin, Coroutines, `StateFlow`, `SharedFlow`)
* **UI Framework:** Jetpack Compose + Material 3 (Dark/Light theme support)
* **Architecture:** Clean Architecture + MVVM (Unidirectional Data Flow)
* **Maps & Location:**
  * `com.google.maps.android:maps-compose:9.0.1` (latest Maps Compose library)
  * `com.google.maps.android:android-maps-utils` (for polyline decoding and spherical geometry/bearing calculations)
  * `com.google.android.gms:play-services-location` (Fused Location Provider for user GPS)
  * **Google Places API** (for pickup/drop-off autocomplete search)
  * **Google Routes API** (REST call via Retrofit/Ktor Client to fetch real road distance, duration, and encoded polyline)
* **Networking:** Retrofit 2 + OkHttp + Kotlinx Serialization (or Moshi)
* **Local Persistence:** Room Database (for saving trip history, receipts, and ratings)
* **Dependency Injection:** Hilt (or Koin)

---

## 3. Google Maps API Configuration & Security
* **Required Google Cloud APIs to Enable:**
  1. Maps SDK for Android
  2. Places API
  3. Routes API
* **API Key Security:**
  * NEVER hardcode the API key in Kotlin files or commit it to Git.
  * Store the key in `local.properties` as `MAPS_API_KEY=your_key_here` and inject it into `AndroidManifest.xml` via Gradle `manifestPlaceholders`.
  * Restrict the key in Google Cloud Console to the Android app's package name and SHA-1 fingerprint.

---

## 4. Clean Architecture Folder Structure
Organize the codebase under `com.ridesim.app`:

```text
app/src/main/java/com/ridesim/app/
├── data/
│   ├── local/          # Room DB, TripDao, TripEntity
│   ├── remote/         # RoutesApiService, PlacesService, DTOs
│   ├── simulation/     # DriverFleetSimulator, RouteAnimator, SurgeEngine
│   └── repository/     # RideRepositoryImpl, LocationRepositoryImpl
├── domain/
│   ├── model/          # VehicleType, Driver, RideState, FareEstimate, TripReceipt
│   ├── repository/     # Interfaces
│   └── usecase/        # CalculateFareUseCase, FindNearestDriverUseCase, DecodeRouteUseCase
├── presentation/
│   ├── home/           # HomeScreen (Full-screen GoogleMap + BottomSheet)
│   ├── search/         # LocationSearchScreen (Pickup & Drop-off Autocomplete)
│   ├── booking/        # VehicleSelectionSheet (Bike, Auto, Mini, Sedan)
│   ├── tracking/       # LiveRideOverlay (Driver info, OTP, ETA, Cancel, SOS)
│   ├── summary/        # TripSummaryDialog (Receipt breakdown + Star rating)
│   ├── history/        # PastTripsScreen
│   └── components/     # Custom Map Markers, Animated Vehicle Icon, BottomSheet helpers
└── di/                 # Hilt/Koin modules