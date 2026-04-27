package com.raymi.app.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.usecase.notifications.EnviarMensajeUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Worker que verifica diariamente alquileres vencidos y notifica a los clientes.
 *
 * Se programa desde [ScheduleOverdueCheckUseCase] con periodicidad de 1 día.
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
            val vencidos = obtenerAlquileresVencidos()

            if (vencidos.isEmpty()) {
                return Result.success()
            }

            // Notificar a cada cliente con alquiler vencido
            vencidos.forEach { alquiler ->
                notificarClienteVencido(alquiler)
            }

            Result.success()
        } catch (e: Exception) {
            // Reintentar si hay error de red; fallar definitivamente en errores de lógica
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Obtiene todos los alquileres activos que están vencidos desde Firestore.
     */
    private suspend fun obtenerAlquileresVencidos(): List<Alquiler> {
        return firebaseDataSource
            .getAllDocuments(FirebaseDataSource.COLLECTION_ALQUILERES)
            .mapNotNull { (id, data) -> mapToAlquiler(id, data) }
            .filter { it.estado == EstadoAlquiler.ACTIVO && it.estaVencido }
    }

    /**
     * Envía notificación WhatsApp/SMS al cliente del alquiler vencido.
     * Los errores de envío se ignoran para no bloquear el resto de notificaciones.
     */
    private suspend fun notificarClienteVencido(alquiler: Alquiler) {
        try {
            val telefonoCliente = obtenerTelefonoCliente(alquiler.clienteId)
            if (telefonoCliente.isBlank()) return

            // Ignoramos el resultado; si falla, el siguiente ciclo lo reintentará
            enviarMensajeUseCase.enviarRecordatorioVencido(
                telefonoCliente = telefonoCliente,
                nombreCliente   = alquiler.clienteNombre,
                vestuarioNombre = alquiler.vestuarioNombre,
                diasVencido     = (-alquiler.diasRestantes).coerceAtLeast(1)
            ).first()
        } catch (_: Exception) {
            // No propagamos el error para seguir con el resto de alquileres
        }
    }

    /**
     * Obtiene el teléfono de un cliente desde Firestore.
     */
    private suspend fun obtenerTelefonoCliente(clienteId: String): String {
        if (clienteId.isBlank()) return ""
        return try {
            val data = firebaseDataSource.getDocument(
                FirebaseDataSource.COLLECTION_CLIENTES,
                clienteId
            )
            data?.get("telefono") as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    // ─── Mapeo de datos ──────────────────────────────────────────────────────

    private fun mapToAlquiler(id: String, data: Map<String, Any>): Alquiler? {
        return try {
            Alquiler(
                id              = id,
                clienteId       = data["clienteId"]       as? String    ?: return null,
                clienteNombre   = data["clienteNombre"]   as? String    ?: "",
                vestuarioId     = data["vestuarioId"]     as? String    ?: return null,
                vestuarioNombre = data["vestuarioNombre"] as? String    ?: "",
                vestuarioCodigo = data["vestuarioCodigo"] as? String    ?: "",
                cantidad        = (data["cantidad"]       as? Long)?.toInt() ?: 1,
                fechaInicio     = data["fechaInicio"]     as? Timestamp ?: Timestamp.now(),
                fechaFinPrevista= data["fechaFinPrevista"]as? Timestamp ?: Timestamp.now(),
                fechaDevolucion = data["fechaDevolucion"] as? Timestamp,
                precioUnitario  = (data["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                precioTotal     = (data["precioTotal"]    as? Number)?.toDouble() ?: 0.0,
                adelanto        = (data["adelanto"]       as? Number)?.toDouble() ?: 0.0,
                saldo           = (data["saldo"]          as? Number)?.toDouble() ?: 0.0,
                estado = when (data["estado"] as? String) {
                    "ACTIVO"    -> EstadoAlquiler.ACTIVO
                    "DEVUELTO"  -> EstadoAlquiler.DEVUELTO
                    "VENCIDO"   -> EstadoAlquiler.VENCIDO
                    "CANCELADO" -> EstadoAlquiler.CANCELADO
                    else        -> EstadoAlquiler.ACTIVO
                },
                observaciones   = data["observaciones"]   as? String    ?: "",
                createdAt       = data["createdAt"]       as? Timestamp ?: Timestamp.now(),
                updatedAt       = data["updatedAt"]       as? Timestamp ?: Timestamp.now()
            )
        } catch (_: Exception) {
            null
        }
    }
}
