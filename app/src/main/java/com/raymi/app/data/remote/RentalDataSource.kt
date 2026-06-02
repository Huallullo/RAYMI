package com.raymi.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RentalDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addPago(workspaceId: String, alquilerId: String, pagoData: Map<String, Any>) {
        val alquilerRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection("alquileres").document(alquilerId)
        val pagosRef = alquilerRef.collection("pagos")
        val statsRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection("metadata").document("stats")
        val monto = (pagoData["monto"] as? Number)?.toDouble() ?: 0.0

        firestore.runTransaction { transaction ->
            // 2. ESCRIBIR cambios
            val newPagoRef = pagosRef.document()
            transaction.set(newPagoRef, pagoData + mapOf(
                "id" to newPagoRef.id,
                "workspaceId" to workspaceId // OPTIMIZACIÓN: Añadir workspaceId para evitar N+1 queries en el futuro
            ))
            
            // OPTIMIZACIÓN: Usar increment atómico para evitar race conditions en el saldo
            transaction.update(alquilerRef, mapOf(
                "saldo" to com.google.firebase.firestore.FieldValue.increment(-monto), 
                "adelanto" to com.google.firebase.firestore.FieldValue.increment(monto), 
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))

            // ACTUALIZAR ESTADÍSTICAS MENSUALES Y TOTALES
            val cal = java.util.Calendar.getInstance()
            val mesKey = "ingresos_${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.MONTH)}"
            
            transaction.update(statsRef, mapOf(
                "totalIngresos" to FieldValue.increment(monto),
                "totalSaldoPendiente" to FieldValue.increment(-monto),
                mesKey to FieldValue.increment(monto)
            ))
        }.await()
    }

    suspend fun createAlquilerTransactional(workspaceId: String, alquilerData: Map<String, Any>): String {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val itemsRef = negocioRef.collection("items")
        val alquileresRef = negocioRef.collection("alquileres")
        val statsRef = negocioRef.collection("metadata").document("stats")
        val itemsList = (alquilerData["items"] as? List<Map<String, Any>>) ?: emptyList()

        return firestore.runTransaction { transaction ->
            // 1. LEER todos los ítems primero (Regla de Firestore)
            val itemSnapshots = itemsList.mapNotNull { itemData ->
                val id = itemData["itemId"] as? String ?: return@mapNotNull null
                id to transaction.get(itemsRef.document(id))
            }.toMap()

            // 2. Realizar validaciones y ESCRIBIR
            itemsList.forEach { itemData ->
                val itemId = itemData["itemId"] as? String ?: return@forEach
                val cantidad = (itemData["cantidad"] as? Number)?.toInt() ?: 1
                val itemSnap = itemSnapshots[itemId] ?: throw IllegalStateException("Ítem no encontrado: $itemId")
                
                if (!itemSnap.exists()) throw IllegalStateException("Ítem no existe en DB: $itemId")
                val stockTotal = (itemSnap.get("cantidad") as? Number)?.toInt() ?: 0
                val alquiladosActuales = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 0
                
                if (alquiladosActuales + cantidad > stockTotal) {
                    throw IllegalStateException("Stock insuficiente para: ${itemSnap.getString("nombre")}")
                }
                
                val nuevasUnidades = alquiladosActuales + cantidad
                transaction.update(itemsRef.document(itemId), mapOf(
                    "unidadesAlquiladas" to nuevasUnidades,
                    "estado" to if (nuevasUnidades >= stockTotal) "ALQUILADO" else "DISPONIBLE"
                ))
            }

            // Guardar Alquiler
            val newRef = alquileresRef.document()
            transaction.set(newRef, alquilerData + mapOf(
                "id" to newRef.id,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // Registrar Pago Inicial
            val adelanto = (alquilerData["adelanto"] as? Number)?.toDouble() ?: 0.0
            if (adelanto > 0) {
                val initialPagoRef = newRef.collection("pagos").document()
                transaction.set(initialPagoRef, mapOf(
                    "id" to initialPagoRef.id,
                    "alquilerId" to newRef.id,
                    "workspaceId" to workspaceId, // OPTIMIZACIÓN: Añadir workspaceId
                    "monto" to adelanto,
                    "metodoPago" to (alquilerData["metodoPago"] ?: "EFECTIVO"),
                    "referencia" to "Pago Inicial",
                    "fecha" to FieldValue.serverTimestamp()
                ))
                transaction.update(statsRef, "totalIngresos", FieldValue.increment(adelanto))
            }

            // Actualizar Estadísticas extendidas
            val cal = java.util.Calendar.getInstance()
            val mesKey = "ingresos_${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.MONTH)}"
            val hoyKey = "operaciones_${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.DAY_OF_YEAR)}"

            transaction.update(statsRef, mapOf(
                "alquileresActivos" to FieldValue.increment(1),
                "totalAlquileres" to FieldValue.increment(1), // Histórico
                "totalIngresos" to FieldValue.increment(adelanto),
                "totalSaldoPendiente" to FieldValue.increment(alquilerData["saldo"] as? Double ?: 0.0),
                mesKey to FieldValue.increment(adelanto),
                "$hoyKey.entregas" to FieldValue.increment(1)
            ))
            newRef.id
        }.await()
    }

    suspend fun getPagos(workspaceId: String, alquilerId: String): List<Map<String, Any>> {
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection("alquileres").document(alquilerId).collection("pagos").orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING).get().await()
        return snapshot.documents.mapNotNull { it.data }
    }

    suspend fun getPagosDeAlquileres(workspaceId: String, alquileres: List<String>): List<Map<String, Any>> {
        // OPTIMIZACIÓN: Usar collectionGroup con filtro de workspaceId para evitar N+1 queries.
        // Nota: Requiere que los pagos antiguos tengan el campo workspaceId o usar fallback.
        return try {
            val snapshot = firestore.collectionGroup("pagos")
                .whereEqualTo("workspaceId", workspaceId)
                .get()
                .await()
            
            if (snapshot.isEmpty && alquileres.isNotEmpty()) {
                // Fallback para datos antiguos (N+1 controlado o chunks)
                val allPagos = mutableListOf<Map<String, Any>>()
                alquileres.chunked(30).forEach { chunk ->
                    val snaps = firestore.collectionGroup("pagos")
                        .whereIn("alquilerId", chunk)
                        .get()
                        .await()
                    allPagos.addAll(snaps.documents.mapNotNull { it.data })
                }
                allPagos
            } else {
                snapshot.documents.mapNotNull { it.data }
            }
        } catch (e: Exception) {
            // Si falla el collectionGroup (ej: falta índice), usar fallback por alquiler
            val pagos = mutableListOf<Map<String, Any>>()
            for (id in alquileres) {
                val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
                    .collection("alquileres").document(id).collection("pagos").get().await()
                pagos.addAll(snapshot.documents.mapNotNull { it.data })
            }
            pagos
        }
    }

    suspend fun getAlquiler(workspaceId: String, alquilerId: String): Map<String, Any>? {
        val snapshot = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection("alquileres").document(alquilerId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun updateAlquiler(workspaceId: String, alquilerId: String, data: Map<String, Any>) {
        firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection("alquileres").document(alquilerId).update(data).await()
    }

    suspend fun updateAlquilerTransactional(workspaceId: String, alquilerId: String, newData: Map<String, Any>, itemId: String, diff: Int) {
        val itemRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection("items").document(itemId)
        val alquilerRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId).collection("alquileres").document(alquilerId)
        firestore.runTransaction { transaction ->
            // 1. LEER
            val itemSnap = if (diff != 0) transaction.get(itemRef) else null
            
            // 2. ESCRIBIR
            if (itemSnap != null && itemSnap.exists()) {
                val current = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 0
                val total = (itemSnap.get("cantidad") as? Number)?.toInt() ?: 1
                transaction.update(itemRef, mapOf("unidadesAlquiladas" to (current + diff), "estado" to if (current + diff >= total) "ALQUILADO" else "DISPONIBLE"))
            }
            transaction.update(alquilerRef, newData)
        }.await()
    }

    suspend fun deleteAlquilerTransactional(workspaceId: String, alquilerId: String) {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val alqRef = negocioRef.collection("alquileres").document(alquilerId)
        val statsRef = negocioRef.collection("metadata").document("stats")

        firestore.runTransaction { transaction ->
            // 1. LEER Alquiler
            val snapshot = transaction.get(alqRef)
            if (!snapshot.exists()) return@runTransaction
            val items = (snapshot.get("items") as? List<Map<String, Any>>) ?: emptyList()
            val estado = snapshot.getString("estado") ?: "ACTIVO"

            // 2. LEER todos los ítems asociados
            val itemSnapshots = if (estado == "ACTIVO" || estado == "VENCIDO" || estado == "RESERVADO") {
                items.mapNotNull { itemData ->
                    val id = itemData["itemId"] as? String ?: return@mapNotNull null
                    id to transaction.get(negocioRef.collection("items").document(id))
                }.toMap()
            } else emptyMap()

            // 3. ESCRIBIR cambios
            if (estado == "ACTIVO" || estado == "VENCIDO" || estado == "RESERVADO") {
                items.forEach { itemData ->
                    val id = itemData["itemId"] as? String ?: return@forEach
                    val cant = (itemData["cantidad"] as? Number)?.toInt() ?: 1
                    val itemSnap = itemSnapshots[id]
                    if (itemSnap != null && itemSnap.exists()) {
                        val alq = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 0
                        transaction.update(negocioRef.collection("items").document(id), mapOf("unidadesAlquiladas" to (alq - cant).coerceAtLeast(0), "estado" to "DISPONIBLE"))
                    }
                }
                transaction.update(statsRef, "alquileresActivos", FieldValue.increment(-1))
            }
            transaction.delete(alqRef)
        }.await()
    }

    suspend fun registrarDevolucionTransactional(
        workspaceId: String, 
        alquilerId: String, 
        penalidad: Double = 0.0, 
        observaciones: String = "", 
        montoGarantiaRetenida: Double = 0.0,
        unidadesARetornar: Int = 0 // 0 significa devolver TODO
    ) {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val alqRef = negocioRef.collection("alquileres").document(alquilerId)
        val statsRef = negocioRef.collection("metadata").document("stats")

        firestore.runTransaction { transaction ->
            // 1. LEER Alquiler
            val alqSnap = transaction.get(alqRef)
            if (!alqSnap.exists()) throw IllegalStateException("Alquiler no encontrado")
            
            val itemsOriginales = (alqSnap.get("items") as? List<Map<String, Any>>) ?: emptyList()
            val totalCantidadAlquilada = (alqSnap.get("cantidad") as? Number)?.toInt() ?: 1
            
            // 2. Determinar cuántas unidades se están devolviendo realmente
            val esDevolucionParcial = unidadesARetornar > 0 && unidadesARetornar < totalCantidadAlquilada
            val cantidadADevolverEfectiva = if (unidadesARetornar <= 0) totalCantidadAlquilada else unidadesARetornar

            // 3. LEER todos los ítems (Indispensable leer ANTES de actualizar nada)
            val itemSnapshots = itemsOriginales.mapNotNull { itemData ->
                val id = itemData["itemId"] as? String ?: return@mapNotNull null
                id to transaction.get(negocioRef.collection("items").document(id))
            }.toMap()

            // 4. ESCRIBIR cambios en Alquiler
            val gTotal = (alqSnap.get("garantia") as? Number)?.toDouble() ?: 0.0
            val infoG = if (montoGarantiaRetenida > 0) "\n[Garantía]: Retenida S/. $montoGarantiaRetenida de S/. $gTotal" else "\n[Garantía]: Devuelta íntegra"
            val prevObs = alqSnap.getString("observaciones") ?: ""
            val statusTag = if (esDevolucionParcial) "[Devolución Parcial ($cantidadADevolverEfectiva)]" else "[Devolución Total]"
            val newObs = if (observaciones.isNotBlank()) "$prevObs\n$statusTag: $observaciones$infoG" else "$prevObs\n$statusTag$infoG"

            // Si es parcial, actualizamos el campo 'items' con las nuevas unidades devueltas
            val nuevosItems = itemsOriginales.map { itemData ->
                val actualDevueltas = (itemData["unidadesDevueltas"] as? Number)?.toInt() ?: 0
                // En esta lógica simplificada (asumiendo 1 item principal por ahora o prorrateo):
                itemData + mapOf("unidadesDevueltas" to (actualDevueltas + cantidadADevolverEfectiva))
            }

            val todasDevueltas = nuevosItems.all { 
                ((it["unidadesDevueltas"] as? Number)?.toInt() ?: 0) >= ((it["cantidad"] as? Number)?.toInt() ?: 0) 
            }

            val extraIngreso = penalidad + montoGarantiaRetenida

            transaction.update(alqRef, mapOf(
                "estado" to if (todasDevueltas) "DEVUELTO" else "ACTIVO", 
                "items" to nuevosItems,
                "penalidad" to FieldValue.increment(extraIngreso), 
                "adelanto" to FieldValue.increment(extraIngreso), // ✅ Sumar al total pagado para historial exacto
                "observaciones" to newObs, 
                "fechaDevolucion" to if (todasDevueltas) FieldValue.serverTimestamp() else alqSnap.get("fechaDevolucion"), 
                "updatedAt" to FieldValue.serverTimestamp()
            ))

            // ACTUALIZAR TOTAL PENDIENTE EN STATS
            val saldoActual = (alqSnap.get("saldo") as? Number)?.toDouble() ?: 0.0
            if (todasDevueltas) {
                 transaction.update(statsRef, mapOf("totalSaldoPendiente" to FieldValue.increment(-saldoActual)))
            }
            
            // 5. ESCRIBIR cambios en Ítems (Liberar Stock - Lógica Senior)
            itemsOriginales.forEach { itemData ->
                val id = itemData["itemId"] as? String ?: return@forEach
                val itemSnap = itemSnapshots[id]
                if (itemSnap != null && itemSnap.exists()) {
                    val stockTotal = (itemSnap.get("cantidad") as? Number)?.toInt() ?: 1
                    val currentAlquiladas = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 0
                    val nuevasAlquiladas = (currentAlquiladas - cantidadADevolverEfectiva).coerceAtLeast(0)
                    
                    val currentEstado = itemSnap.getString("estado") ?: "DISPONIBLE"
                    val nuevoEstado = when {
                        nuevasAlquiladas >= stockTotal -> "ALQUILADO"
                        currentEstado == "ALQUILADO" -> "DISPONIBLE"
                        else -> currentEstado
                    }
                    
                    transaction.update(negocioRef.collection("items").document(id), mapOf(
                        "unidadesAlquiladas" to nuevasAlquiladas, 
                        "estado" to nuevoEstado
                    ))
                }
            }

            // 6. ESCRIBIR estadísticas y registrar Penalidad como Pago si existe
            val cal = java.util.Calendar.getInstance()
            val mesKey = "ingresos_${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.MONTH)}"
            val hoyKey = "operaciones_${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.DAY_OF_YEAR)}"

            if (extraIngreso > 0) {
                val penaltyPagoRef = alqRef.collection("pagos").document()
                transaction.set(penaltyPagoRef, mapOf(
                    "id" to penaltyPagoRef.id,
                    "alquilerId" to alquilerId,
                    "monto" to extraIngreso,
                    "metodoPago" to "EFECTIVO",
                    "referencia" to "Penalidad/Garantía Retenida",
                    "fecha" to FieldValue.serverTimestamp()
                ))
            }

            transaction.update(statsRef, mapOf(
                "alquileresActivos" to if (todasDevueltas) FieldValue.increment(-1) else FieldValue.increment(0),
                "totalIngresos" to FieldValue.increment(extraIngreso),
                mesKey to FieldValue.increment(extraIngreso),
                "$hoyKey.devoluciones" to FieldValue.increment(1)
            ))
        }.await()
    }

    suspend fun cancelarAlquilerTransactional(workspaceId: String, alquilerId: String, motivo: String = "") {
        val negocioRef = firestore.collection(COLLECTION_NEGOCIOS).document(workspaceId)
        val alqRef = negocioRef.collection("alquileres").document(alquilerId)
        val statsRef = negocioRef.collection("metadata").document("stats")

        firestore.runTransaction { transaction ->
            // 1. LEER Alquiler
            val snapshot = transaction.get(alqRef)
            if (!snapshot.exists()) throw IllegalStateException("Alquiler no encontrado")
            val items = (snapshot.get("items") as? List<Map<String, Any>>) ?: emptyList()

            // 2. LEER todos los ítems asociados
            val itemSnapshots = items.mapNotNull { itemData ->
                val id = itemData["itemId"] as? String ?: return@mapNotNull null
                id to transaction.get(negocioRef.collection("items").document(id))
            }.toMap()

            // 3. ESCRIBIR cambios
            val prevObs = snapshot.getString("observaciones") ?: ""
            val newObs = if (motivo.isNotBlank()) "$prevObs\n[Cancelado]: $motivo" else "$prevObs\n[Cancelado]"

            transaction.update(alqRef, mapOf("estado" to "CANCELADO", "observaciones" to newObs, "updatedAt" to FieldValue.serverTimestamp()))
            
            items.forEach { itemData ->
                val id = itemData["itemId"] as? String ?: return@forEach
                val cant = (itemData["cantidad"] as? Number)?.toInt() ?: 1
                val itemSnap = itemSnapshots[id]
                if (itemSnap != null && itemSnap.exists()) {
                    val current = (itemSnap.get("unidadesAlquiladas") as? Number)?.toInt() ?: 0
                    transaction.update(negocioRef.collection("items").document(id), mapOf("unidadesAlquiladas" to (current - cant).coerceAtLeast(0), "estado" to "DISPONIBLE"))
                }
            }
            transaction.update(statsRef, "alquileresActivos", FieldValue.increment(-1))
        }.await()
    }
}
