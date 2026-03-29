package com.example.resqnet.ui.rescue

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resqnet.data.model.TaskStatus
import com.example.resqnet.databinding.FragmentTaskListBinding
import com.example.resqnet.ui.volunteer.TaskAdapter

class RescueTaskManagementFragment : Fragment() {
    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RescueTaskManagementViewModel by viewModels()
    private val adapter = TaskAdapter(
        onPrimaryAction = { task ->
            if (task.assignedVolunteerName.isNullOrBlank()) {
                showAssignDialog(task.id)
            } else {
                val next = when (task.status) {
                    TaskStatus.OPEN -> TaskStatus.ACCEPTED
                    TaskStatus.ACCEPTED -> TaskStatus.IN_PROGRESS
                    TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                    TaskStatus.COMPLETED -> TaskStatus.COMPLETED
                }
                viewModel.updateStatus(task.id, next)
            }
        },
        actionLabel = {
            if (it.assignedVolunteerName.isNullOrBlank()) "Assign Volunteer" else "Advance Status"
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.taskHeader.text = "Volunteer assignments and task status"
        binding.taskList.layoutManager = LinearLayoutManager(requireContext())
        binding.taskList.adapter = adapter
        viewModel.tasks.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showAssignDialog(taskId: String) {
        val input = EditText(requireContext()).apply {
            hint = "Volunteer name"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Assign task")
            .setView(input)
            .setPositiveButton("Assign") { _, _ ->
                val volunteerName = input.text?.toString()?.trim().orEmpty()
                if (volunteerName.isNotBlank()) {
                    viewModel.assign(taskId, volunteerName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
