package com.raymi.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS
import com.raymi.app.core.utils.Constants.COLLECTION_USUARIOS
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        const val DEFAULT_QUERY_LIMIT = 500L
    }

    suspend fun getDocument(collection: String, documentId: String): Map<String, Any>? {
        val snapshot = firestore.collection(collection).document(documentId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun getAllDocuments(collection: String): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    suspend fun updateDocument(collection: String, documentId: String, data: Map<String, Any>) {
        firestore.collection(collection).document(documentId).update(data).await()
    }

    suspend fun deleteDocument(collection: String, documentId: String) {
        firestore.collection(collection).document(documentId).delete().await()
    }

    suspend fun queryDocuments(
        collection: String,
        field: String,
        value: Any
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection).whereEqualTo(field, value).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    // ========== PERFILES Y NEGOCIOS (SaaS) ==========
    
    suspend fun createBusinessProfileForUser(user: FirebaseUser, businessName: String): String {
        val uid = user.uid
        val email = user.email.orEmpty().trim()
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document()
        val usuarioRef = firestore.collection(COLLECTION_USUARIOS).document(uid)
        val miembroRef = negocioRef.collection("miembros").document(uid)
        val statsRef = negocioRef.collection("metadata").document("stats")
        val now = com.google.firebase.firestore.FieldValue.serverTimestamp()
        val negocioNombre = businessName.trim().ifBlank { defaultBusinessName(email) }

        firestore.runBatch { batch ->
            batch.set(negocioRef, mapOf(
                "id" to negocioRef.id,
                "nombre" to negocioNombre, 
                "rubro" to "alquileres", 
                "pais" to "PE",
                "moneda" to "PEN", 
                "plan" to "FREE", 
                "ownerUid" to uid,
                "createdAt" to now, 
                "updatedAt" to now, 
                "ultimoAcceso" to now
            ))
            batch.set(statsRef, mapOf(
                "totalItems" to 0L,
                "alquileresActivos" to 0L,
                "totalIngresos" to 0.0,
                "totalClientes" to 0L,
                "updatedAt" to now
            ))
            batch.set(usuarioRef, mapOf(
                "uid" to uid, 
                "email" to email, 
                "emailLowercase" to email.lowercase(),
                "nombre" to (user.displayName ?: ""), 
                "negocioId" to negocioRef.id,
                "rol" to "owner", 
                "idioma" to "es", 
                "createdAt" to now, 
                "updatedAt" to now
            ), com.google.firebase.firestore.SetOptions.merge())
            batch.set(miembroRef, mapOf(
                "uid" to uid, 
                "email" to email, 
                "rol" to "owner", 
                "estado" to "ACTIVO", 
                "createdAt" to now, 
                "updatedAt" to now
            ))
        }.await()
        return negocioRef.id
    }

    suspend fun ensureBusinessProfileForUser(user: FirebaseUser): String {
        val uid = user.uid
        val usuarioRef = firestore.collection(COLLECTION_USUARIOS).document(uid)
        
        return try {
            val snapshot = usuarioRef.get().await()
            val negocioId = snapshot.getString("negocioId")
            
            if (snapshot.exists() && !negocioId.isNullOrBlank()) {
                val miembroSnap = firestore.collection(COLLECTION_NEGOCIOS)
                    .document(negocioId)
                    .collection("miembros")
                    .document(uid)
                    .get()
                    .await()
                
                if (miembroSnap.exists()) {
                    negocioId
                } else {
                    createBusinessProfileForUser(user, defaultBusinessName(user.email.orEmpty()))
                }
            } else {
                createBusinessProfileForUser(user, defaultBusinessName(user.email.orEmpty()))
            }
        } catch (e: Exception) {
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                createBusinessProfileForUser(user, defaultBusinessName(user.email.orEmpty()))
            } else {
                throw e
            }
        }
    }

    private fun defaultBusinessName(email: String): String {
        val prefix = email.substringBefore('@').replace('.', ' ').replace('_', ' ').trim()
        return if (prefix.isBlank()) "Mi negocio" else "Negocio de $prefix"
    }

    suspend fun getCurrentBusinessId(): String {
        val user = auth.currentUser ?: throw IllegalStateException("Usuario no autenticado")
        return ensureBusinessProfileForUser(user)
    }

    suspend fun getBusinessDocument(collection: String, documentId: String, negocioId: String? = null): Map<String, Any>? {
        val targetId = negocioId ?: getCurrentBusinessId()
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(targetId).collection(collection).document(documentId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun getAllBusinessDocumentsOrderedLimited(
        collection: String,
        orderByField: String,
        descending: Boolean = true,
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> {
        val direction = if (descending) com.google.firebase.firestore.Query.Direction.DESCENDING else com.google.firebase.firestore.Query.Direction.ASCENDING
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(getCurrentBusinessId()).collection(collection)
            .orderBy(orderByField, direction)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    suspend fun queryBusinessDocuments(
        collection: String,
        field: String,
        value: Any,
        limit: Long = DEFAULT_QUERY_LIMIT,
        negocioId: String? = null
    ): List<Pair<String, Map<String, Any>>> {
        val targetNegocioId = negocioId ?: getCurrentBusinessId()
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(targetNegocioId).collection(collection)
            .whereEqualTo(field, value)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    suspend fun queryBusinessArrayContainsLimited(
        collection: String,
        field: String,
        value: String,
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(getCurrentBusinessId()).collection(collection)
            .whereArrayContains(field, value)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    suspend fun updateBusinessDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any>,
        negocioId: String? = null
    ) {
        val targetNegocioId = negocioId ?: getCurrentBusinessId()
        firestore.collection(COLLECTION_NEGOCIOS).document(targetNegocioId)
            .collection(collection).document(documentId).update(data).await()
    }

    suspend fun deleteBusinessDocument(
        collection: String,
        documentId: String,
        negocioId: String? = null
    ) {
        val targetNegocioId = negocioId ?: getCurrentBusinessId()
        firestore.collection(COLLECTION_NEGOCIOS).document(targetNegocioId)
            .collection(collection).document(documentId).delete().await()
    }

    suspend fun addBusinessDocument(workspaceId: String, collection: String, data: Map<String, Any>): String {
        val docRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection(collection).add(data).await()
        return docRef.id
    }
}
