package com.pickop.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pickop.app.databinding.BottomSheetReceiptBinding
import com.pickop.app.db.BookingDbHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReceiptBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripId = arguments?.getLong(ARG_TRIP_ID) ?: return
        val db = BookingDbHelper(requireContext())
        val trip = db.getBooking(tripId) ?: return

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        
        binding.tvReceiptDate.text = dateFormat.format(Date(trip.createdAt))
        
        // Fare details
        val base = if (trip.baseFare > 0.0) trip.baseFare else (trip.fare / trip.surgeMultiplier)
        binding.tvBaseFare.text = "₹%.2f".format(base)
        binding.tvSurge.text = "%.1fx".format(trip.surgeMultiplier)
        binding.tvTotalFare.text = "₹%.0f".format(trip.fare)
        binding.tvPaymentMode.text = "Paid via ${trip.paymentMode}"

        // Driver details
        binding.tvDriverInfo.text = "${trip.driverName} • ${trip.vehicleNumber} (${trip.cabType})"
        binding.tvDriverRating.text = if (trip.driverRating > 0) {
            "⭐ ${trip.driverRating} Rating Given"
        } else {
            "No Rating Given"
        }

        binding.btnBookAgain.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("prefill_pickup", trip.pickup)
                putExtra("prefill_dropoff", trip.dropoff)
            }
            startActivity(intent)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: Long): ReceiptBottomSheet {
            val fragment = ReceiptBottomSheet()
            val args = Bundle()
            args.putLong(ARG_TRIP_ID, tripId)
            fragment.arguments = args
            return fragment
        }
    }
}
