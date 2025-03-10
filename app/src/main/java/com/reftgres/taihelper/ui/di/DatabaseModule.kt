//управляет Room-базой данных и предоставляет DAO
package com.reftgres.taihelper.ui.di

import android.content.Context
import androidx.room.Room
import com.reftgres.taihelper.data.local.AppDatabase
import com.reftgres.taihelper.data.local.MIGRATION_1_2
import com.reftgres.taihelper.data.local.dao.CalibrationDao
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(MIGRATION_1_2) // Добавляем миграцию здесь
            .build()
    }

    @Provides
    fun provideSensorDao(database: AppDatabase): SensorDao = database.sensorDao()

    @Provides
    fun provideCalibrationDao(database: AppDatabase): CalibrationDao = database.calibrationDao()

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao = database.syncQueueDao()
}