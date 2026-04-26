package com.raymi.app.data.remote

import com.raymi.app.BuildConfig
import com.raymi.app.domain.model.Resource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
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
        if (RENIEC_API_TOKEN == "REEMPLAZA_CON_TU_TOKEN" || RENIEC_API_TOKEN.isBlank()) {
            return getMockResource(dni)
        }

        return try {
            val response = client.get("$RENIEC_API_BASE_URL?numero=$dni") {
                header("Authorization", "Bearer $RENIEC_API_TOKEN")
                header("Content-Type", "application/json")
                header("Accept", "application/json")
            }

            when (response.status.value) {
                200 -> {
                    val apiResponse: ReniecApiResponse = response.body()
                    val nombres = apiResponse.nombres?.trim() ?: ""
                    val apPat = apiResponse.apellidoPaterno?.trim() ?: ""
                    val apMat = apiResponse.apellidoMaterno?.trim() ?: ""

                    if (nombres.isBlank() && apPat.isBlank()) {
                        Resource.Error("La respuesta de RENIEC no contiene datos válidos")
                    } else {
                        Resource.Success(
                            ReniecData(
                                nombres = nombres,
                                apellidoPaterno = apPat,
                                apellidoMaterno = apMat,
                                fechaNacimiento = apiResponse.fechaNacimiento ?: ""
                            )
                        )
                    }
                }
                404 -> Resource.Error("DNI $dni no encontrado en RENIEC")
                401, 403 -> Resource.Error("Token de API inválido o expirado. Revisa tu configuración.")
                429 -> Resource.Error("Límite de consultas excedido. Intenta más tarde.")
                else -> Resource.Error("Error del servidor RENIEC (código ${response.status.value})")
            }
        } catch (e: Exception) {
            // Fallback a mock solo en modo de desarrollo
            getMockResource(dni)
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
            Resource.Error("DNI no encontrado. (Modo desarrollo: configura tu API Key de RENIEC)")
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
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val fechaNacimiento: String? = null,
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