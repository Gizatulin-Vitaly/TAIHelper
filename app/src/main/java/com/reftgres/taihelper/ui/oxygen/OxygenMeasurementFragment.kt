package com.reftgres.taihelper.ui.oxygen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.reftgres.taihelper.databinding.FragmentOxigenBinding
import dagger.hilt.android.AndroidEntryPoint
import com.reftgres.taihelper.R

@AndroidEntryPoint
class OxygenMeasurementFragment : Fragment() {

    private val viewModel: OxygenMeasurementViewModel by viewModels()
    private var _binding: FragmentOxigenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("OxygenMeasurementFragment", "onCreateView called")
        _binding = FragmentOxigenBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.blockSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val blocks = viewModel.blocks.value
                Log.d("OxygenMeasurementFragment", "Block selected at position: $position, blocks: ${blocks?.size}")
                if (blocks != null && position >= 0 && position < blocks.size) {
                    val blockId = blocks[position].id
                    Log.d("OxygenMeasurementFragment", "Loading sensors for blockId: $blockId")
                    viewModel.loadSensorsForBlock(blockId)
                } else {
                    Log.e("OxygenMeasurementFragment", "Invalid position or blocks list is null")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("OxygenMeasurementFragment", "No block selected")
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }


    private fun setupObservers() {
        // Наблюдение за списком блоков
        viewModel.blocks.observe(viewLifecycleOwner) { blocks ->
            Log.d("OxygenMeasurementFragment", "Blocks loaded: $blocks")
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                blocks.map { it.name }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.blockSpinner.adapter = adapter
        }

        // Наблюдение за списком датчиков для выбранного блока
        viewModel.sensors.observe(viewLifecycleOwner) { sensors ->
            Log.d("OxygenMeasurementFragment", "Sensors observed: ${sensors.size}")
            val sensorNames = sensors.map { it.position }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sensorNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.sensorSpinner.adapter = adapter
        }

        val networkStatusBar = view?.findViewById<View>(R.id.networkStatusBar)
        viewModel.isOnline.observe(viewLifecycleOwner) { isOnline ->
            networkStatusBar?.visibility = if (isOnline) View.GONE else View.VISIBLE
        }

        // Наблюдение за выбранным датчиком
        viewModel.selectedSensor.observe(viewLifecycleOwner) { sensor ->
            sensor?.let {
                // Обновление UI с информацией о датчике
                binding.tvPositionSensor.text= it.position
                binding.tvNumberSensor.text = it.serialNumber.ifEmpty { "Не указан" }
                binding.tvMiddleSensor.text = it.midPoint
                binding.tvAnalogOutput.text = it.outputScale
            }
        }
    }

    private fun setupListeners() {
        // Слушатель выбора блока
        binding.blockSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d("OxygenMeasurementFragment", "Block selected at position: $position")
                val blockId = viewModel.blocks.value?.get(position)?.id ?: return
                viewModel.loadSensorsForBlock(blockId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.sensorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                Log.d("OxygenMeasurementFragment", "Sensor selected at position: $position")
                val sensors = viewModel.sensors.value ?: return
                if (position >= 0 && position < sensors.size) {
                    val sensorPosition = sensors[position].position
                    viewModel.selectSensorByPosition(sensorPosition)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.cardAllOxygen.setOnClickListener {
            Log.d("OxygenMeasurementFragment", "MaterialCard clicked")
            handleSensorCardClick()
        }
    }

    private fun handleSensorCardClick() {
        try {
            Log.d("OxygenMeasurementFragment", "Navigating to all measurements")
            findNavController().navigate(R.id.action_oxygenMeasurementFragment_to_all_measurements)
        } catch (e: Exception) {
            Log.e("OxygenMeasurementFragment", "Error navigating to measurements", e)
            Toast.makeText(requireContext(), "Ошибка при переходе: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}