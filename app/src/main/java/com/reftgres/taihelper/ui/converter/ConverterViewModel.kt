package com.reftgres.taihelper.ui.converter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val converterUseCase: ConverterUseCase
) : ViewModel() {

    private val _conversionResult = MutableLiveData<Double>()
    val conversionResult: LiveData<Double> get() = _conversionResult

    fun convert(value: Double) {
        _conversionResult.value = converterUseCase.convert(value)
    }
}
