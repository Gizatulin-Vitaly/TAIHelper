package com.reftgres.taihelper.ui.sensors

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.reftgres.taihelper.R

/**
 * Адаптер для выпадающих списков с объектами, имеющими ID и имя
 */
class DropdownAdapter<T : DropdownAdapter.DropdownItem>(
    context: Context,
    private val items: List<T>
) : ArrayAdapter<T>(context, R.layout.item_dropdown, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown, parent, false)

        val item = items[position]
        val textView = view.findViewById<TextView>(R.id.text1)
        textView.text = item.name

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }

    /**
     * Интерфейс для объектов, которые могут отображаться в выпадающем списке
     */
    interface DropdownItem {
        val id: String
        val name: String
    }
}