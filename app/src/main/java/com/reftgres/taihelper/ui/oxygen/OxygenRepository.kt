package com.reftgres.taihelper.ui.oxygen

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.SensorEntity
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import com.reftgres.taihelper.ui.model.SensorMeasurement
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
            try {
                val snapshot = firestore.collection("blocks").get().await()
                val blocksList = snapshot.documents.flatMapIndexed { index, doc ->
                    val blockData = doc.get("block") as? List<String> ?: emptyList()
                    blockData.mapIndexed { dataIndex, name ->
                        Block(id = index * blockData.size + dataIndex + 1, name = "Блок ${name}")
                    }
                }

                if (blocksList.isEmpty()) {
                    (1..10).map { Block(id = it, name = "Блок $it") }
                } else {
                    blocksList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке блоков из Firestore", e)
                (1..10).map { Block(id = it, name = "Блок $it") }
            }
        } else {
            Log.d(TAG, "Нет сети, используем тестовые данные")
            (1..10).map { Block(id = it, name = "Блок $it") }
        }
    }

    /**
     * Загружает датчики для выбранного блока из Firestore или из локальной базы данных
     */
    fun loadSensorsForBlock(blockId: Int): Flow<List<Sensor>> = flow {
        Log.d(TAG, "loadSensorsForBlock: Загружаем датчики для блока $blockId")

        if (networkService.isNetworkAvailable()) {
            try {
                val blockPath = "$blockId"
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

    // В OxygenRepository.kt
    fun loadSensorMeasurementHistory(blockId: Int, sensorPosition: String): Flow<List<LatestMeasurement>> = flow {
        Log.d(TAG, "⭐ Загрузка истории для блока $blockId и датчика $sensorPosition")

        if (networkService.isNetworkAvailable()) {
            try {
                // Получаем все документы из коллекции без фильтров и сортировки
                val allDocs = firestore.collection("measurements").get().await()

                // Фильтруем по blockNumber и сортируем по timestamp на клиенте
                val filteredAndSorted = allDocs.documents
                    .filter { it.getLong("blockNumber")?.toInt() == blockId }
                    .sortedByDescending { it.getLong("timestamp") ?: 0L }
                    .take(10)

                // Преобразуем документы в объекты измерений
                val measurements = filteredAndSorted.mapNotNull { doc ->
                    try {
                        val date = doc.getString("date") ?: ""
                        val blockNumber = doc.getLong("blockNumber")?.toInt() ?: 0
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        // Получаем массив датчиков
                        val allSensors = doc.get("sensors") as? List<*> ?: emptyList<Any>()

                        // Ищем нужный датчик
                        val filteredSensors = allSensors.mapNotNull { sensorObj ->
                            val sensorMap = sensorObj as? Map<*, *> ?: return@mapNotNull null
                            val title = sensorMap["sensorTitle"] as? String

                            if (title == sensorPosition) {
                                SensorMeasurement(
                                    sensorTitle = title,
                                    panelValue = sensorMap["panelValue"] as? String ?: "",
                                    testoValue = sensorMap["testoValue"] as? String ?: "",
                                    correctionValue = sensorMap["correctionValue"] as? String ?: ""
                                )
                            } else null
                        }

                        if (filteredSensors.isNotEmpty()) {
                            LatestMeasurement(
                                id = doc.id,
                                date = date,
                                blockNumber = blockNumber,
                                sensors = filteredSensors,
                                timestamp = timestamp
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e(TAG, "⭐ Ошибка обработки документа: ${e.message}", e)
                        null
                    }
                }

                Log.d(TAG, "⭐ После фильтрации найдено ${measurements.size} измерений для датчика $sensorPosition")
                emit(measurements)
            } catch (e: Exception) {
                Log.e(TAG, "⭐ Ошибка загрузки измерений: ${e.message}", e)
                emit(emptyList())
            }
        } else {
            Log.d(TAG, "⭐ Нет сети, возвращаем пустой список")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Загружает последние измерения для блока
     */
    fun loadLatestMeasurements(blockId: Int): Flow<List<LatestMeasurement>> = flow {
        Log.d(TAG, "⭐ loadLatestMeasurements: Запрос для блока $blockId")

        if (networkService.isNetworkAvailable()) {
            try {
                // Так как в Firestore требуется индекс для составного запроса,
                // сначала просто загрузим последние документы без фильтрации по blockNumber
                val query = firestore.collection("measurements")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)

                Log.d(TAG, "⭐ Отправляем запрос в Firestore без фильтра по blockNumber")
                Log.d(TAG, "⭐ Запрос: $query")

                val snapshot = query.get().await()
                Log.d(TAG, "⭐ Получено ${snapshot.documents.size} документов всего")

                // Фильтруем документы по blockNumber на стороне клиента
                val filteredDocs = snapshot.documents.filter { doc ->
                    val docBlockNumber = doc.getLong("blockNumber")?.toInt()
                    Log.d(TAG, "⭐ Документ: ${doc.id}, blockNumber: $docBlockNumber")
                    docBlockNumber == blockId
                }

                Log.d(TAG, "⭐ После фильтрации осталось ${filteredDocs.size} документов для блока $blockId")

                // Оставляем только последние 5 документов
                val latestDocs = filteredDocs.take(5)

                val measurements = latestDocs.mapNotNull { doc ->
                    try {
                        // ID документа
                        val id = doc.id

                        // Получаем поля
                        val date = doc.getString("date") ?: ""
                        Log.d(TAG, "⭐ Дата: $date")

                        val blockNumber = doc.getLong("blockNumber")?.toInt() ?: 0
                        Log.d(TAG, "⭐ Номер блока: $blockNumber")

                        val timestamp = doc.getLong("timestamp") ?: 0L

                        // Получаем массив датчиков
                        val sensorsArray = doc.get("sensors") as? List<*>
                        Log.d(TAG, "⭐ Датчики: ${sensorsArray?.size ?: "null"}")

                        // Преобразуем датчики
                        val sensorMeasurements = sensorsArray?.mapNotNull { sensorObj ->
                            try {
                                val sensorMap = sensorObj as? Map<*, *>
                                if (sensorMap == null) {
                                    Log.e(TAG, "⭐ Ошибка: датчик не является Map")
                                    return@mapNotNull null
                                }

                                val sensorTitle = sensorMap["sensorTitle"] as? String ?: ""
                                val panelValue = sensorMap["panelValue"] as? String ?: ""
                                val testoValue = sensorMap["testoValue"] as? String ?: ""
                                val correctionValue = sensorMap["correctionValue"] as? String ?: ""

                                Log.d(TAG, "⭐ Датчик: $sensorTitle, testo: $testoValue, correction: $correctionValue")

                                SensorMeasurement(
                                    sensorTitle = sensorTitle,
                                    panelValue = panelValue,
                                    testoValue = testoValue,
                                    correctionValue = correctionValue
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "⭐ Ошибка при преобразовании датчика: ${e.message}", e)
                                null
                            }
                        } ?: emptyList()

                        LatestMeasurement(
                            id = id,
                            date = date,
                            blockNumber = blockNumber,
                            sensors = sensorMeasurements,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "⭐ Ошибка при преобразовании документа: ${e.message}", e)
                        null
                    }
                }

                Log.d(TAG, "⭐ Преобразовано ${measurements.size} измерений")
                emit(measurements)

            } catch (e: Exception) {
                Log.e(TAG, "⭐ Ошибка при загрузке измерений: ${e.message}", e)

                // Попробуем альтернативный подход, если первый не сработал
                try {
                    Log.d(TAG, "⭐ Пробуем альтернативный подход для получения измерений")
                    // Получаем все документы коллекции без фильтрации и сортировки
                    val allDocs = firestore.collection("measurements").get().await()

                    // Фильтруем и сортируем на стороне клиента
                    val filteredAndSorted = allDocs.documents
                        .filter { it.getLong("blockNumber")?.toInt() == blockId }
                        .sortedByDescending { it.getLong("timestamp") ?: 0L }
                        .take(5)

                    Log.d(TAG, "⭐ Альтернативным способом найдено ${filteredAndSorted.size} документов")

                    val measurements = filteredAndSorted.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val date = doc.getString("date") ?: ""
                            val blockNumber = doc.getLong("blockNumber")?.toInt() ?: 0
                            val timestamp = doc.getLong("timestamp") ?: 0L

                            // Обработка массива датчиков
                            val sensorsArray = doc.get("sensors") as? List<*>
                            val sensorMeasurements = sensorsArray?.mapNotNull { sensorObj ->
                                try {
                                    val sensorMap = sensorObj as? Map<*, *> ?: return@mapNotNull null
                                    SensorMeasurement(
                                        sensorTitle = sensorMap["sensorTitle"] as? String ?: "",
                                        panelValue = sensorMap["panelValue"] as? String ?: "",
                                        testoValue = sensorMap["testoValue"] as? String ?: "",
                                        correctionValue = sensorMap["correctionValue"] as? String ?: ""
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()

                            LatestMeasurement(
                                id = id,
                                date = date,
                                blockNumber = blockNumber,
                                sensors = sensorMeasurements,
                                timestamp = timestamp
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    emit(measurements)

                } catch (e2: Exception) {
                    Log.e(TAG, "⭐ Ошибка при альтернативном получении измерений: ${e2.message}", e2)
                    emit(emptyList())
                }
            }
        } else {
            Log.d(TAG, "⭐ Сеть недоступна")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Загружает последние 10 измерений
     */
    fun loadLastTenMeasurements(): Flow<List<LatestMeasurement>> = flow {
        Log.d(TAG, "⭐ Загрузка последних 10 измерений")

        if (networkService.isNetworkAvailable()) {
            try {
                // Из-за проблемы с индексами, получаем все документы и сортируем на клиенте
                val allDocs = firestore.collection("measurements").get().await()
                Log.d(TAG, "⭐ Всего получено ${allDocs.documents.size} документов")

                // Сортируем по timestamp и берем первые 10
                val latestDocs = allDocs.documents
                    .sortedByDescending { it.getLong("timestamp") ?: 0L }
                    .take(10)

                Log.d(TAG, "⭐ Отобрано ${latestDocs.size} последних документов")

                val measurements = latestDocs.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val date = doc.getString("date") ?: ""
                        val blockNumber = doc.getLong("blockNumber")?.toInt() ?: 0
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        // Получаем массив датчиков
                        val sensorsArray = doc.get("sensors") as? List<*> ?: emptyList<Any>()

                        // Преобразуем датчики
                        val sensorMeasurements = sensorsArray.mapNotNull { sensorObj ->
                            try {
                                val sensorMap = sensorObj as? Map<*, *> ?: return@mapNotNull null

                                SensorMeasurement(
                                    sensorTitle = sensorMap["sensorTitle"] as? String ?: "",
                                    panelValue = sensorMap["panelValue"] as? String ?: "",
                                    testoValue = sensorMap["testoValue"] as? String ?: "",
                                    correctionValue = sensorMap["correctionValue"] as? String ?: ""
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "⭐ Ошибка при преобразовании датчика: ${e.message}", e)
                                null
                            }
                        }

                        LatestMeasurement(
                            id = id,
                            date = date,
                            blockNumber = blockNumber,
                            sensors = sensorMeasurements,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "⭐ Ошибка при преобразовании документа: ${e.message}", e)
                        null
                    }
                }

                Log.d(TAG, "⭐ Преобразовано ${measurements.size} измерений")
                emit(measurements)
            } catch (e: Exception) {
                Log.e(TAG, "⭐ Ошибка при загрузке измерений: ${e.message}", e)
                emit(emptyList())
            }
        } else {
            Log.d(TAG, "⭐ Нет сети, возвращаем пустой список")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}