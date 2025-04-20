package com.reftgres.taihelper.ui.ajk

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reftgres.taihelper.R
import java.text.SimpleDateFormat
import java.util.Locale

class CalibrationHistoryAdapter(
    private var items: List<DataAjk>,
    private val onItemClick: (DataAjk) -> Unit
) : RecyclerView.Adapter<CalibrationHistoryAdapter.ViewHolder>() {

    fun update(newItems: List<DataAjk>) {
        items = newItems
        notifyDataSetChanged()
        Log.d("Adapter", "Обновляем список: ${items.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calibration_summary, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val calibration = items[position]
        val dateFormatted = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calibration.timestamp)

        holder.position.text = "Позиция: ${calibration.sensorPosition}"
        holder.serial.text = "Серийный №: ${calibration.sensorSerial}"
        holder.date.text = "Дата: $dateFormatted"

        holder.itemView.setOnClickListener {
            onItemClick(calibration)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val position: TextView = view.findViewById(R.id.item_sensor_position)
        val serial: TextView = view.findViewById(R.id.item_sensor_serial)
        val date: TextView = view.findViewById(R.id.item_calibration_date)
    }
}
