package com.reftgres.taihelper.ui.oxygennew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NewMeasurementsViewModel : ViewModel() {
    // Входные данные
    private val _blockNumber = MutableLiveData(1)
    val blockNumber: LiveData<Int> = _blockNumber

    private val _measurementDate = MutableLiveData<String>()
    val measurementDate: LiveData<String> = _measurementDate

    // Вычисляемое свойство для названий датчиков
    private val _sensorTitles = MutableLiveData<List<String>>()
    val sensorTitles: LiveData<List<String>> = _sensorTitles

    // Структурированные данные датчиков
    private val _sensorsData = MutableLiveData<Map<String, SensorData>>()
    val sensorsData: LiveData<Map<String, SensorData>> = _sensorsData

    // Модель данных
    data class SensorData(
        val title: String,
        val panel: String = "",
        val testo: String = "",
        val correction: String = ""
    )

    // Первоначальная инициализация
    init {
        updateSensorTitles(1)
    }

    // Методы для обновления данных
    fun setBlockNumber(number: Int) {
        _blockNumber.value = number
        updateSensorTitles(number)
    }

    fun setMeasurementDate(date: String) {
        _measurementDate.value = date
    }

    private fun updateSensorTitles(blockNumber: Int) {
        val titles = when (blockNumber) {
            in 1..6 -> listOf("К-601", "К-602", "К-603", "К-604")
            in 7..10 -> listOf("К-603", "К-604", "К-605", "К-606")
            else -> listOf("К-601", "К-602", "К-603", "К-604")
        }
        _sensorTitles.value = titles

        // Инициализация пустых данных датчиков с новыми заголовками
        _sensorsData.value = titles.associateWith { SensorData(it) }
    }

    fun updateSensorData(index: Int, panel: String, testo: String, correction: String) {
        val currentSensors = _sensorsData.value?.toMutableMap() ?: return
        val currentTitles = _sensorTitles.value ?: return

        if (index < currentTitles.size) {
            val title = currentTitles[index]
            currentSensors[title] = SensorData(title, panel, testo, correction)
            _sensorsData.value = currentSensors
        }
    }

    // Метод для сохранения данных в Firestore
    fun saveMeasurements(): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        // Здесь логика сохранения в Firestore
        // ...

        result.value = true // или false в случае ошибки
        return result
    }
}