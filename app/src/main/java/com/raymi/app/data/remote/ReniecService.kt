package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para consultar datos de RENIEC usando API comercial autorizada.
 *
 * CONFIGURACIÓN REQUERIDA:
 * - Reemplaza RENIEC_API_BASE_URL con la URL de tu proveedor (ej: apisperu.com)
 * - Almacena el token en local.properties o en un backend propio, NUNCA en código fuente.
 *
 * Proveedores populares en Perú:
 *  - https://apisperu.com
 *  - https://apis.net.pe
 *  - https://consultasdni.com
 */
@Singleton
class ReniecService @Inject constructor() {

    companion object {
        // ⚠️ IMPORTANTE: Mueve estas constantes a BuildConfig o a un backend seguro.
        // En local.properties agrega: RENIEC_API_URL=https://tu-proveedor.com/api
        //                              RENIEC_API_TOKEN=tu_token_aqui
        private const val RENIEC_API_BASE_URL = BuildConfig.RENIEC_API_URL
        private const val RENIEC_API_TOKEN    = BuildConfig.RENIEC_API_TOKEN
    }

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    /**
     * Consulta datos de un ciudadano por DNI.
     * Primero intenta la API real; si falla o no está configurada, usa datos mock (solo para desarrollo).
     */
    suspend fun consultarPorDni(dni: String): Resource<ReniecData> {
        if (dni.length != 8 || !dni.all { it.isDigit() }) {
            return Resource.Error("El DNI debe tener exactamente 8 dígitos numéricos")
        }

        // Si el token no está configurado, ir directo al mock para no hacer requests inútiles
        if (RENIEC_API_TOKEN.isBlank()) {
            return getMockResource(dni)
        }

        return withContext(Dispatchers.IO) {
            try {
                // Construir la URL de consulta - soporta múltiples proveedores
                // Para apisperu.com: https://dniruc.apisperu.com/api/v1/dni/{dni}?token={token}
                val url = if (RENIEC_API_BASE_URL.contains("apisperu")) {
                    "$RENIEC_API_BASE_URL/$dni?token=$RENIEC_API_TOKEN"
                } else if (RENIEC_API_BASE_URL.contains("decolecta")) {
                    "$RENIEC_API_BASE_URL/dnis/$dni"
                } else {
                    "$RENIEC_API_BASE_URL/$dni"
                }

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RAYMI-App/1.0")
                    connectTimeout = 10000
                    readTimeout = 10000
                    doInput = true
                }

                val responseCode = connection.responseCode
                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        val apiResponse: ReniecApiResponse = json.decodeFromString(responseBody)
                        
                        val nombres = apiResponse.nombres?.trim() ?: apiResponse.name?.trim() ?: ""
                        val apPat = apiResponse.apellidoPaterno?.trim() ?: apiResponse.fatherSurname?.trim() ?: ""
                        val apMat = apiResponse.apellidoMaterno?.trim() ?: apiResponse.motherSurname?.trim() ?: ""

                        if (nombres.isBlank() && apPat.isBlank()) {
                            Resource.Error("La respuesta de RENIEC no contiene datos válidos")
                        } else {
                            Resource.Success(
                                ReniecData(
                                    nombres = nombres,
                                    apellidoPaterno = apPat,
                                    apellidoMaterno = apMat,
                                    fechaNacimiento = apiResponse.fechaNacimiento ?: apiResponse.dateOfBirth ?: ""
                                )
                            )
                        }
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> Resource.Error("DNI $dni no encontrado en RENIEC")
                    HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> 
                        Resource.Error("Token de API inválido o expirado. Revisa tu configuración.")
                    429 -> Resource.Error("Límite de consultas excedido. Intenta más tarde.")
                    else -> Resource.Error("Error del servidor RENIEC (código $responseCode)")
                }
            } catch (e: Exception) {
                // Mostrar el error real en lugar de fallback a mock
                Resource.Error("Error de conexión: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Devuelve datos simulados para desarrollo/testing.
     * En producción, asegúrate de que el token real esté configurado.
     */
    private fun getMockResource(dni: String): Resource<ReniecData> {
        val data = MOCK_DATA[dni]
        return if (data != null) {
            Resource.Success(data)
        } else {
            Resource.Error("DNI no encontrado en la base de datos de desarrollo")
        }
    }

    private val MOCK_DATA = mapOf(
        "12345678" to ReniecData("Juan Carlos",  "Pérez",    "García",    "1990-01-15"),
        "87654321" to ReniecData("María Elena",  "López",    "Rodríguez", "1985-03-22"),
        "11223344" to ReniecData("Carlos Alberto","Martínez","Silva",     "1992-07-10"),
        "44332211" to ReniecData("Ana María",    "Gómez",   "Torres",    "1988-11-05"),
        "55667788" to ReniecData("Pedro José",   "Ramírez", "Díaz",      "1995-09-18"),
        "66778899" to ReniecData("Laura Isabel", "Fernández","Ruiz",     "1991-12-30"),
        "77889900" to ReniecData("Miguel Ángel", "Sánchez", "Morales",   "1987-06-14"),
        "88990011" to ReniecData("Sofía Andrea", "Jiménez", "Castro",    "1993-08-25"),
        "99001122" to ReniecData("Diego Armando","Ruiz",    "Vargas",    "1989-04-07"),
        "00112233" to ReniecData("Valentina Rosa","Morales","Peña",      "1994-02-19"),
        "23456789" to ReniecData("Luis Fernando","Torres",  "Mendoza",   "1986-05-12"),
        "34567890" to ReniecData("Carmen Rosa",  "Flores",  "Castillo",  "1990-08-03"),
        "45678901" to ReniecData("Jorge Luis",   "Rivas",   "Herrera",   "1984-10-28"),
        "56789012" to ReniecData("Patricia Elena","Gutiérrez","León",    "1991-06-15"),
        "67890123" to ReniecData("Roberto Carlos","Vargas", "Soto",      "1987-09-07"),
        "78901234" to ReniecData("Gabriela Susana","Reyes", "Aguilar",   "1993-11-20"),
        "89012345" to ReniecData("Fernando José","Mendoza", "Rojas",     "1985-12-01"),
        "90123456" to ReniecData("Mónica Beatriz","Silva",  "Ponce",     "1992-04-18"),
        "01234567" to ReniecData("Ricardo Antonio","Ortega","Vega",      "1988-07-25"),
        "13579246" to ReniecData("Elena Cristina","Delgado","Campos",    "1994-03-14")
    )
}

// ─── Modelos de respuesta ────────────────────────────────────────────────────

@Serializable
data class ReniecApiResponse(
    val dni: String? = null,
    val nombres: String? = null,
    val name: String? = null,
    val apellidoPaterno: String? = null,
    val fatherSurname: String? = null,
    val apellidoMaterno: String? = null,
    val motherSurname: String? = null,
    val fechaNacimiento: String? = null,
    val dateOfBirth: String? = null,
    val codVerifica: String? = null,
    val estado: String? = null
)

/**
 * Modelo de dominio simplificado con los datos necesarios para la app.
 */
data class ReniecData(
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String = "",
    val fechaNacimiento: String = ""
) {
    /** Apellidos completos: "Pérez García" */
    val apellidosCompletos: String
        get() = "$apellidoPaterno $apellidoMaterno".trim()

    /** Nombre completo: "Juan Carlos Pérez García" */
    val nombreCompleto: String
        get() = "$nombres $apellidosCompletos".trim()
}
