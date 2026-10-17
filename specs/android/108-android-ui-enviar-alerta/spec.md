# 108 android-ui-enviar-alerta

Capa: 3 · Persona: P2
Depende de: 100, 101, 102, 105, 107 (solo interfaces)
Entrega: pestaña **Enviar alerta**: formulario, botón de mantener presionado, pantalla "Mi alerta" y cuenta regresiva del movimiento brusco, con sus `UiState`, ViewModels y textos
Consume: `EmergencyType`, `Vulnerable` y `StatusCode` (101); `OwnPacketRepository` (102); `LocationSource` (105); `MeshStatus` y `AutoAlertController` (107); componentes, formato y convenciones (100)

## Objetivo

Que una persona afectada pida ayuda en pocos toques, o sin tocar nada tras un movimiento brusco, y vea con claridad qué pasó con su alerta: guardada, anunciándose, retransmitida, subida y con voluntarios en camino.

## Alcance

- Formulario: tipo de emergencia, personas, vulnerables y ubicación.
- Envío con botón de mantener presionado.
- Pantalla "Mi alerta": estado de una alerta propia.
- Cuenta regresiva de la alerta automática dentro de la app.
- Catálogo completo de variables visibles y mensajes (100, sección 9), del que sale el design brief.
- Textos de las notificaciones de la cuenta regresiva (las publica la 107).

## Fuera de alcance

- Detectar el movimiento brusco (105) y escucharlo con la app cerrada (107).
- Guardar, anunciar y subir: la 108 guarda con la 102, y la 107 y la 106 hacen el resto (sección 7).
- Colores, tipografía y espaciado finales (Figma). Aquí solo se fija el diseño temporal (100, sección 10).
- Cancelar o marcar a salvo: es **opcional** (000). Su contenido se describe, rotulado como opcional, para que el diseño lo contemple.

## 1. Configuración

`features/sendalert/SendAlertConfig.kt` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `SEND_HOLD_MS` | 1500 | tiempo de mantener presionado para enviar |
| `LOCATION_STALE_WARN_MS` | 300000 (5 min) | a partir de aquí, la ubicación usada se marca como antigua |

## 2. Navegación de la pestaña

```
Enviar alerta
 ├─ Formulario  (si no hay una alerta propia activa)
 ├─ Mi alerta   (tras enviar; también si hay una alerta propia activa al abrir)
 │    └─ "Enviar otra alerta" → Formulario
 └─ Cuenta regresiva (capa a pantalla completa, sobre cualquier pestaña, cuando la 107 la inicia)
```

Una alerta propia está **activa** si no está resuelta y tiene menos de `maxAgeMs` (001).

## 3. Formulario

### 3.1 Estructura temporal
1. `ScreenScaffold` con el título y un `StatusChip` del estado de la malla.
2. `SectionCard` "¿Qué necesitas?": tres opciones grandes de tipo, una por fila, con texto y color.
3. `SectionCard` "¿Cuántas personas?": botones − y + y el número en el centro.
4. `SectionCard` "¿Hay personas vulnerables?" (opcional): cuatro opciones que se activan y desactivan.
5. `SectionCard` "Tu ubicación": estado, precisión y antigüedad.
6. Botón de mantener presionado, fijo abajo y fuera del desplazamiento, para que siempre se vea.

### 3.2 Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `selectedType` | usuario | `EmergencyType` o nulo | `TRAPPED_OR_SEVERE` | ninguna opción marcada |
| `people` | usuario | 1-255; mostrado con la regla de personas (100) | `3 personas` | `1 persona` (valor inicial) |
| `vulnerable` | usuario | 4 booleanos | niños, heridos | ninguno marcado |
| `location.state` | `LocationSource` (105) | `SEARCHING`, `FIX`, `LAST_KNOWN`, `DISABLED`, `NO_PERMISSION` o `UNAVAILABLE` | `FIX` | `SEARCHING` |
| `location.accuracyM` | 105 | `±15 m` | `±15 m` | `—` |
| `location.ageMs` | `nowMs - fixTimeMs` | tiempo transcurrido (100) | `hace 3 min` | `—` |
| `location.isStale` | `ageMs > LOCATION_STALE_WARN_MS` | booleano | `false` | — |
| `location.coordinates` | `latE7`, `lonE7` | 5 decimales con punto | `-33.50000, -70.65000` | `—` |
| `meshState` | `MeshStatus` (107) | ver mensajes | `RUNNING` | `STARTING` |
| `canSend` | `selectedType != null` y `location.state ∈ {FIX, LAST_KNOWN}` y no se está enviando | booleano | `true` | `false` |
| `holdProgress` | mientras se mantiene presionado | 0-100 % | `60 %` | `0 %` |
| `sendError` | resultado al guardar | ver mensajes | `null` | `null` |

### 3.3 Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `sendalert_title` | siempre | barra superior | — | Pedir ayuda | — |
| `sendalert_type_header` | siempre | tarjeta de tipo | — | ¿Qué necesitas? | — |
| `sendalert_type_food_water` | opción 0 | tarjeta de tipo | — | Comida o agua | seleccionar |
| `sendalert_type_medical` | opción 1 | tarjeta de tipo | — | Asistencia médica | seleccionar |
| `sendalert_type_trapped` | opción 2 | tarjeta de tipo | — | Atrapado o situación grave | seleccionar |
| `sendalert_people_header` | siempre | tarjeta de personas | — | ¿Cuántas personas necesitan ayuda? | − / + |
| `sendalert_people_max` | `people = 255` | tarjeta de personas | INFO | 255 o más personas | — |
| `sendalert_vulnerable_header` | siempre | tarjeta de vulnerables | — | ¿Hay personas vulnerables? (opcional) | — |
| `sendalert_vulnerable_children` / `_elderly` / `_injured` / `_mobility` | siempre | opciones | — | Niños · Adultos mayores · Personas heridas · Movilidad reducida | activar / desactivar |
| `sendalert_location_header` | siempre | tarjeta de ubicación | — | Tu ubicación | — |
| `sendalert_location_searching` | `SEARCHING` | tarjeta de ubicación | INFO | Buscando tu ubicación… | — |
| `sendalert_location_fix` | `FIX` | tarjeta de ubicación | SUCCESS | Ubicación lista · ±15 m | — |
| `sendalert_location_last_known` | `LAST_KNOWN` y no `isStale` | tarjeta de ubicación | INFO | Se usará tu última ubicación (±45 m, hace 3 min). | — |
| `sendalert_location_stale` | `LAST_KNOWN` e `isStale` | tarjeta de ubicación | WARNING | Tu última ubicación es de hace 40 min. Si te moviste, puede no ser exacta. | — |
| `sendalert_location_disabled` | `DISABLED` | tarjeta de ubicación | ERROR | La ubicación del teléfono está desactivada. Sin ubicación no se puede enviar la alerta. | Activar |
| `sendalert_location_no_permission` | `NO_PERMISSION` | — | — | (vuelve a la pantalla de permisos, 100) | — |
| `sendalert_location_unavailable` | `UNAVAILABLE`: sin ubicación actual ni anterior | tarjeta de ubicación | ERROR | Todavía no hay ninguna ubicación. Sal a un lugar abierto si puedes; la alerta se podrá enviar en cuanto aparezca. | — |
| `sendalert_hold_to_send` | `canSend` | botón | — | Mantén presionado para enviar | mantener `SEND_HOLD_MS` |
| `sendalert_hold_release_early` | se soltó antes de tiempo | bajo el botón | INFO | Mantén presionado hasta que se llene. | — |
| `sendalert_cannot_send_type` | `selectedType = null` | bajo el botón | INFO | Elige qué necesitas para poder enviar. | — |
| `sendalert_cannot_send_location` | sin `FIX` ni `LAST_KNOWN` | bajo el botón | WARNING | Falta tu ubicación para poder enviar. | — |
| `sendalert_sending` | guardando | botón | INFO | Guardando… | — |
| `sendalert_error_storage` | la 102 falló al guardar | banner | ERROR | No se pudo guardar la alerta en el teléfono. Inténtalo de nuevo. | Reintentar |
| `sendalert_error_counter` | contador agotado (101) | banner | ERROR | Este teléfono alcanzó el máximo de alertas. Reinstala la app para seguir. | — |
| `sendalert_error_invalid` | la 101 rechazó un campo | banner | ERROR | Hay un dato inválido en la alerta. Revisa el formulario. | — |
| `sendalert_motion_on` | detección de movimiento activa (ajuste en 109) | encabezado | INFO | Si detectamos un movimiento brusco, te preguntaremos antes de enviar una alerta. | — |

## 4. Mi alerta

### 4.1 Estructura temporal
1. Encabezado con el tipo (texto y color), las personas y la hora.
2. `SectionCard` "Qué pasó con tu alerta": una línea de progreso con un estado por fila (guardada, anunciándose, retransmitida, subida).
3. `SectionCard` "Ayuda": número de voluntarios en camino, destacado.
4. `SectionCard` "Detalles": vulnerables, ubicación usada y clave de la alerta.
5. Acciones: "Enviar otra alerta" (secundaria). Si STATUS está implementado: "Estoy a salvo" (principal) y "Cancelar alerta" (secundaria), ambas con `ConfirmDialog`.

### 4.2 Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `type`, `people`, `vulnerable`, `automatic` | registro propio (102) | como en el formulario; `automatic` como etiqueta | `Atrapado o grave · 3 personas · Automática` | — |
| `createdAtMs` | 102 | hora y tiempo transcurrido (100) | `07:33 · hace 5 min` | — |
| `alertKey` | 101 | clave canónica | `a1b2c3d4e5f6-0001` | — |
| `locationUsed` | 102 | precisión y antigüedad al enviar | `±15 m` | `—` |
| `saved` | 102 | siempre verdadero si se muestra | ✓ | — |
| `announceUntilMs` | 102 (lo escribe la 107) | cuenta atrás `m:ss` mientras `now < announceUntilMs` | `0:42` | "Esperando para anunciar" |
| `echoCount` | 102 (lo escribe la 107) | número de `relayTag` distintos que la retransmitieron | `2 teléfonos` | `0` |
| `uploadedAtMs` | 102 (lo escribe la 106) | hora | `07:36` | no subida |
| `volunteers` | 102 | entero | `2` | `0` |
| `latestStatus` | 102 (opcional) | `CANCELLED` o `SAFE` | `SAFE` | `null` |
| `meshState` | `MeshStatus` (107) | ver mensajes | `RUNNING` | — |

### 4.3 Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `sendalert_mine_title` | siempre | barra superior | — | Tu alerta | — |
| `sendalert_mine_saved` | siempre | progreso | SUCCESS | Guardada en tu teléfono | — |
| `sendalert_mine_waiting` | sin `announceUntilMs` | progreso | INFO | Preparando el anuncio… | — |
| `sendalert_mine_announcing` | `now < announceUntilMs` | progreso | INFO | Anunciándose a teléfonos cercanos · 0:42 | — |
| `sendalert_mine_announce_ended` | `now ≥ announceUntilMs` y `echoCount = 0` | progreso | WARNING | El anuncio terminó y ningún teléfono cercano la retransmitió todavía. | — |
| `sendalert_mine_echoed` | `echoCount > 0` | progreso | SUCCESS | Retransmitida por 2 teléfonos cercanos | — |
| `sendalert_mine_uploaded` | `uploadedAtMs != null` | progreso | SUCCESS | Este teléfono la subió al servidor a las 07:36 | — |
| `sendalert_mine_not_uploaded` | `uploadedAtMs = null` | progreso | INFO | Se subirá al servidor cuando este teléfono u otro tengan internet. | — |
| `sendalert_mine_volunteers_none` | `volunteers = 0` | tarjeta de ayuda | INFO | Aún nadie avisó que va en camino. | — |
| `sendalert_mine_volunteers` | `volunteers > 0` | tarjeta de ayuda | SUCCESS | 2 personas van en camino | — |
| `sendalert_mine_mesh_bt_off` | `meshState = BLUETOOTH_OFF` | banner | ERROR | Tu alerta está guardada, pero no se está anunciando: el Bluetooth está apagado. | Encender |
| `sendalert_mine_mesh_failed` | `meshState ∈ {ADVERTISE_UNSUPPORTED, ADVERTISE_FAILED}` | banner | ERROR | Tu alerta está guardada, pero este teléfono no puede anunciarla. Pide a alguien cercano que abra la app. | — |
| `sendalert_mine_mesh_stopped` | `meshState = STOPPED` | banner | WARNING | La malla está detenida. Actívala en Estado para anunciar tu alerta. | Ir a Estado |
| `sendalert_mine_another` | siempre | botón secundario | — | Enviar otra alerta | → Formulario |
| `sendalert_mine_safe` | opcional | botón principal | — | Estoy a salvo | `ConfirmDialog` |
| `sendalert_mine_cancel` | opcional | botón secundario | — | Cancelar alerta | `ConfirmDialog` |
| `sendalert_mine_confirm_safe` | al tocar "Estoy a salvo" | diálogo | WARNING | ¿Confirmas que ya estás a salvo? Los voluntarios dejarán de ver tu alerta como pendiente. No se puede deshacer. | Confirmar · Volver |
| `sendalert_mine_confirm_cancel` | al tocar "Cancelar alerta" | diálogo | WARNING | ¿Cancelar la alerta? Úsalo si la enviaste por error. No se puede deshacer. | Cancelar alerta · Volver |
| `sendalert_mine_resolved_safe` / `_cancelled` | `latestStatus` | encabezado | SUCCESS / INFO | Marcaste que estás a salvo · Cancelaste esta alerta | — |

## 5. Cuenta regresiva (alerta automática)

### 5.1 Estructura temporal
Capa a pantalla completa, sobre cualquier pestaña:
1. Título grande.
2. Número de la cuenta regresiva, muy grande, en el centro.
3. Resumen de lo que se enviará.
4. Botón "Cancelar", grande y ancho, abajo.

### 5.2 Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `countdown.state` | `AutoAlertController` (107) | `COUNTING`, `WAITING_LOCATION`, `SENT`, `CANCELLED` o `FAILED` (en la 107: `Counting`, `WaitingLocation`, `Sent`, `Cancelled`, `Failed`). La duración (`AUTO_COUNTDOWN_MS`, 15 s) vive en la 107 | `COUNTING` | — |
| `countdown.remainingMs` | 107 | segundos (100) | `12` | — |
| `location` | `LocationSource` (105) | como en el formulario | `±15 m` | `—` |

### 5.3 Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `sendalert_auto_title` | `COUNTING` | capa | WARNING | Detectamos un movimiento brusco | — |
| `sendalert_auto_body` | `COUNTING` | capa | — | Si no cancelas, enviaremos una alerta de emergencia en 12 s. | — |
| `sendalert_auto_summary` | `COUNTING` | capa | INFO | Se enviará: Atrapado o grave · 1 persona · tu ubicación | — |
| `sendalert_auto_cancel` | `COUNTING` o `WAITING_LOCATION` | botón | — | Cancelar, estoy bien | cancela |
| `sendalert_auto_waiting_location` | terminó la cuenta y no hay ubicación | capa | WARNING | Esperando tu ubicación para enviar la alerta… | Cancelar |
| `sendalert_auto_sent` | `SENT` | capa, luego "Mi alerta" | SUCCESS | Alerta enviada | — |
| `sendalert_auto_cancelled` | `CANCELLED` | aviso breve | INFO | Alerta cancelada. No se envió nada. | — |
| `sendalert_auto_failed` | `FAILED`: no se pudo guardar la alerta automática | capa | ERROR | No se pudo guardar la alerta automática. Envíala a mano desde el formulario. | Ir al formulario |

### 5.4 Notificaciones (las publica la 107; el texto se fija aquí)

| id | Cuándo | Título | Texto | Acciones |
|---|---|---|---|---|
| `sendalert_notif_countdown` | cuenta regresiva con la app en segundo plano | Movimiento brusco detectado | Se enviará una alerta en 12 s. | Cancelar · Abrir |
| `sendalert_notif_sent` | se envió la alerta automática | Alerta enviada | Tu alerta de emergencia se está anunciando. | Abrir |
| `sendalert_notif_volunteer` | llega un `ON_THE_WAY` para una alerta propia | Ayuda en camino | 1 persona va en camino a tu alerta. | Abrir |

## 6. Comportamiento

1. Al completarse `SEND_HOLD_MS`, el ViewModel arma un `AlertDraft` (102) con el formulario y la ubicación `FIX` o `LAST_KNOWN`, y llama a `OwnPacketRepository.saveNewAlert`. La 102 asigna el contador, crea el paquete con `PacketFactory.newAlert` (101) y lo guarda, todo en una transacción (regla 21). Sus errores (`StorageFailed`, `CounterExhausted`, `InvalidField`) son los de `sendalert_error_*`.
2. Si se guarda bien, navega a "Mi alerta". La 108 **no** llama al motor ni al BLE: la 107 toma de la 102 los paquetes propios pendientes de anunciar.
3. "Mi alerta" observa el registro propio en la 102 y `MeshStatus`. El tiempo restante se recalcula con el `Clock` (100) una vez por segundo, solo mientras la pantalla está visible.
4. La cuenta regresiva la lleva la 107 (sigue con la app cerrada). La 108 solo la muestra y llama a `cancel()`.
5. Ningún dato de la alerta se muestra con texto libre: todo sale de campos de la 001 (regla 24).

## 7. Requisitos para otras specs

La 108 se implementa con los fakes de estas interfaces. Cada dueño las incluye en su spec (se registran como `[PIDE]` en la memoria).

| Spec | Interfaz | Debe ofrecer |
|---|---|---|
| 102 | `OwnPacketRepository` | **resuelto en la 102, sección 1.1:** `saveNewAlert(draft, nowMs)`, `observeOwnAlerts()` (`OwnAlertView`) y `saveStatus(...)` (opcional) |
| 105 | `LocationSource` | **resuelto en la 105, sección 2:** `highAccuracy()` mientras el formulario está visible. Equivalencias: `Fresh` = `FIX`, `LastKnown` = `LAST_KNOWN`, `Searching` = `SEARCHING`, `Unavailable` = `UNAVAILABLE`, `Disabled` = `DISABLED`, `NoPermission` = `NO_PERMISSION` |
| 107 | `MeshStatus` | **resuelto en la 107, sección 2:** `state.health` (`MeshHealth`) |
| 107 | `AutoAlertController` | **resuelto en la 107, secciones 2 y 5** |
| 107 | escritura en la 102 | **resuelto en la 107, sección 3:** `markOriginated` y `recordEcho` |
| 106 | escritura en la 102 | `uploadedAtMs` al subir un paquete propio |

## Criterios de aceptación

1. Con `selectedType = null` o sin ubicación usable, el botón no envía y muestra el mensaje correspondiente.
2. Soltar antes de `SEND_HOLD_MS` no envía y muestra `sendalert_hold_release_early`. Mantenerlo `SEND_HOLD_MS` guarda exactamente una alerta (prueba con `FakeClock` y fakes de 102 y 105).
3. La alerta guardada tiene los campos del formulario, `automatic = false` y la hora redondeada a segundos (101).
4. Cada estado de ubicación de la sección 3.2 muestra su mensaje (prueba de Compose con un `LocationSource` falso).
5. "Mi alerta" muestra cada fila de progreso y cada banner de malla según el registro y `MeshState` (prueba de Compose con fakes).
6. La cuenta regresiva muestra los segundos restantes, "Cancelar" llama a `cancel()`, y `WAITING_LOCATION` y `SENT` muestran su mensaje.
7. Cada `id` de las secciones 3.3, 4.3, 5.3 y 5.4 existe en `strings.xml`. Lo comprueba una prueba que compara con una lista.
8. Ninguna pantalla de la 108 llama a `System.currentTimeMillis()`; el tiempo sale del `Clock` (100).

## Tareas

1. `SendAlertConfig` y los textos de `strings.xml`.
2. `UiState` y ViewModel del formulario, con sus pruebas (criterios 1-3).
3. Pantalla del formulario con los componentes de la 100 y el botón de mantener presionado (criterio 4).
4. `UiState`, ViewModel y pantalla de "Mi alerta" (criterio 5).
5. Capa de cuenta regresiva (criterio 6).
6. Prueba del catálogo de textos (criterio 7).
7. Registrar en `memory/P2.md` los pedidos de la sección 7.
