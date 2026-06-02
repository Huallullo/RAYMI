package com.raymi.app.domain.usecase.workspace

import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import com.raymi.app.domain.repository.ClienteRepository
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject

/**
 * Caso de Uso Maestro: Auditoría y Reparación Total de Datos.
 * Recalcula todas las estadísticas del negocio leyendo la "fuente de la verdad" de Firestore.
 * Optimizado para el plan gratuito: solo debe llamarse manualmente ante descuadres.
 */
class PerformFullAuditUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository,
    private val itemRepository: ItemRepository,
    private val clienteRepository: ClienteRepository,
    private val updateWorkspaceUseCase: UpdateWorkspaceUseCase,
    private val getWorkspaceStatsUseCase: GetWorkspaceStatsUseCase
) {
    operator fun invoke(workspaceId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Invalidar caches locales para forzar lectura de red en el próximo acceso
            itemRepository.invalidateCache(workspaceId)
            getWorkspaceStatsUseCase.invalidateCache(workspaceId)
            
            // 2. Obtener Alquileres (limitado a 150 para seguridad de costos)
            val resAlq = alquilerRepository.getAlquileresOnce(workspaceId, limit = 150)
            val alquileres = (resAlq as? Resource.Success)?.data ?: emptyList()
            val ids = alquileres.map { it.id }
            
            // 3. Obtener Pagos reales (usa collectionGroup optimizado)
            val pagosResult = alquilerRepository.getPagosDeAlquileres(workspaceId, ids)
            val allPagos = (pagosResult as? Resource.Success)?.data ?: emptyList()

            // 4. Obtener Items reales (máximo 200)
            val resItems = itemRepository.getItemsByWorkspaceOnce(workspaceId, 200)
            val items = (resItems as? Resource.Success)?.data ?: emptyList()
            
            // 5. Obtener Clientes reales
            val resClientes = clienteRepository.getClientesOnce(workspaceId)
            val clientes = (resClientes as? Resource.Success)?.data ?: emptyList()

            val cal = Calendar.getInstance()
            val anio = cal.get(Calendar.YEAR)
            val mes = cal.get(Calendar.MONTH)
            val diaAnio = cal.get(Calendar.DAY_OF_YEAR)
            
            // AUDITORÍA FINANCIERA (Suma de caja real)
            val ingresosEsteMes = allPagos.filter {
                val c = Calendar.getInstance().apply { time = it.fecha.toDate() }
                c.get(Calendar.YEAR) == anio && c.get(Calendar.MONTH) == mes
            }.sumOf { it.monto }

            val totalRecaudado = allPagos.sumOf { it.monto }
            val saldoPendienteTotal = alquileres.filter { it.estado != EstadoAlquiler.DEVUELTO && it.estado != EstadoAlquiler.CANCELADO }
                .sumOf { it.saldoPendienteReal }

            // AUDITORÍA OPERATIVA
            val entregasHoyCount = alquileres.count { 
                val c = Calendar.getInstance().apply { time = it.fechaInicio.toDate() }
                c.get(Calendar.YEAR) == anio && c.get(Calendar.DAY_OF_YEAR) == diaAnio 
            }
            val retornosHoyCount = alquileres.count {
                val c = Calendar.getInstance().apply { time = it.fechaFinPrevista.toDate() }
                c.get(Calendar.YEAR) == anio && c.get(Calendar.DAY_OF_YEAR) == diaAnio
            }

            // 6. REPARACIÓN: Sobreescribir el documento de stats con la verdad absoluta
            val repairData = mapOf(
                "totalIngresos" to totalRecaudado,
                "totalSaldoPendiente" to saldoPendienteTotal,
                "totalClientes" to clientes.size.toLong(),
                "totalItems" to items.size.toLong(),
                "alquileresActivos" to alquileres.count { it.estado == EstadoAlquiler.ACTIVO || it.estado == EstadoAlquiler.VENCIDO }.toLong(),
                "ingresos_${anio}_$mes" to ingresosEsteMes,
                "operaciones_${anio}_$diaAnio" to mapOf("entregas" to entregasHoyCount, "devoluciones" to retornosHoyCount),
                "lastAuditAt" to com.google.firebase.Timestamp.now()
            )
            
            updateWorkspaceUseCase.updateStats(workspaceId, repairData)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Fallo en auditoría: ${e.localizedMessage}"))
        }
    }
}
