package com.example.resqnet.ui.dashboard

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.resqnet.ui.control.ControlCenterFragment
import com.example.resqnet.ui.devices.DevicesFragment
import com.example.resqnet.ui.emergency.EmergencyFragment
import com.example.resqnet.ui.messages.MessagesFragment
import com.example.resqnet.ui.operations.OperationsMapFragment
import com.example.resqnet.ui.rescue.RescueOverviewFragment

class DashboardPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ControlCenterFragment()
            1 -> RescueOverviewFragment()
            2 -> MessagesFragment()
            3 -> OperationsMapFragment()
            4 -> DevicesFragment()
            else -> EmergencyFragment()
        }
    }

    companion object {
        val tabs = listOf("Overview", "Alerts", "Messages", "Map", "Nearby", "Response")
    }
}
