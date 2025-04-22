package com.reftgres.taihelper.ui.oxygennew

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.NewMeasuremensBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class NewMeasurementsFragment : Fragment() {
    private var _binding: NewMeasuremensBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NewMeasurementsViewModel by viewModels()

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NewMeasuremensBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.GONE

        setupToolbar()
        setupInitialDate()
        setupObservers()
        setupDatePicker()
        setupBlockSelector()
        setupSaveButton()
        handleBackPress()
    }

    private fun setupInitialDate() {
        // Устанавливаем начальную дату и передаем ее в ViewModel
        val currentDate = dateFormat.format(calendar.time)
        binding.dateLastMeasurement.text = currentDate
        viewModel.setMeasurementDate(currentDate)
    }

    private fun setupObservers() {
        // Подписываемся на обновления данных из ViewModel
        viewModel.sensorTitles.observe(viewLifecycleOwner) { titles ->
            updateSensorTitlesInUI(titles)
        }

        // При необходимости можно добавить другие наблюдатели
        viewModel.blockNumber.observe(viewLifecycleOwner) { blockNumber ->
            // Можно обновить UI при изменении номера блока
        }


    }

    private fun updateSensorTitlesInUI(titles: List<String>) {
        Log.d("SensorDebug", "Обновляем заголовки: ${titles.joinToString()}")

        if (titles.size >= 4) {
            binding.sensorGroup1.root.findViewById<TextView>(R.id.sensor_title)?.text = titles[0]
            binding.sensorGroup2.root.findViewById<TextView>(R.id.sensor_title)?.text = titles[1]
            binding.sensorGroup3.root.findViewById<TextView>(R.id.sensor_title)?.text = titles[2]
            binding.sensorGroup4.root.findViewById<TextView>(R.id.sensor_title)?.text = titles[3]
        }
        val correctionMap = viewModel.sensorsData.value ?: return

        binding.sensorGroup1.root.findViewById<TextInputEditText>(R.id.midpoint_correction_value)
            ?.setText(correctionMap[titles[0]]?.correction ?: "")

        binding.sensorGroup2.root.findViewById<TextInputEditText>(R.id.midpoint_correction_value)
            ?.setText(correctionMap[titles[1]]?.correction ?: "")

        binding.sensorGroup3.root.findViewById<TextInputEditText>(R.id.midpoint_correction_value)
            ?.setText(correctionMap[titles[2]]?.correction ?: "")

        binding.sensorGroup4.root.findViewById<TextInputEditText>(R.id.midpoint_correction_value)
            ?.setText(correctionMap[titles[3]]?.correction ?: "")
    }

    private fun setupDatePicker() {
        binding.changeDateButton.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                    val formattedDate = dateFormat.format(calendar.time)
                    binding.dateLastMeasurement.text = formattedDate
                    viewModel.setMeasurementDate(formattedDate)
                },
                year,
                month,
                day
            ).show()
        }
    }

    private fun setupBlockSelector() {
        val blocks = (1..10).map { it.toString() }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            blocks
        )

        (binding.blockDropdown.editText as? AutoCompleteTextView)?.apply {
            setAdapter(adapter)
            setText("1", false)

            setOnItemClickListener { _, _, position, _ ->
                val selectedBlock = adapter.getItem(position)?.toIntOrNull() ?: 1
                viewModel.setBlockNumber(selectedBlock)
                viewModel.preloadMidpointsForBlock(selectedBlock)
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            // Собираем данные из UI
            collectAndSaveSensorData(0, binding.sensorGroup1.root)
            collectAndSaveSensorData(1, binding.sensorGroup2.root)
            collectAndSaveSensorData(2, binding.sensorGroup3.root)
            collectAndSaveSensorData(3, binding.sensorGroup4.root)

            // Используем метод с поддержкой офлайн-режима
            viewModel.saveMeasurementsWithOfflineSupport().observe(viewLifecycleOwner) { result ->
                when (result) {
                    is SaveResult.Loading -> {
                        binding.saveButton.isEnabled = false
                        binding.saveButton.text = "Сохранение..."
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is SaveResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "Сохранить"
                        Toast.makeText(context, "Данные успешно сохранены", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    is SaveResult.OfflineSuccess -> {
                        binding.progressBar.visibility = View.GONE
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "Сохранить"
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                    is SaveResult.PartialSuccess -> {
                        binding.progressBar.visibility = View.GONE
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "Сохранить"
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        // Можно оставить пользователя на странице для возможности повторной попытки
                    }
                    is SaveResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "Сохранить"
                        Toast.makeText(context, "Ошибка: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun collectAndSaveSensorData(index: Int, rootView: View) {
        val panel = rootView.findViewById<TextInputEditText>(R.id.panel_value).text.toString()
        val testo = rootView.findViewById<TextInputEditText>(R.id.testo_value).text.toString()
        val correction = rootView.findViewById<TextInputEditText>(R.id.midpoint_correction_value).text.toString()

        viewModel.updateSensorData(index, panel, testo, correction)
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        requireActivity().findViewById<MaterialToolbar>(R.id.toolbar)?.apply {
            setNavigationOnClickListener {
                // Проверяем, что фрагмент все еще прикреплен
                if (isAdded) {
                    findNavController().navigateUp()
                } else {
                    // Альтернативный способ вернуться назад
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
        _binding = null
    }
}