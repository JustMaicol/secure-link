# Plan · 108 android-ui-enviar-alerta

Spec: `specs/android/108-android-ui-enviar-alerta/spec.md` (manda la spec; si este plan la contradice, se corrige el plan).
Persona: **P2**. Contexto: `specs/plan-prioridades-android.md`, fase 1 (prototipo visual).
Carpetas: `app/**/features/sendalert/`, `app/**/prototype/` (compartida con la 109, ver sección 4) y los textos `sendalert_*` de `strings.xml`.
Diseño: `specs/design/android-design-brief.md`, pantallas E1, E2, E3 y notificaciones N/02-N/04.

## 1. Lugar en el plan

- Se construye **entera sobre fakes** en la fase 1. No espera ninguna implementación real.
- Es parte del hito **H1 (prototipo visual)**: el APK debe mostrar todos los frames E1, E2 y E3 del brief.
- En la fase 2 no cambia el código de la 108: P1 cambia los fakes por las implementaciones reales en `AppContainer`.

## 2. Pasos

| Paso | Qué | Verificación (criterios de la spec) |
|---|---|---|
| **U1** · textos y configuración | `SendAlertConfig` (`SEND_HOLD_MS`, `LOCATION_STALE_WARN_MS`). **Todos** los `id` de las secciones 3.3, 4.3, 5.3 y 5.4 en `strings.xml`, incluidos los de notificaciones (`sendalert_notif_*`), que usa P1 en la 107, y `sendalert_auto_failed` | criterio 7 (prueba que compara con la lista) |
| **U2** · formulario: estado | `SendAlertUiState` con las variables de la sección 3.2. ViewModel que recolecta `LocationSource.highAccuracy()` solo con la pantalla visible, lleva el progreso del botón con el `Clock` y, al completarse, arma `AlertDraft` y llama a `OwnPacketRepository.saveNewAlert`. Traduce `StorageFailed`, `CounterExhausted` e `InvalidField` a `sendalert_error_*` | criterios 1-3 con `FakeClock`, `InMemoryStorage` y `FakeLocationSource` |
| **U3** · formulario: pantalla | Estructura de la sección 3.1 con los componentes de la 100. Botón de **mantener presionado** con progreso y vibración al completarse (componente propio en `features/sendalert/`, no en `ui/`). Fijo abajo, fuera del desplazamiento | criterio 4 (Compose con cada estado de ubicación) |
| **U4** · Mi alerta | `MyAlertUiState` desde `OwnAlertView` (102) y `MeshStatus` (107). Progreso de 4 pasos, ayuda, detalles y banners de malla. Cuenta atrás recalculada cada segundo con el `Clock`, solo con la pantalla visible. "Enviar otra alerta". Los botones de STATUS existen pero ocultos tras una bandera (opcional, 000) | criterio 5 |
| **U5** · cuenta regresiva | Capa a pantalla completa sobre cualquier pestaña (se monta en el contenedor de navegación, que es de P2). Dibuja `CountdownState` de `AutoAlertController` (107); "Cancelar, estoy bien" llama a `cancel()`. Estados `Counting`, `WaitingLocation`, `Sent`, `Cancelled` y `Failed` | criterio 6 con `FakeAutoAlertController` |
| **U6** · reglas | Prueba que busca `System.currentTimeMillis` en `features/sendalert/` | criterio 8 |
| **U7** · frames del prototipo | Registrar en la galería (sección 4) un estado por frame del brief: `E1/01`-`E1/11`, `E2/01`-`E2/12`, `E3/01`-`E3/05` | todos visibles en la galería |

**Orden:** U1 → U2 → U3 → U4 → U5 → U6 → U7. U1 va en el primer PR: P1 necesita los textos de las notificaciones.

## 3. Dependencias (todas por contrato de la fase 0)

| Necesita | De quién | Si no está |
|---|---|---|
| Componentes, formato, tema y navegación | P2 (100-B) | es del mismo P2: B2-B4 van antes que U3 |
| `EmergencyType`, `Vulnerable`, `AlertKey` | P1 (101, C1) | pedir con `[PIDE a P1]` |
| `OwnPacketRepository`, `AlertDraft`, `OwnAlertView`, `InMemoryStorage` | P3 (102) | pedir con `[PIDE a P3]` |
| `LocationSource` + fake | P3 (105) | pedir con `[PIDE a P3]` |
| `MeshStatus`, `AutoAlertController` + fakes | P1 (107, S0) | pedir con `[PIDE a P1]` |
| `Clock`, `FakeClock` | P1 (100-A) | — |

## 4. Prototipo visual (compartido con la 109)

En `app/src/debug/.../prototype/` (solo depuración, dueño P2):
- **Datos de prueba:** los de la sección 8 del design brief (teléfono `a1b2 c3d4 e5f6`, mi alerta y las 5 alertas cercanas), cargados en `InMemoryStorage` y en los fakes de 105 y 107.
- **Galería de frames:** una pantalla que lista los frames del brief por código (`E1/03`, `C2/04`…) y abre cada uno con su `UiState` exacto. Como las pantallas solo dibujan su `UiState`, cada frame es una entrada de la lista.
- **Rótulo** "PROTOTIPO · datos de prueba" visible siempre que se usen estos datos (regla 30).
- `AppContainer` (P1) usa los fakes en la fase 0; la galería solo necesita los `UiState`.

## 5. Terminado

- **Hito H1:** U1-U5 y U7 en el APK de depuración, comparados con Figma.
- **Spec completa:** criterios 1-8, PR fusionados, y `memory/P2.md` marca la 108 como `terminada` con el commit `spec 108: …`.

## 6. Riesgos

| Riesgo | Mitigación |
|---|---|
| Figma cambia textos o la estructura | Solo cambia `strings.xml` y la pantalla; `UiState` y ViewModel siguen igual. Avisar para actualizar la spec y el anexo del brief |
| El botón de mantener presionado se dispara mal | Prueba con `FakeClock` del borde exacto de `SEND_HOLD_MS` |
| La capa de cuenta regresiva no aparece sobre otras pestañas | Se monta en el contenedor de navegación, no dentro de la pestaña |
