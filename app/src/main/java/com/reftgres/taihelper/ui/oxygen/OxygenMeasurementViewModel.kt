package com.reftgres.taihelper.ui.oxygen

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.reftgres.taihelper.service.NetworkConnectivityService
// Импортируем модели
import com.reftgres.taihelper.ui.oxygen.Sensor
import com.reftgres.taihelper.ui.oxygen.Block
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OxygenMeasurementViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val repository: OxygenRepository,
    private val networkService: NetworkConnectivityService
) : ViewModel() {
    // Удалите определения Sensor и Block отсюда!

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
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData для сообщений об ошибках
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

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

    // Выбор датчика по его ID
    fun selectSensor(sensorId: String) {
        // Сначала проверяем, есть ли датчик в текущем списке
        _sensors.value?.find { it.id == sensorId }?.let { sensor ->
            _selectedSensor.value = sensor
            return
        }

        // Если не найден в текущем списке, запрашиваем из репозитория
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val sensor = repository.getSensorById(sensorId)
                _selectedSensor.postValue(sensor)
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("OxygenViewModel", "Error selecting sensor", e)
                _errorMessage.value = "Ошибка выбора датчика: ${e.message}"
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

    // Метод для сброса ошибки
    fun resetError() {
        _errorMessage.value = ""
    }

    // Метод для принудительного обновления данных
    fun refreshData() {
        if (networkService.isNetworkAvailable()) {
            loadBlocks()
            _selectedSensor.value?.let { sensor ->
                try {
                    loadSensorsForBlock(sensor.blockId.toInt())
                } catch (e: Exception) {
                    Log.e("OxygenViewModel", "Error parsing blockId", e)
                }
            }
        } else {
            _errorMessage.value = "Нет подключения к интернету. Используются кэшированные данные."
        }
    }

    // Сохранение нового измерения (если такая функциональность нужна)
    fun saveMeasurement(measurement: Map<String, Any>) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Вызов метода репозитория для сохранения измерения
                // repository.saveMeasurement(measurement)

                _isLoading.value = false
                // Успешное сохранение
            } catch (e: Exception) {
                Log.e("OxygenViewModel", "Error saving measurement", e)
                _errorMessage.value = "Ошибка сохранения измерения: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Получение истории измерений для датчика (если такая функциональность нужна)
    fun loadMeasurementHistory(sensorId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Вызов метода репозитория для загрузки истории измерений
                // val history = repository.getMeasurementHistory(sensorId)

                _isLoading.value = false
                // Здесь можно обновить LiveData с историей измерений
            } catch (e: Exception) {
                Log.e("OxygenViewModel", "Error loading measurement history", e)
                _errorMessage.value = "Ошибка загрузки истории измерений: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Дополнительный метод для фильтрации датчиков (если нужно)
    fun filterSensors(query: String) {
        if (query.isEmpty()) {
            // Если строка поиска пуста, загружаем все датчики для текущего блока
            _selectedSensor.value?.let {
                try {
                    loadSensorsForBlock(it.blockId.toInt())
                } catch (e: Exception) {
                    Log.e("OxygenViewModel", "Error parsing blockId", e)
                }
            }
        } else {
            // Фильтруем текущий список датчиков
            _sensors.value?.let { sensorList ->
                val filteredList = sensorList.filter {
                    it.position.contains(query, ignoreCase = true) ||
                            it.serialNumber.contains(query, ignoreCase = true)
                }
                _sensors.value = filteredList
            }
        }
    }
}