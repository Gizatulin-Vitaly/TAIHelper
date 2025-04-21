package com.reftgres.taihelper.ui.oxygen

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reftgres.taihelper.service.NetworkConnectivityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OxygenMeasurementViewModel @Inject constructor(
    private val repository: OxygenRepository,
    private val networkService: NetworkConnectivityService
) : ViewModel() {

    // LiveData для блоков
    private val _blocks = MutableLiveData<List<Block>>()
    val blocks: LiveData<List<Block>> = _blocks

    // LiveData для датчиков, связанных с выбранным блоком
    private val _sensors = MutableLiveData<List<Sensor>>()
    val sensors: LiveData<List<Sensor>> = _sensors

    // LiveData для выбранного датчика
    private val _selectedSensor = MutableLiveData<Sensor?>()
    val selectedSensor: LiveData<Sensor?> = _selectedSensor

    // LiveData для состояния сети
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    // LiveData для состояния загрузки
    private val _isLoading = MutableLiveData<Boolean>(false)

    // LiveData для сообщений об ошибках
    private val _errorMessage = MutableLiveData<String>()

    // LiveData для последних измерений
    private val _latestMeasurements = MutableLiveData<List<LatestMeasurement>>()
    val latestMeasurements: LiveData<List<LatestMeasurement>> = _latestMeasurements

    // Текущий выбранный блок
    private var currentBlockId: Int = 0

    // Добавить новый LiveData для истории измерений конкретного датчика
    private val _sensorMeasurementHistory = MutableLiveData<List<LatestMeasurement>>()
    val sensorMeasurementHistory: LiveData<List<LatestMeasurement>> = _sensorMeasurementHistory

    // LiveData для списка всех последних измерений
    private val _allMeasurements = MutableLiveData<List<LatestMeasurement>>()
    val allMeasurements: LiveData<List<LatestMeasurement>> = _allMeasurements

    private var sortedSensorsCache: List<Sensor> = emptyList()

    fun setSortedSensors(sensors: List<Sensor>) {
        sortedSensorsCache = sensors
    }

    fun getSortedSensors(): List<Sensor> = sortedSensorsCache

    // Метод для загрузки истории измерений для конкретного датчика
    // В OxygenMeasurementViewModel.kt
    fun loadSensorMeasurementHistory(sensorPosition: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "⭐ Загрузка истории для датчика $sensorPosition в блоке $currentBlockId")

                // Очищаем предыдущие результаты, чтобы показать загрузку
                _sensorMeasurementHistory.postValue(emptyList())

                repository.loadSensorMeasurementHistory(
                    blockId = currentBlockId,
                    sensorPosition = sensorPosition
                ).collect { measurements ->
                    Log.d(TAG, "⭐ Получено ${measurements.size} измерений для датчика $sensorPosition")

                    if (measurements.isEmpty()) {
                        Log.d(TAG, "⭐ Измерения не найдены для датчика $sensorPosition")
                    } else {
                        measurements.forEach { measurement ->
                            Log.d(TAG, "⭐ Измерение: ${measurement.date}, датчиков: ${measurement.sensors.size}")
                        }
                    }

                    _sensorMeasurementHistory.postValue(measurements)
                }
            } catch (e: Exception) {
                Log.e(TAG, "⭐ Ошибка при загрузке истории измерений датчика", e)
                _sensorMeasurementHistory.postValue(emptyList())
            }
        }
    }

    // Метод для загрузки последних 10 измерений
    fun loadLastTenMeasurements() {

        viewModelScope.launch {
            try {
                Log.d(TAG, "⭐ Загрузка последних 10 измерений")
                _isLoading.value = true

                repository.loadLastTenMeasurements().collect { measurements ->
                    Log.d(TAG, "⭐ Получено ${measurements.size} измерений")
                    _allMeasurements.postValue(measurements)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "⭐ Ошибка при загрузке измерений: ${e.message}", e)
                _allMeasurements.postValue(emptyList())
                _isLoading.value = false
            }
        }
    }

    // Информация о текущих позициях датчиков для каждого блока
    private val sensorPositionsMap = mapOf(
        1 to listOf("К-601", "К-602", "К-603", "К-604"),
        2 to listOf("К-601", "К-602", "К-603", "К-604"),
        3 to listOf("К-601", "К-602", "К-603", "К-604"),
        4 to listOf("К-601", "К-602", "К-603", "К-604"),
        5 to listOf("К-601", "К-602", "К-603", "К-604"),
        6 to listOf("К-601", "К-602", "К-603", "К-604"),
        7 to listOf("К-603", "К-604", "К-605", "К-606"),
        8 to listOf("К-603", "К-604", "К-605", "К-606"),
        9 to listOf("К-603", "К-604", "К-605", "К-606"),
        10 to listOf("К-603", "К-604", "К-605", "К-606")
    )

    /**
     * Загружает последние измерения для блока
     */
    fun loadLatestMeasurements(blockId: Int) {
        Log.d(TAG, "⭐ loadLatestMeasurements для блока $blockId")
        currentBlockId = blockId

        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.loadLatestMeasurements(blockId).collect { measurements ->
                    Log.d(TAG, "⭐ Получено ${measurements.size} измерений для блока $blockId")

                    _latestMeasurements.postValue(measurements)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "⭐ Ошибка при загрузке измерений: ${e.message}", e)
                _latestMeasurements.postValue(emptyList())
                _isLoading.value = false
            }
        }
    }

    // Метод для получения позиций датчиков для выбранного блока
    fun getSensorPositions(blockId: Int): List<String> {
        return sensorPositionsMap[blockId] ?: listOf("К-601", "К-602", "К-603", "К-604")
    }

    // Загрузка списка блоков при инициализации
    init {
        Log.d("OxygenViewModel", "ViewModel created, loading blocks...")

        // Наблюдаем за состоянием сети
        viewModelScope.launch {
            networkService.networkStatus.collect { isConnected ->
                _isOnline.postValue(isConnected)

                // Если сеть стала доступна, можно инициировать синхронизацию
                if (isConnected) {
                    // Обновляем данные, если они уже были загружены
                    if (_blocks.value != null) {
                        loadBlocks()
                    }
                    if (_sensors.value != null && _selectedSensor.value != null) {
                        loadSensorsForBlock(_selectedSensor.value!!.blockId.toInt())
                    }
                }
            }
        }

        loadBlocks()

        loadLatestMeasurements(currentBlockId)
    }

    // Загрузка блоков
    private fun loadBlocks() {
        Log.d("OxygenViewModel", "loadBlocks() called")
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val blocksList = repository.loadBlocks()
                Log.d("OxygenViewModel", "Blocks received: $blocksList")
                _blocks.value = blocksList
                _isLoading.value = false
            } catch (e: Exception) {
                // Обработка ошибок
                Log.e("OxygenViewModel", "Error loading blocks", e)
                e.printStackTrace()
                _errorMessage.value = "Ошибка загрузки блоков: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadSensorsForBlock(blockId: Int) {
        Log.d("OxygenViewModel", "loadSensorsForBlock() called with blockId: $blockId")
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.loadSensorsForBlock(blockId).collect { sensorsList ->
                    Log.d("OxygenViewModel", "Sensors received: $sensorsList")
                    _sensors.postValue(sensorsList)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("OxygenViewModel", "Error loading sensors", e)
                e.printStackTrace()
                _errorMessage.value = "Ошибка загрузки датчиков: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Выбор датчика по позиции
    fun selectSensorByPosition(position: String) {
        _sensors.value?.find { it.position == position }?.let { sensor ->
            _selectedSensor.value = sensor
        } ?: run {
            _errorMessage.value = "Датчик с позицией $position не найден"
        }
    }

    fun getCurrentBlockId(): Int {
        return currentBlockId
    }
}