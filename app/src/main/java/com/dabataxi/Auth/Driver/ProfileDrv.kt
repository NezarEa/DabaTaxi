package com.dabataxi.Auth.Driver

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix

class ProfileDrv : Fragment(R.layout.fragment_profile_drv) {

    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverEmail: TextView
    private lateinit var tvDriverPhone: TextView
    private lateinit var tvDriverLicense: TextView
    private lateinit var qrCodeView: ImageView
    private lateinit var btnLogout: Button

    private val auth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvDriverName = view.findViewById(R.id.tvDriverName)
        tvDriverEmail = view.findViewById(R.id.tvDriverEmail)
        tvDriverPhone = view.findViewById(R.id.tvDriverPhone)
        tvDriverLicense = view.findViewById(R.id.tvDriverLicense)
        qrCodeView = view.findViewById(R.id.qrCodeView)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Get the current user from FirebaseAuth
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Fetch user info from Firebase Realtime Database
            fetchUserProfile(user.uid)
        }
        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(context, getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show()
       findNavController().navigate(R.id.home2)
    }

    private fun fetchUserProfile(userId: String) {
        // Fetch driver data from Realtime Database
        database.child("drivers").child(userId).get().addOnSuccessListener { dataSnapshot ->
            val name = dataSnapshot.child("name").value as? String ?: "John Doe"
            val email = dataSnapshot.child("email").value as? String ?: "No Email"
            val phone = dataSnapshot.child("phone").value as? String ?: "No Phone"
            val license = dataSnapshot.child("licenseNumber").value as? String ?: "No License"

            // Set the profile info in the TextViews
            tvDriverName.text = name
            tvDriverEmail.text = email
            tvDriverPhone.text = phone
            tvDriverLicense.text = "licenseNumber: $license"

            // Generate the QR code with the user's info
            val qrCodeContent = "Name: $name\nEmail: $email\nPhone: $phone\nlicenseNumber: $license"
            generateQRCode(qrCodeContent)
        }.addOnFailureListener { exception ->
            // Handle the failure to retrieve the user info
            // Show a toast or error message as needed
            exception.printStackTrace()
        }
    }

    private fun generateQRCode(content: String) {
        val writer = QRCodeWriter()
        try {
            // Generate the QR Code
            val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            // Convert the BitMatrix to a Bitmap
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            // Set the generated QR code bitmap to the ImageView
            qrCodeView.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
