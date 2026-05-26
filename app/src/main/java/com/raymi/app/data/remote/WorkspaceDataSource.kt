package com.raymi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.data.remote.FirebaseDataSource.Companion.COLLECTION_NEGOCIOS
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de Espacios de Trabajo (Multi-tenancy).
 */
@Singleton
class WorkspaceDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createWorkspaceAtomic(
        workspaceData: Map<String, Any>,
        statsData: Map<String, Any>,
        uid: String,
        email: String
    ): String {
        val workspaceRef = firestore.collection(COLLECTION_NEGOCIOS).document()
        val statsRef = workspaceRef.collection("metadata").document("stats")
        val miembroRef = workspaceRef.collection("miembros").document(uid)
        val now = FieldValue.serverTimestamp()

        firestore.runBatch { batch ->
            batch.set(workspaceRef, workspaceData + mapOf("id" to workspaceRef.id, "createdAt" to now, "updatedAt" to now))
            batch.set(statsRef, statsData + mapOf("updatedAt" to now))
            batch.set(miembroRef, mapOf(
                "uid" to uid,
                "email" to email,
                "rol" to "owner",
                "estado" to "ACTIVO",
                "createdAt" to now
            ))
        }.await()
        return workspaceRef.id
    }
}
