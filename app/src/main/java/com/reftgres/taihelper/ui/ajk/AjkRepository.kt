package com.reftgres.taihelper.ui.ajk

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class AjkRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val COLLECTION_CALIBRATIONS = "ajkCalibrations"
    }

    /**
     * Сохраняет данные калибровки в Firestore
     * @param data данные для сохранения
     * @return Flow с результатом операции
     */
    fun saveCalibrationData(data: DataAjk): Flow<Result<String>> = callbackFlow {
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
            "timestamp" to Timestamp.now(),
            "userId" to data.userId
        )

        firestore.collection(COLLECTION_CALIBRATIONS)
            .add(dataMap)
            .addOnSuccessListener { documentReference ->
                trySend(Result.success(documentReference.id))
            }
            .addOnFailureListener { exception ->
                trySend(Result.failure(exception))
            }

        awaitClose()
    }

    /**
     * Получает список предыдущих калибровок
     * @return Flow со списком данных калибровки
     */
    fun getCalibrationHistory(): Flow<Result<List<DataAjk>>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_CALIBRATIONS)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    trySend(Result.failure(exception))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val calibrations = snapshot.documents.mapNotNull { document ->
                            try {
                                // Преобразование Firestore документа в объект DataAjk
                                val labValues = document["labValues"] as? List<String> ?: listOf()
                                val testValues = document["testValues"] as? List<String> ?: listOf()
                                val labAverage = (document["labAverage"] as? Number)?.toFloat() ?: 0f
                                val testAverage = (document["testAverage"] as? Number)?.toFloat() ?: 0f
                                val resistance = document["resistance"] as? String ?: ""
                                val constant = (document["constant"] as? Number)?.toFloat() ?: 0f

                                // Получение данных для таблицы сопротивлений
                                val r02Map = document["r02"] as? Map<String, Any> ?: mapOf()
                                val r05Map = document["r05"] as? Map<String, Any> ?: mapOf()
                                val r08Map = document["r08"] as? Map<String, Any> ?: mapOf()
                                val r40Map = document["r40deg"] as? Map<String, Any> ?: mapOf()

                                val r02Resistance = (r02Map["r"] as? Number)?.toFloat() ?: 0f
                                val r02I = r02Map["i"] as? String ?: ""
                                val r02SensorValue = r02Map["sensorValue"] as? String ?: ""

                                val r05Resistance = (r05Map["r"] as? Number)?.toFloat() ?: 0f
                                val r05I = r05Map["i"] as? String ?: ""
                                val r05SensorValue = r05Map["sensorValue"] as? String ?: ""

                                val r08Resistance = (r08Map["r"] as? Number)?.toFloat() ?: 0f
                                val r08I = r08Map["i"] as? String ?: ""
                                val r08SensorValue = r08Map["sensorValue"] as? String ?: ""

                                val r40Resistance = (r40Map["r"] as? Number)?.toFloat() ?: 0f
                                val r40SensorValue = r40Map["sensorValue"] as? String ?: ""

                                val timestamp = document["timestamp"] as? Timestamp ?: Timestamp.now()
                                val userId = document["userId"] as? String ?: ""

                                DataAjk(
                                    id = document.id,
                                    labSensorValues = labValues,
                                    testSensorValues = testValues,
                                    labAverage = labAverage,
                                    testAverage = testAverage,
                                    resistance = resistance,
                                    constant = constant,
                                    r02Resistance = r02Resistance,
                                    r02I = r02I,
                                    r02SensorValue = r02SensorValue,
                                    r05Resistance = r05Resistance,
                                    r05I = r05I,
                                    r05SensorValue = r05SensorValue,
                                    r08Resistance = r08Resistance,
                                    r08I = r08I,
                                    r08SensorValue = r08SensorValue,
                                    r40DegResistance = r40Resistance,
                                    r40DegSensorValue = r40SensorValue,
                                    timestamp = timestamp.toDate(),
                                    userId = userId
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        trySend(Result.success(calibrations))
                    } catch (e: Exception) {
                        trySend(Result.failure(e))
                    }
                }
            }

        // Очистка слушателя при закрытии Flow
        awaitClose { listenerRegistration.remove() }
    }
}