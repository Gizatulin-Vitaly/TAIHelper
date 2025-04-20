package com.reftgres.taihelper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.reftgres.taihelper.data.local.converter.DateConverter
import com.reftgres.taihelper.data.local.converter.ListConverter
import com.reftgres.taihelper.data.local.dao.CalibrationDao
import com.reftgres.taihelper.data.local.dao.MeasurementsDao
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.local.entity.CalibrationEntity
import com.reftgres.taihelper.data.local.entity.MeasurementsEntity
import com.reftgres.taihelper.data.local.entity.SensorEntity
import com.reftgres.taihelper.data.local.entity.SyncQueueEntity


val MIGRATION_1_2 = object : Migration(1, 2) { // от версии 1 к версии 2
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQL для создания новой таблицы
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS measurements_sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                measurementId TEXT NOT NULL,
                blockNumber INTEGER NOT NULL,
                date TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                syncStatus INTEGER NOT NULL,
                sensorData TEXT NOT NULL
            )
        """)
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE calibrations ADD COLUMN sensorPosition TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE calibrations ADD COLUMN sensorSerial TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [
        SensorEntity::class,
        CalibrationEntity::class,
        SyncQueueEntity::class,
        MeasurementsEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class, ListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun measurementsDao(): MeasurementsDao
}