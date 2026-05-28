package com.raymi.app.domain.model

import com.google.firebase.Timestamp

enum class TipoComprobante {
    TICKET,
    BOLETA,
    FACTURA
}

enum class EstadoComprobante {
    GENERANDO,
    GENERADO,
    COMPARTIDO,
    ANULADO,
    ERROR_GENERACION
}

enum class TipoDocumentoCliente {
    DNI,
    RUC,
    SIN_DOCUMENTO
}

data class Comprobante(
    val id: String = "",
    val workspaceId: String = "",
    val alquilerId: String = "",
    val tipo: TipoComprobante = TipoComprobante.TICKET,
    val serie: String = "", // T001, B001, F001
    val numero: Int = 0,    // 000001
    val clienteId: String? = null,
    val clienteNombre: String = "",
    val clienteDocumento: String = "",
    val clienteTipoDocumento: TipoDocumentoCliente = TipoDocumentoCliente.SIN_DOCUMENTO,
    val razonSocial: String? = null,
    val direccionFiscal: String? = null,
    val subtotal: Double = 0.0,
    val igv: Double = 0.0,
    val total: Double = 0.0,
    val pagado: Double = 0.0,
    val saldo: Double = 0.0,
    val metodoPago: String = "EFECTIVO",
    val pdfUrl: String? = null,
    val localUri: String? = null,
    val estado: EstadoComprobante = EstadoComprobante.GENERADO,
    val generadoPor: String = "", // UID del Admin
    val createdAt: Timestamp = Timestamp.now()
) {
    val correlativoCompleto: String
        get() = "$serie-${numero.toString().padStart(6, '0')}"
}
