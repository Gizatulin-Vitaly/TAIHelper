package com.reftgres.taihelper.ui.ajk

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reftgres.taihelper.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController

class CalibrationHistoryFragment : Fragment() {

    private var dateFrom: Date? = null
    private var dateTo: Date? = null
    private val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_calibration_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel: AjkViewModel by activityViewModels()

        val positionInput = view.findViewById<AutoCompleteTextView>(R.id.filter_position_input)
        val serialInput = view.findViewById<EditText>(R.id.filter_serial_input)
        val filterButton = view.findViewById<Button>(R.id.filter_button)
        val recyclerView = view.findViewById<RecyclerView>(R.id.history_recycler_view)

        val adapter = CalibrationHistoryAdapter(emptyList()) { item ->
            val bundle = bundleOf("calibrationId" to item.id)
            findNavController().navigate(R.id.action_calibrationHistoryFragment_to_calibrationDetailFragment, bundle)
        }

        recyclerView.adapter = adapter

        viewModel.getCalibrationHistoryLive().observe(viewLifecycleOwner) { items ->
            viewModel.setFullHistory(items)
            adapter.update(items)

            val allPositions = items.map { it.sensorPosition }.distinct()
            val adapterPosition = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allPositions)
            positionInput.setAdapter(adapterPosition)
        }

        viewModel.importFirestoreToRoom()




        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        filterButton.setOnClickListener {
            viewModel.observeFilteredHistory(
                position = positionInput.text.toString(),
                serial = serialInput.text.toString(),
                dateFrom = dateFrom,
                dateTo = dateTo
            ).observe(viewLifecycleOwner) { filtered ->
                adapter.update(filtered)
            }
        }



        val dateFromButton = view.findViewById<Button>(R.id.date_from_button)
        val dateToButton = view.findViewById<Button>(R.id.date_to_button)

        dateFromButton.setOnClickListener {
            val picker = DatePickerDialog(requireContext())
            picker.setOnDateSetListener { _, y, m, d ->
                val cal = Calendar.getInstance()
                cal.set(y, m, d, 0, 0, 0)
                dateFrom = cal.time
                dateFromButton.text = "С: ${sdf.format(dateFrom!!)}"
            }
            picker.show()
        }

        dateToButton.setOnClickListener {
            val picker = DatePickerDialog(requireContext())
            picker.setOnDateSetListener { _, y, m, d ->
                val cal = Calendar.getInstance()
                cal.set(y, m, d, 23, 59, 59)
                dateTo = cal.time
                dateToButton.text = "ПО: ${sdf.format(dateTo!!)}"
            }
            picker.show()
        }
    }
}

