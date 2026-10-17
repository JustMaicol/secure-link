# Plan · 102 android-almacenamiento

Spec: `specs/android/102-android-almacenamiento/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P3**. Contexto: `specs/plan-prioridades-android.md`, fases 0 a 2.
Carpetas: `core/**/storage/`, `app/**/storage/` y `android/app/schemas/`.

## 1. Lugar en el plan

La 102 es la spec **más consumida**: la usan la 107 (P1), la 108, la 109 y la 106 (P2). Por eso su contrato y su fake tienen que ser buenos desde la fase 0.

| Entrega | Fase | Para qué |
|---|---|---|
| **Contrato + `InMemoryStorage`** | 0.2 | el prototipo visual de P2 y el nodo de P1 funcionan sobre un almacenamiento falso coherente |
| **Pruebas de contrato + Room v1** | 1 | el esquema queda fijo y exportado |
| **Room completo** | 2 | hito **H2**: las alertas se guardan de verdad y la 107 las anuncia desde la bandeja de salida |

## 2. Pasos

### Fase 0.2 · contrato

| Paso | Qué | Verificación |
|---|---|---|
| **D0** · contrato | Todas las interfaces de la sección 1: `StorageError`, `StorageResult`, `AlertDraft`, `OwnPacketRepository`, `NearbyRepository`, `ReceiveOutcome`, `PacketStore`, `PendingUpload`, `UploadQueue` (con `markRejected`), `Stats`, `StorageStats`, `SettingsRepository` (con `meshEnabled`), `OwnAlertView` y `NearbyAlertView`. **`InMemoryStorage`**, que implementa **todas** a la vez, más los fakes sueltos | compila; P2 y P1 lo usan |

**`InMemoryStorage` no usa `PacketFactory`:** en la fase 0, la fábrica de la 101 todavía es `TODO()`. El fake arma el `WirePacket` directamente con las mismas reglas que la fábrica: hora redondeada a segundos, más de 255 personas → 255, menos de 1 → `InvalidField("people")`, `ttl = ttlMax` y `relayTag` propio. Las pruebas de contrato (D1) garantizan que se comporta igual que Room.

### Fase 1 · pruebas de contrato y esquema

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **D1** · pruebas de contrato | Una clase de pruebas **abstracta** con todas las reglas de la sección 3, que recibe la implementación como parámetro. Se ejecuta contra `InMemoryStorage` en la JVM | criterios 1-8 en la JVM (criterio 9, parte JVM) |
| **D2** · Room v1 | Entidades, índices, DAO y base `secure_link.db` versión 1 (sección 2). `exportSchema = true` con el esquema en `android/app/schemas/`, versionado. Sin `fallbackToDestructiveMigration` | criterio 10 |

### Fase 2 · Room completo

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **D3** · repositorios Room | Transacción del contador + `PacketFactory` real (sección 3, reglas 1 y 2). `saveReceived` con `INSERT OR IGNORE` (la primera copia gana). Bandeja de salida y `markOriginated`. `recordEcho`. Cola de subida (`pending`, `markUploaded`, `markRejected`, `observePendingCount`). Vistas (`volunteers`, `iAmGoing`, `latestStatus`, `echoCount`) con consultas SQL. `StorageStats`. Errores de SQLite → `StorageFailed` | las pruebas de D1, ahora contra Room en memoria, en `androidTest` (criterio 9 completo) |
| **D4** · ajustes | `SettingsRepository` con `SharedPreferences` (`settings`): `motion_detection`, `mesh_enabled` y `blocked_relay_tags`, como `StateFlow` con un listener; valores inválidos se ignoran y se registran | criterio 11 |
| **D5** · pruebas en dispositivo | Concurrencia del contador (20 `saveNewAlert` en paralelo dan 1-20) y esquema exportado. **Necesitan ADB** (USB o depuración inalámbrica) | registradas en `memory/P3.md` |

**Orden:** D0 (fase 0) → D1 → D2 (fase 1) → D3 → D4 → D5 (fase 2). D3 es prioridad para el hito H2: sin ella no hay bandeja de salida real.

## 3. Dependencias

| Necesita | De quién | Cuándo | Si no está |
|---|---|---|---|
| Esqueleto que compila, Room + KSP y `room.schemaLocation` en el build | P1 (100-A, A1) | D0, D2 | D0 espera a A1; pedir con `[PIDE a P1]` |
| Modelos, claves y `ProtocolConfig` | P1 (101, C1) | D0 | pedir con `[PIDE a P1]` |
| `PacketFactory` real | P1 (101, PR fase 1 a) | D3 | D1 y D2 avanzan; D3 espera |
| `DeviceIdentity` (para saber qué es propio) | P1 (100-A, A2) | D0, D3 | pedir con `[PIDE a P1]` |
| Un teléfono con ADB | cualquiera del equipo | D5 | D3 y D4 se prueban con el fake y la app; D5 espera |

## 4. Terminado

Criterios 1-11, PR fusionados, y `memory/P3.md` marca la 102 como `terminada` con el commit `spec 102: …`.

## 5. Riesgos

| Riesgo | Mitigación |
|---|---|
| El fake y Room se comportan distinto | Las mismas pruebas de contrato contra ambos (D1 y D3) |
| Cambiar el esquema después de la v1 | Migración explícita + `MigrationTestHelper` (sección 5); nunca borrar datos |
| Consultas lentas con muchas alertas | Índices de la sección 2; con la escala de la demo no debería notarse. Medir si aparece |
| El contador se repite con guardados simultáneos | Transacción de Room y la prueba de concurrencia (D5) |
