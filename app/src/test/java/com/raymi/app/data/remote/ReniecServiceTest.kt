package com.raymi.app.data.remote

import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReniecServiceTest {

    private lateinit var reniecService: ReniecService

    @Before
    fun setUp() {
        reniecService = ReniecService()
    }

    @Test
    fun `consultarPorDni con DNI invalido retorna error`() = runBlocking {
        val result = reniecService.consultarPorDni("123")
        assertTrue(result is Resource.Error)
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", (result as Resource.Error).message)
    }

    @Test
    fun `consultarPorDni con DNI de mock retorna datos exitosos`() = runBlocking {
        val result = reniecService.consultarPorDni("12345678")
        
        assertTrue("El resultado debería ser Success", result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals("Juan Carlos", data?.nombres)
    }

    @Test
    fun `consultarPorDni con DNI inexistente retorna error controlado`() = runBlocking {
        val result = reniecService.consultarPorDni("00000000")
        assertTrue(result is Resource.Error)
    }
}
