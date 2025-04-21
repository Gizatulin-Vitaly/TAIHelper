package com.reftgres.taihelper.ui.oxygen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.reftgres.taihelper.databinding.FragmentOxigenBinding
import dagger.hilt.android.AndroidEntryPoint
import com.reftgres.taihelper.R

@AndroidEntryPoint
class OxygenMeasurementFragment : Fragment() {

    private val TAG = "OxygenFragment"
    private val viewModel: OxygenMeasurementViewModel by viewModels()
    private var _binding: FragmentOxigenBinding? = null
    private val binding get() = _binding!!
    private lateinit var latestMeasurementsAdapter: LatestMeasurementsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView вызван")
        _binding = FragmentOxigenBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.latestMeasurements.observe(viewLifecycleOwner) { list ->
            list.firstOrNull()?.let {
                updateCardAllOxygen(it)
            }
        }

    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Настройка RecyclerView")
        latestMeasurementsAdapter = LatestMeasurementsAdapter()
        binding.recyclerView.apply {
            adapter = latestMeasurementsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private val sensorPositionsMap = mapOf(
        1 to listOf("К-601", "К-602", "К-603", "К-604"),
        2 to listOf("К-601", "К-602", "К-603", "К-604"),
        3 to listOf("К-601", "К-602", "К-603", "К-604"),
        4 to listOf("К-601", "К-602", "К-603", "К-604"),
        5 to listOf("К-601", "К-602", "К-603", "К-604"),
        6 to listOf("К-601", "К-602", "К-603", "К-604"),
        7 to listOf("К-603", "К-604", "К-605", "К-606"),
        8 to listOf("К-603", "К-604", "К-605", "К-606"),
        9 to listOf("К-603", "К-604", "К-605", "К-606"),
        10 to listOf("К-603", "К-604", "К-605", "К-606")
    )

    private fun setupObservers() {
        Log.d(TAG, "Настройка наблюдателей")

        // Наблюдение за списком блоков
        viewModel.blocks.observe(viewLifecycleOwner) { blocks ->
            Log.d(TAG, "Получены блоки: ${blocks.size}")

            if (blocks.isNotEmpty()) {
                val firstBlockId = blocks[0].id
                binding.blockSpinner.setSelection(0)
                viewModel.loadSensorsForBlock(firstBlockId)
                viewModel.loadLatestMeasurements(firstBlockId)
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                blocks.map { it.name }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.blockSpinner.adapter = adapter

            // Если список не пустой, выбираем первый элемент
            if (blocks.isNotEmpty()) {
                binding.blockSpinner.setSelection(0)
            }
        }

        // Наблюдение за списком датчиков для выбранного блока
        viewModel.sensors.observe(viewLifecycleOwner) { sensors ->
            Log.d(TAG, "Получены датчики: ${sensors.size}")

            // Сортируем объекты Sensor по position
            val sortedSensors = sensors.sortedBy { it.position }

            // Сохраняем отсортированный список во ViewModel для onItemSelected
            viewModel.setSortedSensors(sortedSensors) // 🔧 ты добавишь это ниже 👇

            // Извлекаем только названия для отображения
            val sortedSensorTitles = sortedSensors.map { it.position }

            // Создаём адаптер
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sortedSensorTitles
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            // Присваиваем адаптер
            binding.sensorSpinner.adapter = adapter

            // По умолчанию выбираем первый элемент, если список не пустой
            if (sortedSensors.isNotEmpty()) {
                binding.sensorSpinner.setSelection(0)
                // Программно уведомим о выборе
                viewModel.selectSensorByPosition(sortedSensors[0].position)
            }
        }


        // Наблюдение за выбранным датчиком
        viewModel.selectedSensor.observe(viewLifecycleOwner) { sensor ->
            Log.d(TAG, "Выбран датчик: ${sensor?.position}")

            sensor?.let {
                binding.tvPositionSensor.text = it.position
                binding.tvNumberSensor.text = it.serialNumber.ifEmpty { "Не указан" }
                binding.tvMiddleSensor.text = it.midPoint
                binding.tvAnalogOutput.text = it.outputScale

                // При выборе датчика загружаем его историю
                viewModel.loadSensorMeasurementHistory(it.position)
            }
        }

        // Наблюдение за историей измерений конкретного датчика (для card_history)
        viewModel.sensorMeasurementHistory.observe(viewLifecycleOwner) { measurements ->
            Log.d(TAG, "⭐ Получена история измерений: ${measurements.size} записей")

            // Отображаем или скрываем карточку истории в зависимости от наличия данных
            binding.cardHistory.visibility = if (measurements.isEmpty()) View.GONE else View.VISIBLE

            if (measurements.isEmpty()) {
                Log.d(TAG, "⭐ Нет истории измерений для отображения")
                // Опционально: добавить уведомление пользователю
            } else {
                measurements.forEach { measurement ->
                    Log.d(TAG, "⭐ Измерение: ${measurement.date}, датчиков: ${measurement.sensors.size}")
                }
            }

            latestMeasurementsAdapter.submitList(measurements)
        }

        // Наблюдение за последними измерениями блока (для card_all_oxygen)
        viewModel.latestMeasurements.observe(viewLifecycleOwner) { measurements ->
            Log.d(TAG, "Получены общие измерения: ${measurements.size}")

            if (measurements.isNotEmpty()) {
                updateCardAllOxygen(measurements.first())
            } else {
                clearSensorDisplays()
                binding.tvDateControl.text = "Нет данных"
            }
        }

        // Наблюдение за статусом сети
        val networkStatusBar = view?.findViewById<View>(R.id.networkStatusBar)
        viewModel.isOnline.observe(viewLifecycleOwner) { isOnline ->
            Log.d(TAG, "Статус сети: ${if (isOnline) "онлайн" else "офлайн"}")
            networkStatusBar?.visibility = if (isOnline) View.GONE else View.VISIBLE
        }
    }

    private fun setupListeners() {
        Log.d(TAG, "Настройка обработчиков событий")

        // Слушатель выбора блока
        binding.blockSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val blocks = viewModel.blocks.value
                if (blocks != null && position >= 0 && position < blocks.size) {
                    val blockId = blocks[position].id
                    Log.d(TAG, "Выбран блок: $blockId")

                    updateSensorTitlesForBlock(blockId)

                    viewModel.loadSensorsForBlock(blockId)
                    viewModel.loadLatestMeasurements(blockId)
                }
            }


            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "Ничего не выбрано в списке блоков")
            }
        }

        // Слушатель выбора датчика
        binding.sensorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = viewModel.getSortedSensors().getOrNull(position)
                if (selected != null) {
                    Log.d(TAG, "Выбрана позиция датчика: ${selected.position}")
                    viewModel.selectSensorByPosition(selected.position)
                    viewModel.loadSensorMeasurementHistory(selected.position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "Ничего не выбрано в списке датчиков")
            }
        }

        // Карточка с измерениями (переход к истории)
        binding.cardAllOxygen.setOnClickListener {
            Log.d(TAG, "Клик по карточке измерений")
            try {
                findNavController().navigate(R.id.action_oxygenMeasurementFragment_to_all_measurements)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при навигации: ${e.message}")
                Toast.makeText(requireContext(), "Ошибка при переходе к истории измерений", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSensorTitlesForBlock(blockId: Int) {
        val titles = sensorPositionsMap[blockId] ?: listOf("К-601", "К-602", "К-603", "К-604")

        binding.tvFourthSensorTitle.text = titles.getOrNull(0) ?: "--"
        binding.tvSecondSensorTitle.text = titles.getOrNull(1) ?: "--"
        binding.tvThirdSensorTitle.text  = titles.getOrNull(2) ?: "--"
        binding.tvFirstSensorTitle.text  = titles.getOrNull(3) ?: "--"
    }

    /**
     * Обновление UI с последними измерениями
     */
    private fun updateMeasurementsUI(measurements: List<LatestMeasurement>) {
        if (measurements.isEmpty()) {
            Log.d(TAG, "Нет измерений для отображения")
            binding.tvDateControl.text = "Нет данных"
            clearSensorDisplays()
            return
        }

        // Получаем последнее измерение
        val latestMeasurement = measurements.first()
        Log.d(TAG, "Последнее измерение: ${latestMeasurement.date}, датчиков: ${latestMeasurement.sensors.size}")

        // Устанавливаем дату
        binding.tvDateControl.text = latestMeasurement.date

        // Обновляем данные датчиков
        updateSensorDisplay(latestMeasurement, "К-601", binding.tvFirstSensorTitle, binding.tvFirstIndicate, binding.tvFirstMiddle)
        updateSensorDisplay(latestMeasurement, "К-602", binding.tvSecondSensorTitle, binding.tvSecondIndicate, binding.tvSecondMiddle)
        updateSensorDisplay(latestMeasurement, "К-603", binding.tvThirdSensorTitle, binding.tvThirdIndicate, binding.tvThirdMiddle)
        updateSensorDisplay(latestMeasurement, "К-604", binding.tvFourthSensorTitle, binding.tvFourthIndicate, binding.tvFourthMiddle)
    }

    private fun updateCardAllOxygen(measurement: LatestMeasurement) {
        val blockId = measurement.blockNumber
        val sensorTitles = sensorPositionsMap[blockId] ?: listOf("К-601", "К-602", "К-603", "К-604")
        Log.d("SensorUI", "➡ sensorTitles (по позиции): $sensorTitles")


        Log.d("SensorUI", "sensorTitles = $sensorTitles")
        Log.d("SensorUI", "sensor[0] будет в строке 1: ${sensorTitles.getOrNull(0)}")
        Log.d("SensorUI", "sensor[1] будет в строке 2: ${sensorTitles.getOrNull(1)}")
        // Правильный порядок заголовков
        // Заголовки
        binding.tvFirstSensorTitle.text  = sensorTitles.getOrNull(0) ?: "--" // К-601
        binding.tvSecondSensorTitle.text = sensorTitles.getOrNull(1) ?: "--" // К-602
        binding.tvThirdSensorTitle.text  = sensorTitles.getOrNull(2) ?: "--" // К-603
        binding.tvFourthSensorTitle.text = sensorTitles.getOrNull(3) ?: "--" // К-604

// Обнуляем показания
        binding.tvFirstIndicate.text  = "--"
        binding.tvSecondIndicate.text = "--"
        binding.tvThirdIndicate.text  = "--"
        binding.tvFourthIndicate.text = "--"

        binding.tvFirstMiddle.text  = ""
        binding.tvSecondMiddle.text = ""
        binding.tvThirdMiddle.text  = ""
        binding.tvFourthMiddle.text = ""

// Значения из измерений
        measurement.sensors.forEach { sensor ->
            val index = sensorTitles.indexOf(sensor.sensorTitle)
            Log.d("SensorUI", "⬅ Firestore sensor: ${sensor.sensorTitle}, index в sensorTitles = $index")

            when (index) {
                0 -> {
                    Log.d("SensorUI", "🟩 Присваиваем ${sensor.sensorTitle} → tvFirstIndicate")
                    binding.tvFirstIndicate.text = sensor.testoValue.ifEmpty { "--" }
                    binding.tvFirstMiddle.text   = "(${sensor.panelValue})"
                }
                1 -> {
                    Log.d("SensorUI", "🟨 Присваиваем ${sensor.sensorTitle} → tvSecondIndicate")
                    binding.tvSecondIndicate.text = sensor.testoValue.ifEmpty { "--" }
                    binding.tvSecondMiddle.text   = "(${sensor.panelValue})"
                }
                2 -> {
                    Log.d("SensorUI", "🟧 Присваиваем ${sensor.sensorTitle} → tvThirdIndicate")
                    binding.tvThirdIndicate.text = sensor.testoValue.ifEmpty { "--" }
                    binding.tvThirdMiddle.text   = "(${sensor.panelValue})"
                }
                3 -> {
                    Log.d("SensorUI", "🟥 Присваиваем ${sensor.sensorTitle} → tvFourthIndicate")
                    binding.tvFourthIndicate.text = sensor.testoValue.ifEmpty { "--" }
                    binding.tvFourthMiddle.text   = "(${sensor.panelValue})"
                }
            }
        }

        binding.tvDateControl.text = measurement.date
    }


    /**
     * Обновление отображения конкретного датчика
     */
    private fun updateSensorDisplay(
        measurement: LatestMeasurement,
        sensorTitle: String,
        titleView: TextView,
        indicateView: TextView,
        middleView: TextView
    ) {
        // Установка названия
        titleView.text = sensorTitle

        // Поиск данных датчика
        val sensor = measurement.sensors.find { it.sensorTitle == sensorTitle }

        if (sensor != null) {
            Log.d(TAG, "Найден датчик $sensorTitle: testo=${sensor.testoValue}, correction=${sensor.correctionValue}")
            indicateView.text = sensor.testoValue
            middleView.text = "(${sensor.correctionValue})"
        } else {
            Log.d(TAG, "Датчик $sensorTitle не найден")
            indicateView.text = "--"
            middleView.text = "(--)"
        }
    }

    /**
     * Очистка отображения датчиков
     */
    private fun clearSensorDisplays() {
        binding.tvFirstIndicate.text = "--"
        binding.tvFirstMiddle.text = "(--)"

        binding.tvSecondIndicate.text = "--"
        binding.tvSecondMiddle.text = "(--)"

        binding.tvThirdIndicate.text = "--"
        binding.tvThirdMiddle.text = "(--)"

        binding.tvFourthIndicate.text = "--"
        binding.tvFourthMiddle.text = "(--)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}