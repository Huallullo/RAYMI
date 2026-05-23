# 🚀 TRANSFORMACIÓN A SAAS: RAYMI MULTI-PROPÓSITO

**Objetivo:** Convertir app de "Alquiler de Vestuarios" → "Plataforma de Alquileres Genérica"  
**Modelo de Negocio:** Freemium (Ads en versión gratis, Premium sin ads)  
**Timeline:** 4-6 semanas  
**Complejidad:** Media-Alta

---

## 📋 TABLA DE CONTENIDOS

1. [Cambios Arquitectónicos](#cambios-arquitectónicos)
2. [Modelo de Datos Flexible](#modelo-de-datos-flexible)
3. [Sistema Multi-Workspace](#sistema-multi-workspace)
4. [Monetización](#monetización)
5. [Roadmap Detallado](#roadmap-detallado)
6. [Cambios Codificación](#cambios-codificación)

---

## 🏗️ Cambios Arquitectónicos

### **De Específico → Genérico**

```
ANTES (Específico)
├── Cliente
├── Vestuario (Ropa, danzas)
└── Alquiler (vestuarios)

DESPUÉS (Genérico SaaS)
├── User (propietario del workspace)
├── Workspace (negocio del usuario)
│   ├── Categorías (configurables: Ropa, Equipos, Vehículos, etc.)
│   ├── Items (productos genéricos)
│   ├── Clientes
│   └── Alquileres
└── Sistema de Planes (Free, Pro)
```

### **Conceptos Clave**

| Antes | Después | Flexible |
|-------|---------|----------|
| Vestuario | Item | Sí (qué tipo de cosa se alquila) |
| Danza | Categoría | Sí (usuario define) |
| Código de Vestuario | SKU/Código Item | Sí |
| Talla | Atributo Custom | Sí |
| Precio fijo | Configurable | Sí |

---

## 🗄️ Modelo de Datos Flexible

### **Structure: Firestore (Nueva)**

```
firestore/
├── users/                                  # Quién es (nueva)
│   └── {userId}/
│       ├── email: String
│       ├── nombre: String
│       ├── plan: String (FREE|PRO|ENTERPRISE)
│       ├── suscripcionActiva: Boolean
│       ├── workspaceIds: String[]
│       └── createdAt: Timestamp
│
├── workspaces/                             # El negocio del usuario (nueva)
│   └── {workspaceId}/
│       ├── ownerId: String (User)
│       ├── nombre: String (ej: "Mi Tienda de Alquileres")
│       ├── descripcion: String
│       ├── tipoNegocio: String             # VESTUARIOS|EQUIPOS|VEHICULOS|OTRO
│       ├── activo: Boolean
│       ├── configuracion:
│       │   ├── moneda: String (PEN|USD)
│       │   ├── zonaHoraria: String
│       │   ├── idioma: String
│       │   └── mostrarAnuncios: Boolean
│       └── createdAt: Timestamp
│
├── /workspaces/{workspaceId}/categorias/  # Items agrupados (nueva)
│   └── {categoriaId}/
│       ├── nombre: String (ej: "Trajes Folklóricos")
│       ├── descripcion: String
│       ├── icono: String (URL)
│       ├── activa: Boolean
│       └── orden: Int
│
├── /workspaces/{workspaceId}/items/       # Productos (antes: vestuarios)
│   └── {itemId}/
│       ├── nombre: String
│       ├── codigo: String (ÚNICO)
│       ├── categoriaId: String
│       ├── descripcion: String
│       ├── precio: Double
│       ├── cantidad: Int
│       ├── estado: String (DISPONIBLE|ALQUILADO|MANTENIMIENTO)
│       ├── atributos:                      # Dinámicos (nueva)
│       │   ├── talla: String (si aplica)
│       │   ├── color: String (si aplica)
│       │   ├── marca: String (si aplica)
│       │   └── [custom]: String (usuario define)
│       ├── imagenUrl: String
│       └── createdAt: Timestamp
│
├── /workspaces/{workspaceId}/clientes/    # Clientes (sin cambio mayor)
│   └── {clienteId}/
│       ├── nombre: String
│       ├── email: String
│       ├── telefono: String
│       ├── dni: String (ÚNICO por workspace)
│       ├── direccion: String
│       └── createdAt: Timestamp
│
├── /workspaces/{workspaceId}/alquileres/  # Alquileres (mismo modelo)
│   └── {alquilerId}/
│       ├── clienteId: String
│       ├── itemId: String (antes: vestuarioId)
│       ├── fechaInicio: Timestamp
│       ├── fechaFinPrevista: Timestamp
│       ├── precioTotal: Double
│       ├── adelanto: Double
│       ├── estado: String
│       └── createdAt: Timestamp
│
└── ads/                                    # NUEVA: Datos de anuncios
    └── admobConfig/
        ├── androidAppId: String
        ├── bannerAdUnitId: String
        ├── interstitialAdUnitId: String
        └── rewardedAdUnitId: String
```

### **Cambios en Modelos Kotlin**

#### ANTES:
```kotlin
data class Vestuario(
    val codigo: String,
    val danza: String,
    val departamento: String,
    val talla: String,
    val precio: Double
)
```

#### DESPUÉS:
```kotlin
data class Item(
    val id: String,
    val workspaceId: String,           // ✅ NEW
    val nombre: String,
    val codigo: String,
    val categoriaId: String,           // ✅ NEW (flexible)
    val descripcion: String,
    val precio: Double,
    val cantidad: Int,
    val estado: String,
    val atributos: Map<String, String> // ✅ NEW (dinámicos)
    val imagenUrl: String,
    val createdAt: Timestamp
)

data class Workspace(                   // ✅ NEW
    val id: String,
    val ownerId: String,
    val nombre: String,
    val tipoNegocio: String,           // VESTUARIOS|EQUIPOS|VEHICULOS|OTRO
    val configuracion: ConfiguracionWorkspace
)

data class ConfiguracionWorkspace(      // ✅ NEW
    val moneda: String,
    val zonaHoraria: String,
    val idioma: String,
    val mostrarAnuncios: Boolean        // Mostrar solo si plan = FREE
)

data class PlanUsuario(                 // ✅ NEW
    val tipoplan: String,              // FREE|PRO|ENTERPRISE
    val activo: Boolean,
    val fechaInicio: Timestamp,
    val fechaFin: Timestamp?,
    val anunciosMostrados: Int         // Para limite de ads
)
```

---

## 👥 Sistema Multi-Workspace

### **¿Qué es un Workspace?**
Un "espacio de trabajo" = Un negocio del usuario. Un usuario puede tener múltiples negocios.

### **Ejemplos:**
```
Abel (Usuario)
├── Workspace 1: "Mi Tienda de Vestuarios" (Alquiler de ropa)
├── Workspace 2: "Alquileres de Equipos" (Alquiler de equipos de cine)
└── Workspace 3: "Transportes Lima" (Alquiler de vehículos)
```

### **Flujo de Autenticación Nueva**

```
┌─────────────────────────────────────────────┐
│                                             │
│  Login → Seleccionar Workspace → App        │
│     ↓                  ↓                     │
│   Firebase Auth   Cargar datos del         │
│                  workspace seleccionado    │
│                                            │
└─────────────────────────────────────────────┘
```

---

## 💰 Monetización: Freemium + Ads

### **Modelos de Ingresos**

#### **Opción A: Anuncios (Recomendado para inicio)**
```
┌─────────────────────────────────┐
│ PLAN FREE (Inicio 30 días)      │
├─────────────────────────────────┤
│ ✅ Crear 1 workspace            │
│ ✅ Máx 50 items                 │
│ ✅ Máx 100 alquileres           │
│ ❌ SIN anuncios al inicio       │
│ ⚠️ Anuncios después de 30 días  │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ PLAN PRO ($4.99/mes aprox)      │
├─────────────────────────────────┤
│ ✅ Workspaces ilimitados        │
│ ✅ Items ilimitados             │
│ ✅ Alquileres ilimitados        │
│ ✅ SIN anuncios                 │
│ ✅ Reportes avanzados           │
└─────────────────────────────────┘
```

#### **Opción B: Ads + Premium (Más ingresos)**
```
La app SIEMPRE muestra anuncios:
- Banners al pie de pantallas
- Intersticiales entre operaciones
- Opción "Ver anuncio" para funciones premium

Pagar $4.99/mes = Ocultar anuncios
```

### **Integración Google AdMob**

```kotlin
// En MainActivity
MobileAds.initialize(context)

// En cada pantalla
@Composable
fun PantallaPrincipal(userPlan: UserPlan) {
    Column {
        // Contenido principal
        
        if (userPlan.tipo == "FREE") {
            // ✅ Mostrar banner ad
            GoogleBannerAd(
                adUnitId = BuildConfig.ADMOB_BANNER_ID,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Intersticiales
fun mostrarIntersticial() {
    if (userPlan.tipo == "FREE") {
        loadInterstitialAd().show(activity)
    }
}
```

---

## 🗓️ Roadmap Detallado (4-6 semanas)

### **SEMANA 1: Arquitectura Base**
```
[ ] Crear modelo Workspace
[ ] Crear modelo Item (genérico)
[ ] Mover lógica de Vestuario → Item
[ ] Crear UserPlan model
[ ] Estructura Firestore multiworkspace
[ ] Estimado: 20 horas
```

### **SEMANA 2: Autenticación Multi-workspace**
```
[ ] Pantalla de selección de workspace
[ ] Crear nuevo workspace
[ ] Cambiar entre workspaces
[ ] Lógica de "workspace actual" en repository
[ ] Estimado: 16 horas
```

### **SEMANA 3: Flexible Configuración**
```
[ ] Sistema de categorías dinámicas
[ ] Atributos configurables por item
[ ] Moneda/idioma/zona horaria por workspace
[ ] Estimado: 12 horas
```

### **SEMANA 4: Monetización**
```
[ ] Integrar Google AdMob
[ ] Pantalla de planes/suscripción
[ ] Lógica de mostrar ads según plan
[ ] Testing de ads
[ ] Estimado: 16 horas
```

### **SEMANA 5: Polish + Testing**
```
[ ] Tests unitarios críticos
[ ] Optimización performance
[ ] Traducción i18n
[ ] Estimado: 12 horas
```

### **SEMANA 6: Lanzamiento**
```
[ ] Build release
[ ] Firma y testing en Play Store
[ ] Screenshots y descripción
[ ] Play Console setup
[ ] Publicación en alpha/beta
[ ] Estimado: 8 horas
```

---

## 💻 Cambios de Código Principales

### **1. Estructura de Directorios**

```
presentation/
├── auth/                          (sin cambio)
├── workspace/                     ✅ NEW (seleccionar/crear workspace)
├── items/                         ✅ NUEVO (antes vestuarios)
├── categorias/                    ✅ NEW
├── alquileres/                    (sin cambio mayor)
├── clientes/                      (sin cambio)
├── configuracion/                 ✅ NEW (settings workspace)
└── ads/                           ✅ NEW (manejo de anuncios)

data/
├── remote/
│   └── FirebaseDataSource.kt      (agregar workspaceId)
└── repository/
    ├── ItemRepository.kt          ✅ NEW (antes VestuarioRepository)
    ├── WorkspaceRepository.kt     ✅ NEW
    └── UserPlanRepository.kt      ✅ NEW

domain/
└── model/
    ├── Item.kt                    ✅ NUEVO
    ├── Workspace.kt               ✅ NEW
    ├── UserPlan.kt                ✅ NEW
    └── Categoria.kt               ✅ NEW
```

### **2. DI Hilt: Inyectar Workspace Actual**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object WorkspaceModule {
    
    @Provides
    @Singleton
    fun provideWorkspaceManager(): WorkspaceManager {
        return WorkspaceManager()
    }
}

// En cualquier ViewModel:
@HiltViewModel
class AlquileresViewModel @Inject constructor(
    private val workspaceManager: WorkspaceManager,
    private val repository: AlquilerRepository
) : ViewModel() {
    
    fun loadAlquileres() {
        val workspaceActual = workspaceManager.getWorkspaceActual()
        // Cargar alquileres del workspace
    }
}
```

### **3. Queries de Firestore**

```kotlin
// ANTES:
db.collection("alquileres")
    .orderBy("createdAt")
    .get()

// DESPUÉS:
db.collection("workspaces/${workspaceId}/alquileres")
    .orderBy("createdAt")
    .get()
```

### **4. Reglas Firestore (Muy Importante)**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Usuarios solo leen su perfil
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Workspaces: solo owner puede acceder
    match /workspaces/{workspaceId} {
      allow read, write: if isWorkspaceOwner(workspaceId);
      
      // Subcollecciones (items, alquileres, etc.)
      match /{document=**} {
        allow read, write: if isWorkspaceOwner(workspaceId);
      }
    }
    
    // Ads pueden leerse públicamente
    match /ads/{document=**} {
      allow read: if true;
    }
  }
  
  function isWorkspaceOwner(workspaceId) {
    let workspace = get(/databases/{database}/documents/workspaces/{workspaceId});
    return workspace.data.ownerId == request.auth.uid;
  }
}
```

---

## 📊 Comparación: Antes vs Después

| Aspecto | Antes | Después |
|---------|-------|---------|
| Tipo de app | Single-tenant | Multi-tenant SaaS |
| Casos de uso | Solo vestuarios | Cualquier alquiler |
| Usuarios | 1 negocio | N negocios |
| Modelo ingresos | Suscripción | Free + Premium |
| Monetización | N/A | Anuncios (Free) |
| Complejidad | Media | Alta |
| Código total | ~15K líneas | ~25K líneas |
| Testing | 0% | 30%+ |

---

## 🎯 Decisiones Importantes

### **1. ¿Conservar datos actuales?**
```
OPCIÓN A: Beta fresh start
- Crear workspace de prueba
- Migrar datos antiguos manualmente
- Ventaja: Código limpio

OPCIÓN B: Migración automática
- Script que mueve vestuarios → items
- Más fácil para cliente actual
- Riesgo: Complejidad

RECOMENDACIÓN: OPCIÓN A (simpler)
```

### **2. ¿Qué tipo de negocio lanzar?**
```
RECOMENDACIÓN: Vestuarios (lo que tienes probado)
- Ya sé que funciona
- Puedes agregar más tipos después
- Reduce scope inicial
```

### **3. ¿Anuncios intrusivos o sutiles?**
```
RECOMENDACIÓN: Sutiles al inicio
- Banners en pie de pantalla
- Intersticiales después de operaciones
- NO popups molestos
- Ya que: Mejor retención de usuarios
```

---

## 📋 Checklist de Desarrollo

Fase 1: Arquitectura
- [ ] Crear modelo Workspace
- [ ] Crear modelo Item
- [ ] Refactorizar Firestore structure
- [ ] Tests unitarios de modelos

Fase 2: Lógica
- [ ] WorkspaceRepository
- [ ] ItemRepository
- [ ] UserPlanRepository
- [ ] Lógica de cambio de workspace

Fase 3: UI
- [ ] Pantalla seleccionar workspace
- [ ] Pantalla crear workspace
- [ ] Item list (antes vestuarios)
- [ ] Configuración workspace

Fase 4: Monetización
- [ ] Integración AdMob
- [ ] Pantalla planes
- [ ] Lógica mostrar ads
- [ ] Animación suscripción

Fase 5: Release
- [ ] Build release
- [ ] Firma APK
- [ ] Testing en device
- [ ] Publicar Play Store

---

## 💡 Recomendaciones

1. **Start Small:** Lanza con Vestuarios como único tipo, agrega más después
2. **Free Trial:** Dar 30 días sin anuncios antes de mostrar ads
3. **Analytics:** Tracker cuántos users, qué tipos de negocio eligen
4. **Support:** Chatbot básico o email to help users
5. **Feedback:** In-app survey después de 1 semana

---

**Total de Cambios Necesarios:**
- ~8,000 líneas de código nuevo
- ~3,000 líneas refactorizadas
- ~50 archivos nuevos
- Timeline: 4-6 semanas

¿Empezamos con la arquitectura?

