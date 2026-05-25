package com.raymi.app

import android.app.Application
import androidx.work.Configuration
import com.raymi.app.data.remote.FirebaseDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RaymiApp : Application(), Configuration.Provider {

    @Inject
    lateinit var scheduleOverdueCheckUseCase: com.raymi.app.domain.usecase.notifications.ScheduleOverdueCheckUseCase

    @Inject
    lateinit var firebaseDataSource: FirebaseDataSource

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkManagerEntryPoint {
        fun hiltWorkerFactory(): androidx.hilt.work.HiltWorkerFactory
    }

    override val workManagerConfiguration: Configuration
        get() {
            val entryPoint = EntryPoints.get(this, WorkManagerEntryPoint::class.java)
            return Configuration.Builder()
                .setWorkerFactory(entryPoint.hiltWorkerFactory())
                .build()
        }

    override fun onCreate() {
        super.onCreate()

        // Ejecutar tareas de inicialización en segundo plano para NO bloquear el arranque de la UI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduleOverdueCheckUseCase()
            } catch (e: Exception) {
                android.util.Log.e("RaymiApp", "Error al programar verificación de alquileres: ${e.message}", e)
            }
        }
    }
}
