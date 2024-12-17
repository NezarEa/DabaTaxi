package com.dabataxi.Auth.Driver

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpDrv : Fragment(R.layout.fragment_sign_up_drv) {
    private lateinit var edtName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var edtConfirmPassword: TextInputEditText
    private lateinit var edtVehicleType: TextInputEditText
    private lateinit var edtLicenseNumber: TextInputEditText
    private lateinit var radioGender: RadioGroup
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var btnCreateAccount: MaterialButton

    private lateinit var firebaseAuth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize views
        edtName = view.findViewById(R.id.edtName)
        edtEmail = view.findViewById(R.id.edtEmail)
        edtPhone = view.findViewById(R.id.edtPhone)
        edtPassword = view.findViewById(R.id.edtPassword)
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword)
        edtVehicleType = view.findViewById(R.id.edtVehicleType)
        edtLicenseNumber = view.findViewById(R.id.edtLicenseNumber)
        radioGender = view.findViewById(R.id.radioGender)
        progressBar = view.findViewById(R.id.progressBar)
        btnCreateAccount = view.findViewById(R.id.btnCreateAccount)

        // Sign-up button click listener
        btnCreateAccount.setOnClickListener {
            signUpDriver()
        }
    }

    private fun signUpDriver() {
        val name = edtName.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        val confirmPassword = edtConfirmPassword.text.toString().trim()
        val vehicleType = edtVehicleType.text.toString().trim()
        val licenseNumber = edtLicenseNumber.text.toString().trim()
        val genderId = radioGender.checkedRadioButtonId
        val gender = view?.findViewById<RadioButton>(genderId)?.text.toString()

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) ||
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) ||
            TextUtils.isEmpty(vehicleType) || TextUtils.isEmpty(licenseNumber) || gender.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.all_fields_are_required), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(),
                getString(R.string.password_must_be_at_least_6_characters_long), Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Create user in Firebase Auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // Save driver details to Firebase Database
                    val driver = Driver(
                        id = firebaseAuth.currentUser?.uid ?: "",
                        name = name,
                        email = email,
                        phone = phone,
                        vehicleType = vehicleType,
                        licenseNumber = licenseNumber,
                        photoUrl = "", // Handle profile photo upload separately
                        cartId = "" // Add logic for linking to a cart if needed
                    )

                    // Save the driver data in the Firebase Database
                    database.child("drivers").child(driver.id).setValue(driver)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                Toast.makeText(requireContext(),
                                    getString(R.string.driver_sign_up_successful), Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.profileDrv)
                            } else {
                                Toast.makeText(requireContext(),
                                    getString(
                                        R.string.error_saving_driver_data,
                                        dbTask.exception?.message
                                    ), Toast.LENGTH_SHORT).show()
                            }
                        }

                } else {
                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}