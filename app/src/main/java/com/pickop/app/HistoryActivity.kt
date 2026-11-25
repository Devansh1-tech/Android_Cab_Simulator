package com.pickop.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pickop.app.adapter.TripAdapter
import com.pickop.app.databinding.ActivityHistoryBinding
import com.pickop.app.db.BookingDbHelper
import com.pickop.app.model.Booking

class HistoryActivity : AppCompatActivity() {

    private lateinit var b: ActivityHistoryBinding
    private lateinit var db: BookingDbHelper
    private var allTrips: List<Booking> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = BookingDbHelper(this)
        b.btnBack.setOnClickListener { finish() }
        b.btnBookFirstRide.setOnClickListener { finish() } // Goes back to map

        b.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            filterTrips()
        }

        b.rvHistory.layoutManager = LinearLayoutManager(this)
        loadTrips()
    }

    override fun onResume() {
        super.onResume()
        loadTrips()
    }

    private fun loadTrips() {
        allTrips = db.getAllBookings()
        filterTrips()
    }
    
    private fun filterTrips() {
        val filtered = when {
            b.chipCompleted.isChecked -> allTrips.filter { it.status == "COMPLETED" }
            b.chipCancelled.isChecked -> allTrips.filter { it.status == "CANCELLED" }
            else -> allTrips
        }
        
        if (filtered.isEmpty()) {
            b.layoutEmpty.visibility = View.VISIBLE
            b.rvHistory.visibility = View.GONE
        } else {
            b.layoutEmpty.visibility = View.GONE
            b.rvHistory.visibility = View.VISIBLE
            val adapter = TripAdapter(filtered) { trip ->
                val bottomSheet = ReceiptBottomSheet.newInstance(trip.id)
                bottomSheet.show(supportFragmentManager, "ReceiptBottomSheet")
            }
            b.rvHistory.adapter = adapter
        }
    }
}
