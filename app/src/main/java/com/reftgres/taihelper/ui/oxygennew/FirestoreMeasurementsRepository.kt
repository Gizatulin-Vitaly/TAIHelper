package com.reftgres.taihelper.ui.oxygennew

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.reftgres.taihelper.data.local.dao.MeasurementsDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.MeasurementsEntity
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import com.reftgres.taihelper.ui.model.MeasurementRecord
import com.reftgres.taihelper.ui.model.OxygenMeasurementData
import com.reftgres.taihelper.ui.oxygennew.MeasurementsRepository.SensorUpdate
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class FirestoreMeasurementsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val measurementsDao: MeasurementsDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkService: NetworkConnectivityService,
    private val syncManager: SyncManager,
    private val gson: Gson
) : MeasurementsRepository {

    private val measurementsCollection = firestore.collection("measurements")
    private val sensorsCollection = firestore.collection("sensors")

    override suspend fun saveMeasurement(measurement: MeasurementRecord): Result<String> = suspendCoroutine { continuation ->
        measurementsCollection.document(measurement.id)
            .set(measurement)
            .addOnSuccessListener {
                continuation.resume(Result.success(measurement.id))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun getMeasurements(): Result<List<MeasurementRecord>> = suspendCoroutine { continuation ->
        measurementsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val measurements = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(MeasurementRecord::class.java)
                }
                continuation.resume(Result.success(measurements))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun getMeasurementById(id: String): Result<MeasurementRecord?> = suspendCoroutine { continuation ->
        measurementsCollection.document(id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val measurement = documentSnapshot.toObject(MeasurementRecord::class.java)
                continuation.resume(Result.success(measurement))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun updateSensorMidpoint(
        blockNumber: Int,
        sensorTitle: String,
        midpointValue: String
    ): Result<Boolean> = suspendCoroutine { continuation ->
        // Формируем ссылку на блок в формате "/blocks/{blockNumber}"
        val blockReference = "/blocks/${blockNumber - 1}" // Предполагаю, что нумерация начинается с 0

        Log.d("SensorUpdate", "Ищем датчик: блок $blockReference, позиция $sensorTitle")

        sensorsCollection
            .whereEqualTo("block", blockReference)
            .whereEqualTo("position", sensorTitle)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("SensorUpdate", "Результат поиска: ${querySnapshot.size()} документов")

                if (querySnapshot.isEmpty) {
                    val errorMsg = "Датчик не найден: блок $blockReference, позиция $sensorTitle"
                    Log.e("SensorUpdate", errorMsg)
                    continuation.resume(Result.failure(Exception(errorMsg)))
                    return@addOnSuccessListener
                }

                // Получаем первый найденный документ
                val sensorDocument = querySnapshot.documents.first()
                Log.d("SensorUpdate", "Найден документ с ID: ${sensorDocument.id}")

                // Обновляем значение средней точки в поле mid_point
                sensorDocument.reference
                    .update("mid_point", midpointValue)
                    .addOnSuccessListener {
                        Log.d("SensorUpdate", "Успешно обновлена средняя точка")
                        continuation.resume(Result.success(true))
                    }
                    .addOnFailureListener { e ->
                        Log.e("SensorUpdate", "Ошибка обновления: ${e.message}")
                        continuation.resume(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                Log.e("SensorUpdate", "Ошибка поиска: ${e.message}")
                continuation.resume(Result.failure(e))
            }
    }

    override suspend fun saveMeasurementOffline(
        measurement: MeasurementRecord,
        sensorUpdates: List<SensorUpdate>
    ): Result<String> {
        return try {
            if (networkService.isNetworkAvailable()) {
                // Если сеть доступна, сохраняем напрямую
                val saveResult = saveMeasurement(measurement)

                if (saveResult.isSuccess) {
                    // Обновляем датчики
                    sensorUpdates.forEach { update ->
                        updateSensorMidpoint(
                            extractBlockNumber(update.blockReference),
                            update.position,
                            update.midpointValue
                        )
                    }

                    saveResult
                } else {
                    saveResult
                }
            } else {
                // Если сети нет, сохраняем в очередь синхронизации
                val measurementMap = mapOf(
                    "id" to measurement.id,
                    "blockNumber" to measurement.blockNumber,
                    "date" to measurement.date,
                    "timestamp" to measurement.timestamp,
                    "sensors" to measurement.sensors
                )

                val syncData = OxygenMeasurementData(
                    measurement = measurementMap,
                    sensorUpdates = sensorUpdates
                )

                // Преобразуем в JSON
                val dataJson = gson.toJson(syncData)

                // Сохраняем в очередь синхронизации
                val entity = MeasurementsEntity(
                    measurementId = measurement.id,
                    blockNumber = measurement.blockNumber,
                    date = measurement.date,
                    timestamp = measurement.timestamp,
                    syncStatus = MeasurementsEntity.SYNC_STATUS_PENDING,
                    sensorData = dataJson
                )

                val itemId = measurementsDao.insertMeasurement(entity)

                // Запланируем работу синхронизации
                syncManager.requestSync()

                Result.success("offline_${itemId}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Вспомогательный метод для извлечения номера блока из ссылки
    private fun extractBlockNumber(blockReference: String): Int {
        val blockIndex = blockReference.split("/").lastOrNull()?.toIntOrNull() ?: 0
        return blockIndex + 1
    }
}