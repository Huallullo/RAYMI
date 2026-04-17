package com.raymi.app.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidatorsTest {

    // ==================== DNI ====================
    @Test
    fun validateDni_correctLength_allDigits_returnsValid() {
        val result = Validators.validateDni("12345678")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validateDni_blank_returnsError() {
        val result = Validators.validateDni("")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El DNI es requerido")
    }

    @Test
    fun validateDni_lessThanRequiredDigits_returnsError() {
        val result = Validators.validateDni("123")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El DNI debe tener 8 dígitos")
    }

    @Test
    fun validateDni_moreThanRequiredDigits_returnsError() {
        val result = Validators.validateDni("123456789")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El DNI debe tener 8 dígitos")
    }

    @Test
    fun validateDni_containsLetters_returnsError() {
        val result = Validators.validateDni("1234A678")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El DNI solo debe contener números")
    }

    @Test
    fun validateDni_containsSpecialCharacters_returnsError() {
        val result = Validators.validateDni("1234-678")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El DNI solo debe contener números")
    }

    // ==================== NOMBRE ====================
    @Test
    fun validateNombre_valid_returnsValid() {
        val result = Validators.validateNombre("Juan Carlos")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validateNombre_blank_returnsError() {
        val result = Validators.validateNombre("  ")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El nombre es requerido")
    }

    @Test
    fun validateNombre_tooShort_returnsError() {
        val result = Validators.validateNombre("J")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El nombre debe tener al menos 2 caracteres")
    }

    @Test
    fun validateNombre_containsDigits_returnsError() {
        val result = Validators.validateNombre("Juan1")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El nombre solo debe contener letras")
    }

    // ==================== APELLIDOS ====================
    @Test
    fun validateApellidos_valid_returnsValid() {
        val result = Validators.validateApellidos("Pérez García")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validateApellidos_blank_returnsError() {
        val result = Validators.validateApellidos("")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("Los apellidos son requeridos")
    }

    @Test
    fun validateApellidos_tooShort_returnsError() {
        val result = Validators.validateApellidos("P")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("Los apellidos deben tener al menos 2 caracteres")
    }

    // ==================== TELEFONO ====================
    @Test
    fun validateTelefono_valid_returnsValid() {
        val result = Validators.validateTelefono("987654321")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validateTelefono_blank_returnsError() {
        val result = Validators.validateTelefono("")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El teléfono es requerido")
    }

    @Test
    fun validateTelefono_tooShort_returnsError() {
        val result = Validators.validateTelefono("12345")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El teléfono debe tener al menos 9 dígitos")
    }

    @Test
    fun validateTelefono_containsLetters_returnsError() {
        val result = Validators.validateTelefono("98765A321")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El teléfono solo debe contener números")
    }

    // ==================== EMAIL ====================
    @Test
    fun validateEmail_optionalBlank_returnsValid() {
        val result = Validators.validateEmail("", isRequired = false)
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validateEmail_requiredBlank_returnsError() {
        val result = Validators.validateEmail("", isRequired = true)
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El email es requerido")
    }

    @Test
    fun validateEmail_invalidFormat_returnsError() {
        val result = Validators.validateEmail("notanemail", isRequired = true)
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("Email inválido")
    }

    @Test
    fun validateEmail_validFormat_returnsValid() {
        val result = Validators.validateEmail("test@example.com")
        assertThat(result.isValid).isTrue()
    }

    // ==================== CODIGO VESTUARIO ====================
    @Test
    fun validateCodigo_valid_returnsValid() {
        val result = Validators.validateCodigo("V001")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validateCodigo_blank_returnsError() {
        val result = Validators.validateCodigo("")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El código es requerido")
    }

    @Test
    fun validateCodigo_tooShort_returnsError() {
        val result = Validators.validateCodigo("A")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El código debe tener al menos 2 caracteres")
    }

    // ==================== PRECIO ====================
    @Test
    fun validatePrecio_positive_returnsValid() {
        val result = Validators.validatePrecio(100.0)
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validatePrecio_zero_returnsError() {
        val result = Validators.validatePrecio(0.0)
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El precio debe ser mayor a 0")
    }

    @Test
    fun validatePrecio_tooHigh_returnsError() {
        val result = Validators.validatePrecio(20000.0)
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El precio parece muy alto")
    }

    @Test
    fun validatePrecioText_valid_returnsValid() {
        val result = Validators.validatePrecioText("150.50")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validatePrecioText_blank_returnsError() {
        val result = Validators.validatePrecioText("")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El precio es requerido")
    }

    @Test
    fun validatePrecioText_notANumber_returnsError() {
        val result = Validators.validatePrecioText("abc")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El precio debe ser un número válido")
    }

    // ==================== ADELANTO ====================
    @Test
    fun validateAdelanto_lessThanTotal_returnsValid() {
        val result = Validators.validateAdelanto(50.0, 100.0)
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validateAdelanto_negative_returnsError() {
        val result = Validators.validateAdelanto(-10.0, 100.0)
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El adelanto no puede ser negativo")
    }

    @Test
    fun validateAdelanto_greaterThanTotal_returnsError() {
        val result = Validators.validateAdelanto(150.0, 100.0)
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("El adelanto no puede ser mayor al precio total")
    }

    // ==================== PASSWORD ====================
    @Test
    fun validatePassword_valid_returnsValid() {
        val result = Validators.validatePassword("password123")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validatePassword_blank_returnsError() {
        val result = Validators.validatePassword("")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("La contraseña es requerida")
    }

    @Test
    fun validatePassword_tooShort_returnsError() {
        val result = Validators.validatePassword("12345")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("caracteres")
    }

    @Test
    fun validatePasswordMatch_match_returnsValid() {
        val result = Validators.validatePasswordMatch("abc123", "abc123")
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun validatePasswordMatch_mismatch_returnsError() {
        val result = Validators.validatePasswordMatch("abc123", "abc124")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("Las contraseñas no coinciden")
    }
}