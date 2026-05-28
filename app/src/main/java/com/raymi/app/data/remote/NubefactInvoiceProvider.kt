package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.data.remote.model.NubefactItem
import com.raymi.app.data.remote.model.NubefactRequest
import com.raymi.app.data.remote.model.NubefactResponse
import com.raymi.app.domain.model.*
import com.raymi.app.domain.repository.InvoiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class NubefactInvoiceProvider @Inject constructor() : InvoiceProvider {
    
    override val name: String = "Nubefact (PSE Autorizado)"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    override suspend fun emitir(comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace): Resource<String> = withContext(Dispatchers.IO) {
        val urlStr = BuildConfig.NUBEFACT_URL
        val token = BuildConfig.NUBEFACT_TOKEN

        if (urlStr.isBlank() || token.isBlank()) {
            return@withContext Resource.Error("Nubefact no configurado")
        }

        try {
            val request = mapToNubefact(comprobante, alquiler)
            val body = json.encodeToString(request)

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", token)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode == 200) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val resp = json.decodeFromString<NubefactResponse>(respStr)
                
                if (resp.errors != null) {
                    Resource.Error("Nubefact: ${resp.errors}")
                } else {
                    // Retornamos el enlace del PDF como URI final
                    Resource.Success(resp.enlace_del_pdf ?: resp.enlace ?: "")
                }
            } else {
                val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error ${conn.responseCode}"
                Resource.Error("Nubefact Error HTTP: $errStr")
            }
        } catch (e: Exception) {
            Resource.Error("Falla de conexión con Nubefact: ${e.message}")
        }
    }

    private fun mapToNubefact(comprobante: Comprobante, alquiler: Alquiler): NubefactRequest {
        val tipoComp = when (comprobante.tipo) {
            TipoComprobante.FACTURA -> 1
            TipoComprobante.BOLETA -> 2
            TipoComprobante.TICKET -> 2 // Se emite como boleta si se manda a PSE
        }

        val tipoDocCli = when (comprobante.clienteTipoDocumento) {
            TipoDocumentoCliente.DNI -> "1"
            TipoDocumentoCliente.RUC -> "6"
            TipoDocumentoCliente.SIN_DOCUMENTO -> "-"
        }

        val item = NubefactItem(
            codigo = alquiler.itemCodigo,
            descripcion = alquiler.itemNombre,
            cantidad = alquiler.cantidad.toDouble(),
            valor_unitario = alquiler.precioUnitario / 1.18,
            precio_unitario = alquiler.precioUnitario,
            subtotal = (alquiler.precioUnitario / 1.18) * alquiler.cantidad,
            igv = (alquiler.precioUnitario - (alquiler.precioUnitario / 1.18)) * alquiler.cantidad,
            total = alquiler.precioTotal
        )

        return NubefactRequest(
            tipo_de_comprobante = tipoComp,
            serie = comprobante.serie,
            numero = comprobante.numero,
            cliente_tipo_de_documento = tipoDocCli,
            cliente_numero_de_documento = comprobante.clienteDocumento,
            cliente_denominacion = comprobante.clienteNombre,
            cliente_direccion = comprobante.direccionFiscal ?: "",
            fecha_de_emision = dateFormat.format(comprobante.createdAt.toDate()),
            total_gravada = item.subtotal,
            total_igv = item.igv,
            total = item.total,
            items = listOf(item)
        )
    }
}
