# 201 web-core-prioridad-y-zonas

Capa: 2 · Persona: P3
Depende de: 003, 200 (solo el proyecto y `src/core/`)
Entrega: `priorityOf`, `compareAlerts`, `sortAlerts`, `cellOf`, `cellBounds`, `buildZones`, `zoneStyle`, `priorityBreakdown`, `hopsOf` y `PRIORITY_CONFIG`, en `web/src/core/priority/` y `web/src/core/zones/`
Consume: forma de entrada de la 003, sección 1; `TTL_MAX` de la 001; regla de saltos de la 002, sección 3.4

## Objetivo

Implementar en TypeScript puro la prioridad y las zonas de la 003, para que la lista (202) y el mapa (203) solo dibujen.

## Alcance

- Las funciones de la 003, sección 5, con la configuración de la sección 6.
- `priorityBreakdown`: los puntos por componente, para explicar en el panel por qué una alerta está donde está.
- `hopsOf`: los saltos de la 002, sección 3.4.
- `simulatedCount` por zona, para que el mapa rotule las zonas con datos simulados (regla 30). Es una extensión de la 003 y no entra en sus vectores.
- Pruebas con `priority.json`, `cells.json` y `zones.json`.

## Fuera de alcance

- Dibujar (202, 203), leer datos (200) y generar datos de demo (205).

## 1. Sin fake

Son funciones puras: la implementación real sirve directamente en las pruebas de la 202 y la 203. No hay fake (misma justificación que la 101, sección 1).

## 2. API

```ts
// spec 201
export interface AlertInput {           // 003, sección 1 (la 200 la arma desde las filas de la 002)
  alertId: string; emergencyType: 0 | 1 | 2; people: number
  children: boolean; elderly: boolean; injured: boolean; reducedMobility: boolean
  latE7: number; lonE7: number; createdAtMs: number
  volunteers: number; latestStatus: null | 1 | 2
  simulated?: boolean                   // extensión 201; por defecto false
}

export const PRIORITY_CONFIG: { /* 003, sección 6, con los mismos nombres */ }

export function priorityOf(a: AlertInput, nowMs: number): { tier: 0 | 1 | 2; points: number; resolved: boolean }
export function priorityBreakdown(a: AlertInput, nowMs: number):
  { people: number; vulnerable: number; wait: number; volunteers: number /* negativo */; total: number }
export function compareAlerts(a: AlertInput, b: AlertInput, nowMs: number): number
export function sortAlerts<T extends AlertInput>(alerts: readonly T[], nowMs: number): T[]   // sin repetidos por alertId

export function cellOf(latE7: number, lonE7: number): { cellId: string; cellY: number; cellX: number }
export function cellBounds(cellY: number, cellX: number): { southE7: number; westE7: number; northE7: number; eastE7: number }

export interface Zone { cellId: string; cellY: number; cellX: number; alertCount: number; peopleSum: number
  volunteersSum: number; maxTier: 0 | 1 | 2; topAlertId: string; simulatedCount: number }
export function buildZones(alerts: readonly AlertInput[], nowMs: number): Zone[]
export function zoneStyle(z: Zone): { color: string; opacityPercent: number }

export function hopsOf(row: { device_id: number; uploaded_by_device_id: number; ttl_at_upload: number }): number
```

- `sortAlerts` y `buildZones` son genéricas: devuelven los mismos objetos que reciben (con sus campos extra, como `simulated`), así la 202 no pierde datos.
- `floorDiv(a, b) = Math.floor(a / b)`. Es exacto con los valores de la 001 (|a| ≤ 1.8 × 10^9). Prohibido `Math.trunc` (003, sección 4.1).
- `TTL_MAX` se lee de una constante `PROTOCOL_TTL_MAX = 8` en `src/core/priority/config.ts`, con un comentario que apunta a la 001, sección 8.
- No hay flotantes en los cálculos: solo enteros y `Math.floor` sobre enteros.

## 3. Pruebas

En `web/src/core/**/*.test.ts`, con Vitest. Leen los vectores desde `../specs/shared/vectors/` (ruta relativa a `web/`).

| Prueba | Qué verifica |
|---|---|
| `priority.vectors.test.ts` | `priority.json` |
| `cells.vectors.test.ts` | `cells.json` |
| `zones.vectors.test.ts` | `zones.json`, incluido `zoneStyle` |
| `reference.test.ts` | los ejemplos de la 003, sección 8, escritos en el código, para avanzar sin los archivos |
| `tiers.test.ts` | un amarillo con el máximo de puntos (220) nunca supera a un rojo con el mínimo (−50) |
| `order.test.ts` | la misma entrada en otro orden da el mismo resultado; un `alertId` repetido cuenta una vez |
| `breakdown.test.ts` | `total` de `priorityBreakdown` = `points` de `priorityOf` en los ejemplos de la 003 |
| `hops.test.ts` | origen = 0 saltos; `ttl_at_upload = 7` subido por otro teléfono = 2 saltos |

## Criterios de aceptación

1. Pasan los ejemplos de la 003, sección 8 (orden **3 > 2 > 6 > 4 > 1 > 5** y la tabla de zonas).
2. Pasan `priority.json`, `cells.json` y `zones.json`.
3. Se cumplen los criterios 2-6 de la 003.
4. `priorityBreakdown(a).total === priorityOf(a).points` para toda alerta de los vectores.
5. `hopsOf` sigue la 002, sección 3.4.
6. `simulatedCount` cuenta las alertas con `simulated = true` en cada zona.
7. Las constantes solo aparecen en `src/core/priority/config.ts` (revisión en el PR).

## Tareas

1. `config.ts` con `PRIORITY_CONFIG` y `PROTOCOL_TTL_MAX`.
2. `priorityOf`, `priorityBreakdown`, `compareAlerts` y `sortAlerts`, con sus pruebas.
3. `cellOf`, `cellBounds`, `buildZones` y `zoneStyle`, con sus pruebas.
4. `hopsOf`.
5. Pruebas con los vectores. Si todavía no existen, la tarea queda `bloqueada` y se registra en `memory/P3.md`.
