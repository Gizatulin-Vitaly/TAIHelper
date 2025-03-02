package com.reftgres.taihelper.ui.oxygen

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OxygenMeasurementViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Модели данных
    data class Block(val id: Int, val name: String)
    data class Sensor(
        val id: String,
        val position: String,
        val serialNumber: String,
        val midPoint: String,
        val outputScale: String,
        val blockId: String
    )

    // LiveData для блоков
    private val _blocks = MutableLiveData<List<Block>>()
    val blocks: LiveData<List<Block>> = _blocks

    // LiveData для датчиков, связанных с выбранным блоком
    private val _sensors = MutableLiveData<List<Sensor>>()
    val sensors: LiveData<List<Sensor>> = _sensors

    // LiveData для выбранного датчика
    private val _selectedSensor = MutableLiveData<Sensor?>()
    val selectedSensor: LiveData<Sensor?> = _selectedSensor

    // Загрузка списка блоков при инициализации
    init {
        Log.d("OxygenViewModel", "ViewModel created, loading blocks...")
        loadBlocks()
    }

    // Загрузка блоков из Firestore
    private fun loadBlocks() {
        Log.d("OxygenViewModel", "loadBlocks() called")
        viewModelScope.launch {
            try {
                val blocksList = withContext(Dispatchers.IO) {
                    val snapshot = firestore.collection("blocks").get().await()
                    snapshot.documents.flatMap { doc ->
                        doc.get("block") as? List<String> ?: emptyList()
                    }.mapIndexed { index, name -> Block(id = index, name = "Блок: ${name}") }
                }
                Log.d("OxygenViewModel", "Blocks received: $blocksList")
                _blocks.value = blocksList
            } catch (e: Exception) {
                // Обработка ошибок
                Log.e("OxygenViewModel", "Error loading blocks", e)
                e.printStackTrace()
            }
        }
    }

    // Загрузка датчиков для выбранного блока
    fun loadSensorsForBlock(blockId: Int) {
        Log.d("OxygenViewModel", "loadSensorsForBlock() called with blockId: $blockId")
        viewModelScope.launch {
            try {
                val sensorsList = withContext(Dispatchers.IO) {
                    val blockPath = "blocks/block[$blockId]"
                    Log.d("OxygenViewModel", "Fetching sensors for block path: $blockPath")

                    val blockRef = firestore.document(blockPath)

                    val snapshot = firestore.collection("sensors")
                        .whereEqualTo("block", blockRef)
                        .get()
                        .await()

                    Log.d("OxygenViewModel", "Query result: ${snapshot.documents.size} sensors found")

                    snapshot.documents.map { doc ->
                        Sensor(
                            id = doc.id,
                            position = doc.getString("position") ?: "Без позиции",
                            serialNumber = doc.getString("serial_number") ?: "Нет номера",
                            midPoint = doc.getString("mid_point") ?: "-",
                            outputScale = doc.getString("output_scale") ?: "-",
                            blockId = blockId.toString() // Приводим к строке
                        )
                    }
                }
                Log.d("OxygenViewModel", "Sensors received: $sensorsList")

                _sensors.postValue(sensorsList)
            } catch (e: Exception) {
                Log.e("OxygenViewModel", "Error loading sensors", e)
            }
        }
    }




    // Выбор датчика по его ID
    fun selectSensor(sensorId: String) {
        _selectedSensor.value = _sensors.value?.find { it.id == sensorId }
    }

    // Выбор датчика по позиции
    fun selectSensorByPosition(position: String) {
        _selectedSensor.value = _sensors.value?.find { it.position == position }
    }
}