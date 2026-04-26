package com.raymi.app

import android.app.Application
import com.raymi.app.data.remote.FirebaseDataSource
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RaymiApp : Application() {

    @Inject
    lateinit var scheduleOverdueCheckUseCase: com.raymi.app.domain.usecase.notifications.ScheduleOverdueCheckUseCase

    @Inject
    lateinit var firebaseDataSource: FirebaseDataSource

    override fun onCreate() {
        super.onCreate()
        // Programar verificación de alquileres vencidos
        scheduleOverdueCheckUseCase()

        // Poblar datos de prueba si la base está vacía
        CoroutineScope(Dispatchers.IO).launch {
            firebaseDataSource.populateTestDataIfEmpty()
        }
    }
}