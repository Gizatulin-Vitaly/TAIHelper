package com.reftgres.taihelper.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.reftgres.taihelper.data.local.dao.CalibrationDao
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.CalibrationEntity
import com.reftgres.taihelper.data.local.entity.SensorEntity
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * Воркер для выполнения фоновой синхронизации данных с Firestore
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sensorDao: SensorDao,
    private val calibrationDao: CalibrationDao,
    private val syncQueueDao: SyncQueueDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, workerParams) {

    private val gson = Gson()
    private val TAG = "SyncWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker начал выполнение")
        try {
            // Получаем элементы из очереди синхронизации
            val syncItems = syncQueueDao.getPendingSyncItems(50) // Обрабатываем до 50 элементов за раз
            Log.d(TAG, "Получено ${syncItems.size} элементов из очереди синхронизации")

            if (syncItems.isEmpty()) {
                // Если в очереди нет элементов, синхронизируем все несинхронизированные данные
                Log.d(TAG, "Очередь синхронизации пуста, синхронизируем все несинхронизированные данные")
                syncUnsyncedData()
            } else {
                // Обрабатываем элементы очереди
                Log.d(TAG, "Обрабатываем элементы очереди синхронизации")
                for (item in syncItems) {
                    val success = processSyncItem(item)

                    if (success) {
                        Log.d(TAG, "Успешно синхронизирован элемент: ${item.entityId}, тип: ${item.entityType}")
                        syncQueueDao.deleteSyncItem(item)
                    } else if (item.retryCount < 5) { // Максимум 5 попыток
                        Log.d(TAG, "Неудачная синхронизация элемента: ${item.entityId}, увеличиваем счетчик повторов")
                        syncQueueDao.incrementRetryCount(item.id)
                    } else {
                        // Превышено максимальное количество попыток, удаляем элемент
                        Log.d(TAG, "Превышено макс. количество попыток для элемента: ${item.entityId}, удаляем из очереди")
                        syncQueueDao.deleteSyncItem(item)
                    }
                }
            }

            Log.d(TAG, "SyncWorker успешно завершил работу")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в SyncWorker: ${e.message}", e)
            return Result.retry()
        }
    }

    /**
     * Обрабатывает один элемент из очереди синхронизации
     */
    private suspend fun processSyncItem(item: SyncQueueEntity): Boolean {
        Log.d(TAG, "Обработка элемента синхронизации: ${item.entityId}, тип: ${item.entityType}, операция: ${item.operation}")
        return try {
            when (item.entityType) {
                "sensor" -> {
                    when (item.operation) {
                        "insert", "update" -> {
                            val sensor = gson.fromJson(item.jsonData, SensorEntity::class.java)
                            syncSensor(sensor)
                            sensorDao.updateSyncStatus(sensor.id, true)
                        }
                        "delete" -> {
                            firestore.collection("sensors").document(item.entityId).delete().await()
                        }
                        else -> return false
                    }
                }
                "calibration" -> {
                    when (item.operation) {
                        "insert", "update" -> {
                            val calibration = gson.fromJson(item.jsonData, CalibrationEntity::class.java)
                            syncCalibration(calibration)
                            calibrationDao.updateSyncStatus(calibration.id, true)
                        }
                        "delete" -> {
                            firestore.collection("ajkCalibrations").document(item.entityId).delete().await()
                        }
                        else -> return false
                    }
                }
                "measurement" -> {
                    when (item.operation) {
                        "insert", "update" -> {
                            val measurementData = gson.fromJson(item.jsonData, Map::class.java) as Map<String, Any>
                            val dataWithTimestamp = measurementData.toMutableMap().apply {
                                this["timestamp"] = com.google.firebase.Timestamp.now()
                            }

                            firestore.collection("measurements")
                                .document(item.entityId)
                                .set(dataWithTimestamp)
                                .await()
                        }
                        "delete" -> {
                            firestore.collection("measurements").document(item.entityId).delete().await()
                        }
                        else -> return false
                    }
                }
                else -> return false
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обработке элемента синхронизации: ${e.message}", e)
            false
        }
    }

    /**
     * Синхронизирует все несинхронизированные данные
     */
    private suspend fun syncUnsyncedData() {
        // Синхронизируем датчики
        val unsyncedSensors = sensorDao.getUnsyncedSensors()
        Log.d(TAG, "Найдено ${unsyncedSensors.size} несинхронизированных датчиков")

        for (sensor in unsyncedSensors) {
            try {
                syncSensor(sensor)
                sensorDao.updateSyncStatus(sensor.id, true)
                Log.d(TAG, "Успешно синхронизирован датчик: ${sensor.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при синхронизации датчика ${sensor.id}: ${e.message}", e)
                // В случае ошибки, добавляем в очередь
                addToSyncQueue(sensor.id, "sensor", "update", gson.toJson(sensor))
            }
        }

        // Синхронизируем калибровки
        val unsyncedCalibrations = calibrationDao.getUnsyncedCalibrations()
        Log.d(TAG, "Найдено ${unsyncedCalibrations.size} несинхронизированных калибровок")

        for (calibration in unsyncedCalibrations) {
            try {
                syncCalibration(calibration)
                calibrationDao.updateSyncStatus(calibration.id, true)
                Log.d(TAG, "Успешно синхронизирована калибровка: ${calibration.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при синхронизации калибровки ${calibration.id}: ${e.message}", e)
                // В случае ошибки, добавляем в очередь
                addToSyncQueue(calibration.id, "calibration", "update", gson.toJson(calibration))
            }
        }
    }

    /**
     * Синхронизирует датчик с Firestore
     */
    private suspend fun syncSensor(sensor: SensorEntity) {
        Log.d(TAG, "Синхронизация датчика ${sensor.id} с Firestore")
        val sensorMap = hashMapOf(
            "block" to "/blocks/${sensor.blockId}",
            "type" to "/type_sensors/${sensor.type}",
            "position" to sensor.position,
            "serial_number" to sensor.serialNumber,
            "mid_point" to sensor.midPoint,
            "output_scale" to sensor.outputScale,
            "measurement_start" to "/measurement_start/${sensor.measurementStartId}",
            "measurement_end" to "/measurement_end/${sensor.measurementEndId}",
            "modification" to sensor.modification,
            "last_calibration" to sensor.lastCalibration?.let { com.google.firebase.Timestamp(it) }
        )

        firestore.collection("sensors").document(sensor.id).set(sensorMap).await()
    }

    /**
     * Синхронизирует калибровку с Firestore
     */
    private suspend fun syncCalibration(calibration: CalibrationEntity) {
        Log.d(TAG, "Синхронизация калибровки ${calibration.id} с Firestore")
        val calibrationMap = hashMapOf(
            "labValues" to calibration.labSensorValues,
            "testValues" to calibration.testSensorValues,
            "labAverage" to calibration.labAverage,
            "testAverage" to calibration.testAverage,
            "resistance" to calibration.resistance,
            "constant" to calibration.constant,
            "r02" to hashMapOf(
                "r" to calibration.r02Resistance,
                "i" to calibration.r02I,
                "sensorValue" to calibration.r02SensorValue
            ),
            "r05" to hashMapOf(
                "r" to calibration.r05Resistance,
                "i" to calibration.r05I,
                "sensorValue" to calibration.r05SensorValue
            ),
            "r08" to hashMapOf(
                "r" to calibration.r08Resistance,
                "i" to calibration.r08I,
                "sensorValue" to calibration.r08SensorValue
            ),
            "r40deg" to hashMapOf(
                "r" to calibration.r40DegResistance,
                "sensorValue" to calibration.r40DegSensorValue
            ),
            "timestamp" to com.google.firebase.Timestamp(calibration.timestamp),
            "userId" to calibration.userId
        )

        firestore.collection("ajkCalibrations").document(calibration.id).set(calibrationMap).await()
    }

    /**
     * Добавляет элемент в очередь синхронизации
     */
    private suspend fun addToSyncQueue(entityId: String, entityType: String, operation: String, jsonData: String) {
        Log.d(TAG, "Добавление в очередь синхронизации: $entityId, тип: $entityType, операция: $operation")
        val syncItem = SyncQueueEntity(
            entityId = entityId,
            entityType = entityType,
            operation = operation,
            jsonData = jsonData
        )
        syncQueueDao.insertSyncItem(syncItem)
    }
}