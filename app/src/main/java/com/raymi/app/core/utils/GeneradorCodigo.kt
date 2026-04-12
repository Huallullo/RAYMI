package com.raymi.app.core.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Generador de códigos únicos para vestuarios
 */
object GeneradorCodigo {

    /**
     * Genera un código único para vestuario
     * Formato: VES-YYMMDD-XXX
     * Ejemplo: VES-251125-001
     */
    fun generarCodigoVestuario(): String {
        val fechaFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
        val fecha = fechaFormat.format(Date())
        val random = (1..999).random().toString().padStart(3, '0')
        return "VES-$fecha-$random"
    }
}