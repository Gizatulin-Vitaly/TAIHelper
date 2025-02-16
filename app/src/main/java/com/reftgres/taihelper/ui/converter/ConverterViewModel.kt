package com.reftgres.taihelper.ui.converter

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.reftgres.taihelper.R

@HiltViewModel
class ConverterViewModel @Inject constructor() : ViewModel() {

    private val _cardColor = MutableLiveData<Int>().apply { value = R.color.primary_blue }
    val cardColor: LiveData<Int> = _cardColor

    fun changeColor() {
        _cardColor.value = if (_cardColor.value == R.color.primary_blue) R.color.surface_white else R.color.primary_blue
    }
}
