package com.reftgres.taihelper.ui.ajk

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.reftgres.taihelper.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AjkFragment : Fragment() {

    private val viewModel: AjkViewModel by viewModels()

    // UI элементы
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var stepTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var percentTextView: TextView

    // Контейнеры для шагов
    private lateinit var step1Container: LinearLayout
    private lateinit var step2Container: LinearLayout
    private lateinit var step3Container: LinearLayout
    private lateinit var step4Container: LinearLayout

    // Шаг 1: Ввод значений датчиков
    private lateinit var labSensorEditText1: EditText
    private lateinit var labSensorEditText2: EditText
    private lateinit var labSensorEditText3: EditText
    private lateinit var labAverageTextView: TextView

    private lateinit var testSensorEditText1: EditText
    private lateinit var testSensorEditText2: EditText
    private lateinit var testSensorEditText3: EditText
    private lateinit var testAverageTextView: TextView

    // Шаг 2: Сопротивление и константа
    private lateinit var resistanceEditText: EditText
    private lateinit var constantTextView: TextView
    private lateinit var labAvgInfoTextView: TextView

    // Шаг 3: Таблица значений
    private lateinit var r02TextView: TextView
    private lateinit var r02IEditText: EditText
    private lateinit var r02SensorEditText: EditText

    private lateinit var r05TextView: TextView
    private lateinit var r05IEditText: EditText
    private lateinit var r05SensorEditText: EditText

    private lateinit var r08TextView: TextView
    private lateinit var r08IEditText: EditText
    private lateinit var r08SensorEditText: EditText

    // Шаг 4: Температура и результаты
    private lateinit var r40TextView: TextView
    private lateinit var r40SensorEditText: EditText

    private lateinit var resultTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var backToStartButton: Button

    // Кнопки навигации
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ajk, container, false)

        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTextWatchers()
        setupListeners()
        observeViewModel()
        updateUIFields()
    }

    private fun initializeViews(view: View) {
        // Элементы заголовка
        progressBar = view.findViewById(R.id.progress_bar)
        stepTextView = view.findViewById(R.id.step_text_view)
        titleTextView = view.findViewById(R.id.title_text_view)
        percentTextView = view.findViewById(R.id.percent_text_view)

        // Контейнеры для шагов
        step1Container = view.findViewById(R.id.step1_container)
        step2Container = view.findViewById(R.id.step2_container)
        step3Container = view.findViewById(R.id.step3_container)
        step4Container = view.findViewById(R.id.step4_container)

        // Шаг 1: Поля для лабораторного датчика
        labSensorEditText1 = view.findViewById(R.id.lab_sensor_edit_text_1)
        labSensorEditText2 = view.findViewById(R.id.lab_sensor_edit_text_2)
        labSensorEditText3 = view.findViewById(R.id.lab_sensor_edit_text_3)
        labAverageTextView = view.findViewById(R.id.lab_average_text_view)

        // Шаг 1: Поля для поверяемого датчика
        testSensorEditText1 = view.findViewById(R.id.test_sensor_edit_text_1)
        testSensorEditText2 = view.findViewById(R.id.test_sensor_edit_text_2)
        testSensorEditText3 = view.findViewById(R.id.test_sensor_edit_text_3)
        testAverageTextView = view.findViewById(R.id.test_average_text_view)

        // Шаг 2: Сопротивление и константа
        resistanceEditText = view.findViewById(R.id.resistance_edit_text)
        constantTextView = view.findViewById(R.id.constant_text_view)
        labAvgInfoTextView = view.findViewById(R.id.lab_avg_info_text_view)

        // Шаг 3: Таблица значений
        r02TextView = view.findViewById(R.id.r02_text_view)
        r02IEditText = view.findViewById(R.id.r02_i_edit_text)
        r02SensorEditText = view.findViewById(R.id.r02_sensor_edit_text)

        r05TextView = view.findViewById(R.id.r05_text_view)
        r05IEditText = view.findViewById(R.id.r05_i_edit_text)
        r05SensorEditText = view.findViewById(R.id.r05_sensor_edit_text)

        r08TextView = view.findViewById(R.id.r08_text_view)
        r08IEditText = view.findViewById(R.id.r08_i_edit_text)
        r08SensorEditText = view.findViewById(R.id.r08_sensor_edit_text)

        // Шаг 4: Температура и результаты
        r40TextView = view.findViewById(R.id.r40_text_view)
        r40SensorEditText = view.findViewById(R.id.r40_sensor_edit_text)

        resultTextView = view.findViewById(R.id.result_text_view)
        statusTextView = view.findViewById(R.id.status_text_view)
        backToStartButton = view.findViewById(R.id.back_to_start_button)

        // Кнопки навигации
        prevButton = view.findViewById(R.id.prev_button)
        nextButton = view.findViewById(R.id.next_button)
    }

    private fun setupTextWatchers() {
        // Вспомогательная функция для настройки TextWatcher
        fun EditText.setAfterTextChangedListener(action: (String) -> Unit) {
            this.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    action.invoke(editable.toString())
                }
            })
        }

        // Лабораторный датчик
        labSensorEditText1.setAfterTextChangedListener { viewModel.updateLabSensorValue(0, it) }
        labSensorEditText2.setAfterTextChangedListener { viewModel.updateLabSensorValue(1, it) }
        labSensorEditText3.setAfterTextChangedListener { viewModel.updateLabSensorValue(2, it) }

        // Поверяемый датчик
        testSensorEditText1.setAfterTextChangedListener { viewModel.updateTestSensorValue(0, it) }
        testSensorEditText2.setAfterTextChangedListener { viewModel.updateTestSensorValue(1, it) }
        testSensorEditText3.setAfterTextChangedListener { viewModel.updateTestSensorValue(2, it) }

        // Сопротивление
        resistanceEditText.setAfterTextChangedListener { viewModel.updateResistance(it) }

        // Таблица значений
        r02IEditText.setAfterTextChangedListener { viewModel.updateR02I(it) }
        r02SensorEditText.setAfterTextChangedListener { viewModel.updateR02SensorValue(it) }
        r05IEditText.setAfterTextChangedListener { viewModel.updateR05I(it) }
        r05SensorEditText.setAfterTextChangedListener { viewModel.updateR05SensorValue(it) }
        r08IEditText.setAfterTextChangedListener { viewModel.updateR08I(it) }
        r08SensorEditText.setAfterTextChangedListener { viewModel.updateR08SensorValue(it) }

        // Температура
        r40SensorEditText.setAfterTextChangedListener { viewModel.updateR40DegSensorValue(it) }
    }

    private fun setupListeners() {
        // Кнопки навигации
        prevButton.setOnClickListener {
            if (viewModel.currentStep > 1) {
                viewModel.previousStep()
            } else {
                viewModel.resetData()
                updateUIFields()
            }
        }

        nextButton.setOnClickListener {
            if (viewModel.currentStep < 4) {
                // Проверяем, все ли поля заполнены
                if (viewModel.canProceedToNextStep()) {
                    viewModel.nextStep()
                } else {
                    Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.saveToCloud()
            }
        }

        // Кнопка возврата к началу
        backToStartButton.setOnClickListener {
            viewModel.resetData()
            viewModel.setCurrentStep(1)
            updateUIFields()
        }
    }

    private fun observeViewModel() {
        // Наблюдение за текущим шагом
        viewModel.observeCurrentStep().observe(viewLifecycleOwner, Observer { step ->
            updateStepVisibility(step)
            updateStepIndicator(step)
            updateNavigationButtons(step)
        })

        // Наблюдение за средними значениями
        viewModel.observeLabAverage().observe(viewLifecycleOwner, Observer { average ->
            labAverageTextView.text = String.format("%.4f", average)
            labAvgInfoTextView.text = "Среднее (лаб.): ${String.format("%.4f", average)}"
        })

        viewModel.observeTestAverage().observe(viewLifecycleOwner, Observer { average ->
            testAverageTextView.text = String.format("%.4f", average)
        })

        // Наблюдение за константой
        viewModel.observeConstant().observe(viewLifecycleOwner, Observer { constant ->
            constantTextView.text = if (constant > 0) String.format("%.4f", constant) else ""
            updateResistanceLabels()
        })

        // Наблюдение за статусом сохранения
        viewModel.observeSaveStatus().observe(viewLifecycleOwner, Observer { status ->
            updateSaveStatus(status)

            // Показываем кнопку "Перейти в начало" только после успешного сохранения
            backToStartButton.isVisible = status is AjkViewModel.SaveStatus.Success
        })
    }

    private fun updateStepVisibility(step: Int) {
        step1Container.isVisible = step == 1
        step2Container.isVisible = step == 2
        step3Container.isVisible = step == 3
        step4Container.isVisible = step == 4
    }

    private fun updateStepIndicator(step: Int) {
        stepTextView.text = "Шаг $step/4"
        percentTextView.text = "${(step - 1) * 100 / 3}%"
        progressBar.progress = (step - 1) * 100 / 3 // 0, 33, 66, 100
    }

    private fun updateNavigationButtons(step: Int) {
        if (step > 1) {
            prevButton.text = "Назад"
        } else {
            prevButton.text = "Сбросить"
        }

        if (step < 4) {
            nextButton.text = "Далее"
        } else {
            nextButton.text = "Сохранить"
        }
    }

    private fun updateResistanceLabels() {
        val constant = viewModel.observeConstant().value ?: 0f

        if (constant > 0) {
            r02TextView.text = "R = K/0.2 = ${String.format("%.3f", constant / 0.2)}"
            r05TextView.text = "R = K/0.5 = ${String.format("%.3f", constant / 0.5)}"
            r08TextView.text = "R = K/0.8 = ${String.format("%.3f", constant / 0.8)}"
            r40TextView.text = "R = K/0.68 = ${String.format("%.3f", constant / 0.68)}"

            // Обновляем итоговые результаты
            val resultText = """
                Константа: ${String.format("%.4f", constant)}
                Среднее (лаб): ${String.format("%.4f", viewModel.observeLabAverage().value ?: 0f)}
                Среднее (пов): ${String.format("%.4f", viewModel.observeTestAverage().value ?: 0f)}
                R (0.2): ${String.format("%.4f", constant / 0.2)}
                R (0.5): ${String.format("%.4f", constant / 0.5)}
                R (0.8): ${String.format("%.4f", constant / 0.8)}
                R (t=40°C): ${String.format("%.4f", constant / 0.68)}
            """.trimIndent()

            resultTextView.text = resultText
        } else {
            r02TextView.text = "R = K/0.2 = -"
            r05TextView.text = "R = K/0.5 = -"
            r08TextView.text = "R = K/0.8 = -"
            r40TextView.text = "R = K/0.68 = -"
            resultTextView.text = ""
        }
    }

    private fun updateSaveStatus(status: AjkViewModel.SaveStatus) {
        when (status) {
            is AjkViewModel.SaveStatus.None -> {
                statusTextView.isVisible = false
                backToStartButton.isVisible = false
            }
            is AjkViewModel.SaveStatus.Saving -> {
                statusTextView.isVisible = true
                statusTextView.text = "Сохранение данных..."
                statusTextView.setBackgroundResource(R.drawable.bg_status_saving)
                backToStartButton.isVisible = false
            }
            is AjkViewModel.SaveStatus.Success -> {
                statusTextView.isVisible = true
                statusTextView.text = "Данные успешно сохранены!"
                statusTextView.setBackgroundResource(R.drawable.bg_status_success)
                backToStartButton.isVisible = true
            }
            is AjkViewModel.SaveStatus.Error -> {
                statusTextView.isVisible = true
                statusTextView.text = "Ошибка: ${status.message}"
                statusTextView.setBackgroundResource(R.drawable.bg_status_error)
                backToStartButton.isVisible = false
            }
        }
    }

    private fun updateUIFields() {
        // Обновляем значения полей из ViewModel
        labSensorEditText1.setText(viewModel.labSensorValues.getOrNull(0) ?: "")
        labSensorEditText2.setText(viewModel.labSensorValues.getOrNull(1) ?: "")
        labSensorEditText3.setText(viewModel.labSensorValues.getOrNull(2) ?: "")

        testSensorEditText1.setText(viewModel.testSensorValues.getOrNull(0) ?: "")
        testSensorEditText2.setText(viewModel.testSensorValues.getOrNull(1) ?: "")
        testSensorEditText3.setText(viewModel.testSensorValues.getOrNull(2) ?: "")

        resistanceEditText.setText(viewModel.resistance)

        r02IEditText.setText(viewModel.r02I)
        r02SensorEditText.setText(viewModel.r02SensorValue)

        r05IEditText.setText(viewModel.r05I)
        r05SensorEditText.setText(viewModel.r05SensorValue)

        r08IEditText.setText(viewModel.r08I)
        r08SensorEditText.setText(viewModel.r08SensorValue)

        r40SensorEditText.setText(viewModel.r40DegSensorValue)
    }
}