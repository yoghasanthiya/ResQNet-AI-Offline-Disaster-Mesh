package com.example.resqnet.ui.volunteers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.resqnet.R
import com.example.resqnet.data.model.DiscoveredPeer
import com.example.resqnet.databinding.ItemVolunteerBinding
import java.text.DateFormat
import java.util.Date

class VolunteerAdapter(
    private val onConnectClick: (DiscoveredPeer) -> Unit
) : RecyclerView.Adapter<VolunteerAdapter.VolunteerViewHolder>() {
    private val items = mutableListOf<DiscoveredPeer>()

    fun submitList(values: List<DiscoveredPeer>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VolunteerViewHolder {
        val binding = ItemVolunteerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VolunteerViewHolder(binding, onConnectClick)
    }

    override fun onBindViewHolder(holder: VolunteerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VolunteerViewHolder(
        private val binding: ItemVolunteerBinding,
        private val onConnectClick: (DiscoveredPeer) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DiscoveredPeer) {
            binding.volunteerName.text = item.name
            binding.volunteerAddress.text = item.address
            binding.volunteerStatus.text = when {
                item.isConnected -> binding.root.context.getString(R.string.connection_connected)
                item.isConnecting -> binding.root.context.getString(R.string.connection_connecting)
                else -> binding.root.context.getString(R.string.connection_available)
            }
            binding.volunteerSignal.text = item.rssi?.let { rssi ->
                binding.root.context.getString(R.string.signal_strength_format, rssi)
            } ?: binding.root.context.getString(R.string.signal_strength_unavailable)
            binding.volunteerSeen.text = DateFormat.getDateTimeInstance().format(Date(item.lastSeenAt))
            binding.connectButton.text = when {
                item.isConnected -> binding.root.context.getString(R.string.connected_label)
                item.isConnecting -> binding.root.context.getString(R.string.connecting_label)
                else -> binding.root.context.getString(R.string.connect_label)
            }
            binding.connectButton.isEnabled = !item.isConnected && !item.isConnecting
            binding.connectButton.setOnClickListener { onConnectClick(item) }
        }
    }
}
