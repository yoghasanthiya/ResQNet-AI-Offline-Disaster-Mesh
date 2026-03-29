package com.example.resqnet.ui.emergency

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.R
import com.example.resqnet.databinding.FragmentEmergencyBinding

class EmergencyFragment : Fragment() {
    private companion object {
        private const val TAG = "EmergencyFragment"
    }

    private var _binding: FragmentEmergencyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmergencyViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmergencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.requestTypeInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf("Flood", "Fire", "Medical", "Collapsed Structure", "Evacuation")
            )
        )
        binding.requestTypeInput.setText("Flood", false)

        binding.sosButton.setOnClickListener {
            viewModel.sendEmergency(MessageType.SOS, "SOS signal triggered from control panel")
        }
        binding.medicalButton.setOnClickListener {
            viewModel.sendEmergency(MessageType.MEDICAL, "Medical assistance required")
        }
        binding.rescueButton.setOnClickListener {
            viewModel.sendEmergency(MessageType.RESOURCE, "Rescue assistance required")
        }
        binding.foodButton.setOnClickListener {
            viewModel.sendEmergency(MessageType.RESOURCE, "Food supplies needed")
        }
        binding.shelterButton.setOnClickListener {
            viewModel.sendEmergency(MessageType.RESOURCE, "Shelter support needed")
        }
        binding.waterButton.setOnClickListener {
            viewModel.sendEmergency(MessageType.RESOURCE, "Water supplies needed")
        }
        binding.submitRescueRequest.setOnClickListener {
            Log.d(TAG, "Rescue request submit button clicked")
            val name = binding.requestName.text?.toString().orEmpty().trim()
            val location = binding.requestLocation.text?.toString().orEmpty().trim()
            val people = binding.requestPeople.text?.toString().orEmpty().trim()
            val type = binding.requestTypeInput.text?.toString().orEmpty().trim()
            val description = binding.requestDescription.text?.toString().orEmpty().trim()

            binding.requestNameLayout.error = null
            binding.requestLocationLayout.error = null
            binding.requestPeopleLayout.error = null
            binding.requestDescriptionLayout.error = null

            var hasError = false
            if (name.isBlank()) {
                binding.requestNameLayout.error = "Name is required"
                hasError = true
            }
            if (location.isBlank()) {
                binding.requestLocationLayout.error = "Location / landmark is required"
                hasError = true
            }
            if (people.isBlank()) {
                binding.requestPeopleLayout.error = "Number of people is required"
                hasError = true
            }
            if (description.isBlank()) {
                binding.requestDescriptionLayout.error = "Message / description is required"
                hasError = true
            }
            if (hasError) {
                Log.w(TAG, "Rescue request validation failed")
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.submitRescueRequest(name, location, people, type, description)
        }
        viewModel.liveLocation.observe(viewLifecycleOwner) { location ->
            binding.liveLocationStatus.text = location?.let {
                getString(R.string.location_attached_format, it.latitude, it.longitude)
            } ?: getString(R.string.location_pending)
        }
        viewModel.requestSubmissionStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNullOrBlank()) return@observe
            Log.d(TAG, "Rescue request status update: $status")
            binding.requestStatus.text = if (status == "Request submitted successfully") {
                val name = binding.requestName.text?.toString().orEmpty().trim()
                val location = binding.requestLocation.text?.toString().orEmpty().trim()
                getString(R.string.rescue_request_submitted_format, name, location)
            } else {
                status
            }
            Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
            if (status == "Request submitted successfully") {
                binding.requestName.text?.clear()
                binding.requestLocation.text?.clear()
                binding.requestPeople.text?.clear()
                binding.requestDescription.text?.clear()
                binding.requestTypeInput.setText("Flood", false)
            }
            viewModel.consumeRequestSubmissionStatus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
