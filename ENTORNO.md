# ENTORNO.md

Preparación de cada persona al clonar el repositorio. Se hace **una vez**, antes de la primera task.

## 1. Clonar y elegir tu rama

```bash
git clone https://github.com/JustMaicol/secure-link.git
cd secure-link
git switch pN          # p1, p2 o p3 (si no existe en el remoto: git switch -c pN)
git pull origin main
```

## 2. Crear `WHOAMI.md` (obligatorio, regla 32)

En la raíz del repositorio. **No se sube a git** (está en `.gitignore`). Es lo que el agente de IA lee para saber qué te toca: sin este archivo, no actúa. Copia el bloque de **tu** persona:

**P1**
```
Soy P1; escribo solo en memory/P1.md.
Specs asignadas (fase prioridad Android): 100 parte A, 101, 103, 107.
Carpetas: ARCHITECTURE.md, sección 6.1. No toco specs, carpetas ni memoria de P2 o P3.
```

**P2**
```
Soy P2; escribo solo en memory/P2.md.
Specs asignadas (fase prioridad Android): 100 parte B, 108, 109, 106 y los vectores de prueba.
Carpetas: ARCHITECTURE.md, sección 6.1. No toco specs, carpetas ni memoria de P1 o P3.
```

**P3**
```
Soy P3; escribo solo en memory/P3.md.
Specs asignadas (fase prioridad Android): 104, 105, 102; después, web (200-205).
Carpetas: ARCHITECTURE.md, sección 6.1. No toco specs, carpetas ni memoria de P1 o P2.
```

Cuando cambie el reparto (por ejemplo, al terminar la fase Android), se actualiza `WHOAMI.md` según `ARCHITECTURE.md`.

## 3. Leer antes de empezar

1. `AGENTS.md`, `CONSTITUTION.md` y `ARCHITECTURE.md` (secciones 2 y 6).
2. `specs/plan-prioridades-android.md`: fases, hitos y contratos.
3. `MEMORY.md` y los tres `memory/Pn.md`.
4. Tu primera task:

| Persona | Primera task | Archivo |
|---|---|---|
| P1 | A1, esqueleto que compila (**desbloquea a todos**) | `specs/android/100-android-base/task.md` |
| P2 | B1, contrato de permisos (cuando se fusione A1); mientras tanto, los vectores | `specs/android/100-android-base/task.md` |
| P3 | T0, prototipo BLE de 1 hora (no espera a nadie) | `specs/android/104-android-transporte-ble/task.md` |

## 4. Instalar Android Studio y el SDK

1. **Android Studio**, la última versión estable, desde la página oficial de Android Developers. Trae su propio JDK: no hace falta instalar Java aparte.
2. En **Settings → Languages & Frameworks → Android SDK**:
   - **SDK Platforms:** la plataforma del `compileSdk` que fije P1 en A1. Al abrir el proyecto, Android Studio ofrece instalar la que falte.
   - **SDK Tools:** *Android SDK Build-Tools*, *Android SDK Platform-Tools* (`adb`) y *Android SDK Command-line Tools*. *Android Emulator* es opcional (ver la sección 6).
3. En **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**: el JDK incluido (versión 17 o superior).
4. Abrir la carpeta **`android/`** del repositorio, no la raíz, y esperar la sincronización. El wrapper descarga Gradle, y Gradle descarga las bibliotecas con las **versiones exactas** de `gradle/libs.versions.toml`. El primer build necesita internet.
5. `android/local.properties` lo crea Android Studio con `sdk.dir`. **Nunca se sube** a git. Las claves de Supabase (`SUPABASE_URL`, `SUPABASE_ANON_KEY`) se agregan ahí recién en la fase 3.

Comandos útiles (desde `android/`; en Windows, `gradlew.bat` en lugar de `./gradlew`):

| Comando | Para qué |
|---|---|
| `./gradlew :core:test` | pruebas de la lógica pura, sin teléfono |
| `./gradlew :app:assembleDebug` | genera `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew build` | compila y prueba todo, salvo lo que corre en el teléfono |
| `./gradlew :app:installDebug` | instala en el teléfono conectado por ADB |
| `./gradlew :app:connectedDebugAndroidTest` | pruebas en el teléfono conectado por ADB |
| `adb devices` / `adb logcat` | ver los teléfonos conectados / ver los registros |

## 5. Preparar los teléfonos Samsung

**Para instalar el APK sin cable (pruebas manuales, prototipo y demo):**
1. Copiar `app-debug.apk` al teléfono (cable, Drive, mensajería…) y abrirlo.
2. Permitir **"Instalar apps desconocidas"** para la app desde la que se abre.
3. **Ajustes → Batería → Límites de uso en segundo plano:** quitar secure-link de **"Aplicaciones en suspensión"**. Si no, Samsung la congela con la pantalla apagada.

**Para depurar (Logcat, `installDebug` y pruebas en el teléfono):**
1. **Ajustes → Acerca del teléfono → Información de software:** tocar 7 veces **"Número de compilación"**.
2. **Ajustes → Opciones de desarrollador:**
   - **Depuración USB.** En Windows puede hacer falta el driver USB de Samsung.
   - o **Depuración inalámbrica** (sin cable): en Android Studio, *Pair Devices Using Wi-Fi* y escanear el código QR. PC y teléfono en la misma red.
3. Comprobar con `adb devices`.

## 6. Qué se prueba dónde

| Prueba | Dónde |
|---|---|
| Lógica pura (`:core`): codec, gossip, distancia, detector de caída… | PC, `./gradlew :core:test` |
| Pantallas y Room | emulador o teléfono con ADB |
| **BLE, brújula, GPS y acelerómetro** | **solo en Samsung reales**: el emulador no tiene Bluetooth real |
| Prototipo visual, prototipo BLE y demo | Samsung con el APK instalado (sin ADB) |

## 7. Ciclo de trabajo diario

```bash
git switch pN
git pull origin main
# … trabajar una task de tu spec …
git add <tus archivos>
git commit -m "spec NNN: qué hiciste"
git push origin pN
# abrir un Pull Request de pN hacia main en GitHub; tras la fusión:
git pull origin main
```

Marcar la casilla en el `task.md` de tu spec y registrar en tu `memory/Pn.md` (`MEMORY.md`, reglas de uso).

## 8. Más adelante (no hace falta ahora)

| Herramienta | Quién | Cuándo |
|---|---|---|
| Node.js (LTS) | P3 (web) y quien genere vectores | fase 3 y después |
| Cuenta de Supabase y su CLI (como dependencia de `web/`, con versión fijada) | P3 | fase 3 (200 mínima) |
| GitHub Pages | P3 | panel web |
