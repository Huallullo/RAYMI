# RAYMI - Sistema de Gestión de Alquiler de Vestuarios

## 📱 Descripción

RAYMI es una aplicación móvil Android para la gestión de alquiler de vestuarios folklóricos peruanos. Desarrollada con Jetpack Compose y siguiendo los principios de Clean Architecture.

## 🎯 Características

- ✅ Gestión de Clientes (Agregar, editar, buscar)
- ✅ Gestión de Vestuarios (Catálogo, disponibilidad, estados)
- ✅ Gestión de Alquileres (Crear, seguimiento, devolución)
- ✅ Dashboard con estadísticas en tiempo real
- ✅ Historial de operaciones
- ✅ Autenticación con Firebase
- ✅ Sincronización en la nube con Firestore

## 🏗️ Arquitectura

El proyecto sigue **Clean Architecture** con las siguientes capas:

```
com.raymi.app/
├── core/               # Configuración global
│   ├── di/            # Inyección de dependencias (Hilt)
│   ├── navigation/    # Navegación (Compose Navigation)
│   ├── theme/         # Tema Material 3
│   └── utils/         # Utilidades y extensiones
│
├── data/              # Capa de datos
│   ├── model/dto/     # Data Transfer Objects
│   ├── remote/        # Firebase DataSource
│   └── repository/    # Implementaciones de repositorios
│
├── domain/            # Lógica de negocio
│   ├── model/         # Modelos de dominio
│   ├── repository/    # Interfaces de repositorios
│   └── usecase/       # Casos de uso
│
└── presentation/      # Capa de presentación
    ├── auth/          # Autenticación
    ├── dashboard/     # Panel principal
    ├── clientes/      # Gestión de clientes
    ├── vestuarios/    # Gestión de vestuarios
    ├── alquileres/    # Gestión de alquileres
    ├── historial/     # Historial
    └── components/    # Componentes reutilizables
```

## 🛠️ Tecnologías

- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI declarativa
- **Material 3** - Diseño moderno
- **Firebase**
  - Authentication - Autenticación
  - Firestore - Base de datos en tiempo real
- **Hilt** - Inyección de dependencias
- **Coroutines & Flow** - Programación asíncrona
- **Navigation Compose** - Navegación entre pantallas

## 📦 Dependencias Principales

```kotlin
// Compose
androidx.compose:compose-bom:2024.11.00
androidx.compose.material3:material3

// Firebase
com.google.firebase:firebase-bom:33.7.0
com.google.firebase:firebase-auth
com.google.firebase:firebase-firestore

// Hilt
com.google.dagger:hilt-android:2.52
androidx.hilt:hilt-navigation-compose:1.2.0

// Navigation
androidx.navigation:navigation-compose:2.8.5
```

## 🚀 Instalación

1. **Clonar el repositorio**
```bash
git clone https://github.com/tu-usuario/raymi.git
cd raymi
```

2. **Configurar Firebase**
   - Crear un proyecto en [Firebase Console](https://console.firebase.google.com/)
   - Descargar `google-services.json`
   - Colocarlo en `app/`

3. **Abrir en Android Studio**
   - Android Studio Iguana o superior
   - Gradle 8.13
   - JDK 17+

4. **Compilar y ejecutar**
```bash
./gradlew assembleDebug
```

## 🔥 Configuración de Firebase

### Firestore - Colecciones

```
raymi-db/
├── clientes/
│   └── {clienteId}
│       ├── dni: String
│       ├── nombre: String
│       ├── apellidos: String
│       ├── telefono: String
│       ├── email: String
│       ├── direccion: String
│       └── createdAt: Timestamp
│
├── vestuarios/
│   └── {vestuarioId}
│       ├── codigo: String
│       ├── danza: String
│       ├── departamento: String
│       ├── descripcion: String
│       ├── talla: String
│       ├── precio: Double
│       ├── estado: String (DISPONIBLE, ALQUILADO, MANTENIMIENTO, NO_DISPONIBLE)
│       ├── imagenUrl: String
│       └── createdAt: Timestamp
│
└── alquileres/
    └── {alquilerId}
        ├── clienteId: String
        ├── clienteNombre: String
        ├── vestuarioId: String
        ├── vestuarioNombre: String
        ├── vestuarioCodigo: String
        ├── fechaInicio: Timestamp
        ├── fechaFinPrevista: Timestamp
        ├── fechaDevolucion: Timestamp?
        ├── precioTotal: Double
        ├── adelanto: Double
        ├── saldo: Double
        ├── estado: String (ACTIVO, DEVUELTO, VENCIDO, CANCELADO)
        ├── observaciones: String
        ├── createdAt: Timestamp
        └── updatedAt: Timestamp
```

### Reglas de Seguridad (Firestore)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Solo usuarios autenticados
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 🎨 Diseño y Tema

- **Colores**: Inspirados en la cultura Inca
  - Primary: Oro Inca (#FFD700)
  - Secondary: Rojo Inca (#D62828)
  - Terracota, Azul Andino, Púrpura Real
- **Tipografía**: Sistema Material 3
- **Formas**: Bordes redondeados personalizados

## 📱 Pantallas

1. **Login** - Autenticación de usuarios
2. **Dashboard** - Estadísticas y accesos rápidos
3. **Clientes** - Lista y gestión de clientes
4. **Vestuarios** - Catálogo de vestuarios
5. **Alquileres** - Gestión de alquileres activos
6. **Historial** - Registro de operaciones

## 🧪 Testing

```bash
# Tests unitarios
./gradlew test

# Tests instrumentados
./gradlew connectedAndroidTest
```

## 📝 Próximas Características

- [ ] Reportes PDF
- [ ] Notificaciones push para alquileres vencidos
- [ ] Subida de imágenes de vestuarios
- [ ] Sistema de pagos
- [ ] Modo offline completo
- [ ] Exportación de datos a Excel

## 👥 Contribución

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver `LICENSE` para más información.

## 📞 Contacto

Proyecto RAYMI - Sistema de Gestión de Alquiler de Vestuarios

---

**Hecho con ❤️ en Perú 🇵🇪**
