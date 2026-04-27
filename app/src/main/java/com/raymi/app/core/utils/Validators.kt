package com.raymi.app.core.utils

/**
 * Validadores para formularios y campos de entrada
 */
object Validators {

    /**
     * Resultado de una validación
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    // ========== VALIDADORES DE CLIENTE ==========

    /**
     * Valida el DNI de un cliente
     */
    fun validateDni(dni: String): ValidationResult {
        return when {
            dni.isBlank() -> ValidationResult(false, "El DNI es requerido")
            dni.length != Constants.DNI_LENGTH -> ValidationResult(
                false,
                "El DNI debe tener ${Constants.DNI_LENGTH} dígitos"
            )
            !dni.all { it.isDigit() } -> ValidationResult(false, "El DNI solo debe contener números")
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida el nombre
     */
    fun validateNombre(nombre: String): ValidationResult {
        return when {
            nombre.isBlank() -> ValidationResult(false, "El nombre es requerido")
            nombre.length < 2 -> ValidationResult(false, "El nombre debe tener al menos 2 caracteres")
            !nombre.all { it.isLetter() || it.isWhitespace() } -> ValidationResult(
                false,
                "El nombre solo debe contener letras"
            )
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida los apellidos
     */
    fun validateApellidos(apellidos: String): ValidationResult {
        return when {
            apellidos.isBlank() -> ValidationResult(false, "Los apellidos son requeridos")
            apellidos.length < 2 -> ValidationResult(false, "Los apellidos deben tener al menos 2 caracteres")
            !apellidos.all { it.isLetter() || it.isWhitespace() } -> ValidationResult(
                false,
                "Los apellidos solo deben contener letras"
            )
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida el teléfono
     */
    fun validateTelefono(telefono: String): ValidationResult {
        return when {
            telefono.isBlank() -> ValidationResult(false, "El teléfono es requerido")
            telefono.length < Constants.MIN_PHONE_LENGTH -> ValidationResult(
                false,
                "El teléfono debe tener al menos ${Constants.MIN_PHONE_LENGTH} dígitos"
            )
            !telefono.all { it.isDigit() } -> ValidationResult(
                false,
                "El teléfono solo debe contener números"
            )
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida el email (opcional)
     */
    fun validateEmail(email: String, isRequired: Boolean = false): ValidationResult {
        if (!isRequired && email.isBlank()) {
            return ValidationResult(true)
        }

        return when {
            isRequired && email.isBlank() -> ValidationResult(false, "El email es requerido")
            email.isNotBlank() && !email.isValidEmail() -> ValidationResult(false, "Email inválido")
            else -> ValidationResult(true)
        }
    }

    // ========== VALIDADORES DE VESTUARIO ==========

    /**
     * Valida el código del vestuario
     */
    fun validateCodigo(codigo: String): ValidationResult {
        return when {
            codigo.isBlank() -> ValidationResult(false, "El código es requerido")
            codigo.length < 2 -> ValidationResult(false, "El código debe tener al menos 2 caracteres")
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida el nombre de la danza
     */
    fun validateDanza(danza: String): ValidationResult {
        return when {
            danza.isBlank() -> ValidationResult(false, "El nombre de la danza es requerido")
            danza.length < 3 -> ValidationResult(false, "El nombre debe tener al menos 3 caracteres")
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida el precio
     */
    fun validatePrecio(precio: Double): ValidationResult {
        return when {
            precio <= 0 -> ValidationResult(false, "El precio debe ser mayor a 0")
            precio > 10000 -> ValidationResult(false, "El precio parece muy alto")
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida el precio desde texto
     */
    fun validatePrecioText(precioText: String): ValidationResult {
        if (precioText.isBlank()) {
            return ValidationResult(false, "El precio es requerido")
        }

        val precio = precioText.toDoubleOrNull()
        return if (precio == null) {
            ValidationResult(false, "El precio debe ser un número válido")
        } else {
            validatePrecio(precio)
        }
    }

    // ========== VALIDADORES DE ALQUILER ==========

    /**
     * Valida el adelanto
     */
    fun validateAdelanto(adelanto: Double, precioTotal: Double): ValidationResult {
        return when {
            adelanto < 0 -> ValidationResult(false, "El adelanto no puede ser negativo")
            adelanto > precioTotal -> ValidationResult(
                false,
                "El adelanto no puede ser mayor al precio total"
            )
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida el adelanto desde texto
     */
    fun validateAdelantoText(adelantoText: String, precioTotal: Double): ValidationResult {
        if (adelantoText.isBlank()) {
            return ValidationResult(true) // El adelanto es opcional
        }

        val adelanto = adelantoText.toDoubleOrNull()
        return if (adelanto == null) {
            ValidationResult(false, "El adelanto debe ser un número válido")
        } else {
            validateAdelanto(adelanto, precioTotal)
        }
    }

    // ========== VALIDADORES DE AUTENTICACIÓN ==========

    /**
     * Valida la contraseña
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "La contraseña es requerida")
            password.length < Constants.MIN_PASSWORD_LENGTH -> ValidationResult(
                false,
                "La contraseña debe tener al menos ${Constants.MIN_PASSWORD_LENGTH} caracteres"
            )
            else -> ValidationResult(true)
        }
    }

    /**
     * Valida que dos contraseñas coincidan
     */
    fun validatePasswordMatch(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, "Confirma la contraseña")
            password != confirmPassword -> ValidationResult(false, "Las contraseñas no coinciden")
            else -> ValidationResult(true)
        }
    }

    // ========== VALIDADORES GENERALES ==========

    /**
     * Valida que un campo no esté vacío
     */
    fun validateNotEmpty(value: String, fieldName: String = "Campo"): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult(false, "$fieldName es requerido")
        } else {
            ValidationResult(true)
        }
    }

    /**
     * Valida longitud mínima
     */
    fun validateMinLength(value: String, minLength: Int, fieldName: String = "Campo"): ValidationResult {
        return if (value.length < minLength) {
            ValidationResult(false, "$fieldName debe tener al menos $minLength caracteres")
        } else {
            ValidationResult(true)
        }
    }

    /**
     * Valida longitud máxima
     */
    fun validateMaxLength(value: String, maxLength: Int, fieldName: String = "Campo"): ValidationResult {
        return if (value.length > maxLength) {
            ValidationResult(false, "$fieldName no puede tener más de $maxLength caracteres")
        } else {
            ValidationResult(true)
        }
    }
}
