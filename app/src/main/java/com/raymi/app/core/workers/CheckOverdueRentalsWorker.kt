package com.raymi.app.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.core.notifications.NotificationHelper
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.usecase.notifications.EnviarMensajeUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Worker que verifica diariamente alquileres vencidos y notifica a los clientes.
 * Optimizado para SaaS (Collection Group Query) para reducir lecturas de Firestore.
 */
@HiltWorker
class CheckOverdueRentalsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firebaseDataSource: FirebaseDataSource,
    private val enviarMensajeUseCase: EnviarMensajeUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val vencidos = obtenerAlquileresVencidosSaaS()

            if (vencidos.isEmpty()) {
                return Result.success()
            }

            val notificationHelper = NotificationHelper(applicationContext)
            val stringsCache = mutableMapOf<String, com.raymi.app.core.lang.RaymiStrings>()

            vencidos.forEach { alquiler ->
                // ✅ FIX PROBLEM 8b: Memoizar lectura de idioma por negocio (Solo 1 read por negocio único)
                val strings = stringsCache.getOrPut(alquiler.workspaceId) {
                    obtenerStringsParaNegocio(alquiler.workspaceId)
                }
                
                notificarClienteVencido(alquiler)
                marcarComoVencidoEnFirestore(alquiler)
                notificationHelper.sendOverdueNotification(
                    alquilerId = alquiler.id,
                    title = strings.overdueDetected,
                    content = if (strings is com.raymi.app.core.lang.SpanishStrings) 
                        "El alquiler de ${alquiler.clienteNombre} ha vencido." 
                        else "Rental for ${alquiler.clienteNombre} has expired."
                )
            }

            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Obtiene todos los alquileres activos vencidos de TODOS los negocios en una sola query.
     * Uso de Collection Group Query: Escala sin importar el número de negocios.
     */
    private suspend fun obtenerAlquileresVencidosSaaS(): List<Alquiler> {
        return try {
            firebaseDataSource.queryCollectionGroup(
                collectionId = "alquileres",
                field = "estado",
                value = "ACTIVO",
                limit = 5000
            ).mapNotNull { (id, data) ->
                val alquiler = mapToAlquiler(id, data)
                // Filtramos por fecha en memoria (Firestore no permite rangos en Collection Groups sin indexar cada fecha)
                if (alquiler != null && alquiler.estaVencido) alquiler else null
            }
        } catch (e: Exception) {
            android.util.Log.e("CheckOverdueWorker", "Error en SaaS Collection Group query: ${e.message}")
            emptyList()
        }
    }

    private suspend fun marcarComoVencidoEnFirestore(alquiler: Alquiler) {
        if (alquiler.workspaceId.isBlank()) {
            android.util.Log.w("CheckOverdueWorker", "Alquiler ${alquiler.id} sin workspaceId (campo legacy). Ignorando actualización.")
            return
        }
        try {
            firebaseDataSource.updateBusinessDocument(
                collection = "alquileres",
                documentId = alquiler.id,
                data = mapOf(
                    "estado" to "VENCIDO",
                    "updatedAt" to Timestamp.now()
                ),
                negocioId = alquiler.workspaceId
            )
        } catch (_: Exception) { }
    }

    private suspend fun notificarClienteVencido(alquiler: Alquiler) {
        try {
            // ✅ FIX PROBLEM 8a: Usar el teléfono guardado en el alquiler (0 reads Firestore)
            val telefonoCliente = alquiler.clienteTelefono
            if (telefonoCliente.isBlank()) return

            enviarMensajeUseCase.enviarRecordatorioDevolucion(
                telefono = telefonoCliente,
                cliente = alquiler.clienteNombre,
                item = alquiler.itemNombre,
                esVencido = true
            ).collect { }
        } catch (_: Exception) { }
    }

    private suspend fun obtenerStringsParaNegocio(workspaceId: String): com.raymi.app.core.lang.RaymiStrings {
        return try {
            val data = firebaseDataSource.getDocument(com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS, workspaceId)
            val idioma = data?.get("idioma") as? String ?: "es"
            if (idioma == "en") com.raymi.app.core.lang.EnglishStrings() else com.raymi.app.core.lang.SpanishStrings()
        } catch (_: Exception) {
            com.raymi.app.core.lang.SpanishStrings()
        }
    }

    private fun mapToAlquiler(id: String, data: Map<String, Any>): Alquiler? {
        return try {
            Alquiler(
                id              = id,
                workspaceId     = data["workspaceId"]     as? String    ?: "", 
                clienteId       = data["clienteId"]       as? String    ?: return null,
                clienteNombre   = data["clienteNombre"]   as? String    ?: "",
                itemId          = data["itemId"]          as? String    ?: (data["vestuarioId"] as? String) ?: return null,
                itemNombre      = data["itemNombre"]      as? String    ?: (data["vestuarioNombre"] as? String) ?: "",
                itemCodigo      = data["itemCodigo"]      as? String    ?: (data["vestuarioCodigo"] as? String) ?: "",
                cantidad        = (data["cantidad"]       as? Number)?.toInt() ?: 1,
                fechaInicio     = data["fechaInicio"]     as? Timestamp ?: Timestamp.now(),
                fechaFinPrevista= data["fechaFinPrevista"]as? Timestamp ?: Timestamp.now(),
                fechaDevolucion = data["fechaDevolucion"] as? Timestamp,
                precioUnitario  = (data["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                precioTotal     = (data["precioTotal"]    as? Number)?.toDouble() ?: 0.0,
                adelanto        = (data["adelanto"]       as? Number)?.toDouble() ?: 0.0,
                saldo           = (data["saldo"]          as? Number)?.toDouble() ?: 0.0,
                garantia        = (data["garantia"]       as? Number)?.toDouble() ?: 0.0,
                penalidad       = (data["penalidad"]      as? Number)?.toDouble() ?: 0.0,
                estado = when (data["estado"] as? String) {
                    "ACTIVO"    -> EstadoAlquiler.ACTIVO
                    "DEVUELTO"  -> EstadoAlquiler.DEVUELTO
                    "VENCIDO"   -> EstadoAlquiler.VENCIDO
                    "CANCELADO" -> EstadoAlquiler.CANCELADO
                    else        -> EstadoAlquiler.ACTIVO
                },
                observaciones   = data["observaciones"]   as? String    ?: "",
                createdAt       = data["createdAt"]       as? Timestamp ?: Timestamp.now(),
                updatedAt       = data["updatedAt"]       as? Timestamp ?: Timestamp.now(),
                clienteTelefono = data["clienteTelefono"] as? String    ?: ""
            )
        } catch (_: Exception) { null }
    }
}
