# RAYMI - Sistema SaaS de Gestión de Alquileres (v2.0)

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)
![Hilt](https://img.shields.io/badge/Hilt-2.52-blue.svg)
![Compose](https://img.shields.io/badge/Compose-2024.11.00-green.svg)
![Firebase](https://img.shields.io/badge/Firebase-33.7.0-orange.svg)

## 📱 Descripción General

**RAYMI** es una plataforma SaaS (Software as a Service) de alto rendimiento diseñada para la gestión centralizada de negocios de alquiler (vestuarios, equipos, vehículos, herramientas). Desarrollada bajo los estándares más modernos de Android, ofrece una arquitectura **Multi-tenancy** (múltiples negocios por usuario) y un sistema bilingüe dinámico.

## 🎯 Características Principales

### 🏗️ Arquitectura SaaS (Multi-Workspace)
- **Gestión Multi-Negocio**: Los usuarios PRO pueden administrar infinitas sucursales o locales desde una sola cuenta.
- **Aislamiento de Datos**: Cada negocio (Workspace) posee su propio inventario, clientes, finanzas y configuración de marca.
- **Sincronización en la Nube**: Basado en Firebase Firestore con reglas de seguridad granulares.

### 🌎 Internacionalización (i18n)
- **Bilingüe Nativo**: Soporte completo para **Español (PE)** e **Inglés (US)**.
- **Cambio en Tiempo Real**: Selector de idioma en Login y persistencia de preferencias por negocio.
- **Validaciones Localizadas**: Errores técnicos y mensajes de éxito traducidos automáticamente.

### 📦 Inventario Inteligente y Flexible
- **Campos Dinámicos**: Define atributos personalizados según el rubro (Talla, Color, Marca, Serial).
- **Control de Stock Atómico**: Gestión de disponibilidad mediante transacciones de Firestore para evitar sobre-alquileres.
- **SKU & QR**: Generación automática de códigos y escáner integrado para búsqueda rápida.

### 👥 Clientes y Validación
- **Consulta RENIEC**: Integración con API de identidad para auto-completado de nombres mediante DNI.
- **Ficha Maestra**: Historial de operaciones, saldos pendientes y contacto directo vía WhatsApp.

### 💰 Finanzas y Facturación
- **Ciclo de Pagos**: Soporte para adelantos, garantías reembolsables y saldos pendientes.
- **PDF Profesional**: Generación de Tickets y Facturas con iText7, incluyendo códigos QR de validación y logotipos personalizados.
- **Reportes Contables**: Exportación de movimientos a formato CSV.

## 🛠️ Stack Tecnológico

| Componente | Tecnología |
|------------|------------|
| **Lenguaje** | Kotlin 2.0.21 (Strongly Typed) |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Inyección de Dependencias** | Dagger Hilt 2.52 |
| **Base de Datos** | Firebase Firestore (Real-time) |
| **Autenticación** | Firebase Auth |
| **Almacenamiento** | Firebase Storage (Logos e Imágenes) |
| **Programación Asíncrona** | Coroutines & Flow |
| **Localización** | Custom `RaymiStrings` Interface |
| **Generación de PDF** | iText7 for Android |

## 🏗️ Estructura del Proyecto

```
com.raymi.app/
├── core/                # Configuración transversal
│   ├── ads/            # Gestión de AdMob (Banners e Interstitials)
│   ├── di/             # Módulos de Hilt (Singletons)
│   ├── lang/           # Motor de traducción (Spanish/English)
│   ├── theme/          # Sistema de diseño Material 3
│   └── workspace/      # Gestor de sesión y local activo
├── data/                # Implementación de datos
│   ├── remote/         # DataSources (Firebase, Reniec API, Nubefact)
│   ├── repository/     # Implementaciones de repositorios
│   └── model/dto/      # Objetos de transferencia de datos
├── domain/              # Lógica de negocio pura
│   ├── model/          # Modelos de dominio
│   ├── repository/     # Interfaces de repositorios
│   └── usecase/        # Casos de uso atómicos
└── presentation/        # Capa de UI (Compose)
    ├── auth/           # Login bilingüe y registro SaaS
    ├── workspace/      # Selector de negocios
    ├── dashboard/      # Estadísticas y métricas
    ├── alquileres/     # Ciclo de vida de contratos
    └── profile/        # Gestión de cuenta y Manual de Usuario
```

## 💎 Modelos de Suscripción

### Plan FREE
- ✅ Hasta 1 Negocio (Workspace)
- ✅ 30 Productos / Ítems
- ✅ 40 Clientes registrados
- ✅ Tickets locales ilimitados
- ⚠️ Incluye Anuncios de Google

### Plan PRO (S/. 20.00 / $5.40 USD)
- 🔥 **Negocios Ilimitados**
- 🔥 **Productos y Clientes Ilimitados**
- 🔥 **Facturación Electrónica** (SUNAT via Nubefact/ApiPeru)
- 🔥 **Sin Publicidad**
- 🔥 **Reportes PDF Avanzados**
- 🔥 **Soporte Prioritario**

## 🚀 Instalación y Desarrollo

1. **Requisitos**: Android Studio Ladybug+, JDK 17, Gradle 8.13.
2. **Configuración**:
   - Descargar `google-services.json` de Firebase.
   - Configurar variables de entorno para APIs (Reniec, Nubefact) en `local.properties`.
3. **Compilación**:
   ```bash
   ./gradlew assembleDebug
   ```

## 📄 Licencia

Propiedad exclusiva de Abel Huallullo Matos. Prohibida la redistribución sin autorización.

---
**Desarrollado con ❤️ para impulsar el emprendimiento peruano.**
```