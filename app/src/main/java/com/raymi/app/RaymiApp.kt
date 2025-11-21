package com.raymi.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RaymiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar librerías si es necesario
    }
}