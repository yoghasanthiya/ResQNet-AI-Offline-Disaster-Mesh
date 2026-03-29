package com.example.resqnet.ui.shelters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.resqnet.data.model.ShelterPresentation
import com.example.resqnet.databinding.ItemShelterBinding

class ShelterAdapter : RecyclerView.Adapter<ShelterAdapter.ShelterViewHolder>() {
    private val items = mutableListOf<ShelterPresentation>()

    fun submitList(values: List<ShelterPresentation>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShelterViewHolder {
        val binding = ItemShelterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShelterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShelterViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ShelterViewHolder(
        private val binding: ItemShelterBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShelterPresentation) {
            binding.shelterName.text = item.shelter.name
            binding.shelterNotes.text = item.shelter.notes
            binding.shelterDistance.text = item.distanceKm?.let { "${"%.2f".format(it)} km away" }
                ?: "Distance unavailable"
        }
    }
}
