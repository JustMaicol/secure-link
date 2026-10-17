# Plan · 107 android-servicio-primer-plano

Spec: `specs/android/107-android-servicio-primer-plano/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P1**. Contexto: `specs/plan-prioridades-android.md`, fases 0, 2 y 3.
Carpetas: `core/**/node/`, `app/**/service/`, y además `AppContainer.kt`, `SecureLinkApp.kt` y `AndroidManifest.xml` (dueño P1).
**No toca:** `MainActivity.kt`, `navigation/` ni `strings.xml` (P2). Los textos de las notificaciones ya existen en los catálogos de la 108 y la 109, y los escribe P2.

## 1. Tres entregas

| Entrega | Fase | Para qué |
|---|---|---|
| **Contrato** | 0.2 | que P2 construya 108 y 109 sobre fakes, y que `MainActivity` arranque el servicio y abra pantallas desde notificaciones |
| **Nodo básico** | 2 | hito **H2**: alerta de A llega a C con 3 teléfonos |
| **Mejoras** | 3 | hito **H3**: alerta automática, notificaciones, energía y subida |

## 2. Pasos

### Fase 0.2 · contrato

| Paso | Qué | Verificación |
|---|---|---|
| **S0** | `core/node`: `MeshHealth`, `PowerMode`, `MeshState`, `MeshStatus`, `MeshControl`, `CountdownState`, `AutoAlertController` y sus fakes (sección 2); `NodeConfig` (sección 1). `app/service`: `MeshServiceStarter.ensureStarted(context)` que por ahora **no hace nada**, y `NotificationRoutes` con los extras `route` (`nearby_detail`, `my_alert`, `countdown`) y `alert_key` | compila; los fakes permiten cambiar el estado en pruebas de P2 |

### Fase 2 · nodo básico (sin energía, sin alerta automática, sin notificaciones de alertas)

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **S1** · documentación | Revisar la documentación oficial de Android 15, 16 y 17 sobre servicios en primer plano (tipos, inicio desde segundo plano, `START_STICKY`) y BLE en segundo plano. Registrar cada cambio como `[HALLAZGO]` en `memory/P1.md`. Si algo contradice la spec, avisar antes de seguir | hallazgos registrados |
| **S2** · `MeshNode` | Un solo hilo (`limitedParallelism(1)`). Arranque (sección 3.1). Entradas: recibidos, bandeja de salida, temporizador de `nextWakeupMs` y `blockedRelayTags` (sección 3.2). Efectos (sección 3.3), incluido **no anunciar si guardar falla**. Ecos. Teléfonos cerca. Orden de los anuncios. `TransportState` → `MeshHealth` | criterios 1-7 y 11, con `FakeMeshTransport`, `InMemoryStorage` (P3) y tiempo virtual |
| **S3** · `MeshService` | Servicio `connectedDevice\|location` declarado en el manifiesto; `startForeground` con la notificación del servicio (`status_notif_service`, `status_notif_service_bt_off`); `START_STICKY`; `MeshServiceStarter` real (respeta `meshEnabled`); `MeshControl` (detener y reanudar) | la app abre, la notificación aparece y la malla se apaga y enciende desde Estado |
| **S4** · cableado real | En `AppContainer`, cambiar los fakes por las implementaciones reales a medida que P3 las fusiona: `PacketStore`, `OwnPacketRepository` y `SettingsRepository` (102), `BleMeshTransport` (104) y `LocationSource` (105) | la app funciona con lo real; las pruebas JVM siguen con fakes |
| **S5** · hito H2 | 3 Samsung con el APK de depuración y el filtro de depuración (C bloquea a A): alerta en A → aparece en C con distancia y rumbo → C avisa "voy en camino" y A lo ve | registrado en `memory/P1.md` (modelos, tiempos, problemas) |

### Fase 3 · mejoras

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **S6** · energía | `PowerPolicy` pura; cambios de modo con `MODE_MIN_DWELL_MS`; `setScanMode` (104); wake lock solo con anuncios activos y con tiempo límite; `AppVisibilityPort` con `ProcessLifecycleOwner` | criterio 8 |
| **S7** · alerta automática | `AutoAlertController` (sección 5): cuenta regresiva, vibración, alta precisión solo durante la cuenta, espera de ubicación sin límite, reintentos y `Failed` | criterio 9 |
| **S8** · notificaciones | Canales; alerta cercana nueva (una por alerta, agrupadas, sonido una vez por minuto, nunca con la app visible); ayuda en camino; cuenta regresiva y enviada; destinos con `NotificationRoutes` | criterios 10 y 14 |
| **S9** · subida | `SyncRunner.runOnce()` al pasar a con internet, al crecer la cola y al vencer el reintento; `SyncScheduler.ensureScheduled()` en `SecureLinkApp` | criterio 12 con `FakeSyncRunner` |
| **S10** · dispositivo | Demo con **apps cerradas y pantallas apagadas**; notificación con la app cerrada; **consumo** de 1 h en reposo y 1 h con anuncios; quitar de recientes y "Forzar detención" | criterios 13, 15 y 16, registrados en `memory/P1.md`. Con la medición de consumo se acuerda el objetivo con el equipo |

**Orden:** S0 (fase 0) → S1 → S2 → S3 → S4 → S5 (fase 2) → S6 → S7 → S8 → S9 → S10 (fase 3). En la fase 3, S6-S9 son independientes entre sí: pueden ir en PR separados.

## 3. Dependencias

| Necesita | De quién | Cuándo | Si no está |
|---|---|---|---|
| `GossipEngine` | P1 (103) | fase 2 | — |
| Contratos de 102, 104 y 105 (`InMemoryStorage`, `FakeMeshTransport` con `setScanMode`, `FakeLocationSource`, `FakeMotionSource`) | P3 | fase 0 | S2 no puede probarse: pedir con `[PIDE a P3]` |
| Implementaciones reales de 102, 104 y 105 | P3 | fase 2 | S4 deja ese fake y lo registra; S5 espera |
| `PermissionChecker` real | P2 (100-B) | fase 1 | se usa el fake (100, paso A3') |
| `SyncRunner` + fake | P2 (106) | fase 0 | S9 lo necesita: pedir con `[PIDE a P2]` |
| Textos de notificaciones en `strings.xml` | P2 (108, 109) | fase 1 | S3 y S8 los necesitan: pedir con `[PIDE a P2]`, nunca escribirlos |
| `MainActivity` llama a `MeshServiceStarter` y lee `NotificationRoutes` | P2 (100-B, B5) | fase 1 | el servicio no arranca solo: pedir con `[PIDE a P2]` |

## 4. Terminado

- **Nodo básico (fase 2):** criterios 1-7 y 11, S3, S4 y el hito H2 registrado.
- **Spec completa (fase 3):** criterios 1-16, PR fusionados, y `memory/P1.md` marca la 107 como `terminada` con el commit `spec 107: …`.

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| Android 15-17 cambió las reglas de servicios en primer plano o BLE | S1 va primero; si hay contradicción, se para y se revisa la spec |
| Samsung mata el servicio con la pantalla apagada | Pasos de la pantalla de permisos (100) y prueba S10; registrar el modelo y la versión de One UI |
| Carreras entre corrutinas | Todo el estado del nodo en un solo hilo (sección 3) |
| El consumo de batería es alto | La política de energía (S6) y la medición de S10 antes de la demo |
| P3 se atrasa con BLE real | S2 y S3 avanzan con fakes; H2 espera, pero H1 (prototipo visual) no depende de esto |
