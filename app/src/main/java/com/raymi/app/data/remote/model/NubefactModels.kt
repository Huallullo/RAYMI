package com.raymi.app.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class NubefactRequest(
    val operacion: String = "generar_comprobante",
    val tipo_de_comprobante: Int,
    val serie: String,
    val numero: Int,
    val sunat_transaction: Int = 1,
    val cliente_tipo_de_documento: String,
    val cliente_numero_de_documento: String,
    val cliente_denominacion: String,
    val cliente_direccion: String = "",
    val cliente_email: String = "",
    val fecha_de_emision: String,
    val moneda: Int = 1,
    val porcentaje_de_igv: Double = 18.0,
    val total_gravada: Double,
    val total_igv: Double,
    val total: Double,
    val items: List<NubefactItem>,
    val enviar_automaticamente_a_la_sunat: Boolean = true,
    val enviar_automaticamente_al_cliente: Boolean = false,
    val formato_de_pdf: String = "A4"
)

@Serializable
data class NubefactItem(
    val unidad_de_medida: String = "ZZ",
    val codigo: String,
    val descripcion: String,
    val cantidad: Double,
    val valor_unitario: Double,
    val precio_unitario: Double,
    val subtotal: Double,
    val tipo_de_igv: Int = 1,
    val igv: Double,
    val total: Double
)

@Serializable
data class NubefactResponse(
    val tipo_de_comprobante: Int? = null,
    val serie: String? = null,
    val numero: Int? = null,
    val enlace: String? = null,
    val enlace_del_pdf: String? = null,
    val enlace_del_xml: String? = null,
    val enlace_del_cdr: String? = null,
    val aceptada_por_sunat: Boolean? = null,
    val sunat_description: String? = null,
    val cadena_para_codigo_qr: String? = null,
    val codigo_hash: String? = null,
    val errors: String? = null,
    val codigo: Int? = null
)
