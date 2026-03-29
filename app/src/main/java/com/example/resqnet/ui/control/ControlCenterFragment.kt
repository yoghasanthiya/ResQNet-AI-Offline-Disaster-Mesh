package com.example.resqnet.ui.control

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.resqnet.R
import com.example.resqnet.databinding.FragmentControlCenterBinding

class ControlCenterFragment : Fragment() {
    private companion object {
        private const val TAG = "ControlCenterFragment"
    }

    private var _binding: FragmentControlCenterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ControlCenterViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentControlCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: binding initialized for quick actions")
        viewModel.statusLabel.observe(viewLifecycleOwner) { status ->
            binding.networkStatusValue.text = if (status.contains("Connected") || status.contains("Active")) {
                getString(R.string.dashboard_status_active)
            } else {
                getString(R.string.dashboard_status_offline)
            }
        }
        viewModel.relayMetrics.observe(viewLifecycleOwner) { (relayed, peers, latency) ->
            binding.peersValue.text = peers.toString()
            binding.relayedValue.text = relayed.toString()
            binding.latencyValue.text = "${latency}ms"
        }
        viewModel.recentActivity.observe(viewLifecycleOwner) { events ->
            binding.activityLine1.text = events.getOrElse(0) { "" }
            binding.activityLine2.text = events.getOrElse(1) { "" }
            binding.activityLine3.text = events.getOrElse(2) { "" }
            binding.activityLine4.text = events.getOrElse(3) { "" }
        }
        configureQuickActionButton(
            button = binding.emergencyAlarmButton,
            label = getString(R.string.action_emergency_alarm)
        ) {
            Toast.makeText(requireContext(), "Emergency Alarm tapped", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Emergency Alarm button click detected")
            viewModel.triggerEmergencyAlarm()
        }
        configureQuickActionButton(
            button = binding.shareLocationButton,
            label = getString(R.string.action_share_location)
        ) {
            Toast.makeText(requireContext(), "Share Location tapped", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Share Location button click detected")
            viewModel.triggerShareLocation()
        }
        configureQuickActionButton(
            button = binding.statusUpdateButton,
            label = getString(R.string.action_status_update)
        ) {
            Toast.makeText(requireContext(), "Status Update tapped", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Status Update button click detected")
            viewModel.triggerStatusUpdate()
        }
        viewModel.actionFeedback.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrBlank()) return@observe
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            viewModel.consumeActionFeedback()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configureQuickActionButton(
        button: View,
        label: String,
        onClick: () -> Unit
    ) {
        button.isEnabled = true
        button.isClickable = true
        button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "$label touch down detected")
            }
            false
        }
        button.setOnClickListener { onClick() }
    }
}
