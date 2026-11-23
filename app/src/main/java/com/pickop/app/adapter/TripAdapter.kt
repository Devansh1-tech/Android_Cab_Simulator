package com.pickop.app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pickop.app.databinding.ItemTripBinding
import com.pickop.app.model.Booking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val trips: List<Booking>
) : RecyclerView.Adapter<TripAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    inner class VH(val b: ItemTripBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val trip = trips[position]

        holder.b.tvTripCabType.text = trip.cabType
        holder.b.tvTripFare.text = "₹%.0f".format(trip.fare)
        holder.b.tvTripRoute.text = "${trip.pickup}  →  ${trip.dropoff}"
        holder.b.tvTripDate.text = dateFormat.format(Date(trip.createdAt))

        holder.b.tvTripStatus.text = trip.status
        holder.b.tvTripStatus.setTextColor(
            when (trip.status) {
                "COMPLETED"  -> Color.parseColor("#4CAF50")
                "CANCELLED"  -> Color.parseColor("#E53935")
                "EXPIRED"    -> Color.parseColor("#FF9800")
                "IN_PROGRESS"-> Color.parseColor("#1A73E8")
                else         -> Color.parseColor("#757575")
            }
        )
    }

    override fun getItemCount() = trips.size
}
