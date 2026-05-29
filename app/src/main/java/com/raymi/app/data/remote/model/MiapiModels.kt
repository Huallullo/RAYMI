package com.raymi.app.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MiapiCpeRequest(
    val tipDoc: String,
    val serie: String,
    val correlativo: Int,
    val fechaEmision: String,
    val moneda: String = "PEN",
    val clienteTipoDoc: String,
    val clienteNumDoc: String,
    val clienteDenominacion: String,
    val clienteDireccion: String? = null,
    val mtoGravada: Double,
    val mtoIgv: Double,
    val mtoTotal: Double,
    val items: List<MiapiItem>
)

@Serializable
data class MiapiItem(
    val codInterno: String,
    val descripcion: String,
    val cantidad: Double,
    val mtoPrecioUnitario: Double,
    val mtoValorUnitario: Double,
    val mtoIgv: Double,
    val mtoBaseIgv: Double,
    val mtoTotalItem: Double,
    val tipAfeIgv: String = "10"
)

@Serializable
data class MiapiResponse(
    val success: Boolean,
    val message: String? = null,
    val pdf_url: String? = null,
    val xml_url: String? = null,
    val cdr_url: String? = null
)
