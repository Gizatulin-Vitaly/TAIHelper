package com.reftgres.taihelper.ui.ajk

import java.util.Date

data class DataAjk(
    val id: String = "",
    val labSensorValues: List<String>,
    val testSensorValues: List<String>,
    val labAverage: Float,
    val testAverage: Float,
    val resistance: String,
    val constant: Float,
    val r02Resistance: Float,
    val r02I: String,
    val r02SensorValue: String,
    val r05Resistance: Float,
    val r05I: String,
    val r05SensorValue: String,
    val r08Resistance: Float,
    val r08I: String,
    val r08SensorValue: String,
    val r40DegResistance: Float,
    val r40DegSensorValue: String,
    val timestamp: Date? = null,
    val userId: String
)