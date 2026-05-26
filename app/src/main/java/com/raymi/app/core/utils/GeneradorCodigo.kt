package com.raymi.app.core.utils

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generador de códigos únicos para vestuarios.
 *
 * Usa [SecureRandom] en lugar de [kotlin.random.Random] para garantizar
 * la unicidad incluso cuando se llama desde múltiples coroutines.
 */
object GeneradorCodigo {

    private val random = SecureRandom()

    /**
     * Genera un código único para vestuario.
     *
     * Formato: VES-YYMMDD-XXX  (ej: VES-260426-847)
     *
     * El componente aleatorio de 3 dígitos reduce la probabilidad de colisión
     * cuando se crean varios vestuarios el mismo día. Para una garantía absoluta
     * de unicidad en Firestore se usa la colección `items_codigo_index`
     * con transacciones atómicas (ver ItemDataSource).
     */
    fun generarCodigoItem(): String {
        return generarCodigoConPrefijo("ITEM")
    }

    private fun generarCodigoConPrefijo(prefijo: String): String {
        val fecha  = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        // Aumentamos a 4 dígitos para reducir colisiones a 1/9000 por día por prefijo
        val numero = (random.nextInt(9000) + 1000)
        return "$prefijo-$fecha-$numero"
    }
}
