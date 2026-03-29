package com.example.resqnet.ui.operations

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.resqnet.R
import com.example.resqnet.data.model.EmergencyMessage
import com.example.resqnet.data.model.HelpCenter
import com.example.resqnet.databinding.FragmentOperationsMapBinding
import com.example.resqnet.util.MapViewConfigurator
import com.example.resqnet.util.SampleHelpCenters
import com.example.resqnet.util.SampleShelters
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class OperationsMapFragment : Fragment() {
    private var _binding: FragmentOperationsMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OperationsMapViewModel by viewModels()
    private var alerts: List<EmergencyMessage> = emptyList()
    private var currentLocation: Location? = null
    private var helpCenters: List<HelpCenter> = SampleHelpCenters.centers
    private var hasCenteredOnUserLocation = false
    private var mapInitialized = false
    private var tapMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperationsMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        runSafely {
            MapViewConfigurator.configure(binding.operationsMap, requireContext())
            binding.operationsMap.controller.setZoom(11.5)
            mapInitialized = true
        }
        binding.mapStatus.text = if (hasLocationPermission()) {
            getString(R.string.map_waiting_for_location)
        } else {
            getString(R.string.map_location_permission_required)
        }
        if (hasLocationPermission()) {
            viewModel.refreshCurrentLocation()
        }
        refreshMap()

        viewModel.alerts.observe(viewLifecycleOwner) {
            alerts = it
            refreshMap()
        }
        viewModel.currentLocation.observe(viewLifecycleOwner) {
            currentLocation = it
            binding.mapStatus.text = if (it != null) {
                getString(R.string.location_attached_format, it.latitude, it.longitude)
            } else if (hasLocationPermission()) {
                getString(R.string.map_waiting_for_location)
            } else {
                getString(R.string.map_location_permission_required)
            }
            refreshMap()
        }
        viewModel.helpCenters.observe(viewLifecycleOwner) {
            helpCenters = it
            refreshMap()
        }
    }

    override fun onResume() {
        super.onResume()
        runSafely {
            binding.operationsMap.onResume()
            if (hasLocationPermission()) {
                viewModel.refreshCurrentLocation()
            }
        }
    }

    override fun onPause() {
        runSafely {
            binding.operationsMap.onPause()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tapMarker = null
        mapInitialized = false
        _binding = null
    }

    private fun refreshMap() {
        val mapView = _binding?.operationsMap ?: return
        if (!mapInitialized) return

        runSafely {
            mapView.overlays.clear()

            currentLocation?.let { location ->
                val userPoint = GeoPoint(location.latitude, location.longitude)
                mapView.overlays.add(
                    createMarker(
                        mapView = mapView,
                        position = userPoint,
                        title = getString(R.string.map_marker_your_location),
                        subDescription = getString(R.string.map_marker_your_location)
                    )
                )
                if (!hasCenteredOnUserLocation) {
                    mapView.controller.animateTo(userPoint)
                    hasCenteredOnUserLocation = true
                } else {
                    mapView.controller.setCenter(userPoint)
                }
            }

            SampleShelters.shelters.forEach { shelter ->
                mapView.overlays.add(
                    createMarker(
                        mapView = mapView,
                        position = GeoPoint(shelter.latitude, shelter.longitude),
                        title = shelter.name,
                        subDescription = shelter.notes
                    )
                )
            }

            helpCenters.forEach { center ->
                mapView.overlays.add(
                    createMarker(
                        mapView = mapView,
                        position = GeoPoint(center.latitude, center.longitude),
                        title = "${center.type} | ${center.name}",
                        subDescription = center.notes
                    )
                )
            }

            alerts.filter { it.latitude != null && it.longitude != null }.forEach { alert ->
                val latitude = alert.latitude ?: return@forEach
                val longitude = alert.longitude ?: return@forEach
                mapView.overlays.add(
                    createMarker(
                        mapView = mapView,
                        position = GeoPoint(latitude, longitude),
                        title = "${alert.type.name} | ${alert.senderName}",
                        subDescription = alert.content
                    )
                )
            }

            tapMarker?.let(mapView.overlays::add)
            mapView.overlays.add(createMapTapOverlay(mapView))
            mapView.invalidate()
        }
    }

    private fun createMarker(
        mapView: MapView,
        position: GeoPoint,
        title: String,
        subDescription: String? = null
    ): Marker {
        return Marker(mapView).apply {
            this.position = position
            this.title = title
            this.subDescription = subDescription
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { marker, tappedMap ->
                runSafely {
                    marker.showInfoWindow()
                    tappedMap.controller.animateTo(marker.position)
                }
                true
            }
        }
    }

    private fun createMapTapOverlay(mapView: MapView): MapEventsOverlay {
        return MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    handleMapTap(point)
                    return true
                }

                override fun longPressHelper(point: GeoPoint): Boolean {
                    handleMapTap(point)
                    return true
                }
            }
        )
    }

    private fun handleMapTap(point: GeoPoint?) {
        if (point == null || !mapInitialized) return
        val mapView = _binding?.operationsMap ?: return
        runSafely {
            tapMarker = createMarker(
                mapView = mapView,
                position = point,
                title = getString(R.string.map_selected_location_title),
                subDescription = getString(R.string.map_tap_location_format, point.latitude, point.longitude)
            )
            binding.mapStatus.text = getString(R.string.map_tap_location_format, point.latitude, point.longitude)
            refreshMap()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun runSafely(action: () -> Unit) {
        try {
            action()
        } catch (exception: Exception) {
            Log.w("OperationsMapFragment", "Map interaction failed safely", exception)
        }
    }
}
