package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Boleta electrónica PERÚ (Comprobante de venta)
 * 
 * Generada después que un alquiler es DEVUELTO y PAGADO completamente
 * CódigoQR incluye datos SUNAT
 * 
 * Requisito PERÚ:
 * - Solo después de alquiler DEVUELTO
 * - Firmada digitalmente (puede ser empresa o desarrollador)
 * - Almacenada en SUNAT
 * - Usuario descarga como PDF
 */
data class Boleta(
    val id: String = "",  // Formato: WORKSPACE-CORRELATIVO (ej: WS001-000001)
    val alquilerId: String = "",
    val workspaceId: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val clienteDni: String = "",
    val clienteEmail: String = "",
    val clienteDireccion: String = "",
    
    // Datos del negocio/workspace
    val negocioNombre: String = "",
    val negocioRuc: String = "",  // RUC del propietario del workspace
    val negocioEmail: String = "",
    val negocioDireccion: String = "",
    val negocioTelefono: String = "",
    
    // Items alquilados
    val items: List<BoletaItem> = emptyList(),
    
    // Montos
    val subtotal: Double = 0.0,
    val igv: Double = 0.0,  // 18% en PERÚ
    val total: Double = 0.0,
    val moneda: String = "PEN",  // Soles peruanos
    
    // Método de pago
    val metodoPago: String = "EFECTIVO",  // EFECTIVO|TRANSFERENCIA|TARJETA|BILLETERA
    val referenciaTransferencia: String? = null,  // Número de operación si fue transferencia
    
    // Estados
    val estado: EstadoBoleta = EstadoBoleta.BORRADOR,
    val numeroBoleta: Int = 0,  // Correlativo: 000001, 000002...
    
    // Datos notariales (SUNAT)
    val serieNumeracion: String = "B001",  // Serie de numeración SUNAT
    val codigoQr: String = "",  // Datos QR: SUNAT standard
    val hashFirma: String? = null,  // Hash digital
    val cpeUblVersion: String = "2.1",  // Versión UBL estándar
    
    // Timestamps
    val fechaEmision: Timestamp = Timestamp.now(),
    val fechaPago: Timestamp? = null,
    val fechaVencimiento: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    
    // Observaciones
    val observaciones: String = ""
) {
    val fechaFormatted: String
        get() = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(fechaEmision.toDate())
    
    val boletaCompleta: String
        get() = "$serieNumeracion-$numeroBoleta".padEnd(7, '0')
}

data class BoletaItem(
    val itemNombre: String = "",
    val itemCodigo: String = "",
    val cantidad: Int = 1,
    val diasAlquiler: Int = 1,
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0
)

enum class EstadoBoleta {
    BORRADOR,       // Aún no se genera
    EMITIDA,        // Se ha generado
    PAGADA,         // Cliente pagó todo
    CANCELADA,      // Se anuló
    DEVUELTA        // Se devolvió/anula
}

