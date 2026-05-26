package com.raymi.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        const val COLLECTION_CLIENTES = "clientes"
        const val COLLECTION_USUARIOS = "usuarios"
        const val COLLECTION_NEGOCIOS = "negocios"
        const val DEFAULT_QUERY_LIMIT = 500L
    }

    // ========== OPERACIONES GENÉRICAS (colecciones raíz) ==========
    suspend fun getDocument(collection: String, documentId: String): Map<String, Any>? {
        val snapshot = firestore.collection(collection).document(documentId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun getAllDocuments(collection: String): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    suspend fun getDocumentsPageOrdered(
        collection: String,
        orderByField: String,
        descending: Boolean = true,
        pageSize: Long = DEFAULT_QUERY_LIMIT,
        startAfter: DocumentSnapshot? = null
    ): Pair<List<Pair<String, Map<String, Any>>>, DocumentSnapshot?> {
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        var query: Query = firestore.collection(collection).orderBy(orderByField, direction).limit(pageSize)
        if (startAfter != null) query = query.startAfter(startAfter)
        val snapshot = query.get().await()
        val documents = snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
        return documents to snapshot.documents.lastOrNull()
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

    // ========== AUTENTICACIÓN ==========
    fun getCurrentUser() = auth.currentUser

    // ========== PERFILES Y NEGOCIOS (SaaS) ==========
    
    suspend fun createBusinessProfileForUser(user: FirebaseUser, businessName: String): String {
        val uid = user.uid
        val email = user.email.orEmpty().trim()
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document()
        val usuarioRef = firestore.collection(COLLECTION_USUARIOS).document(uid)
        val miembroRef = negocioRef.collection("miembros").document(uid)
        val statsRef = negocioRef.collection("metadata").document("stats")
        val now = FieldValue.serverTimestamp()
        val negocioNombre = businessName.trim().ifBlank { defaultBusinessName(email) }

        try {
            firestore.runBatch { batch ->
                // 1. Crear el negocio (doc raíz)
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

                // 2. Crear las estadísticas iniciales (subcolección)
                batch.set(statsRef, mapOf(
                    "totalItems" to 0L,
                    "alquileresActivos" to 0L,
                    "totalIngresos" to 0.0,
                    "totalClientes" to 0L,
                    "updatedAt" to now
                ))

                // 3. Crear el perfil de usuario (colección raíz)
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

                // 4. Agregar al usuario como miembro (subcolección)
                batch.set(miembroRef, mapOf(
                    "uid" to uid, 
                    "email" to email, 
                    "nombre" to (user.displayName ?: ""),
                    "rol" to "owner", 
                    "estado" to "ACTIVO", 
                    "createdAt" to now, 
                    "updatedAt" to now
                ))
            }.await()
            
            return negocioRef.id
        } catch (e: Exception) {
            android.util.Log.e("FirebaseDataSource", "Error creando perfil en batch: ${e.message}")
            throw e
        }
    }

    suspend fun ensureBusinessProfileForUser(user: FirebaseUser): String {
        val uid = user.uid
        val usuarioRef = firestore.collection(COLLECTION_USUARIOS).document(uid)
        
        return try {
            val snapshot = usuarioRef.get().await()
            val negocioId = snapshot.getString("negocioId")
            
            if (snapshot.exists() && !negocioId.isNullOrBlank()) {
                negocioId
            } else {
                // Si no existe el perfil, intentamos crearlo
                createBusinessProfileForUser(user, defaultBusinessName(user.email.orEmpty()))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseDataSource", "Error en ensureBusinessProfileForUser: ${e.message}")
            // Si falla por permisos pero el usuario está autenticado, devolvemos un ID temporal 
            // o lanzamos una excepción controlada que el repositorio manejará.
            throw e
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

    private suspend fun businessCollection(collection: String) =
        firestore.collection(COLLECTION_NEGOCIOS).document(getCurrentBusinessId()).collection(collection)

    suspend fun getBusinessDocument(collection: String, documentId: String): Map<String, Any>? {
        val snapshot = businessCollection(collection).document(documentId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun getAllBusinessDocumentsOrderedLimited(
        collection: String,
        orderByField: String,
        descending: Boolean = true,
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> {
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        val snapshot = businessCollection(collection)
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
        val snapshot = businessCollection(collection)
            .whereArrayContains(field, value)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    fun observeBusinessCollectionOrderedLimited(
        collection: String,
        orderByField: String,
        descending: Boolean = true,
        limit: Long = 200,
        negocioId: String? = null
    ): Flow<List<Pair<String, Map<String, Any>>>> = callbackFlow {
        val user = auth.currentUser ?: run { close(Exception("Usuario no autenticado")); return@callbackFlow }
        val finalNegocioId = negocioId ?: try { ensureBusinessProfileForUser(user) } catch (e: Exception) { close(e); return@callbackFlow }
        
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        val subscription = firestore.collection(COLLECTION_NEGOCIOS)
            .document(finalNegocioId)
            .collection(collection)
            .orderBy(orderByField, direction)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val documents = snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
                    trySend(documents)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateBusinessDocument(collection: String, documentId: String, data: Map<String, Any>) {
        businessCollection(collection).document(documentId).update(data).await()
    }

    suspend fun deleteBusinessDocument(collection: String, documentId: String) {
        businessCollection(collection).document(documentId).delete().await()
    }

    suspend fun addBusinessDocument(workspaceId: String, collection: String, data: Map<String, Any>): String {
        val docRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection(collection).add(data).await()
        return docRef.id
    }

    // ========== FUNCIONES BUSINESS PARA CLIENTES (¡LA QUE FALTABA!) ==========
    suspend fun addBusinessClienteWithUniqueDni(
        clienteData: Map<String, Any>,
        dniRaw: String
    ): String {
        val negocioId = getCurrentBusinessId()
        val dni = dniRaw.trim().uppercase()

        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(negocioId)
        val clientesRef = negocioRef.collection("clientes")
        val dniIndexRef = negocioRef.collection("clientes_dni_index").document(dni)

        return firestore.runTransaction { transaction ->
            val dniIndexSnap = transaction.get(dniIndexRef)
            if (dniIndexSnap.exists()) {
                throw IllegalStateException("Ya existe un cliente con este DNI")
            }
            val clienteRef = clientesRef.document()
            transaction.set(
                clienteRef,
                clienteData + mapOf(
                    "negocioId" to negocioId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.set(
                dniIndexRef,
                mapOf(
                    "clienteId" to clienteRef.id,
                    "dni" to dni,
                    "negocioId" to negocioId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            clienteRef.id
        }.await()
    }

    // ========== FUNCIONES BUSINESS PARA ÍTEMS (VESTUARIOS) ==========
    suspend fun addBusinessItemWithUniqueCodigo(
        itemData: Map<String, Any>,
        codigoRaw: String
    ): String {
        val negocioId = getCurrentBusinessId()
        val codigo = codigoRaw.trim().uppercase()

        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(negocioId)
        val itemsRef = negocioRef.collection("items")
        val codigoIndexRef = negocioRef.collection("items_codigo_index").document(codigo)

        return firestore.runTransaction { transaction ->
            val codigoIndexSnap = transaction.get(codigoIndexRef)
            if (codigoIndexSnap.exists()) {
                throw IllegalStateException("Ya existe un vestuario con este código")
            }
            val itemRef = itemsRef.document()
            transaction.set(
                itemRef,
                itemData + mapOf(
                    "negocioId" to negocioId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.set(
                codigoIndexRef,
                mapOf(
                    "itemId" to itemRef.id,
                    "codigo" to codigo,
                    "negocioId" to negocioId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            itemRef.id
        }.await()
    }

    suspend fun queryBusinessItemByCodigo(
        codigo: String,
        limit: Long = 5
    ): List<Pair<String, Map<String, Any>>> {
        return queryBusinessDocuments(
            collection = "items",
            field = "codigo",
            value = codigo.trim().uppercase(),
            limit = limit
        )
    }

    fun observeBusinessItemsOrderedLimited(
        orderByField: String = "createdAt",
        descending: Boolean = true,
        limit: Long = 500
    ): Flow<List<Pair<String, Map<String, Any>>>> {
        return observeBusinessCollectionOrderedLimited("items", orderByField, descending, limit)
    }

    // ========== ESTADÍSTICAS ATÓMICAS (AHORRO DE DINERO) ==========

    /**
     * Incrementa o decrementa contadores de forma atómica en el documento de metadatos.
     * Esto evita leer toda la colección para obtener totales en el Dashboard.
     */
    suspend fun updateStats(workspaceId: String, field: String, increment: Long) {
        val statsRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("metadata").document("stats")
        
        // Usar set con merge para asegurar que el documento exista sin fallar
        statsRef.set(mapOf(field to FieldValue.increment(increment)), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /**
     * Obtiene el documento único de estadísticas para el Dashboard.
     * Costo: 1 lectura de Firestore.
     */
    suspend fun getStats(workspaceId: String): Map<String, Any>? {
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("metadata").document("stats").get().await()
        return if (snapshot.exists()) snapshot.data else null
    }
    suspend fun addBusinessAlquiler(
        alquilerData: Map<String, Any>,
        itemId: String
    ): String {
        val negocioId = getCurrentBusinessId()
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(negocioId)
        val alquileresRef = negocioRef.collection("alquileres")
        val itemsRef = negocioRef.collection("items")

        return firestore.runTransaction { transaction ->
            val itemRef = itemsRef.document(itemId)
            val itemSnap = transaction.get(itemRef)
            if (!itemSnap.exists()) throw IllegalStateException("Item no encontrado")
            val estado = itemSnap.getString("estado")
            if (estado != "DISPONIBLE") throw IllegalStateException("El vestuario no está disponible")

            val alquilerRef = alquileresRef.document()
            val dataCompleta = alquilerData + mapOf(
                "negocioId" to negocioId,
                "itemId" to itemId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(alquilerRef, dataCompleta)
            transaction.update(itemRef, "estado", "ALQUILADO")
            alquilerRef.id
        }.await()
    }

    suspend fun registrarDevolucionBusiness(alquilerId: String) {
        val negocioId = getCurrentBusinessId()
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(negocioId)
        val alquilerRef = negocioRef.collection("alquileres").document(alquilerId)

        firestore.runTransaction { transaction ->
            val alquilerSnap = transaction.get(alquilerRef)
            if (!alquilerSnap.exists()) throw IllegalStateException("Alquiler no encontrado")
            val itemId = alquilerSnap.getString("itemId")
                ?: throw IllegalStateException("Item no encontrado en el alquiler")
            val itemRef = negocioRef.collection("items").document(itemId)

            transaction.update(
                alquilerRef,
                mapOf(
                    "estado" to "DEVUELTO",
                    "fechaDevolucion" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.update(itemRef, "estado", "DISPONIBLE")
        }.await()
    }

    suspend fun getBusinessAlquiler(id: String): Map<String, Any>? = getBusinessDocument("alquileres", id)
    suspend fun updateBusinessAlquiler(id: String, data: Map<String, Any>) = updateBusinessDocument("alquileres", id, data)
    suspend fun deleteBusinessAlquiler(id: String) = deleteBusinessDocument("alquileres", id)
    suspend fun queryBusinessAlquileres(
        field: String,
        value: Any,
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> = queryBusinessDocuments("alquileres", field, value, limit)

    fun observeBusinessAlquileresOrderedLimited(
        orderByField: String = "createdAt",
        descending: Boolean = true,
        limit: Long = 500,
        negocioId: String? = null
    ): Flow<List<Pair<String, Map<String, Any>>>> =
        observeBusinessCollectionOrderedLimited("alquileres", orderByField, descending, limit, negocioId)

}
