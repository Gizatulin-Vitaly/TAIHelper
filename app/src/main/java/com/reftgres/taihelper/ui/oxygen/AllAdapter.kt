package com.reftgres.taihelper.ui.oxygen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.dateLastMeasurement.text = item.date
            binding.firstIktsTittle.text = item.iktsValues.getOrElse(0) { "" }
            binding.secondIktsTittle.text = item.iktsValues.getOrElse(1) { "" }
            binding.thirdIktsTittle.text = item.iktsValues.getOrElse(2) { "" }
            binding.fourthIktsTittle.text = item.iktsValues.getOrElse(3) { "" }

            binding.firstSensor.text = item.sensorValues.getOrElse(0) { "" }
            binding.secondSensor.text = item.sensorValues.getOrElse(1) { "" }
            binding.thirdSensor.text = item.sensorValues.getOrElse(2) { "" }
            binding.fourthSensor.text = item.sensorValues.getOrElse(3) { "" }

            binding.firstTesto.text = item.testoValues.getOrElse(0) { "" }
            binding.secondTesto.text = item.testoValues.getOrElse(1) { "" }
            binding.thirdTesto.text = item.testoValues.getOrElse(2) { "" }
            binding.fourthTesto.text = item.testoValues.getOrElse(3) { "" }
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
