package com.example.resqnet.ui.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.resqnet.R
import com.example.resqnet.data.model.UserRole
import com.example.resqnet.data.model.VolunteerSkill
import com.example.resqnet.databinding.FragmentRegistrationBinding

class RegistrationFragment : Fragment() {
    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistrationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val roles = UserRole.entries.map(::labelForRole)
        binding.roleInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        )
        binding.roleInput.setText(roles.first(), false)
        updateVolunteerSkills(UserRole.CIVILIAN)

        binding.roleInput.setOnItemClickListener { _, _, position, _ ->
            updateVolunteerSkills(UserRole.entries[position])
        }

        binding.registerButton.setOnClickListener {
            val name = binding.nameInput.text?.toString()?.trim().orEmpty()
            val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()
            val location = binding.locationInput.text?.toString()?.trim().orEmpty()
            val role = roleFromLabel(binding.roleInput.text?.toString().orEmpty())
            val skills = selectedSkills()

            if (name.isBlank() || phone.isBlank() || location.isBlank()) {
                Toast.makeText(requireContext(), R.string.registration_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (role == UserRole.VOLUNTEER && skills.isEmpty()) {
                Toast.makeText(requireContext(), R.string.volunteer_skill_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveProfile(
                name = name,
                phone = phone,
                role = role,
                locationLabel = location,
                skills = skills
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun selectedSkills(): List<VolunteerSkill> {
        val selected = mutableListOf<VolunteerSkill>()
        if (binding.skillMedical.isChecked) selected += VolunteerSkill.MEDICAL
        if (binding.skillTransport.isChecked) selected += VolunteerSkill.TRANSPORT
        if (binding.skillFood.isChecked) selected += VolunteerSkill.FOOD_SUPPLY
        return selected
    }

    private fun updateVolunteerSkills(role: UserRole) {
        binding.skillGroup.visibility = if (role == UserRole.VOLUNTEER) View.VISIBLE else View.GONE
    }

    private fun labelForRole(role: UserRole): String {
        return when (role) {
            UserRole.CIVILIAN -> getString(R.string.role_civilian)
            UserRole.VOLUNTEER -> getString(R.string.role_volunteer)
            UserRole.RESCUE_TEAM -> getString(R.string.role_rescue_team)
        }
    }

    private fun roleFromLabel(label: String): UserRole {
        return when (label) {
            getString(R.string.role_volunteer) -> UserRole.VOLUNTEER
            getString(R.string.role_rescue_team) -> UserRole.RESCUE_TEAM
            else -> UserRole.CIVILIAN
        }
    }
}
