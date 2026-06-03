package com.raymi.app.core.di

import android.content.Context
import com.raymi.app.data.repository.*
import com.raymi.app.domain.repository.*
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

    /**
     * Proporciona la implementación del repositorio de comprobantes
     */
    @Binds
    @Singleton
    abstract fun bindComprobanteRepository(
        comprobanteRepositoryImpl: ComprobanteRepositoryImpl
    ): ComprobanteRepository

    /**
     * Proporciona la implementación del repositorio de consultas externas
     */
    @Binds
    @Singleton
    abstract fun bindExternalLookupRepository(
        externalLookupRepositoryImpl: ExternalLookupRepositoryImpl
    ): ExternalLookupRepository

    @Binds
    @Singleton
    abstract fun bindMantenimientoRepository(
        mantenimientoRepositoryImpl: MantenimientoRepositoryImpl
    ): MantenimientoRepository

    @Binds
    @Singleton
    abstract fun bindPdfGeneratorPort(
        pdfService: com.raymi.app.data.remote.PdfService
    ): com.raymi.app.domain.port.PdfGeneratorPort

    @Binds
    @Singleton
    abstract fun bindInvoiceGeneratorPort(
        fallbackInvoiceService: com.raymi.app.data.remote.FallbackInvoiceService
    ): com.raymi.app.domain.port.InvoiceGeneratorPort

    companion object {
        @Provides
        @Singleton
        fun provideConnectivityObserver(@ApplicationContext context: Context): com.raymi.app.core.utils.ConnectivityObserver {
            return com.raymi.app.core.utils.ConnectivityObserver(context)
        }

        @Provides
        @Singleton
        fun provideContext(@ApplicationContext context: Context): Context {
            return context
        }
    }
}
