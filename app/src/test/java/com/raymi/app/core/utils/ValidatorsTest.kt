package com.raymi.app.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun validateDni_invalido_si_no_tiene_8_digitos() {
        val result = Validators.validateDni("12345")
        assertFalse(result.isValid)
    }

    @Test
    fun validateDni_valido_si_tiene_8_digitos_numericos() {
        val result = Validators.validateDni("12345678")
        assertTrue(result.isValid)
    }

    @Test
    fun validateEmail_valido_en_formato_correcto() {
        val result = Validators.validateEmail("test@mail.com", isRequired = true)
        assertTrue(result.isValid)
    }

    @Test
    fun validatePrecio_invalido_si_es_cero() {
        val result = Validators.validatePrecio(0.0)
        assertFalse(result.isValid)
    }
}