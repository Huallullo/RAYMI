package com.raymi.app.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidatorsTest {

    // ==================== DNI ====================
    @Test
    fun validarDni_correcto_8DigitosNumericos_devuelveValido() {
        val resultado = Validators.validateDni("12345678")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarDni_vacio_devuelveError() {
        val resultado = Validators.validateDni("")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El DNI es requerido")
    }

    @Test
    fun validarDni_menosDe8Digitos_devuelveError() {
        val resultado = Validators.validateDni("1234567")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El DNI debe tener 8 dígitos")
    }

    @Test
    fun validarDni_masDe8Digitos_devuelveError() {
        val resultado = Validators.validateDni("123456789")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El DNI debe tener 8 dígitos")
    }

    @Test
    fun validarDni_contieneLetras_devuelveError() {
        val resultado = Validators.validateDni("1234A678")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El DNI solo debe contener números")
    }

    @Test
    fun validarDni_contieneCaracteresEspeciales_devuelveError() {
        val resultado = Validators.validateDni("1234-678")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El DNI solo debe contener números")
    }

    // ==================== NOMBRE ====================
    @Test
    fun validarNombre_valido_devuelveValido() {
        val resultado = Validators.validateNombre("Juan Carlos")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarNombre_vacio_devuelveError() {
        val resultado = Validators.validateNombre("  ")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El nombre es requerido")
    }

    @Test
    fun validarNombre_muyCorto_devuelveError() {
        val resultado = Validators.validateNombre("J")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El nombre debe tener al menos 2 caracteres")
    }

    @Test
    fun validarNombre_contieneDigitos_devuelveError() {
        val resultado = Validators.validateNombre("Juan1")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El nombre solo debe contener letras")
    }

    // ==================== APELLIDOS ====================
    @Test
    fun validarApellidos_validos_devuelveValido() {
        val resultado = Validators.validateApellidos("Pérez García")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarApellidos_vacio_devuelveError() {
        val resultado = Validators.validateApellidos("")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("Los apellidos son requeridos")
    }

    @Test
    fun validarApellidos_muyCorto_devuelveError() {
        val resultado = Validators.validateApellidos("P")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("Los apellidos deben tener al menos 2 caracteres")
    }

    // ==================== TELEFONO ====================
    @Test
    fun validarTelefono_valido_9DigitosEmpiezaCon9_devuelveValido() {
        val resultado = Validators.validateTelefono("987654321")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarTelefono_vacio_devuelveError() {
        val resultado = Validators.validateTelefono("")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El teléfono es requerido")
    }

    @Test
    fun validarTelefono_menosDe9Digitos_devuelveError() {
        val resultado = Validators.validateTelefono("12345")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El teléfono debe tener al menos 9 dígitos")
    }

    @Test
    fun validarTelefono_contieneLetras_devuelveError() {
        val resultado = Validators.validateTelefono("98765A321")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El teléfono solo debe contener números")
    }

    // ==================== EMAIL ====================
    @Test
    fun validarEmail_opcionalVacio_devuelveValido() {
        val resultado = Validators.validateEmail("", isRequired = false)
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarEmail_requeridoVacio_devuelveError() {
        val resultado = Validators.validateEmail("", isRequired = true)
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El email es requerido")
    }

    @Test
    fun validarEmail_formatoInvalido_devuelveError() {
        val resultado = Validators.validateEmail("correoInvalido", isRequired = true)
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("Email inválido")
    }

    @Test
    fun validarEmail_formatoValido_devuelveValido() {
        val resultado = Validators.validateEmail("usuario@dominio.com")
        assertThat(resultado.isValid).isTrue()
    }

    // ==================== CODIGO VESTUARIO ====================
    @Test
    fun validarCodigoVestuario_valido_devuelveValido() {
        val resultado = Validators.validateCodigo("V001")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarCodigoVestuario_vacio_devuelveError() {
        val resultado = Validators.validateCodigo("")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El código es requerido")
    }

    @Test
    fun validarCodigoVestuario_muyCorto_devuelveError() {
        val resultado = Validators.validateCodigo("A")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El código debe tener al menos 2 caracteres")
    }

    // ==================== PRECIO ====================
    @Test
    fun validarPrecio_positivo_devuelveValido() {
        val resultado = Validators.validatePrecio(100.0)
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarPrecio_cero_devuelveError() {
        val resultado = Validators.validatePrecio(0.0)
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El precio debe ser mayor a 0")
    }

    @Test
    fun validarPrecio_muyAlto_devuelveError() {
        val resultado = Validators.validatePrecio(20000.0)
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El precio parece muy alto")
    }

    @Test
    fun validarPrecioTexto_valido_devuelveValido() {
        val resultado = Validators.validatePrecioText("150.50")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarPrecioTexto_vacio_devuelveError() {
        val resultado = Validators.validatePrecioText("")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El precio es requerido")
    }

    @Test
    fun validarPrecioTexto_noEsNumero_devuelveError() {
        val resultado = Validators.validatePrecioText("abc")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El precio debe ser un número válido")
    }

    // ==================== ADELANTO ====================
    @Test
    fun validarAdelanto_menorQueTotal_devuelveValido() {
        val resultado = Validators.validateAdelanto(50.0, 100.0)
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarAdelanto_negativo_devuelveError() {
        val resultado = Validators.validateAdelanto(-10.0, 100.0)
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El adelanto no puede ser negativo")
    }

    @Test
    fun validarAdelanto_mayorQueTotal_devuelveError() {
        val resultado = Validators.validateAdelanto(150.0, 100.0)
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("El adelanto no puede ser mayor al precio total")
    }

    // ==================== CONTRASEÑA ====================
    @Test
    fun validarPassword_valida_devuelveValido() {
        val resultado = Validators.validatePassword("clave123")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarPassword_vacia_devuelveError() {
        val resultado = Validators.validatePassword("")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("La contraseña es requerida")
    }

    @Test
    fun validarPassword_muyCorta_devuelveError() {
        val resultado = Validators.validatePassword("12345")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).contains("caracteres")
    }

    @Test
    fun validarPasswordCoinciden_coinciden_devuelveValido() {
        val resultado = Validators.validatePasswordMatch("abc123", "abc123")
        assertThat(resultado.isValid).isTrue()
    }

    @Test
    fun validarPasswordCoinciden_noCoinciden_devuelveError() {
        val resultado = Validators.validatePasswordMatch("abc123", "abc124")
        assertThat(resultado.isValid).isFalse()
        assertThat(resultado.errorMessage).isEqualTo("Las contraseñas no coinciden")
    }
}
