package com.example.resqnet.ui.rescue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.resqnet.R
import com.example.resqnet.data.model.AlertPresentation
import com.example.resqnet.databinding.ItemAlertBinding
import java.text.DateFormat
import java.util.Date

class AlertAdapter : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {
    private val items = mutableListOf<AlertPresentation>()

    fun submitList(values: List<AlertPresentation>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AlertViewHolder(
        private val binding: ItemAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AlertPresentation) {
            binding.alertType.text = item.type
            binding.alertLocation.text = item.location
            binding.alertSummary.text = item.summary
            binding.alertTime.text = DateFormat.getDateTimeInstance().format(Date(item.timestamp))
            binding.alertSeverity.text = item.severity

            val accent = when (item.severity) {
                "Severe" -> R.color.priority_sos
                "High" -> R.color.priority_sos
                "Moderate" -> R.color.alert_warning
                else -> R.color.accent_cyan
            }
            binding.alertSeverity.setTextColor(ContextCompat.getColor(binding.root.context, accent))
            binding.alertCard.strokeColor = ContextCompat.getColor(binding.root.context, accent)
        }
    }
}
