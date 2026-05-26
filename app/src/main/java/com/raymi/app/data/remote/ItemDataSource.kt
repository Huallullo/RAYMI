package com.raymi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.data.remote.FirebaseDataSource.Companion.COLLECTION_NEGOCIOS
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para el Inventario Genérico (Items).
 */
@Singleton
class ItemDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addItemTransactional(
        workspaceId: String,
        itemData: Map<String, Any>,
        codigo: String
    ): String {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val itemsRef = negocioRef.collection("items")
        val codeIndexRef = negocioRef.collection("items_codigo_index").document(codigo)
        val statsRef = negocioRef.collection("metadata").document("stats")

        return firestore.runTransaction { transaction ->
            if (transaction.get(codeIndexRef).exists()) {
                throw IllegalStateException("Ya existe un producto con este código")
            }
            val itemRef = itemsRef.document()
            transaction.set(itemRef, itemData + mapOf("id" to itemRef.id))
            transaction.set(codeIndexRef, mapOf("itemId" to itemRef.id, "codigo" to codigo))
            transaction.update(statsRef, "totalItems", FieldValue.increment(1))
            itemRef.id
        }.await()
    }
}
