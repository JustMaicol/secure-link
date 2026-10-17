# Plan · 106 android-sync-supabase

Spec: `specs/android/106-android-sync-supabase/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P2**. Contexto: `specs/plan-prioridades-android.md`, fase 0 (contrato) y fase 3 (implementación).
Carpetas: `core/**/sync/` y `app/**/sync/`.

## 1. Lugar en el plan

| Entrega | Fase | Para qué |
|---|---|---|
| **Contrato** | 0.2 | P1 dispara la subida con `SyncRunner` (107) y la pantalla Estado del prototipo usa `SyncStatus` (109) |
| **Implementación** | 3 | hito **H3**: las alertas se suben solas al recuperar internet, con la app cerrada |

Entre la fase 1 y la 3, si P2 termina antes, puede adelantar Y1-Y3, que son puros y no necesitan servidor.

## 2. Pasos

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **Y0** · contrato (fase 0) | `SyncRunner` + `FakeSyncRunner`; `SyncStatus` + `FakeSyncStatus`; `SyncState`, `SyncError` y `RunResult` (secciones 4 y 5) | compila; P1 y la 109 lo usan con fakes |
| **Y1** · filas | `SyncConfig`, `Table` y `toRow` (sección 2). Nunca `received_at_ms`, `alert_id` ni `relayTag` | criterio 1: el JSON exacto del ejemplo |
| **Y2** · reintentos | `RetryPolicy` pura: 5 s, 10 s, 20 s… hasta 5 min; 15 min ante `AUTH`; reinicio al volver internet | criterio 5 (parte pura) |
| **Y3** · motor | `SyncEngine` con `HttpPort` y `ConnectivityPort` falsos: lotes de 100 por tabla en orden, división del lote para aislar una fila rechazada (`markRejected`), `Mutex`, `SyncState` | criterios 2-9 con tiempo virtual (`kotlinx-coroutines-test`) |
| **Y4** · puertos reales | `OkHttpPort` (cabeceras de la 002, sección 6; tiempos límite; excepciones traducidas) y `AndroidConnectivityPort` (solo internet **validado**) | prueba de `OkHttpPort` con un servidor HTTP falso (`MockWebServer`, si P1 agrega la dependencia) o en dispositivo |
| **Y5** · WorkManager | `UploadWorker` (`CoroutineWorker`) y `SyncScheduler.ensureScheduled()` con `enqueueUniquePeriodicWork(KEEP)`, condición de red y 15 min | el worker queda programado tras reiniciar (manual) |
| **Y6** · pruebas reales | Contra el proyecto Supabase de P3 (200 mínima): alerta sin internet → internet con la app cerrada → aparece una vez en la base. Otro teléfono con la misma alerta → no se duplica. Reinicio sin abrir la app → la sube el worker | criterios 10 y 11, registrados en `memory/P2.md` |

**Orden:** Y0 (fase 0) → Y1 → Y2 → Y3 → Y4 → Y5 → Y6 (fase 3).

## 3. Dependencias

| Necesita | De quién | Cuándo | Si no está |
|---|---|---|---|
| `UploadQueue` con `markRejected` + fake | P3 (102) | fase 0 | pedir con `[PIDE a P3]` |
| `WirePacket`, `PacketKey` | P1 (101, C1) | fase 0 | pedir con `[PIDE a P1]` |
| `Clock`, `DeviceIdentity`, `SUPABASE_URL` y `SUPABASE_ANON_KEY` en `BuildConfig` | P1 (100-A) | fase 0 | pedir con `[PIDE a P1]` |
| Dependencias `okhttp`, `work-runtime-ktx` y `kotlinx-serialization` en `:app` (y `mockwebserver` para pruebas, si se usa) | P1 (catálogo) | antes de Y4 | pedir con `[PIDE a P1]`; nunca editar el catálogo |
| Crear `SyncEngine` en `AppContainer`, `ensureScheduled()` al iniciar y disparar `runOnce` | P1 (107, S9) | fase 3 | pedir con `[PIDE a P1]` |
| Proyecto Supabase con la migración de la 002, clave `anon` y usuario autoridad | P3 (200 mínima) | fase 3 | Y1-Y5 avanzan; Y6 espera |

## 4. Terminado

Criterios 1-11, PR fusionados, y `memory/P2.md` marca la 106 como `terminada` con el commit `spec 106: …`.

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| `ignore-duplicates` sin `select` no funciona para `anon` (pendiente de la 002) | P3 lo verifica en la 200. Si falla, la 002 cambia a una función `SECURITY DEFINER` y solo cambia `OkHttpPort` |
| La clave `anon` termina en el repositorio | Solo en `local.properties` (ignorado por git, regla 25) |
| WorkManager se retrasa en Samsung | Es solo el respaldo; el camino principal es el servicio. Registrar el retraso observado en Y6 |
