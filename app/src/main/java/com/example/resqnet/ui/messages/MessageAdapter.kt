package com.example.resqnet.ui.messages

import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.resqnet.R
import com.example.resqnet.data.model.EmergencyMessage
import com.example.resqnet.data.model.MessageType
import com.example.resqnet.databinding.ItemMessageBinding
import java.text.DateFormat
import java.util.Date

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val items = mutableListOf<EmergencyMessage>()

    fun submitList(values: List<EmergencyMessage>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EmergencyMessage) {
            binding.messageSender.text = "${item.senderName} | ${item.senderPhone}"
            binding.messageTime.text = DateFormat.getDateTimeInstance().format(Date(item.createdAt))
            binding.messageType.text = item.type.name
            binding.messageContent.text = if (item.latitude != null && item.longitude != null) {
                binding.root.context.getString(
                    R.string.message_content_with_location,
                    item.content,
                    item.latitude,
                    item.longitude
                )
            } else {
                item.content
            }
            binding.messageMeta.text = buildString {
                append("Priority ${item.priorityScore} | ${item.deliveryStatus.name}")
                if (item.hopCount > 0) {
                    append(" | ${item.hopCount} hops")
                }
            }

            val colorRes = when (item.type) {
                MessageType.SOS -> R.color.priority_sos
                MessageType.MEDICAL -> R.color.priority_medical
                MessageType.RESOURCE -> R.color.priority_resource
                MessageType.NORMAL -> R.color.priority_normal
            }

            val containerParams = binding.messageCard.layoutParams as FrameLayout.LayoutParams
            containerParams.gravity = if (item.isLocalAuthor) Gravity.END else Gravity.START
            binding.messageCard.layoutParams = containerParams

            binding.messageCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (item.isLocalAuthor) R.color.surface_card_alt else R.color.surface_card
                )
            )
            binding.messageCard.setStrokeColor(ContextCompat.getColor(binding.root.context, colorRes))
            binding.messageType.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
        }
    }
}
