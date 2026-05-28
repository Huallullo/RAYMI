package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.domain.model.EmpresaData
import com.raymi.app.domain.model.PersonaData
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalLookupService @Inject constructor(
    private val reniecService: ReniecService
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun buscarDni(dni: String): Resource<PersonaData> {
        val result = reniecService.consultarPorDni(dni)
        return when (result) {
            is Resource.Success -> {
                val data = result.data!!
                Resource.Success(PersonaData(
                    dni = dni,
                    nombres = data.nombres,
                    apellidoPaterno = data.apellidoPaterno,
                    apellidoMaterno = data.apellidoMaterno,
                    fechaNacimiento = data.fechaNacimiento
                ))
            }
            is Resource.Error -> Resource.Error(result.message ?: "Error desconocido")
            is Resource.Loading -> Resource.Loading()
        }
    }

    suspend fun buscarRuc(ruc: String): Resource<EmpresaData> = withContext(Dispatchers.IO) {
        if (ruc.length != 11) return@withContext Resource.Error("RUC inválido")
        
        // Simulación de Fallback para RUC (Similar a DNI)
        // Intentar Provider 1: apisperu.com
        val token = BuildConfig.RENIEC_API_TOKEN // Reutilizando el mismo token si el proveedor es el mismo
        if (token.isNotBlank()) {
             try {
                 val url = "${BuildConfig.RENIEC_API_URL}/ruc/$ruc?token=$token"
                 val conn = URL(url).openConnection() as HttpURLConnection
                 if (conn.responseCode == 200) {
                     val body = conn.inputStream.bufferedReader().use { it.readText() }
                     val resp: RucApiResponse = json.decodeFromString(body)
                     return@withContext Resource.Success(EmpresaData(
                         ruc = ruc,
                         razonSocial = resp.razonSocial ?: resp.nombre ?: "",
                         direccion = resp.direccion,
                         departamento = resp.departamento,
                         provincia = resp.provincia,
                         distrito = resp.distrito
                     ))
                 }
             } catch (_: Exception) {}
        }

        // Mock para desarrollo
        val mock = MOCK_RUC[ruc]
        if (mock != null) Resource.Success(mock)
        else Resource.Error("No se pudo encontrar el RUC en los servidores")
    }

    private val MOCK_RUC = mapOf(
        "20100017491" to EmpresaData("20100017491", "PETROLEOS DEL PERU - PETROPERU S.A."),
        "20131312955" to EmpresaData("20131312955", "SUNAT"),
        "20100128056" to EmpresaData("20100128056", "SEDAPAL")
    )
}

@Serializable
data class RucApiResponse(
    val ruc: String? = null,
    val razonSocial: String? = null,
    val nombre: String? = null,
    val direccion: String? = null,
    val departamento: String? = null,
    val provincia: String? = null,
    val distrito: String? = null
)
