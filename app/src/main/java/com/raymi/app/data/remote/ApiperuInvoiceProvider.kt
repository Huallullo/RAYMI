package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.data.remote.model.*
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

class ApiperuInvoiceProvider @Inject constructor() : InvoiceProvider {
    
    override val name: String = "ApiPeru.dev (Respaldo 1)"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun emitir(comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace): Resource<String> = withContext(Dispatchers.IO) {
        val urlStr = BuildConfig.APIPERU_URL
        val token = BuildConfig.APIPERU_TOKEN

        if (urlStr.isBlank() || token.isBlank()) {
            return@withContext Resource.Error("ApiPeru no configurado")
        }

        try {
            val request = mapToApiperu(comprobante, alquiler)
            val body = json.encodeToString(request)

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            if (code == 200 || code == 201) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val resp = json.decodeFromString<ApiperuResponse>(respStr)
                
                if (resp.success && resp.data?.pdf_url != null) {
                    Resource.Success(resp.data.pdf_url)
                } else {
                    Resource.Error("ApiPeru: ${resp.message ?: "Falla en respuesta"}")
                }
            } else {
                val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $code"
                Resource.Error("ApiPeru HTTP $code: $errStr")
            }
        } catch (e: Exception) {
            Resource.Error("Falla de conexión con ApiPeru: ${e.message}")
        }
    }

    private fun mapToApiperu(comprobante: Comprobante, alquiler: Alquiler): ApiperuCpeRequest {
        val tipoComp = when (comprobante.tipo) {
            TipoComprobante.FACTURA -> "01"
            TipoComprobante.BOLETA -> "03"
            TipoComprobante.TICKET -> "03"
        }

        val tipoDocCli = when (comprobante.clienteTipoDocumento) {
            TipoDocumentoCliente.DNI -> "1"
            TipoDocumentoCliente.RUC -> "6"
            TipoDocumentoCliente.SIN_DOCUMENTO -> "-"
        }

        val item = ApiperuItem(
            codigo_interno = alquiler.itemCodigo,
            descripcion = alquiler.itemNombre,
            cantidad = alquiler.cantidad.toDouble(),
            precio_unitario = alquiler.precioUnitario,
            valor_unitario = alquiler.precioUnitario / 1.18,
            subtotal = (alquiler.precioUnitario / 1.18) * alquiler.cantidad,
            igv = (alquiler.precioTotal - (alquiler.precioUnitario / 1.18) * alquiler.cantidad),
            total = alquiler.precioTotal
        )

        return ApiperuCpeRequest(
            tipo_comprobante = tipoComp,
            serie = comprobante.serie,
            numero = comprobante.numero,
            fecha_emision = dateFormat.format(comprobante.createdAt.toDate()),
            cliente_tipo_documento = tipoDocCli,
            cliente_numero_documento = comprobante.clienteDocumento,
            cliente_denominacion = comprobante.clienteNombre,
            cliente_direccion = comprobante.direccionFiscal,
            total_gravada = item.subtotal,
            total_igv = item.igv,
            total_venta = item.total,
            items = listOf(item)
        )
    }
}
