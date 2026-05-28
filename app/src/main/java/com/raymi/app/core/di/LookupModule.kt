package com.raymi.app.core.di

import com.raymi.app.data.remote.*
import com.raymi.app.domain.repository.RucLookupProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LookupModule {

    @Binds
    @Singleton
    abstract fun bindApiPeruProvider(impl: ApiPeruRucProvider): RucLookupProvider

    @Binds
    @Singleton
    abstract fun bindNubefactInvoiceProvider(impl: NubefactInvoiceProvider): com.raymi.app.domain.repository.InvoiceProvider
}
