package com.reftgres.taihelper.ui.oxygen

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reftgres.taihelper.R

class LatestMeasurementsAdapter : ListAdapter<LatestMeasurement, LatestMeasurementsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.last_measurements, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val measurement = getItem(position)
        Log.d("Adapter", "⭐ onBindViewHolder: позиция $position, дата: ${measurement.date}")
        holder.bind(measurement)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateTextView: TextView = view.findViewById(R.id.data_measurement)
        private val boardTextView: TextView = view.findViewById(R.id.measurement_board)
        private val testoTextView: TextView = view.findViewById(R.id.measurement_testo)
        private val dynamicIcon: ImageView = view.findViewById(R.id.image_dinamic)

        fun bind(measurement: LatestMeasurement) {
            Log.d("Adapter", "⭐ Binding measurement: ${measurement.date}, датчиков: ${measurement.sensors.size}")

            // Устанавливаем дату измерения
            dateTextView.text = measurement.date

            // Обрабатываем данные датчика (должен быть только один после фильтрации)
            val sensor = measurement.sensors.firstOrNull()
            if (sensor != null) {
                Log.d("Adapter", "⭐ Датчик ${sensor.sensorTitle}: panel=${sensor.panelValue}, testo=${sensor.testoValue}")

                // Устанавливаем значения
                boardTextView.text = sensor.panelValue.ifEmpty { "--" }
                testoTextView.text = sensor.testoValue.ifEmpty { "--" }

                // Логика отображения индикатора направления
                try {
                    val panelValue = sensor.panelValue.toFloatOrNull() ?: 0f
                    val testoValue = sensor.testoValue.toFloatOrNull() ?: 0f

                    dynamicIcon.visibility = View.VISIBLE

                    // Устанавливаем направление стрелки в зависимости от разницы
                    val iconResource = when {
                        testoValue > panelValue -> R.drawable.ic_arrow_up
                        testoValue < panelValue -> R.drawable.ic_arrow_down
                        else -> R.drawable.ic_same
                    }
                    dynamicIcon.setImageResource(iconResource)

                } catch (e: Exception) {
                    Log.e("Adapter", "⭐ Ошибка при установке индикатора", e)
                    dynamicIcon.visibility = View.INVISIBLE
                }
            } else {
                Log.d("Adapter", "⭐ Датчик не найден в измерении")
                boardTextView.text = "--"
                testoTextView.text = "--"
                dynamicIcon.visibility = View.INVISIBLE
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