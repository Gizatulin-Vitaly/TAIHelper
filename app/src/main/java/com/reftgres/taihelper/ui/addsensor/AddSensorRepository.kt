package com.reftgres.taihelper.ui.addsensor

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.SensorEntity
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity
import com.reftgres.taihelper.service.NetworkConnectivityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddSensorRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sensorDao: SensorDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkService: NetworkConnectivityService
) : SensorRepository {

    private val gson = Gson()

    override suspend fun getBlocks() = getListFromFirestore(
        "blocks",
        "m4GnBFXchve7t17GUUJz",
        "block",
        { index, value -> SensorRepository.Block(index.toString(), "Блок $value") },
        listOf(
            SensorRepository.Block("fallback1", "Блок 1 (тест)"),
            SensorRepository.Block("fallback2", "Блок 2 (тест)"),
            SensorRepository.Block("fallback3", "Блок 3 (тест)")
        )
    )

    override suspend fun getSensorTypes() = getListFromFirestore(
        "type_sensors",
        "GnamO7YBhE1fHvTQ8TUn",
        "type",
        { index, value -> SensorRepository.SensorType(index.toString(), value.toString()) },
        listOf(
            SensorRepository.SensorType("0", "Датчик температуры"),
            SensorRepository.SensorType("1", "Датчик давления"),
            SensorRepository.SensorType("2", "Датчик уровня")
        )
    )

    override suspend fun getMeasurementStarts() = getListFromFirestore(
        "measurement_start",
        "k7LVhxBf7YC4ogF0PgPT",
        "start",
        { index, value -> SensorRepository.Measurement(index.toString(), value.toString()) },
        listOf(
            SensorRepository.Measurement("0", "Автоматическое начало"),
            SensorRepository.Measurement("1", "Ручное начало")
        )
    )

    override suspend fun getMeasurementEnds() = getListFromFirestore(
        "measurement_end",
        "uwHVpk5uZVC3tRYAhPpg",
        "end",
        { index, value -> SensorRepository.Measurement(index.toString(), value.toString()) },
        listOf(
            SensorRepository.Measurement("0", "Автоматическое окончание"),
            SensorRepository.Measurement("1", "Ручное окончание")
        )
    )

    override fun getOutputRanges() = listOf(
        SensorRepository.OutputRange("0-5", "0-5 мА"),
        SensorRepository.OutputRange("4-20", "4-20 мА")
    )

    private suspend fun <T> getListFromFirestore(
        collection: String,
        documentId: String,
        fieldName: String,
        transform: (index: Int, value: Any) -> T,
        fallbackList: List<T>
    ): List<T> {
        // Проверяем доступность сети
        return if (networkService.isNetworkAvailable()) {
            try {
                val docRef = firestore.collection(collection).document(documentId).get().await()
                val array = docRef.get(fieldName) as? ArrayList<*>

                if (array != null && array.isNotEmpty()) {
                    array.mapIndexed(transform)
                } else {
                    fallbackList
                }
            } catch (e: Exception) {
                fallbackList
            }
        } else {
            // Здесь можно добавить загрузку из локальной базы данных,
            // если эти справочники кэшируются
            fallbackList
        }
    }

    override suspend fun saveSensor(
        typeId: String,
        position: String,
        outputScale: String,
        midPoint: String,
        modification: String,
        blockId: String,
        measurementStartId: String,
        measurementEndId: String
    ): Result<String> {
        val sensorId = UUID.randomUUID().toString()

        // Создаем объект для локального хранения
        val sensorEntity = SensorEntity(
            id = sensorId,
            position = position,
            serialNumber = "",
            midPoint = midPoint,
            outputScale = outputScale,
            blockId = blockId,
            type = typeId,
            measurementStartId = measurementStartId,
            measurementEndId = measurementEndId,
            modification = modification,
            lastCalibration = Date(),
            isSynced = networkService.isNetworkAvailable()
        )

        return try {
            // Сохраняем в локальную базу данных
            withContext(Dispatchers.IO) {
                sensorDao.insertSensor(sensorEntity)
            }

            // Если сеть доступна, пытаемся синхронизировать с Firestore
            if (networkService.isNetworkAvailable()) {
                try {
                    val sensor = hashMapOf(
                        "block" to "/blocks/$blockId",
                        "last_calibration" to Timestamp(Date()),
                        "measurement_end" to "/measurement_end/$measurementEndId",
                        "measurement_start" to "/measurement_start/$measurementStartId",
                        "mid_point" to midPoint,
                        "modification" to modification,
                        "output_scale" to outputScale,
                        "position" to position,
                        "serial_number" to "",
                        "type" to "/type_sensors/$typeId"
                    )

                    firestore.collection("sensors").document(sensorId).set(sensor).await()

                    // Обновляем статус синхронизации
                    withContext(Dispatchers.IO) {
                        sensorDao.updateSyncStatus(sensorId, true)
                    }
                } catch (e: Exception) {
                    // Если синхронизация не удалась, добавляем в очередь
                    val syncItem = SyncQueueEntity(
                        entityId = sensorId,
                        entityType = "sensor",
                        operation = "insert",
                        jsonData = gson.toJson(sensorEntity)
                    )
                    withContext(Dispatchers.IO) {
                        syncQueueDao.insertSyncItem(syncItem)
                    }
                }
            } else {
                // Если сеть недоступна, добавляем в очередь синхронизации
                val syncItem = SyncQueueEntity(
                    entityId = sensorId,
                    entityType = "sensor",
                    operation = "insert",
                    jsonData = gson.toJson(sensorEntity)
                )
                withContext(Dispatchers.IO) {
                    syncQueueDao.insertSyncItem(syncItem)
                }
            }

            Result.success(sensorId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}