package com.example.resqnet.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.resqnet.databinding.FragmentDashboardBinding
import com.google.android.material.tabs.TabLayoutMediator

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.userHeader.observe(viewLifecycleOwner) { binding.dashboardUser.text = it }
        viewModel.meshStatus.observe(viewLifecycleOwner) { binding.dashboardStatus.text = it }
        setupTabs()
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        super.onDestroyView()
        _binding = null
    }

    private fun setupTabs() {
        binding.dashboardPager.adapter = DashboardPagerAdapter(this)
        tabMediator?.detach()
        tabMediator = TabLayoutMediator(binding.dashboardTabs, binding.dashboardPager) { tab, position ->
            tab.text = DashboardPagerAdapter.tabs[position]
        }.also { it.attach() }
    }
}
