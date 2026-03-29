package com.example.resqnet.ui.shelters

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resqnet.R
import com.example.resqnet.data.model.ShelterPresentation
import com.example.resqnet.databinding.FragmentSheltersBinding
import com.example.resqnet.util.MapViewConfigurator
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class SheltersFragment : Fragment() {
    private var _binding: FragmentSheltersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SheltersViewModel by viewModels()
    private val adapter = ShelterAdapter()
    private var hasCenteredOnUserLocation = false
    private var mapInitialized = false
    private var tapMarker: Marker? = null
    private var shelters: List<ShelterPresentation> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSheltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.shelterList.layoutManager = LinearLayoutManager(requireContext())
        binding.shelterList.adapter = adapter
        runSafely {
            MapViewConfigurator.configure(binding.shelterMap, requireContext())
            binding.shelterMap.controller.setZoom(12.5)
            mapInitialized = true
        }

        binding.addShelterButton.setOnClickListener {
            if (hasLocationPermission()) {
                viewModel.refreshCurrentLocation()
            }
            viewModel.addCurrentLocationShelter()
        }
        binding.shareLocationButton.setOnClickListener {
            if (hasLocationPermission()) {
                viewModel.refreshCurrentLocation()
            }
            viewModel.shareCurrentLocationShelter()
        }

        binding.locationStatus.text = if (hasLocationPermission()) {
            getString(R.string.map_waiting_for_location)
        } else {
            getString(R.string.map_location_permission_required)
        }
        if (hasLocationPermission()) {
            viewModel.refreshCurrentLocation()
        }
        refreshMap(emptyList())

        viewModel.liveLocation.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                viewModel.updateLocation(location)
            }
            binding.locationStatus.text = location?.let {
                getString(R.string.location_attached_format, it.latitude, it.longitude)
            } ?: if (hasLocationPermission()) {
                getString(R.string.map_waiting_for_location)
            } else {
                getString(R.string.map_location_permission_required)
            }
        }
        viewModel.shelters.observe(viewLifecycleOwner) {
            shelters = it
            adapter.submitList(it)
            refreshMap(it)
        }
        viewModel.shareStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNullOrBlank()) return@observe
            Snackbar.make(binding.root, status, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        runSafely {
            binding.shelterMap.onResume()
            if (hasLocationPermission()) {
                viewModel.refreshCurrentLocation()
            }
        }
    }

    override fun onPause() {
        runSafely {
            binding.shelterMap.onPause()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tapMarker = null
        mapInitialized = false
        _binding = null
    }

    private fun refreshMap(items: List<ShelterPresentation>) {
        val mapView = _binding?.shelterMap ?: return
        if (!mapInitialized) return

        runSafely {
            mapView.overlays.clear()

            viewModel.currentLocation()?.let { location ->
                val marker = createMarker(
                    mapView = mapView,
                    position = GeoPoint(location.latitude, location.longitude),
                    title = getString(R.string.map_marker_your_location)
                )
                mapView.overlays.add(marker)
                if (!hasCenteredOnUserLocation) {
                    mapView.controller.animateTo(marker.position)
                    hasCenteredOnUserLocation = true
                } else {
                    mapView.controller.setCenter(marker.position)
                }
            }

            items.forEach { shelter ->
                mapView.overlays.add(
                    createMarker(
                        mapView = mapView,
                        position = GeoPoint(shelter.shelter.latitude, shelter.shelter.longitude),
                        title = if (shelter.shelter.name == "Current Location Shelter") {
                            "Shelter Available Here"
                        } else {
                            shelter.shelter.name
                        },
                        subDescription = shelter.shelter.notes
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
        val mapView = _binding?.shelterMap ?: return
        runSafely {
            tapMarker = createMarker(
                mapView = mapView,
                position = point,
                title = getString(R.string.map_selected_location_title),
                subDescription = getString(R.string.map_tap_location_format, point.latitude, point.longitude)
            )
            binding.locationStatus.text = getString(R.string.map_tap_location_format, point.latitude, point.longitude)
            refreshMap(shelters)
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
            Log.w("SheltersFragment", "Map interaction failed safely", exception)
        }
    }
}
