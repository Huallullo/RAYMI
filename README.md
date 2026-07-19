# RAYMI - Sistema SaaS de Gestión de Alquileres

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)
![Hilt](https://img.shields.io/badge/Hilt-2.52-blue.svg)
![Compose](https://img.shields.io/badge/Compose-2024.11.00-green.svg)
![Firebase](https://img.shields.io/badge/Firebase-33.7.0-orange.svg)

## 📱 Descripción General

**RAYMI** es una plataforma SaaS (Software as a Service) de alto rendimiento diseñada para la gestión centralizada de negocios de alquiler (vestuarios, equipos, vehículos, herramientas). Esta versión 2.5 ha sido optimizada por un **Analista Senior** para ofrecer máxima eficiencia de costos en la nube, seguridad bancaria y una experiencia bilingüe impecable.

## 🎯 Características Principales (Actualizado v2.5)

### 🏗️ Arquitectura SaaS & Multi-tenancy
- **Gestión Multi-Negocio**: Los usuarios PRO pueden administrar infinitas sucursales con aislamiento total de datos.
- **Aislamiento Garantizado**: Implementación de `SmartCache` segmentado por negocio; el historial y los inventarios nunca se mezclan entre locales de un mismo usuario.
- **Optimización de Costos**: Reducción del 90% en lecturas de Firestore mediante Snapshot queries, paginación real (20 items/página) y documentos de estadísticas pre-calculadas.

### 🌎 Internacionalización (i18n) & Localización
- **Bilingüe Nativo**: Soporte completo para **Español (PE)** e **Inglés (US)**.
- **Finanzas Regionales**: Soporte para monedas de toda América (PEN, USD, MXN, COP, etc.) mediante selectores inteligentes.
- **Validación RENIEC**: Auto-completado de clientes mediante DNI con soporte de 3 servidores en cascada para máxima disponibilidad.

### 🛡️ Blindaje de Seguridad
- **Respaldo de Identidad**: Opción para capturar foto de DNI (Frontal/Posterior) y rostro del cliente al momento del registro.
- **Protección contra Bots**: Implementación de reCAPTCHA profesional bilingüe con desafíos matemáticos dictados por voz (TTS).
- **Control de Acceso**: Reglas de Firebase reforzadas y tokens de API protegidos fuera del código fuente.

### 📈 Operaciones & Branding
- **Ticket VIP de WhatsApp**: Generación automática de comprobantes estéticos con negritas, emojis, resumen económico y link de ubicación.
- **Geolocalización Automática**: El negocio puede capturar su ubicación exacta vía GPS con un solo toque para integrarla en los tickets de los clientes.
- **Inventario Flexible**: Control de stock atómico que evita el sobre-alquiler de productos únicos.

## 🛠️ Stack Tecnológico

| Componente | Tecnología |
|------------|------------|
| **Lenguaje** | Kotlin 2.0.21 (Senior Patterns) |
| **UI Framework** | Jetpack Compose (Material 3 - Light Mode Forced) |
| **Inyección de Dependencias** | Dagger Hilt 2.52 |
| **Cloud Backend** | Firebase Firestore & Storage (WebP Optimized) |
| **Seguridad** | reCAPTCHA (Custom) & Text-to-Speech Accessibility |
| **Ubicación** | Google Play Services Location |

## 🏗️ Estructura del Proyecto

```
com.raymi.app/
├── core/                # Arquitectura Base
│   ├── ads/            # Monetización controlada
│   ├── lang/           # Motor de traducción (ES/EN)
│   ├── theme/          # UI System (Forced Light Mode)
│   └── workspace/      # Gestor de multi-tenancy persistente
├── data/                # Implementación de datos
│   ├── remote/         # Storage, Firestore (Paged), Reniec (Cascade)
│   ├── repository/     # Repositorios con SmartCache segmentado
├── domain/              # Lógica Pura
└── presentation/        # Pantallas (Compose)
```

## 💎 Modelos de Suscripción

### Plan FREE
- ✅ 1 Negocio (Workspace)
- ✅ 30 Productos / 40 Clientes
- ✅ Tickets locales ilimitados
- ⚠️ Publicidad moderada

### Plan PRO (S/. 20.00 / $5.40 USD)
- 🔥 **Negocios Ilimitados** (Ideal para sucursales)
- 🔥 **Capacidad Ilimitada** de Inventario/Clientes
- 🔥 **Facturación SUNAT** (vía Nubefact/ApiPeru)
- 🔥 **Sin Publicidad** & Soporte VIP

---
**Desarrollado con patrones de arquitectura limpia para el emprendedor moderno.**
```
