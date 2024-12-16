package com.dabataxi.Auth.Customer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.dabataxi.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeAppCST : Fragment(R.layout.fragment_home_app_cst) {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var viewPager: ViewPager2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPager)
        bottomNavigation = view.findViewById(R.id.bottomNavigation)

        setupViewPager()
        setupBottomNavigation()

        // Set default selected item in bottom navigation
        bottomNavigation.selectedItemId = R.id.nav_map
    }

    private fun setupViewPager() {
        val adapter = HomePagerAdapter(this)
        viewPager.adapter = adapter

        // Synchronize bottom navigation with ViewPager2 swipe
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Update the selected item based on the page selected
                when (position) {
                    0 -> bottomNavigation.selectedItemId = R.id.nav_map
                    1 -> bottomNavigation.selectedItemId = R.id.nav_history
                    2 -> bottomNavigation.selectedItemId = R.id.nav_profile
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    viewPager.currentItem = 0 // Navigate to the Map page
                    true
                }
                R.id.nav_history -> {
                    viewPager.currentItem = 1 // Navigate to the History page
                    true
                }
                R.id.nav_profile -> {
                    viewPager.currentItem = 2 // Navigate to the Profile page
                    true
                }
                else -> false
            }
        }
    }
}
