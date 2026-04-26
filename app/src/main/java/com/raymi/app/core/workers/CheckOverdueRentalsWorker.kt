package com.raymi.app.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.usecase.notifications.EnviarMensajeUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * Worker que verifica alquileres vencidos y envía notificaciones push y mensajes
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
            // Obtener todos los alquileres
            val alquileres = firebaseDataSource.getAllDocuments(FirebaseDataSource.COLLECTION_ALQUILERES)
                .map { (id, data) ->
                    // Convertir a Alquiler (simplificado)
                    Alquiler(
                        id = id,
                        clienteId = data["clienteId"] as? String ?: "",
                        clienteNombre = data["clienteNombre"] as? String ?: "",
                        vestuarioId = data["vestuarioId"] as? String ?: "",
                        vestuarioNombre = data["vestuarioNombre"] as? String ?: "",
                        vestuarioCodigo = data["vestuarioCodigo"] as? String ?: "",
                        cantidad = (data["cantidad"] as? Long)?.toInt() ?: 1,
                        fechaInicio = data["fechaInicio"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        fechaFinPrevista = data["fechaFinPrevista"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        fechaDevolucion = data["fechaDevolucion"] as? com.google.firebase.Timestamp,
                        precioUnitario = (data["precioUnitario"] as? Double) ?: 0.0,
                        precioTotal = (data["precioTotal"] as? Double) ?: 0.0,
                        adelanto = (data["adelanto"] as? Double) ?: 0.0,
                        saldo = (data["saldo"] as? Double) ?: 0.0,
                        estado = when (data["estado"] as? String) {
                            "ACTIVO" -> com.raymi.app.domain.model.EstadoAlquiler.ACTIVO
                            "DEVUELTO" -> com.raymi.app.domain.model.EstadoAlquiler.DEVUELTO
                            "VENCIDO" -> com.raymi.app.domain.model.EstadoAlquiler.VENCIDO
                            "CANCELADO" -> com.raymi.app.domain.model.EstadoAlquiler.CANCELADO
                            else -> com.raymi.app.domain.model.EstadoAlquiler.ACTIVO
                        },
                        observaciones = data["observaciones"] as? String ?: "",
                        createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                    )
                }

            // Filtrar alquileres vencidos
            val vencidos = alquileres.filter { it.estaVencido }

            if (vencidos.isNotEmpty()) {
                // Enviar notificación push
                sendOverdueNotification(vencidos.size)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun sendOverdueNotification(count: Int) {
        try {
            // Obtener token FCM (en producción, enviar a servidor)
            val token = FirebaseMessaging.getInstance().token.await()

            // Aquí normalmente enviarías la notificación desde un servidor
            // Por simplicidad, solo logueamos
            println("Alquileres vencidos: $count")

            // Enviar mensajes a clientes con alquileres vencidos
            val vencidos = getVencidos() // Necesitamos obtener los alquileres vencidos
            for (alquiler in vencidos) {
                enviarMensajeUseCase.enviarRecordatorioVencido(
                    telefonoCliente = getTelefonoCliente(alquiler.clienteId),
                    nombreCliente = alquiler.clienteNombre,
                    vestuarioNombre = alquiler.vestuarioNombre,
                    diasVencido = alquiler.diasRestantes * -1 // Días vencido
                ).collect { result ->
                    when (result) {
                        is com.raymi.app.domain.model.Resource.Success -> {
                            println("Mensaje enviado a ${alquiler.clienteNombre}: ${result.data}")
                        }
                        is com.raymi.app.domain.model.Resource.Error -> {
                            println("Error enviando mensaje a ${alquiler.clienteNombre}: ${result.message}")
                        }
                        is com.raymi.app.domain.model.Resource.Loading -> {
                            // Loading
                        }
                    }
                }
            }

            // En una implementación real, usarías Firebase Functions o un servidor
            // para enviar notificaciones push a administradores

        } catch (e: Exception) {
            // Manejar error
        }
    }

    private suspend fun getVencidos(): List<Alquiler> {
        // Reutilizar la lógica del doWork para obtener alquileres vencidos
        return firebaseDataSource.getAllDocuments(FirebaseDataSource.COLLECTION_ALQUILERES)
            .map { (id, data) ->
                Alquiler(
                    id = id,
                    clienteId = data["clienteId"] as? String ?: "",
                    clienteNombre = data["clienteNombre"] as? String ?: "",
                    vestuarioId = data["vestuarioId"] as? String ?: "",
                    vestuarioNombre = data["vestuarioNombre"] as? String ?: "",
                    vestuarioCodigo = data["vestuarioCodigo"] as? String ?: "",
                    cantidad = (data["cantidad"] as? Long)?.toInt() ?: 1,
                    fechaInicio = data["fechaInicio"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                    fechaFinPrevista = data["fechaFinPrevista"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                    fechaDevolucion = data["fechaDevolucion"] as? com.google.firebase.Timestamp,
                    precioUnitario = (data["precioUnitario"] as? Double) ?: 0.0,
                    precioTotal = (data["precioTotal"] as? Double) ?: 0.0,
                    adelanto = (data["adelanto"] as? Double) ?: 0.0,
                    saldo = (data["saldo"] as? Double) ?: 0.0,
                    estado = when (data["estado"] as? String) {
                        "ACTIVO" -> com.raymi.app.domain.model.EstadoAlquiler.ACTIVO
                        "DEVUELTO" -> com.raymi.app.domain.model.EstadoAlquiler.DEVUELTO
                        "VENCIDO" -> com.raymi.app.domain.model.EstadoAlquiler.VENCIDO
                        "CANCELADO" -> com.raymi.app.domain.model.EstadoAlquiler.CANCELADO
                        else -> com.raymi.app.domain.model.EstadoAlquiler.ACTIVO
                    },
                    observaciones = data["observaciones"] as? String ?: "",
                    createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                    updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                )
            }.filter { it.estaVencido }
    }

    private suspend fun getTelefonoCliente(clienteId: String): String {
        val clienteData = firebaseDataSource.getDocument(FirebaseDataSource.COLLECTION_CLIENTES, clienteId)
        return clienteData?.get("telefono") as? String ?: ""
    }
}
