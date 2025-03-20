//предоставляет зависимости для Firebase, WorkManager, сетевого сервиса и репозиториев.
package com.reftgres.taihelper.ui.di

import android.content.Context
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.reftgres.taihelper.data.local.AppDatabase
import com.reftgres.taihelper.data.local.dao.CalibrationDao
import com.reftgres.taihelper.data.local.dao.MeasurementsDao
import com.reftgres.taihelper.data.local.dao.SensorDao
import com.reftgres.taihelper.data.local.dao.SyncQueueDao
import com.reftgres.taihelper.data.repository.PdfDocumentRepository
import com.reftgres.taihelper.service.NetworkConnectivityService
import com.reftgres.taihelper.service.SyncManager
import com.reftgres.taihelper.ui.addsensor.AddSensorRepository
import com.reftgres.taihelper.ui.addsensor.SensorRepository
import com.reftgres.taihelper.ui.ajk.AjkRepository
import com.reftgres.taihelper.ui.ajk.AjkRepositoryImpl
import com.reftgres.taihelper.ui.oxygen.OxygenRepository
import com.reftgres.taihelper.ui.oxygennew.FirestoreMeasurementsRepository
import com.reftgres.taihelper.ui.oxygennew.MeasurementsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideNetworkConnectivityService(@ApplicationContext context: Context): NetworkConnectivityService {
        return NetworkConnectivityService(context)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        workManager: WorkManager,
        networkService: NetworkConnectivityService
    ): SyncManager {
        return SyncManager(context, workManager, networkService)
    }

    @Provides
    @Singleton
    fun provideAjkRepository(
        firestore: FirebaseFirestore,
        calibrationDao: CalibrationDao,
        syncQueueDao: SyncQueueDao,
        networkService: NetworkConnectivityService,
        syncManager: SyncManager
    ): AjkRepository {
        return AjkRepositoryImpl(firestore, calibrationDao, syncQueueDao, networkService, syncManager)
    }

    @Provides
    @Singleton
    fun provideSensorRepository(
        firestore: FirebaseFirestore,
        sensorDao: SensorDao,
        syncQueueDao: SyncQueueDao,
        networkService: NetworkConnectivityService
    ): SensorRepository {
        return AddSensorRepository(firestore, sensorDao, syncQueueDao, networkService)
    }

    @Provides
    @Singleton
    fun provideOxygenRepository(
        firestore: FirebaseFirestore,
        sensorDao: SensorDao,
        syncQueueDao: SyncQueueDao,
        networkService: NetworkConnectivityService,
        syncManager: SyncManager
    ): OxygenRepository {
        return OxygenRepository(firestore, sensorDao, syncQueueDao, networkService, syncManager)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun provideMeasurementsRepository(
        firestore: FirebaseFirestore,
        measurementsDao: MeasurementsDao,
        syncQueueDao: SyncQueueDao,
        networkService: NetworkConnectivityService,
        syncManager: SyncManager,
        gson: Gson
    ): MeasurementsRepository {
        return FirestoreMeasurementsRepository(
            firestore,
            measurementsDao,
            syncQueueDao,
            networkService,
            syncManager,
            gson
        )
    }

    @Provides
    @Singleton
    fun provideMeasurementsDao(db: AppDatabase): MeasurementsDao {
        return db.measurementsDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PdfDocumentModule {

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun providePdfDocumentRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        networkService: NetworkConnectivityService,
        @ApplicationContext context: Context
    ): PdfDocumentRepository {
        return PdfDocumentRepository(firestore, storage, networkService, context)
    }
}