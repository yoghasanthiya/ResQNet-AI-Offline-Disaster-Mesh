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

class VolunteerRequestsFragment : Fragment() {
    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VolunteerRequestsViewModel by viewModels()
    private val adapter = TaskAdapter(
        onPrimaryAction = { viewModel.accept(it.id) },
        actionLabel = { if (it.status == TaskStatus.OPEN) "Accept Task" else "Already Claimed" }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.taskList.layoutManager = LinearLayoutManager(requireContext())
        binding.taskList.adapter = adapter
        binding.taskHeader.text = "Nearby emergency requests"
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks.filter { it.status == TaskStatus.OPEN })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
