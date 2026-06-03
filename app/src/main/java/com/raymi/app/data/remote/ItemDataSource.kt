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
            // 1. LEER datos necesarios
            val itemSnap = transaction.get(itemRef)
            if (!itemSnap.exists()) return@runTransaction
            
            // ✅ [A-05] Verificación de integridad: No borrar si está alquilado
            val unidadesAlquiladas = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 0
            if (unidadesAlquiladas > 0) {
                throw IllegalStateException("No se puede eliminar: El ítem tiene $unidadesAlquiladas unidades en alquiler activo.")
            }

            val statsSnap = transaction.get(statsRef)
            val currentTotal = (statsSnap.get("totalItems") as? Number)?.toInt() ?: 0

            // 2. ESCRIBIR cambios
            transaction.delete(itemRef)
            transaction.delete(codeIndexRef)
            
            // Solo decrementar si es mayor a 0 para evitar el "-1"
            if (currentTotal > 0) {
                transaction.update(statsRef, "totalItems", FieldValue.increment(-1))
            }
        }.await()
    }
}
