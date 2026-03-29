package com.example.resqnet.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resqnet.R
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.databinding.FragmentMessagesBinding

class MessagesFragment : Fragment() {
    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MessagesViewModel by viewModels()
    private val adapter = MessageAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val typeLabels = MessageType.entries.map(::labelForType)
        binding.messageTypePicker.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, typeLabels)
        )
        binding.messageTypePicker.setText(typeLabels.first(), false)

        binding.messageList.layoutManager = LinearLayoutManager(requireContext())
        binding.messageList.adapter = adapter

        binding.sendButton.setOnClickListener {
            val selectedType = typeFromLabel(binding.messageTypePicker.text?.toString().orEmpty())
            val content = binding.messageInput.text?.toString().orEmpty().trim()
            if (content.isBlank()) return@setOnClickListener
            viewModel.sendMessage(type = selectedType, content = content)
            binding.messageInput.text?.clear()
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            binding.emptyState.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            if (messages.isNotEmpty()) {
                binding.messageList.scrollToPosition(0)
            }
        }

        viewModel.meshStatus.observe(viewLifecycleOwner) { binding.meshStatus.text = it }
        viewModel.liveLocation.observe(viewLifecycleOwner) { location ->
            binding.locationStatus.text = location?.let {
                getString(R.string.location_attached_format, it.latitude, it.longitude)
            } ?: getString(R.string.location_pending)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun labelForType(type: MessageType): String {
        return when (type) {
            MessageType.NORMAL -> getString(R.string.message_type_normal)
            MessageType.SOS -> getString(R.string.message_type_sos)
            MessageType.MEDICAL -> getString(R.string.message_type_medical)
            MessageType.RESOURCE -> getString(R.string.message_type_resource)
        }
    }

    private fun typeFromLabel(label: String): MessageType {
        return when (label) {
            getString(R.string.message_type_sos) -> MessageType.SOS
            getString(R.string.message_type_medical) -> MessageType.MEDICAL
            getString(R.string.message_type_resource) -> MessageType.RESOURCE
            else -> MessageType.NORMAL
        }
    }
}
