package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.*

data class ComprobanteDto(
    val id: String = "",
    val workspaceId: String = "",
    val alquilerId: String = "",
    val tipo: String = "TICKET",
    val serie: String = "",
    val numero: Int = 0,
    val clienteId: String? = null,
    val clienteNombre: String = "",
    val clienteDocumento: String = "",
    val clienteTipoDocumento: String = "SIN_DOCUMENTO",
    val razonSocial: String? = null,
    val direccionFiscal: String? = null,
    val subtotal: Double = 0.0,
    val igv: Double = 0.0,
    val total: Double = 0.0,
    val pagado: Double = 0.0,
    val saldo: Double = 0.0,
    val metodoPago: String = "EFECTIVO",
    val pdfUrl: String? = null,
    val estado: String = "GENERADO",
    val generadoPor: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toDomain(): Comprobante = Comprobante(
        id = id,
        workspaceId = workspaceId,
        alquilerId = alquilerId,
        tipo = try { TipoComprobante.valueOf(tipo) } catch (_: Exception) { TipoComprobante.TICKET },
        serie = serie,
        numero = numero,
        clienteId = clienteId,
        clienteNombre = clienteNombre,
        clienteDocumento = clienteDocumento,
        clienteTipoDocumento = try { TipoDocumentoCliente.valueOf(clienteTipoDocumento) } catch (_: Exception) { TipoDocumentoCliente.SIN_DOCUMENTO },
        razonSocial = razonSocial,
        direccionFiscal = direccionFiscal,
        subtotal = subtotal,
        igv = igv,
        total = total,
        pagado = pagado,
        saldo = saldo,
        metodoPago = metodoPago,
        pdfUrl = pdfUrl,
        estado = try { EstadoComprobante.valueOf(estado) } catch (_: Exception) { EstadoComprobante.GENERADO },
        generadoPor = generadoPor,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: Comprobante): ComprobanteDto = ComprobanteDto(
            id = domain.id,
            workspaceId = domain.workspaceId,
            alquilerId = domain.alquilerId,
            tipo = domain.tipo.name,
            serie = domain.serie,
            numero = domain.numero,
            clienteId = domain.clienteId,
            clienteNombre = domain.clienteNombre,
            clienteDocumento = domain.clienteDocumento,
            clienteTipoDocumento = domain.clienteTipoDocumento.name,
            razonSocial = domain.razonSocial,
            direccionFiscal = domain.direccionFiscal,
            subtotal = domain.subtotal,
            igv = domain.igv,
            total = domain.total,
            pagado = domain.pagado,
            saldo = domain.saldo,
            metodoPago = domain.metodoPago,
            pdfUrl = domain.pdfUrl,
            estado = domain.estado.name,
            generadoPor = domain.generadoPor,
            createdAt = domain.createdAt
        )

        fun fromMap(id: String, map: Map<String, Any>): ComprobanteDto = ComprobanteDto(
            id = id,
            workspaceId = map["workspaceId"] as? String ?: "",
            alquilerId = map["alquilerId"] as? String ?: "",
            tipo = map["tipo"] as? String ?: "TICKET",
            serie = map["serie"] as? String ?: "",
            numero = (map["numero"] as? Number)?.toInt() ?: 0,
            clienteId = map["clienteId"] as? String,
            clienteNombre = map["clienteNombre"] as? String ?: "",
            clienteDocumento = map["clienteDocumento"] as? String ?: "",
            clienteTipoDocumento = map["clienteTipoDocumento"] as? String ?: "SIN_DOCUMENTO",
            razonSocial = map["razonSocial"] as? String,
            direccionFiscal = map["direccionFiscal"] as? String,
            subtotal = (map["subtotal"] as? Number)?.toDouble() ?: 0.0,
            igv = (map["igv"] as? Number)?.toDouble() ?: 0.0,
            total = (map["total"] as? Number)?.toDouble() ?: 0.0,
            pagado = (map["pagado"] as? Number)?.toDouble() ?: 0.0,
            saldo = (map["saldo"] as? Number)?.toDouble() ?: 0.0,
            metodoPago = map["metodoPago"] as? String ?: "EFECTIVO",
            pdfUrl = map["pdfUrl"] as? String,
            estado = map["estado"] as? String ?: "GENERADO",
            generadoPor = map["generadoPor"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "workspaceId" to workspaceId,
        "alquilerId" to alquilerId,
        "tipo" to tipo,
        "serie" to serie,
        "numero" to numero,
        "clienteId" to clienteId,
        "clienteNombre" to clienteNombre,
        "clienteDocumento" to clienteDocumento,
        "clienteTipoDocumento" to clienteTipoDocumento,
        "razonSocial" to razonSocial,
        "direccionFiscal" to direccionFiscal,
        "subtotal" to subtotal,
        "igv" to igv,
        "total" to total,
        "pagado" to pagado,
        "saldo" to saldo,
        "metodoPago" to metodoPago,
        "pdfUrl" to pdfUrl,
        "estado" to estado,
        "generadoPor" to generadoPor,
        "createdAt" to createdAt
    )
}
