package com.example.resqnet.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.resqnet.R
import com.example.resqnet.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.protocolInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf(getString(R.string.protocol_bluetooth), getString(R.string.protocol_wifi_direct))
            )
        )
        binding.protocolInput.setText(getString(R.string.protocol_bluetooth), false)

        binding.powerModeInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listOf(getString(R.string.power_balanced), getString(R.string.power_high))
            )
        )
        binding.powerModeInput.setText(getString(R.string.power_balanced), false)

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            if (profile == null) return@observe
            binding.profileName.setText(profile.name)
            binding.profilePhone.setText(profile.phoneNumber)
            binding.profileRole.setText(profile.role.name, false)
        }

        viewModel.stats.observe(viewLifecycleOwner) { (sent, received, avg) ->
            binding.messagesSentValue.text = sent.toString()
            binding.messagesReceivedValue.text = received.toString()
            binding.avgResponseValue.text = "${avg}ms"
        }

        viewModel.uptime.observe(viewLifecycleOwner) { uptime ->
            binding.uptimeValue.text = "${uptime / 1000}s"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
