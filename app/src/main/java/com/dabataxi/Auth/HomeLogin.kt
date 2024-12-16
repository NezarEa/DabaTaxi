package com.dabataxi.Auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.dabataxi.R
import com.google.firebase.auth.FirebaseAuth

class HomeLogin : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCustomer: Button = view.findViewById(R.id.btnCustomer)
        val btnDriver: Button = view.findViewById(R.id.btnDriver)

        // Set button click listeners
        btnCustomer.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                // Navigate to Customer Login screen, ensure it clears the back stack so the user cannot go back to Onboarding
                findNavController().navigate(R.id.action_login_to_customer, null, null,
                    null)
            }, 500)
        }

        btnDriver.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                // Navigate to Driver Login screen, similarly, ensuring no back navigation to Onboarding
                findNavController().navigate(R.id.action_login_to_driver, null, null,
                    null)
            }, 500)
        }
    }
}
