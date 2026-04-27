package com.raymi.app.core.di

import android.content.Context
import com.raymi.app.data.repository.AlquilerRepositoryImpl
import com.raymi.app.data.repository.AuthRepositoryImpl
import com.raymi.app.data.repository.ClienteRepositoryImpl
import com.raymi.app.data.repository.VestuarioRepositoryImpl
import com.raymi.app.domain.repository.AlquilerRepository
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.repository.ClienteRepository
import com.raymi.app.domain.repository.VestuarioRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para la aplicación
 * Vincula las interfaces de repositorio con sus implementaciones
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /**
     * Proporciona la implementación del repositorio de autenticación
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    /**
     * Proporciona la implementación del repositorio de clientes
     */
    @Binds
    @Singleton
    abstract fun bindClienteRepository(
        clienteRepositoryImpl: ClienteRepositoryImpl
    ): ClienteRepository

    /**
     * Proporciona la implementación del repositorio de vestuarios
     */
    @Binds
    @Singleton
    abstract fun bindVestuarioRepository(
        vestuarioRepositoryImpl: VestuarioRepositoryImpl
    ): VestuarioRepository

    /**
     * Proporciona la implementación del repositorio de alquileres
     */
    @Binds
    @Singleton
    abstract fun bindAlquilerRepository(
        alquilerRepositoryImpl: AlquilerRepositoryImpl
    ): AlquilerRepository

    companion object {
        @Provides
        @Singleton
        fun provideContext(@ApplicationContext context: Context): Context {
            return context
        }
    }
}
