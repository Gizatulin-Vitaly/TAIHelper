package com.reftgres.taihelper.ui.converter

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.reftgres.taihelper.R
import kotlinx.coroutines.launch

@HiltViewModel
class ConverterViewModel @Inject constructor() : ViewModel() {

    private val _isVoltagePrimary = MutableLiveData<Boolean>().apply { value = true }
    val isVoltagePrimary: LiveData<Boolean> = _isVoltagePrimary

    private val _isPrimarySelected = MutableLiveData<Boolean>().apply { value = true }
    val isPrimarySelected: LiveData<Boolean> = _isPrimarySelected

    private val _result = MutableLiveData<Double>()
    val result: LiveData<Double> = _result

    private val _currentRange = MutableLiveData<Pair<Float, Float>>().apply { value = Pair(0f, 5f) } // По умолчанию - 0-5 мА
    val currentRange: LiveData<Pair<Float, Float>> = _currentRange

    fun selectVoltagePrimary(isPrimary: Boolean) {
        _isVoltagePrimary.value = isPrimary
    }

    fun selectPrimary(isPrimary: Boolean) {
        _isPrimarySelected.value = isPrimary
    }

    fun selectCurrentRange(rangeType: Int) {
        _currentRange.value = if (rangeType == 0) Pair(0f, 5f) else Pair(4f, 20f)
    }



    fun calculateResult(userValue: Float, startScaleSens: Float, endScaleSens: Float) {
        val isPrimary = _isPrimarySelected.value ?: true
        val isVoltagePrimary = _isVoltagePrimary.value ?: true

        val (min, max) = if (isVoltagePrimary) Pair(0f, 5f) else Pair(4f, 20f)

        if (max == min) {
            return
        }

        val resultValue = if (isPrimary) {
            ((userValue - startScaleSens) * (max - min)) / (endScaleSens - startScaleSens) + min
        } else {
            ((userValue - min) * (endScaleSens - startScaleSens)) / (max - min) + startScaleSens
        }

        _result.value = resultValue.toDouble()
    }





}


