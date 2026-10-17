# ENTORNO.md

Preparación de cada persona al clonar el repositorio. Se hace **una vez**, antes de la primera task. **Los tres instalan el entorno completo de Android**: cada uno compila y prueba su parte antes de abrir un PR (`CONSTITUTION.md`, regla 19).

**[DECISIÓN] No se usa Android Studio.** Se trabaja en **VS Code**, con JDK, SDK de Android por línea de comandos, Gradle (`gradlew`) y `adb` desde la terminal.

## 0. Instrucciones para el agente de IA

Si eres un agente y te piden preparar el entorno, sigue esta guía en orden:

1. **Lee `WHOAMI.md`.** Si no existe, haz primero la sección 2 con la persona. La tabla de la sección 4 dice qué herramientas le tocan.
2. **Verifica antes de instalar.** Ejecuta el comando "Verificar" de cada herramienta y salta las que ya cumplen la versión.
3. **Pide permiso antes de cada instalación**: instala software en el equipo de la persona. Instala solo lo de la sección 4 que le corresponda.
4. **Hay pasos que no puedes hacer tú** (marcados con 👤): aceptar las licencias del SDK, iniciar sesión en cuentas, los ajustes del teléfono y las claves. Explícale a la persona qué hacer y espera a que confirme.
5. **Nunca escribas claves ni contraseñas** en archivos versionados, en el chat ni en `memory/`. La persona las pega en `android/local.properties` o en `web/.env` (regla 25).
6. **Termina con la sección 12** y reporta cada casilla como verificada (con la salida del comando) o no verificada (regla 29).
7. Si encuentras un problema de entorno que afecta a los demás (por ejemplo, una versión que no funciona), regístralo en el `memory/Pn.md` de la persona con `[HALLAZGO]`.

Los comandos son para **Windows (PowerShell)**, el sistema del equipo. En macOS, usa `brew install --cask temurin@17 android-commandlinetools visual-studio-code` y `brew install git node gh`. El SDK queda en `/opt/homebrew/share/android-commandlinetools` y ahí apunta `ANDROID_HOME`.

## 1. Clonar y elegir tu rama

> El proyecto se llama **Senda**, pero el repositorio de GitHub y la carpeta siguen llamándose `secure-link` hasta que se renombren (los comandos de abajo usan ese nombre).

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

## 4. Qué instala cada persona

| Herramienta | P1 | P2 | P3 | Para qué |
|---|---|---|---|---|
| Git | ✅ | ✅ | ✅ | ramas y PRs |
| JDK 17 (Temurin) | ✅ | ✅ | ✅ | ejecutar Gradle y compilar Kotlin |
| SDK de Android por línea de comandos (`sdkmanager`, `platform-tools`, plataforma y build-tools) | ✅ | ✅ | ✅ | compilar, instalar y depurar Android |
| Variables `ANDROID_HOME`, `JAVA_HOME` y `adb` en el `PATH` | ✅ | ✅ | ✅ | usar `gradlew` y `adb` desde la terminal, VS Code y el agente |
| Gradle (solo una vez, para generar el wrapper) | ✅ (A1) | — | — | crear `gradlew`; después nadie usa Gradle instalado |
| Driver USB de Samsung | ✅ | ✅ | ✅ | depurar por cable (opcional si se usa depuración inalámbrica) |
| VS Code + extensiones de la sección 9 | ✅ | ✅ | ✅ | specs, memoria y agente de IA |
| GitHub CLI (`gh`) | recomendado | opcional | opcional | PRs desde la terminal (P1 integra y revisa) |
| Node.js LTS | opcional | opcional | ✅ | web, CLI de Supabase y scripts de vectores |
| Cuenta de Supabase | invitado | — | ✅ (dueño del proyecto) | base de datos (sección 10) |

## 5. Git y GitHub CLI

| Paso | Comando |
|---|---|
| Verificar | `git --version` · `gh --version` |
| Instalar | `winget install --id Git.Git -e` · `winget install --id GitHub.cli -e` |
| Configurar | `git config --global user.name "<nombre>"` · `git config --global user.email "<email de GitHub>"` · `gh auth login` 👤 |

## 6. JDK 17 y SDK de Android (sin Android Studio)

### 6.1 JDK 17

1. **Verificar:** `java -version` muestra 17 y `$env:JAVA_HOME` apunta a esa instalación.
2. **Instalar:** `winget install --id EclipseAdoptium.Temurin.17.JDK -e`. Es la versión de la toolchain de la spec 100: así todos compilan con el mismo Java.

### 6.2 SDK de Android por línea de comandos

El SDK vive en `%LOCALAPPDATA%\Android\Sdk`.

1. **Verificar:** `Test-Path "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat"`.
2. **Descargar las *Command line tools only* para Windows** desde la página oficial de Android Developers (`developer.android.com/studio`, sección *Command line tools only*). El archivo se llama `commandlinetools-win-<número>_latest.zip`. El agente busca en esa página el enlace vigente: no lo inventa.
3. **Descomprimir con la estructura exacta** que exige `sdkmanager`, o no arranca:

   ```powershell
   $sdk = "$env:LOCALAPPDATA\Android\Sdk"
   New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
   Expand-Archive <ruta del zip> -DestinationPath "$sdk\cmdline-tools\_tmp"
   Move-Item "$sdk\cmdline-tools\_tmp\cmdline-tools" "$sdk\cmdline-tools\latest"
   Remove-Item "$sdk\cmdline-tools\_tmp"
   ```

   Resultado: `...\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`.

### 6.3 Variables de entorno

Son variables de usuario. Después hay que **cerrar y reabrir la terminal y VS Code**.

```powershell
$jdk = (Get-ChildItem "$env:ProgramFiles\Eclipse Adoptium" -Directory -Filter 'jdk-17*' | Select-Object -First 1).FullName
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $sdk, 'User')
$p = [Environment]::GetEnvironmentVariable('Path', 'User')
[Environment]::SetEnvironmentVariable('Path', "$p;$jdk\bin;$sdk\platform-tools;$sdk\cmdline-tools\latest\bin", 'User')
```

### 6.4 Paquetes del SDK

En una terminal nueva, con `<N>` = `compileSdk` y `<versión>` = build-tools. **P1 los elige en A1** (con `sdkmanager --list`, la plataforma estable más reciente) y los registra en `memory/P1.md`. Los demás instalan exactamente esos.

```powershell
sdkmanager "platform-tools" "platforms;android-<N>" "build-tools;<versión>"
```

👤 **Licencias:** la persona ejecuta `sdkmanager --licenses` y acepta con `y`. El agente no las acepta en su nombre. Sin las licencias, Gradle no compila.

Sin emulador: las pruebas van en los Samsung (sección 11). El emulador no tiene Bluetooth real.

### 6.5 Solo P1: generar el wrapper de Gradle (A1)

Gradle se instala una sola vez, para crear `gradlew`. Desde ese momento, todos (incluido P1) usan `gradlew`.

1. Descargar `gradle-<versión>-bin.zip` (la versión estable más reciente compatible con el AGP elegido) desde `gradle.org/releases` y descomprimirlo en una carpeta temporal fuera del repositorio.
2. Desde `android/`: `& <carpeta temporal>\gradle-<versión>\bin\gradle.bat wrapper --gradle-version <versión>`.
3. Se versionan `gradlew`, `gradlew.bat` y `gradle/wrapper/` (incluido `gradle-wrapper.jar`). La versión se registra en `memory/P1.md`.

## 7. Trabajar el proyecto desde VS Code

1. Abrir la **raíz del repositorio** en VS Code: así se ven las specs, la memoria y el código juntos.
2. Todo se ejecuta en la terminal de VS Code, desde `android/`. El primer build descarga Gradle y las bibliotecas con las **versiones exactas** de `gradle/libs.versions.toml`, y necesita internet.
3. **No hace falta `sdk.dir`:** Gradle encuentra el SDK por `ANDROID_HOME`. `android/local.properties` se crea a mano recién en la fase 3, para las claves de Supabase (sección 10). **Nunca se sube** a git.
4. Usar **siempre** `gradlew`, nunca un Gradle instalado. Así todos usan la misma versión.

**Lo que se pierde sin Android Studio, y cómo se cubre:**

| Sin Android Studio no hay… | Se cubre con… |
|---|---|
| Vistas previas de Compose (`@Preview`) | la **galería de frames** del prototipo (`app/src/debug/**/prototype`), instalada en el teléfono con `installDebug` |
| Logcat gráfico | `adb logcat` filtrado por la app (tabla de abajo) |
| Depurador con puntos de interrupción en el teléfono | pruebas automáticas (la lógica está en `:core`, sin Android) y registros con `Log` |
| Inspector de la base Room | `adb shell run-as com.senda ls databases/` y pruebas de la 102 |
| Emparejar la depuración inalámbrica con QR | `adb pair` y `adb connect` (sección 11) |

Comandos útiles (desde `android/`; en PowerShell, `.\gradlew.bat` en lugar de `./gradlew`):

| Comando | Para qué |
|---|---|
| `./gradlew :core:test` | pruebas de la lógica pura, sin teléfono |
| `./gradlew :app:assembleDebug` | genera `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew build` | compila y prueba todo, salvo lo que corre en el teléfono |
| `./gradlew :app:installDebug` | instala en el teléfono conectado por ADB |
| `./gradlew :app:connectedDebugAndroidTest` | pruebas en el teléfono conectado por ADB |
| `./gradlew tasks` | lista de tareas disponibles |
| `adb devices` | ver los teléfonos conectados |
| `adb logcat --pid=$(adb shell pidof -s com.senda)` | registros solo de la app (con la app abierta) |
| `adb logcat -c` | limpiar los registros antes de una prueba |

Antes de A1 (mientras no exista `android/`), basta con verificar las secciones 5 y 6.

## 8. Node.js (P3; opcional para los demás)

| Paso | Comando |
|---|---|
| Verificar | `node --version` · `npm --version` |
| Instalar | `winget install --id OpenJS.NodeJS.LTS -e` |

La CLI de Supabase **no se instala global**: es dependencia de desarrollo de `web/` con versión fijada (spec 200) y se usa con `npx supabase ...`.

## 9. VS Code y extensiones

**VS Code es el único editor**: Kotlin, Gradle, specs, memoria, agente de IA y web.

- **Verificar:** `code --version`.
- **Instalar:** `winget install --id Microsoft.VisualStudioCode -e`.
- `.vscode/` está en `.gitignore`, así que las extensiones se instalan a mano con `code --install-extension <id>`.

| Extensión | ID | Quién | Para qué |
|---|---|---|---|
| Claude Code | `anthropic.claude-code` | todos | agente de IA dentro del editor |
| Markdown All in One | `yzhang.markdown-all-in-one` | todos | specs, memoria, tablas y vista previa |
| GitHub Pull Requests | `github.vscode-pull-request-github` | todos | revisar y crear PRs |
| GitLens | `eamodio.gitlens` | todos (opcional) | historial y autoría por línea |
| Error Lens | `usernamehw.errorlens` | todos (opcional) | errores en la misma línea |
| Kotlin | `fwcd.kotlin` | P1, P2, P3 | resaltado, autocompletado y navegación de Kotlin. Su soporte de proyectos Android es limitado: **la verdad la da `gradlew`**, no el editor |
| Gradle for Java | `vscjava.vscode-gradle` | P1, P2, P3 | ver y ejecutar las tareas de Gradle desde la barra lateral |
| XML | `redhat.vscode-xml` | P1, P2 | `AndroidManifest.xml`, `strings.xml` y recursos |
| ESLint | `dbaeumer.vscode-eslint` | P3 | web |
| Prettier | `esbenp.prettier-vscode` | P3 | formato de la web |
| Vitest | `vitest.explorer` | P3 | pruebas de la web |
| DotENV | `mikestead.dotenv` | P3 | `web/.env` |
| SQLTools + driver de Postgres | `mtxr.sqltools`, `mtxr.sqltools-driver-pg` | P3 | consultar la base (sección 10) |

Instalación de las comunes y las de Android (P1, P2 y P3), en una sola línea:

```powershell
"anthropic.claude-code","yzhang.markdown-all-in-one","github.vscode-pull-request-github","eamodio.gitlens","usernamehw.errorlens","fwcd.kotlin","vscjava.vscode-gradle","redhat.vscode-xml" | ForEach-Object { code --install-extension $_ }
```

Si el autocompletado de Kotlin falla o tarda, no bloquea: se compila con `gradlew` y se corrigen los errores que reporta.

## 10. Supabase (dueño: P3)

**P3 crea y conecta la base** (specs 002 y 200; la 200 mínima, en la fase 3). Los demás solo reciben la URL y la clave `anon`.

1. 👤 P3 crea el proyecto en supabase.com (plan gratuito, región cercana a la demo) e **invita a P1** como miembro del proyecto, para que el integrador no dependa de una sola cuenta.
2. 👤 P3 guarda la contraseña de la base en su gestor de contraseñas. No va al repositorio ni al chat del equipo.
3. Migraciones, desde `web/`: `npx supabase login` 👤, `npx supabase link --project-ref <ref>` 👤 y `npx supabase db push`.
4. Para SQLTools o pgAdmin se usa la cadena **Session pooler** (funciona por IPv4). La *Direct connection* solo funciona por IPv6 en el plan gratuito. El *Transaction pooler* es para funciones serverless y no se usa.
5. P3 comparte con el equipo, por un canal privado, solo la **URL del proyecto** y la clave **`anon`** (Project Settings → API). Cada persona las pega 👤 en su archivo ignorado por git:

   ```properties
   # android/local.properties
   SUPABASE_URL=https://<ref>.supabase.co
   SUPABASE_ANON_KEY=<clave anon>
   ```

   Para la web: `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY` en `web/.env`.
6. **La clave `service_role` y la cadena de conexión nunca salen del panel de Supabase** ni de la máquina de P3: dan acceso total a la base y saltan el RLS.

La app y el panel **no** usan cadenas de conexión de Postgres: hablan con la API REST de Supabase (spec 002, secciones 6 y 7).

## 11. Preparar los teléfonos Samsung

**Para instalar el APK sin cable (pruebas manuales, prototipo y demo):**
1. Copiar `app-debug.apk` al teléfono (cable, Drive, mensajería…) y abrirlo.
2. Permitir **"Instalar apps desconocidas"** para la app desde la que se abre.
3. **Ajustes → Batería → Límites de uso en segundo plano:** quitar Senda de **"Aplicaciones en suspensión"**. Si no, Samsung la congela con la pantalla apagada.

**Para depurar (Logcat, `installDebug` y pruebas en el teléfono):**
1. **Ajustes → Acerca del teléfono → Información de software:** tocar 7 veces **"Número de compilación"**.
2. **Ajustes → Opciones de desarrollador:**
   - **Depuración USB.** En Windows hace falta el driver USB de Samsung (*Samsung Android USB Driver*, en la web de desarrolladores de Samsung) 👤.
   - o **Depuración inalámbrica** (sin cable ni driver; PC y teléfono en la misma red): en el teléfono, **Depuración inalámbrica → Vincular dispositivo con código de vinculación**. En la PC, `adb pair <ip>:<puerto de vinculación> <código>` y después `adb connect <ip>:<puerto>` (el puerto que se ve en la pantalla principal de Depuración inalámbrica, distinto del de vinculación).
3. Comprobar con `adb devices`: el teléfono aparece como `device`, no como `unauthorized` (si dice eso, aceptar el aviso en la pantalla del teléfono).

**Qué se prueba dónde:**

| Prueba | Dónde |
|---|---|
| Lógica pura (`:core`): codec, gossip, distancia, detector de caída… | PC, `./gradlew :core:test` |
| Pantallas y Room | teléfono con ADB (no se instala emulador) |
| **BLE, brújula, GPS y acelerómetro** | **solo en Samsung reales**: el emulador no tiene Bluetooth real |
| Prototipo visual, prototipo BLE y demo | Samsung con el APK instalado (sin ADB) |

## 12. Verificación final

El agente ejecuta cada comando en una **terminal nueva** y reporta la salida.

- [ ] `git --version` y `git config user.email` responden.
- [ ] `java -version` muestra 17 y `$env:JAVA_HOME` apunta a ese JDK.
- [ ] `$env:ANDROID_HOME` apunta a `...\Android\Sdk`.
- [ ] `sdkmanager --list_installed` muestra `platform-tools` y, después de A1, la plataforma y las build-tools de `memory/P1.md`.
- [ ] `adb version` responde.
- [ ] `code --version` responde y `code --list-extensions` incluye las extensiones de la sección 9 que le tocan a la persona.
- [ ] Solo P3: `node --version` muestra una versión LTS.
- [ ] Cuando exista `android/` (después de A1): `.\gradlew.bat :core:test` termina con `BUILD SUCCESSFUL`.
- [ ] Con un teléfono conectado: `adb devices` lo muestra como `device`.
- [ ] `git status` no muestra `local.properties`, `.env` ni `WHOAMI.md` (están ignorados).

## 13. Ciclo de trabajo diario

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
