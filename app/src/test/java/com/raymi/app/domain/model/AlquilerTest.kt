package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AlquilerTest {

    @Test
    fun `test saldo calculation with penalty`() {
        val alquiler = Alquiler(
            precioTotal = 100.0,
            adelanto = 40.0,
            penalidad = 20.0,
            saldo = 80.0
        )
        // total = 100 + 20 = 120
        // pagado = 40
        // saldo = 120 - 40 = 80
        assertEquals(80.0, alquiler.saldoPendienteReal, 0.01)
    }

    @Test
    fun `test expiration logic`() {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val alquiler = Alquiler(
            fechaFinPrevista = Timestamp(yesterday.time),
            estado = EstadoAlquiler.ACTIVO
        )
        assertEquals(true, alquiler.estaVencido)
    }

    @Test
    fun `test not expired if returned`() {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val alquiler = Alquiler(
            fechaFinPrevista = Timestamp(yesterday.time),
            estado = EstadoAlquiler.DEVUELTO
        )
        assertEquals(false, alquiler.estaVencido)
    }
}
