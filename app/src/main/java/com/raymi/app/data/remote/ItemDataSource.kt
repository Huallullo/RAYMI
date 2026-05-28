package com.raymi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS
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

    /**
     * Elimina un ítem de forma transaccional limpiando su índice de código y actualizando estadísticas.
     */
    suspend fun deleteItemTransactional(
        workspaceId: String,
        itemId: String,
        codigo: String
    ) {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val itemRef = negocioRef.collection("items").document(itemId)
        val codeIndexRef = negocioRef.collection("items_codigo_index").document(codigo)
        val statsRef = negocioRef.collection("metadata").document("stats")

        firestore.runTransaction { transaction ->
            // 1. Eliminar el documento del ítem
            transaction.delete(itemRef)
            // 2. Eliminar la entrada del índice de unicidad
            transaction.delete(codeIndexRef)
            // 3. Decrementar contador global
            transaction.update(statsRef, "totalItems", FieldValue.increment(-1))
        }.await()
    }
}
