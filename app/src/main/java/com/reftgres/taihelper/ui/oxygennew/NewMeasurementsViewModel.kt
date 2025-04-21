package com.reftgres.taihelper.ui.oxygennew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reftgres.taihelper.ui.model.MeasurementRecord
import com.reftgres.taihelper.ui.model.SensorMeasurement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NewMeasurementsViewModel @Inject constructor(
    private val measurementsRepository: MeasurementsRepository
) : ViewModel() {

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


    fun saveMeasurementsWithOfflineSupport(): LiveData<SaveResult> {
        val result = MutableLiveData<SaveResult>()
        result.value = SaveResult.Loading

        viewModelScope.launch {
            try {
                val currentBlockNumber = blockNumber.value ?: 1
                val currentDate = measurementDate.value ?: ""
                val currentSensorsData = sensorsData.value ?: mapOf()

                // Формируем объект измерения
                val sensorsList = currentSensorsData.map { (title, data) ->
                    SensorMeasurement(
                        sensorTitle = title,
                        panelValue = data.panel,
                        testoValue = data.testo,
                        correctionValue = data.correction
                    )
                }

                val measurementRecord = MeasurementRecord(
                    id = UUID.randomUUID().toString(),
                    blockNumber = currentBlockNumber,
                    date = currentDate,
                    timestamp = System.currentTimeMillis(),
                    sensors = sensorsList
                )

                // Формируем список обновлений для датчиков
                val blockReference = currentBlockNumber.toString()


                // Создаем список обновлений датчиков используя тип из репозитория
                val sensorUpdates = currentSensorsData.map { (title, data) ->
                    MeasurementsRepository.SensorUpdate(
                        blockReference = blockReference,
                        position = title,
                        midpointValue = data.correction
                    )
                }

                // Вызываем метод с поддержкой офлайн-режима
                val saveResult = measurementsRepository.saveMeasurementOffline(
                    measurementRecord,
                    sensorUpdates
                )

                if (saveResult.isSuccess) {
                    val id = saveResult.getOrThrow()
                    if (id.startsWith("offline_")) {
                        // Данные сохранены офлайн
                        result.value = SaveResult.OfflineSuccess(id, "Данные сохранены офлайн и будут синхронизированы при подключении к сети")
                    } else {
                        // Данные сохранены онлайн
                        result.value = SaveResult.Success(id)
                    }
                } else {
                    // Ошибка сохранения
                    result.value = SaveResult.Error(
                        saveResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    )
                }
            } catch (e: Exception) {
                result.value = SaveResult.Error(e.message ?: "Неизвестная ошибка")
            }
        }

        return result
    }
}

// Класс для результатов сохранения
sealed class SaveResult {
    object Loading : SaveResult()
    data class Success(val id: String) : SaveResult()
    data class OfflineSuccess(val id: String, val message: String) : SaveResult()
    data class PartialSuccess(val id: String, val message: String) : SaveResult()
    data class Error(val message: String) : SaveResult()
}
