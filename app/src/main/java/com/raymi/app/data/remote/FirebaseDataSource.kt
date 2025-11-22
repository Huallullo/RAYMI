package com.raymi.app.data.remote

import com.google.firebase.auth.FirebaseAuth
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
}