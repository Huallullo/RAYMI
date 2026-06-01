package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
        
        // API Fallback 1 - decolecta.com
        private const val RENIEC_API_BASE_URL_FALLBACK = BuildConfig.RENIEC_API_URL_FALLBACK
        private const val RENIEC_API_TOKEN_FALLBACK    = BuildConfig.RENIEC_API_TOKEN_FALLBACK
        
        // API Fallback 2 - consultasperu.com
        private const val RENIEC_API_BASE_URL_FALLBACK2 = BuildConfig.RENIEC_API_URL_FALLBACK2
        private const val RENIEC_API_TOKEN_FALLBACK2    = BuildConfig.RENIEC_API_TOKEN_FALLBACK2
    }

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    /**
     * Consulta datos de un ciudadano por DNI.
     * Intenta con 3 APIs en cascada: apisperu → decolecta → consultasperu
     */
    suspend fun consultarPorDni(dni: String): Resource<ReniecData> {
        android.util.Log.d("ReniecService", "Iniciando consulta para DNI: $dni")
        
        if (dni.length != 8 || !dni.all { it.isDigit() }) {
            return Resource.Error("El DNI debe tener exactamente 8 dígitos numéricos")
        }

        // Detectar si estamos en modo desarrollo (sin tokens configurados)
        val noTokens = RENIEC_API_TOKEN.isBlank() && 
                       RENIEC_API_TOKEN_FALLBACK.isBlank() && 
                       RENIEC_API_TOKEN_FALLBACK2.isBlank()

        if (noTokens) {
            android.util.Log.d("ReniecService", "Usando datos MOCK (sin tokens configurados)")
            val mockResult = getMockResource(dni)
            return if (mockResult is Resource.Error) {
                Resource.Error("Modo Demo: Solo funcionan los DNI de prueba (ej: 12345678). Configura los tokens reales en local.properties.")
            } else mockResult
        }

        return withContext(Dispatchers.IO) {
            // ... (resto del código igual) ...
            // 1️⃣ Intentar con la API principal (apisperu.com)
            if (RENIEC_API_TOKEN.isNotBlank()) {
                android.util.Log.d("ReniecService", "Intentando API Principal...")
                val resultado = consultarConApi(
                    dni,
                    RENIEC_API_BASE_URL,
                    RENIEC_API_TOKEN,
                    "apisperu.com (API Principal)"
                )
                if (resultado !is Resource.Error) return@withContext resultado
                android.util.Log.w("ReniecService", "API Principal falló: ${resultado.message}")
            }

            // 2️⃣ Intentar con la primera API fallback (decolecta.com)
            if (RENIEC_API_TOKEN_FALLBACK.isNotBlank()) {
                android.util.Log.d("ReniecService", "Intentando API Fallback 1 (Decolecta)...")
                val resultado = consultarConApiDecolecta(
                    dni,
                    RENIEC_API_BASE_URL_FALLBACK,
                    RENIEC_API_TOKEN_FALLBACK
                )
                if (resultado !is Resource.Error) return@withContext resultado
                android.util.Log.w("ReniecService", "API Fallback 1 falló: ${resultado.message}")
            }

            // 3️⃣ Intentar con la segunda API fallback (consultasperu.com)
            if (RENIEC_API_TOKEN_FALLBACK2.isNotBlank()) {
                android.util.Log.d("ReniecService", "Intentando API Fallback 2 (ConsultasPeru)...")
                val resultado = consultarConApiConsultasPeru(
                    dni,
                    RENIEC_API_BASE_URL_FALLBACK2,
                    RENIEC_API_TOKEN_FALLBACK2
                )
                if (resultado !is Resource.Error) return@withContext resultado
                android.util.Log.w("ReniecService", "API Fallback 2 falló: ${resultado.message}")
            }

            // Si todas las APIs fallan, devolver error final
            android.util.Log.e("ReniecService", "Todas las APIs fallaron.")
            Resource.Error("❌ No se pudo conectar con RENIEC. Verifica tu conexión o intenta más tarde.")
        }
    }

    /**
     * Consulta una API específica de RENIEC.
     * @param dni El DNI a consultar
     * @param baseUrl La URL base de la API
     * @param token El token de autenticación
     * @param nombreApi Nombre de la API para logs/errores
     */
     private suspend fun consultarConApi(
         dni: String,
         baseUrl: String,
         token: String,
         nombreApi: String
     ): Resource<ReniecData> {
         return try {
             // Construir la URL para apisperu.com
             val url = "$baseUrl/$dni?token=$token"

             val connection = URL(url).openConnection() as HttpURLConnection
             connection.apply {
                 requestMethod = "GET"
                 setRequestProperty("Content-Type", "application/json")
                 setRequestProperty("Accept", "application/json")
                 setRequestProperty("Authorization", "Bearer $token")
                 setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RAYMI-App/1.0")
                 connectTimeout = 15000
                 readTimeout = 15000
                 doInput = true
             }

             val responseCode = connection.responseCode
             val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
             val responseBody = responseStream?.bufferedReader()?.use { it.readText() } ?: ""
             
             android.util.Log.d("ReniecService", "Respuesta de $nombreApi: Code $responseCode - Body: $responseBody")

             when (responseCode) {
                 HttpURLConnection.HTTP_OK -> {
                     val apiResponse: ReniecApiResponse = json.decodeFromString(responseBody)
                     
                     if (apiResponse.success == false) {
                         return Resource.Error(apiResponse.message ?: "DNI no encontrado")
                     }
                     
                     val nombres = (apiResponse.nombres ?: apiResponse.name ?: "").trim()
                     val apPat = (apiResponse.apellidoPaterno ?: apiResponse.fatherSurname ?: "").trim()
                     val apMat = (apiResponse.apellidoMaterno ?: apiResponse.motherSurname ?: "").trim()

                     if (nombres.isBlank() && apPat.isBlank()) {
                         Resource.Error("DNI $dni no encontrado")
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
                 HttpURLConnection.HTTP_NOT_FOUND -> Resource.Error("DNI no registrado")
                 HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> 
                     Resource.Error("Error de autenticación (Token)")
                 429 -> Resource.Error("Demasiadas consultas. Intenta en unos minutos.")
                 else -> Resource.Error("Servidor temporalmente ocupado ($responseCode)")
             }
         } catch (e: Exception) {
             android.util.Log.e("ReniecService", "Excepción en $nombreApi", e)
             Resource.Error("Error de conexión con RENIEC")
         }
     }

    /**
     * Devuelve datos simulados para desarrollo/testing.
     * En producción, asegúrate de que el token real esté configurado.
     */
    private suspend fun consultarConApiDecolecta(
        dni: String,
        baseUrl: String,
        token: String
    ): Resource<ReniecData> {
        return try {
            // URL para decolecta.com: https://api.decolecta.com/v1/reniec/dni?numero=46027897
            val url = "$baseUrl?numero=$dni"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RAYMI-App/1.0")
                connectTimeout = 15000
                readTimeout = 15000
                doInput = true
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val apiResponse: DecolectaApiResponse = json.decodeFromString(responseBody)
                    
                    val nombres = (apiResponse.first_name ?: "").trim()
                    val apPat = (apiResponse.first_last_name ?: "").trim()
                    val apMat = (apiResponse.second_last_name ?: "").trim()

                    if (nombres.isBlank() && apPat.isBlank()) {
                        Resource.Error("DNI no encontrado")
                    } else {
                        Resource.Success(
                            ReniecData(
                                nombres = nombres,
                                apellidoPaterno = apPat,
                                apellidoMaterno = apMat,
                                fechaNacimiento = ""
                            )
                        )
                    }
                }
                else -> Resource.Error("Error en servidor secundario ($responseCode)")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión (S2)")
        }
    }

    /**
     * Consulta consultasperu.com usando POST con token en el body JSON
     */
    private suspend fun consultarConApiConsultasPeru(
        dni: String,
        baseUrl: String,
        token: String
    ): Resource<ReniecData> {
        return try {
            val connection = URL(baseUrl).openConnection() as HttpURLConnection
            
            // Preparar el JSON del request
            val requestBody = json.encodeToString(
                ConsultasPeruRequest(
                    token = token,
                    type_document = "dni",
                    document_number = dni
                )
            )

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) RAYMI-App/1.0")
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
            }

            // Escribir el body
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = responseStream?.bufferedReader()?.use { it.readText() } ?: ""

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val apiResponse: ConsultasPeruResponse = json.decodeFromString(responseBody)
                    
                    if (apiResponse.success == false) {
                        return Resource.Error(apiResponse.message ?: "DNI no encontrado")
                    }

                    val data = apiResponse.data
                    if (data != null && (data.name?.isNotBlank() == true || data.surname?.isNotBlank() == true)) {
                        Resource.Success(
                            ReniecData(
                                nombres = (data.name ?: "").trim(),
                                apellidoPaterno = (data.surname ?: "").trim(),
                                apellidoMaterno = "",
                                fechaNacimiento = data.date_of_birth?.trim() ?: ""
                            )
                        )
                    } else {
                        Resource.Error("DNI no registrado")
                    }
                }
                else -> Resource.Error("Error en servidor de reserva ($responseCode)")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión (S3)")
        }
    }

    /**
     * Devuelve datos simulados para desarrollo/testing.
     * En producción, asegúrate de que el token real esté configurado.
     */
    private suspend fun getMockResource(dni: String): Resource<ReniecData> {
        kotlinx.coroutines.delay(1000) // Simular latencia de red para feedback visual
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
    val success: Boolean? = null,
    val message: String? = null,
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
 * Respuesta de decolecta.com
 */
@Serializable
data class DecolectaApiResponse(
    val first_name: String? = null,
    val first_last_name: String? = null,
    val second_last_name: String? = null,
    val full_name: String? = null,
    val document_number: String? = null
)

/**
 * Request para consultasperu.com
 */
@Serializable
data class ConsultasPeruRequest(
    val token: String,
    val type_document: String,
    val document_number: String
)

/**
 * Respuesta de consultasperu.com
 */
@Serializable
data class ConsultasPeruResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: ConsultasPeruData? = null
)

@Serializable
data class ConsultasPeruData(
    val number: String? = null,
    val full_name: String? = null,
    val name: String? = null,
    val surname: String? = null,
    val date_of_birth: String? = null,
    val department: String? = null,
    val province: String? = null,
    val district: String? = null,
    val address: String? = null,
    val ubigeo: String? = null
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
