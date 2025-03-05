package com.reftgres.taihelper.ui.addsensor

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSensorViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val networkService: NetworkConnectivityService
) : ViewModel() {

    private val TAG = "AddSensorViewModel"

    // Состояния для списков выбора
    private val _blocks = MutableStateFlow<List<SensorRepository.Block>>(emptyList())
    val blocks: StateFlow<List<SensorRepository.Block>> = _blocks

    private val _measurementEnds = MutableStateFlow<List<SensorRepository.Measurement>>(emptyList())
    val measurementEnds: StateFlow<List<SensorRepository.Measurement>> = _measurementEnds

    private val _measurementStarts = MutableStateFlow<List<SensorRepository.Measurement>>(emptyList())
    val measurementStarts: StateFlow<List<SensorRepository.Measurement>> = _measurementStarts

    private val _typeSensors = MutableStateFlow<List<SensorRepository.SensorType>>(emptyList())
    val typeSensors: StateFlow<List<SensorRepository.SensorType>> = _typeSensors

    // Состояния загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Состояния ошибок
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Состояние успешного сохранения
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // Состояние сети
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    // Состояние оффлайн-сохранения
    private val _saveOffline = MutableLiveData<Boolean>(false)
    val saveOffline: LiveData<Boolean> = _saveOffline

    private val _outputRanges = MutableStateFlow<List<SensorRepository.OutputRange>>(emptyList())
    val outputRanges: StateFlow<List<SensorRepository.OutputRange>> = _outputRanges

    init {
        Log.d(TAG, "ViewModel инициализирован")

        // Наблюдаем за состоянием сети
        viewModelScope.launch {
            networkService.networkStatus.collect { isConnected ->
                _isOnline.postValue(isConnected)

                // Если сеть стала доступна, можно обновить данные
                if (isConnected) {
                    loadAllData()
                }
            }
        }

        loadAllData()
    }

    fun loadAllData() {
        Log.d(TAG, "Загрузка всех данных...")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadBlocks()
                loadMeasurementEnds()
                loadMeasurementStarts()
                loadTypeSensors()
                loadOutputRanges()
                Log.d(TAG, "Все данные успешно загружены")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке данных", e)
                _error.value = "Ошибка загрузки данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadOutputRanges() {
        val ranges = sensorRepository.getOutputRanges()
        _outputRanges.value = ranges
        Log.d(TAG, "Загружено ${ranges.size} диапазонов: ${ranges.map { it.name }}")
    }


    private suspend fun loadBlocks() {
        try {
            Log.d(TAG, "Загрузка блоков...")
            val blocksList = sensorRepository.getBlocks()
            Log.d(TAG, "Загружено ${blocksList.size} блоков: ${blocksList.map { it.name }}")

            if (blocksList.isEmpty()) {
                // Если список пуст, добавляем тестовые данные
                val defaultBlocks = listOf(
                    SensorRepository.Block("default_block_1", "Блок 1 (по умолчанию)"),
                    SensorRepository.Block("default_block_2", "Блок 2 (по умолчанию)"),
                    SensorRepository.Block("default_block_3", "Блок 3 (по умолчанию)")
                )
                Log.d(TAG, "Список блоков пуст, используем данные по умолчанию: ${defaultBlocks.map { it.name }}")
                _blocks.value = defaultBlocks
            } else {
                _blocks.value = blocksList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке блоков", e)
            // Устанавливаем данные по умолчанию при ошибке
            val errorBlocks = listOf(
                SensorRepository.Block("error_block_1", "Блок 1 (ошибка загрузки)"),
                SensorRepository.Block("error_block_2", "Блок 2 (ошибка загрузки)")
            )
            _blocks.value = errorBlocks
            _error.value = "Ошибка загрузки блоков: ${e.message}"
        }
    }

    private suspend fun loadMeasurementEnds() {
        try {
            Log.d(TAG, "Загрузка окончаний измерений...")
            val measurements = sensorRepository.getMeasurementEnds()
            Log.d(TAG, "Загружено ${measurements.size} окончаний измерений: ${measurements.map { it.name }}")

            if (measurements.isEmpty()) {
                // Если список пуст, добавляем тестовые данные
                val defaultMeasurements = listOf(
                    SensorRepository.Measurement("default_end_1", "Окончание 1 (по умолчанию)"),
                    SensorRepository.Measurement("default_end_2", "Окончание 2 (по умолчанию)")
                )
                Log.d(TAG, "Список окончаний измерений пуст, используем данные по умолчанию: ${defaultMeasurements.map { it.name }}")
                _measurementEnds.value = defaultMeasurements
            } else {
                _measurementEnds.value = measurements
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке окончаний измерений", e)
            // Устанавливаем данные по умолчанию при ошибке
            val errorMeasurements = listOf(
                SensorRepository.Measurement("error_end_1", "Окончание 1 (ошибка загрузки)"),
                SensorRepository.Measurement("error_end_2", "Окончание 2 (ошибка загрузки)")
            )
            _measurementEnds.value = errorMeasurements
            _error.value = "Ошибка загрузки окончаний измерений: ${e.message}"
        }
    }



    private suspend fun loadMeasurementStarts() {
        try {
            Log.d(TAG, "Загрузка начал измерений...")
            val measurements = sensorRepository.getMeasurementStarts()
            Log.d(TAG, "Загружено ${measurements.size} начал измерений: ${measurements.map { it.name }}")

            if (measurements.isEmpty()) {
                // Если список пуст, добавляем тестовые данные
                val defaultMeasurements = listOf(
                    SensorRepository.Measurement("default_start_1", "Начало 1 (по умолчанию)"),
                    SensorRepository.Measurement("default_start_2", "Начало 2 (по умолчанию)")
                )
                Log.d(TAG, "Список начал измерений пуст, используем данные по умолчанию: ${defaultMeasurements.map { it.name }}")
                _measurementStarts.value = defaultMeasurements
            } else {
                _measurementStarts.value = measurements
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке начал измерений", e)
            // Устанавливаем данные по умолчанию при ошибке
            val errorMeasurements = listOf(
                SensorRepository.Measurement("error_start_1", "Начало 1 (ошибка загрузки)"),
                SensorRepository.Measurement("error_start_2", "Начало 2 (ошибка загрузки)")
            )
            _measurementStarts.value = errorMeasurements
            _error.value = "Ошибка загрузки начал измерений: ${e.message}"
        }
    }

    private suspend fun loadTypeSensors() {
        try {
            Log.d(TAG, "Загрузка типов датчиков...")
            val types = sensorRepository.getSensorTypes()
            Log.d(TAG, "Загружено ${types.size} типов датчиков: ${types.map { it.name }}")

            if (types.isEmpty()) {
                // Если список пуст, добавляем тестовые данные
                val defaultTypes = listOf(
                    SensorRepository.SensorType("default_type_1", "Температура (по умолчанию)"),
                    SensorRepository.SensorType("default_type_2", "Давление (по умолчанию)"),
                    SensorRepository.SensorType("default_type_3", "Уровень (по умолчанию)")
                )
                Log.d(TAG, "Список типов датчиков пуст, используем данные по умолчанию: ${defaultTypes.map { it.name }}")
                _typeSensors.value = defaultTypes
            } else {
                _typeSensors.value = types
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке типов датчиков", e)
            // Устанавливаем данные по умолчанию при ошибке
            val errorTypes = listOf(
                SensorRepository.SensorType("error_type_1", "Температура (ошибка загрузки)"),
                SensorRepository.SensorType("error_type_2", "Давление (ошибка загрузки)")
            )
            _typeSensors.value = errorTypes
            _error.value = "Ошибка загрузки типов датчиков: ${e.message}"
        }
    }


    fun saveSensor(
        typeId: String,
        position: String,
        outputScale: String,
        midPoint: String,
        modification: String,
        blockId: String,
        measurementStartId: String,
        measurementEndId: String
    ) {
        Log.d(TAG, "Сохранение датчика с typeId=$typeId, blockId=$blockId")

        if (typeId.isBlank()) {
            Log.w(TAG, "Тип датчика не указан")
            _error.value = "Тип датчика обязателен для заполнения"
            return
        }

        _isLoading.value = true
        _saveOffline.value = false // Сбрасываем статус оффлайн-сохранения

        viewModelScope.launch {
            try {
                val result = sensorRepository.saveSensor(
                    typeId = typeId,
                    position = position,
                    outputScale = outputScale,
                    midPoint = midPoint,
                    modification = modification,
                    blockId = blockId,
                    measurementStartId = measurementStartId,
                    measurementEndId = measurementEndId
                )

                result.fold(
                    onSuccess = { id ->
                        Log.d(TAG, "Датчик успешно сохранен с ID: $id")
                        _isLoading.value = false
                        _saveSuccess.value = true

                        // Если сейчас оффлайн-режим, информируем пользователя
                        if (!networkService.isNetworkAvailable()) {
                            _saveOffline.value = true
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Ошибка сохранения датчика", e)
                        _isLoading.value = false
                        _error.value = "Ошибка сохранения датчика: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Необработанное исключение при сохранении датчика", e)
                _isLoading.value = false
                _error.value = "Непредвиденная ошибка: ${e.message}"
            }
        }
    }

    // Метод для сброса состояния ошибки
    fun resetError() {
        _error.value = ""
    }

    // Метод для сброса состояния успешного сохранения
    fun resetSaveSuccess() {
        _saveSuccess.value = false
        _saveOffline.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel уничтожена")
    }
}