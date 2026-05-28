package com.raymi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.data.remote.FirebaseDataSource.Companion.COLLECTION_NEGOCIOS
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de estadísticas atómicas del negocio.
 */
@Singleton
class StatsDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Incrementa o decrementa contadores de forma atómica en el documento de metadatos.
     */
    suspend fun updateStats(workspaceId: String, field: String, increment: Long) {
        val statsRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("metadata").document("stats")
        
        statsRef.set(mapOf(field to FieldValue.increment(increment)), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /**
     * Obtiene el documento único de estadísticas para el Dashboard.
     */
    suspend fun getStats(workspaceId: String): Map<String, Any>? {
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("metadata").document("stats").get().await()
        return if (snapshot.exists()) snapshot.data else null
    }
}
