# 💻 GUÍA PRÁCTICA: CAMBIOS DE CÓDIGO PARA SaaS

**Objetivo:** Transformar el código actual de single-tenant → multi-tenant SaaS  
**Tiempo estimado:** 40-60 horas de desarrollo  
**Complejidad:** Alta

---

## 🎯 FASE 1: CREAR MODELOS NUEVOS (4-6 horas)

### **1. Crear `Workspace.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/model/Workspace.kt

package com.raymi.app.domain.model

import com.google.firebase.Timestamp

data class Workspace(
    val id: String = "",
    val ownerId: String = "",              // Usuario propietario
    val nombre: String = "",               // "Mi Tienda de Vestuarios"
    val descripcion: String = "",
    val tipoNegocio: String = "",          // VESTUARIOS|EQUIPOS|VEHICULOS|OTRO
    val logoUrl: String? = null,
    val activo: Boolean = true,
    
    // Configuración del workspace
    val moneda: String = "PEN",            // Moneda: PEN, USD, etc
    val zonaHoraria: String = "America/Lima",
    val idioma: String = "es",
    val mostrarAnuncios: Boolean = true,   // Mostrar ads si plan = FREE
    
    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val ultimoAcceso: Timestamp = Timestamp.now()
)

data class ConfiguracionWorkspace(
    val moneda: String = "PEN",
    val zonaHoraria: String = "America/Lima",
    val idioma: String = "es",
    val mostrarAnuncios: Boolean = true
)
```

### **2. Crear `Item.kt`** ✅ NUEVO (antes: Vestuario → genérico)

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/model/Item.kt

package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Item genérico para cualquier tipo de alquiler
 * Reemplaza a Vestuario (que era específico de ropa)
 */
data class Item(
    val id: String = "",
    val workspaceId: String = "",          // A qué workspace pertenece
    val nombre: String = "",               // "Traje de Siku"
    val codigo: String = "",               // SKU único
    val categoriaId: String = "",          // Referencia a Categoria
    val descripcion: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 1,
    val estado: String = "DISPONIBLE",     // DISPONIBLE|ALQUILADO|MANTENIMIENTO|NO_DISPONIBLE
    
    // Atributos dinámicos (usuario define según tipo de negocio)
    val atributos: Map<String, String> = mapOf(),
    // Ejemplos:
    // Si es ropa: { "talla": "M", "color": "rojo", "danza": "Marinera" }
    // Si es equipos: { "marca": "Sony", "modelo": "A7III" }
    // Si es vehículos: { "placa": "ABC123", "capacidad": "5 pasajeros" }
    
    val imagenUrl: String? = null,
    val imagenesSuplementarias: List<String> = emptyList(),
    
    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val espacioOcupado: Int
        get() = (cantidad - 1)  // Contar alquileres activos
}
```

### **3. Crear `Categoria.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/model/Categoria.kt

package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Agrupa items por tipo dentro de un workspace
 * Ejemplos:
 * - "Trajes Folkóricos" (para vestuarios)
 * - "Cámaras" (para equipos de cine)
 * - "SUV" (para vehículos)
 */
data class Categoria(
    val id: String = "",
    val workspaceId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String? = null,             // URL o emoji
    val color: String = "#3F51B5",         // Color hexadecimal
    val activa: Boolean = true,
    val orden: Int = 0,                    // Para ordenar en UI
    val createdAt: Timestamp = Timestamp.now()
)
```

### **4. Crear `UserPlan.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/model/UserPlan.kt

package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Representa el plan/suscripción del usuario
 */
data class UserPlan(
    val userId: String = "",
    val tipo: String = "FREE",             // FREE|PRO|ENTERPRISE
    val activo: Boolean = true,
    
    // Para subscripciones pagadas
    val transactionId: String? = null,     // Google Play transaction ID
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFin: Timestamp? = null,       // null = sin expiración
    val renovacionAutomatica: Boolean = true,
    
    // Límites según plan
    val limites: LimitesPlan = LimitesPlan(),
    
    // Anuncios
    val mostrarAnuncios: Boolean = true,   // FREE = true, PRO = false
    val anunciosMostrados: Int = 0,
    val ultimaVezAnunciado: Timestamp? = null,
    
    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class LimitesPlan(
    val workspacesMax: Int = 1,            // FREE: 1, PRO: ilimitado
    val itemsMax: Int = 50,                // FREE: 50, PRO: ilimitado
    val alquileresMax: Int = 100,          // FREE: 100, PRO: ilimitado
    val clientesMax: Int = 50              // FREE: 50, PRO: ilimitado
)
```

---

## 🔄 FASE 2: REFACTORIZAR MODELOS EXISTENTES (6-8 horas)

### **1. Actualizar `Alquiler.kt` para multi-workspace**

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/model/Alquiler.kt

// ANTES
data class Alquiler(
    val id: String = "",
    val vestuarioId: String = "",
    // ... resto
)

// DESPUÉS
data class Alquiler(
    val id: String = "",
    val workspaceId: String = "",          // ✅ NUEVO
    val itemId: String = "",               // ✅ RENOMBRADO (antes: vestuarioId)
    val itemNombre: String = "",           // ✅ ACTUALIZADO
    val itemCodigo: String = "",           // ✅ ACTUALIZADO
    // ... resto sin cambios
)
```

### **2. Actualizar `Cliente.kt` para multi-workspace**

```kotlin
// ANTES
data class Cliente(
    val id: String = "",
    val dni: String = "",
    // ... resto
)

// DESPUÉS
data class Cliente(
    val id: String = "",
    val workspaceId: String = "",          // ✅ NUEVO
    val dni: String = "",
    // ... resto sin cambios
    
    // El DNI ahora es único por workspace, no global
)
```

---

## 🏗️ FASE 3: NUEVOS REPOSITORIES (8-10 horas)

### **1. Crear `WorkspaceRepository.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/repository/WorkspaceRepository.kt

package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    
    /**
     * Obtiene todos los workspaces del usuario actual
     */
    suspend fun getWorkspacesDelUsuario(userId: String): Flow<Resource<List<Workspace>>>
    
    /**
     * Obtiene un workspace específico por ID
     */
    suspend fun getWorkspaceById(workspaceId: String): Flow<Resource<Workspace>>
    
    /**
     * Crea un nuevo workspace
     */
    suspend fun crearWorkspace(
        ownerId: String,
        nombre: String,
        tipoNegocio: String,
        descripcion: String
    ): Flow<Resource<String>>  // Retorna workspaceId
    
    /**
     * Actualiza configuración del workspace
     */
    suspend fun actualizarWorkspace(workspace: Workspace): Flow<Resource<Unit>>
    
    /**
     * Elimina un workspace
     */
    suspend fun eliminarWorkspace(workspaceId: String): Flow<Resource<Unit>>
}
```

### **2. Crear `ItemRepository.kt`** ✅ NUEVO (reemplaza VestuarioRepository)

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/repository/ItemRepository.kt

package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Item
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    
    /**
     * Obtiene todos los items del workspace
     */
    suspend fun getItemsByWorkspace(workspaceId: String): Flow<Resource<List<Item>>>
    
    /**
     * Obtiene items por categoría
     */
    suspend fun getItemsByCategoria(
        workspaceId: String,
        categoriaId: String
    ): Flow<Resource<List<Item>>>
    
    /**
     * Obtiene un item específico
     */
    suspend fun getItemById(workspaceId: String, itemId: String): Flow<Resource<Item>>
    
    /**
     * Crea un nuevo item
     */
    suspend fun createItem(workspaceId: String, item: Item): Flow<Resource<String>>
    
    /**
     * Actualiza un item
     */
    suspend fun updateItem(workspaceId: String, item: Item): Flow<Resource<Unit>>
    
    /**
     * Elimina un item
     */
    suspend fun deleteItem(workspaceId: String, itemId: String): Flow<Resource<Unit>>
    
    /**
     * Obtiene items disponibles
     */
    suspend fun getItemsDisponibles(workspaceId: String): Flow<Resource<List<Item>>>
}
```

### **3. Crear `UserPlanRepository.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/domain/repository/UserPlanRepository.kt

package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import kotlinx.coroutines.flow.Flow

interface UserPlanRepository {
    
    /**
     * Obtiene el plan actual del usuario
     */
    suspend fun getPlanDelUsuario(userId: String): Flow<Resource<UserPlan>>
    
    /**
     * Actualiza plan al usuario
     */
    suspend fun actualizarPlan(userPlan: UserPlan): Flow<Resource<Unit>>
    
    /**
     * Verifica si usuario alcanzó límite
     */
    suspend fun verificarLimite(userId: String, tipoLimite: String): Flow<Resource<Boolean>>
}
```

---

## 🔌 FASE 4: IMPLEMENTACIONES EN DATA LAYER (10-12 horas)

### **1. Actualizar `FirebaseDataSource.kt`**

```kotlin
// Archivo: app/src/main/java/com/raymi/app/data/remote/FirebaseDataSource.kt

// ANTES
suspend fun getBusinessAlquileres(): List<Pair<String, Map<String, Any>>> {
    return firestore.collection("alquileres")
        .get()
        .await()
        .documents
        .map { it.id to it.data }
}

// DESPUÉS
suspend fun getAlquileresByWorkspace(workspaceId: String): List<Pair<String, Map<String, Any>>> {
    return firestore
        .collection("workspaces")
        .document(workspaceId)
        .collection("alquileres")
        .get()
        .await()
        .documents
        .map { it.id to it.data }
}

// NUEVO: Items (antes: vestuarios)
suspend fun getItemsByWorkspace(workspaceId: String): List<Pair<String, Map<String, Any>>> {
    return firestore
        .collection("workspaces")
        .document(workspaceId)
        .collection("items")  // ← Cambio importante
        .get()
        .await()
        .documents
        .map { it.id to it.data }
}

// NUEVO: Workspaces
suspend fun getWorkspacesByUser(userId: String): List<Pair<String, Map<String, Any>>> {
    return firestore
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .get()
        .await()
        .documents
        .map { it.id to it.data }
}
```

### **2. Crear `ItemRepositoryImpl.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/data/repository/ItemRepositoryImpl.kt

package com.raymi.app.data.repository

import com.raymi.app.data.model.ItemDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : ItemRepository {
    
    override suspend fun getItemsByWorkspace(workspaceId: String): Flow<Resource<List<Item>>> = flow {
        try {
            emit(Resource.Loading())
            val documents = dataSource.getItemsByWorkspace(workspaceId)
            val items = documents.map { (id, data) ->
                ItemDto.fromMap(id, data).toDomain()
            }
            emit(Resource.Success(items))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }
    
    override suspend fun createItem(workspaceId: String, item: Item): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val itemDto = ItemDto.fromDomain(item)
            val itemId = dataSource.createItem(workspaceId, itemDto.toMap())
            emit(Resource.Success(itemId))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }
    
    // ... otros métodos
}
```

---

## 🎨 FASE 5: ACTUALIZAR CAPA DE PRESENTACIÓN (12-14 horas)

### **1. Crear `WorkspaceSelectionScreen.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/presentation/workspace/WorkspaceSelectionScreen.kt

package com.raymi.app.presentation.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.Workspace

@Composable
fun WorkspaceSelectionScreen(
    viewModel: WorkspaceSelectionViewModel = hiltViewModel(),
    onWorkspaceSelected: (Workspace) -> Unit,
    onCreateWorkspace: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateWorkspace) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo workspace")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                // Mostrar loading
            }
            uiState.workspaces.isEmpty() -> {
                // Mostrar empty state
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(uiState.workspaces) { workspace ->
                        WorkspaceItemCard(
                            workspace = workspace,
                            onClick = { onWorkspaceSelected(workspace) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceItemCard(
    workspace: Workspace,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = workspace.nombre,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = workspace.tipoNegocio,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### **2. Crear `WorkspaceSelectionViewModel.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/presentation/workspace/WorkspaceSelectionViewModel.kt

package com.raymi.app.presentation.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.usecase.workspace.GetWorkspacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceSelectionViewModel @Inject constructor(
    private val getWorkspacesUseCase: GetWorkspacesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkspaceSelectionUiState())
    val uiState: StateFlow<WorkspaceSelectionUiState> = _uiState.asStateFlow()
    
    init {
        loadWorkspaces()
    }
    
    fun loadWorkspaces() {
        viewModelScope.launch {
            // userId del usuario autenticado
            val userId = "USER_ID_ACTUAL"  // TODO: Obtener del AuthRepository
            
            getWorkspacesUseCase(userId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            workspaces = result.data ?: emptyList(),
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }
}

data class WorkspaceSelectionUiState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### **3. Actualizar `AlquileresViewModel.kt` para multi-workspace**

```kotlin
// ANTES
class AlquileresViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase
) : ViewModel()

// DESPUÉS
class AlquileresViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val workspaceManager: WorkspaceManager    // ✅ NUEVO
) : ViewModel() {
    
    fun loadAlquileres() {
        val workspaceId = workspaceManager.getWorkspaceActualId()  // ✅ Obtener workspace
        // Cargar alquileres del workspace específico
    }
}
```

---

## 🔐 FASE 6: FIREBASE RULES (2-3 horas)

### **Actualizar `firestore.rules`** ✅ CRÍTICO

```javascript
// Archivo: firestore.rules

rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // ═══════════════════════════════════════════════════════════════
    // USUARIOS
    // ═══════════════════════════════════════════════════════════════
    match /users/{userId} {
      // Solo el usuario puede leer/escribir su perfil
      allow read, write: if request.auth.uid == userId;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // WORKSPACES
    // ═══════════════════════════════════════════════════════════════
    match /workspaces/{workspaceId} {
      // Permite read/write solo al owner
      allow read, write: if resource.data.ownerId == request.auth.uid;
      
      // Subcollecciones del workspace
      match /{document=**} {
        allow read, write: if get(/databases/{database}/documents/workspaces/{workspaceId}).data.ownerId == request.auth.uid;
      }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ANUNCIOS (públicos)
    // ═══════════════════════════════════════════════════════════════
    match /ads/{document=**} {
      allow read: if true;  // Público
    }
  }
}
```

---

## 📱 FASE 7: INTEGRACIÓN DE ANUNCIOS (6-8 horas)

### **1. Agregar dependencias en `build.gradle.kts`**

```kotlin
// En app/build.gradle.kts
dependencies {
    // ... existing
    
    // Google Mobile Ads
    implementation("com.google.android.gms:play-services-ads:22.6.0")
}
```

### **2. Crear `AdManager.kt`** ✅ NUEVO

```kotlin
// Archivo: app/src/main/java/com/raymi/app/core/ads/AdManager.kt

package com.raymi.app.core.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    
    private var interstitialAd: InterstitialAd? = null
    
    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }
    
    fun loadInterstitialAd(context: Context, adUnitId: String) {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    super.onAdLoaded(ad)
                    interstitialAd = ad
                }
            }
        )
    }
    
    fun showInterstitialAd(activity: android.app.Activity) {
        interstitialAd?.show(activity)
            ?: run {
                // Ad no está listo aún
            }
    }
    
    fun shouldShowAds(userPlan: UserPlan): Boolean {
        return userPlan.tipo == "FREE" && userPlan.mostrarAnuncios
    }
}
```

### **3. Crear Composable para Banner Ads**

```kotlin
// Archivo: app/src/main/java/com/raymi/app/presentation/components/GoogleAdBanner.kt

package com.raymi.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun GoogleAdBanner(adUnitId: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
```

---

## 🚀 FASE 8: SETUP DE PLAY CONSOLE (4-6 horas)

### **1. Crear cuenta Google AdMob**
- Ir a `admob.google.com`
- Crear proyecto
- Obtener `App ID`, `Banner Ad Unit ID`, `Interstitial Ad Unit ID`

### **2. Configurar en `BuildConfig`**

```kotlin
// En app/build.gradle.kts
defaultConfig {
    // ... existing
    
    buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy\"")
    buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
}
```

### **3. Inicializar en `MainActivity`**

```kotlin
// En MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar AdMob
        MobileAds.initialize(this)
        
        // ...
    }
}
```

---

## 📋 CHECKLIST FINAL

### ✅ Modelos (Fase 1)
- [ ] Workspace.kt creado
- [ ] Item.kt creado
- [ ] Categoria.kt creado
- [ ] UserPlan.kt creado

### ✅ Refactoring (Fase 2)
- [ ] Alquiler.kt actualizado (agregar workspaceId)
- [ ] Cliente.kt actualizado (agregar workspaceId)
- [ ] Vestuario.kt → Item.kt migrado

### ✅ Repositories (Fase 3-4)
- [ ] WorkspaceRepository creado
- [ ] ItemRepository creado
- [ ] UserPlanRepository creado
- [ ] Implementaciones en data layer

### ✅ UI (Fase 5)
- [ ] WorkspaceSelectionScreen creado
- [ ] CreateWorkspaceScreen creado
- [ ] ItemsScreen actualizado (antes VestidiosScreen)
- [ ] Navegación actualizada

### ✅ Firestore (Fase 6)
- [ ] firestore.rules actualizado
- [ ] Reglas de seguridad desplegadas

### ✅ Anuncios (Fase 7)
- [ ] Google AdMob account creado
- [ ] AdManager creado
- [ ] Anuncios integrados en pantallas

### ✅ Play Store (Fase 8)
- [ ] Keystore creado
- [ ] APK firmado
- [ ] Play Console configurado
- [ ] Beta testing iniciado

---

## 🎯 Recomendación de Orden de Trabajo

**SEMANA 1:**
1. Crear modelos (Fase 1) → 6 horas
2. Refactorizar existentes (Fase 2) → 8 horas
3. Testing de modelos → 2 horas

**SEMANA 2:**
4. Repositories y data layer (Fase 3-4) → 10 horas
5. Casos de uso (use cases) → 6 horas

**SEMANA 3:**
6. UI nuevas pantallas (Fase 5) → 14 horas
7. Navegación → 2 horas

**SEMANA 4:**
8. Firestore rules (Fase 6) → 3 horas
9. Anuncios (Fase 7) → 8 horas
10. Testing & debugging → 5 horas

**SEMANA 5-6:**
11. Polish & optimización → 10 horas
12. Play Store setup (Fase 8) → 6 horas
13. Beta testing → 4 horas

---

**Total: ~85-100 horas** (distribución realista para 1 desarrollador)

¿Comenzamos por la Fase 1 (Crear Modelos)?

