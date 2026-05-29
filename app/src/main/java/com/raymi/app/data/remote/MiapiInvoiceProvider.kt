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

class MiapiInvoiceProvider @Inject constructor() : InvoiceProvider {
    
    override val name: String = "MiApi.cloud (Respaldo 2)"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun emitir(comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace): Resource<String> = withContext(Dispatchers.IO) {
        val urlStr = BuildConfig.MIAPI_URL
        val token = BuildConfig.MIAPI_TOKEN

        if (urlStr.isBlank() || token.isBlank()) {
            return@withContext Resource.Error("MiApi no configurado")
        }

        try {
            val request = mapToMiapi(comprobante, alquiler)
            val body = json.encodeToString(request)

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 20000
            }

            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode == 200 || conn.responseCode == 201) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val resp = json.decodeFromString<MiapiResponse>(respStr)
                
                if (resp.success && resp.pdf_url != null) {
                    Resource.Success(resp.pdf_url)
                } else {
                    Resource.Error("MiApi: ${resp.message ?: "Error en la emisión"}")
                }
            } else {
                val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error ${conn.responseCode}"
                Resource.Error("MiApi HTTP Error: $errStr")
            }
        } catch (e: Exception) {
            Resource.Error("Falla de conexión con MiApi: ${e.message}")
        }
    }

    private fun mapToMiapi(comprobante: Comprobante, alquiler: Alquiler): MiapiCpeRequest {
        val tipoComp = when (comprobante.tipo) {
            TipoComprobante.FACTURA -> "01"
            TipoComprobante.BOLETA -> "03"
            TipoComprobante.TICKET -> "03"
        }

        val tipoDocCli = when (comprobante.clienteTipoDocumento) {
            TipoDocumentoCliente.DNI -> "1"
            TipoDocumentoCliente.RUC -> "6"
            TipoDocumentoCliente.SIN_DOCUMENTO -> "0"
        }

        val item = MiapiItem(
            codInterno = alquiler.itemCodigo,
            descripcion = alquiler.itemNombre,
            cantidad = alquiler.cantidad.toDouble(),
            mtoPrecioUnitario = alquiler.precioUnitario,
            mtoValorUnitario = alquiler.precioUnitario / 1.18,
            mtoBaseIgv = (alquiler.precioUnitario / 1.18) * alquiler.cantidad,
            mtoIgv = (alquiler.precioTotal - (alquiler.precioUnitario / 1.18) * alquiler.cantidad),
            mtoTotalItem = alquiler.precioTotal
        )

        return MiapiCpeRequest(
            tipDoc = tipoComp,
            serie = comprobante.serie,
            correlativo = comprobante.numero,
            fechaEmision = dateFormat.format(comprobante.createdAt.toDate()),
            clienteTipoDoc = tipoDocCli,
            clienteNumDoc = comprobante.clienteDocumento,
            clienteDenominacion = comprobante.clienteNombre,
            clienteDireccion = comprobante.direccionFiscal,
            mtoGravada = item.mtoBaseIgv,
            mtoIgv = item.mtoIgv,
            mtoTotal = item.mtoTotalItem,
            items = listOf(item)
        )
    }
}
