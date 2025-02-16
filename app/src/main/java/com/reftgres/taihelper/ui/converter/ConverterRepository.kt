package com.reftgres.taihelper.ui.converter

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConverterRepository @Inject constructor() {
    fun converter(value: Double): Double {
        return value * 3.1415
    }
}