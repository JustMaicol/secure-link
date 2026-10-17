# 202 web-lista-prioridad

Capa: 2 · Persona: P3
Depende de: 200, 201
Entrega: columna izquierda del panel: resumen, filtros, lista priorizada y detalle de una alerta, en `web/src/features/list/`
Consume: `useDashboard`, `createFakeDashboard`, componentes, formato y textos (200); `sortAlerts`, `priorityOf`, `priorityBreakdown` y `hopsOf` (201)

## Objetivo

Que la autoridad vea de un vistazo qué atender primero, entienda por qué una alerta está arriba y pueda ubicarla en el mapa.

## Alcance

- Resumen en la barra superior.
- Filtros por tipo, por zona seleccionada y de alertas resueltas.
- Lista ordenada con `sortAlerts` (201).
- Detalle de la alerta seleccionada, con el desglose de su prioridad.
- Selección compartida con el mapa (a través del store de la 200).
- Catálogo de contenido visible (convención de la 200, sección 9).

## Fuera de alcance

- Calcular la prioridad (201), el mapa (203), el resaltado de alertas nuevas (lo marca la 204; la 202 solo lo dibuja) y los datos de demo (205; la 202 solo los rotula).

## 1. Estructura temporal

```
┌─ Lista (columna izquierda) ─────────────────────────┐
│ Filtros: [Rojo] [Naranja] [Amarillo]  ☐ Resueltas    │
│ [Zona -13401:-28260 · 5 alertas ✕]   (si hay zona)   │
│ ┌ fila 1 ─────────────────────────────────────────┐ │
│ │ #1  ■ Atrapado o grave · 3 personas    hace 30 min│ │
│ │     Niños · Heridos  ·  2 en camino  ·  2 saltos  │ │
│ └──────────────────────────────────────────────────┘ │
│ ...                                                   │
└───────────────────────────────────────────────────────┘
 Con una alerta seleccionada, el detalle reemplaza la lista y tiene un botón "Volver a la lista".
```

## 2. Comportamiento

1. Las filas salen de `sortAlerts(alerts, nowMs)` (201). Las resueltas van al final y solo se ven con el filtro "Resueltas".
2. Los filtros de tipo son acumulables. Si se desactivan los tres, se muestran todos.
3. Si la selección del store es una celda (`cellId`, la elige el mapa), la lista muestra solo las alertas de esa celda, con un chip para quitar el filtro.
4. Al tocar una fila: `select({ alertId })`, el detalle se abre y el mapa (203) se centra en la alerta.
5. Si el mapa selecciona una alerta, la 202 abre su detalle.
6. `#` es la posición en el orden completo, antes de filtrar, para que el número no cambie al filtrar.
7. Los tiempos se recalculan con `nowMs` de `useDashboard` (200).

## 3. Resumen (barra superior)

| Variable | Origen | Formato | Ejemplo |
|---|---|---|---|
| `activeCount` | alertas no resueltas | número (200) | `128 alertas activas` |
| `byTier` | conteo por `tier` | número + `TypeLabel` | `42 rojas · 50 naranjas · 36 amarillas` |
| `peopleSum` | suma de `people` activas | número (200) | `312 personas` |
| `volunteersSum` | suma de `volunteers` activas | número | `57 voluntarios en camino` |
| `simulatedCount` | activas con `simulated` | número; solo si es mayor que 0 | `300 simuladas` |

## 4. Fila

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `rank` | posición en `sortAlerts` | `#n` | `#1` | — |
| `type` | `emergency_type` | `TypeLabel` | Atrapado o grave | — |
| `people` | `people` | personas (200) | `3 personas` | — |
| `vulnerable` | 4 booleanos | lista corta | `Niños · Heridos` | se oculta |
| `automatic` | `automatic` | `Badge` | `Automática` | se oculta |
| `waiting` | `created_at_ms` | tiempo transcurrido | `hace 30 min` | — |
| `volunteers` | `volunteers` | número | `2 en camino` | `Sin voluntarios` |
| `hops` | `hopsOf` (201) | número | `2 saltos` · `Directa` (0) | — |
| `simulated` | `simulated` | `Badge` tono `SIMULATION` | `SIMULADA` | se oculta |
| `isNew` | `newSince` (204) | resaltado temporal | — | sin resaltar |
| `resolved` | `latestStatus` | fila apagada + `Badge` | `A salvo` · `Cancelada` | — |
| `selected` | selección del store | fila resaltada | — | — |

## 5. Detalle

| Variable | Origen | Formato | Ejemplo |
|---|---|---|---|
| `type`, `people`, `vulnerable`, `automatic`, `simulated` | fila | como en la fila | — |
| `createdAt` | `created_at_ms` | hora + tiempo transcurrido | `07:33 · hace 30 min` |
| `receivedAt` | `received_at_ms` | hora | `Recibida en el servidor: 07:41` |
| `latency` | `received_at_ms − created_at_ms` | duración (200) | `Tardó 8 min en llegar` |
| `hops` | `hopsOf` | número | `Llegó por 2 saltos` |
| `uploadedBy` | `uploaded_by_device_id` | `deviceId` (200) | `Subida por 0b0b 0b0b 0b0b` |
| `coordinates` | `lat_e7`, `lon_e7` | coordenadas (200) | `-33.50200, -70.64900` |
| `alertId` | `alert_id` | texto | `a1b2c3d4e5f6-0001` |
| `volunteers` | `volunteers` | número | `2 voluntarios en camino` |
| `status` | `latestStatus` | `Badge` | `A salvo` |
| `tier` | `priorityOf` | `TypeLabel` | Nivel: Atrapado o grave |
| `breakdown` | `priorityBreakdown` (201) | una línea por componente, con signo | `Personas +30 · Vulnerables +30 · Espera +15 · Voluntarios −40 = 35 puntos` |

## 6. Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `list_summary_active` | siempre | barra superior | — | 128 alertas activas | — |
| `list_summary_people` | siempre | barra superior | — | 312 personas | — |
| `list_summary_volunteers` | siempre | barra superior | — | 57 voluntarios en camino | — |
| `list_summary_simulated` | `simulatedCount > 0` | barra superior | SIMULATION | 300 simuladas | — |
| `list_filter_food` / `_medical` / `_trapped` | siempre | filtros | — | Comida o agua · Asistencia médica · Atrapado o grave | activar / desactivar |
| `list_filter_resolved` | siempre | filtros | — | Mostrar resueltas | activar / desactivar |
| `list_filter_zone` | selección de celda | chip | INFO | Zona seleccionada · 5 alertas | ✕ quitar |
| `list_empty_title` | sin alertas | estado vacío | — | Todavía no hay alertas | — |
| `list_empty_body` | sin alertas | estado vacío | INFO | Aparecerán aquí en cuanto un teléfono con internet las suba. | — |
| `list_filtered_empty` | hay alertas pero los filtros las ocultan todas | estado vacío | INFO | Ninguna alerta coincide con los filtros. | Quitar filtros |
| `list_row_volunteers_none` | `volunteers = 0` | fila | WARNING | Sin voluntarios | — |
| `list_row_volunteers` | `volunteers > 0` | fila | SUCCESS | 2 en camino | — |
| `list_row_hops_direct` | `hops = 0` | fila | — | Directa | — |
| `list_row_hops` | `hops > 0` | fila | — | 2 saltos | — |
| `list_row_automatic` | `automatic` | fila | INFO | Automática | — |
| `list_row_simulated` | `simulated` | fila | SIMULATION | SIMULADA | — |
| `list_row_new` | `isNew` | fila | INFO | Nueva | — |
| `list_row_safe` / `_cancelled` | `latestStatus` | fila | SUCCESS / INFO | A salvo · Cancelada | — |
| `list_detail_back` | detalle abierto | detalle | — | Volver a la lista | cierra el detalle |
| `list_detail_locate` | detalle abierto | detalle | — | Ver en el mapa | centra el mapa |
| `list_detail_copy` | detalle abierto | detalle | — | Copiar coordenadas | copia |
| `list_detail_copied` | tras copiar | aviso breve | SUCCESS | Coordenadas copiadas | — |
| `list_detail_copy_failed` | el navegador no permitió copiar | aviso breve | ERROR | No se pudieron copiar. Selecciónalas a mano. | — |
| `list_detail_priority_title` | detalle abierto | detalle | — | Por qué está en esta posición | — |
| `list_detail_priority_help` | detalle abierto | detalle | INFO | Primero manda la gravedad. Dentro de cada gravedad suman las personas, los vulnerables y la espera; los voluntarios en camino restan. | — |
| `list_detail_automatic_note` | `automatic` | detalle | INFO | Alerta enviada sola por un movimiento brusco: puede ser una falsa alarma que no se canceló. | — |
| `list_detail_simulated_note` | `simulated` | detalle | SIMULATION | Alerta simulada del modo demo. No corresponde a una persona real. | — |

## Criterios de aceptación

1. Con los datos de la 003, sección 8, en `createFakeDashboard`, la lista muestra **3, 2, 6, 4, 1** y oculta la 5 (resuelta) hasta activar "Mostrar resueltas".
2. Los filtros de tipo y de zona reducen la lista sin cambiar `#`. "Quitar filtros" vuelve a la lista completa.
3. Tocar una fila despacha `select({ alertId })`. Una selección que llega desde el store abre el detalle.
4. El desglose suma exactamente `points` (201).
5. Cada `Badge` y cada mensaje de la sección 6 aparece solo en su condición (pruebas con Testing Library).
6. El resumen cuenta solo las alertas activas, e incluye las simuladas en `simulatedCount`.
7. Cada `id` de la sección 6 existe en `src/strings/es.ts`.

## Tareas

1. Textos de la sección 6.
2. Resumen (criterio 6).
3. Lista y filas, con filtros (criterios 1, 2 y 5).
4. Detalle y desglose (criterios 3 y 4).
5. Prueba del catálogo (criterio 7).
