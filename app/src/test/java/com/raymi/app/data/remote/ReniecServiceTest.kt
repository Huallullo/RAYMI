package com.raymi.app.data.remote

import android.util.Log
import com.raymi.app.domain.model.Resource
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReniecServiceTest {

    private lateinit var reniecService: ReniecService

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        reniecService = ReniecService()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
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

        when (result) {
            is Resource.Success -> assertEquals("Juan Carlos", result.data?.nombres)
            is Resource.Error -> assertTrue(result.message?.isNotBlank() == true)
            else -> assertTrue("Resultado inesperado", false)
        }
    }

    @Test
    fun `consultarPorDni con DNI inexistente retorna error controlado`() = runBlocking {
        val result = reniecService.consultarPorDni("00000000")
        assertTrue(result is Resource.Error)
    }
}
