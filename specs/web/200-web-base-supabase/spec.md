# 200 web-base-supabase

Capa: 1 · Persona: P3
Depende de: 000, 001 (solo `mulberry32` y la forma canónica de las claves), 002
Entrega: proyecto web (React + Vite + TypeScript); migraciones y políticas de Supabase; script de aceptación de la 002; `Clock` + `fakeClock`; `mulberry32`; `AlertRepository` + fake; `AuthService` + fake; store del panel (`dashboardReducer`, `useDashboard`) + `createFakeDashboard`; inicio de sesión; layout con lista y mapa; tema; componentes base, formato y convenciones de contenido visible; despliegue en GitHub Pages
Consume: esquema, RLS y contratos de subida y lectura (002)

## Objetivo

Dejar listo el panel de las autoridades y su base de datos, para que la lista (202), el mapa (203), el tiempo real (204) y el modo demo (205) trabajen cada uno en su carpeta, sin tocar archivos compartidos.

## Alcance

- Proyecto web en `web/` con React, Vite y TypeScript.
- Supabase: migración de la 002, registro desactivado, usuario autoridad y script que verifica los criterios de la 002.
- Primitivas comunes: reloj y `mulberry32`.
- Acceso a datos: carga inicial de las tres tablas.
- Store del panel: estado único con un reductor puro e idempotente.
- Inicio de sesión, layout de dos columnas, tema claro y oscuro, y componentes base.
- Reglas de formato y convenciones de contenido visible.
- Despliegue en GitHub Pages.

## Fuera de alcance

- Prioridad, celdas y zonas (201), lista (202), mapa (203), tiempo real (204) y modo demo (205).
- Diseño final (Figma). Aquí solo se fija un diseño temporal estructural.

## 1. Stack

| Pieza | Elección |
|---|---|
| UI | React con componentes de función y hooks |
| Build | Vite, con `@vitejs/plugin-react` |
| Lenguaje | TypeScript en modo `strict` |
| Mapa | Leaflet, usado directamente dentro de un componente (sin `react-leaflet`) |
| Datos | `@supabase/supabase-js` |
| Estilos | CSS propio con variables (`--color-*`, `--space-*`), sin biblioteca de componentes. Así, los tokens de Figma se pasan a variables sin rehacer nada |
| Pruebas | Vitest; Testing Library + jsdom para los componentes |
| Base de datos | CLI de Supabase como dependencia de desarrollo (`supabase`) |

- **Todas las versiones exactas** en `package.json`, sin `^`, `~` ni `latest` (regla 12), y `package-lock.json` versionado. Las versiones se fijan en la tarea 1 con la última estable y se registran en `memory/P3.md`.
- Gestor de paquetes: `npm`.
- Navegador objetivo: Chrome o Edge recientes, en una laptop. El panel no se diseña para teléfono.

## 2. Carpetas y dueños

```
web/
  package.json, package-lock.json, vite.config.ts, tsconfig.json, index.html   [200]
  .env.example                                                                 [200]
  src/
    main.tsx                 crea Clock, cliente de Supabase y servicios; monta <App>   [200; en capa 3, 204]
    app/                     App, AuthGate, Layout, store del panel                     [200]
    core/
      common/                Clock, mulberry32                                          [200]
      priority/  zones/      prioridad, celdas, zonas, saltos (003)                     [201]
    data/                    supabase client, AlertRepository, AuthService y fakes      [200]
    ui/
      theme/                 variables CSS, tema claro y oscuro                         [200]
      components/            componentes base (sección 9)                               [200]
      format/                reglas de formato (sección 10)                             [200]
    features/
      list/                  [202]   map/   [203]   realtime/   [204]   demo/   [205]
    strings/es.ts            textos visibles, por bloques de feature                    [200; cada feature agrega su bloque]
  supabase/
    migrations/              migración de la 002                                        [200]
    seed/                    vacío: los datos de demo viven solo en el navegador (002)  [200]
  scripts/verify-002.ts      criterios de aceptación de la 002                          [200]
.github/workflows/web-pages.yml   despliegue                                            [200]
```

- Los archivos marcados `[200]` son compartidos (`ARCHITECTURE.md`, sección 4).
- Los fakes viven en `src/` junto a su interfaz, con el prefijo `fake` o `createFake`.
- La 200 crea vacías las carpetas de las features, cada una con un componente provisional rotulado "Pendiente: spec NNN".
- `src/core/` es TypeScript puro: no importa React, ni el DOM, ni `supabase-js`. Una prueba lo verifica.

## 3. Configuración

Variables de entorno de Vite (`web/.env`, ignorado por git; `web/.env.example`, versionado sin valores reales):

| Variable | Uso | Por defecto |
|---|---|---|
| `VITE_SUPABASE_URL` | URL del proyecto | — (obligatoria) |
| `VITE_SUPABASE_ANON_KEY` | clave `anon` (pública por diseño; la protege el RLS de la 002) | — (obligatoria) |
| `VITE_MAP_CENTER_LAT_E7`, `VITE_MAP_CENTER_LON_E7` | centro del mapa y del escenario demo | `-334500000`, `-706667000` (Santiago) |
| `VITE_MAP_ZOOM` | zoom inicial | `13` |
| `VITE_BASE_PATH` | ruta base para GitHub Pages | `/` |

Se leen en un solo módulo, `src/app/config.ts`. Si falta una obligatoria, el panel muestra el mensaje `common_config_missing` en lugar de fallar en silencio (regla 16).

## 4. Supabase

1. Migración con las secciones 3-5 de la 002 en `supabase/migrations/<fecha>_init.sql`.
2. En el proyecto cloud: registro de usuarios desactivado y un usuario "autoridad" creado a mano. Sus credenciales no van al repositorio (regla 25); se comparten fuera de git.
3. `scripts/verify-002.ts`: ejecuta los criterios de aceptación 1-8 de la 002 contra el proyecto cloud. Usa la clave `anon` y el usuario autoridad, que lee de variables de entorno. Inserta solo filas con `device_id` dentro de un rango reservado para pruebas (`0xFFFF00000000`-`0xFFFFFFFFFFFF`) y lo declara en su salida.
4. Antes de terminar, se confirma lo que la 002 deja pendiente: `ignore-duplicates` sin `select` para `anon`, y Realtime con RLS. El resultado se registra en `memory/P3.md`.

## 5. Primitivas comunes (`src/core/common/`)

```ts
// spec 200
export interface Clock { nowMs(): number }        // epoch ms UTC (regla 11)
export const systemClock: Clock                    // único uso de Date.now() en el proyecto (regla 6)
export function fakeClock(startMs: number): Clock & { advanceBy(ms: number): void; set(ms: number): void }

// spec 200 — igual a la 001, sección 7; probado con specs/shared/vectors/prng.json
export function mulberry32(seed: number): () => number   // uint32
```

## 6. Datos (`src/data/`)

```ts
// spec 200 — filas tal como las define la 002 (snake_case)
export interface AlertRow { device_id: number; counter: number; alert_id: string; emergency_type: 0 | 1 | 2;
  automatic: boolean; children: boolean; elderly: boolean; injured: boolean; reduced_mobility: boolean;
  people: number; lat_e7: number; lon_e7: number; created_at_ms: number;
  uploaded_by_device_id: number; ttl_at_upload: number; received_at_ms: number }
export interface OnTheWayRow { alert_id: string; volunteer_device_id: number; created_at_ms: number; received_at_ms: number /* ... */ }
export interface StatusRow { alert_id: string; status: 1 | 2; created_at_ms: number; received_at_ms: number /* ... */ }

export type LoadError = 'NETWORK' | 'AUTH' | 'UNKNOWN'
export type Result<T, E> = { ok: true; value: T } | { ok: false; error: E }

export interface AlertRepository {
  /** Lee las tres tablas completas, en páginas de 1000 filas, ordenadas por clave primaria. */
  loadAll(): Promise<Result<{ alerts: AlertRow[]; onTheWay: OnTheWayRow[]; statuses: StatusRow[] }, LoadError>>
}
export function createFakeAlertRepository(data: ...): AlertRepository

export type AuthError = 'INVALID_CREDENTIALS' | 'NETWORK' | 'UNKNOWN'
export interface AuthService {
  session(): { email: string } | null
  signIn(email: string, password: string): Promise<Result<{ email: string }, AuthError>>
  signOut(): Promise<void>
  onChange(listener: (session: { email: string } | null) => void): () => void
}
export function createFakeAuthService(...): AuthService
```

- `device_id` y similares caben en un `number` de JS (48 bits < 2^53).
- Cada llamada de red tiene tiempo límite (10 s) y su error es explícito (reglas 16 y 23). Un `401` o `403` se traduce como `AUTH`.

## 7. Store del panel (`src/app/store/`)

Un único estado, actualizado por un reductor **puro e idempotente**: aplicar la misma acción dos veces deja el mismo estado (reglas 13 y 22).

```ts
// spec 200
export interface DashboardState {
  alerts: Map<string, PanelAlert>                 // por alert_id
  volunteers: Map<string, Set<number>>            // alert_id -> volunteer_device_id
  statuses: Map<string, StatusRow[]>              // alert_id -> estados
  load: 'IDLE' | 'LOADING' | 'LOADED' | { error: LoadError }
  selection: { alertId: string } | { cellId: string } | null
  newSince: Map<string, number>                   // alert_id -> ms en que llegó por tiempo real (204)
}

export interface PanelAlert extends AlertRow { simulated: boolean }

export type DashboardAction =
  | { type: 'loadStarted' } | { type: 'loadFailed'; error: LoadError }
  | { type: 'loaded'; alerts: AlertRow[]; onTheWay: OnTheWayRow[]; statuses: StatusRow[] }   // fusiona, no reemplaza
  | { type: 'alertInserted'; row: AlertRow; atMs: number }                                    // 204
  | { type: 'onTheWayInserted'; row: OnTheWayRow } | { type: 'statusInserted'; row: StatusRow }
  | { type: 'demoAdded'; alerts: AlertRow[]; onTheWay: OnTheWayRow[] } | { type: 'demoCleared' } // 205
  | { type: 'select'; selection: DashboardState['selection'] }
  | { type: 'newExpired'; beforeMs: number }                                                 // 204: quita de newSince lo anterior a beforeMs

export function dashboardReducer(s: DashboardState, a: DashboardAction): DashboardState

/** Vista derivada para 201, 202 y 203 (forma de la 003, sección 1, más los campos de PanelAlert). */
export function useDashboard(): { alerts: PanelAlertView[]; nowMs: number; state: DashboardState; dispatch: ... }
export function createFakeDashboard(initial: Partial<DashboardState>): ...   // para pruebas de 202-205
```

- `PanelAlertView` agrega `volunteers` (tamaño del conjunto) y `latestStatus` (el estado con mayor `created_at_ms`, desempatando por `status`, como en la 002) a cada `PanelAlert`.
- Las filas simuladas (205) llevan `simulated = true` y viven en el mismo estado. `demoCleared` quita solo esas.
- `nowMs` sale de `useNow(1000)`, que lee el `Clock` inyectado una vez por segundo (regla 6).

## 8. Pantallas de la 200

### 8.1 Inicio de sesión

| Variable | Origen | Formato | Ejemplo |
|---|---|---|---|
| `email`, `password` | usuario | texto | `autoridad@ejemplo.org` |
| `submitting` | estado local | booleano | `false` |
| `authError` | `AuthService` | `INVALID_CREDENTIALS`, `NETWORK` o `UNKNOWN` | `null` |

### 8.2 Layout principal

```
┌────────────────────────── Barra superior ──────────────────────────────┐
│ Senda · Panel de emergencias   [resumen]  [estado RT] [tema] [salir]│
├──────────── Lista (202) ─────────────┬──────────── Mapa (203) ───────────┤
│  ~40 % del ancho                     │  ~60 % del ancho                  │
│  desplazamiento propio               │                                   │
└──────────────────────────────────────┴───────────────────────────────────┘
 Banner del modo demo (205), visible solo si está activo, bajo la barra superior.
```

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `userEmail` | `AuthService` | texto | `autoridad@ejemplo.org` | — |
| `load` | store | ver mensajes | `LOADED` | `LOADING` |
| `theme` | sistema o elección del usuario | `light` / `dark` | `dark` | el del sistema |

La barra superior reserva tres huecos: resumen (202), estado del tiempo real (204) y botón del modo demo (205). Mientras su spec no exista, quedan vacíos.

### 8.3 Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `common_app_title` | siempre | barra superior | — | Senda · Panel de emergencias | — |
| `auth_title` | sin sesión | inicio de sesión | — | Acceso para autoridades | — |
| `auth_email` / `auth_password` | sin sesión | campos | — | Correo · Contraseña | — |
| `auth_submit` | sin sesión | botón | — | Entrar | inicia sesión |
| `auth_submitting` | `submitting` | botón | INFO | Entrando… | — |
| `auth_error_credentials` | `INVALID_CREDENTIALS` | bajo el formulario | ERROR | Correo o contraseña incorrectos. | — |
| `auth_error_network` | `NETWORK` | bajo el formulario | ERROR | No se pudo conectar con el servidor. Revisa tu conexión. | Reintentar |
| `auth_error_unknown` | `UNKNOWN` | bajo el formulario | ERROR | Algo salió mal al iniciar sesión. Inténtalo de nuevo. | — |
| `auth_no_signup` | sin sesión | pie del formulario | INFO | El acceso lo entrega el equipo de emergencias. No hay registro público. | — |
| `auth_session_expired` | la sesión venció o el servidor respondió `AUTH` | inicio de sesión | WARNING | Tu sesión terminó. Vuelve a entrar. | — |
| `common_signout` | con sesión | barra superior | — | Salir | cierra sesión |
| `common_theme_toggle` | con sesión | barra superior | — | Tema claro · Tema oscuro | alterna |
| `common_loading` | `load = LOADING` | sobre la lista y el mapa | INFO | Cargando alertas… | — |
| `common_load_error_network` | `loadFailed NETWORK` | banner | ERROR | No se pudieron cargar las alertas. Revisa tu conexión. | Reintentar |
| `common_load_error_unknown` | `loadFailed UNKNOWN` | banner | ERROR | Error al cargar las alertas. | Reintentar |
| `common_config_missing` | falta una variable obligatoria | pantalla completa | ERROR | Falta configurar el servidor en esta compilación (VITE_SUPABASE_URL o VITE_SUPABASE_ANON_KEY). | — |

## 9. Diseño temporal y componentes base (`src/ui/`)

Mismo criterio que en Android (100, sección 10): el diseño final sale de Figma, y el temporal solo separa bien textos, botones y contenedores.

- **Convención de contenido visible:** igual a la de Android (100, sección 9). Cada spec con pantalla trae una tabla de variables visibles y otra de mensajes con `id`, condición, tono, texto provisional y acción. Tonos: `INFO`, `SUCCESS`, `WARNING`, `ERROR` y `SIMULATION`.
- **Textos:** todos en `src/strings/es.ts`, por bloques con el prefijo de la feature (`common_`, `auth_`, `list_`, `map_`, `rt_`, `demo_`). Los textos son provisionales.
- **Variables CSS en `ui/theme/`:** espaciado 4, 8, 16, 24 y 32 px; colores de tipo de la 003; tonos; y dos juegos de colores, claro y oscuro. El tema sigue al sistema y se puede cambiar a mano (se guarda en `localStorage`, con `try/catch`).

| Componente | Uso |
|---|---|
| `TopBar` | barra superior con huecos para las features |
| `Panel(title?)` | contenedor con título |
| `Badge(text, tone)` | estado corto o etiqueta ("SIMULADA", "Automática") |
| `Banner(text, tone, action?)` | aviso o error |
| `Button(variant: primary \| secondary \| ghost)` | acciones |
| `EmptyState(title, body, action?)` | sin contenido |
| `KeyValue(label, value)` | par etiqueta-valor |
| `TypeLabel(type)` | tipo de emergencia: color **y** texto, nunca solo color |
| `SimulationBadge` | "SIMULACIÓN" |

## 10. Formato de valores (`src/ui/format/`)

Las mismas reglas que Android (100, sección 11) donde aplican, más las propias del panel:

| Valor | Regla | Ejemplos |
|---|---|---|
| Tiempo transcurrido | igual que Android | `ahora`, `hace 5 min`, `hace 3 h`, `hace 2 d` |
| Hora | `HH:mm` y, si no es hoy, `d MMM HH:mm`, en la zona horaria del navegador | `07:33`, `15 oct 22:10` |
| Personas | igual que Android | `1 persona`, `255 o más personas` |
| Duración (latencia) | `s`, `m s` o `h m` | `45 s`, `2 min 10 s`, `1 h 5 min` |
| Coordenadas | 5 decimales | `-33.50000, -70.65000` |
| `deviceId` | igual que Android | `a1b2 c3d4 e5f6` |
| Números | separador de miles con punto | `1.250 personas` |

## 11. Despliegue

- `.github/workflows/web-pages.yml`: con cada push a `main` que cambie `web/`, instala con `npm ci`, ejecuta las pruebas y publica el build en GitHub Pages.
- `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` y `VITE_BASE_PATH` se cargan como variables del repositorio en GitHub, no en archivos (regla 25).
- Sin enrutador: con sesión, el panel; sin sesión, el inicio de sesión. Así no hay problemas de rutas en Pages.

## Criterios de aceptación

1. `npm ci && npm test && npm run build` termina sin errores desde un clon limpio, con un `.env` de ejemplo.
2. `package.json` no tiene `^`, `~`, `latest` ni rangos (una prueba lo lee).
3. Ningún archivo de `src/core/` importa `react`, `@supabase/supabase-js` ni usa `window` o `document` (una prueba lo busca).
4. `Date.now()` solo aparece en `systemClock` (una prueba lo busca).
5. `mulberry32` reproduce `prng.json` y las salidas de la 001, sección 7.
6. `dashboardReducer`:
   - aplicar dos veces `loaded`, `alertInserted`, `onTheWayInserted` o `statusInserted` con las mismas filas da el mismo estado;
   - `loaded` fusiona y no borra lo que ya llegó por tiempo real;
   - `demoCleared` quita solo las filas simuladas;
   - un `onTheWayInserted` de una alerta que todavía no llegó se guarda y cuenta cuando la alerta aparece (caso de la 002, criterio 6);
   - `alertInserted` de una alerta que ya estaba no la marca como nueva en `newSince`;
   - `newExpired` quita de `newSince` solo lo anterior a `beforeMs`.
7. `useDashboard` deriva `volunteers` y `latestStatus` como la vista `alert_overview` de la 002.
8. El inicio de sesión muestra cada error de la sección 8.3 con un `AuthService` falso. Con sesión, se ve el layout de dos columnas con los marcadores de posición de 202 y 203.
9. Sin `VITE_SUPABASE_URL` o `VITE_SUPABASE_ANON_KEY`, se ve `common_config_missing`.
10. `scripts/verify-002.ts` pasa los 8 criterios de la 002 contra el proyecto cloud. El resultado, incluidos los dos puntos pendientes de la 002, queda en `memory/P3.md`.
11. El workflow publica el panel en GitHub Pages y se puede iniciar sesión desde la URL publicada (prueba manual registrada en `memory/P3.md`).
12. Cada `id` de la sección 8.3 existe en `src/strings/es.ts`.

## Tareas

1. Proyecto Vite + React + TypeScript, `package.json` con versiones exactas, Vitest y estructura de carpetas (criterios 1-3).
2. `config.ts` y `.env.example` (criterio 9).
3. `src/core/common/`: `Clock` y `mulberry32` (criterios 4 y 5).
4. Migración de la 002, configuración de Auth y `verify-002.ts` (criterio 10).
5. `src/data/`: `AlertRepository`, `AuthService` y sus fakes.
6. Store: `dashboardReducer`, `useDashboard`, `useNow` y `createFakeDashboard` (criterios 6 y 7).
7. `ui/theme`, `ui/components` y `ui/format`, con sus pruebas.
8. Inicio de sesión y layout (criterio 8).
9. Workflow de GitHub Pages (criterio 11).
10. Textos y prueba del catálogo (criterio 12).
