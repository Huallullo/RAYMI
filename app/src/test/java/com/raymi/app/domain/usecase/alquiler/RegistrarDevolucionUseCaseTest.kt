package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrarDevolucionUseCaseTest {

    private val repository = mockk<AlquilerRepository>()
    private val useCase = RegistrarDevolucionUseCase(repository)

    @Test
    fun `cuando hay saldo pendiente mayor a 0,01 debe retornar error`() = runTest {
        val alquiler = Alquiler(id = "123", precioTotal = 100.0, adelanto = 50.0) // Saldo 50.0
        coEvery { repository.getAlquilerById("123") } returns flowOf(Resource.Success(alquiler))

        val results = useCase("123").toList()
        
        assertTrue(results.any { it is Resource.Error && it.message?.contains("saldo pendiente") == true })
    }

    @Test
    fun `cuando no hay saldo pendiente debe llamar al repositorio`() = runTest {
        val alquiler = Alquiler(id = "123", precioTotal = 100.0, adelanto = 100.0) // Saldo 0.0
        coEvery { repository.getAlquilerById("123") } returns flowOf(Resource.Success(alquiler))
        coEvery { repository.registrarDevolucion("123", any(), any(), any()) } returns flowOf(Resource.Success(Unit))

        val results = useCase("123").toList()

        assertTrue(results.any { it is Resource.Success })
    }
}
