package com.example.resqnet.ui.volunteer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.resqnet.data.model.RescueTask
import com.example.resqnet.data.model.TaskStatus
import com.example.resqnet.databinding.ItemTaskBinding

class TaskAdapter(
    private val onPrimaryAction: (RescueTask) -> Unit,
    private val actionLabel: (RescueTask) -> String
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private val items = mutableListOf<RescueTask>()

    fun submitList(values: List<RescueTask>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding, onPrimaryAction, actionLabel)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class TaskViewHolder(
        private val binding: ItemTaskBinding,
        private val onPrimaryAction: (RescueTask) -> Unit,
        private val actionLabel: (RescueTask) -> String
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RescueTask) {
            binding.taskTitle.text = "${item.requestType.name} | ${item.requesterName}"
            binding.taskDetail.text = buildString {
                append("Priority ${item.priorityScore}")
                item.requiredSkill?.let { append(" | $it") }
                item.locationLabel?.let { append(" | $it") }
                item.assignedVolunteerName?.let { append(" | Assigned to $it") }
            }
            binding.taskStatus.text = item.status.name
            binding.taskAction.text = actionLabel(item)
            binding.taskAction.isEnabled = item.status != TaskStatus.COMPLETED
            binding.taskAction.setOnClickListener { onPrimaryAction(item) }
        }
    }
}
