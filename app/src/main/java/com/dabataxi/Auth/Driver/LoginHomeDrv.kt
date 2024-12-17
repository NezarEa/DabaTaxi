package com.dabataxi.Auth.Driver

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginHomeDrv : Fragment(R.layout.fragment_login_home_dvr) {

    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnCreateAccount: TextView
    private lateinit var btnForgotPassword: TextView


    private lateinit var firebaseAuth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize views
        edtEmail = view.findViewById(R.id.edtEmail)
        edtPassword = view.findViewById(R.id.edtPassword)
        progressBar = view.findViewById(R.id.progressBar)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnCreateAccount = view.findViewById(R.id.btnCreateAccount)
        btnForgotPassword = view.findViewById(R.id.btnForgotPassword)

        // Set up click listeners
        btnLogin.setOnClickListener { loginDriver() }
        btnCreateAccount.setOnClickListener { navigateToCreateAccount() }
        btnForgotPassword.setOnClickListener { navigateToForgotPassword() }
    }

    private fun loginDriver() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(requireContext(),
                getString(R.string.please_enter_your_email), Toast.LENGTH_SHORT).show()
            return
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(),
                getString(R.string.please_enter_your_password), Toast.LENGTH_SHORT).show()
            return
        }

        // Show the progress bar while logging in
        progressBar.visibility = View.VISIBLE

        // Sign in with FirebaseAuth
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                progressBar.visibility = View.GONE // Hide the progress bar after completion

                if (task.isSuccessful) {
                    // Login success
                    Toast.makeText(requireContext(),
                        getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                    navigateToDriverHome()
                } else {
                    // Handle login failure (more informative message)
                    Toast.makeText(requireContext(),
                        getString(R.string.login_failed, task.exception?.localizedMessage), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToCreateAccount() {
        // Navigate to the sign-up screen for drivers
        findNavController().navigate(R.id.signUpDrv)
    }

    private fun navigateToForgotPassword() {
        // Navigate to the forgot password screen
        findNavController().navigate(R.id.forgetPassCst)
    }

    private fun navigateToDriverHome() {
        // Navigate to the driver's profile/home screen
        findNavController().navigate(R.id.profileDrv)
    }
}
