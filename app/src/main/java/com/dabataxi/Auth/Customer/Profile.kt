package com.dabataxi.Auth.Customer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.dabataxi.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar

class Profile : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Initialize the ViewModel using the viewModels delegate
    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // Observe LiveData from ViewModel
        observeViewModel()

        // Load user profile
        viewModel.loadUserProfile()

        // Edit Profile Button logic
        binding.btnEditProfile.setOnClickListener {
           findNavController().navigate(R.id.editProfile)
            /*// Make sure the current destination matches the actual ID in nav_graph.xml
            if (navController.currentDestination?.id == R.id.profile) {
                navController.navigate(R.id.editProfile)
            } else {
                Snackbar.make(view, getString(R.string.unable_to_navigate_edit_profile), Snackbar.LENGTH_SHORT).show()
            }*/
        }

        // Logout Button logic
        binding.btnLogout.setOnClickListener {
            // Sign out the user via ViewModel
            viewModel.signOut()
            findNavController().navigate(R.id.loginHomeCST)
        }
    }

    private fun observeViewModel() {
        // Observe userProfile LiveData
        viewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            if (userProfile != null) {
                binding.userName.text = userProfile.name
                binding.userEmail.text = userProfile.email
                binding.userPhone.text = userProfile.phone

                // Load profile image using Glide, if available
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

        // Observe statusMessage LiveData
        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
            }
        }

        // Observe isLoading LiveData
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
