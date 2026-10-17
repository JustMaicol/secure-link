# Plan · 100 android-base

Spec: `specs/android/100-android-base/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Contexto: `specs/plan-prioridades-android.md`, fases 0 y 1.

La 100 se divide en **dos partes que no comparten archivos**. Cada parte la hace una sola persona y se cierra por separado.

| Parte | Persona | Tareas de la spec | Criterios de la spec | Fase |
|---|---|---|---|---|
| **100-A** · base técnica | **P1** | 1, 2, 3, 4, 7, 8, 9 | 1, 2, 3, 4, 5, 9, 10 | 0 |
| **100-B** · base visual | **P2** | 5, 6, 10, 11, 12 | 6, 7, 8, 11, 12, 13 | 0 y 1 |

## 1. Quién es dueño de qué

| Archivo o carpeta | 100-A (P1) | 100-B (P2) |
|---|---|---|
| `settings.gradle.kts`, `build.gradle.kts` (los 3), `gradle/libs.versions.toml`, `gradle/wrapper/` | ✔ | |
| `app/src/main/AndroidManifest.xml` | ✔ | |
| `core/src/**/common/` | ✔ | |
| `app/src/**/common/` (`SystemClock`, `PrefsDeviceIdentity`) | ✔ | |
| `SecureLinkApp.kt`, `AppContainer.kt` | ✔ | |
| Pruebas de reglas del proyecto | ✔ | |
| `.gitkeep` de las carpetas de todas las specs | ✔ (en el esqueleto) | |
| `MainActivity.kt` | crea una versión mínima en el esqueleto → **pasa a P2** | ✔ desde el hito H0 |
| `res/values/strings.xml` | crea solo `app_name` en el esqueleto → **pasa a P2** | ✔ desde el hito H0 |
| `app/src/**/permissions/` | | ✔ |
| `app/src/**/navigation/` | | ✔ |
| `app/src/**/ui/` (`theme/`, `components/`, `format/`) | | ✔ |
| Pantallas provisionales en `features/*/` | | ✔ (solo el archivo provisional; luego es de la 108 y la 109) |

**Regla:** si una parte necesita cambiar un archivo de la otra, lo pide en su `memory/Pn.md` con `[PIDE a Pn]`. Nunca lo edita.

## 2. Dependencias entre las dos partes

```
A1 esqueleto ──► B1 contrato de permisos ──► A3 AppContainer (con FakePermissionChecker)
     │                                                │
     ├──► A2, A4, A5 (P1 sigue solo)                  │
     └──► B2 … B6 (P2 sigue solo) ── B5 terminado ──► A3' cambia el fake por AndroidPermissionChecker
```

Solo hay tres esperas, y todas cortas:
1. **B espera a A1:** sin proyecto no compila nada. A1 es lo primero que hace P1 y va en su propio PR.
2. **A3 espera a B1:** `AppContainer` necesita la interfaz `PermissionChecker` y su fake.
3. **A3' espera a B5:** el cambio a la implementación real es una línea, a pedido de P2.

## 3. Plan de 100-A (P1)

| Paso | Qué | Archivos | Verificación | PR |
|---|---|---|---|---|
| **A1** · esqueleto | Gradle con `:core` (Kotlin JVM, Java 17) y `:app` (Android, `com.securelink`, `minSdk 34`, `compileSdk` y `targetSdk` = último estable instalado). Catálogo con **todas** las dependencias de la tabla de la 100, sección 3 (Compose, ViewModel, navegación, corrutinas, Room + KSP, ubicación, OkHttp, WorkManager, serialización, pruebas), con versión exacta. `BuildConfig` con `SUPABASE_URL` y `SUPABASE_ANON_KEY` desde `local.properties` (vacíos si faltan). Argumento de KSP `room.schemaLocation`. Propiedad `vectorsDir` en las pruebas de `:core`. Manifiesto con los permisos (100, sección 5.1), `uses-feature` de BLE, `SecureLinkApp` y `MainActivity` **solo en vertical**. `MainActivity` mínima con un texto. `strings.xml` solo con `app_name`. Carpetas de todas las specs con `.gitkeep` (100, sección 2) | raíz de `android/` | `./gradlew build` pasa desde un clon limpio (criterio 1) | **PR 1**, lo antes posible: desbloquea a todos |
| **A2** · contrato `common` | `Clock`, `FakeClock`, `DeviceIdentity`, `FixedDeviceIdentity` y `DEVICE_ID_MAX` (100, sección 4) | `core/.../common/` | pruebas de `FakeClock` (criterio 4) | PR 2 (fase 0.2) |
| **A4** · implementaciones `common` | `SystemClock` (único `System.currentTimeMillis()`). `PrefsDeviceIdentity` con el generador inyectado. Para probarlo en la JVM sin Robolectric, guarda el valor a través de una interfaz mínima (`IdStore`: leer y escribir un texto) con dos implementaciones: `SharedPreferences` y un fake | `app/.../common/` | criterio 5 en la JVM con el fake; una prueba en dispositivo con `SharedPreferences` reales | PR 3 |
| **A5** · reglas del proyecto | Pruebas que fallan si: `:core` importa `android.*` (criterio 2); el catálogo tiene `+`, `latest` o rangos (criterio 3); no existe `vectorsDir` (criterio 9) | pruebas de `:core` | `./gradlew :core:test` | PR 3 |
| **A3** · `AppContainer` de prototipo | `SecureLinkApp` crea un único `AppContainer` (100, sección 7). `clock = SystemClock()`, `deviceIdentity = PrefsDeviceIdentity(...)`, `permissionChecker = FakePermissionChecker(todo concedido)` **provisionalmente**. Los demás contratos (101-107) se cablean a sus fakes a medida que se fusionan | `AppContainer.kt`, `SecureLinkApp.kt` | la app abre en el teléfono | PR 4 (fase 0.3) |
| **A3'** · permisos reales | A pedido de P2 (`[PIDE a P1]`), cambiar el fake por `AndroidPermissionChecker` | `AppContainer.kt` | la pantalla de permisos aparece en una instalación nueva | PR pequeño, fase 1 |
| **A6** · instalación | Instalar el APK de depuración en un Samsung con Android 14 o superior (sin ADB) | — | criterio 10; modelo y versión de One UI en `memory/P1.md` | — |

**Orden:** A1 → A2 → (A4 y A5 en cualquier orden) → A3 (cuando estén B1 y los demás contratos) → A6 → A3' (cuando P2 lo pida).

**Al fijar las versiones (A1):** consultar la última estable de cada biblioteca ese día, fijarla exacta y registrar la lista en `memory/P1.md`. No usar versiones de memoria ni inventadas (regla 3).

**100-A terminada cuando:** pasan los criterios 1, 2, 3, 4, 5, 9 y 10, sus PR están fusionados, y `memory/P1.md` la marca `terminada` con el commit `spec 100: …`.

## 4. Plan de 100-B (P2)

| Paso | Qué | Archivos | Verificación | PR |
|---|---|---|---|---|
| **B1** · contrato de permisos | `Requirement`, `PermissionChecker`, `FakePermissionChecker` y `missingBlocking` (100, sección 5.2) | `app/.../permissions/` | criterio 6 en la JVM | PR 1 de P2 (fase 0.2), justo después de A1 |
| **B2** · tema y formato | Tema Material 3 claro y oscuro, `Spacing` (4, 8, 16, 24 y 32 dp) y colores de tipo de la 003 (100, sección 10). Reglas de formato (100, sección 11): clasificación pura (por ejemplo `TimeAgo.Minutes(5)`) y textos desde `strings.xml` con plurales | `ui/theme/`, `ui/format/` | criterio 11 con todos los bordes de la tabla | PR 2 |
| **B3** · componentes | Los 9 componentes de la 100, sección 10, y `UiTone` con 5 tonos | `ui/components/` | `@Preview` con todos los tonos (criterio 13) | PR 2 o 3 |
| **B4** · navegación | Barra inferior con `sendalert`, `nearby` y `status`, y las pantallas provisionales rotuladas "Pendiente: spec NNN" | `navigation/`, `features/*/` (solo el archivo provisional) | criterio 8 | PR 3 |
| **B5** · pantalla de permisos | `AndroidPermissionChecker`: permisos en tiempo de ejecución, Bluetooth encendido, ubicación del sistema y exclusión de batería. Detección de "negado permanentemente" (ya se pidió y `shouldShowRequestPermissionRationale` es falso). Paso de Samsung con "Ya lo hice" guardado. Botones hacia los ajustes. `MainActivity`: consulta el estado en `onResume` y muestra la pantalla de permisos o la navegación. Si existe el contrato de P1, al pasar la pantalla llama a `MeshServiceStarter.ensureStarted(context)` | `permissions/`, `MainActivity.kt` | criterio 7 (Compose con `FakePermissionChecker`) y criterio 12 (cada estado de fila) | PR 4; después, `[PIDE a P1]` para A3' |
| **B6** · textos | Todos los `id` de la 100, sección 12, y los `common_*` en `strings.xml`. Prueba que compara con la lista | `strings.xml`, prueba | criterio 12 | junto con B5 |

**Orden:** B1 (apenas se fusione A1) → B2 → B3 → B4 → B5 + B6.

**Datos que nunca inventa la 100-B:** textos y colores salen de la spec y del design brief (`specs/design/android-design-brief.md`). Si Figma cambia un texto, se cambia en `strings.xml` y se avisa para actualizar la spec.

**100-B terminada cuando:** pasan los criterios 6, 7, 8, 11, 12 y 13, sus PR están fusionados, P1 hizo A3', y `memory/P2.md` la marca `terminada` con el commit `spec 100: …`.

## 5. Cierre de la spec 100

La 100 queda **terminada** cuando ambas partes lo están. Lo anota quien cierre la última, en su propio archivo de memoria, mencionando a la otra persona.

## 6. Riesgos

| Riesgo | Mitigación |
|---|---|
| A1 tarda y bloquea a todos | PR 1 con lo mínimo que compila; lo demás (A2-A5) va en PR posteriores |
| Conflicto en `MainActivity.kt` o `strings.xml` | traspaso explícito en H0: después, P1 no los vuelve a tocar |
| Una dependencia nueva aparece después | se pide a P1 con `[PIDE a P1]`; nadie edita el catálogo por su cuenta |
| La detección de "negado permanentemente" varía según la versión de Android | B5 la prueba en el Samsung; lo que se descubra se registra como `[HALLAZGO]` |
