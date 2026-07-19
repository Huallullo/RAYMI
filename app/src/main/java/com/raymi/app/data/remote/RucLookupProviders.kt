package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.domain.model.EmpresaData
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.RucLookupProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
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
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    override suspend fun buscar(ruc: String): Resource<EmpresaData> {
        val token = BuildConfig.RENIEC_API_TOKEN_FALLBACK
        val baseUrl = BuildConfig.RENIEC_API_URL_FALLBACK
        if (token.isBlank() || baseUrl.isBlank()) return Resource.Error("API Fallback 1 no configurada")
        
        return try {
            val url = "$baseUrl?numero=$ruc"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RAYMI-App/1.0")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val resp: DecolectaRucResponse = json.decodeFromString(body)
                val razonSocial = (resp.razon_social ?: resp.nombre_comercial ?: "").trim()
                if (razonSocial.isBlank()) {
                    Resource.Error("RUC no encontrado en API Fallback 1")
                } else {
                    Resource.Success(EmpresaData(
                        ruc = ruc,
                        razonSocial = razonSocial,
                        direccion = resp.direccion,
                        departamento = resp.departamento,
                        provincia = resp.provincia,
                        distrito = resp.distrito
                    ))
                }
            } else {
                Resource.Error("Error en API Fallback 1: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Resource.Error("Falla de conexión en API Fallback 1")
        }
    }
}

class ConsultaPeruRucProvider @Inject constructor() : RucLookupProvider {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    override suspend fun buscar(ruc: String): Resource<EmpresaData> {
        val token = BuildConfig.RENIEC_API_TOKEN_FALLBACK2
        val baseUrl = BuildConfig.RENIEC_API_URL_FALLBACK2
        if (token.isBlank() || baseUrl.isBlank()) return Resource.Error("API Fallback 2 no configurada")
        
        return try {
            val requestBody = json.encodeToString(
                ConsultasPeruRequest(
                    token = token,
                    type_document = "ruc",
                    document_number = ruc
                )
            )
            val conn = URL(baseUrl).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RAYMI-App/1.0")
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
            }
            conn.outputStream.bufferedWriter().use { writer ->
                writer.write(requestBody)
                writer.flush()
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val resp: ConsultasPeruRucResponse = json.decodeFromString(body)
                val data = resp.data
                if (resp.success && data != null) {
                    val razonSocial = (data.full_name ?: data.name ?: "").trim()
                    Resource.Success(EmpresaData(
                        ruc = ruc,
                        razonSocial = razonSocial,
                        direccion = data.address,
                        departamento = data.department,
                        provincia = data.province,
                        distrito = data.district
                    ))
                } else {
                    Resource.Error(resp.message ?: "RUC no encontrado en API Fallback 2")
                }
            } else {
                Resource.Error("Error en API Fallback 2: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Resource.Error("Falla de conexión en API Fallback 2")
        }
    }
}

@kotlinx.serialization.Serializable
data class DecolectaRucResponse(
    val ruc: String? = null,
    val razon_social: String? = null,
    val nombre_comercial: String? = null,
    val direccion: String? = null,
    val departamento: String? = null,
    val provincia: String? = null,
    val distrito: String? = null
)

@kotlinx.serialization.Serializable
data class ConsultasPeruRucResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: ConsultasPeruRucData? = null
)

@kotlinx.serialization.Serializable
data class ConsultasPeruRucData(
    val number: String? = null,
    val full_name: String? = null,
    val name: String? = null,
    val address: String? = null,
    val department: String? = null,
    val province: String? = null,
    val district: String? = null
)
