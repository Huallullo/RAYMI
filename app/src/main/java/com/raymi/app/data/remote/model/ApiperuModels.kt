package com.raymi.app.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiperuCpeRequest(
    val tipo_comprobante: String,
    val serie: String,
    val numero: Int,
    val fecha_emision: String,
    val moneda: String = "PEN",
    val cliente_tipo_documento: String,
    val cliente_numero_documento: String,
    val cliente_denominacion: String,
    val cliente_direccion: String? = null,
    val total_gravada: Double,
    val total_igv: Double,
    val total_venta: Double,
    val items: List<ApiperuItem>
)

@Serializable
data class ApiperuItem(
    val codigo_interno: String,
    val descripcion: String,
    val cantidad: Double,
    val unidad_medida: String = "NIU",
    val precio_unitario: Double,
    val valor_unitario: Double,
    val subtotal: Double,
    val tipo_igv: String = "10",
    val igv: Double,
    val total: Double
)

@Serializable
data class ApiperuResponse(
    val success: Boolean,
    val message: String? = null,
    val data: ApiperuCpeData? = null
)

@Serializable
data class ApiperuCpeData(
    val pdf_url: String? = null,
    val xml_url: String? = null,
    val cdr_url: String? = null,
    val hash: String? = null,
    val qr: String? = null
)
