package com.raymi.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS
import com.raymi.app.domain.model.TipoComprobante
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComprobanteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getNextNumberAtomic(workspaceId: String, tipo: TipoComprobante): Int {
        val metadataRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("metadata").document("comprobantes")
        
        val fieldName = when (tipo) {
            TipoComprobante.TICKET -> "ticketNextNumber"
            TipoComprobante.BOLETA -> "boletaNextNumber"
            TipoComprobante.FACTURA -> "facturaNextNumber"
        }

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(metadataRef)
            val currentNumber = if (snapshot.exists()) {
                (snapshot.get(fieldName) as? Number)?.toInt() ?: 1
            } else {
                1
            }
            
            transaction.set(metadataRef, mapOf(fieldName to currentNumber + 1), com.google.firebase.firestore.SetOptions.merge())
            currentNumber
        }.await()
    }

    suspend fun saveComprobante(workspaceId: String, data: Map<String, Any>): String {
        val id = data["id"] as? String
        val docRef = if (id.isNullOrBlank()) {
            firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
                .collection("comprobantes").document()
        } else {
            firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
                .collection("comprobantes").document(id)
        }
        
        val finalData = if (id.isNullOrBlank()) {
            data + mapOf("id" to docRef.id)
        } else data
        
        docRef.set(finalData, com.google.firebase.firestore.SetOptions.merge()).await()
        return docRef.id
    }
}
