package com.reftgres.taihelper.ui.ajk

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AjkViewModel @Inject constructor(
    private val repository: AjkRepository,
    private val networkService: NetworkConnectivityService,
    private val syncManager: SyncManager
) : ViewModel() {

    // Текущий шаг
    private val _currentStep = MutableLiveData<Int>(1)
    val currentStep: Int get() = _currentStep.value ?: 1
    fun observeCurrentStep(): LiveData<Int> = _currentStep

    // Значения лабораторного датчика
    private val _labSensorValues = MutableLiveData<List<String>>(listOf("", "", ""))
    val labSensorValues: List<String> get() = _labSensorValues.value ?: listOf("", "", "")

    // Значения поверяемого датчика
    private val _testSensorValues = MutableLiveData<List<String>>(listOf("", "", ""))
    val testSensorValues: List<String> get() = _testSensorValues.value ?: listOf("", "", "")

    // Средние значения
    private val _labAverage = MutableLiveData<Float>(0f)
    fun observeLabAverage(): LiveData<Float> = _labAverage

    private val _testAverage = MutableLiveData<Float>(0f)
    fun observeTestAverage(): LiveData<Float> = _testAverage

    // Состояние сети
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    // Сопротивление и константа
    private val _resistance = MutableLiveData<String>("")
    val resistance: String get() = _resistance.value ?: ""

    private val _constant = MutableLiveData<Float>(0f)
    fun observeConstant(): LiveData<Float> = _constant

    // Значения для таблицы R = константа / 0.2
    private val _r02I = MutableLiveData<String>("")
    val r02I: String get() = _r02I.value ?: ""

    private val _r02SensorValue = MutableLiveData<String>("")
    val r02SensorValue: String get() = _r02SensorValue.value ?: ""

    // Значения для таблицы R = константа / 0.5
    private val _r05I = MutableLiveData<String>("")
    val r05I: String get() = _r05I.value ?: ""

    private val _r05SensorValue = MutableLiveData<String>("")
    val r05SensorValue: String get() = _r05SensorValue.value ?: ""

    // Значения для таблицы R = константа / 0.8
    private val _r08I = MutableLiveData<String>("")
    val r08I: String get() = _r08I.value ?: ""

    private val _r08SensorValue = MutableLiveData<String>("")
    val r08SensorValue: String get() = _r08SensorValue.value ?: ""

    // Значения для R при t=40°C
    private val _r40DegSensorValue = MutableLiveData<String>("")
    val r40DegSensorValue: String get() = _r40DegSensorValue.value ?: ""

    // Статус сохранения данных
    private val _saveStatus = MutableLiveData<SaveStatus>(SaveStatus.None)
    fun observeSaveStatus(): LiveData<SaveStatus> = _saveStatus

    // История калибровок
    private val _calibrationHistory = MutableLiveData<List<DataAjk>>(emptyList())

    init {
        // Наблюдаем за состоянием сети
        viewModelScope.launch {
            networkService.networkStatus.collect { isConnected ->
                Log.d("AjkViewModel", "Состояние сети изменилось: $isConnected")
                _isOnline.postValue(isConnected)

                // Если сеть восстановилась, можно обновить историю калибровок
                if (isConnected) {
                    Log.d("AjkViewModel", "Сеть восстановлена, запрашиваем синхронизацию")
                    syncManager.requestSync() // Явно запрашиваем синхронизацию
                    loadCalibrationHistory()
                }
            }
        }

        // Загружаем историю калибровок при инициализации
        loadCalibrationHistory()
    }

    // Загрузка истории калибровок
    private fun loadCalibrationHistory() {
        viewModelScope.launch {
            repository.getCalibrationHistory().collect { result ->
                result.fold(
                    onSuccess = { calibrations ->
                        _calibrationHistory.postValue(calibrations)
                    },
                    onFailure = { error ->
                        Log.e("AjkViewModel", "Ошибка при загрузке истории калибровок", error)
                    }
                )
            }
        }
    }

    // Обновление значений лабораторного датчика
    fun updateLabSensorValue(index: Int, value: String) {
        val currentValues = _labSensorValues.value?.toMutableList() ?: mutableListOf("", "", "")
        if (index in currentValues.indices) {
            currentValues[index] = value
            _labSensorValues.value = currentValues
            calculateAverages()
        }
    }

    // Обновление значений поверяемого датчика
    fun updateTestSensorValue(index: Int, value: String) {
        val currentValues = _testSensorValues.value?.toMutableList() ?: mutableListOf("", "", "")
        if (index in currentValues.indices) {
            currentValues[index] = value
            _testSensorValues.value = currentValues
            calculateAverages()
        }
    }

    // Расчет средних значений
    private fun calculateAverages() {
        val labValues = _labSensorValues.value?.mapNotNull { it.toFloatOrNull() } ?: listOf()
        val testValues = _testSensorValues.value?.mapNotNull { it.toFloatOrNull() } ?: listOf()

        if (labValues.size == 3) {
            _labAverage.value = labValues.sum() / labValues.size
            calculateConstant()
        }

        if (testValues.size == 3) {
            _testAverage.value = testValues.sum() / testValues.size
        }
    }

    // Обновление сопротивления
    fun updateResistance(value: String) {
        _resistance.value = value
        calculateConstant()
    }

    // Расчет константы
    private fun calculateConstant() {
        val resistanceValue = _resistance.value?.toFloatOrNull() ?: 0f
        val labAverage = _labAverage.value ?: 0f

        if (resistanceValue > 0 && labAverage > 0) {
            _constant.value = resistanceValue * labAverage
        }
    }

    fun setCurrentStep(step: Int) {
        if (step in 1..4) {
            _currentStep.value = step
        }
    }

    // Обновление значений для таблицы
    fun updateR02I(value: String) {
        _r02I.value = value
    }

    fun updateR02SensorValue(value: String) {
        _r02SensorValue.value = value
    }

    fun updateR05I(value: String) {
        _r05I.value = value
    }

    fun updateR05SensorValue(value: String) {
        _r05SensorValue.value = value
    }

    fun updateR08I(value: String) {
        _r08I.value = value
    }

    fun updateR08SensorValue(value: String) {
        _r08SensorValue.value = value
    }

    fun updateR40DegSensorValue(value: String) {
        _r40DegSensorValue.value = value
    }

    // Переход к следующему шагу
    fun nextStep() {
        val current = _currentStep.value ?: 1
        if (current < 4) {
            _currentStep.value = current + 1
        }
    }

    // Переход к предыдущему шагу
    fun previousStep() {
        val current = _currentStep.value ?: 1
        if (current > 1) {
            _currentStep.value = current - 1
        }
    }

    // Убедимся, что reset_data полностью сбрасывает статус
    fun resetData() {
        _labSensorValues.value = listOf("", "", "")
        _testSensorValues.value = listOf("", "", "")
        _labAverage.value = 0f
        _testAverage.value = 0f
        _resistance.value = ""
        _constant.value = 0f
        _r02I.value = ""
        _r02SensorValue.value = ""
        _r05I.value = ""
        _r05SensorValue.value = ""
        _r08I.value = ""
        _r08SensorValue.value = ""
        _r40DegSensorValue.value = ""
        _currentStep.value = 1
        _saveStatus.value = SaveStatus.None
    }

    // Проверка возможности перехода к следующему шагу
    fun canProceedToNextStep(): Boolean {
        return when (_currentStep.value) {
            1 -> {
                val labValues = _labSensorValues.value
                val testValues = _testSensorValues.value

                if (labValues == null || testValues == null) return false

                labValues.all { it.isNotEmpty() } && testValues.all { it.isNotEmpty() }
            }
            2 -> _resistance.value?.isNotEmpty() == true
            3 -> _r02I.value?.isNotEmpty() == true && _r02SensorValue.value?.isNotEmpty() == true &&
                    _r05I.value?.isNotEmpty() == true && _r05SensorValue.value?.isNotEmpty() == true &&
                    _r08I.value?.isNotEmpty() == true && _r08SensorValue.value?.isNotEmpty() == true
            else -> true
        }
    }

    // Сохранение данных
    fun saveToCloud() {
        // Если уже идет сохранение, игнорируем запрос
        if (_saveStatus.value is SaveStatus.Saving) {
            Log.d("AjkViewModel", "Сохранение уже в процессе, игнорируем запрос")
            return
        }

        // Устанавливаем статус "Сохранение"
        _saveStatus.value = SaveStatus.Saving

        Log.d("AjkViewModel", "Начинаем сохранение, статус сети: ${if (_isOnline.value == true) "ONLINE" else "OFFLINE"}")

        // Подготовка данных калибровки
        val calibrationData = prepareCalibrationData()

        viewModelScope.launch {
            try {
                repository.saveCalibrationData(calibrationData).collect { result ->
                    // Определяем финальный статус
                    val finalStatus = result.fold(
                        onSuccess = { id ->
                            Log.d("AjkViewModel", "Успешное сохранение с ID: $id")

                            if (_isOnline.value != true) {
                                Log.d("AjkViewModel", "Сохранено в офлайн-режиме")
                                SaveStatus.OfflineSaved
                            } else {
                                Log.d("AjkViewModel", "Сохранено и синхронизировано")
                                SaveStatus.Success
                            }
                        },
                        onFailure = { error ->
                            Log.e("AjkViewModel", "Ошибка сохранения: ${error.message}")
                            SaveStatus.Error(error.message ?: "Неизвестная ошибка")
                        }
                    )

                    Log.d("AjkViewModel", "Устанавливаем итоговый статус: $finalStatus")
                    _saveStatus.postValue(finalStatus)

                    // Обновляем историю калибровок после сохранения
                    loadCalibrationHistory()
                }
            } catch (e: Exception) {
                Log.e("AjkViewModel", "Исключение при сохранении: ${e.message}")
                _saveStatus.postValue(SaveStatus.Error(e.message ?: "Неизвестная ошибка"))
            }
        }
    }

    // Подготовка данных для сохранения
    private fun prepareCalibrationData(): DataAjk {
        val constant = _constant.value ?: 0f

        return DataAjk(
            id = "", // ID будет сгенерировано в репозитории
            labSensorValues = _labSensorValues.value ?: listOf(),
            testSensorValues = _testSensorValues.value ?: listOf(),
            labAverage = _labAverage.value ?: 0f,
            testAverage = _testAverage.value ?: 0f,
            resistance = _resistance.value ?: "",
            constant = constant,
            r02Resistance = constant / 0.2f,
            r02I = _r02I.value ?: "",
            r02SensorValue = _r02SensorValue.value ?: "",
            r05Resistance = constant / 0.5f,
            r05I = _r05I.value ?: "",
            r05SensorValue = _r05SensorValue.value ?: "",
            r08Resistance = constant / 0.8f,
            r08I = _r08I.value ?: "",
            r08SensorValue = _r08SensorValue.value ?: "",
            r40DegResistance = constant / 0.68f,
            r40DegSensorValue = _r40DegSensorValue.value ?: "",
            timestamp = Date(),
            userId = getUserId()
        )
    }

    // Получение ID пользователя (заглушка)
    private fun getUserId(): String {
        // В реальном приложении здесь должна быть логика получения ID текущего пользователя
        return "user123"
    }

    // Класс для отслеживания статуса сохранения
    sealed class SaveStatus {
        object None : SaveStatus()
        object Saving : SaveStatus()
        object Success : SaveStatus()
        object OfflineSaved : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }
}