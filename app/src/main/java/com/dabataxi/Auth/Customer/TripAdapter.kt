package com.dabataxi.Auth.Customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dabataxi.R
import java.text.SimpleDateFormat
import java.util.*

class TripAdapter(private val trips: List<Trip>) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFare: TextView = itemView.findViewById(R.id.tvFare)
        val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        // Fare
        val fareText = if (trip.fare > 0.0) {
            "Fare: $${"%.2f".format(trip.fare)}"
        } else {
            "Fare: N/A"
        }
        holder.tvFare.text = fareText

        // Start Time
        val startText = "Start Time: ${formatTimestamp(trip.startTime)}"
        holder.tvStartTime.text = startText

        // End Time
        val endText = "End Time: ${formatTimestamp(trip.endTime)}"
        holder.tvEndTime.text = endText
    }

    override fun getItemCount(): Int = trips.size

    private fun formatTimestamp(timestamp: Long): String {
        // If timestamp is 0 or less, return "N/A"
        if (timestamp <= 0) {
            return "N/A"
        }
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}
