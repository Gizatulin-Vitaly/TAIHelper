package com.reftgres.taihelper.ui.converter

import javax.inject.Inject

class ConverterRepository @Inject constructor() {
    fun convertMeasurement(value: Double): Double {
        return value * 3.1415
    }
}