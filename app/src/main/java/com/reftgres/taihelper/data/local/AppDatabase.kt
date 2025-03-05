package com.reftgres.taihelper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.reftgres.taihelper.data.local.converter.DateConverter
import com.reftgres.taihelper.data.local.converter.ListConverter
import com.reftgres.taihelper.data.local.dao.CalibrationDao
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.CalibrationEntity
import com.reftgres.taihelper.data.local.entity.SensorEntity
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        SensorEntity::class,
        CalibrationEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class, ListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun syncQueueDao(): SyncQueueDao
}