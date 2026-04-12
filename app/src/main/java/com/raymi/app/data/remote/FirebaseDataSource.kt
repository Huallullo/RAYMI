package com.raymi.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para Firebase
 * Maneja todas las operaciones directas con Firestore y Auth
 */
@Singleton
class FirebaseDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // ========== COLECCIONES DE FIRESTORE ==========
    companion object {
        const val COLLECTION_CLIENTES = "clientes"
        const val COLLECTION_VESTUARIOS = "vestuarios"
        const val COLLECTION_ALQUILERES = "alquileres"
        const val COLLECTION_CLIENTES_DNI_INDEX = "clientes_dni_index"
        const val COLLECTION_VESTUARIOS_CODIGO_INDEX = "vestuarios_codigo_index"
    }

    // ========== OPERACIONES GENÉRICAS ==========

    /**
     * Agrega un documento a una colección
     * @param collection Nombre de la colección
     * @param data Datos a agregar
     * @return ID del documento creado
     */
    suspend fun addDocument(
        collection: String,
        data: Map<String, Any>
    ): String {
        val docRef = firestore.collection(collection).add(data).await()
        return docRef.id
    }

    /**
     * Obtiene un documento por ID
     * @param collection Nombre de la colección
     * @param documentId ID del documento
     * @return Map con los datos del documento o null si no existe
     */
    suspend fun getDocument(
        collection: String,
        documentId: String
    ): Map<String, Any>? {
        val snapshot = firestore.collection(collection)
            .document(documentId)
            .get()
            .await()

        return if (snapshot.exists()) {
            snapshot.data
        } else {
            null
        }
    }

    /**
     * Obtiene todos los documentos de una colección
     * @param collection Nombre de la colección
     * @return Lista de Maps con los datos
     */
    suspend fun getAllDocuments(
        collection: String
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { data ->
                doc.id to data
            }
        }
    }

    /**
     * Actualiza un documento
     * @param collection Nombre de la colección
     * @param documentId ID del documento
     * @param data Datos a actualizar
     */
    suspend fun updateDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any>
    ) {
        firestore.collection(collection)
            .document(documentId)
            .update(data)
            .await()
    }

    /**
     * Elimina un documento
     * @param collection Nombre de la colección
     * @param documentId ID del documento
     */
    suspend fun deleteDocument(
        collection: String,
        documentId: String
    ) {
        firestore.collection(collection)
            .document(documentId)
            .delete()
            .await()
    }

    /**
     * Busca documentos por un campo específico
     * @param collection Nombre de la colección
     * @param field Campo por el cual buscar
     * @param value Valor a buscar
     * @return Lista de documentos que coinciden
     */
    suspend fun queryDocuments(
        collection: String,
        field: String,
        value: Any
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection)
            .whereEqualTo(field, value)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { data ->
                doc.id to data
            }
        }
    }

    /**
     * Observa cambios en tiempo real de una colección
     * @param collection Nombre de la colección
     * @return Flow que emite la lista actualizada cuando hay cambios
     */
    fun observeCollection(
        collection: String
    ): Flow<List<Pair<String, Map<String, Any>>>> = callbackFlow {
        val subscription = firestore.collection(collection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val documents = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            doc.id to data
                        }
                    }
                    trySend(documents)
                }
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Busca documentos con query personalizada
     * @param collection Nombre de la colección
     * @param queryBuilder Lambda para construir la query
     * @return Lista de documentos que coinciden
     */
    suspend fun customQuery(
        collection: String,
        queryBuilder: (Query) -> Query
    ): List<Pair<String, Map<String, Any>>> {
        val baseQuery = firestore.collection(collection)
        val query = queryBuilder(baseQuery)
        val snapshot = query.get().await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { data ->
                doc.id to data
            }
        }
    }

    // ========== OPERACIONES DE AUTENTICACIÓN ==========

    /**
     * Obtiene el usuario actual de Firebase Auth
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * Verifica si hay un usuario autenticado
     */
    fun isUserAuthenticated() = auth.currentUser != null

    /**
     * Inicia sesión con email y contraseña
     */
    suspend fun signIn(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    /**
     * Registra un nuevo usuario
     */
    suspend fun signUp(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    /**
     * Cierra la sesión actual
     */
    fun signOut() = auth.signOut()

    suspend fun createAlquilerAndMarkVestuarioAlquilado(
        alquilerData: Map<String, Any>,
        vestuarioId: String
    ): String {
        return firestore.runTransaction { transaction ->
            val vestuarioRef = firestore.collection(COLLECTION_VESTUARIOS).document(vestuarioId)
            val vestuarioSnap = transaction.get(vestuarioRef)

            if (!vestuarioSnap.exists()) {
                throw IllegalStateException("Vestuario no encontrado")
            }

            val estado = vestuarioSnap.getString("estado")
            if (estado != "DISPONIBLE") {
                throw IllegalStateException("El vestuario no está disponible")
            }

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

            if (!alquilerSnap.exists()) {
                throw IllegalStateException("Alquiler no encontrado")
            }

            val vestuarioId = alquilerSnap.getString("vestuarioId")
                ?: throw IllegalStateException("Vestuario no encontrado en el alquiler")

            val vestuarioRef = firestore.collection(COLLECTION_VESTUARIOS).document(vestuarioId)

            transaction.update(
                alquilerRef,
                mapOf(
                    "estado" to "DEVUELTO",
                    "fechaDevolucion" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

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
            val dniIndexSnap = transaction.get(dniIndexRef)

            if (dniIndexSnap.exists()) {
                throw IllegalStateException("Ya existe un cliente con este DNI")
            }

            val clienteRef = firestore.collection(COLLECTION_CLIENTES).document()
            transaction.set(clienteRef, clienteData)

            transaction.set(
                dniIndexRef,
                mapOf(
                    "clienteId" to clienteRef.id,
                    "dni" to dni,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

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
            val codigoIndexSnap = transaction.get(codigoIndexRef)

            if (codigoIndexSnap.exists()) {
                throw IllegalStateException("Ya existe un vestuario con este código")
            }

            val vestuarioRef = firestore.collection(COLLECTION_VESTUARIOS).document()
            transaction.set(vestuarioRef, vestuarioData)

            transaction.set(
                codigoIndexRef,
                mapOf(
                    "vestuarioId" to vestuarioRef.id,
                    "codigo" to codigo,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            vestuarioRef.id
        }.await()
    }
    suspend fun queryArrayContains(
        collection: String,
        field: String,
        value: String
    ): List<Pair<String, Map<String, Any>>> {
        val snapshot = firestore.collection(collection)
            .whereArrayContains(field, value)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { data -> doc.id to data }
        }
    }
}