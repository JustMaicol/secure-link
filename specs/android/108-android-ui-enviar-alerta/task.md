# Tasks · 108 android-ui-enviar-alerta

Spec: `108-android-ui-enviar-alerta/spec.md` · Plan: `108-android-ui-enviar-alerta/plan.md` (si algo choca, manda la spec).
Persona: **P2**. Carpetas: `app/**/features/sendalert/`, `app/src/debug/**/prototype/` y los `sendalert_*` de `strings.xml`.
Diseño: design brief, pantallas E1, E2, E3 y N/02-N/04. Anexo A con todos los textos.

Todo se construye **sobre fakes** (`InMemoryStorage`, `FakeLocationSource`, `FakeMeshStatus`, `FakeAutoAlertController`, `FakeClock`). Al terminar cada task: commit `spec 108: <qué>` y nota en `memory/P2.md`.

*Depende de:* 100-B (B2-B4) y los contratos de la fase 0 (101 C1, 102 D0, 105 E0, 107 S0).

## U1 · Textos y configuración (**primer PR**: P1 necesita los de notificaciones)

- [ ] **U1.1** `SendAlertConfig`: `SEND_HOLD_MS = 1500` y `LOCATION_STALE_WARN_MS = 300000`.
- [ ] **U1.2** Todos los `id` de las secciones 3.3, 4.3, 5.3 y 5.4 en `strings.xml`, incluidos `sendalert_notif_countdown`, `sendalert_notif_sent`, `sendalert_notif_volunteer` y `sendalert_auto_failed`. Textos del anexo A del brief.
- [ ] **U1.3** Prueba que compara los `id` de `strings.xml` con la lista de esas secciones (criterio 7).
- [ ] **U1.4** PR y aviso a P1 en `memory/P2.md`: "textos de notificaciones de la 108 listos".

## U2 · Formulario: estado y ViewModel

- [ ] **U2.1** `SendAlertUiState` con las variables de la sección 3.2 (`selectedType`, `people`, `vulnerable`, `location.*`, `meshState`, `canSend`, `holdProgress`, `sendError`).
- [ ] **U2.2** Recolectar `LocationSource.highAccuracy()` **solo con la pantalla visible**, y traducir: `Fresh` → `FIX`, `LastKnown` → `LAST_KNOWN` (con `isStale` si pasa `LOCATION_STALE_WARN_MS`), `Searching`, `Unavailable`, `Disabled` y `NoPermission`.
- [ ] **U2.3** Acciones: elegir tipo, personas (− / +, entre 1 y 255), activar o desactivar vulnerables.
- [ ] **U2.4** Botón de mantener presionado con el `Clock`: `onPress`, `onRelease` antes de tiempo (`sendalert_hold_release_early`) y completado al cumplir `SEND_HOLD_MS`.
- [ ] **U2.5** Al completarse: `AlertDraft` → `OwnPacketRepository.saveNewAlert(draft, now)`. `StorageFailed` → `sendalert_error_storage`, `CounterExhausted` → `sendalert_error_counter`, `InvalidField` → `sendalert_error_invalid`. Si sale bien, navegar a Mi alerta.
- [ ] **U2.6** Pruebas (JVM, `FakeClock` + fakes):
  - sin tipo o sin ubicación no envía (criterio 1);
  - soltar en 1499 ms no guarda y mantener 1500 ms guarda **una** alerta (criterio 2);
  - la alerta guardada tiene los campos del formulario, `automatic = false` y la hora redondeada (criterio 3).

## U3 · Formulario: pantalla

- [ ] **U3.1** Estructura de la sección 3.1 con `ScreenScaffold` y `SectionCard`: tipo (3 opciones con color **y** texto), personas, vulnerables y ubicación.
- [ ] **U3.2** `HoldToSendButton` en `features/sendalert/`: progreso de llenado, vibración al completarse, fijo abajo y fuera del desplazamiento.
- [ ] **U3.3** Mensajes de la sección 3.3 según el estado (bajo el botón y en la tarjeta de ubicación).
- [ ] **U3.4** Prueba de Compose: cada estado de ubicación muestra su mensaje (criterio 4).

## U4 · Mi alerta

- [ ] **U4.1** `MyAlertUiState` desde `OwnAlertView` (102) y `MeshState` (107), con las variables de la sección 4.2.
- [ ] **U4.2** Progreso de 4 pasos (guardada, anuncio con cuenta atrás `m:ss`, retransmitida, subida), tarjeta de ayuda y detalles. Recalcular cada segundo con el `Clock`, solo con la pantalla visible.
- [ ] **U4.3** Banners de malla: `BLUETOOTH_OFF`, `ADVERTISE_UNSUPPORTED` / `ADVERTISE_FAILED` y `STOPPED`.
- [ ] **U4.4** "Enviar otra alerta" → formulario. "Estoy a salvo" y "Cancelar alerta" detrás de una bandera **apagada** (opcional, 000), con su `ConfirmDialog`.
- [ ] **U4.5** Al abrir la pestaña: si hay una alerta propia activa, mostrar Mi alerta.
- [ ] **U4.6** Pruebas de Compose: cada fila de progreso y cada banner según el registro y el estado (criterio 5).

## U5 · Cuenta regresiva

- [ ] **U5.1** Capa a pantalla completa, montada en el contenedor de navegación (sobre cualquier pestaña).
- [ ] **U5.2** Dibujar `CountdownState`: `Counting` (segundos en grande, resumen y "Cancelar, estoy bien"), `WaitingLocation`, `Sent` (→ Mi alerta), `Cancelled` (aviso breve) y `Failed` (`sendalert_auto_failed` + "Ir al formulario").
- [ ] **U5.3** "Cancelar" llama a `AutoAlertController.cancel()`.
- [ ] **U5.4** Pruebas con `FakeAutoAlertController` para cada estado (criterio 6).

## U6 · Reglas

- [ ] **U6.1** Prueba que busca `System.currentTimeMillis` en `features/sendalert/` (criterio 8).

## U7 · Prototipo visual (galería)

- [ ] **U7.1** Si no existe: infraestructura en `app/src/debug/**/prototype/`, con una lista de frames por código y su `UiState`, y el rótulo "PROTOTIPO · datos de prueba" (compartida con la 109).
- [ ] **U7.2** Datos de prueba del brief, sección 8 (teléfono `a1b2 c3d4 e5f6` y mi alerta Atrapado o grave · 3 personas · 07:33), cargados en `InMemoryStorage`.
- [ ] **U7.3** Un `UiState` por frame: `E1/01`-`E1/11`, `E2/01`-`E2/12` y `E3/01`-`E3/05`.
- [ ] **U7.4** Instalar el APK de depuración (sin ADB) y comparar cada frame con Figma. Diferencias de texto → lista de cambios del brief (entregable 5).

## Cierre

- [ ] Criterios 1-8 pasan; U7 visto en el teléfono (parte del hito H1).
- [ ] `memory/P2.md`: 108 `terminada`.
