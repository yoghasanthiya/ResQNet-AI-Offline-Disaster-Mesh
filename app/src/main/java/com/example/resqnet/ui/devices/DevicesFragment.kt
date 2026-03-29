package com.example.resqnet.ui.devices

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resqnet.databinding.FragmentDevicesBinding
import com.example.resqnet.util.LocationServicesHelper
import com.example.resqnet.util.NearbyPermissionHelper
import com.example.resqnet.ui.volunteers.VolunteerAdapter
import com.example.resqnet.ui.volunteers.VolunteersViewModel

class DevicesFragment : Fragment() {
    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VolunteersViewModel by viewModels()
    private val adapter = VolunteerAdapter { peer -> viewModel.connect(peer.address) }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            continueDiscoveryFlow()
        } else {
            viewModel.reportStatus("Bluetooth and location permissions are required for nearby mesh discovery.")
        }
    }
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.isBluetoothEnabled()) {
            continueDiscoveryFlow()
        }
    }
    private val locationEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isLocationRequiredForDiscovery().not() || LocationServicesHelper.isLocationEnabled(requireContext())) {
            continueDiscoveryFlow()
        } else {
            viewModel.reportStatus("Location is still off. Android Bluetooth discovery needs location enabled on Android 10 and 11.")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.deviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.deviceList.adapter = adapter
        binding.scanButton.setOnClickListener { ensureNearbyDiscoveryReady() }
        viewModel.meshStatus.observe(viewLifecycleOwner) { binding.connectionStatus.text = it }
        viewModel.peers.observe(viewLifecycleOwner) { peers ->
            adapter.submitList(peers)
            binding.emptyState.visibility = if (peers.isEmpty()) View.VISIBLE else View.GONE
        }
        ensureNearbyDiscoveryReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun ensureNearbyDiscoveryReady() {
        val missingPermissions = NearbyPermissionHelper.missingPermissions(requireContext())
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            return
        }
        continueDiscoveryFlow()
    }

    private fun continueDiscoveryFlow() {
        if (!viewModel.isBluetoothSupported()) {
            viewModel.reportStatus("Bluetooth unsupported on this device")
            return
        }
        if (!viewModel.isBluetoothEnabled()) {
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        if (isLocationRequiredForDiscovery() && !LocationServicesHelper.isLocationEnabled(requireContext())) {
            viewModel.reportStatus("Enable location services. Android Bluetooth discovery needs location on Android 10 and 11.")
            locationEnableLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        viewModel.beginDiscovery()
    }

    private fun isLocationRequiredForDiscovery(): Boolean {
        return android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R
    }
}
