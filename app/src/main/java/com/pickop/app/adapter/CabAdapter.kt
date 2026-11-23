package com.pickop.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pickop.app.databinding.ItemCabBinding
import com.pickop.app.model.CabType
import com.pickop.app.model.CabTypes

class CabAdapter(
    private val cabs: List<CabType>,
    private val distanceKm: Double,
    private val onCabSelected: (CabType, Double) -> Unit
) : RecyclerView.Adapter<CabAdapter.VH>() {

    inner class VH(val b: ItemCabBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cab = cabs[position]
        val fare = CabTypes.fareFor(cab, distanceKm)

        holder.b.tvCabIcon.text = cab.icon
        holder.b.tvCabName.text = cab.name
        holder.b.tvCabEta.text = "${cab.etaMinutes} min away"
        holder.b.tvCabFare.text = "₹%.0f".format(fare)

        holder.itemView.setOnClickListener {
            onCabSelected(cab, fare)
        }
    }

    override fun getItemCount() = cabs.size
}
