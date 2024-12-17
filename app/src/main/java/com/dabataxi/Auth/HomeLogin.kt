package com.dabataxi.Auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.dabataxi.R

class HomeLogin : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCustomer: Button = view.findViewById(R.id.btnCustomer)
        val btnDriver: Button = view.findViewById(R.id.btnDriver)

        btnCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_customer)
        }

        btnDriver.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_driver)
        }
    }
}
