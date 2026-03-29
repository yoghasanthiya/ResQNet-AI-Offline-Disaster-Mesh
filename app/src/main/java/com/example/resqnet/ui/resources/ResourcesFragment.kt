package com.example.resqnet.ui.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resqnet.databinding.FragmentResourcesBinding

class ResourcesFragment : Fragment() {
    private var _binding: FragmentResourcesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ResourcesViewModel by viewModels()
    private val adapter = ResourceAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.resourceList.layoutManager = LinearLayoutManager(requireContext())
        binding.resourceList.adapter = adapter
        viewModel.resourceSummaries.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            binding.resourcesLocation.text = location?.let {
                getString(com.example.resqnet.R.string.location_attached_format, it.latitude, it.longitude)
            } ?: getString(com.example.resqnet.R.string.resources_location_unavailable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
