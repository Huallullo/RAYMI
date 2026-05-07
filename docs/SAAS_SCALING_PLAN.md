# RAYMI - Plan SaaS escalable e híbrido

Este documento define el camino recomendado para convertir RAYMI de una app orientada a un negocio de vestuarios en una plataforma SaaS para negocios de alquiler, cuidando costos, aislamiento de datos, monetización e internacionalización.

## Decisión de producto

RAYMI debe evolucionar en dos capas:

1. **Producto base gratuito** para captar usuarios y validar negocios de alquiler.
2. **Planes premium** para monetizar funciones avanzadas como multiusuario, reportes, respaldo avanzado, sincronización entre dispositivos, exportaciones y límites superiores.

## Objetivos de arquitectura

- Cada negocio debe tener su propia data aislada.
- Cada usuario debe pertenecer a un negocio y tener un rol.
- La app debe funcionar con cache/base local para reducir lecturas remotas.
- Firebase debe usarse como nube/sincronización, no como única fuente de lectura constante.
- Las reglas de seguridad deben impedir que un negocio lea la información de otro.
- El producto debe poder internacionalizarse inicialmente en Español e Inglés.

## Modelo Firestore recomendado

```text
usuarios/{uid}
  email
  nombre
  negocioId
  rol
  idioma
  createdAt
  updatedAt

negocios/{negocioId}
  nombre
  rubro
  pais
  moneda
  plan
  ownerUid
  createdAt
  updatedAt

negocios/{negocioId}/miembros/{uid}
  email
  nombre
  rol
  estado
  createdAt

negocios/{negocioId}/clientes/{clienteId}
negocios/{negocioId}/items/{itemId}
negocios/{negocioId}/alquileres/{alquilerId}
negocios/{negocioId}/historial/{historialId}
negocios/{negocioId}/configuracion/general
```

### Nombres de dominio

Para crecer más allá de vestuarios, `vestuarios` debe migrar gradualmente a `items` o `activos`.

| Actual | SaaS recomendado |
| --- | --- |
| Vestuario | Item / Activo |
| Danza | Categoría |
| Talla | Atributo |
| Departamento | Atributo / origen |
| Alquiler | Operación de alquiler |
| Historial | Historial por negocio |

## Reglas Firestore recomendadas para SaaS

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isSignedIn() {
      return request.auth != null;
    }

    function isBusinessMember(negocioId) {
      return isSignedIn()
        && exists(/databases/$(database)/documents/negocios/$(negocioId)/miembros/$(request.auth.uid));
    }

    match /usuarios/{uid} {
      allow read, write: if isSignedIn() && request.auth.uid == uid;
    }

    match /negocios/{negocioId}/{document=**} {
      allow read, write: if isBusinessMember(negocioId);
    }
  }
}
```

> Nota: estas reglas son una guía de arquitectura. Deben ajustarse cuando se implemente el modelo real de `negocios`, `miembros` y roles.

## Estrategia híbrida: Room + Firebase

Para reducir costos y mejorar experiencia offline, RAYMI debe evolucionar a una arquitectura híbrida:

```text
UI / ViewModel
  -> Repositorio
    -> Room local (lectura principal)
    -> SyncService / WorkManager
      -> Firebase Firestore (nube y respaldo)
```

### Comportamiento esperado

1. La app lee primero desde Room.
2. Los cambios se guardan localmente con estado `pendingSync`.
3. WorkManager sincroniza con Firebase cuando hay internet.
4. Firestore se usa para respaldo, multi-dispositivo y restauración.
5. El dashboard usa agregados guardados para evitar leer toda la colección.

### Campos de sincronización recomendados

```text
id
negocioId
createdAt
updatedAt
deletedAt
syncStatus: SYNCED | PENDING_CREATE | PENDING_UPDATE | PENDING_DELETE | ERROR
lastSyncedAt
version
```

## Control de costos Firebase

Acciones prioritarias:

- Evitar listeners globales para colecciones grandes.
- Usar paginación real por `negocioId`.
- Guardar resúmenes del dashboard en documentos agregados.
- Leer solo cambios desde `updatedAt` cuando sea posible.
- Activar presupuestos y alertas si se usa Blaze.
- Medir lecturas, escrituras y almacenamiento por negocio.

## Monetización recomendada

### Plan gratuito

- 1 negocio.
- 1 usuario administrador.
- Límite bajo de items/clientes/alquileres.
- Reportes básicos.
- Marca RAYMI visible.

### Plan Pro mensual

- Más clientes/items/alquileres.
- Multiusuario.
- Reportes PDF avanzados.
- Backup/sync multi-dispositivo.
- Soporte prioritario.
- Exportación de datos.

### Plan Negocio / Premium

- Sucursales.
- Roles avanzados.
- Auditoría/historial avanzado.
- Personalización de moneda/país/idioma.
- Soporte y onboarding.

## Google Play Billing

Si se vende acceso premium dentro de la app como suscripción digital, debe evaluarse Google Play Billing. Para empezar, se recomienda mantener la app gratuita y monetizar luego con planes premium cuando la arquitectura multi-negocio esté lista.

## Internacionalización

Primera etapa:

- Español por defecto.
- Inglés como segundo idioma.
- Mover textos hardcodeados a `strings.xml`.
- Crear `values-en/strings.xml`.
- Guardar preferencia de idioma por usuario/negocio.

## Roadmap recomendado

### Fase 1: Seguridad y base SaaS

- [ ] Eliminar secretos del repositorio (`keystore.properties`).
- [ ] Confirmar reglas Firestore publicadas: no usar `allow read, write: if true`.
- [x] Crear documentos `usuarios/{uid}` al registrarse.
- [x] Crear `negocios/{negocioId}` al crear cuenta.
- [x] Asociar cada usuario a un `negocioId`.

### Fase 2: Multi-negocio

- [ ] Migrar `clientes` a `negocios/{negocioId}/clientes`.
- [ ] Migrar `vestuarios` a `negocios/{negocioId}/items`.
- [ ] Migrar `alquileres` a `negocios/{negocioId}/alquileres`.
- [ ] Migrar índices únicos por negocio.
- [ ] Actualizar reglas Firestore por membresía.

### Fase 3: Híbrido/offline

- [ ] Agregar Room.
- [ ] Crear entidades locales para clientes, items, alquileres e historial.
- [ ] Implementar cola de sincronización.
- [ ] Implementar restauración desde Firebase.
- [ ] Reducir listeners remotos a sincronización controlada.

### Fase 4: Producto internacional y monetizable

- [ ] Traducir UI a Español/Inglés.
- [ ] Definir límites por plan.
- [ ] Implementar pantalla de planes.
- [ ] Evaluar Google Play Billing para suscripciones.
- [ ] Agregar métricas de uso por negocio.