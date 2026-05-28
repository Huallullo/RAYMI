package com.raymi.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raymi.app.data.remote.FirebaseDataSource.Companion.COLLECTION_NEGOCIOS
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos dedicada a la observación en tiempo real de colecciones.
 */
@Singleton
class ObserverDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Observa una colección dentro de un negocio con ordenamiento y límite.
     */
    fun observeBusinessCollection(
        workspaceId: String,
        collection: String,
        orderByField: String,
        descending: Boolean = true,
        limit: Long = 200
    ): Flow<List<Pair<String, Map<String, Any>>>> = callbackFlow {
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        val subscription = firestore.collection(COLLECTION_NEGOCIOS)
            .document(workspaceId)
            .collection(collection)
            .orderBy(orderByField, direction)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val documents = snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
                    trySend(documents)
                }
            }
        awaitClose { subscription.remove() }
    }
}
