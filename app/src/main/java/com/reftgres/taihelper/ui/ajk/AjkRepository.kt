package com.reftgres.taihelper.ui.ajk

import kotlinx.coroutines.flow.Flow
import java.util.Date

interface AjkRepository {
    fun saveCalibrationData(data: DataAjk): Flow<Result<String>>
    fun getCalibrationHistory(): Flow<Result<List<DataAjk>>>
    suspend fun importAllFromFirestore()
}