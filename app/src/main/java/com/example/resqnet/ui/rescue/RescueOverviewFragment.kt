package com.example.resqnet.ui.rescue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resqnet.databinding.FragmentRescueOverviewBinding
import com.google.android.material.snackbar.Snackbar

class RescueOverviewFragment : Fragment() {
    private var _binding: FragmentRescueOverviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RescueOverviewViewModel by viewModels()
    private val adapter = AlertAdapter()
    private var lastAlertIds = emptySet<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRescueOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.alertList.layoutManager = LinearLayoutManager(requireContext())
        binding.alertList.adapter = adapter
        viewModel.alerts.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.alertSubtitle.text = getString(
                com.example.resqnet.R.string.alerts_count_format,
                it.count { alert -> alert.severity == "Severe" || alert.severity == "High" },
                it.size
            )
            val newAlertIds = it.map { alert -> alert.id }.toSet()
            if (lastAlertIds.isNotEmpty() && newAlertIds.any { id -> id !in lastAlertIds }) {
                Snackbar.make(binding.root, com.example.resqnet.R.string.new_alert_notification, Snackbar.LENGTH_SHORT).show()
            }
            lastAlertIds = newAlertIds
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
