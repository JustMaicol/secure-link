# 003 prioridad y zonas

Capa: 0 · Redacta: P2 · Revisan: P1, P3
Depende de: 000, 001, 002
Entrega: fórmula de prioridad, orden de la lista, cuadrícula de celdas, agregación por zona, colores, configuración y formato de los vectores de prioridad y zonas
Consume: tipos de emergencia y forma canónica del `alertId` (001); filas de `alert_overview` (002)

## Objetivo

Fijar cómo el panel decide qué alerta va primero y cómo agrupa las alertas en zonas de colores, con funciones puras y enteras que la spec 201 implementa en TypeScript y prueba con vectores.

## Alcance

- Prioridad de una alerta: nivel por gravedad y puntos dentro del nivel.
- Orden estable de la lista del panel (202).
- Celda de una ubicación y agregación por zona para el mapa (203).
- Colores e intensidad de cada zona.
- Configuración y vectores.

## Fuera de alcance

- Dibujar la lista y el mapa (202, 203) y el tiempo real (204).
- La lista de la app Android (109): ordena por distancia, no por esta prioridad.
- Rutas, asignación de recursos y logística (trabajo futuro, spec 000).

## 1. Entrada

Las funciones reciben alertas ya leídas (de `alert_overview` o del modo demo de la 205) con esta forma:

| Campo | Tipo | Origen (002) |
|---|---|---|
| `alertId` | texto canónico | `alert_id` |
| `emergencyType` | 0, 1 o 2 | `emergency_type` |
| `people` | 1-255 | `people` |
| `children`, `elderly`, `injured`, `reducedMobility` | booleanos | columnas homónimas |
| `latE7`, `lonE7` | enteros | `lat_e7`, `lon_e7` |
| `createdAtMs` | epoch ms | `created_at_ms` |
| `volunteers` | entero ≥ 0 | `volunteers` |
| `latestStatus` | `null`, 1 (cancelada) o 2 (a salvo) | `latest_status` |

`nowMs` se inyecta (regla 6). Todo es aritmética entera: sin flotantes (regla 11).

Si llegan dos alertas con el mismo `alertId` (por ejemplo, al mezclar los datos reales con los del modo demo), se toma una sola (regla 22): la primera según el orden de la sección 3.

## 2. Prioridad

### 2.1 Nivel
`tier = emergencyType`: 2 rojo (atrapado o grave), 1 naranja (asistencia médica), 0 amarillo (comida o agua). **El nivel siempre manda:** ningún puntaje hace que una alerta pase a un nivel superior.

### 2.2 Resuelta
`resolved = latestStatus ∈ {1, 2}`. Una alerta resuelta va al final de la lista y no cuenta en el mapa.

### 2.3 Puntos dentro del nivel

```
vulnerables = número de flags verdaderos entre children, elderly, injured, reducedMobility   (0-4)
waitMs      = max(0, nowMs - createdAtMs)          # una hora futura (reloj desfasado) cuenta 0
points =  min(people, PEOPLE_CAP) * PEOPLE_POINTS
        + vulnerables * VULNERABLE_POINTS
        + min(floorDiv(waitMs, WAIT_STEP_MS), WAIT_STEPS_CAP) * WAIT_POINTS
        - min(volunteers, VOLUNTEER_CAP) * VOLUNTEER_POINTS
```

`points` puede ser negativo. Con la configuración de la sección 6, va de −50 a 220.

| Componente | Máximo | Motivo |
|---|---|---|
| Personas | 100 (10 o más personas) | más gente, más urgencia, con tope para que no domine |
| Vulnerables | 60 | niños, mayores, heridos y movilidad reducida suben la urgencia |
| Espera | 60 (a las 2 h) | lo que lleva más tiempo sin atender sube de a poco |
| Voluntarios | −60 (3 o más) | reparte la ayuda; nunca baja de nivel |

## 3. Orden de la lista

`compareAlerts(a, b, nowMs)`, estable y con desempate explícito (regla 9):

1. No resueltas antes que resueltas.
2. `tier` mayor primero.
3. `points` mayor primero.
4. `createdAtMs` menor primero (la más antigua).
5. `alertId` menor primero, comparado como texto.

## 4. Celdas y zonas

### 4.1 Celda
Cuadrícula fija sobre las coordenadas enteras:

```
cellY  = floorDiv(latE7, CELL_SIZE_E7)
cellX  = floorDiv(lonE7, CELL_SIZE_E7)
cellId = "<cellY>:<cellX>"            # p. ej. "-13401:-28260"
```

- `floorDiv` redondea hacia −∞, también con negativos: `Math.floor(a / b)` en TS, `Math.floorDiv` en Kotlin. Prohibido truncar (`Math.trunc`, `/` entero de Kotlin), porque junta las celdas a ambos lados del 0.
- Esquina suroeste: `(cellY * CELL_SIZE_E7, cellX * CELL_SIZE_E7)`; esquina noreste: `((cellY + 1) * CELL_SIZE_E7, (cellX + 1) * CELL_SIZE_E7)`. Solo se pasan a grados al dibujar en Leaflet.
- Con 0.0025°, una celda mide unos 280 m norte-sur. En este-oeste mide 280 m × cos(latitud), unos 230 m en Santiago.

### 4.2 Zona
Una zona es una celda con al menos una alerta **no resuelta**. Por zona:

| Campo | Cálculo |
|---|---|
| `cellId`, `cellY`, `cellX` | de la celda |
| `alertCount` | número de `alertId` distintos |
| `peopleSum` | suma de `people` |
| `volunteersSum` | suma de `volunteers` |
| `maxTier` | el `tier` más grave; define el color |
| `topAlertId` | la primera alerta de la zona según el orden de la sección 3 |

### 4.3 Color e intensidad

| `maxTier` | Color | Hex |
|---|---|---|
| 2 | rojo | `#DC2626` |
| 1 | naranja | `#F97316` |
| 0 | amarillo | `#EAB308` |

Opacidad del relleno según `peopleSum`:

| `peopleSum` | Opacidad |
|---|---|
| 1-4 | 35 % |
| 5-19 | 55 % |
| 20 o más | 75 % |

### 4.4 Orden de las zonas
1. `maxTier` mayor primero.
2. `peopleSum` mayor primero.
3. `alertCount` mayor primero.
4. `cellId` menor primero, comparado como texto.

## 5. Funciones (las implementa la 201)

| Función | Resultado |
|---|---|
| `priorityOf(alert, nowMs)` | `{ tier, points, resolved }` |
| `compareAlerts(a, b, nowMs)` | negativo, 0 o positivo |
| `sortAlerts(alerts, nowMs)` | lista nueva, sin repetidos, ordenada |
| `cellOf(latE7, lonE7)` | `{ cellId, cellY, cellX }` |
| `buildZones(alerts, nowMs)` | zonas ordenadas según la sección 4.4 |
| `zoneStyle(zone)` | `{ color, opacityPercent }` |

Todas son puras: no leen el reloj, la red ni el DOM (regla 5).

## 6. Configuración

Una sola configuración documentada (regla 17):

| Constante | Valor |
|---|---|
| `PEOPLE_POINTS` | 10 |
| `PEOPLE_CAP` | 10 |
| `VULNERABLE_POINTS` | 15 |
| `WAIT_STEP_MS` | 600000 (10 min) |
| `WAIT_POINTS` | 5 |
| `WAIT_STEPS_CAP` | 12 |
| `VOLUNTEER_POINTS` | 20 |
| `VOLUNTEER_CAP` | 3 |
| `CELL_SIZE_E7` | 25000 (0.0025°) |
| `OPACITY_BUCKETS` | `[[1, 35], [5, 55], [20, 75]]` (desde `peopleSum`, en %) |
| `TIER_COLORS` | `{2: "#DC2626", 1: "#F97316", 0: "#EAB308"}` |

Cambiar un valor cambia esta spec y sus vectores.

## 7. Vectores

En `specs/shared/vectors/`, mismas convenciones que la 001 (enteros JSON, `nowMs` explícito):

| Archivo | Contenido | Mínimo |
|---|---|---|
| `priority.json` | `{ "nowMs", "alerts": [...], "expected": { "priorities": {alertId: {tier, points, resolved}}, "order": [alertId...] } }` | el ejemplo de la sección 8, más: topes de personas, vulnerables, espera y voluntarios; hora futura; `alertId` repetido |
| `cells.json` | `[{ "latE7", "lonE7", "cellId" }]` | los casos de borde de la sección 8 |
| `zones.json` | `{ "nowMs", "alerts": [...], "expected": [zona...] }` con `zoneStyle` incluido | el ejemplo de la sección 8, más un caso de cada tramo de opacidad |

## 8. Ejemplos de referencia

`nowMs = 1760600000000`. Por defecto, sin vulnerables, sin voluntarios y sin estado.

| Alerta | Tipo | Personas | Extras | Hace | latE7, lonE7 |
|---|---|---|---|---|---|
| `000000000001-0001` | 0 amarillo | 50 | | 120 min | −335000000, −706500000 |
| `000000000002-0001` | 2 rojo | 1 | | 5 min | −335010000, −706510000 |
| `000000000003-0001` | 2 rojo | 3 | niños, heridos, 2 voluntarios | 30 min | −335020000, −706490000 |
| `000000000004-0001` | 1 naranja | 2 | adultos mayores | 45 min | −334970000, −706470000 |
| `000000000005-0001` | 2 rojo | 4 | a salvo (`latestStatus = 2`) | 60 min | −335000000, −706500000 |
| `000000000006-0001` | 2 rojo | 1 | | 5 min | −335010000, −706510000 |

Prioridades:

| Alerta | tier | points | Cálculo | resolved |
|---|---|---|---|---|
| 1 | 0 | 160 | 100 (tope) + 0 + 60 (tope) | no |
| 2 | 2 | 10 | 10 + 0 + 0 | no |
| 3 | 2 | 35 | 30 + 30 + 15 − 40 | no |
| 4 | 1 | 55 | 20 + 15 + 20 | no |
| 5 | 2 | 70 | 40 + 0 + 30 | **sí** |
| 6 | 2 | 10 | 10 + 0 + 0 | no |

Orden: **3 > 2 > 6 > 4 > 1 > 5**.
- 2 y 6 empatan en todo menos el `alertId`.
- El amarillo con 160 puntos va después de todos los rojos y naranjas activos.
- La 5 va al final porque está resuelta.

Zonas (la 5 no cuenta):

| cellId | alertCount | peopleSum | volunteersSum | maxTier | Estilo | topAlertId |
|---|---|---|---|---|---|---|
| `-13401:-28260` | 1 | 3 | 2 | 2 | rojo 35 % | 3 |
| `-13401:-28261` | 2 | 2 | 0 | 2 | rojo 35 % | 2 |
| `-13399:-28259` | 1 | 2 | 0 | 1 | naranja 35 % | 4 |
| `-13400:-28260` | 1 | 50 | 0 | 0 | amarillo 75 % | 1 |

La celda `-13400:-28260` sería roja si contara la 5. Como está resuelta, queda amarilla.

Bordes de celda:

| latE7 | lonE7 | cellId |
|---|---|---|
| 0 | 0 | `0:0` |
| 24999 | −1 | `0:-1` |
| −1 | 25000 | `-1:1` |
| −25000 | −25001 | `-1:-2` |
| 900000000 | 1800000000 | `36000:72000` |
| −900000000 | −1800000000 | `-36000:-72000` |

## Criterios de aceptación

1. `priorityOf` y `sortAlerts` reproducen las prioridades y el orden de la sección 8.
2. Ningún valor de `points` hace que una alerta supere a otra de un nivel superior. Se prueba con el máximo de puntos en amarillo (220) frente al mínimo en rojo (−50).
3. `cellOf` reproduce los bordes de la sección 8, incluidos los negativos.
4. `buildZones` y `zoneStyle` reproducen la tabla de zonas de la sección 8. Una alerta resuelta no aparece en ninguna zona.
5. Con el mismo `nowMs` y la misma entrada en otro orden, el resultado es idéntico.
6. Un `alertId` repetido en la entrada cuenta una sola vez en la lista y en la zona.
7. Ninguna constante aparece fuera de la configuración de la sección 6.

## Limitaciones declaradas

- Los pesos son un criterio razonable para el MVP, no un protocolo de triaje validado por expertos.
- Una alerta automática (movimiento brusco) entra como roja con una persona, aunque sea una falsa alarma que no se canceló a tiempo.
- La espera depende de `createdAtMs`, que viene del reloj del teléfono.
- Las celdas son una cuadrícula de grados, sin ajuste por latitud: lejos del ecuador son más angostas de este a oeste.

## Tareas

1. Revisión de P1 y P3 (comentarios en el PR) y aprobación de los tres (regla 4).
2. Crear los vectores de la sección 7 en `specs/shared/vectors/`.
3. Spec 201: funciones de la sección 5 en `web/src/core/`, probadas con los vectores.
4. Specs 202 y 203: usar `sortAlerts`, `buildZones` y `zoneStyle` sin reimplementarlos.
