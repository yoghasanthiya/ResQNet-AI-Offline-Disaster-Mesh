package com.example.resqnet.ui.resources

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.resqnet.data.model.HelpCenter
import com.example.resqnet.databinding.ItemResourceBinding

class ResourceAdapter : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {
    private val items = mutableListOf<Pair<HelpCenter, String>>()

    fun submitList(values: List<Pair<HelpCenter, String>>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val binding = ItemResourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        holder.bind(items[position].first, items[position].second)
    }

    override fun getItemCount(): Int = items.size

    class ResourceViewHolder(
        private val binding: ItemResourceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HelpCenter, distanceLabel: String) {
            binding.resourceTitle.text = item.name
            binding.resourceCount.text = item.type
            binding.resourceMeta.text = "$distanceLabel • ${item.contact}"
            binding.resourceNotes.text = item.notes
        }
    }
}
