package com.reftgres.taihelper.ui.oxygen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reftgres.taihelper.R


class FiveAdapter(private val data: List<String>) : RecyclerView.Adapter<FiveAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.data_measurement)
        val board: TextView = view.findViewById(R.id.measurement_board)
        val value: TextView = view.findViewById(R.id.measurement_testo)
        val direction : ImageView = view.findViewById(R.id.image_dinamic)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.last_measurements, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.date.text = "26.11.1989"
        holder.board.text = "3.57"
        holder.value.text = "3.57"
        holder.direction.setImageResource(R.drawable.ic_arrow_up)
    }

    override fun getItemCount() = data.size
}
