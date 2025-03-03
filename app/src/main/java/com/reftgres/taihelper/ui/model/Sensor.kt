package com.reftgres.taihelper.model

import java.util.Date

data class Sensor(
    val id: String = "",
    val block: String = "",
    val lastCalibration: Date? = null,
    val measurementEnd: String = "",
    val measurementStart: String = "",
    val midPoint: String = "",
    val modification: String = "",
    val outputScale: String = "",
    val position: String = "",
    val serialNumber: String = "",
    val type: String = ""
)