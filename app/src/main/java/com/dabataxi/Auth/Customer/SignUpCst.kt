package com.dabataxi.Auth.Customer

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.dabataxi.databinding.FragmentSignUpCstBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class SignUpCst : Fragment() {

    private lateinit var binding: FragmentSignUpCstBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "UserSessionPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpCstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth, Database, and Storage
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Set up click listener
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnCreateAccount.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val phone = binding.edtPhone.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val confirmPassword = binding.edtConfirmPassword.text.toString().trim()

            if (validateInputs(name, email, phone, password, confirmPassword)) {
                createUserAccount(name, email, phone, password)
            }
        }

    }

    private fun validateInputs(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.name_is_required)
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_is_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email_format)
            isValid = false
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = getString(R.string.phone_number_is_required)
            isValid = false
        } else if (phone.length != 10) {
            binding.tilPhone.error = getString(R.string.phone_number_must_be_10_digits)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_is_required)
            isValid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.password_must_be_at_least_8_characters)
            isValid = false
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.please_confirm_your_password)
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.passwords_do_not_match)
            isValid = false
        }

        return isValid
    }

    private fun createUserAccount(name: String, email: String, phone: String, password: String) {
        // Show progress indicator
        binding.progressBar.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                // Hide progress indicator
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener

                    // Determine gender
                    val gender = when (binding.radioGender.checkedRadioButtonId) {
                        R.id.radioMale -> "Male"
                        R.id.radioFemale -> "Female"
                        else -> "Not Specified"
                    }

                    // Prepare user data to save in Realtime Database
                    val userData = mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "gender" to gender,
                        "profileImageUrl" to "default_image_url"  // Default image path
                    )

                    // Save user data to database
                    saveUserDataToDatabase(userId, userData)
                } else {
                    handleSignUpError(task.exception)
                }
            }
    }

    private fun saveUserDataToDatabase(userId: String, userData: Map<String, String>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Save to Firebase database on IO thread
                withContext(Dispatchers.IO) {
                    database.child("users").child(userId).setValue(userData).await()
                }

                // Mark the user as logged in
                setUserLoggedIn(true)

                // Show success message
                Snackbar.make(requireView(),
                    getString(R.string.account_created_successfully), Snackbar.LENGTH_SHORT).show()

                // Navigate to home screen
                findNavController().navigate(R.id.action_signUpCst_to_homeAppCST)
            } catch (e: Exception) {
                // Show error message
                Snackbar.make(requireView(),
                    getString(R.string.error, e.message), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignUpError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthUserCollisionException -> getString(R.string.this_email_is_already_registered)
            else -> getString(R.string.sign_up_failed, exception?.message ?: getString(R.string.unknown_error))
        }

        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
    }

    private fun setUserLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            apply()
        }
    }
}
