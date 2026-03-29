package com.example.resqnet

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.example.resqnet.service.MeshForegroundService
import com.example.resqnet.databinding.ActivityMainBinding
import com.example.resqnet.ui.dashboard.DashboardFragment
import com.example.resqnet.ui.registration.RegistrationFragment
import com.example.resqnet.util.NearbyPermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val locationDenied = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { permission ->
            result.containsKey(permission) && result[permission] != true
        }
        if (locationDenied) {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
        } else {
            startLiveLocationTrackingIfPermitted()
        }
        if (viewModel.hasRegisteredUser.value == true) {
            ensureBluetoothEnabled()
            MeshForegroundService.start(this)
        }
    }
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.hasRegisteredUser.value == true) {
            MeshForegroundService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(binding.mainContainer.id, RegistrationFragment())
            }
        }

        viewModel.hasRegisteredUser.observe(this) { hasProfile ->
            if (hasProfile == null) return@observe

            val fragment = if (hasProfile) DashboardFragment() else RegistrationFragment()
            supportFragmentManager.commit {
                replace(binding.mainContainer.id, fragment)
            }

            if (hasProfile) {
                requestRuntimePermissions()
                startLiveLocationTrackingIfPermitted()
                ensureBluetoothEnabled()
                MeshForegroundService.start(this)
            } else {
                MeshForegroundService.stop(this)
                (application as ResQNetApp).meshCoordinator.stop()
                (application as ResQNetApp).liveLocationTracker.stop()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.hasRegisteredUser.value == true) {
            requestRuntimePermissions()
            startLiveLocationTrackingIfPermitted()
            ensureBluetoothEnabled()
            MeshForegroundService.start(this)
            MeshForegroundService.notifyForeground(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.hasRegisteredUser.value == true) {
            MeshForegroundService.notifyBackground(this)
        }
    }

    private fun requestRuntimePermissions() {
        val missing = NearbyPermissionHelper.requiredPermissions().filterNot { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startLiveLocationTrackingIfPermitted() {
        val app = application as ResQNetApp
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return

        app.liveLocationTracker.start()
        app.liveLocationTracker.requestFreshLocation()
    }

    private fun ensureBluetoothEnabled() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        if (!adapter.isEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}
