package com.reftgres.taihelper.ui.addsensor

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    data class Block(val id: String, val name: String)
    data class Measurement(val id: String, val name: String)
    data class SensorType(val id: String, val name: String)
    data class OutputRange(val id: String, val name: String)

    private suspend fun <T> getListFromFirestore(
        collection: String,
        documentId: String,
        fieldName: String,
        transform: (index: Int, value: Any) -> T,
        fallbackList: List<T>
    ): List<T> {
        return try {
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
    }

    suspend fun getBlocks() = getListFromFirestore(
        "blocks",
        "m4GnBFXchve7t17GUUJz",
        "block",
        { index, value -> Block(index.toString(), "Блок $value") },
        listOf(
            Block("fallback1", "Блок 1 (тест)"),
            Block("fallback2", "Блок 2 (тест)"),
            Block("fallback3", "Блок 3 (тест)")
        )
    )

    suspend fun getSensorTypes() = getListFromFirestore(
        "type_sensors",
        "GnamO7YBhE1fHvTQ8TUn",
        "type",
        { index, value -> SensorType(index.toString(), value.toString()) },
        listOf(
            SensorType("0", "Датчик температуры"),
            SensorType("1", "Датчик давления"),
            SensorType("2", "Датчик уровня")
        )
    )

    suspend fun getMeasurementStarts() = getListFromFirestore(
        "measurement_start",
        "k7LVhxBf7YC4ogF0PgPT",
        "start",
        { index, value -> Measurement(index.toString(), value.toString()) },
        listOf(
            Measurement("0", "Автоматическое начало"),
            Measurement("1", "Ручное начало")
        )
    )

    suspend fun getMeasurementEnds() = getListFromFirestore(
        "measurement_end",
        "uwHVpk5uZVC3tRYAhPpg",
        "end",
        { index, value -> Measurement(index.toString(), value.toString()) },
        listOf(
            Measurement("0", "Автоматическое окончание"),
            Measurement("1", "Ручное окончание")
        )
    )

    fun getOutputRanges() = listOf(
        OutputRange("0-5", "0-5 мА"),
        OutputRange("4-20", "4-20 мА")
    )

    suspend fun saveSensor(
        typeId: String,
        position: String,
        outputScale: String,
        midPoint: String,
        modification: String,
        blockId: String,
        measurementStartId: String,
        measurementEndId: String
    ): Result<String> {
        return try {
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

            val result = firestore.collection("sensors").add(sensor).await()
            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}