# 205 web-modo-demo

Capa: 2 · Persona: P3
Depende de: 200
Entrega: generador puro `buildDemoScenario`, reproductor del escenario, controles del modo demo y banner de simulación, en `web/src/features/demo/`
Consume: `mulberry32`, `Clock`, configuración (centro del mapa), store (acciones `demoAdded` y `demoCleared`), componentes y textos (200)

## Objetivo

Mostrar en la demo cómo se ve el panel con cientos de alertas llegando, algo que con pocos teléfonos reales no se puede ver. Siempre rotulado como simulación (regla 30) y siempre igual (semilla fija).

## Alcance

- Escenario simulado generado en el navegador: unas 300 alertas en focos, que van apareciendo durante 2 minutos, con voluntarios que se suman.
- Reproductor con iniciar, pausar, reanudar, reiniciar y quitar.
- Rótulos de simulación: banner, progreso y botón. La 202 y la 203 rotulan cada fila y cada zona.

## Fuera de alcance

- Guardar datos simulados en Supabase: viven solo en el navegador (002). Nunca se insertan en la base.
- Simular la malla BLE: eso es la simulación (300).
- Estados `CANCELADA / A_SALVO` simulados.

## 1. Configuración

`features/demo/demoConfig.ts` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `DEMO_SEED` | 2026 | semilla del escenario |
| `DEMO_ALERTS` | 300 | alertas en total |
| `DEMO_DURATION_MS` | 120000 | duración del escenario |
| `DEMO_HOTSPOTS` | 6 | focos alrededor del centro |
| `DEMO_AREA_RADIUS_E7` | 270000 (~3 km) | radio donde caen los focos |
| `DEMO_HOTSPOT_RADIUS_E7` | 60000 (~670 m) | radio de cada foco |
| `DEMO_TYPE_WEIGHTS` | rojo 25 · naranja 35 · amarillo 40 (%) | reparto de tipos |
| `DEMO_AUTOMATIC_PERCENT` | 30 (de las rojas) | alertas automáticas |
| `DEMO_VULNERABLE_PERCENT` | 20 (por cada vulnerable) | vulnerables |
| `DEMO_LARGE_GROUP_PERCENT` | 5 | grupos de 20 a 79 personas; el resto, de 1 a 6 |
| `DEMO_MESH_DELAY_MAX_MS` | 5400000 (90 min) | antigüedad máxima de una alerta al llegar (simula su viaje por la malla) |
| `DEMO_VOLUNTEER_PERCENT` | 40 | alertas que reciben voluntarios (de 1 a 3) |
| `DEMO_DEVICE_ID_BASE` | `0xDE0000000000` | rango de `deviceId` reservado para filas simuladas |
| `DEMO_TICK_MS` | 250 | cada cuánto el reproductor agrega lo que ya tocó |

## 2. Generador (puro)

```ts
// spec 205
export interface DemoEvent { atOffsetMs: number; kind: 'alert' | 'onTheWay'; row: AlertRow | OnTheWayRow }
export function buildDemoScenario(params: { seed: number; startMs: number; centerLatE7: number; centerLonE7: number },
                                  config = DEMO_CONFIG): DemoEvent[]
```

Reglas, todas con un único `mulberry32(seed)` (200) y solo con enteros:

1. Se eligen los focos dentro de un disco de radio `DEMO_AREA_RADIUS_E7` alrededor del centro: se sortean `dx` y `dy` en `[-R, R]` y se descarta el punto si `dx² + dy² > R²`.
2. Cada alerta toma un foco al azar y un punto dentro de un disco de radio `DEMO_HOTSPOT_RADIUS_E7` alrededor de él (mismo método).
3. El tipo, las personas, los vulnerables y si es automática salen de los porcentajes de la configuración. Una automática es roja, con 1 persona y sin vulnerables (000).
4. `atOffsetMs` es uniforme en `[0, DEMO_DURATION_MS)`. `received_at_ms = startMs + atOffsetMs`; `created_at_ms = received_at_ms − retardo`, con el retardo en `[0, DEMO_MESH_DELAY_MAX_MS]` y redondeado a segundos (002).
5. `device_id = DEMO_DEVICE_ID_BASE + i` y `counter = 1`. `uploaded_by_device_id` es otro `deviceId` del mismo rango. `ttl_at_upload` está en `[3, 8]`, así que da de 1 a 6 saltos según `hopsOf` (201).
6. Voluntarios: el `DEMO_VOLUNTEER_PERCENT` % de las alertas recibe de 1 a 3 `ON_THE_WAY`, cada uno entre 5 y 60 s después de su alerta, sin pasar de `DEMO_DURATION_MS`.
7. Todas las filas cumplen los rangos de la 002, sección 3.2, y llevan `simulated = true` al entrar al store.
8. El resultado se ordena por `(atOffsetMs, kind, clave)`: es estable (regla 9).
9. El orden en que se consumen los valores del generador es fijo: primero los focos, luego cada alerta en orden de `i` (posición, tipo, personas, vulnerables, automática, tiempo, retardo y saltos), y después los voluntarios.

## 3. Reproductor

| Estado | Qué pasa |
|---|---|
| `OFF` | sin datos simulados |
| `PLAYING` | cada `DEMO_TICK_MS`, despacha `demoAdded` con los eventos cuyo `atOffsetMs` ya pasó (según el `Clock` inyectado) |
| `PAUSED` | se congela el tiempo del escenario; lo ya agregado sigue visible |
| `DONE` | se agregaron todos; los datos siguen visibles hasta quitarlos |

Acciones:
- **Iniciar** (`OFF` → `PLAYING`): genera el escenario con `startMs = clock.nowMs()`.
- **Pausar** y **Reanudar**.
- **Reiniciar**: `demoCleared` y luego vuelve a `PLAYING` desde 0.
- **Quitar datos simulados**: `demoCleared` y vuelve a `OFF`.

Las alertas reales siguen llegando por la 204 mientras el modo demo está activo: conviven en el mismo store y se distinguen por `simulated`.

## 4. Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `state` | reproductor | ver mensajes | `PLAYING` | `OFF` |
| `elapsedMs` / `DEMO_DURATION_MS` | reproductor | `m:ss / m:ss` | `1:12 / 2:00` | — |
| `addedAlerts` / `DEMO_ALERTS` | reproductor | `n de N` | `184 de 300 alertas` | — |
| `seed` | configuración | número | `Semilla 2026` | — |

## 5. Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `demo_button` | siempre | barra superior | SIMULATION | Modo demo | abre los controles |
| `demo_controls_title` | controles abiertos | panel de controles | SIMULATION | Escenario simulado | — |
| `demo_controls_explain` | controles abiertos | panel de controles | INFO | Agrega 300 alertas simuladas durante 2 minutos para mostrar el panel a escala. No se guardan en el servidor ni llegan a los teléfonos. | — |
| `demo_start` | `OFF` | botón | — | Iniciar | inicia |
| `demo_pause` / `demo_resume` | `PLAYING` / `PAUSED` | botón | — | Pausar · Reanudar | — |
| `demo_restart` | no `OFF` | botón | — | Reiniciar | reinicia |
| `demo_clear` | no `OFF` | botón | — | Quitar datos simulados | quita |
| `demo_banner_playing` | `PLAYING` | banner bajo la barra superior | SIMULATION | SIMULACIÓN · Escenario en curso · 1:12 / 2:00 · 184 de 300 alertas | Pausar · Quitar |
| `demo_banner_paused` | `PAUSED` | banner | SIMULATION | SIMULACIÓN · Escenario en pausa · 184 de 300 alertas | Reanudar · Quitar |
| `demo_banner_done` | `DONE` | banner | SIMULATION | SIMULACIÓN · Escenario completo · 300 alertas simuladas en pantalla | Reiniciar · Quitar |
| `demo_seed` | controles abiertos | panel de controles | — | Semilla 2026 (siempre el mismo escenario) | — |

El banner de simulación es visible en todo momento mientras haya datos simulados en pantalla.

## Criterios de aceptación

1. `buildDemoScenario` con la misma semilla y los mismos parámetros devuelve exactamente lo mismo (prueba de igualdad y una instantánea de Vitest versionada).
2. Devuelve `DEMO_ALERTS` alertas. Todas caen dentro de `DEMO_AREA_RADIUS_E7 + DEMO_HOTSPOT_RADIUS_E7` del centro, cumplen los rangos de la 002 y están en el rango de `deviceId` reservado.
3. Las alertas automáticas son rojas, con 1 persona y sin vulnerables.
4. Ningún `ON_THE_WAY` llega antes que su alerta ni después de `DEMO_DURATION_MS`.
5. Con `fakeClock`: a la mitad de la duración se agregaron los eventos con `atOffsetMs` menor a la mitad, y ni uno más. La pausa detiene el avance y reanudar lo retoma donde estaba.
6. "Quitar datos simulados" deja el store sin filas simuladas, y las reales siguen intactas.
7. Mientras hay datos simulados, el banner `SIMULATION` está visible.
8. La 205 no llama a ninguna función de Supabase (una prueba lo verifica con un `AlertRepository` falso que falla si se usa).
9. Cada `id` de la sección 5 existe en `src/strings/es.ts`.

## Tareas

1. `demoConfig.ts` y textos.
2. `buildDemoScenario`, con sus pruebas (criterios 1-4).
3. Reproductor con `Clock` inyectado (criterio 5).
4. Controles y banner (criterios 6 y 7).
5. Pruebas de aislamiento y del catálogo (criterios 8 y 9).
