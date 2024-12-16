package com.dabataxi.Auth.Customer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.dabataxi.databinding.FragmentEditProfileBinding
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfile : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)

        // Observe user profile data from ViewModel
        observeViewModel()

        // Load the current user profile
        viewModel.loadUserProfile()

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnChangeImage.setOnClickListener {
            chooseImage()
        }

        binding.btnUpdateProfile.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val phone = binding.edtPhone.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (validateInputs(name, email, phone, password)) {
                if (password.isNotEmpty()) {
                    reauthenticateUser(password) {
                        updateUserProfile(name, email, phone, password)
                    }
                } else {
                    updateUserProfile(name, email, phone, null)
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.homeAppCST) // Navigate back to the home screen
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            if (userProfile != null) {
                binding.edtName.setText(userProfile.name)
                binding.edtEmail.setText(userProfile.email)
                binding.edtPhone.setText(userProfile.phone)

                // Load profile image using Glide
                if (!userProfile.photoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(userProfile.photoUrl)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.ic_erro24)
                        .into(binding.imgProfile)
                } else {
                    binding.imgProfile.setImageResource(R.drawable.user)
                }
            }
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun validateInputs(name: String, email: String, phone: String, password: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.inputLayoutName.error = getString(R.string.name_cannot_be_empty)
            isValid = false
        } else {
            binding.inputLayoutName.error = null
        }

        if (email.isEmpty()) {
            binding.inputLayoutEmail.error = getString(R.string.email_cannot_be_empty)
            isValid = false
        } else {
            binding.inputLayoutEmail.error = null
        }

        if (phone.isEmpty()) {
            binding.inputLayoutPhone.error = getString(R.string.phone_cannot_be_empty)
            isValid = false
        } else {
            binding.inputLayoutPhone.error = null
        }

        if (password.isNotEmpty() && password.length < 8) {
            binding.inputLayoutPassword.error = getString(R.string.password_length_error)
            isValid = false
        } else {
            binding.inputLayoutPassword.error = null
        }

        return isValid
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImageUri)
                binding.imgProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Snackbar.make(requireView(), getString(R.string.failed_to_load_image), Snackbar.LENGTH_LONG).show()
            }
        } else {
            selectedImageUri = null  // Clear the image URI if no image is selected
        }
    }

    private fun reauthenticateUser(password: String, onSuccess: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(user!!.email!!, password)

        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess() // Proceed with profile update
                } else {
                    Snackbar.make(requireView(), "Re-authentication failed. Please try again.", Snackbar.LENGTH_LONG).show()
                }
            }
    }

    private fun updateUserProfile(name: String, email: String, phone: String, password: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(userId)

            // Prepare updated data
            val updatedData = hashMapOf(
                "name" to name,
                "email" to email,
                "phone" to phone
            )

            // Check if an image is selected and upload it to Firebase Storage
            selectedImageUri?.let {
                val storageRef: StorageReference = FirebaseStorage.getInstance().reference
                    .child("profile_images")
                    .child(userId)

                // Check if path already exists by attempting to download the URL or creating a new one
                storageRef.getDownloadUrl().addOnSuccessListener { existingUri ->
                    // If the path exists, return the existing URL
                    updatedData["photoUrl"] = existingUri.toString()
                    saveProfileToDatabase(userRef, updatedData, password)
                }.addOnFailureListener { exception ->
                    // If the path doesn't exist, proceed with upload
                    storageRef.putFile(it).addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            updatedData["photoUrl"] = uri.toString()
                            saveProfileToDatabase(userRef, updatedData, password)
                        }
                    }.addOnFailureListener {
                        Snackbar.make(requireView(), "Image upload failed: ${it.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } ?: saveProfileToDatabase(userRef, updatedData, password) // If no image selected, save without image
        } else {
            Snackbar.make(requireView(), "User not authenticated", Snackbar.LENGTH_LONG).show()
        }
    }


    private fun saveProfileToDatabase(userRef: DatabaseReference, updatedData: Map<String, Any>, password: String?) {
        userRef.updateChildren(updatedData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (password != null) {
                    updatePassword(password)
                } else {
                    Snackbar.make(requireView(), "Profile updated successfully", Snackbar.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_editProfile_to_profile)
                }
            } else {
                // More detailed error handling
                val errorMessage = task.exception?.message ?: "Unknown error"
                Snackbar.make(requireView(), "Error updating profile: $errorMessage", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updatePassword(password: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.updatePassword(password)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Snackbar.make(requireView(), "Password updated successfully", Snackbar.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_editProfile_to_profile)
            } else {
                Snackbar.make(requireView(), "Error updating password: ${task.exception?.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
