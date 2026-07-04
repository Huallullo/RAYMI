package com.raymi.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
        esperarCargaInicial()
        if (estaEnLogin()) {
            realizarLogin(testEmail, testPassword)
        }
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithTag("nav_inicio").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithTag("workspace_selection_title").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun CP_FUN_LOGIN_02_login_fallido_contrasena_incorrecta() {
        esperarCargaInicial()
        if (!estaEnLogin()) {
            composeTestRule.onNodeWithTag("nav_perfil").performClick()
            composeTestRule.onNodeWithText("Cerrar Sesión", ignoreCase = true).performClick()
            esperarCargaInicial()
        }

        composeTestRule.onNodeWithTag("email_input").performTextReplacement(testEmail)
        composeTestRule.onNodeWithTag("password_input").performTextReplacement("wrongPass123")
        composeTestRule.onNodeWithTag("login_button").performClick()

        composeTestRule.waitUntil(25000) {
            composeTestRule.onAllNodes(hasText("incorrect", substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasText("error", substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasText("falló", substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasText("inválid", substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("login_button").assertExists()
    }

    @Test
    fun CP_FUN_CLIENTE_01_registrar_cliente() {
        loginSiEsNecesario()
        composeTestRule.onNodeWithTag("nav_clientes").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("fab_add_cliente").performClick()

        composeTestRule.onNodeWithTag("cliente_dni_input").performTextInput("12345678")
        composeTestRule.onNodeWithTag("cliente_nombre_input").performTextInput("Test User")
        composeTestRule.onNodeWithTag("cliente_apellidos_input").performTextInput("QA")
        composeTestRule.onNodeWithTag("cliente_telefono_input").performTextInput("900000000")
        
        // Cerrar teclado antes de guardar
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("cliente_guardar_button").performClick()

        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodes(hasText("Test User", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun CP_FUN_ITEM_01_registrar_item() {
        loginSiEsNecesario()
        composeTestRule.onNodeWithTag("nav_inventario").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.waitUntil(25000) { composeTestRule.onAllNodesWithTag("fab_add_item").fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithTag("fab_add_item").performClick()

        composeTestRule.waitUntil(15000) {
            composeTestRule.onAllNodesWithTag("item_categoria_spinner").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Crear Categoría", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithText("Crear Categoría", substring = true).fetchSemanticsNodes().isNotEmpty()) {
             composeTestRule.onNodeWithText("Crear Categoría", substring = true).performClick()
             composeTestRule.onNodeWithTag("fab_add_categoria").performClick()
             composeTestRule.onNodeWithTag("categoria_nombre_input").performTextInput("TEST")
             composeTestRule.onNodeWithTag("categoria_guardar_button").performClick()
             composeTestRule.onNodeWithContentDescription("Volver").performClick()
             composeTestRule.onNodeWithTag("fab_add_item").performClick()
        }

        composeTestRule.onNodeWithTag("item_categoria_spinner").performClick()
        composeTestRule.waitUntil(10000) { composeTestRule.onAllNodesWithTag("category_option").fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onAllNodesWithTag("category_option").onFirst().performClick()

        composeTestRule.onNodeWithTag("item_nombre_input").performTextInput("Item QA")
        composeTestRule.onNodeWithTag("item_codigo_input").performTextInput("SKU-QA")
        composeTestRule.onNodeWithTag("item_precio_input").performTextInput("10.00")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("item_guardar_button").performClick()

        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodes(hasText("Item QA", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun CP_FUN_ALQUILER_01_crear_alquiler_completo() {
        loginSiEsNecesario()
        
        // Asegurar que estamos en Alquileres
        composeTestRule.onNodeWithTag("nav_alquileres").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.waitUntil(25000) { composeTestRule.onAllNodesWithTag("fab_create_alquiler").fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithTag("fab_create_alquiler").performClick()

        // 1. Cliente
        composeTestRule.onNodeWithTag("alquiler_select_cliente").performClick()
        composeTestRule.waitUntil(20000) { 
            composeTestRule.onAllNodesWithTag("client_option").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("No se encontraron", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        
        if (composeTestRule.onAllNodesWithTag("client_option").fetchSemanticsNodes().isEmpty()) {
            // Registrar uno rápido si no hay
            Espresso.pressBack()
            CP_FUN_CLIENTE_01_registrar_cliente()
            composeTestRule.onNodeWithTag("nav_alquileres").performClick()
            composeTestRule.onNodeWithTag("fab_create_alquiler").performClick()
            composeTestRule.onNodeWithTag("alquiler_select_cliente").performClick()
            composeTestRule.waitUntil(10000) { composeTestRule.onAllNodesWithTag("client_option").fetchSemanticsNodes().isNotEmpty() }
        }
        composeTestRule.onAllNodesWithTag("client_option").onFirst().performClick()

        // 2. Fecha
        composeTestRule.onNodeWithTag("alquiler_fecha_fin").performClick()
        composeTestRule.waitForIdle()
        try {
            onView(withId(android.R.id.button1)).perform(click())
        } catch (_: Exception) {
            try { onView(withText("OK")).perform(click()) } catch(_:Exception){}
        }
        composeTestRule.waitForIdle()

        // 3. Ítem
        composeTestRule.waitUntil(15000) {
            try {
                composeTestRule.onNodeWithTag("alquiler_add_item").assertIsEnabled()
                true
            } catch (_: AssertionError) { false }
        }
        composeTestRule.onNodeWithTag("alquiler_add_item").performClick()
        composeTestRule.waitUntil(20000) { 
            composeTestRule.onAllNodesWithTag("add_item_confirm_button").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("No se encontraron", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        
        if (composeTestRule.onAllNodesWithTag("add_item_confirm_button").fetchSemanticsNodes().isEmpty()) {
            Espresso.pressBack()
            CP_FUN_ITEM_01_registrar_item()
            composeTestRule.onNodeWithTag("nav_alquileres").performClick()
            composeTestRule.onNodeWithTag("fab_create_alquiler").performClick()
            // Re-seleccionar cliente y fecha
            composeTestRule.onNodeWithTag("alquiler_select_cliente").performClick()
            composeTestRule.onAllNodesWithTag("client_option").onFirst().performClick()
            composeTestRule.onNodeWithTag("alquiler_fecha_fin").performClick()
            try { onView(withId(android.R.id.button1)).perform(click()) } catch(_:Exception){}
            composeTestRule.onNodeWithTag("alquiler_add_item").performClick()
            composeTestRule.waitUntil(10000) { composeTestRule.onAllNodesWithTag("add_item_confirm_button").fetchSemanticsNodes().isNotEmpty() }
        }
        
        composeTestRule.onAllNodesWithTag("add_item_confirm_button").onFirst().performClick()
        
        // Esperar a que el item aparezca en la lista de "inventario" de la pantalla de creación
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithText("S/. ", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Adelanto y Confirmar
        composeTestRule.onNodeWithTag("alquiler_adelanto_input").performScrollTo().performTextReplacement("10.00")
        
        Espresso.closeSoftKeyboard()
        composeTestRule.waitForIdle()

        // 5. Confirmar
        composeTestRule.onNodeWithTag("alquiler_confirmar_button").performScrollTo().performClick()

        // Verificación de éxito: Esperar a que volvamos a ver el botón de "Gestión de Alquileres" o el FAB
        composeTestRule.waitUntil(60000) {
            composeTestRule.onAllNodesWithTag("fab_create_alquiler").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ========== HELPERS ==========

    private fun esperarCargaInicial() {
        composeTestRule.waitUntil(40000) {
            composeTestRule.onAllNodesWithTag("email_input").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithTag("nav_inicio").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithTag("workspace_selection_title").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun estaEnLogin() = composeTestRule.onAllNodesWithTag("email_input").fetchSemanticsNodes().isNotEmpty()

    private fun realizarLogin(email: String, pass: String) {
        composeTestRule.onNodeWithTag("email_input").performTextReplacement(email)
        composeTestRule.onNodeWithTag("password_input").performTextReplacement(pass)
        composeTestRule.onNodeWithTag("login_button").performClick()
        
        composeTestRule.waitUntil(40000) {
            composeTestRule.onAllNodesWithTag("workspace_selection_title").fetchSemanticsNodes().isNotEmpty() ||
                    composeTestRule.onAllNodesWithTag("nav_inicio").fetchSemanticsNodes().isNotEmpty()
        }

        if (composeTestRule.onAllNodesWithTag("workspace_selection_title").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.waitUntil(20000) { composeTestRule.onAllNodesWithTag("workspace_card").fetchSemanticsNodes().isNotEmpty() }
            composeTestRule.onAllNodesWithTag("workspace_card").onFirst().performClick()
        }
    }

    private fun loginSiEsNecesario() {
        esperarCargaInicial()
        if (estaEnLogin()) realizarLogin(testEmail, testPassword)
    }
}
