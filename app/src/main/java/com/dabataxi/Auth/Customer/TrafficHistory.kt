package com.dabataxi.Auth.Customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dabataxi.R
import com.dabataxi.databinding.FragmentTrafficHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TrafficHistory : Fragment() {

    private var _binding: FragmentTrafficHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val tripList = mutableListOf<Trip>()
    private lateinit var tripAdapter: TripAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrafficHistoryBinding.inflate(inflater, container, false)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://daba-taxi-43b44-default-rtdb.europe-west1.firebasedatabase.app")
            .reference

        // Initialize RecyclerView Adapter
        tripAdapter = TripAdapter(tripList)

        // Set up RecyclerView
        binding.recyclerViewTrips.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tripAdapter
            setHasFixedSize(true)
        }

        // Set up SwipeRefreshLayout for refreshing trips
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchTrips(isRefreshing = true)
        }

        // Fetch trips initially
        fetchTrips()

        return binding.root
    }

    private fun fetchTrips(isRefreshing: Boolean = false) {
        if (!isRefreshing) {
            // Show loading indicators
            binding.progressBar.visibility = View.VISIBLE
            binding.recyclerViewTrips.visibility = View.GONE
            binding.tvEmptyState.visibility = View.GONE
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            handleUserNotAuthenticated(isRefreshing)
            return
        }

        // Fetch trips for the current user
        val userTripsRef = database.child("trips").orderByChild("userId").equalTo(userId)
        Log.d("TrafficHistory", "Fetching trips for userId: $userId")

        userTripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripList.clear()  // Clear any old data
                Log.d("TrafficHistory", "Trips snapshot exists: ${snapshot.exists()}")

                for (tripSnapshot in snapshot.children) {
                    val trip = tripSnapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        tripList.add(trip)
                    } else {
                        Log.w("TrafficHistory", "TripSnapshot is null or doesn't match Trip data class")
                    }
                }

                updateUIAfterFetchingTrips()
            }

            override fun onCancelled(error: DatabaseError) {
                handleDatabaseError(error)
            }
        })
    }

    private fun handleUserNotAuthenticated(isRefreshing: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
        binding.tvEmptyState.text = getString(R.string.user_not_authenticated)
        binding.tvEmptyState.visibility = View.VISIBLE
        Log.w("TrafficHistory", "User is not signed in.")
    }

    private fun handleDatabaseError(error: DatabaseError) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
        binding.recyclerViewTrips.visibility = View.GONE
        binding.tvEmptyState.text = getString(R.string.error_loading_trips)
        binding.tvEmptyState.visibility = View.VISIBLE
        Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
        Log.e("TrafficHistory", "Database error: ${error.toException()}")
    }

    private fun updateUIAfterFetchingTrips() {
        if (tripList.isNotEmpty()) {
            // Sort trips by start time in descending order
            tripList.sortByDescending { it.startTime }
            tripAdapter.notifyDataSetChanged()

            binding.recyclerViewTrips.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        } else {
            binding.recyclerViewTrips.visibility = View.GONE
            binding.tvEmptyState.text = getString(R.string.no_trips_found)
            binding.tvEmptyState.visibility = View.VISIBLE
        }

        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Clean up binding to avoid memory leaks
    }
}
