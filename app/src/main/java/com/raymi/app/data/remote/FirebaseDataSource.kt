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
        // Verificar autenticación antes de observar
        if (auth.currentUser == null) {
            close(Exception("Usuario no autenticado"))
            return@callbackFlow
        }

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

    // ========== POBLAR DATA DE PRUEBA ==========

    /**
     * Pobla la base de datos con datos de prueba si está vacía
     * COMENTADO: No poblar datos de prueba en producción
     */
    /*suspend fun populateTestDataIfEmpty() {
        // Verificar si ya hay datos
        val existingClientes = getAllDocuments(COLLECTION_CLIENTES)
        val existingVestuarios = getAllDocuments(COLLECTION_VESTUARIOS)
        val existingAlquileres = getAllDocuments(COLLECTION_ALQUILERES)

        if (existingClientes.isNotEmpty() || existingVestuarios.isNotEmpty() || existingAlquileres.isNotEmpty()) {
            return // Ya hay datos, no poblar
        }

        // Poblar con datos de prueba
        populateTestData()
    }

    /**
     * Pobla la base de datos con datos de prueba
     * COMENTADO: No poblar datos de prueba en producción
     */
    suspend fun populateTestData() {
        // Clientes de prueba (20)
        val clientes = listOf(
            mapOf("dni" to "12345678", "nombre" to "Juan", "apellidos" to "Pérez García", "telefono" to "987654321", "email" to "juan.perez@email.com", "direccion" to "Av. Principal 123, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "87654321", "nombre" to "María", "apellidos" to "López Rodríguez", "telefono" to "987654322", "email" to "maria.lopez@email.com", "direccion" to "Jr. Secundario 456, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "11223344", "nombre" to "Carlos", "apellidos" to "Martínez Silva", "telefono" to "987654323", "email" to "carlos.martinez@email.com", "direccion" to "Calle Tercera 789, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "44332211", "nombre" to "Ana", "apellidos" to "Gómez Torres", "telefono" to "987654324", "email" to "ana.gomez@email.com", "direccion" to "Plaza Mayor 101, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "55667788", "nombre" to "Pedro", "apellidos" to "Ramírez Díaz", "telefono" to "987654325", "email" to "pedro.ramirez@email.com", "direccion" to "Av. Libertad 202, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "66778899", "nombre" to "Laura", "apellidos" to "Fernández Ruiz", "telefono" to "987654326", "email" to "laura.fernandez@email.com", "direccion" to "Calle Nueva 303, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "77889900", "nombre" to "Miguel", "apellidos" to "Sánchez Morales", "telefono" to "987654327", "email" to "miguel.sanchez@email.com", "direccion" to "Jr. Viejo 404, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "88990011", "nombre" to "Sofia", "apellidos" to "Jiménez Castro", "telefono" to "987654328", "email" to "sofia.jimenez@email.com", "direccion" to "Av. Moderna 505, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "99001122", "nombre" to "Diego", "apellidos" to "Ruiz Vargas", "telefono" to "987654329", "email" to "diego.ruiz@email.com", "direccion" to "Plaza Central 606, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "00112233", "nombre" to "Valentina", "apellidos" to "Morales Peña", "telefono" to "987654330", "email" to "valentina.morales@email.com", "direccion" to "Calle Antigua 707, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "11223344", "nombre" to "Andrés", "apellidos" to "Torres Mendoza", "telefono" to "987654331", "email" to "andres.torres@email.com", "direccion" to "Av. Futura 808, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "22334455", "nombre" to "Camila", "apellidos" to "Vargas Soto", "telefono" to "987654332", "email" to "camila.vargas@email.com", "direccion" to "Jr. Histórico 909, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "33445566", "nombre" to "Felipe", "apellidos" to "Mendoza Reyes", "telefono" to "987654333", "email" to "felipe.mendoza@email.com", "direccion" to "Calle Verde 1010, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "44556677", "nombre" to "Isabella", "apellidos" to "Reyes Castro", "telefono" to "987654334", "email" to "isabella.reyes@email.com", "direccion" to "Plaza Azul 1111, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "55667788", "nombre" to "Sebastián", "apellidos" to "Castro Peña", "telefono" to "987654335", "email" to "sebastian.castro@email.com", "direccion" to "Av. Roja 1212, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "66778899", "nombre" to "Gabriela", "apellidos" to "Peña Morales", "telefono" to "987654336", "email" to "gabriela.pena@email.com", "direccion" to "Jr. Amarillo 1313, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "77889900", "nombre" to "Emiliano", "apellidos" to "Morales Torres", "telefono" to "987654337", "email" to "emiliano.morales@email.com", "direccion" to "Calle Naranja 1414, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "88990011", "nombre" to "Antonella", "apellidos" to "Torres Vargas", "telefono" to "987654338", "email" to "antonella.torres@email.com", "direccion" to "Plaza Morada 1515, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "99001122", "nombre" to "Leonardo", "apellidos" to "Vargas Mendoza", "telefono" to "987654339", "email" to "leonardo.vargas@email.com", "direccion" to "Av. Celeste 1616, Lima", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("dni" to "00112233", "nombre" to "Martina", "apellidos" to "Mendoza Reyes", "telefono" to "987654340", "email" to "martina.mendoza@email.com", "direccion" to "Jr. Rosa 1717, Lima", "createdAt" to FieldValue.serverTimestamp())
        )

        // Vestuarios de prueba (20)
        val vestuarios = listOf(
            mapOf("codigo" to "VEST001", "danza" to "Folclórica", "departamento" to "Polleras", "descripcion" to "Pollera tradicional peruana", "talla" to "M", "precio" to 150.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST002", "danza" to "Marinera", "departamento" to "Trajes", "descripcion" to "Traje completo de marinera norteña", "talla" to "S", "precio" to 200.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST003", "danza" to "Huayno", "departamento" to "Ponchos", "descripcion" to "Poncho tradicional andino", "talla" to "L", "precio" to 80.0, "estado" to "ALQUILADO", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST004", "danza" to "Tondero", "departamento" to "Accesorios", "descripcion" to "Sombrero de tondero", "talla" to "Única", "precio" to 50.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST005", "danza" to "Folclórica", "departamento" to "Polleras", "descripcion" to "Pollera bordada con motivos andinos", "talla" to "XL", "precio" to 180.0, "estado" to "MANTENIMIENTO", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST006", "danza" to "Salsa", "departamento" to "Trajes", "descripcion" to "Traje de salsa moderna", "talla" to "M", "precio" to 120.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST007", "danza" to "Caporal", "departamento" to "Trajes", "descripcion" to "Traje completo de caporal", "talla" to "L", "precio" to 250.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST008", "danza" to "Huayno", "departamento" to "Ponchos", "descripcion" to "Poncho de lana de alpaca", "talla" to "M", "precio" to 100.0, "estado" to "ALQUILADO", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST009", "danza" to "Vals", "departamento" to "Trajes", "descripcion" to "Traje de vals criollo", "talla" to "S", "precio" to 160.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST010", "danza" to "Danza de tijeras", "departamento" to "Trajes", "descripcion" to "Traje completo de danza de tijeras", "talla" to "M", "precio" to 220.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST011", "danza" to "Folclórica", "departamento" to "Polleras", "descripcion" to "Pollera con bordados complejos", "talla" to "L", "precio" to 190.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST012", "danza" to "Marinera", "departamento" to "Accesorios", "descripcion" to "Mantón de Manila", "talla" to "Única", "precio" to 70.0, "estado" to "ALQUILADO", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST013", "danza" to "Huayno", "departamento" to "Ponchos", "descripcion" to "Poncho de vicuña", "talla" to "XL", "precio" to 300.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST014", "danza" to "Tondero", "departamento" to "Trajes", "descripcion" to "Traje completo de tondero", "talla" to "M", "precio" to 140.0, "estado" to "MANTENIMIENTO", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST015", "danza" to "Salsa", "departamento" to "Accesorios", "descripcion" to "Zapatos de salsa", "talla" to "38", "precio" to 40.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST016", "danza" to "Caporal", "departamento" to "Accesorios", "descripcion" to "Sombrero de caporal", "talla" to "Única", "precio" to 60.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST017", "danza" to "Vals", "departamento" to "Polleras", "descripcion" to "Pollera de vals", "talla" to "S", "precio" to 130.0, "estado" to "ALQUILADO", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST018", "danza" to "Danza de tijeras", "departamento" to "Accesorios", "descripcion" to "Tijeras decorativas", "talla" to "Única", "precio" to 30.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST019", "danza" to "Folclórica", "departamento" to "Trajes", "descripcion" to "Traje completo folclórico", "talla" to "L", "precio" to 280.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp()),
            mapOf("codigo" to "VEST020", "danza" to "Marinera", "departamento" to "Polleras", "descripcion" to "Pollera de marinera", "talla" to "M", "precio" to 170.0, "estado" to "DISPONIBLE", "imagenUrl" to "", "createdAt" to FieldValue.serverTimestamp())
        )

        // Agregar clientes
        for (cliente in clientes) {
            firestore.collection(COLLECTION_CLIENTES).add(cliente).await()
        }

        // Agregar vestuarios
        for (vestuario in vestuarios) {
            firestore.collection(COLLECTION_VESTUARIOS).add(vestuario).await()
        }

        // Alquileres de prueba
        val alquileres = listOf(
            mapOf("clienteId" to "cliente1", "clienteNombre" to "Juan Pérez García", "vestuarioId" to "vest1", "vestuarioNombre" to "Pollera tradicional peruana", "vestuarioCodigo" to "VEST001", "cantidad" to 1, "fechaInicio" to FieldValue.serverTimestamp(), "fechaFinPrevista" to FieldValue.serverTimestamp(), "fechaDevolucion" to null, "precioUnitario" to 150.0, "precioTotal" to 150.0, "adelanto" to 75.0, "saldo" to 75.0, "estado" to "ACTIVO", "observaciones" to "Alquiler para evento cultural", "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()),
            mapOf("clienteId" to "cliente2", "clienteNombre" to "María López Rodríguez", "vestuarioId" to "vest2", "vestuarioNombre" to "Traje completo de marinera norteña", "vestuarioCodigo" to "VEST002", "cantidad" to 1, "fechaInicio" to FieldValue.serverTimestamp(), "fechaFinPrevista" to FieldValue.serverTimestamp(), "fechaDevolucion" to null, "precioUnitario" to 200.0, "precioTotal" to 200.0, "adelanto" to 100.0, "saldo" to 100.0, "estado" to "ACTIVO", "observaciones" to "Para competencia de danza", "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp())
        )

        // Agregar alquileres
        for (alquiler in alquileres) {
            firestore.collection(COLLECTION_ALQUILERES).add(alquiler).await()
        }
    }*/
}
