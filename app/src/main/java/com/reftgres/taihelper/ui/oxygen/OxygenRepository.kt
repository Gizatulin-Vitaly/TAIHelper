package com.reftgres.taihelper.ui.oxygen

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.SensorEntity
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OxygenRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sensorDao: SensorDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkService: NetworkConnectivityService,
    private val syncManager: SyncManager
) {

    private val gson = Gson()
    private val TAG = "OxygenRepository"

    /**
     * Загружает блоки из Firestore или из кэша при отсутствии сети
     */
    suspend fun loadBlocks(): List<Block> {
        return if (networkService.isNetworkAvailable()) {
            // Загружаем данные из Firestore
            try {
                val snapshot = firestore.collection("blocks").get().await()
                val blocksList = snapshot.documents.flatMap { doc ->
                    doc.get("block") as? List<String> ?: emptyList()
                }.mapIndexed { index, name -> Block(id = index, name = "Блок: ${name}") }

                blocksList
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке блоков из Firestore", e)
                // В случае ошибки возвращаем пустой список
                emptyList()
            }
        } else {
            Log.d(TAG, "Нет сети, используем локальные данные")
            // Возвращаем пустой список, если нет кэшированных данных
            emptyList()
        }
    }

    /**
     * Загружает датчики для выбранного блока из Firestore или из локальной базы данных
     */
    fun loadSensorsForBlock(blockId: Int): Flow<List<Sensor>> = flow {
        Log.d(TAG, "loadSensorsForBlock: Загружаем датчики для блока $blockId")

        // Простой подход: сначала всегда пытаемся загрузить из Firestore
        if (networkService.isNetworkAvailable()) {
            try {
                val blockPath = "/blocks/$blockId"
                Log.d(TAG, "loadSensorsForBlock: Прямой запрос к Firestore по пути: $blockPath")

                val snapshot = firestore.collection("sensors")
                    .whereEqualTo("block", blockPath)
                    .get()
                    .await()

                Log.d(TAG, "loadSensorsForBlock: Получено ${snapshot.documents.size} документов из Firestore")

                val sensorsList = snapshot.documents.map { doc ->
                    val sensor = Sensor(
                        id = doc.id,
                        position = doc.getString("position") ?: "Без позиции",
                        serialNumber = doc.getString("serial_number") ?: "Нет номера",
                        midPoint = doc.getString("mid_point") ?: "-",
                        outputScale = doc.getString("output_scale") ?: "-",
                        blockId = blockId.toString()
                    )

                    // Сохраняем в локальную базу данных для будущего использования
                    try {
                        val sensorEntity = SensorEntity(
                            id = sensor.id,
                            position = sensor.position,
                            serialNumber = sensor.serialNumber,
                            midPoint = sensor.midPoint,
                            outputScale = sensor.outputScale,
                            blockId = sensor.blockId,
                            type = doc.getString("type")?.substringAfterLast("/") ?: "",
                            measurementStartId = doc.getString("measurement_start")?.substringAfterLast("/") ?: "",
                            measurementEndId = doc.getString("measurement_end")?.substringAfterLast("/") ?: "",
                            modification = doc.getString("modification") ?: "",
                            isSynced = true
                        )

                        withContext(Dispatchers.IO) {
                            sensorDao.insertSensor(sensorEntity)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "loadSensorsForBlock: Ошибка при сохранении в БД: ${e.message}", e)
                    }

                    sensor
                }

                Log.d(TAG, "loadSensorsForBlock: Эмитим ${sensorsList.size} датчиков из Firestore")
                emit(sensorsList)

            } catch (e: Exception) {
                Log.e(TAG, "loadSensorsForBlock: Ошибка при загрузке из Firestore: ${e.message}", e)

                // Если не удалось загрузить из Firestore, пробуем загрузить из локальной БД
                try {
                    val localSensors = withContext(Dispatchers.IO) {
                        sensorDao.getSensorsByBlock(blockId.toString())
                    }

                    Log.d(TAG, "loadSensorsForBlock: Найдено ${localSensors.size} датчиков в локальной БД")

                    if (localSensors.isNotEmpty()) {
                        val mappedSensors = localSensors.map { entity ->
                            Sensor(
                                id = entity.id,
                                position = entity.position,
                                serialNumber = entity.serialNumber,
                                midPoint = entity.midPoint,
                                outputScale = entity.outputScale,
                                blockId = entity.blockId
                            )
                        }
                        emit(mappedSensors)
                    } else {
                        emit(emptyList())
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "loadSensorsForBlock: Ошибка при загрузке из локальной БД: ${e2.message}", e2)
                    emit(emptyList())
                }
            }
        } else {
            // Если нет сети, пробуем загрузить из локальной БД
            try {
                val localSensors = withContext(Dispatchers.IO) {
                    sensorDao.getSensorsByBlock(blockId.toString())
                }

                Log.d(TAG, "loadSensorsForBlock (offline): Найдено ${localSensors.size} датчиков в локальной БД")

                if (localSensors.isNotEmpty()) {
                    val mappedSensors = localSensors.map { entity ->
                        Sensor(
                            id = entity.id,
                            position = entity.position,
                            serialNumber = entity.serialNumber,
                            midPoint = entity.midPoint,
                            outputScale = entity.outputScale,
                            blockId = entity.blockId
                        )
                    }
                    emit(mappedSensors)
                } else {
                    emit(emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSensorsForBlock (offline): Ошибка при загрузке из локальной БД: ${e.message}", e)
                emit(emptyList())
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Получает датчик по ID из локальной базы данных или из Firestore
     */
    suspend fun getSensorById(sensorId: String): Sensor? {
        Log.d(TAG, "Получаем датчик по ID: $sensorId")

        // Сначала пытаемся получить из локальной базы данных
        val localSensor = withContext(Dispatchers.IO) {
            sensorDao.getSensorById(sensorId)
        }

        if (localSensor != null) {
            Log.d(TAG, "Датчик найден в локальной базе данных")
            return Sensor(
                id = localSensor.id,
                position = localSensor.position,
                serialNumber = localSensor.serialNumber,
                midPoint = localSensor.midPoint,
                outputScale = localSensor.outputScale,
                blockId = localSensor.blockId
            )
        }

        // Если не найден локально и сеть доступна, пытаемся получить из Firestore
        if (networkService.isNetworkAvailable()) {
            try {
                Log.d(TAG, "Пытаемся получить датчик из Firestore")
                val doc = firestore.collection("sensors").document(sensorId).get().await()
                if (doc.exists()) {
                    val blockPath = doc.getString("block") ?: ""
                    val blockId = blockPath.substringAfterLast("/", "")

                    val sensor = Sensor(
                        id = doc.id,
                        position = doc.getString("position") ?: "Без позиции",
                        serialNumber = doc.getString("serial_number") ?: "Нет номера",
                        midPoint = doc.getString("mid_point") ?: "-",
                        outputScale = doc.getString("output_scale") ?: "-",
                        blockId = blockId
                    )

                    // Сохраняем в локальную базу данных
                    val sensorEntity = SensorEntity(
                        id = sensor.id,
                        position = sensor.position,
                        serialNumber = sensor.serialNumber,
                        midPoint = sensor.midPoint,
                        outputScale = sensor.outputScale,
                        blockId = sensor.blockId,
                        type = doc.getString("type")?.substringAfterLast("/") ?: "",
                        measurementStartId = doc.getString("measurement_start")?.substringAfterLast("/") ?: "",
                        measurementEndId = doc.getString("measurement_end")?.substringAfterLast("/") ?: "",
                        modification = doc.getString("modification") ?: "",
                        isSynced = true
                    )

                    withContext(Dispatchers.IO) {
                        sensorDao.insertSensor(sensorEntity)
                    }

                    Log.d(TAG, "Датчик получен из Firestore и сохранен локально")
                    return sensor
                } else {
                    Log.d(TAG, "Датчик не найден в Firestore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении датчика из Firestore", e)
            }
        } else {
            Log.d(TAG, "Нет сети, датчик не найден локально")
        }

        return null
    }

    /**
     * Сохраняет новое измерение для датчика
     * @param sensorId идентификатор датчика
     * @param measurementData данные измерения
     * @return результат операции
     */
    suspend fun saveMeasurement(sensorId: String, measurementData: Map<String, Any>): Result<String> {
        Log.d(TAG, "Сохранение измерения для датчика $sensorId")

        val measurementId = java.util.UUID.randomUUID().toString()
        val isSynced = networkService.isNetworkAvailable()

        return try {
            // Если сеть доступна, сразу сохраняем в Firestore
            if (isSynced) {
                try {
                    val dataWithTimestamp = measurementData.toMutableMap().apply {
                        this["timestamp"] = com.google.firebase.Timestamp.now()
                        this["sensorId"] = sensorId
                    }

                    firestore.collection("measurements")
                        .document(measurementId)
                        .set(dataWithTimestamp)
                        .await()

                    Log.d(TAG, "Измерение сохранено в Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при сохранении измерения в Firestore", e)

                    // Добавляем в очередь синхронизации
                    val syncItem = SyncQueueEntity(
                        entityId = measurementId,
                        entityType = "measurement",
                        operation = "insert",
                        jsonData = gson.toJson(measurementData)
                    )

                    withContext(Dispatchers.IO) {
                        syncQueueDao.insertSyncItem(syncItem)
                    }

                    Log.d(TAG, "Измерение добавлено в очередь синхронизации")
                }
            } else {
                // Если сеть недоступна, добавляем в очередь синхронизации
                val syncItem = SyncQueueEntity(
                    entityId = measurementId,
                    entityType = "measurement",
                    operation = "insert",
                    jsonData = gson.toJson(measurementData)
                )

                withContext(Dispatchers.IO) {
                    syncQueueDao.insertSyncItem(syncItem)
                }

                Log.d(TAG, "Нет сети, измерение добавлено в очередь синхронизации")
            }

            // В любом случае возвращаем успех, так как данные сохранены
            // либо в Firestore, либо в очереди синхронизации
            Result.success(measurementId)
        } catch (e: Exception) {
            Log.e(TAG, "Непредвиденная ошибка при сохранении измерения", e)
            Result.failure(e)
        }
    }
}