package com.reftgres.taihelper.ui.converter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.reftgres.taihelper.R

class ConverterFragment : Fragment() {
    private lateinit var chose05: LinearLayout
    private lateinit var chose420: LinearLayout
    private lateinit var separator: View
    private lateinit var selectedVoltageText: TextView
    private var isExpanded = false
    private var currentSelected: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_converter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedVoltageText = view.findViewById(R.id.selectedVoltageText)
        chose05 = view.findViewById(R.id.chose_0_5)
        chose420 = view.findViewById(R.id.chose_4_20)
        separator = view.findViewById(R.id.separator)

        // Инициализация текущего выбранного элемента
        currentSelected = chose05
        chose05.visibility = View.VISIBLE
        chose420.visibility = View.GONE
        separator.visibility = View.GONE

        // Обработчики кликов для обоих элементов
        chose05.setOnClickListener { handleVoltageClick(it as LinearLayout) }
        chose420.setOnClickListener { handleVoltageClick(it as LinearLayout) }
    }

    private fun handleVoltageClick(clickedLayout: LinearLayout) {
        if (clickedLayout == currentSelected) {
            // Клик по текущему элементу: открыть/закрыть список
            toggleSelection()
        } else {
            // Клик по элементу списка: выбрать его
            selectVoltage(clickedLayout)
        }
    }

    private fun toggleSelection() {
        isExpanded = !isExpanded
        val alternative = if (currentSelected == chose05) chose420 else chose05
        alternative.visibility = if (isExpanded) View.VISIBLE else View.GONE
        separator.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun selectVoltage(selected: LinearLayout) {
        // Скрыть предыдущий выбранный элемент
        currentSelected?.visibility = View.GONE
        // Показать новый выбранный элемент
        selected.visibility = View.VISIBLE
        currentSelected = selected
        // Скрыть список
        isExpanded = false
        // Скрыть альтернативный элемент и separator
        val alternative = if (currentSelected == chose05) chose420 else chose05
        alternative.visibility = View.GONE
        separator.visibility = View.GONE
    }
}
