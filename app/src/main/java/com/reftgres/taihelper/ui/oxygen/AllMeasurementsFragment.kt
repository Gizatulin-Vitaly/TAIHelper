package com.reftgres.taihelper.ui.oxygen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.AllMeasurementsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllMeasurementsFragment : Fragment() {

    private val TAG = "AllMeasurementsFragment"
    private val viewModel: OxygenMeasurementViewModel by viewModels()
    private var _binding: AllMeasurementsFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var allMeasurementsAdapter: AllMeasurementsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView вызван")
        _binding = AllMeasurementsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val blockId = arguments?.getInt("blockId") ?: -1
        Log.d(TAG, "🧩 Получен blockId = $blockId из аргументов")

        if (blockId != -1) {
            viewModel.loadLatestMeasurements(blockId) // Загружаем измерения только выбранного блока
        } else {
            viewModel.loadLastTenMeasurements() // Фолбэк, если не передали blockId
        }
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.GONE


        setupRecyclerView()
        setupObservers()

        // Загружаем последние 10 измерений
        viewModel.loadLastTenMeasurements()
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Настройка RecyclerView")
        allMeasurementsAdapter = AllMeasurementsAdapter()
        binding.recyclerViewSecond.apply {
            adapter = allMeasurementsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        Log.d(TAG, "Настройка наблюдателей")

        // Наблюдение за списком всех измерений
        viewModel.latestMeasurements.observe(viewLifecycleOwner) { measurements ->
            Log.d(TAG, "📥 Получено ${measurements.size} измерений для текущего блока")
            allMeasurementsAdapter.submitList(measurements)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
        _binding = null
    }
}