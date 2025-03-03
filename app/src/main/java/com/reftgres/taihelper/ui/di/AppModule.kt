package com.reftgres.taihelper.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reftgres.taihelper.ui.addsensor.SensorRepository
import com.reftgres.taihelper.ui.ajk.AjkRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

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
    fun provideSensorRepository(firestore: FirebaseFirestore): SensorRepository {
        return SensorRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideAjkRepository(firestore: FirebaseFirestore): AjkRepository {
        return AjkRepository(firestore)
    }
}