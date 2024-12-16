package com.dabataxi.Auth.Customer

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomePagerAdapter(fragmentActivity: HomeAppCST) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MapsTraffic()
            1 -> TrafficHistory() // Second fragment for History
            2 -> Profile() // Third fragment for Profile
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}