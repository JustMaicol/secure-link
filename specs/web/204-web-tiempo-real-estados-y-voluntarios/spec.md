# 204 web-tiempo-real-estados-y-voluntarios

Capa: 3 · Persona: P3
Depende de: 200, 202, 203
Entrega: `RealtimeSource` (interfaz + implementación con Supabase + fake), ciclo de vida de la conexión, estado del tiempo real en la barra superior, avisos de alertas nuevas y marcas de "nuevo", en `web/src/features/realtime/`; cableado en `src/main.tsx`
Consume: `AlertRepository.loadAll`, store (`dashboardReducer`, acciones `loaded`, `alertInserted`, `onTheWayInserted`, `statusInserted` y `newExpired`), `Clock`, componentes y textos (200); la 202 y la 203 dibujan `isNew`

## Objetivo

Que el panel se actualice solo, sin recargar, cuando un gateway sube alertas, avisos de "voy en camino" o estados. Y que la autoridad note lo nuevo y sepa si está viendo datos en vivo.

## Alcance

- Suscripción a los `INSERT` de `alerts`, `on_the_way` y `alert_status` (002, sección 5).
- Orden de arranque sin huecos: primero suscribirse y después cargar.
- Recarga completa al reconectar, que es segura porque el store es idempotente (200).
- Estado de la conexión, visible.
- Marcas de "nuevo" con vencimiento, y avisos agrupados de alertas graves.
- Contador en el título de la pestaña del navegador.

## Fuera de alcance

- Dibujar la lista y el mapa (202, 203): solo leen `newSince`.
- Datos simulados (205): no pasan por el tiempo real. La 205 los agrega al store directamente.

## 1. Configuración

`features/realtime/realtimeConfig.ts` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `NEW_HIGHLIGHT_MS` | 30000 | cuánto dura la marca de "nuevo" |
| `TOAST_GROUP_MS` | 5000 | agrupa los avisos de alertas graves que llegan juntas |
| `TOAST_DURATION_MS` | 8000 | cuánto se ve un aviso |
| `TOAST_MIN_TIER` | 2 | solo avisan las alertas de este nivel o más grave (rojas) |

## 2. Interfaz

```ts
// spec 204
export type RealtimeStatus = 'CONNECTING' | 'LIVE' | 'RECONNECTING' | 'OFFLINE' | 'AUTH_ERROR'

export interface RealtimeSource {
  subscribe(handlers: {
    onAlert(row: AlertRow): void
    onOnTheWay(row: OnTheWayRow): void
    onStatus(row: StatusRow): void
    onConnection(status: RealtimeStatus): void
  }): () => void                      // devuelve la función para cancelar
}
export function createSupabaseRealtimeSource(client: SupabaseClient): RealtimeSource
export function createFakeRealtimeSource(): RealtimeSource & { emit...(row): void; setStatus(s: RealtimeStatus): void }
```

`OFFLINE` sale de los eventos `online` y `offline` del navegador. `AUTH_ERROR` aparece cuando el canal se rechaza por sesión vencida.

## 3. Ciclo de vida

1. Al iniciar sesión: `subscribe(...)`, con estado `CONNECTING`.
2. Al pasar a `LIVE` por primera vez: `loadAll()` (200) → `loaded`. Los eventos que llegan durante la carga se aplican igual: el store fusiona sin duplicar (200, criterio 6).
3. Cada evento: `alertInserted` (con `atMs = clock.nowMs()`), `onTheWayInserted` o `statusInserted`.
4. Al volver a `LIVE` después de `RECONNECTING` u `OFFLINE`: se repite `loadAll()` para recuperar lo perdido mientras tanto.
5. Con `AUTH_ERROR`: se cierra la sesión y la 200 muestra `auth_session_expired`.
6. Cada segundo (con `useNow` de la 200): `newExpired(now - NEW_HIGHLIGHT_MS)`.
7. Al cerrar sesión: se cancela la suscripción.

`alertInserted` solo marca como nueva una alerta que no estaba en el store. Si ya estaba (por ejemplo, llegó también con la carga), no se vuelve a marcar.

## 4. Avisos

- Cuando llega una alerta nueva de nivel `TOAST_MIN_TIER` o mayor, se muestra un aviso en la esquina superior derecha.
- Si llegan varias dentro de `TOAST_GROUP_MS`, se agrupan en un solo aviso.
- Los avisos no tapan la barra superior ni la leyenda del mapa. Se cierran solos tras `TOAST_DURATION_MS` o a mano.
- "Ver" selecciona la alerta (o, en un aviso agrupado, la primera según `sortAlerts`).
- Las alertas simuladas (205) no generan avisos.

## 5. Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `status` | `RealtimeSource` | chip en la barra superior | `En vivo` | `Conectando…` |
| `lastUpdateMs` | último evento o última carga | tiempo transcurrido (200) | `Actualizado hace 5 s` | `—` |
| `toast.items` | alertas nuevas agrupadas | tipo, personas y tiempo | `Atrapado o grave · 3 personas · hace 1 min` | — |
| `toast.count` | tamaño del grupo | número | `5 alertas graves nuevas` | — |
| `unseenCount` | alertas nuevas mientras la pestaña estaba oculta | número en el título | `(3) Senda` | sin número |

## 6. Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `rt_status_connecting` | `CONNECTING` | chip | INFO | Conectando… | — |
| `rt_status_live` | `LIVE` | chip | SUCCESS | En vivo | — |
| `rt_status_reconnecting` | `RECONNECTING` | chip | WARNING | Reconectando… | — |
| `rt_status_offline` | `OFFLINE` | chip y banner | ERROR | Sin conexión. Lo que ves puede estar desactualizado. | — |
| `rt_status_recovered` | vuelve a `LIVE` y termina la recarga | aviso breve | SUCCESS | Conexión recuperada. Datos actualizados. | — |
| `rt_last_update` | siempre | junto al chip | — | Actualizado hace 5 s | — |
| `rt_toast_single` | una alerta grave nueva | aviso | ERROR | Nueva alerta: Atrapado o grave · 3 personas | Ver · Cerrar |
| `rt_toast_group` | varias en `TOAST_GROUP_MS` | aviso | ERROR | 5 alertas graves nuevas | Ver · Cerrar |
| `rt_tab_title` | `unseenCount > 0` con la pestaña oculta | título del navegador | — | (3) Senda | — |

## Criterios de aceptación

1. Con `createFakeRealtimeSource`: se suscribe antes de cargar, y carga al primer `LIVE`.
2. Un `INSERT` de alerta, aviso o estado llega al store. La 202 lo muestra sin recargar y la 203 lo marca como nuevo.
3. Un evento repetido, o uno que llega también en la carga, no duplica nada ni cuenta dos veces un voluntario.
4. Tras `RECONNECTING` y luego `LIVE`, se vuelve a ejecutar `loadAll`, y se muestra `rt_status_recovered`.
5. La marca de "nuevo" desaparece a los `NEW_HIGHLIGHT_MS` (prueba con `fakeClock`).
6. Tres alertas rojas en menos de `TOAST_GROUP_MS` producen un solo aviso agrupado; una alerta naranja o simulada no produce aviso.
7. `AUTH_ERROR` cierra la sesión y lleva al inicio de sesión con `auth_session_expired`.
8. Contra el proyecto cloud: insertar una fila con la clave `anon` (`scripts/verify-002.ts`) la hace aparecer en el panel en menos de 5 s (prueba manual registrada en `memory/P3.md`).
9. Cada `id` de la sección 6 existe en `src/strings/es.ts`.

## Tareas

1. Configuración y textos.
2. `RealtimeSource`: implementación con Supabase y fake.
3. Ciclo de vida y cableado en `main.tsx` (criterios 1-4 y 7).
4. Marcas de "nuevo" y su vencimiento (criterio 5).
5. Avisos y título de la pestaña (criterio 6).
6. Prueba contra el proyecto cloud (criterio 8) y del catálogo (criterio 9).
