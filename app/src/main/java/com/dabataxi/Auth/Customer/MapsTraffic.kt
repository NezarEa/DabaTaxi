package com.dabataxi.Auth.Customer

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.dabataxi.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.zxing.BarcodeFormat
import java.util.UUID

class MapsTraffic : Fragment(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 124
        private const val CHANNEL_ID = "trip_notifications"
        private const val NOTIFICATION_ID = 101
    }
    private val BASE_FARE = 2.5
    private val RATE_PER_KM = 1.5
    private val PER_MINUTE_RATE = 0.5
    private lateinit var mapView: MapView
    private lateinit var btnStartTrip: MaterialButton
    private lateinit var fabScanQR: FloatingActionButton
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvFare: TextView
    private lateinit var ivQRCode: ImageView
    private var googleMap: GoogleMap? = null
    private var currentMarker: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var database: DatabaseReference
    private var isTripping = false
    private var startLocation: Location? = null
    private var startTime: Long = 0
    private var totalDistance: Float = 0f
    private var lastLocation: Location? = null
    private val locations = mutableListOf<LatLng>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps_traffic, container, false)
        initViews(view)
        database = FirebaseDatabase.getInstance().reference
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupListeners()
        createNotificationChannel()
        requestLocationPermission()
        return view
    }

    private fun initViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        btnStartTrip = view.findViewById(R.id.btnStartTrip)
        fabScanQR = view.findViewById(R.id.fabScanQR)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvTime = view.findViewById(R.id.tvTime)
        tvFare = view.findViewById(R.id.tvFare)
    }

    private fun setupListeners() {
        btnStartTrip.setOnClickListener {
            if (!isTripping) {
                startTrip()
            } else {
                endTrip()
            }
        }

        fabScanQR.setOnClickListener {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt(getString(R.string.qr_scan_prompt))
            integrator.initiateScan()
        }
    }

    @AfterPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE)
    private fun requestLocationPermission() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
            initializeLocationTracking()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission()
            }
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.location_permission_rationale),
                LOCATION_PERMISSION_REQUEST_CODE,
                *perms
            )
        }
    }

    @AfterPermissionGranted(NOTIFICATION_PERMISSION_REQUEST_CODE)
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (EasyPermissions.hasPermissions(requireContext(), Manifest.permission.POST_NOTIFICATIONS)) {
            } else {
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.notification_permission_rationale),
                    NOTIFICATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun initializeLocationTracking() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    updateLocationOnMap(location)
                    if (isTripping) {
                        updateTaxiMeter(location)
                    }
                }
            }
        }

        if (EasyPermissions.hasPermissions(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    private fun updateLocationOnMap(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        if (currentMarker == null) {
            currentMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.current_position))
            )
        } else {
            currentMarker?.position = latLng
        }
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun startTrip() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (checkLocationPermission()) {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    startLocation = it
                    startTime = System.currentTimeMillis()
                    locations.clear()
                    locations.add(LatLng(it.latitude, it.longitude))

                    isTripping = true
                    btnStartTrip.text = getString(R.string.end_trip)
                    tvDistance.text = getString(R.string.distance, 0.0)
                    tvTime.text = getString(R.string.time, 0)
                    tvFare.text = getString(R.string.fare, BASE_FARE)
                }
            }
        }
    }

    private fun endTrip() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (checkLocationPermission()) {
                val endLocation = fusedLocationClient.lastLocation.await()
                endLocation?.let { end ->
                    val distance = calculateTotalDistance()
                    val duration = calculateTripDuration()
                    val fare = calculateFare(distance, duration)

                    tvDistance.text = getString(R.string.distance, distance)
                    tvTime.text = getString(R.string.time, duration.toInt())
                    tvFare.text = getString(R.string.fare, fare)

                    drawTripRoute()

                    // Save trip details to Firebase
                    saveTripToFirebase(end, distance, duration, fare)

                    isTripping = false
                    btnStartTrip.text = getString(R.string.start_trip)

                    sendTripEndNotification()
                }
            }
        }
    }

    private fun calculateTotalDistance(): Double {
        var totalDistance = 0.0
        for (i in 1 until locations.size) {
            val results = FloatArray(1)
            Location.distanceBetween(
                locations[i - 1].latitude, locations[i - 1].longitude,
                locations[i].latitude, locations[i].longitude,
                results
            )
            totalDistance += results[0] / 1000.0
        }
        return totalDistance
    }

    private fun calculateTripDuration(): Double {
        return (System.currentTimeMillis() - startTime) / 60000.0
    }

    private fun calculateFare(distance: Double, minutes: Double): Double {
        return if (distance < 0 || minutes < 0) {
            Toast.makeText(requireContext(), "Invalid trip data. Please try again.", Toast.LENGTH_SHORT).show()
            BASE_FARE
        } else {
            BASE_FARE + (distance * RATE_PER_KM) + (minutes * PER_MINUTE_RATE)
        }
    }

    private fun drawTripRoute() {
        googleMap?.addPolyline(
            PolylineOptions().addAll(locations).color(Color.BLUE)
        )
    }

    private fun checkLocationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.location_permission_rationale),
                LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            false
        } else {
            true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTripEndNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Notification permission required", Toast.LENGTH_SHORT).show()
            return
        }

        val distanceText = tvDistance.text.toString()
        val timeText = tvTime.text.toString()
        val fareText = tvFare.text.toString()

        val notificationBuilder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.trip_ended))
            .setContentText("$distanceText, $timeText, $fareText")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(requireContext())) {
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        val defaultLocation = LatLng(33.5731, -7.5898) // Casablanca coordinates
        currentMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(defaultLocation)
                .title(getString(R.string.default_location))
        )
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                initializeLocationTracking()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermission()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {

            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                Toast.makeText(requireContext(), getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show()
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                Toast.makeText(requireContext(), getString(R.string.notification_permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun updateTaxiMeter(location: Location) {
        var distanceKm = 0.0
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location) // distance in meters
            totalDistance += distance
            distanceKm = totalDistance / 1000.0
            tvDistance.text = getString(R.string.distance, distanceKm)
            locations.add(LatLng(location.latitude, location.longitude))
        } else {
            locations.add(LatLng(location.latitude, location.longitude))
        }
        lastLocation = location
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
        val minutes = elapsedTime / 60.0
        tvTime.text = getString(R.string.time, minutes.toInt())
        val fare = calculateFare(distanceKm, minutes)
        tvFare.text = getString(R.string.fare, fare)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show()
            } else {
                // Fetch driver info and generate QR code after scanning
                fetchDriverInfoAndGenerateQRCode(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun fetchDriverInfoAndGenerateQRCode(qrData: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("drivers").document(currentUserId)

            userRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val driverName = documentSnapshot.getString("name") ?: "Unknown"
                    val driverEmail = documentSnapshot.getString("email") ?: "Unknown"
                    val driverPhone = documentSnapshot.getString("phone") ?: "Unknown"
                    val driverLicense = documentSnapshot.getString("licenseNumber") ?: "Unknown"

                    // Now generate the QR code with the scanned data and driver info
                    generateQRCode(qrData, driverName, driverEmail, driverPhone, driverLicense)
                } else {
                    // Handle case where document doesn't exist
                    Toast.makeText(requireContext(), "Driver data not found!", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Driver data not found for UID: $currentUserId")
                }
            }.addOnFailureListener { exception ->
                // Log the  exception message
                Toast.makeText(requireContext(), "Error fetching driver data", Toast.LENGTH_SHORT).show()

                Log.e("Firestore", "Error fetching driver data", exception)

                if (exception is FirebaseFirestoreException) {
                    Log.e("FirestoreErrorCode", "Error code: ${exception.code}")
                    Log.e("FirestoreError", "Detailed message: ${exception.message}")
                }
            }
        } else {
            // Handle case where user is not logged in
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }



    private fun generateQRCode(data: String, driverName: String, driverEmail: String, driverPhone: String, driverLicense: String) {
        try {
            // Generate the QR code
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(
                data,
                BarcodeFormat.QR_CODE,
                512,
                512
            )
            ivQRCode.setImageBitmap(bitmap) // Set the generated QR code in your ImageView

            // Show an alert with the driver's information
            showDriverInfoDialog(driverName, driverEmail, driverPhone, driverLicense)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), getString(R.string.error_qr_generation), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDriverInfoDialog(driverName: String, driverEmail: String, driverPhone: String, driverLicense: String) {
        // Create an AlertDialog to display the driver's information
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Driver Information")
            .setMessage(
                "Name: $driverName\n" +
                        "Email: $driverEmail\n" +
                        "Phone: $driverPhone\n" +
                        "License: $driverLicense"
            )
            .setPositiveButton("OK") { dialog: DialogInterface, _ ->
                dialog.dismiss() // Close the dialog when the user clicks OK
            }
            .setCancelable(true)

        // Optionally, add the QR code image in the dialog
        val imageView = ImageView(requireContext())
        imageView.setImageBitmap(generateQRCodeBitmap(driverName, driverEmail, driverPhone, driverLicense)) // Use your QR code generation logic here
        builder.setView(imageView)

        builder.show()
    }

    // Helper method to generate QR code bitmap for displaying in the dialog
    private fun generateQRCodeBitmap(driverName: String, driverEmail: String, driverPhone: String, driverLicense: String): Bitmap {
        val data = "$driverName\n$driverEmail\n$driverPhone\n$driverLicense"
        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.encodeBitmap(
            data,
            BarcodeFormat.QR_CODE,
            512,
            512
        )
    }

    private fun saveTripToFirebase(endLocation: Location, distance: Double, duration: Double, fare: Double) {
        val tripId = UUID.randomUUID().toString()
        val userId = "user123"
        val startLocData = startLocation?.let {
            LocationData(it.latitude, it.longitude)
        }
        val endLocData = LocationData(endLocation.latitude, endLocation.longitude)
        val routeData = locations.map { LatLng ->
            LocationData(LatLng.latitude, LatLng.longitude)
        }
        val trip = Trip(
            tripId = tripId,
            userId = userId,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            startLocation = startLocData,
            endLocation = endLocData,
            distance = distance,
            duration = duration,
            fare = fare,
            route = routeData
        )
        database.child("trips").child(tripId).setValue(trip)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Trip saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save trip: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}