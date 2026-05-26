package com.raymi.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RaymiFunctionalTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val testEmail = "test@raymi.com"
    private val testPassword = "Test123456"

    @Test
    fun CP_FUN_LOGIN_01_login_exitoso() {
        // 1. Esperar a que el Splash termine y ver qué pantalla aparece
        esperarCargaInicial()

        // 2. Si estamos en login, procedemos. Si ya estamos dentro, la prueba pasa.
        if (estaEnLogin()) {
            realizarLogin(testEmail, testPassword)
        }

        // 3. Verificar que llegamos a un destino seguro (Dashboard o Selección)
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("nav_inicio").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithText("Selecciona el centro de operaciones").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun CP_FUN_CLIENTE_01_registrar_cliente() {
        loginSiEsNecesario()

        composeTestRule.onNodeWithTag("nav_clientes").performClick()
        composeTestRule.onNodeWithTag("fab_add_cliente").performClick()

        composeTestRule.onNodeWithTag("cliente_dni_input").performTextInput("12345678")
        composeTestRule.onNodeWithTag("cliente_nombre_input").performTextInput("Juan Carlos")
        composeTestRule.onNodeWithTag("cliente_apellidos_input").performTextInput("Pérez García")
        composeTestRule.onNodeWithTag("cliente_telefono_input").performTextInput("987654321")

        composeTestRule.onNodeWithTag("cliente_guardar_button").performClick()

        // Verificación de éxito
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Juan Carlos Pérez García").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Juan Carlos Pérez García").assertExists()
    }

    @Test
    fun CP_FUN_ITEM_01_registrar_item() {
        loginSiEsNecesario()

        composeTestRule.onNodeWithTag("nav_inventario").performClick()
        
        // Esperar a que la pantalla de inventario cargue
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("fab_add_item").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithTag("fab_add_item").performClick()

        // QA Senior: El sistema puede mostrar un diálogo si no hay categorías.
        // Esperamos a ver qué aparece: o el formulario de item o el diálogo de advertencia.
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("item_categoria_spinner").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Categoría Requerida").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Categoría Requerida").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Crear Categoría Ahora").performClick()
            
            // Esperar pantalla de categorías
            composeTestRule.waitUntil(10000) {
                composeTestRule.onAllNodesWithTag("fab_add_categoria").fetchSemanticsNodes().isNotEmpty()
            }

            // Crear categoría "AUTOS"
            composeTestRule.onNodeWithTag("fab_add_categoria").performClick()
            composeTestRule.onNodeWithTag("categoria_nombre_input").performTextInput("AUTOS")
            composeTestRule.onNodeWithTag("categoria_guardar_button").performClick()
            
            // Volver al inventario (Tag: Volver)
            composeTestRule.onNodeWithContentDescription("Volver").performClick()
            
            // Re-intentar añadir item
            composeTestRule.onNodeWithTag("fab_add_item").performClick()
        }

        // Ahora sí, esperar el formulario de registro de item
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("item_categoria_spinner").fetchSemanticsNodes().isNotEmpty()
        }

        // Seleccionar categoría
        composeTestRule.onNodeWithTag("item_categoria_spinner").performClick()
        
        // Esperar a que las categorías carguen en el dropdown
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("AUTOS", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        // QA Senior: Usar .onFirst() para evitar ambigüedad con los chips de la pantalla anterior
        composeTestRule.onAllNodesWithText("AUTOS", ignoreCase = true).onFirst().performClick()

        composeTestRule.onNodeWithTag("item_nombre_input").performTextInput("Vehículo de Prueba")
        composeTestRule.onNodeWithTag("item_codigo_input").performTextInput("SKU-${System.currentTimeMillis() % 1000}")
        composeTestRule.onNodeWithTag("item_precio_input").performTextInput("100.00")

        composeTestRule.onNodeWithTag("item_guardar_button").performClick()
        
        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithText("Vehículo de Prueba").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Vehículo de Prueba").assertExists()
    }

    // ========== HELPERS DE PRUEBA (QA SENIOR) ==========

    private fun esperarCargaInicial() {
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("email_input").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithTag("nav_inicio").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithText("Selecciona el centro de operaciones").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun estaEnLogin(): Boolean {
        return composeTestRule.onAllNodesWithTag("email_input").fetchSemanticsNodes().isNotEmpty()
    }

    private fun realizarLogin(email: String, pass: String) {
        composeTestRule.onNodeWithTag("email_input").performTextInput(email)
        composeTestRule.onNodeWithTag("password_input").performTextInput(pass)
        composeTestRule.onNodeWithTag("login_button").performClick()

        // QA Senior Fix: Esperar a que cargue el siguiente estado (Selección o Dashboard)
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithText("Selecciona el centro de operaciones").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithTag("nav_inicio").fetchSemanticsNodes().isNotEmpty()
        }

        // Si aparece la selección de negocio, hacer clic en el primero disponible
        if (composeTestRule.onAllNodesWithText("Selecciona el centro de operaciones").fetchSemanticsNodes().isNotEmpty()) {
            // Esperar a que los negocios carguen (dejen de estar en loading)
            composeTestRule.waitUntil(10000) {
                composeTestRule.onAllNodesWithTag("workspace_card").fetchSemanticsNodes().isNotEmpty()
            }
            
            // Hacer clic en el primer negocio de la lista
            composeTestRule.onAllNodesWithTag("workspace_card").onFirst().performClick()
        }
    }

    private fun loginSiEsNecesario() {
        esperarCargaInicial()
        if (estaEnLogin()) {
            realizarLogin(testEmail, testPassword)
        }
    }
}
