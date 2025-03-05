package com.reftgres.taihelper.ui.addsensor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.reftgres.taihelper.databinding.FragmentAddSensorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.reftgres.taihelper.R
import com.reftgres.taihelper.ui.sensors.DropdownAdapter

@AndroidEntryPoint
class AddSensorFragment : Fragment() {

    private var _binding: FragmentAddSensorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddSensorViewModel by viewModels()

    // Переменные для хранения выбранных значений из выпадающих списков
    private var selectedTypeId: String? = null
    private var selectedBlockId: String? = null
    private var selectedMeasurementStartId: String? = null
    private var selectedMeasurementEndId: String? = null
    private var selectedOutputRangeId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.GONE

        binding.actvSensorType.setOnClickListener {
            Log.d(TAG, "Клик по actvSensorType")
            if (binding.actvSensorType.adapter != null && binding.actvSensorType.adapter.count > 0) {
                binding.actvSensorType.showDropDown()
            } else {
                Log.e(TAG, "Адаптер не инициализирован или пуст")
            }
        }

        Log.d(TAG, "onViewCreated: Инициализация фрагмента")

        setupObservers()
        setupListeners()

        // Принудительная загрузка данных
        viewModel.loadAllData()
    }

    private fun setupObservers() {
        // Наблюдение за загрузкой
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Состояние загрузки: $isLoading")
            binding.progressBar.isVisible = isLoading
            binding.btnSaveSensor.isVisible = !isLoading
        }


        // Наблюдение за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Log.e(TAG, "Ошибка: $errorMessage")
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Наблюдение за успешным сохранением
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Log.d(TAG, "Датчик успешно сохранен")
                Toast.makeText(requireContext(), "Датчик успешно сохранен", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        // Наблюдение за типами датчиков
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.typeSensors.collect { types ->
                Log.d(TAG, "Получено ${types.size} типов датчиков")

                if (types.isNotEmpty()) {
                    Log.d(TAG, "Типы датчиков: ${types.map { it.name }}")

                    // Используем интерфейс DropdownItem
                    val dropdownItems = types.map { type ->
                        object : DropdownAdapter.DropdownItem {
                            override val id = type.id
                            override val name = type.name
                        }
                    }

                    setupDropdown(
                        binding.actvSensorType,
                        dropdownItems,
                        onItemSelected = { id ->
                            selectedTypeId = id
                            Log.d(TAG, "Выбран тип датчика: $selectedTypeId")
                        }
                    )
                } else {
                    Log.w(TAG, "Список типов датчиков пуст")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.outputRanges.collect { ranges ->
                Log.d(TAG, "Получено ${ranges.size} диапазонов выхода")

                if (ranges.isNotEmpty()) {
                    Log.d(TAG, "Диапазоны выхода: ${ranges.map { it.name }}")

                    // Преобразуем в DropdownItem
                    val dropdownItems = ranges.map { range ->
                        object : DropdownAdapter.DropdownItem {
                            override val id = range.id
                            override val name = range.name
                        }
                    }

                    setupDropdown(
                        binding.actvOutputScale,
                        dropdownItems,
                        onItemSelected = { id ->
                            selectedOutputRangeId = id
                            Log.d(TAG, "Выбран диапазон выхода: $selectedOutputRangeId")
                        }
                    )
                }
            }
        }

        // Наблюдение за блоками
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.blocks.collect { blocks ->
                Log.d(TAG, "Получено ${blocks.size} блоков")

                if (blocks.isNotEmpty()) {
                    Log.d(TAG, "Блоки: ${blocks.map { it.name }}")

                    // Преобразуем блоки в DropdownItem
                    val dropdownItems = blocks.map { block ->
                        object : DropdownAdapter.DropdownItem {
                            override val id = block.id
                            override val name = block.name
                        }
                    }

                    setupDropdown(
                        binding.actvBlock,
                        dropdownItems,
                        onItemSelected = { id ->
                            selectedBlockId = id
                            Log.d(TAG, "Выбран блок: $selectedBlockId")
                        }
                    )
                } else {
                    Log.w(TAG, "Список блоков пуст")
                }
            }
        }

// Наблюдение за началами измерений
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.measurementStarts.collect { measurements ->
                Log.d(TAG, "Получено ${measurements.size} начал измерений")

                if (measurements.isNotEmpty()) {
                    Log.d(TAG, "Начала измерений: ${measurements.map { it.name }}")

                    // Преобразуем измерения в DropdownItem
                    val dropdownItems = measurements.map { measurement ->
                        object : DropdownAdapter.DropdownItem {
                            override val id = measurement.id
                            override val name = measurement.name
                        }
                    }

                    setupDropdown(
                        binding.actvMeasurementStart,
                        dropdownItems,
                        onItemSelected = { id ->
                            selectedMeasurementStartId = id
                            Log.d(TAG, "Выбрано начало измерения: $selectedMeasurementStartId")
                        }
                    )
                } else {
                    Log.w(TAG, "Список начал измерений пуст")
                }
            }
        }



// Наблюдение за окончаниями измерений
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.measurementEnds.collect { measurements ->
                Log.d(TAG, "Получено ${measurements.size} окончаний измерений")

                if (measurements.isNotEmpty()) {
                    Log.d(TAG, "Окончания измерений: ${measurements.map { it.name }}")

                    // Преобразуем измерения в DropdownItem
                    val dropdownItems = measurements.map { measurement ->
                        object : DropdownAdapter.DropdownItem {
                            override val id = measurement.id
                            override val name = measurement.name
                        }
                    }

                    setupDropdown(
                        binding.actvMeasurementEnd,
                        dropdownItems,
                        onItemSelected = { id ->
                            selectedMeasurementEndId = id
                            Log.d(TAG, "Выбрано окончание измерения: $selectedMeasurementEndId")
                        }
                    )
                } else {
                    Log.w(TAG, "Список окончаний измерений пуст")
                }
            }
        }
    }

    // Вместо строк используем объекты
    private fun setupDropdown(
        autoCompleteTextView: AutoCompleteTextView,
        items: List<DropdownAdapter.DropdownItem>,
        onItemSelected: (String) -> Unit
    ) {
        val adapter = DropdownAdapter(requireContext(), items)
        autoCompleteTextView.setAdapter(adapter)

        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = items[position]
            // Устанавливаем текст явно
            autoCompleteTextView.setText(selectedItem.name, false)
            // Вызываем обработчик с ID
            onItemSelected(selectedItem.id)

            // Логирование для отладки
            Log.d(TAG, "Выбран элемент: ${selectedItem.name}, ID: ${selectedItem.id}")
        }
    }

    private fun setupListeners() {
        binding.btnSaveSensor.setOnClickListener {
            saveSensor()
        }
    }


    private fun saveSensor() {
        Log.d(TAG, "Сохранение датчика...")

        val position = binding.etPosition.text.toString().trim()
        val outputScale = selectedOutputRangeId ?: binding.actvOutputScale.text.toString().trim()
        val midPoint = binding.etMidPoint.text.toString().trim()
        val modification = binding.etModification.text.toString().trim()

        // Проверяем тип датчика
        if (selectedTypeId == null) {
            Log.w(TAG, "Тип датчика не выбран")
            Toast.makeText(requireContext(), "Выберите тип датчика", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем, что все остальные идентификаторы установлены
        val blockId = selectedBlockId
        if (blockId == null) {
            Log.w(TAG, "Блок не выбран")
            Toast.makeText(requireContext(), "Выберите блок", Toast.LENGTH_SHORT).show()
            return
        }

        val measurementStartId = selectedMeasurementStartId
        if (measurementStartId == null) {
            Log.w(TAG, "Начало измерения не выбрано")
            Toast.makeText(requireContext(), "Выберите начало измерения", Toast.LENGTH_SHORT).show()
            return
        }

        val measurementEndId = selectedMeasurementEndId
        if (measurementEndId == null) {
            Log.w(TAG, "Окончание измерения не выбрано")
            Toast.makeText(requireContext(), "Выберите окончание измерения", Toast.LENGTH_SHORT).show()
            return
        }

        // Логируем все выбранные значения для отладки
        Log.d(TAG, "Выбранные значения: typeId=$selectedTypeId, blockId=$blockId, " +
                "startId=$measurementStartId, endId=$measurementEndId")
        Log.d(TAG, "Введенные значения: position=$position, scale=$outputScale, " +
                "midPoint=$midPoint, modification=$modification")

        // Сохранение датчика
        viewModel.saveSensor(
            typeId = selectedTypeId!!,
            position = position,
            outputScale = outputScale,
            midPoint = midPoint,
            modification = modification,
            blockId = blockId,
            measurementStartId = measurementStartId,
            measurementEndId = measurementEndId
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Освобождение ресурсов")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
        _binding = null
    }

    companion object {
        private const val TAG = "AddSensorFragment"
    }
}