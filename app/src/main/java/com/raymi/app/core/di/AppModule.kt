package com.raymi.app.core.di

import android.content.Context
import com.raymi.app.data.repository.AlquilerRepositoryImpl
import com.raymi.app.data.repository.AuthRepositoryImpl
import com.raymi.app.data.repository.BusinessRepositoryImpl
import com.raymi.app.data.repository.CategoriaRepositoryImpl
import com.raymi.app.data.repository.ClienteRepositoryImpl
import com.raymi.app.data.repository.ItemRepositoryImpl
import com.raymi.app.data.repository.UserPlanRepositoryImpl
import com.raymi.app.data.repository.VestuarioRepositoryImpl
import com.raymi.app.data.repository.WorkspaceRepositoryImpl
import com.raymi.app.domain.repository.AlquilerRepository
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.repository.BusinessRepository
import com.raymi.app.domain.repository.CategoriaRepository
import com.raymi.app.domain.repository.ClienteRepository
import com.raymi.app.domain.repository.ItemRepository
import com.raymi.app.domain.repository.UserPlanRepository
import com.raymi.app.domain.repository.VestuarioRepository
import com.raymi.app.domain.repository.WorkspaceRepository
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

    /**
     * Proporciona la implementación del repositorio de negocios
     */
    @Binds
    @Singleton
    abstract fun bindBusinessRepository(
        businessRepositoryImpl: BusinessRepositoryImpl
    ): BusinessRepository

    /**
     * Proporciona la implementación del repositorio de workspaces
     */
    @Binds
    @Singleton
    abstract fun bindWorkspaceRepository(
        workspaceRepositoryImpl: WorkspaceRepositoryImpl
    ): WorkspaceRepository

    /**
     * Proporciona la implementación del repositorio de ítems (genérico)
     */
    @Binds
    @Singleton
    abstract fun bindItemRepository(
        itemRepositoryImpl: ItemRepositoryImpl
    ): ItemRepository

    /**
     * Proporciona la implementación del repositorio de categorías
     */
    @Binds
    @Singleton
    abstract fun bindCategoriaRepository(
        categoriaRepositoryImpl: CategoriaRepositoryImpl
    ): CategoriaRepository

    /**
     * Proporciona la implementación del repositorio de planes
     */
    @Binds
    @Singleton
    abstract fun bindUserPlanRepository(
        userPlanRepositoryImpl: UserPlanRepositoryImpl
    ): UserPlanRepository

    companion object {
        @Provides
        @Singleton
        fun provideContext(@ApplicationContext context: Context): Context {
            return context
        }
    }
}
