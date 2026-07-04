package com.raymi.app.domain.usecase.alquiler

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.DomainError
import com.raymi.app.domain.model.EstadoCliente
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import com.raymi.app.domain.repository.ClienteRepository
import com.raymi.app.domain.repository.ItemRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class CreateAlquilerUseCaseTest {

    @MockK
    lateinit var repository: AlquilerRepository

    @MockK
    lateinit var itemRepository: ItemRepository

    @MockK
    lateinit var clienteRepository: ClienteRepository

    private lateinit var useCase: CreateAlquilerUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = CreateAlquilerUseCase(repository, itemRepository, clienteRepository)
    }

    @Test
    fun `crear alquiler con cliente vacio retorna error`() = runBlocking {
        val alquiler = Alquiler(clienteId = "")
        val results = useCase(alquiler).toList()
        
        assertTrue(results.any { it is Resource.Error && it.message == "Debe seleccionar un cliente" })
    }

    @Test
    fun `crear alquiler con saldo negativo retorna error`() = runBlocking {
        val alquiler = Alquiler(
            clienteId = "c1",
            itemId = "i1",
            precioTotal = 100.0,
            adelanto = -10.0
        )
        val results = useCase(alquiler).toList()
        
        assertTrue(results.any { it is Resource.Error && it.message == DomainError.NegativeBalance.message })
    }

    @Test
    fun `crear alquiler con fechas invalidas retorna error`() = runBlocking {
        val ahora = Date()
        val alquiler = Alquiler(
            clienteId = "c1",
            itemId = "i1",
            precioTotal = 100.0,
            adelanto = 10.0,
            fechaInicio = Timestamp(ahora),
            fechaFinPrevista = Timestamp(ahora) // Mismo tiempo es invalido
        )
        val results = useCase(alquiler).toList()
        
        assertTrue(results.any { it is Resource.Error && it.message == DomainError.InvalidDateRange.message })
    }

    @Test
    fun `crear alquiler con adelanto mayor al total retorna error`() = runBlocking {
        val alquiler = Alquiler(
            clienteId = "c1",
            itemId = "i1",
            precioTotal = 100.0,
            adelanto = 150.0
        )
        val results = useCase(alquiler).toList()
        
        assertTrue(results.any { it is Resource.Error && it.message == "El adelanto no puede superar el precio total" })
    }

    @Test
    fun `crear alquiler con precio cero retorna error`() = runBlocking {
        val alquiler = Alquiler(
            clienteId = "c1",
            itemId = "i1",
            precioTotal = 0.0
        )
        val results = useCase(alquiler).toList()
        
        assertTrue(results.any { it is Resource.Error && it.message == "El precio debe ser mayor a 0" })
    }

    @Test
    fun `crear alquiler valido llama al repositorio`() = runBlocking {
        val alquiler = Alquiler(
            workspaceId = "w1",
            clienteId = "c1",
            itemId = "i1",
            precioTotal = 100.0,
            adelanto = 50.0,
            fechaInicio = Timestamp(Date(100000)),
            fechaFinPrevista = Timestamp(Date(200000))
        )

        coEvery { itemRepository.getItemById(any(), any()) } returns flowOf(
            Resource.Success(Item(id = "i1", cantidad = 10, unidadesAlquiladas = 0))
        )
        coEvery { clienteRepository.getClienteById(any()) } returns flowOf(
            Resource.Success(Cliente(id = "c1", estado = EstadoCliente.ACTIVO))
        )

        coEvery { repository.createAlquiler(alquiler) } returns flowOf(Resource.Success("new_id"))
        
        val results = useCase(alquiler).toList()
        
        assertTrue(results.any { it is Resource.Success && it.data == "new_id" })
    }
}
