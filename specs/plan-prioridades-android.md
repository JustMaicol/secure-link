# Plan de prioridades · app Android

Orden de trabajo de las specs Android para tres personas en paralelo, sin que el código de una dependa del código de otra.

- **Prioridad 1:** liberar el **prototipo visual**: la app completa navegable con datos de prueba, para validar el diseño de Figma.
- **Prioridad 2:** las **funciones básicas**: crear una alerta y que se propague de teléfono en teléfono (A → B → C).
- **Prioridad 3:** las **mejoras**: subida al servidor, alerta automática, notificaciones, ahorro de batería…
- **Después:** panel web (200-205), integración (400) y simulación (300-301).

> **Reparto temporal "prioridad Android".** Mientras los tres trabajan en Android, rige el reparto de este plan, ya reflejado en `ARCHITECTURE.md` (secciones 5 y 6), `AGENTS.md`, `MEMORY.md` y `memory/Pn.md`. Al terminar Android, P3 vuelve a la web (200-205), según `ARCHITECTURE.md`, sección 6.2.

## 1. Reglas para no depender del código de otro

1. **Contratos primero.** Cada persona publica en la fase 0 las interfaces y los fakes que otros consumen, copiados de su spec, en un PR pequeño. Desde que se fusiona, cada uno programa y prueba contra los **fakes** de los demás, nunca contra su implementación (`ARCHITECTURE.md`, reglas de independencia).
2. **Carpetas disjuntas.** Cada persona solo toca las carpetas de la tabla de la sección 2. Si dos personas tocan el mismo archivo, el plan está mal: hay que avisar.
3. **Archivos compartidos con un solo dueño:**

| Archivo | Dueño | Los demás… |
|---|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml`, `AppContainer.kt` | P1 | piden cambios en su `memory/Pn.md` con `[PIDE a P1]` |
| `strings.xml` | P2 | P2 escribe todos los textos de los catálogos (100, 108, 109) |
| `MainActivity.kt` y `navigation/` | P2 | — |
| `android/app/schemas/` (Room) | P3 | — |

4. **Una rama por persona** (`p1`, `p2`, `p3`), PRs pequeños y frecuentes hacia `main` (`MEMORY.md`, guía de git).
5. **Todo lo que muestra datos de prueba lleva el rótulo "PROTOTIPO · datos de prueba"** (regla 30).

## 2. Reparto

| Persona | Rol en Android | Specs | Carpetas |
|---|---|---|---|
| **P1** | Base y núcleo de la malla | 100-A (proyecto, build, primitivas, `AppContainer`), 101, 103, 107 | `android/` raíz y Gradle, `core/common`, `core/protocol`, `core/gossip`, `core/node`, `app/common`, `app/service`, `AppContainer.kt`, `SendaApplication.kt` |
| **P2** | Interfaz y prototipo visual | 100-B (permisos, componentes, formato, navegación, tema), 108, 109, 106 (+ vectores de prueba) | `app/permissions`, `app/navigation`, `app/ui`, `app/features/*`, `app/src/debug/**/prototype` (galería y datos de prueba), `MainActivity.kt`, `strings.xml`, `core/sync`, `app/sync`, `specs/shared/vectors/` |
| **P3** | Radio, sensores y datos | 104, 105, 102 | `core/mesh`, `app/mesh/ble`, `app/src/debug/**/mesh` (pantalla del prototipo BLE), `core/geo`, `core/motion`, `app/location`, `app/motion`, `core/storage`, `app/storage`, `app/schemas` |

La 100 se divide en dos partes que no comparten carpetas:
- **100-A (P1):** tareas 1-4, 7, 8 y 9 de la spec 100.
- **100-B (P2):** tareas 5, 6, 10, 11 y 12.

## 3. Fases

### Fase 0 · Arranque (bloqueante y corta)

| Paso | P1 | P2 | P3 |
|---|---|---|---|
| **0.1** | **Esqueleto que compila** (100, tareas 1 y 2): Gradle, módulos `:core` y `:app`, catálogo de versiones con **todas** las dependencias, manifiesto con los permisos. PR inmediato | **Vectores** de la 001 y la 003 en `specs/shared/vectors/` (solo JSON, no necesita el proyecto) | **Prototipo BLE de 1 hora** (104, tarea 1) en un proyecto Android aparte, que no se fusiona. Decide si se sigue con BLE o se pasa a Nearby |
| **0.2** | Contratos: `core/common` (100, tarea 3) y los modelos, claves y `ProtocolConfig` de la 101 (secciones 2-4, sin codec); interfaces y fakes de la 107 (sección 2: `MeshStatus`, `MeshControl`, `AutoAlertController`); `MeshServiceStarter` y `NotificationRoutes` (sección 4) | Contratos: `PermissionChecker`, `FakePermissionChecker` y `missingBlocking` (100, sección 5.2); `SyncRunner` y `FakeSyncRunner` (sección 4) | Contratos: interfaces y fakes de la 104 (`MeshTransport`), la 105 (`LocationSource`, `Compass`, `MapsLauncher`, `MotionSource`, `GeoPoint`) y la 102 (todas las de la sección 1, con `InMemoryStorage`) |
| **0.3** | `AppContainer` de prototipo: todas las dependencias apuntan a **fakes** | — | — |

**Hito H0:** `main` compila, con todos los contratos y un `AppContainer` con fakes. Desde aquí, nadie espera a nadie.

Si el prototipo BLE falla (P1 de la 104, `DATA_TOO_LARGE`), se detiene el trabajo de la malla y se decide en equipo el cambio de la 001 antes de la fase 2.

### Fase 1 · Prototipo visual (prioridad 1)

| P1 | P2 | P3 |
|---|---|---|
| 101 completa: codec, fábricas, `Mulberry32` y `Hex` (tareas 3-6) | 100-B: componentes, formato, tema, navegación y **pantalla de permisos** | 105: distancia, rumbo, `e7ToDecimal` y `classifyLocation`, puros y con pruebas (tareas 2 y 3) |
| 103: motor de reenvío completo, con `ScenarioRunner` y los escenarios de referencia | **108** completa sobre fakes: formulario, botón de mantener presionado, "Mi alerta" y cuenta regresiva | 102: pruebas de contrato y Room v1 (tareas 2 y 3) |
| | **109** completa sobre fakes: lista, detalle con flecha, "Voy en camino", Maps, Estado y filtro de depuración | 104: `AnnouncementRotation` y `BleController` con puertos falsos (tareas 3 y 4) |
| | `app/src/debug/**/prototype/`: datos de prueba con los **datos de ejemplo del design brief** (sección 8), cargados en los fakes; **galería de frames** del brief; rótulo "PROTOTIPO" (planes de la 108 y la 109) | |

**Hito H1, prototipo visual liberado:** APK de depuración que se instala en cualquier Samsung **sin ADB**. Recorre todas las pantallas del brief con datos de prueba coherentes con Figma. Ninguna función real todavía.

### Fase 2 · Funciones básicas: crear y propagar alertas (prioridad 2)

| P1 | P2 | P3 |
|---|---|---|
| **Revisar los cambios de Android 15-17** en servicios en primer plano y BLE (107, tarea 1) | Pruebas de Compose de 108 y 109 sobre fakes (sus criterios) | **104** con la API real: `BleMeshTransport` y receptor del estado del Bluetooth (tareas 5 y 6) |
| **107 básica:** `MeshNode` con arranque, entradas, efectos, bandeja de salida, ecos y teléfonos cerca (tarea 3); `MeshService` con la notificación del servicio (tarea 7). **Sin** política de energía, alerta automática ni notificaciones de alertas | Prueba del catálogo de textos (108 y 109, criterio de `strings.xml`) | **102** con Room: contador transaccional, recepción idempotente, bandeja de salida, ecos y ajustes (tareas 4 y 5) |
| Cambiar en `AppContainer` los fakes por las implementaciones reales de 102, 104 y 105 a medida que se fusionan | | **105** real: `LocationSource` con FusedLocationProvider, `Compass` con declinación y `MapsLauncher` (tareas 4, 5 y 7) |

**Hito H2, malla básica:** con 3 Samsung y el filtro de depuración, una alerta creada en A aparece en la lista de C, con distancia y rumbo, y C puede avisar "voy en camino" (000, criterios 1, 2 y 5 en los teléfonos).

### Fase 3 · Mejoras (prioridad 3)

| P1 | P2 | P3 |
|---|---|---|
| 107: alerta automática (tarea 4), notificaciones con destinos (tarea 5), **política de energía** y wake lock, disparo de la subida (tarea 6) | **106** completa: subida, reintentos, WorkManager | 105: `ShakeDetector`, `MotionSource` con lecturas agrupadas y **medidor de movimiento** (tareas 6 y 8) |
| 107: pruebas en dispositivo, con apps cerradas, consumo y recientes (tarea 8) | | 104: `setScanMode` (lo usa la política de energía) |
| | | **200 mínima:** migración de la 002 y usuario autoridad en Supabase, para que la 106 tenga contra qué subir |
| | | 105: brújula en dispositivo y ajuste de umbrales con el medidor (tarea 9) |

**Hito H3, app completa:** las alertas se suben solas al recuperar internet, con la app cerrada; la alerta automática funciona; llegan las notificaciones; y el consumo de batería está medido.

### Después de Android

| Orden | Qué | Quién |
|---|---|---|
| 1 | Panel web completo (200-205) | P3, con ayuda de P1 y P2 si terminan antes |
| 2 | Spec 400 (integración) y ensayo de la demo | todos (dueño: P3) |
| 3 | Simulación (300, 301): se recorta primero si falta tiempo | P3 y P1 |
| Opcional | `CANCELADA / A_SALVO` (000) | P2 (UI) y P3 (102) |

## 4. Qué consume cada persona de las demás (solo contratos)

| Persona | Consume de P1 | Consume de P2 | Consume de P3 |
|---|---|---|---|
| **P1** | — | `PermissionChecker` y `SyncRunner` (fakes) | `MeshTransport`, `PacketStore`, `OwnPacketRepository`, `SettingsRepository`, `UploadQueue`, `LocationSource` y `MotionSource` (fakes) |
| **P2** | modelos de la 101; `MeshStatus`, `MeshControl` y `AutoAlertController` (fakes); `Clock` y `DeviceIdentity` | — | `OwnPacketRepository`, `NearbyRepository`, `SettingsRepository`, `StorageStats`, `LocationSource`, `Compass`, `MapsLauncher`, geo y `MotionSource` (fakes) |
| **P3** | modelos, `PacketCodec` y `PacketFactory` de la 101 (en la 102 y la 104); `Clock` | `PermissionChecker` (fake) | — |

**Única dependencia de implementación que queda:** la 102 y la 104 de P3 usan el **codec y las fábricas reales** de la 101 (P1), porque son código puro sin fake (101, sección 1). Para que no bloquee:
- P1 fusiona la 101 completa al inicio de la fase 1.
- Mientras tanto, P3 avanza con lo que no la necesita: las pruebas de contrato de la 102 con `InMemoryStorage`, la rotación de la 104 y la 105.

### Puntos de contacto entre P1 y P2 (resueltos con contratos)

| Necesidad | Contrato | Lo publica | Lo usa |
|---|---|---|---|
| La 107 dispara la subida de la 106 | interfaz `SyncRunner { suspend fun runOnce(): RunResult }` + `FakeSyncRunner` en `core/sync`; `SyncEngine` la implementa | P2, en la fase 0 | P1 |
| Arrancar el servicio al pasar la pantalla de permisos | `MeshServiceStarter.ensureStarted(context)` en `app/service` (al principio, sin hacer nada) | P1, en la fase 0 | P2 lo llama desde `MainActivity` |
| Abrir una pantalla al tocar una notificación | `NotificationRoutes`: constantes de los extras (`route` = `nearby_detail`, `my_alert` o `countdown`, y `alert_key`) en `app/service` | P1, en la fase 0 | P2 los lee en `MainActivity` y navega |

Así, P1 nunca edita `MainActivity` ni `navigation/`, y P2 nunca edita `app/service`.

## 5. Checklist de hitos

- [ ] **H0** `main` compila, con todos los contratos y un `AppContainer` con fakes.
- [ ] **Prototipo BLE** decidido: se sigue con BLE o se pasa a Nearby, registrado en `memory/`.
- [ ] **H1** APK del prototipo visual instalado en los Samsung y comparado con Figma.
- [ ] **H2** Alerta A → B → C con 3 teléfonos, distancia, rumbo y "voy en camino".
- [ ] **H3** Subida sin abrir la app, alerta automática, notificaciones y consumo medido.
- [ ] Panel web, integración (400) y ensayo de la demo.

## 6. Pruebas en los teléfonos

- **Sin ADB:** el prototipo visual (H1), el prototipo BLE, la demo A → B → C, la brújula, el medidor de movimiento y el consumo. Basta con instalar el APK de depuración.
- **Con ADB** (USB o depuración inalámbrica): las pruebas automáticas en dispositivo de la 100, la 102, la 108 y la 109 (`connectedDebugAndroidTest`). Alcanza con un teléfono conectado de cualquier persona.
