package com.pickop.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pickop.app.adapter.TripAdapter
import com.pickop.app.databinding.ActivityHistoryBinding
import com.pickop.app.db.BookingDbHelper

class HistoryActivity : AppCompatActivity() {

    private lateinit var b: ActivityHistoryBinding
    private lateinit var db: BookingDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = BookingDbHelper(this)
        b.btnBack.setOnClickListener { finish() }

        loadTrips()
    }

    override fun onResume() {
        super.onResume()
        loadTrips()
    }

    private fun loadTrips() {
        val trips = db.getAllBookings()

        if (trips.isEmpty()) {
            b.tvEmpty.visibility = View.VISIBLE
            b.rvHistory.visibility = View.GONE
        } else {
            b.tvEmpty.visibility = View.GONE
            b.rvHistory.visibility = View.VISIBLE
            b.rvHistory.layoutManager = LinearLayoutManager(this)
            b.rvHistory.adapter = TripAdapter(trips)
        }
    }
}
