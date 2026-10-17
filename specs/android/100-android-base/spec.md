# 100 android-base

Capa: 1 · Persona: P1 (100-A: tareas 1-4, 7-9) · P2 (100-B: tareas 5, 6, 10-12)
Depende de: 000, 001 (solo el tamaño de 48 bits del `deviceId`)
Entrega: proyecto Gradle con los módulos `:core` (Kotlin puro) y `:app` (Android); `Clock` + `FakeClock`; `DeviceIdentity` + `FixedDeviceIdentity`; `PermissionChecker` + `FakePermissionChecker`; pantalla de permisos; navegación con pantallas provisionales; tema temporal, componentes base y reglas de formato; convenciones de contenido visible; `AppContainer`
Consume: ninguna interfaz

## Objetivo

Dejar listo el proyecto Android sobre el que cada spec de la capa 2 trabaja en su propia carpeta, sin tocar archivos compartidos: módulos, dependencias, permisos, primitivas comunes y cableado.

## Alcance

- Proyecto Gradle en `android/`, con un módulo `:core` (JVM, sin Android) y un módulo `:app` (Android).
- Catálogo de versiones con todas las dependencias que necesita la capa 2, en versión exacta.
- `AndroidManifest.xml` con todos los permisos del MVP.
- Pantalla de entrada que pide y comprueba los permisos.
- Primitivas compartidas: reloj e identidad del dispositivo.
- Navegación con las tres pantallas (enviar alerta, cercanas, estado) como marcadores de posición.
- Tema Material 3 y textos en `strings.xml` en español (regla 34).
- `AppContainer` con el cableado manual.
- Lectura de la URL y la clave `anon` de Supabase desde `local.properties`.

## Fuera de alcance

- Lógica del protocolo (101, 103), BLE (104), Room (102), sensores (105), sync (106), servicio en primer plano (107) y pantallas reales (108, 109).
- Declarar el servicio en el manifiesto: lo agrega la 107 (capa 3).
- Cliente de Supabase: lo elige y lo agrega la 106 (capa 3).
- Reinicio al encender el teléfono (`BOOT_COMPLETED`). Si la 107 confirma que hace falta, lo agrega.

## 1. Módulos

| Módulo | Tipo | Contiene | Puede usar |
|---|---|---|---|
| `:core` | Kotlin JVM (plugin `org.jetbrains.kotlin.jvm`) | toda la lógica pura y las interfaces | Kotlin estándar y `kotlinx-coroutines-core` |
| `:app` | Android (plugin `com.android.application`) | adaptadores, servicio, UI y cableado | `:core` y las bibliotecas de Android |

`:app` depende de `:core`. `:core` nunca depende de `:app` ni de Android: si algún archivo de `:core` importa `android.*`, no compila. Así se cumplen las reglas 5 y 14 por construcción.

## 2. Carpetas y dueños

```
android/
  settings.gradle.kts            [100]
  build.gradle.kts               [100]
  gradle/libs.versions.toml      [100]
  gradle/wrapper/                [100]
  core/
    build.gradle.kts             [100]
    src/main/kotlin/com/senda/core/
      common/        Clock, DeviceIdentity y sus fakes            [100]
      protocol/      modelos, codec, claves, config 001           [101]
      gossip/        motor de reenvío puro                        [103]
      mesh/          MeshTransport, rotación, BleConfig           [104]
      storage/       interfaces de almacenamiento y fakes         [102]
      sync/          motor de subida, reintentos y puertos        [106]
      node/          coordinador del nodo y política de energía   [107]
      geo/           distancia y rumbo                            [105]
      motion/        ShakeDetector                                [105]
    src/test/kotlin/com/senda/core/<mismo paquete>/          [dueño del paquete]
  app/
    build.gradle.kts             [100]
    src/main/AndroidManifest.xml [100]
    src/main/java/com/senda/
      SendaApplication.kt           Application: crea AppContainer   [100]
      AppContainer.kt            cableado                         [100]
      MainActivity.kt            permisos + navegación            [100]
      common/        SystemClock, PrefsDeviceIdentity             [100]
      permissions/   PermissionChecker, pantalla de permisos      [100]
      navigation/    rutas y barra inferior                       [100]
      ui/theme/      tema temporal, Spacing, colores de tipo       [100]
      ui/components/ componentes base (sección 10)                [100]
      ui/format/     reglas de formato (sección 11)               [100]
      mesh/          MeshTransport y ble/                         [104]
      storage/       Room                                         [102]
      location/      LocationSource, Compass, intent de Maps      [105]
      motion/        adaptador del acelerómetro                   [105]
      sync/          subida a Supabase                            [106]
      service/       Foreground Service                           [107]
      features/sendalert/   [108]   features/nearby/   [109]   features/status/   [109]
    src/main/res/values/strings.xml                               [100; cada feature agrega sus textos, ver sección 8]
    src/test/        pruebas JVM de :app (Robolectric no se usa)
    src/androidTest/ pruebas en dispositivo
```

- Los archivos marcados `[100]` son compartidos (`ARCHITECTURE.md`, sección 4). En la capa 2 solo los edita P1. En la capa 3 los edita quien tenga la spec de composición.
- Los fakes viven en el código principal (`src/main`), en el mismo paquete que su interfaz, con el prefijo `Fake` o `Fixed`. Así cualquier spec los usa sin depender de las pruebas de otra.
- La 100 crea vacías las carpetas de las demás specs (con un `.gitkeep`), y en cada `features/*` una pantalla provisional rotulada "Pendiente: spec NNN".

## 3. Build

- Gradle con Kotlin DSL y catálogo de versiones (`gradle/libs.versions.toml`). Wrapper de Gradle incluido.
- **Todas las versiones exactas** (regla 12): AGP, Kotlin, KSP, Compose BOM y cada biblioteca. Prohibido `+`, `latest` y rangos.
- Las versiones se fijan en la tarea 1 con la última estable disponible ese día y se registran en `memory/P1.md`. Esta spec no las inventa (regla 3).
- `minSdk = 34`. `compileSdk` y `targetSdk` = el nivel estable más reciente instalado al hacer la tarea 1.
- Toolchain de Java 17 en ambos módulos.
- `applicationId` y `namespace`: `com.senda`.

Dependencias que la 100 deja declaradas para la capa 2:

| Uso | Biblioteca | Módulo | Para |
|---|---|---|---|
| Corrutinas | `kotlinx-coroutines-core`, `kotlinx-coroutines-test` | `:core` | todas |
| Lectura de vectores en pruebas | `kotlinx-serialization-json` (+ plugin) | `:core` (solo pruebas) | 101, 103 |
| Pruebas JVM | `junit` 4 | ambos | todas |
| Compose | BOM, `material3`, `ui`, `ui-tooling-preview`, `activity-compose` | `:app` | 100, 108, 109 |
| ViewModel | `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | `:app` | 108, 109 |
| Navegación | `navigation-compose` | `:app` | 100 |
| Corrutinas Android | `kotlinx-coroutines-android` | `:app` | 104, 105, 107 |
| Room | `room-runtime`, `room-ktx`, `room-compiler` (KSP), `room-testing` | `:app` | 102 |
| Ubicación | `play-services-location` | `:app` | 105 |
| HTTP | `okhttp` | `:app` | 106 |
| Tareas en segundo plano | `work-runtime-ktx` | `:app` | 106 |
| JSON | `kotlinx-serialization-json` (+ plugin) | `:app` | 106 |
| Pruebas en dispositivo | `androidx.test.ext:junit`, `ui-test-junit4`, `ui-test-manifest` | `:app` | todas |

- Supabase en `BuildConfig`: `SUPABASE_URL` y `SUPABASE_ANON_KEY` se leen de `android/local.properties` (ignorado por git, regla 25). Si faltan, el proyecto compila con cadenas vacías y la 106 devuelve un error explícito al subir.
- Las pruebas de `:core` reciben la ruta de los vectores en la propiedad de sistema `vectorsDir`, que apunta a `<raíz del repo>/specs/shared/vectors`.
- Room exporta su esquema: argumento de KSP `room.schemaLocation = android/app/schemas/` (lo pide la 102), con esa carpeta versionada en git.

Comandos:

| Comando | Qué hace |
|---|---|
| `./gradlew :core:test` | pruebas de la lógica pura, sin dispositivo |
| `./gradlew :app:testDebugUnitTest` | pruebas JVM de `:app` |
| `./gradlew :app:assembleDebug` | APK de depuración |
| `./gradlew :app:connectedDebugAndroidTest` | pruebas en el Samsung conectado |
| `./gradlew build` | todo lo anterior salvo las pruebas en dispositivo |

## 4. Primitivas comunes (`:core`, paquete `common`)

```kotlin
// spec 100
fun interface Clock {
    /** Epoch en ms, UTC (regla 11). */
    fun nowMs(): Long
}

// spec 100
class FakeClock(private var nowMs: Long) : Clock {
    override fun nowMs(): Long = nowMs
    fun advanceBy(ms: Long) { require(ms >= 0); nowMs += ms }
    fun set(ms: Long) { nowMs = ms }
}

// spec 100
interface DeviceIdentity {
    /** deviceId de 48 bits (spec 001), estable mientras no se reinstale la app. */
    val deviceId: Long
}

// spec 100
class FixedDeviceIdentity(override val deviceId: Long) : DeviceIdentity {
    init { require(deviceId in 0L..DEVICE_ID_MAX) }
}

const val DEVICE_ID_MAX: Long = 0xFFFF_FFFF_FFFFL  // 48 bits, spec 001
```

Implementaciones en `:app` (paquete `common`):

- `SystemClock`: la **única** clase del proyecto que llama a `System.currentTimeMillis()` (regla 6).
- `PrefsDeviceIdentity(prefs, generate: () -> Long)`: la primera vez llama a `generate()`, se queda con los 48 bits bajos (`and DEVICE_ID_MAX`) y lo guarda en `SharedPreferences` (`identity` / `device_id`, en hexadecimal de 12 dígitos). Las siguientes veces lo lee. El generador se inyecta (regla 8); en producción es `SecureRandom().nextLong()`.

El contador de alertas no está aquí: se asigna al guardar la alerta, en la misma transacción (102, regla 21).

## 5. Permisos

### 5.1 Manifiesto

| Permiso | Tipo | Para |
|---|---|---|
| `BLUETOOTH_SCAN` | en tiempo de ejecución | escanear (104). **Sin** `neverForLocation`: esa marca hace que Android filtre algunos anuncios BLE, y la app pide ubicación de todos modos |
| `BLUETOOTH_ADVERTISE` | en tiempo de ejecución | anunciar (104) |
| `BLUETOOTH_CONNECT` | en tiempo de ejecución | pedir que se encienda el Bluetooth y leer el estado del adaptador |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | en tiempo de ejecución, se piden juntos | ubicación de la alerta y escaneo (105, 104) |
| `ACCESS_BACKGROUND_LOCATION` | en tiempo de ejecución, **se pide aparte y después** de la ubicación | que el servicio en primer plano (107) conserve la ubicación tras un reinicio automático: Android no deja crear un servicio de tipo `location` ni darle ubicación desde segundo plano sin él. En Android 11 o superior no hay diálogo: el sistema abre los ajustes de ubicación de la app y el usuario elige "Permitir todo el tiempo" (2026-10-17, hallazgo de la 107 · S1) |
| `POST_NOTIFICATIONS` | en tiempo de ejecución | notificación del servicio en primer plano (107) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `FOREGROUND_SERVICE_LOCATION` | normales | servicio en primer plano (107) |
| `INTERNET`, `ACCESS_NETWORK_STATE` | normales | subida y detección de red (106) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | normal | pedir la exclusión de la optimización de batería. La app no se publica en Play: se instala a mano para la demo |
| `VIBRATE` | normal | cuenta regresiva del movimiento brusco (108) |

Además: `<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />`.

### 5.2 Interfaz

```kotlin
// spec 100 (paquete permissions de :app)
enum class Requirement { BLUETOOTH, LOCATION, NOTIFICATIONS, BLUETOOTH_ON, LOCATION_ON, BACKGROUND_LOCATION, BATTERY_UNRESTRICTED }

interface PermissionChecker {
    /** Estado actual de cada requisito; se vuelve a consultar al volver a la app. */
    fun check(): Map<Requirement, Boolean>
}

class FakePermissionChecker(var state: Map<Requirement, Boolean>) : PermissionChecker {
    override fun check() = state
}

/** Puro: requisitos bloqueantes que faltan, en el orden del enum. */
fun missingBlocking(state: Map<Requirement, Boolean>): List<Requirement>
```

- **Bloqueantes:** `BLUETOOTH` (los tres de Bluetooth), `LOCATION`, `BLUETOOTH_ON` (adaptador encendido) y `LOCATION_ON` (ubicación del sistema activada).
- **Recomendados, no bloqueantes:** `NOTIFICATIONS` (el diseño de Figma deja continuar sin ellas; el servicio en primer plano funciona igual, solo no se ve su notificación), `BACKGROUND_LOCATION` y `BATTERY_UNRESTRICTED`.
- `BACKGROUND_LOCATION` solo se puede pedir con `LOCATION` concedido (`prerequisiteOf`): la pantalla deshabilita su botón hasta entonces.
- Las demás specs no piden permisos. Si una operación falla por falta de permiso, devuelve un error explícito (regla 16) y la app vuelve a la pantalla de permisos.

### 5.3 Pantalla de permisos
1. Al abrir la app, o al volver a ella, `MainActivity` consulta `PermissionChecker`. Si `missingBlocking` no está vacío, muestra la pantalla de permisos en lugar de la navegación. La primera vez también se muestra aunque no falte nada, hasta que el usuario toca "Continuar" (se guarda como `onboardingDone`); si después se pierde un permiso bloqueante, vuelve con un aviso.
2. La pantalla lista cada requisito con una frase en español que explica para qué sirve y un botón:
   - "Permitir" para los permisos en tiempo de ejecución y "Revisar" cuando ya están concedidos (abre los ajustes que correspondan).
   - "Ajustes" si el usuario los negó de forma permanente; "Encender" para el Bluetooth apagado y "Activar" para la ubicación del sistema desactivada.
   - La ubicación en segundo plano solo se puede pedir con la ubicación concedida; en Android 11 o superior el sistema abre los ajustes de la app.
3. Paso recomendado: "Quitar restricción de batería" (abre la solicitud de exclusión).
4. Paso informativo para Samsung: cómo desactivar "Aplicaciones en suspensión" para la app, con un botón a los ajustes de la app. La app no puede comprobarlo, así que se muestra hasta que el usuario toca "Ya lo hice" (se guarda en `SharedPreferences`).
5. Cuando no falta ningún requisito bloqueante, entra a la navegación.

## 6. Navegación y tema

- `MainActivity` solo en vertical (`android:screenOrientation="portrait"`), lo pide la 105: simplifica la brújula y deja un solo diseño.
- Barra inferior con tres destinos: **Enviar alerta** (`sendalert`), **Cercanas** (`nearby`) y **Estado** (`status`).
- Cada destino muestra la pantalla provisional de su carpeta (`SendAlertScreen`, `NearbyScreen`, `StatusScreen`) hasta que la reemplace su spec.
- Tema Material 3 claro y oscuro en `ui/theme/`.

## 7. `AppContainer`

```kotlin
// spec 100
class AppContainer(context: Context) {
    val clock: Clock = SystemClock()
    val deviceIdentity: DeviceIdentity = PrefsDeviceIdentity(prefs(context), SecureRandom()::nextLong)
    val permissionChecker: PermissionChecker = AndroidPermissionChecker(context)
    // Las specs de capa 3 agregan aquí sus dependencias.
}
```

- `SendaApplication` (la `Application`) crea un único `AppContainer` y lo expone como propiedad. No hay singletons globales ni inyección por reflexión (regla 10, `ARCHITECTURE.md`).
- Los ViewModel se crean con `viewModelFactory { initializer { ... } }` y toman lo que necesitan del contenedor.

## 8. Textos

Todos los textos visibles en `res/values/strings.xml`, en español. Los nombres de los recursos van en inglés con el prefijo de la feature (`sendalert_`, `nearby_`, `status_`, `permissions_`, `common_`). Así cada feature agrega sus líneas en su propio bloque sin chocar. Los textos de los catálogos de esta spec, la 108 y la 109 son **provisionales**: el texto final sale del diseño en Figma y solo cambia `strings.xml`.

## 9. Contenido visible: convenciones para las specs de UI

Las specs con pantallas (100, 108, 109) describen su contenido con dos tablas, de las que sale directamente el design brief:

- **Variables visibles:** `variable` · origen (spec o interfaz) · formato · ejemplo · qué se muestra si falta.
- **Mensajes:** `id` (clave en `strings.xml`) · cuándo aparece (condición interna) · dónde · tono · texto provisional · acción.

Tonos (`UiTone`): `INFO`, `SUCCESS`, `WARNING`, `ERROR` y `SIMULATION`. Este último rotula todo lo simulado (regla 30).

Cada pantalla tiene un `UiState` inmutable (una `data class`) con exactamente sus variables visibles. El ViewModel lo arma a partir de las interfaces que consume, y la pantalla solo lo dibuja.

## 10. Diseño y componentes base

**Desde el 2026-10-17 el diseño sale del Figma "SENDA APP"** (tokens en `ui/theme/`: `SendaColors`, `SendaText` con Roboto, `Dimens`; íconos Lucide en `res/drawable`; textos en `strings.xml`). Quedan construidos: `ScreenScaffold`, `ScreenHeader`, `ConnectivityStrip`, `SendaCard`, `EmergencyBadge`, `DemoBadge`, `SimulationBadge`, `SendaButton`, `SmallActionButton`, `SendaTextButton`, `SendaBottomBar`, `SendaSwitch`, `SendaCheckbox`, `QuantityStepper` e `InfoRow`. Faltan `StatusChip`, `MessageBanner`, `EmptyState` y `ConfirmDialog`, que Figma no dibuja: se construyen con la primera pantalla que los necesite, en el mismo estilo. La vista previa de los componentes es una galería solo para depuración (`gallery.GalleryActivity`), no `@Preview`. El texto que sigue conserva la descripción original.


El diseño final (colores, tipografía y espaciado) sale de Figma. Hasta entonces, la app usa un diseño temporal cuyo único objetivo es que cada pantalla se entienda: textos, botones y contenedores separados, nunca todo apilado.

Componentes base en `ui/components/` (dueño: 100), con estilos Material 3 por defecto:

| Componente | Uso |
|---|---|
| `ScreenScaffold(title, actions, fab, content)` | estructura de cada pantalla: barra superior, contenido con desplazamiento y botón flotante opcional |
| `SectionCard(title?, content)` | contenedor que agrupa un bloque de información |
| `InfoRow(label, value, tone?)` | par etiqueta-valor en una línea |
| `StatusChip(text, tone)` | estado corto ("Anunciando", "Sin internet") |
| `MessageBanner(text, tone, actionLabel?, onAction?)` | aviso o error con una acción opcional |
| `PrimaryButton` / `SecondaryButton` | acción principal y acción secundaria |
| `EmptyState(title, body, actionLabel?)` | pantalla o lista sin contenido |
| `ConfirmDialog(title, body, confirm, dismiss)` | confirmar acciones irreversibles |
| `SimulationBadge()` | etiqueta "SIMULACIÓN" |

- Espaciado temporal: una sola escala en `ui/theme/Spacing.kt` (4, 8, 16, 24 y 32 dp).
- Colores del tipo de emergencia: los del diseño de Figma (amarillo `#866900` sobre `#fff7d6`, naranja `#aa510e` sobre `#fff0e1`, rojo `#b83837` sobre `#fceaea`; en oscuro `#f4d56a`, `#ffb36b` y `#ff9290`), definidos una vez en `ui/theme/SendaColors.kt`. **No coinciden con los de la 003** (rojo `#DC2626`, naranja `#F97316`, amarillo `#EAB308`, del panel web): hay que alinearlos (hallazgo 2026-10-17). El tipo **nunca** se comunica solo por color: siempre va con su texto.

## 11. Formato de valores

Reglas comunes en `ui/format/` (dueño: 100). La clasificación es pura y se prueba en la JVM; el texto sale de `strings.xml` (con plurales).

| Valor | Regla | Ejemplos |
|---|---|---|
| Distancia | < 1000 m: redondeo a 10 m; de 1 a 100 km: un decimal con coma; ≥ 100 km: entero | `850 m`, `1,2 km`, `123 km`; sin dato: `—` |
| Tiempo transcurrido | < 1 s: "ahora"; < 1 min: segundos; < 1 h: minutos; < 24 h: horas; si no, días (los segundos salen del diseño de Figma) | `ahora`, `hace 20 s`, `hace 5 min`, `hace 3 h`, `hace 2 d` |
| Hora | `HH:mm` en la zona horaria del teléfono (solo al mostrar; internamente, UTC, regla 11) | `07:33` |
| Personas | singular, plural y tope de la 001 | `1 persona`, `3 personas`, `255 o más personas` |
| Precisión | metros enteros | `±15 m` |
| Rumbo cardinal | 8 sectores de 45° centrados en cada punto | `N`, `NE`, `E`, `SE`, `S`, `SO`, `O`, `NO` |
| Cuenta atrás | segundos o `m:ss` | `15`, `0:42` |
| `deviceId` | 12 hexadecimales en minúscula, en grupos de 4 | `a1b2 c3d4 e5f6` |
| `relayTag` | 4 hexadecimales en minúscula | `e5f6` |

## 12. Contenido visible de la pantalla de permisos

Diseño: marco "P1 · Permisos" del Figma SENDA APP. Lo que Figma no dibuja se marca **(no está en Figma)** y usa el mismo estilo.

### Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `rows` | `PermissionChecker` | `Bluetooth`, `Dispositivos cercanos`, `Ubicación` y `Notificaciones`, más `Ubicación del teléfono` solo mientras el interruptor del sistema está apagado | ver mensajes | — |
| `recommended` | `PermissionChecker` | `Ubicación en segundo plano` y `Sin restricción de batería` (no bloquean) | ver mensajes | — |
| `row.state` | `PermissionChecker` + historial de negaciones | `GRANTED`, `MISSING` o `DENIED_PERMANENTLY` | `MISSING` | — |
| `row.action` | estado de la fila | `REQUEST`, `OPEN_APP_SETTINGS`, `ENABLE_BLUETOOTH`, `OPEN_BLUETOOTH_SETTINGS`, `OPEN_LOCATION_SETTINGS`, `REQUEST_BATTERY_EXCLUSION` u `OPEN_BATTERY_SETTINGS` | `REQUEST` | — |
| `row.actionEnabled` | `prerequisiteOf` | falso en la ubicación en segundo plano hasta tener la ubicación | `true` | — |
| `canContinue` | `missingBlocking(...).isEmpty()` | booleano | `false` | — |
| `showSamsungStep` | `SharedPreferences` | verdadero hasta tocar "Ya lo hice" | `true` | `true` |
| `showLostPermissionBanner` | `SharedPreferences` + `canContinue` | ya se había pasado la pantalla y falta un requisito bloqueante | `false` | `false` |

### Mensajes

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `permissions_title` | siempre | encabezado | — | Preparar tu teléfono | — |
| `permissions_subtitle` | siempre | encabezado | — | Una red de ayuda, incluso sin internet. | — |
| `permissions_intro` | siempre | bloque de introducción | INFO | Estos permisos permiten compartir y recibir alertas entre teléfonos. | — |
| `permissions_bluetooth_title` | fila `BLUETOOTH_ON` | fila | — | Bluetooth | — |
| `permissions_bluetooth_body` | fila `BLUETOOTH_ON` | fila | — | Comparte alertas con la malla cercana. | — |
| `permissions_nearby_title` | fila `BLUETOOTH` | fila | — | Dispositivos cercanos | — |
| `permissions_nearby_body` | fila `BLUETOOTH` | fila | — | Detecta y conecta con otros teléfonos. | — |
| `permissions_location_title` | fila `LOCATION` | fila | — | Ubicación | — |
| `permissions_location_body` | fila `LOCATION` | fila | — | Añade una posición aproximada a tu alerta. | — |
| `permissions_notifications_title` | fila `NOTIFICATIONS` | fila | — | Notificaciones | — |
| `permissions_notifications_body` | fila `NOTIFICATIONS` | fila | — | Recibe avisos aunque no abras la app. | — |
| `permissions_state_active` | Bluetooth `GRANTED` | estado de la fila | SUCCESS | ✓ Activo | — |
| `permissions_state_granted_masculine` | Dispositivos cercanos `GRANTED` | estado de la fila | SUCCESS | ✓ Permitido | — |
| `permissions_state_granted_feminine` | Ubicación o ubicación en segundo plano `GRANTED` | estado de la fila | SUCCESS | ✓ Permitida | — |
| `permissions_state_pending` | `row.state = MISSING` | estado de la fila | WARNING | ○ Pendiente | — |
| `permissions_action_review` | `row.state = GRANTED` | botón de la fila | — | Revisar | abre los ajustes que correspondan |
| `permissions_action_allow` | `row.state = MISSING` | botón de la fila | — | Permitir | `REQUEST` o `REQUEST_BATTERY_EXCLUSION` |
| `permissions_continue` | siempre | botón principal | — | Continuar | entra a la navegación; deshabilitado si `canContinue = false` |
| `permissions_continue_hint` | `canContinue = true` | sobre el botón | — | Puedes continuar sin notificaciones y activarlas después en Estado. | — |
| `permissions_demo_notice` | versión de depuración | bajo el botón | — | Demostración · No se enviarán alertas reales | — |
| `permissions_state_granted_feminine_plural` | Notificaciones `GRANTED` **(no está en Figma)** | estado de la fila | SUCCESS | ✓ Permitidas | — |
| `permissions_state_battery_ok` | batería `GRANTED` **(no está en Figma)** | estado de la fila | SUCCESS | ✓ Sin restricción | — |
| `permissions_state_denied` | `row.state = DENIED_PERMANENTLY` **(no está en Figma)** | estado de la fila | ERROR | Lo negaste antes: actívalo en Ajustes. | — |
| `permissions_action_settings` | `row.state = DENIED_PERMANENTLY` **(no está en Figma)** | botón de la fila | — | Ajustes | `OPEN_APP_SETTINGS` |
| `permissions_action_enable` | Bluetooth apagado **(no está en Figma)** | botón de la fila | — | Encender | `ENABLE_BLUETOOTH` u `OPEN_BLUETOOTH_SETTINGS` |
| `permissions_action_activate` | ubicación del sistema apagada **(no está en Figma)** | botón de la fila | — | Activar | `OPEN_LOCATION_SETTINGS` |
| `permissions_location_services_title` | fila `LOCATION_ON` sin cumplir **(no está en Figma)** | fila | WARNING | Ubicación del teléfono | — |
| `permissions_location_services_body` | fila `LOCATION_ON` sin cumplir **(no está en Figma)** | fila | WARNING | Activa la ubicación del sistema para calcular distancias. | — |
| `permissions_recommended_title` | siempre **(no está en Figma)** | título de sección | — | Recomendado | — |
| `permissions_background_title` | fila `BACKGROUND_LOCATION` **(no está en Figma)** | fila | — | Ubicación en segundo plano | — |
| `permissions_background_body` | fila `BACKGROUND_LOCATION` **(no está en Figma)** | fila | — | Mantiene la ubicación si el sistema reinicia la app. En Ajustes, elige «Permitir todo el tiempo». | — |
| `permissions_background_locked` | `actionEnabled = false` **(no está en Figma)** | estado de la fila | WARNING | Primero permite la ubicación. | — |
| `permissions_battery_title` | fila `BATTERY_UNRESTRICTED` **(no está en Figma)** | fila | — | Sin restricción de batería | — |
| `permissions_battery_body` | fila `BATTERY_UNRESTRICTED` **(no está en Figma)** | fila | — | Evita que el sistema detenga la app. | — |
| `permissions_samsung_title` | `showSamsungStep` **(no está en Figma)** | paso informativo | INFO | Aplicaciones en suspensión | — |
| `permissions_samsung_body` | `showSamsungStep` **(no está en Figma)** | paso informativo | INFO | Samsung: en Ajustes > Batería > Límites de uso en segundo plano, quita Senda de «Aplicaciones en suspensión». | — |
| `permissions_samsung_open` | `showSamsungStep` **(no está en Figma)** | paso informativo | — | Abrir ajustes | ajustes de la app |
| `permissions_samsung_done` | `showSamsungStep` **(no está en Figma)** | paso informativo | — | Ya lo hice | guarda `samsungAcknowledged` |
| `permissions_missing_hint` | `canContinue = false` **(no está en Figma)** | sobre el botón | WARNING | Faltan permisos obligatorios. | — |
| `common_lost_permission` | `showLostPermissionBanner` **(no está en Figma)** | banner superior | ERROR | Se perdió un permiso necesario. Concédelo de nuevo para seguir. | — |

## Criterios de aceptación

1. Desde un clon limpio, con solo `sdk.dir` en `local.properties`, `./gradlew build` termina sin errores. Funciona también sin las claves de Supabase.
2. `:core` no tiene dependencias de Android: una prueba de `:core` falla si encuentra `import android` en `core/src`. Además, el plugin del módulo es `kotlin.jvm`.
3. Ningún archivo de Gradle contiene `+`, `latest` ni rangos de versión. Lo comprueba una prueba que lee `libs.versions.toml`.
4. `FakeClock` devuelve la hora fijada y avanza solo con `advanceBy` o `set`. `advanceBy` con un valor negativo lanza una excepción.
5. `PrefsDeviceIdentity` llama al generador una sola vez. Una segunda instancia sobre las mismas preferencias devuelve el mismo `deviceId`. El valor siempre está en `0..DEVICE_ID_MAX`, aunque el generador devuelva un `Long` negativo.
6. `missingBlocking` devuelve los requisitos bloqueantes que faltan, en orden, e ignora `NOTIFICATIONS`, `BACKGROUND_LOCATION` y `BATTERY_UNRESTRICTED`.
7. Con `FakePermissionChecker`, una prueba de Compose muestra la pantalla de permisos si falta un requisito bloqueante y la navegación si no falta ninguno.
8. La navegación muestra los tres destinos con sus pantallas provisionales rotuladas.
9. Una prueba de `:core` encuentra la carpeta `vectorsDir`.
10. El APK de depuración se instala y abre en un Samsung Galaxy con Android 14 o superior. Es una prueba manual en dispositivo y se registra el modelo en `memory/P1.md`.
11. Cada regla de formato de la sección 11 tiene una prueba JVM con los ejemplos de su tabla, incluidos los bordes (999 m, 1000 m, 59 s, 60 min, 24 h y los límites de cada sector cardinal).
12. Cada mensaje de la sección 12 existe en `strings.xml` con su `id`, y la pantalla de permisos muestra cada estado de fila (lo comprueba una prueba de Compose con `FakePermissionChecker`).
13. Los componentes de la sección 10 existen y se ven en una pantalla de vista previa (`@Preview`) con todos los tonos.

## Tareas

1. Crear el proyecto Gradle con los dos módulos, el wrapper y el catálogo de versiones. Fijar y registrar las versiones exactas.
2. Escribir el manifiesto y la lectura de `local.properties` para `BuildConfig`.
3. `:core/common`: `Clock`, `FakeClock`, `DeviceIdentity`, `FixedDeviceIdentity`, con sus pruebas (criterio 4).
4. `:app/common`: `SystemClock` y `PrefsDeviceIdentity`, con sus pruebas (criterio 5).
5. `permissions/`: `PermissionChecker`, `AndroidPermissionChecker`, `FakePermissionChecker`, `missingBlocking` y la pantalla, con sus pruebas (criterios 6 y 7).
6. Navegación, tema, pantallas provisionales y carpetas vacías de las demás specs (criterio 8).
7. `AppContainer` y `SendaApplication`.
8. Pruebas de reglas del proyecto (criterios 2, 3 y 9).
9. Instalar en un Samsung y registrar el resultado (criterio 10).
10. `ui/format/` con sus pruebas (criterio 11).
11. `ui/components/` y `Spacing`, con la vista previa de tonos (criterio 13).
12. Textos de la sección 12 en `strings.xml` y estados de fila en la pantalla de permisos (criterio 12).
