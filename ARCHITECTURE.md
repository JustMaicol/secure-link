# ARCHITECTURE.md

Estructura de carpetas, modularización y orden de specs. Obligatorio por `CONSTITUTION.md`.

## 1. Principios

- **Capas con dependencia en un solo sentido:** `core` (lógica pura) ← `adaptadores` (BLE, Room, Supabase, GPS) ← `features` (UI) ← `composition root`. Una capa nunca importa de la que está encima.
- **`core` no importa nada de Android ni de la web.** Es Kotlin puro (Android) o TypeScript puro (web). Se prueba sin dispositivo, red ni reloj.
- **Cada módulo se consume por una interfaz.** Quien consume una interfaz no conoce su implementación.
- **Cada módulo entrega un fake** de su interfaz (en memoria, determinista). Con él, los demás avanzan sin esperar.
- **El cableado vive en un solo lugar** (`AppContainer` en Android, `main` en web), manual y explícito, sin inyección por reflexión.

## 2. Carpetas

### Android (`android/`, paquete `com.senda`)

Dos módulos Gradle (detalle y dueños de cada carpeta en la spec 100):

```
core/   módulo Kotlin JVM, sin Android: si importa android.*, no compila
  common/      Clock, DeviceIdentity y sus fakes
  protocol/    modelos, codec de paquetes, claves de dedup, mulberry32, config (001)
  gossip/      motor de reenvío puro
  mesh/        MeshTransport (interfaz + fake), AnnouncementRotation, BleConfig (104)
  storage/     interfaces de almacenamiento, vistas y fakes (102)
  sync/        SyncEngine, RetryPolicy, mapeo a filas, puertos y fakes (106)
  node/        MeshNode (coordinador), PowerPolicy, MeshStatus, MeshControl, AutoAlertController (107)
  geo/         distancia y rumbo
  motion/      ShakeDetector
app/    módulo Android, depende de :core
  common/      SystemClock, PrefsDeviceIdentity
  permissions/ PermissionChecker y pantalla de permisos
  navigation/  rutas y barra inferior
  mesh/ble/    BleMeshTransport: anuncio y escaneo reales (104)
  storage/     Room y SharedPreferences: implementación de las interfaces de :core/storage (102)
  sync/        OkHttp, conectividad y WorkManager de respaldo (106)
  location/    LocationSource, Compass, intent a Google Maps
  motion/      adaptador del acelerómetro
  service/     MeshService (Foreground Service), notificaciones, wake lock (107)
  features/
    sendalert/   pantalla y ViewModel de enviar alerta
    nearby/      alertas cercanas y "voy en camino"
    status/      estado de la malla y ajustes
  ui/theme/    tema y componentes compartidos
  AppContainer.kt   cableado
```
Pruebas: `core/src/test` (JVM, toda la lógica pura), `app/src/test` (JVM) y `app/src/androidTest` (dispositivo). Los fakes viven en `src/main`, junto a su interfaz.

### Web (`web/`): React + Vite + TypeScript, desplegado en GitHub Pages (detalle y dueños en la spec 200)
```
src/core/        TypeScript puro, sin React ni DOM: common/ (Clock, mulberry32), priority/ y zones/ (003)
src/data/        cliente Supabase, AlertRepository, AuthService y fakes
src/app/         App, inicio de sesión, layout y store del panel (reductor puro e idempotente)
src/ui/          tema (variables CSS), componentes base y formato
src/features/    list/  map/ (Leaflet + CARTO)  realtime/  demo/
src/strings/     textos visibles (es)
src/main.tsx     cableado
supabase/        migrations/  seed/ (vacío)
scripts/         verify-002.ts
```

### Simulación (`simulation/`)
```
src/engine/      motor de nodos y reglas del protocolo
src/viz/         visualización
src/metrics/     entrega, saltos, latencia, mensajes totales
```

### Specs y contratos (`specs/`)
```
shared/        000-003 contratos comunes
  vectors/     vectores de prueba en JSON (ver sección 3)
android/  web/  simulation/  integration/
design/        design briefs derivados de las specs de UI (si chocan, manda la spec)
plan-prioridades-android.md   plan de la fase actual (sección 6.1)
_plantilla.md
```

**Una carpeta por spec**, con el nombre de la spec (`NNN-nombre-de-la-spec/`), y dentro:

| Archivo | Qué es | Cuándo existe |
|---|---|---|
| `spec.md` | la spec: qué se construye y sus criterios (manda sobre los otros dos) | siempre |
| `plan.md` | cómo y en qué orden se construye, dueños y dependencias | antes de implementar |
| `task.md` | lista de tareas con casillas para marcar | antes de implementar |

Ejemplo: `specs/android/101-android-core-paquetes/spec.md`, `.../plan.md` y `.../task.md`.

## 3. Un solo protocolo, dos implementaciones

Android (Kotlin) y el simulador (JavaScript) no pueden compartir código. Comparten **vectores de prueba** en `specs/shared/vectors/`:
- Paquetes de ejemplo con su codificación exacta en bytes.
- Escenarios de reenvío con semilla fija y el resultado esperado.

Ambas implementaciones deben pasar los mismos vectores. Si un vector cambia, cambia el spec 001.

## 4. Orden de specs: escalonado

Los números indican la capa. Se trabaja de arriba abajo.

| Capa | Specs | Regla |
|---|---|---|
| **0. Contratos** | 000-003 | Cada una tiene un dueño que la redacta. Los otros dos la revisan con comentarios en el PR, sin editarla. No avanza nada sin ellas. |
| **1. Base de cada área** | 100 Android · 200 web/Supabase | Proyecto, estructura de carpetas, permisos, cableado. Dependen solo de la capa 0. |
| **2. Módulos** | 101-105 · 201-203 · 205 · 300 | **Independientes entre sí.** Cada uno depende solo de la capa 0 y de la base de su área. |
| **3. Composición** | 106-109 · 204 · 301 | Une módulos ya terminados. Consume sus interfaces o sus fakes. |
| **4. Integración** | 400 | Extremo a extremo con teléfonos y panel reales. |

### Reglas de independencia
1. Toda spec declara en su cabecera: `Depende de`, `Entrega` (interfaz + fake) y `Consume` (interfaces de otras specs).
2. Una spec solo puede depender de **interfaces** de specs anteriores, nunca de su implementación.
3. Entre módulos de la capa 2 no hay dependencias. Si aparece una, se extrae una interfaz a la capa 0 o 1.
4. Una spec se puede implementar con los fakes de lo que consume, sin esperar a nadie.
5. Una spec no se marca terminada sin sus pruebas y su fake entregados.

### Reglas para no pisarse
1. **Cada spec es dueña de su carpeta.** Nadie edita carpetas de una spec ajena.
2. Los archivos compartidos (`settings.gradle.kts`, los `build.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml`, `strings.xml`, `AppContainer`, `web/src/main.ts`) solo los edita el dueño de la spec base (100 / 200) o, en la capa 3, quien tenga la spec de composición. El resto pide el cambio en `MEMORY.md`.
3. Dos personas nunca trabajan a la vez en la misma spec ni en la misma carpeta.
4. Las specs compartidas (000-003, 400) y los vectores las edita solo su dueño. Los demás piden cambios en su propio archivo `memory/Pn.md`.
5. Cada persona escribe su memoria solo en `memory/Pn.md`. `MEMORY.md` (el índice) lo edita únicamente el coordinador.

## 5. Mapa de specs

> Los dueños indican quién ejecuta cada task. La fase de especificación, en la que una sola persona redactó todas las specs, **terminó el 2026-10-16**. Desde entonces cada persona trabaja solo sus specs, según su `WHOAMI.md` (regla 32).

Las specs son pocas y de módulos pequeños; algunas agrupan varios módulos relacionados.

### Compartidas (un dueño cada una; los otros dos revisan)
- 000 visión y alcance del MVP · **P2**
- 001 protocolo y paquetes (+ vectores) · **P1**
- 002 esquema de datos Supabase · **P3**
- 003 prioridad y zonas · **P2**

### Android
> Reparto temporal "prioridad Android" (sección 6.1). La columna Persona ya lo refleja.

| # | Spec | Capa | Depende de | Persona |
|---|---|---|---|---|
| 100 | android-base (módulos `:core` y `:app`, permisos, `Clock`, `DeviceIdentity`, `AppContainer`) | 1 | 000, 001 | 1 (100-A: build, primitivas, `AppContainer`) · 2 (100-B: permisos, UI base, navegación) |
| 101 | android-core-paquetes (modelos, codec, claves, fábricas, mulberry32, config) | 2 | 001, 100 (solo `:core`) | 1 |
| 102 | android-almacenamiento (Room) | 2 | 001, 100, 101 | 3 |
| 103 | android-gossip-motor (reenvío puro con semilla; incluye "voy en camino") | 2 | 101 | 1 |
| 104 | android-transporte-ble (advertiser/scanner; **prototipo de 1 hora primero**) | 2 | 001, 100, 101 | 3 |
| 105 | android-sensores (ubicación, brújula, distancia y rumbo, Google Maps, movimiento brusco) | 2 | 100 | 3 |
| 106 | android-sync-supabase (subida idempotente: servicio + WorkManager) | 3 | 002, 100, 101, 102 | 2 |
| 107 | android-servicio-primer-plano (une 102-106; energía y notificaciones) | 3 | 100-106 (interfaces) | 1 |
| 108 | android-ui-enviar-alerta | 3 | 100, 101, 102, 105, 107 (interfaces) | 2 |
| 109 | android-ui-alertas-cercanas-y-estado (lista, "voy en camino", estado y ajustes) | 3 | 100, 101, 102, 103, 105, 106, 107 (interfaces) | 2 |

### Web y Supabase (Persona 3)
| # | Spec | Capa | Depende de |
|---|---|---|---|
| 200 | web-base-supabase (proyecto, migraciones, RLS, estructura) | 1 | 002 |
| 201 | web-core-prioridad-y-zonas (TypeScript puro) | 2 | 003, 200 (solo `src/core/`) |
| 202 | web-lista-prioridad | 2 | 200, 201 |
| 203 | web-mapa-celdas-y-colores | 2 | 200, 201 |
| 204 | web-tiempo-real-estados-y-voluntarios | 3 | 200, 202, 203 |
| 205 | web-modo-demo (datos simulados rotulados) | 2 | 200 |

### Simulación (al final)
| # | Spec | Capa | Depende de | Persona |
|---|---|---|---|---|
| 300 | sim-motor-protocolo (carga de vectores y reglas) | 2 | 001 | 3 |
| 301 | sim-visualizacion-y-metricas | 3 | 300 | 1 |

### Integración
- 400 demo de extremo a extremo · dueño **P3**; los demás aportan desde su propio archivo de memoria

## 6. Olas de trabajo

### 6.1 Fase actual: prioridad Android (temporal)

Desde el 2026-10-16, los tres trabajan en la app Android para liberar primero el prototipo visual y después la creación y propagación de alertas. El detalle (fases, hitos, contratos y puntos de contacto) está en **`specs/plan-prioridades-android.md`**, que manda mientras dure esta fase.

| Persona | Rol en Android | Specs | Carpetas |
|---|---|---|---|
| **P1** | Base y núcleo de la malla | 100-A, 101, 103, 107 | Gradle y manifiesto, `core/common`, `core/protocol`, `core/gossip`, `core/node`, `app/common`, `app/service`, `AppContainer.kt`, `SendaApplication.kt` |
| **P2** | Interfaz y prototipo visual | 100-B, 108, 109, 106, vectores | `app/permissions`, `app/navigation`, `app/ui`, `app/features/*`, `app/src/debug/**/prototype`, `MainActivity.kt`, `strings.xml`, `core/sync`, `app/sync`, `specs/shared/vectors/` |
| **P3** | Radio, sensores y datos | 104, 105, 102 | `core/mesh`, `app/mesh/ble`, `app/src/debug/**/mesh`, `core/geo`, `core/motion`, `app/location`, `app/motion`, `core/storage`, `app/storage`, `app/schemas` |

| Fase | Resultado |
|---|---|
| **0** | Esqueleto que compila, contratos (interfaces + fakes) de todos y `AppContainer` con fakes. En paralelo: vectores (P2) y prototipo BLE de 1 hora (P3) |
| **1** | **Prototipo visual:** APK con todas las pantallas sobre datos de prueba rotulados |
| **2** | **Funciones básicas:** alerta creada en A llega a C con 3 teléfonos |
| **3** | **Mejoras:** subida a Supabase (con la migración de la 002 por P3), alerta automática, notificaciones y energía |

Mientras dure esta fase, `strings.xml`, `MainActivity.kt` y `navigation/` son de P2, aunque la 100 diga otra cosa. Los cambios en Gradle, el manifiesto y `AppContainer.kt` los hace P1, a pedido.

### 6.2 Olas originales (rigen al terminar la fase Android)


El trabajo va por olas. Dentro de una ola, cada persona trabaja en carpetas distintas, así nunca coinciden. Una persona pasa a la siguiente ola cuando sus specs están terminadas y registradas en `MEMORY.md`.

| Ola | Persona 1 (malla y servicio) | Persona 2 (datos, sensores, UI) | Persona 3 (web) |
|---|---|---|---|
| **0** | 001 (revisa 000, 002, 003) | 000, 003 (revisa 001, 002) | 002 (revisa 000, 001, 003) |
| **1** | 100 | vectores de `specs/shared/vectors/` | 200 |
| **2** | 101 → 103 → 104 | 105, 102 | 201, 202, 203, 205 |
| **3** | 107 | 106, 108, 109 | 204 |
| **4** | aporta a 400 | aporta a 400 | 400 (dueño) |
| **5** | 301 | | 300 |

Carga original: Persona 1 tiene 7 specs (5 de código, 001 y 301), Persona 2 tiene 7 (5 de código y 2 documentos cortos) y Persona 3 tiene 9 (las web son pequeñas, más 002, 300 y 400). Durante la fase Android, la carga es la de la sección 6.1: la 102, la 104 y la 105 pasan a P3, la 100 se divide entre P1 y P2, y la web espera.

## 7. Plantilla de cabecera de spec

```
# NNN nombre
Capa: N · Persona: X
Depende de: <specs>
Entrega: <interfaz> + fake
Consume: <interfaces de otras specs>
Objetivo / Alcance / Fuera de alcance
Criterios de aceptación (verificables)
Tareas
```
