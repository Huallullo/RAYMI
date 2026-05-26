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
     * de unicidad en Firestore se usa la colección `vestuarios_codigo_index`
     * con transacciones atómicas (ver [FirebaseDataSource.addVestuarioWithUniqueCodigo]).
     */
    fun generarCodigoVestuario(): String {
        return generarCodigoConPrefijo("VES")
    }

    fun generarCodigoItem(): String {
        return generarCodigoConPrefijo("ITEM")
    }

    private fun generarCodigoConPrefijo(prefijo: String): String {
        val fecha  = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        val numero = (random.nextInt(900) + 100) // 100-999, siempre 3 dígitos
        return "$prefijo-$fecha-$numero"
    }
}
