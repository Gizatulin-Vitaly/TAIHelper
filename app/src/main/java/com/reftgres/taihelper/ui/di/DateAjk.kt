package com.reftgres.taihelper.ui.ajk

import java.util.Date

/**
 * Модель данных для хранения информации о калибровке
 */
data class DataAjk(
    val id: String = "", // ID документа в Firestore
    val labSensorValues: List<String> = listOf(), // Значения лабораторного датчика
    val testSensorValues: List<String> = listOf(), // Значения поверяемого датчика
    val labAverage: Float = 0f, // Среднее значение лабораторного датчика
    val testAverage: Float = 0f, // Среднее значение поверяемого датчика
    val resistance: String = "", // Значение сопротивления
    val constant: Float = 0f, // Константа (resistance * labAverage)

    // Данные для R = константа / 0.2
    val r02Resistance: Float = 0f,
    val r02I: String = "",
    val r02SensorValue: String = "",

    // Данные для R = константа / 0.5
    val r05Resistance: Float = 0f,
    val r05I: String = "",
    val r05SensorValue: String = "",

    // Данные для R = константа / 0.8
    val r08Resistance: Float = 0f,
    val r08I: String = "",
    val r08SensorValue: String = "",

    // Данные для R при t=40°C
    val r40DegResistance: Float = 0f,
    val r40DegSensorValue: String = "",

    val timestamp: Date = Date(), // Время создания записи
    val userId: String = "" // ID пользователя
)