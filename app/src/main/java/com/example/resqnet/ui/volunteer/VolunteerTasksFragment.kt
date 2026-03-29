package com.example.resqnet.ui.volunteer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.resqnet.data.model.TaskStatus
import com.example.resqnet.databinding.FragmentTaskListBinding

class VolunteerTasksFragment : Fragment() {
    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VolunteerTasksViewModel by viewModels()
    private val adapter = TaskAdapter(
        onPrimaryAction = {
            val next = when (it.status) {
                TaskStatus.ACCEPTED -> TaskStatus.IN_PROGRESS
                TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                TaskStatus.OPEN -> TaskStatus.ACCEPTED
                TaskStatus.COMPLETED -> TaskStatus.COMPLETED
            }
            viewModel.updateStatus(it.id, next)
        },
        actionLabel = {
            when (it.status) {
                TaskStatus.ACCEPTED -> "Start Task"
                TaskStatus.IN_PROGRESS -> "Mark Complete"
                TaskStatus.OPEN -> "Accept"
                TaskStatus.COMPLETED -> "Completed"
            }
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.taskHeader.text = "My accepted rescue tasks"
        binding.taskList.layoutManager = LinearLayoutManager(requireContext())
        binding.taskList.adapter = adapter
        viewModel.tasks.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
