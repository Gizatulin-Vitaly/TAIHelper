package com.reftgres.taihelper.ui.oxygen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reftgres.taihelper.R
import com.reftgres.taihelper.databinding.AllMeasurementsBinding

// Модель данных для элемента списка
data class Measurement(
    val date: String,
    val iktsValues: List<String>,
    val sensorValues: List<String>,
    val testoValues: List<String>
)

class AllAdapter : ListAdapter<Measurement, AllAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: AllMeasurementsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Measurement) {
            // Установка даты
            binding.measurementCard.findViewById<TextView>(R.id.date_last_measurement).text = item.date

            // Настройка датчика 9K-606 (бывший fourth)
            setupSensor(
                binding.measurementCard.findViewById(R.id.sensor_606),
                item.iktsValues.getOrElse(3) { "" },
                item.sensorValues.getOrElse(3) { "" },
                item.testoValues.getOrElse(3) { "" }
            )

            // Настройка датчика 9K-604 (бывший second)
            setupSensor(
                binding.measurementCard.findViewById(R.id.sensor_604),
                item.iktsValues.getOrElse(1) { "" },
                item.sensorValues.getOrElse(1) { "" },
                item.testoValues.getOrElse(1) { "" }
            )

            // Настройка датчика 9K-605 (бывший third)
            setupSensor(
                binding.measurementCard.findViewById(R.id.sensor_605),
                item.iktsValues.getOrElse(2) { "" },
                item.sensorValues.getOrElse(2) { "" },
                item.testoValues.getOrElse(2) { "" }
            )

            // Настройка датчика 9K-603 (бывший first)createCategoryButton
            setupSensor(
                binding.measurementCard.findViewById(R.id.sensor_603),
                item.iktsValues.getOrElse(0) { "" },
                item.sensorValues.getOrElse(0) { "" },
                item.testoValues.getOrElse(0) { "" }
            )
        }

        // Вспомогательный метод для настройки одного датчика
        private fun setupSensor(sensorView: View, title: String, panelValue: String, testoValue: String) {
            sensorView.findViewById<TextView>(R.id.sensor_title).text = title
            sensorView.findViewById<TextView>(R.id.panel_value).text = panelValue
            sensorView.findViewById<TextView>(R.id.testo_value).text = testoValue
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AllMeasurementsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Measurement>() {
        override fun areItemsTheSame(oldItem: Measurement, newItem: Measurement) =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: Measurement, newItem: Measurement) =
            oldItem == newItem
    }
}