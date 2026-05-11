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
        const val COLLECTION_VESTUARIOS = "vestuarios"
        const val COLLECTION_ALQUILERES = "alquileres"
        const val COLLECTION_USUARIOS = "usuarios"
        const val COLLECTION_NEGOCIOS = "negocios"
        const val COLLECTION_CLIENTES_DNI_INDEX = "clientes_dni_index"
        const val COLLECTION_VESTUARIOS_CODIGO_INDEX = "vestuarios_codigo_index"
        const val DEFAULT_QUERY_LIMIT = 500L
    }

    // ========== OPERACIONES GENÉRICAS (colecciones raíz) ==========
    suspend fun addDocument(collection: String, data: Map<String, Any>): String {
        val docRef = firestore.collection(collection).add(data).await()
        return docRef.id
    }

    suspend fun getDocument(collection: String, documentId: String): Map<String, Any>? {
        val snapshot = firestore.collection(collection).document(documentId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun getAllDocuments(collection: String): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    suspend fun getAllDocumentsOrderedLimited(
        collection: String,
        orderByField: String,
        descending: Boolean = true,
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> {
        return getDocumentsPageOrdered(collection, orderByField, descending, limit, null).first
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

    suspend fun queryDocumentsLimited(
        collection: String,
        field: String,
        value: Any,
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection).whereEqualTo(field, value).limit(limit).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    fun observeCollection(collection: String): Flow<List<Pair<String, Map<String, Any>>>> = callbackFlow {
        if (auth.currentUser == null) {
            close(Exception("Usuario no autenticado"))
            return@callbackFlow
        }
        val subscription = firestore.collection(collection).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null) {
                val documents = snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
                trySend(documents)
            }
        }
        awaitClose { subscription.remove() }
    }

    fun observeCollectionOrderedLimited(
        collection: String,
        orderByField: String,
        descending: Boolean = true,
        limit: Long = 200
    ): Flow<List<Pair<String, Map<String, Any>>>> = callbackFlow {
        if (auth.currentUser == null) {
            close(Exception("Usuario no autenticado"))
            return@callbackFlow
        }
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        val subscription = firestore.collection(collection)
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

    suspend fun customQuery(
        collection: String,
        queryBuilder: (Query) -> Query
    ): List<Pair<String, Map<String, Any>>> {
        val query = queryBuilder(firestore.collection(collection))
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    // ========== AUTENTICACIÓN ==========
    fun getCurrentUser() = auth.currentUser
    fun isUserAuthenticated() = auth.currentUser != null
    suspend fun signIn(email: String, password: String) = auth.signInWithEmailAndPassword(email, password).await()
    suspend fun signUp(email: String, password: String) = auth.createUserWithEmailAndPassword(email, password).await()
    suspend fun sendPasswordResetEmail(email: String) = auth.sendPasswordResetEmail(email).await()
    fun signOut() = auth.signOut()

    // ========== PERFILES Y NEGOCIOS (SaaS) ==========
    suspend fun createBusinessProfileForUser(user: FirebaseUser, businessName: String): String {
        val uid = user.uid
        val email = user.email.orEmpty().trim()
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document()
        val usuarioRef = firestore.collection(COLLECTION_USUARIOS).document(uid)
        val miembroRef = negocioRef.collection("miembros").document(uid)
        val now = FieldValue.serverTimestamp()
        val negocioNombre = businessName.trim().ifBlank { defaultBusinessName(email) }

        val batch = firestore.batch()
        batch.set(negocioRef, mapOf(
            "nombre" to negocioNombre, "rubro" to "alquileres", "pais" to "PE",
            "moneda" to "PEN", "plan" to "FREE", "ownerUid" to uid,
            "createdAt" to now, "updatedAt" to now
        ))
        batch.set(usuarioRef, mapOf(
            "uid" to uid, "email" to email, "emailLowercase" to email.lowercase(),
            "nombre" to (user.displayName ?: ""), "negocioId" to negocioRef.id,
            "rol" to "owner", "idioma" to "es", "createdAt" to now, "updatedAt" to now
        ))
        batch.set(miembroRef, mapOf(
            "uid" to uid, "email" to email, "nombre" to (user.displayName ?: ""),
            "rol" to "owner", "estado" to "ACTIVO", "createdAt" to now, "updatedAt" to now
        ))
        batch.commit().await()
        return negocioRef.id
    }

    suspend fun ensureBusinessProfileForUser(user: FirebaseUser): String {
        val usuarioRef = firestore.collection(COLLECTION_USUARIOS).document(user.uid)
        val snapshot = usuarioRef.get().await()
        val negocioId = snapshot.getString("negocioId")
        return if (snapshot.exists() && !negocioId.isNullOrBlank()) {
            negocioId
        } else {
            createBusinessProfileForUser(user, defaultBusinessName(user.email.orEmpty()))
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
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = businessCollection(collection)
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
        limit: Long = 200
    ): Flow<List<Pair<String, Map<String, Any>>>> = callbackFlow {
        val user = auth.currentUser ?: run { close(Exception("Usuario no autenticado")); return@callbackFlow }
        val negocioId = try { ensureBusinessProfileForUser(user) } catch (e: Exception) { close(e); return@callbackFlow }
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        val subscription = firestore.collection(COLLECTION_NEGOCIOS)
            .document(negocioId)
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

    // ========== FUNCIONES BUSINESS PARA ALQUILERES ==========
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
        limit: Long = 500
    ): Flow<List<Pair<String, Map<String, Any>>>> =
        observeBusinessCollectionOrderedLimited("alquileres", orderByField, descending, limit)

    // ========== FUNCIONES ANTIGUAS (se mantienen por compatibilidad temporal) ==========
    suspend fun createAlquilerAndMarkVestuarioAlquilado(
        alquilerData: Map<String, Any>,
        vestuarioId: String
    ): String {
        return firestore.runTransaction { transaction ->
            val vestuarioRef = firestore.collection(COLLECTION_VESTUARIOS).document(vestuarioId)
            val vestuarioSnap = transaction.get(vestuarioRef)
            if (!vestuarioSnap.exists()) throw IllegalStateException("Vestuario no encontrado")
            if (vestuarioSnap.getString("estado") != "DISPONIBLE") throw IllegalStateException("El vestuario no está disponible")
            val alquilerRef = firestore.collection(COLLECTION_ALQUILERES).document()
            transaction.set(alquilerRef, alquilerData)
            transaction.update(vestuarioRef, "estado", "ALQUILADO")
            alquilerRef.id
        }.await()
    }

    suspend fun registrarDevolucionAtomica(alquilerId: String) {
        firestore.runTransaction { transaction ->
            val alquilerRef = firestore.collection(COLLECTION_ALQUILERES).document(alquilerId)
            val alquilerSnap = transaction.get(alquilerRef)
            if (!alquilerSnap.exists()) throw IllegalStateException("Alquiler no encontrado")
            val vestuarioId = alquilerSnap.getString("vestuarioId") ?: throw IllegalStateException("Vestuario no encontrado en el alquiler")
            val vestuarioRef = firestore.collection(COLLECTION_VESTUARIOS).document(vestuarioId)
            transaction.update(alquilerRef, mapOf("estado" to "DEVUELTO", "fechaDevolucion" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            transaction.update(vestuarioRef, "estado", "DISPONIBLE")
        }.await()
    }

    suspend fun addClienteWithUniqueDni(
        clienteData: Map<String, Any>,
        dniRaw: String
    ): String {
        val dni = dniRaw.trim().uppercase()
        return firestore.runTransaction { transaction ->
            val dniIndexRef = firestore.collection(COLLECTION_CLIENTES_DNI_INDEX).document(dni)
            if (transaction.get(dniIndexRef).exists()) throw IllegalStateException("Ya existe un cliente con este DNI")
            val clienteRef = firestore.collection(COLLECTION_CLIENTES).document()
            transaction.set(clienteRef, clienteData)
            transaction.set(dniIndexRef, mapOf("clienteId" to clienteRef.id, "dni" to dni, "createdAt" to FieldValue.serverTimestamp()))
            clienteRef.id
        }.await()
    }

    suspend fun addVestuarioWithUniqueCodigo(
        vestuarioData: Map<String, Any>,
        codigoRaw: String
    ): String {
        val codigo = codigoRaw.trim().uppercase()
        return firestore.runTransaction { transaction ->
            val codigoIndexRef = firestore.collection(COLLECTION_VESTUARIOS_CODIGO_INDEX).document(codigo)
            if (transaction.get(codigoIndexRef).exists()) throw IllegalStateException("Ya existe un vestuario con este código")
            val vestuarioRef = firestore.collection(COLLECTION_VESTUARIOS).document()
            transaction.set(vestuarioRef, vestuarioData)
            transaction.set(codigoIndexRef, mapOf("vestuarioId" to vestuarioRef.id, "codigo" to codigo, "createdAt" to FieldValue.serverTimestamp()))
            vestuarioRef.id
        }.await()
    }

    suspend fun queryArrayContains(
        collection: String,
        field: String,
        value: String
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection).whereArrayContains(field, value).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }

    suspend fun queryArrayContainsLimited(
        collection: String,
        field: String,
        value: String,
        limit: Long = DEFAULT_QUERY_LIMIT
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection).whereArrayContains(field, value).limit(limit).get().await()
        return snapshot.documents.mapNotNull { doc -> doc.data?.let { doc.id to it } }
    }
}