package com.reftgres.taihelper.ui.oxygen

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

    // Выбор датчика по позиции
    fun selectSensorByPosition(position: String) {
        _sensors.value?.find { it.position == position }?.let { sensor ->
            _selectedSensor.value = sensor
        } ?: run {
            _errorMessage.value = "Датчик с позицией $position не найден"
        }
    }
}