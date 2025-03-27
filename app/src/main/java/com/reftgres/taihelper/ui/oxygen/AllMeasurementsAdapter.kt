package com.reftgres.taihelper.ui.oxygen

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reftgres.taihelper.R
import com.reftgres.taihelper.ui.model.SensorMeasurement

class AllMeasurementsAdapter : ListAdapter<LatestMeasurement, AllMeasurementsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.all_measurements, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val measurement = getItem(position)
        Log.d("AllMeasurementsAdapter", "⭐ onBindViewHolder: позиция $position, дата: ${measurement.date}, блок: ${measurement.blockNumber}")
        holder.bind(measurement)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateTextView: TextView = view.findViewById(R.id.date_last_measurement)

        // Ссылки на контейнеры датчиков
        private val sensorContainers = mapOf(
            1 to view.findViewById<View>(R.id.sensor_603),
            2 to view.findViewById<View>(R.id.sensor_604),
            3 to view.findViewById<View>(R.id.sensor_605),
            4 to view.findViewById<View>(R.id.sensor_606)
        )

        // Датчики для блоков 1-6
        private val sensorsBlock1To6 = listOf("К-601", "К-602", "К-603", "К-604")

        // Датчики для блоков 7-10
        private val sensorsBlock7To10 = listOf("К-603", "К-604", "К-605", "К-606")

        fun bind(measurement: LatestMeasurement) {
            Log.d("AllMeasurementsAdapter", "⭐ Binding measurement: ${measurement.date}, блок: ${measurement.blockNumber}, датчиков: ${measurement.sensors.size}")

            // Установка даты
            dateTextView.text = measurement.date

            // Определяем список датчиков в зависимости от номера блока
            val sensorsList = if (measurement.blockNumber in 1..6) {
                sensorsBlock1To6
            } else {
                sensorsBlock7To10
            }

            // Устанавливаем заголовки датчиков в соответствии с блоком
            for (i in 1..4) {
                val container = sensorContainers[i]
                val sensorTitle = container?.findViewById<TextView>(R.id.sensor_title)
                val panelValue = container?.findViewById<TextView>(R.id.panel_value)
                val testoValue = container?.findViewById<TextView>(R.id.testo_value)

                // Установка заголовка датчика
                sensorTitle?.text = sensorsList.getOrNull(i-1) ?: "--"

                // Сброс значений
                panelValue?.text = "--"
                testoValue?.text = "--"
            }

            // Заполняем значения датчиков из измерения
            measurement.sensors.forEach { sensor ->
                // Находим индекс датчика в списке
                val sensorIndex = sensorsList.indexOf(sensor.sensorTitle)

                if (sensorIndex != -1) {
                    // Получаем контейнер для этого датчика
                    val container = sensorContainers[sensorIndex + 1]

                    // Обновляем значения
                    container?.findViewById<TextView>(R.id.panel_value)?.text =
                        sensor.panelValue.ifEmpty { "--" }
                    container?.findViewById<TextView>(R.id.testo_value)?.text =
                        sensor.testoValue.ifEmpty { "--" }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LatestMeasurement>() {
        override fun areItemsTheSame(oldItem: LatestMeasurement, newItem: LatestMeasurement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LatestMeasurement, newItem: LatestMeasurement): Boolean {
            return oldItem == newItem
        }
    }
}