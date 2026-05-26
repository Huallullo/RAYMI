package com.raymi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.data.remote.FirebaseDataSource.Companion.COLLECTION_NEGOCIOS
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de Clientes.
 */
@Singleton
class ClientDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addClienteTransactional(
        workspaceId: String,
        clienteData: Map<String, Any>,
        dni: String
    ): String {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val clientesRef = negocioRef.collection("clientes")
        val dniIndexRef = negocioRef.collection("clientes_dni_index").document(dni)
        val statsRef = negocioRef.collection("metadata").document("stats")

        return firestore.runTransaction { transaction ->
            if (transaction.get(dniIndexRef).exists()) {
                throw IllegalStateException("Ya existe un cliente con este DNI")
            }
            val clienteRef = clientesRef.document()
            transaction.set(clienteRef, clienteData + mapOf("id" to clienteRef.id))
            transaction.set(dniIndexRef, mapOf("clienteId" to clienteRef.id, "dni" to dni))
            transaction.update(statsRef, "totalClientes", FieldValue.increment(1))
            clienteRef.id
        }.await()
    }
}
