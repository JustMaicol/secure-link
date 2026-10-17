# Tasks · 100 android-base

Spec: `100-android-base/spec.md` · Plan: `100-android-base/plan.md` (si algo choca, manda la spec).
La 100 tiene **dos partes con dueños distintos**. Cada persona marca **solo sus casillas**.

| Parte | Persona | Tasks |
|---|---|---|
| **A** · base técnica | **P1** | A1-A6 y A3' |
| **B** · base visual | **P2** | B1-B6 |

Al terminar cada task: commit `spec 100: <qué>` y anotar en el propio `memory/Pn.md`. Al terminar la parte completa: marcarla `terminada` (regla 33).

---

## Parte A · P1

### A1 · Esqueleto que compila (fase 0.1, PR 1, **desbloquea a todos**)

- [x] **A1.1 Gradle raíz.** `android/settings.gradle.kts` (módulos `:core` y `:app`), `android/build.gradle.kts` y wrapper de Gradle con versión exacta.
  *Hecho cuando:* `./gradlew help` funciona.
- [x] **A1.2 Catálogo de versiones.** `gradle/libs.versions.toml` con **todas** las dependencias de la 100, sección 3: corrutinas, `kotlinx-serialization-json` + plugin, JUnit 4, Compose BOM, `material3`, `ui`, `ui-tooling-preview`, `activity-compose`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `navigation-compose`, `kotlinx-coroutines-android`, Room (`runtime`, `ktx`, `compiler`, `testing`) + KSP, `play-services-location`, `okhttp`, `work-runtime-ktx`, `androidx.test.ext:junit`, `ui-test-junit4` y `ui-test-manifest`. **Versiones exactas**, consultadas ese día (última estable); sin `+`, `latest` ni rangos.
  *Hecho cuando:* la lista de versiones queda en `memory/P1.md`.
- [x] **A1.3 Módulo `:core`.** Plugin `org.jetbrains.kotlin.jvm`, toolchain de Java 17, `kotlinx-coroutines-core`, y para pruebas JUnit, `kotlinx-coroutines-test` y `kotlinx-serialization-json`. Propiedad de sistema `vectorsDir` = `<raíz del repo>/specs/shared/vectors` en la tarea `test`.
  *Hecho cuando:* `./gradlew :core:test` corre (aunque no haya pruebas).
- [x] **A1.4 Módulo `:app`.** `com.android.application`, `namespace` y `applicationId` = `com.securelink`, `minSdk 34`, `compileSdk` y `targetSdk` = último estable instalado, Compose activado, `buildConfig` con `SUPABASE_URL` y `SUPABASE_ANON_KEY` leídos de `local.properties` (cadena vacía si faltan), KSP con `room.schemaLocation` = `android/app/schemas/`, y dependencia de `:core`.
  *Hecho cuando:* `./gradlew :app:assembleDebug` genera el APK.
- [x] **A1.5 Manifiesto.** Todos los permisos de la 100, sección 5.1 (sin `neverForLocation` en `BLUETOOTH_SCAN`); `uses-feature` `bluetooth_le` obligatorio; `application` con `android:name=".SecureLinkApp"`; `MainActivity` como lanzador con `android:screenOrientation="portrait"`.
- [x] **A1.6 Arranque mínimo.** `SecureLinkApp` vacía; `MainActivity` con un único texto Compose; `res/values/strings.xml` solo con `app_name` = `secure-link`.
  *Nota:* desde el hito H0, `MainActivity.kt` y `strings.xml` **son de P2**. P1 no los vuelve a tocar.
- [x] **A1.7 Carpetas de todas las specs** con `.gitkeep`, según el árbol de la 100, sección 2 (`core/.../protocol`, `gossip`, `mesh`, `storage`, `sync`, `node`, `geo`, `motion`; `app/.../mesh/ble`, `storage`, `location`, `motion`, `sync`, `service`, `permissions`, `navigation`, `ui/theme`, `ui/components`, `ui/format`, `features/sendalert`, `features/nearby`, `features/status`).
- [x] **A1.8 Verificación y PR.** Desde un clon limpio, con solo `sdk.dir` en `local.properties`: `./gradlew build` pasa (criterio 1). PR 1 hacia `main`.
  *Depende de:* nada.

### A2 · Contrato `common` (fase 0.2)

- [x] **A2.1** `Clock` (`fun interface`) y `FakeClock(nowMs)` con `advanceBy` (exige ms ≥ 0) y `set` (100, sección 4).
- [x] **A2.2** `DeviceIdentity`, `FixedDeviceIdentity` (exige `0..DEVICE_ID_MAX`) y `DEVICE_ID_MAX = 0xFFFF_FFFF_FFFFL`.
- [x] **A2.3** Pruebas: `FakeClock` devuelve la hora fijada; avanza solo con `advanceBy` o `set`; `advanceBy(-1)` lanza (criterio 4). `FixedDeviceIdentity` fuera de rango lanza.
  *Depende de:* A1.

### A4 · Implementaciones `common`

- [x] **A4.1** `SystemClock` en `app/.../common/`: **única** clase con `System.currentTimeMillis()`.
- [x] **A4.2** Interfaz `IdStore` (leer y escribir un texto), con `SharedPreferencesIdStore` (`identity` / `device_id`) y `FakeIdStore`.
- [x] **A4.3** `PrefsDeviceIdentity(store, generate)`: la primera vez llama a `generate()`, aplica `and DEVICE_ID_MAX` y guarda 12 hexadecimales; después lee.
- [x] **A4.4** Pruebas JVM con `FakeIdStore` (criterio 5): un solo llamado al generador; una segunda instancia devuelve el mismo valor; un generador que devuelve un `Long` negativo da un valor dentro de rango.
- [x] **A4.5** Una prueba en dispositivo con `SharedPreferences` reales (necesita ADB).
  *Depende de:* A2.

### A5 · Reglas del proyecto

- [x] **A5.1** Prueba en `:core`: falla si algún archivo de `core/src` contiene `import android` (criterio 2).
- [x] **A5.2** Prueba: falla si `libs.versions.toml` contiene `+`, `latest` o un rango (criterio 3).
- [x] **A5.3** Prueba: la carpeta de `vectorsDir` existe (criterio 9).
  *Depende de:* A1.

### A3 · `AppContainer` de prototipo (fase 0.3)

- [x] **A3.1** `AppContainer(context)` con `clock = SystemClock()`, `deviceIdentity = PrefsDeviceIdentity(...)` y `permissionChecker = FakePermissionChecker(todo concedido)`, con un comentario `// provisional: A3'`.
- [x] **A3.2** `SecureLinkApp` crea **un solo** `AppContainer` y lo expone como propiedad (sin singletons globales).
- [ ] **A3.3** Cablear cada fake **cuando su contrato se fusione**:
  - [ ] 102: `InMemoryStorage` (todas las interfaces de almacenamiento)
  - [ ] 104: `FakeMeshTransport`
  - [ ] 105: `FakeLocationSource`, `FakeCompass`, `FakeMapsLauncher`, `FakeMotionSource`
  - [ ] 106: `FakeSyncStatus`, `FakeSyncRunner`
  - [ ] 107: `FakeMeshStatus`, `FakeMeshControl`, `FakeAutoAlertController`
  *Depende de:* A4 y B1 (contrato de permisos de P2).

### A6 · Instalación

- [x] **A6.1** Instalar el APK de depuración en un Samsung con Android 14 o superior, sin ADB (criterio 10). Registrar el modelo y la versión de One UI en `memory/P1.md`.

### A3' · Permisos reales (cuando P2 lo pida)

- [ ] **A3'.1** Cambiar `FakePermissionChecker` por `AndroidPermissionChecker` en `AppContainer`.
  *Hecho cuando:* en una instalación nueva aparece la pantalla de permisos.
  *Depende de:* B5 y el `[PIDE a P1]` de P2.

### Cierre de la parte A

- [ ] Criterios 1, 2, 3, 4, 5, 9 y 10 pasan.
- [ ] `memory/P1.md`: 100-A `terminada`.

---

## Parte B · P2

### B1 · Contrato de permisos (fase 0.2, apenas se fusione A1)

- [x] **B1.1** `enum class Requirement { BLUETOOTH, LOCATION, NOTIFICATIONS, BLUETOOTH_ON, LOCATION_ON, BATTERY_UNRESTRICTED }`, en ese orden.
- [x] **B1.2** `PermissionChecker` (`check(): Map<Requirement, Boolean>`) y `FakePermissionChecker(var state)`.
- [x] **B1.3** `missingBlocking(state)`: los bloqueantes que faltan, en el orden del enum, ignorando `BATTERY_UNRESTRICTED`. Pruebas JVM (criterio 6).
  *Depende de:* A1.

### B2 · Tema y formato

- [ ] **B2.1** `ui/theme/`: `Spacing` (4, 8, 16, 24 y 32 dp), colores de tipo de la 003 (`#DC2626`, `#F97316`, `#EAB308`) y tema Material 3 claro y oscuro temporal.
- [ ] **B2.2** `UiTone { INFO, SUCCESS, WARNING, ERROR, SIMULATION }`.
- [ ] **B2.3** `ui/format/`: clasificación **pura** de cada regla de la 100, sección 11 (distancia, tiempo transcurrido, hora, personas, precisión, rumbo cardinal, cuenta atrás, `deviceId` y `relayTag`).
- [ ] **B2.4** Pruebas JVM con los ejemplos y bordes de la tabla: 999 m, 1000 m, 99,9 km, 100 km, 59 s, 60 min, 24 h, 22,5° / 67,5° del rumbo, 255 personas (criterio 11).
- [ ] **B2.5** Textos de formato en `strings.xml`, con plurales en español.
  *Depende de:* B1 (proyecto).

### B3 · Componentes

- [ ] **B3.1** Los 9 componentes de la 100, sección 10: `ScreenScaffold`, `SectionCard`, `InfoRow`, `StatusChip`, `MessageBanner`, `PrimaryButton`, `SecondaryButton`, `EmptyState`, `ConfirmDialog` y `SimulationBadge`.
- [ ] **B3.2** Pantalla `@Preview` con todos los tonos y variantes (criterio 13).
  *Depende de:* B2.

### B4 · Navegación

- [ ] **B4.1** Rutas `sendalert`, `nearby` y `status`, con barra inferior.
- [ ] **B4.2** Pantallas provisionales `SendAlertScreen`, `NearbyScreen` y `StatusScreen`, rotuladas "Pendiente: spec 108" / "spec 109".
- [ ] **B4.3** Prueba de Compose: los tres destinos navegan y muestran su rótulo (criterio 8).
  *Depende de:* B3.

### B5 · Pantalla de permisos

- [ ] **B5.1** `AndroidPermissionChecker`: los tres de Bluetooth juntos, ubicación fina y aproximada juntas, notificaciones, adaptador encendido, ubicación del sistema activada y exclusión de batería.
- [ ] **B5.2** Detección de "negado permanentemente": ya se pidió (guardado en `SharedPreferences`) y `shouldShowRequestPermissionRationale` es falso.
- [ ] **B5.3** Pantalla según la 100, sección 12: filas con chip de estado (`Listo`, `Falta`, `Lo negaste antes…`) y sus botones (pedir permiso, abrir ajustes de la app, encender Bluetooth, ajustes de ubicación, exclusión de batería). Paso de Samsung con "Ya lo hice" guardado. "Continuar" deshabilitado con `permissions_missing_hint`.
- [ ] **B5.4** `MainActivity`: consulta `PermissionChecker` en `onResume` y muestra la pantalla de permisos o la navegación; banner `common_lost_permission` si volvió por un permiso perdido.
- [ ] **B5.5** Al pasar la pantalla de permisos: `MeshServiceStarter.ensureStarted(context)` (contrato de P1). Si todavía no existe, dejarlo anotado y pedirlo con `[PIDE a P1]`.
- [ ] **B5.6** Pruebas de Compose con `FakePermissionChecker`: pantalla de permisos con un bloqueante faltante; navegación sin ninguno (criterio 7); cada estado de fila (criterio 12).
- [ ] **B5.7** `[PIDE a P1]` en `memory/P2.md`: cambiar el fake por `AndroidPermissionChecker` en `AppContainer` (A3').
  *Depende de:* B4.

### B6 · Textos

- [ ] **B6.1** Todos los `id` de la 100, sección 12, y los `common_*` en `strings.xml`.
- [ ] **B6.2** Prueba que compara los `id` de `strings.xml` con la lista de la sección 12 (criterio 12).
  *Depende de:* B5.

### Cierre de la parte B

- [ ] Criterios 6, 7, 8, 11, 12 y 13 pasan, y P1 hizo A3'.
- [ ] `memory/P2.md`: 100-B `terminada`.

---

## Cierre de la spec 100

- [ ] Partes A y B `terminadas`. Quien cierre la última lo anota en su memoria, mencionando a la otra persona.
