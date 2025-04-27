package com.reftgres.taihelper.ui.ajk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.reftgres.taihelper.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class CalibrationDetailFragment : Fragment() {

    private val viewModel: AjkViewModel by viewModels()

    private lateinit var positionText: TextView
    private lateinit var serialText: TextView
    private lateinit var dateText: TextView

    private lateinit var labValuesText: TextView
    private lateinit var labAvgText: TextView

    private lateinit var testValuesText: TextView
    private lateinit var testAvgText: TextView

    private lateinit var resistanceText: TextView
    private lateinit var constantText: TextView
    private lateinit var rSeriesText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_calibration_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Получаем ID из аргументов
        val id = arguments?.getString("calibrationId") ?: return

        // Привязка вьюшек
        positionText = view.findViewById(R.id.detail_position)
        serialText = view.findViewById(R.id.detail_serial)
        dateText = view.findViewById(R.id.detail_date)

        labValuesText = view.findViewById(R.id.detail_lab_values)
        labAvgText = view.findViewById(R.id.detail_lab_avg)

        testValuesText = view.findViewById(R.id.detail_test_values)
        testAvgText = view.findViewById(R.id.detail_test_avg)

        resistanceText = view.findViewById(R.id.detail_resistance)
        constantText = view.findViewById(R.id.detail_constant)
        rSeriesText = view.findViewById(R.id.detail_r_series)

        // Подгружаем данные
        viewModel.getCalibrationById(id).observe(viewLifecycleOwner) { calibration ->
            if (calibration != null) {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

                positionText.text = "Позиция: ${calibration.sensorPosition}"
                serialText.text = "Заводской №: ${calibration.sensorSerial}"
                dateText.text = "Дата: ${sdf.format(calibration.timestamp)}"

                labValuesText.text = calibration.labSensorValues.joinToString(", ")
                labAvgText.text = "Среднее: ${calibration.labAverage}"

                testValuesText.text = calibration.testSensorValues.joinToString(", ")
                testAvgText.text = "Среднее: ${calibration.testAverage}"

                resistanceText.text = "Сопротивление: ${calibration.resistance}"
                constantText.text = "Константа: ${calibration.constant}"

                val rList = buildList {
                    add("▪ 0.2\n   R: ${calibration.r02Resistance} Ом\n   I: ${calibration.r02I} мА\n   Показания: ${calibration.r02SensorValue}")
                    add("▪ 0.5\n   R: ${calibration.r05Resistance} Ом\n   I: ${calibration.r05I} мА\n   Показания: ${calibration.r05SensorValue}")
                    add("▪ 0.8\n   R: ${calibration.r08Resistance} Ом\n   I: ${calibration.r08I} мА\n   Показания: ${calibration.r08SensorValue}")
                    add("▪ 40°C\n   R: ${calibration.r40DegResistance} Ом\n   Показания: ${calibration.r40DegSensorValue}")
                }
                rSeriesText.text = rList.joinToString("\n\n")

            } else {
                positionText.text = "Ошибка: калибровка не найдена"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        positionText = TextView(requireContext())
        serialText = TextView(requireContext())
        dateText = TextView(requireContext())
        labValuesText = TextView(requireContext())
        labAvgText = TextView(requireContext())
        testValuesText = TextView(requireContext())
        testAvgText = TextView(requireContext())
        resistanceText = TextView(requireContext())
        constantText = TextView(requireContext())
        rSeriesText = TextView(requireContext())
    }

}
