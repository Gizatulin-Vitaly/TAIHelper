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
        Log.d(TAG, "Сохранение калибровки с ID: $calibrationId")

        // Создаем Entity для локального хранения
        val calibrationEntity = CalibrationEntity(
            id = calibrationId,
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
            isSynced = networkService.isNetworkAvailable() // Устанавливаем статус синхронизации
        )

        try {
            // Сохраняем в локальную базу данных
            calibrationDao.insertCalibration(calibrationEntity)
            Log.d(TAG, "Калибровка сохранена в локальной базе данных")

            // Если сеть доступна, пытаемся синхронизировать с Firestore
            if (networkService.isNetworkAvailable()) {
                try {
                    Log.d(TAG, "Синхронизация с Firestore...")
                    val dataMap = hashMapOf(
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
                    Log.e(TAG, "Ошибка синхронизации с Firestore", e)
                    val syncItem = SyncQueueEntity(
                        entityId = calibrationId,
                        entityType = "calibration",
                        operation = "insert",
                        jsonData = gson.toJson(calibrationEntity)
                    )
                    syncQueueDao.insertSyncItem(syncItem)
                    Log.d(TAG, "Калибровка добавлена в очередь синхронизации")
                }
            } else {
                // Если сеть недоступна, добавляем элемент в очередь синхронизации
                Log.d(TAG, "Нет сети, добавление в очередь синхронизации")
                val syncItem = SyncQueueEntity(
                    entityId = calibrationId,
                    entityType = "calibration",
                    operation = "insert",
                    jsonData = gson.toJson(calibrationEntity)
                )
                syncQueueDao.insertSyncItem(syncItem)
                Log.d(TAG, "Калибровка добавлена в очередь синхронизации")
            }

            emit(Result.success(calibrationId))

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении калибровки", e)
            emit(Result.failure(e))
        }
    }.catch { e ->
        Log.e(TAG, "Непредвиденная ошибка в Flow", e)
        emit(Result.failure(e))
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