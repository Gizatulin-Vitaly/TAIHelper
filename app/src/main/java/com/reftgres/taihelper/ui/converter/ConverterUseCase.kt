package com.reftgres.taihelper.ui.converter

import javax.inject.Inject

class ConverterUseCase @Inject constructor(
    private val repository: ConverterRepository
) {
    fun convert(value: Double): Double {
        return repository.convertMeasurement(value)
    }
}
