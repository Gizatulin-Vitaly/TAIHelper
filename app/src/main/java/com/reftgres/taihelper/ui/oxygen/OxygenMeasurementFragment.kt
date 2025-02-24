package com.reftgres.taihelper.ui.oxygen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.reftgres.taihelper.databinding.FragmentOxigenBinding
import com.reftgres.taihelper.R


class OxygenMeasurementFragment : Fragment() {

    private var _binding: FragmentOxigenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOxigenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.allOxygenCard.setOnClickListener {
            findNavController().navigate(R.id.action_oxygenMeasurementFragment_to_all_measurements)
        }
        val data = listOf("Значение 1", "Значение 2", "Значение 3", "Значение 4", "Значение 5")
        val adapter = FiveAdapter(data)
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}