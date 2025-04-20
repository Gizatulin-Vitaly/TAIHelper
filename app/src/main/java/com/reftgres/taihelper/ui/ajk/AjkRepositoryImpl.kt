package com.reftgres.taihelper.ui.ajk

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.reftgres.taihelper.data.local.dao.CalibrationDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.CalibrationEntity
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class AjkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val calibrationDao: CalibrationDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkService: NetworkConnectivityService,
    private val syncManager: SyncManager
) : AjkRepository {

    private val gson = Gson()
    private val TAG = "AjkRepository"

    companion object {
        private const val COLLECTION_CALIBRATIONS = "ajkCalibrations"
    }

    /**
     * Сохраняет данные калибровки в локальную базу данных и синхронизирует с Firestore при наличии сети
     * @param data данные для сохранения
     * @return Flow с результатом операции
     */
    override fun saveCalibrationData(data: DataAjk): Flow<Result<String>> = flow {
        val calibrationId = data.id.ifEmpty { UUID.randomUUID().toString() }
        Log.d(TAG, "Сохранение калибровки с ID: $calibrationId, сеть: ${networkService.isNetworkAvailable()}")

        try {
            // Создаем Entity для локального хранения
            val calibrationEntity = CalibrationEntity(
                id = calibrationId,
                sensorPosition = data.sensorPosition,
                sensorSerial = data.sensorSerial,
                labSensorValues = data.labSensorValues,
                testSensorValues = data.testSensorValues,
                labAverage = data.labAverage,
                testAverage = data.testAverage,
                resistance = data.resistance,
                constant = data.constant,
                r02Resistance = data.r02Resistance,
                r02I = data.r02I,
                r02SensorValue = data.r02SensorValue,
                r05Resistance = data.r05Resistance,
                r05I = data.r05I,
                r05SensorValue = data.r05SensorValue,
                r08Resistance = data.r08Resistance,
                r08I = data.r08I,
                r08SensorValue = data.r08SensorValue,
                r40DegResistance = data.r40DegResistance,
                r40DegSensorValue = data.r40DegSensorValue,
                timestamp = data.timestamp ?: Date(),
                userId = data.userId,
                isSynced = false  // Всегда начинаем с "не синхронизировано"
            )

            // Сохраняем в локальную базу данных
            Log.d(TAG, "Сохраняем в локальную базу данных")
            calibrationDao.insertCalibration(calibrationEntity)
            Log.d("AjkRepository", "Сохраняем в Room: ${calibrationEntity.id}")

            // Проверяем доступность сети
            val isNetworkAvailable = networkService.isNetworkAvailable()

            if (isNetworkAvailable) {
                try {
                    Log.d(TAG, "Синхронизация с Firestore...")
                    val dataMap = hashMapOf(
                        "sensorPosition" to data.sensorPosition,
                        "sensorSerial" to data.sensorSerial,
                        "labValues" to data.labSensorValues,
                        "testValues" to data.testSensorValues,
                        "labAverage" to data.labAverage,
                        "testAverage" to data.testAverage,
                        "resistance" to data.resistance,
                        "constant" to data.constant,
                        "r02" to hashMapOf(
                            "r" to data.r02Resistance,
                            "i" to data.r02I,
                            "sensorValue" to data.r02SensorValue
                        ),
                        "r05" to hashMapOf(
                            "r" to data.r05Resistance,
                            "i" to data.r05I,
                            "sensorValue" to data.r05SensorValue
                        ),
                        "r08" to hashMapOf(
                            "r" to data.r08Resistance,
                            "i" to data.r08I,
                            "sensorValue" to data.r08SensorValue
                        ),
                        "r40deg" to hashMapOf(
                            "r" to data.r40DegResistance,
                            "sensorValue" to data.r40DegSensorValue
                        ),
                        "timestamp" to Timestamp(calibrationEntity.timestamp),
                        "userId" to data.userId
                    )

                    firestore.collection(COLLECTION_CALIBRATIONS)
                        .document(calibrationId)
                        .set(dataMap)
                        .await()

                    // Обновляем статус синхронизации
                    calibrationDao.updateSyncStatus(calibrationId, true)
                    Log.d(TAG, "Калибровка синхронизирована с Firestore")
                } catch (e: Exception) {
                    // Если произошла ошибка при синхронизации с Firestore,
                    // добавляем элемент в очередь синхронизации
                    Log.e(TAG, "Ошибка синхронизации с Firestore: ${e.message}", e)
                    addToSyncQueue(calibrationId, calibrationEntity)
                }
            } else {
                // Если сеть недоступна, добавляем элемент в очередь синхронизации
                Log.d(TAG, "Нет сети, добавление в очередь синхронизации")
                addToSyncQueue(calibrationId, calibrationEntity)
            }

            // В любом случае возвращаем успешный результат с ID
            Log.d(TAG, "Возвращаем успешный результат сохранения: $calibrationId")
            emit(Result.success(calibrationId))
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении калибровки: ${e.message}", e)
            emit(Result.failure(e))
        }
    }.catch { e ->
        Log.e(TAG, "Непредвиденная ошибка в Flow: ${e.message}", e)
        emit(Result.failure(e))
    }

    override suspend fun importAllFromFirestore() {
        try {
            val snapshots = firestore.collection("ajkCalibrations").get().await()
            for (doc in snapshots.documents) {
                val map = doc.data ?: continue

                val entity = CalibrationEntity(
                    id = doc.id,
                    sensorPosition = map["sensorPosition"] as? String ?: "",
                    sensorSerial = map["sensorSerial"] as? String ?: "",
                    labSensorValues = map["labValues"] as? List<String> ?: emptyList(),
                    testSensorValues = map["testValues"] as? List<String> ?: emptyList(),
                    labAverage = (map["labAverage"] as? Number)?.toFloat() ?: 0f,
                    testAverage = (map["testAverage"] as? Number)?.toFloat() ?: 0f,
                    resistance = map["resistance"] as? String ?: "",
                    constant = (map["constant"] as? Number)?.toFloat() ?: 0f,
                    r02Resistance = (map["r02"] as? Map<*, *>)?.get("r")?.toString()?.toFloatOrNull() ?: 0f,
                    r02I = (map["r02"] as? Map<*, *>)?.get("i") as? String ?: "",
                    r02SensorValue = (map["r02"] as? Map<*, *>)?.get("sensorValue") as? String ?: "",
                    r05Resistance = (map["r05"] as? Map<*, *>)?.get("r")?.toString()?.toFloatOrNull() ?: 0f,
                    r05I = (map["r05"] as? Map<*, *>)?.get("i") as? String ?: "",
                    r05SensorValue = (map["r05"] as? Map<*, *>)?.get("sensorValue") as? String ?: "",
                    r08Resistance = (map["r08"] as? Map<*, *>)?.get("r")?.toString()?.toFloatOrNull() ?: 0f,
                    r08I = (map["r08"] as? Map<*, *>)?.get("i") as? String ?: "",
                    r08SensorValue = (map["r08"] as? Map<*, *>)?.get("sensorValue") as? String ?: "",
                    r40DegResistance = (map["r40deg"] as? Map<*, *>)?.get("r")?.toString()?.toFloatOrNull() ?: 0f,
                    r40DegSensorValue = (map["r40deg"] as? Map<*, *>)?.get("sensorValue") as? String ?: "",
                    timestamp = (map["timestamp"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    userId = map["userId"] as? String ?: "",
                    isSynced = true
                )

                calibrationDao.insertCalibration(entity)
            }
        } catch (e: Exception) {
            Log.e("AjkRepository", "Ошибка импорта Firestore → Room: ${e.message}", e)
        }
    }


    // Вспомогательный метод для добавления элемента в очередь синхронизации
    private suspend fun addToSyncQueue(calibrationId: String, calibrationEntity: CalibrationEntity) {
        try {
            val syncItem = SyncQueueEntity(
                entityId = calibrationId,
                entityType = "calibration",
                operation = "insert",
                jsonData = gson.toJson(calibrationEntity)
            )
            syncQueueDao.insertSyncItem(syncItem)
            Log.d(TAG, "Калибровка добавлена в очередь синхронизации: $calibrationId")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении в очередь синхронизации: ${e.message}", e)
        }
    }

    /**
     * Получает список предыдущих калибровок из локальной базы данных
     * @return Flow со списком данных калибровки
     */
    override fun getCalibrationHistory(): Flow<Result<List<DataAjk>>> {
        Log.d(TAG, "Получение истории калибровок")

        // Если сеть доступна, запрашиваем синхронизацию
        if (networkService.isNetworkAvailable()) {
            Log.d(TAG, "Сеть доступна, запрос синхронизации")
            syncManager.requestSync()
        } else {
            Log.d(TAG, "Сеть недоступна, используем только локальные данные")
        }

        // Возвращаем данные из локальной базы данных
        return calibrationDao.getAllCalibrations().map { calibrations ->
            Log.d(TAG, "Получено ${calibrations.size} калибровок из базы данных")
            Result.success(calibrations.map { entity ->
                DataAjk(
                    id = entity.id,
                    labSensorValues = entity.labSensorValues,
                    testSensorValues = entity.testSensorValues,
                    labAverage = entity.labAverage,
                    testAverage = entity.testAverage,
                    resistance = entity.resistance,
                    constant = entity.constant,
                    r02Resistance = entity.r02Resistance,
                    r02I = entity.r02I,
                    r02SensorValue = entity.r02SensorValue,
                    r05Resistance = entity.r05Resistance,
                    r05I = entity.r05I,
                    r05SensorValue = entity.r05SensorValue,
                    r08Resistance = entity.r08Resistance,
                    r08I = entity.r08I,
                    r08SensorValue = entity.r08SensorValue,
                    r40DegResistance = entity.r40DegResistance,
                    r40DegSensorValue = entity.r40DegSensorValue,
                    timestamp = entity.timestamp,
                    userId = entity.userId
                )
            })
        }.catch { e ->
            Log.e(TAG, "Ошибка при получении истории калибровок", e)
            emit(Result.failure(e))
        }
    }
}