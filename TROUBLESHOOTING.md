# Troubleshooting: La App No Abre

## Paso 1: Verificar que la compilación es exitosa
En PowerShell:
```powershell
cd C:\Users\user\AndroidStudioProjects\RAYMI2
./gradlew assembleDebug
```
**Resultado esperado:** `BUILD SUCCESSFUL`

## Paso 2: Instalar en el dispositivo
Si la compilación fue exitosa, instala manualmente:

### Opción A: Desde PowerShell
```powershell
./gradlew installDebug
```

### Opción B: Desde Android Studio
1. Click en `Run` → `Run 'app'`
2. Selecciona tu dispositivo Samsung
3. Espera a que termine la instalación

## Paso 3: Depurar en Logcat
Mientras la app intenta abrirse, monitorea Logcat en Android Studio:

1. Abre Android Studio
2. Click en `Logcat` tab (abajo)
3. Filtra por: `RaymiApp` o `WM-WorkerFactory`
4. Busca líneas con:
   - `ERROR`
   - `Exception`
   - `FATAL`

## Paso 4: Problemas Comunes y Soluciones

### Problema: "NoSuchMethodException: CheckOverdueRentalsWorker.<init>"
**Solución:** El Worker se inicializa incorrectamente. Los cambios ya incluyen un try-catch para esto.

### Problema: "Firebase initialization failed"
**Solución:** Asegúrate de que `google-services.json` está en `app/`

### Problema: "Permission denied: ...drawable/ic_raymi_logo"
**Solución:** Verifica que el archivo `app/src/main/res/drawable/ic_raymi_logo.xml` o `.png` existe

### Problema: "ClassNotFoundException"
**Solución:** Limpia la build:
```powershell
./gradlew clean ; ./gradlew assembleDebug
```

## Paso 5: Si Aún No Abre

### 5.1 - Crear build limpio
```powershell
cd C:\Users\user\AndroidStudioProjects\RAYMI2
./gradlew clean
./gradlew assembleDebug
./gradlew installDebug
```

### 5.2 - Desinstalar completamente
```powershell
./gradlew uninstallDebug
```

### 5.3 - Esperar a que el gradle cache se reconstruya
```powershell
./gradlew bundleDebug
```

## Información Crítica
- **Google Services**: Verifica que `google-services.json` está en `app/`
- **Hilt Compilation**: Si hay errores de Hilt, ejecuta:
  ```powershell
  ./gradlew assembleDebug --info
  ```
- **Java Version**: Asegúrate que usas Java 17+ (tal como define `build.gradle.kts`)

## Test Rápido sin la App
Si la App no abre pero quieres probar la consulta RENIEC, ejecuta en Logcat:
```
adb logcat | grep "RENIEC"
```

Esto mostrará las consultas a la API en real-time.

