package com.raymi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.data.remote.FirebaseDataSource.Companion.COLLECTION_NEGOCIOS
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de Alquileres y Pagos.
 */
@Singleton
class RentalDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addPago(
        workspaceId: String,
        alquilerId: String,
        pagoData: Map<String, Any>
    ) {
        val alquilerRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("alquileres").document(alquilerId)
        val pagosRef = alquilerRef.collection("pagos")
        val statsRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("metadata").document("stats")

        val monto = (pagoData["monto"] as? Number)?.toDouble() ?: 0.0

        firestore.runTransaction { transaction ->
            // 1. Agregar el pago
            val newPagoRef = pagosRef.document()
            transaction.set(newPagoRef, pagoData + mapOf("id" to newPagoRef.id))

            // 2. Actualizar saldo y adelanto en el alquiler
            val snapshot = transaction.get(alquilerRef)
            val saldoActual = (snapshot.get("saldo") as? Number)?.toDouble() ?: 0.0
            val adelantoActual = (snapshot.get("adelanto") as? Number)?.toDouble() ?: 0.0
            
            transaction.update(alquilerRef, mapOf(
                "saldo" to (saldoActual - monto).coerceAtLeast(0.0),
                "adelanto" to (adelantoActual + monto),
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // 3. Actualizar ingresos totales en estadísticas
            transaction.update(statsRef, "totalIngresos", FieldValue.increment(monto))
        }.await()
    }

    suspend fun createAlquilerTransactional(
        workspaceId: String,
        alquilerData: Map<String, Any>,
        itemId: String
    ): String {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val itemRef = negocioRef.collection("items").document(itemId)
        val alquileresRef = negocioRef.collection("alquileres")
        val statsRef = negocioRef.collection("metadata").document("stats")

        return firestore.runTransaction { transaction ->
            // 1. Verificar Stock
            val itemSnap = transaction.get(itemRef)
            if (!itemSnap.exists()) throw IllegalStateException("Producto no encontrado")
            
            val cantidadTotal = (itemSnap.get("cantidad") as? Number)?.toInt() ?: 1
            val itemNombre = itemSnap.getString("nombre") ?: ""
            
            // Contar alquileres activos para este item (Esto es costoso en transacciones si hay muchos, 
            // pero necesario para integridad. Alternativa: contador 'alquilados' en el Item)
            // Por ahora, mejor usar un contador en el Item para eficiencia SaaS.
            
            val unidadesAlquiladas = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 0
            if (unidadesAlquiladas >= cantidadTotal) {
                throw IllegalStateException("No hay stock disponible para $itemNombre")
            }

            // 2. Crear el Alquiler
            val newAlquilerRef = alquileresRef.document()
            val finalData = alquilerData + mapOf(
                "id" to newAlquilerRef.id,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            transaction.set(newAlquilerRef, finalData)

            // 3. Actualizar Item (unidades alquiladas y estado)
            val nuevasUnidades = unidadesAlquiladas + 1
            val nuevoEstado = if (nuevasUnidades >= cantidadTotal) "ALQUILADO" else "DISPONIBLE"
            
            transaction.update(itemRef, mapOf(
                "unidadesAlquiladas" to nuevasUnidades,
                "estado" to nuevoEstado
            ))

            // 4. Actualizar Estadísticas
            transaction.update(statsRef, "alquileresActivos", FieldValue.increment(1))
            
            newAlquilerRef.id
        }.await()
    }

    suspend fun getPagos(workspaceId: String, alquilerId: String): List<Map<String, Any>> {
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("alquileres").document(alquilerId)
            .collection("pagos")
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    suspend fun getAlquiler(workspaceId: String, alquilerId: String): Map<String, Any>? {
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("alquileres").document(alquilerId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun updateAlquiler(workspaceId: String, alquilerId: String, data: Map<String, Any>) {
        firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("alquileres").document(alquilerId).update(data).await()
    }

    suspend fun deleteAlquiler(workspaceId: String, alquilerId: String) {
        firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
            .collection("alquileres").document(alquilerId).delete().await()
    }

    suspend fun registrarDevolucionTransactional(workspaceId: String, alquilerId: String) {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val alquilerRef = negocioRef.collection("alquileres").document(alquilerId)
        val statsRef = negocioRef.collection("metadata").document("stats")

        firestore.runTransaction { transaction ->
            val alqSnap = transaction.get(alquilerRef)
            if (!alqSnap.exists()) throw IllegalStateException("Alquiler no encontrado")
            
            val itemId = alqSnap.getString("itemId") ?: return@runTransaction
            val itemRef = negocioRef.collection("items").document(itemId)
            
            // 1. Marcar Alquiler como DEVUELTO
            transaction.update(alquilerRef, mapOf(
                "estado" to "DEVUELTO",
                "fechaDevolucion" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // 2. Decrementar unidades alquiladas y actualizar estado Item
            val itemSnap = transaction.get(itemRef)
            val alquiladas = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 1
            transaction.update(itemRef, mapOf(
                "unidadesAlquiladas" to (alquiladas - 1).coerceAtLeast(0),
                "estado" to "DISPONIBLE"
            ))

            // 3. Actualizar Stats
            transaction.update(statsRef, "alquileresActivos", FieldValue.increment(-1))
        }.await()
    }
}
