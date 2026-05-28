package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.domain.model.EmpresaData
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.RucLookupProvider
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class ApiPeruRucProvider @Inject constructor() : RucLookupProvider {
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun buscar(ruc: String): Resource<EmpresaData> {
        val token = BuildConfig.RENIEC_API_TOKEN
        val baseUrl = BuildConfig.RENIEC_API_URL
        if (token.isBlank() || baseUrl.isBlank()) return Resource.Error("API Principal no configurada")
        
        return try {
            val url = "$baseUrl/ruc/$ruc?token=$token"
            val conn = URL(url).openConnection() as HttpURLConnection
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val resp: RucApiResponse = json.decodeFromString(body)
                Resource.Success(EmpresaData(
                    ruc = ruc,
                    razonSocial = resp.razonSocial ?: resp.nombre ?: "",
                    direccion = resp.direccion,
                    departamento = resp.departamento,
                    provincia = resp.provincia,
                    distrito = resp.distrito
                ))
            } else {
                Resource.Error("Error en API Principal: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Resource.Error("Falla de conexión en API Principal")
        }
    }
}

class DecolectaRucProvider @Inject constructor() : RucLookupProvider {
    override suspend fun buscar(ruc: String): Resource<EmpresaData> {
        // Implementar similar a DNI fallback 1
        return Resource.Error("Decolecta RUC no implementado")
    }
}

class ConsultaPeruRucProvider @Inject constructor() : RucLookupProvider {
    override suspend fun buscar(ruc: String): Resource<EmpresaData> {
        // Implementar similar a DNI fallback 2
        return Resource.Error("ConsultaPeru RUC no implementado")
    }
}
