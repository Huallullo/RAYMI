# RAYMI - Checklist de salida a producción

## 1) Firma de release (obligatorio)
- [ ] Crear keystore de producción (`.jks`) y guardarla fuera del repo.
- [ ] Configurar variables seguras en `~/.gradle/gradle.properties`:
    - `RAYMI_UPLOAD_STORE_FILE`
    - `RAYMI_UPLOAD_STORE_PASSWORD`
    - `RAYMI_UPLOAD_KEY_ALIAS`
    - `RAYMI_UPLOAD_KEY_PASSWORD`
- [ ] Compilar bundle firmado:
    - `./gradlew clean :app:bundleRelease`

## 2) Validaciones técnicas
- [ ] `./gradlew :app:lintRelease`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] Verificar que la app inicie, login funcione y dashboard cargue sin crashes.
- [ ] Verificar exportación PDF y compartir por WhatsApp.

## 3) Firebase
- [ ] Confirmar `google-services.json` del proyecto de producción.
- [ ] Confirmar reglas Firestore desplegadas (modelo actual: usuario autenticado).
- [ ] Validar que el usuario final pueda registrarse/iniciar sesión correctamente.

## 4) Play Console
- [ ] Política de privacidad publicada y URL configurada.
- [ ] Formulario de seguridad de datos completado.
- [ ] Clasificación de contenido y público objetivo completos.
- [ ] Subir AAB a pista interna, ejecutar prueba cerrada y revisar Android Vitals.

## 5) Smoke test pre-lanzamiento (mínimo)
- [ ] Alta/edición/búsqueda de cliente.
- [ ] Alta/edición de vestuario.
- [ ] Crear alquiler, visualizar vencidos, devolver alquiler.
- [ ] Generar PDF resumen anual y compartir.
- [ ] Cerrar sesión e iniciar sesión con usuario válido.