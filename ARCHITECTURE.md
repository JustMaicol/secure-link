# ARCHITECTURE.md

Estructura de carpetas, modularización y orden de specs. Obligatorio por `CONSTITUTION.md`.

## 1. Principios

- **Capas con dependencia en un solo sentido:** `core` (lógica pura) ← `adaptadores` (BLE, Room, Supabase, GPS) ← `features` (UI) ← `composition root`. Una capa nunca importa de la que está encima.
- **`core` no importa nada de Android ni de la web.** Es Kotlin puro (Android) o TypeScript puro (web). Se prueba sin dispositivo, red ni reloj.
- **Cada módulo se consume por una interfaz.** Quien consume una interfaz no conoce su implementación.
- **Cada módulo entrega un fake** de su interfaz (en memoria, determinista). Con él, los demás avanzan sin esperar.
- **El cableado vive en un solo lugar** (`AppContainer` en Android, `main` en web), manual y explícito, sin inyección por reflexión.

## 2. Carpetas

### Android (`android/app/src/main/java/<paquete>/`)
```
core/        modelos (Alert, Response, PacketType), codec de paquetes, dedup, Clock, SeededRandom, config
mesh/        MeshTransport (interfaz), gossip/ (motor puro), ble/ (advertiser y scanner)
storage/     Room: entidades, DAO, repositorios (interfaz + implementación + fake)
sync/        subida a Supabase (cola de salida idempotente)
location/    LocationSource, Compass, cálculo de distancia y rumbo (puro), intent a Google Maps
motion/      ShakeDetector (puro) y adaptador del sensor
service/     Foreground Service que une mesh + storage + sync
features/
  sendalert/     pantalla y ViewModel de enviar alerta
  nearby/        alertas cercanas y "voy en camino"
  status/        estado de la malla y ajustes
ui/theme/    tema y componentes compartidos
AppContainer.kt   cableado
```
Pruebas en `src/test` (JVM, solo `core` y lógica pura) y `src/androidTest` (dispositivo).

### Web (`web/`)
```
src/core/        prioridad, celdas/zonas y colores (TypeScript puro)
src/data/        cliente Supabase y consultas
src/features/    list/  map/  realtime/  demo/
src/main.ts      cableado
supabase/        migrations/  policies/ (RLS)  seed/
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
_plantilla.md
```

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
2. Los archivos compartidos (`build.gradle`, `AndroidManifest.xml`, `AppContainer`, `web/src/main.ts`) solo los edita el dueño de la spec base (100 / 200) o, en la capa 3, quien tenga la spec de composición. El resto pide el cambio en `MEMORY.md`.
3. Dos personas nunca trabajan a la vez en la misma spec ni en la misma carpeta.
4. Las specs compartidas (000-003, 400) y los vectores las edita solo su dueño. Los demás piden cambios en su propio archivo `memory/Pn.md`.
5. Cada persona escribe su memoria solo en `memory/Pn.md`. `MEMORY.md` (el índice) lo edita únicamente el coordinador.

## 5. Mapa de specs

Las specs son pocas y de módulos pequeños; algunas agrupan varios módulos relacionados.

### Compartidas (un dueño cada una; los otros dos revisan)
- 000 visión y alcance del MVP · **P2**
- 001 protocolo y paquetes (+ vectores) · **P1**
- 002 esquema de datos Supabase · **P3**
- 003 prioridad y zonas · **P2**

### Android
| # | Spec | Capa | Depende de | Persona |
|---|---|---|---|---|
| 100 | android-base (proyecto, estructura, permisos, `AppContainer`) | 1 | 000 | 1 |
| 101 | android-core-paquetes (modelos, codec, dedup) | 2 | 001 | 1 |
| 102 | android-almacenamiento (Room) | 2 | 100, 101 (solo modelos) | 2 |
| 103 | android-gossip-motor (reenvío puro con semilla; incluye "voy en camino") | 2 | 101 | 1 |
| 104 | android-transporte-ble (advertiser/scanner; **prototipo de 1 hora primero**) | 2 | 001, 100 | 1 |
| 105 | android-sensores (ubicación, brújula, distancia y rumbo, Google Maps, movimiento brusco) | 2 | 100 | 2 |
| 106 | android-sync-supabase (cola de subida idempotente) | 3 | 002, 100, 102 | 2 |
| 107 | android-servicio-primer-plano (une 103, 104, 102) | 3 | 102, 103, 104 | 1 |
| 108 | android-ui-enviar-alerta | 3 | 101, 102, 105 | 2 |
| 109 | android-ui-alertas-cercanas-y-estado (lista, "voy en camino", estado y ajustes) | 3 | 101, 102, 103, 105, 107 | 2 |

### Web y Supabase (Persona 3)
| # | Spec | Capa | Depende de |
|---|---|---|---|
| 200 | web-base-supabase (proyecto, migraciones, RLS, estructura) | 1 | 002 |
| 201 | web-core-prioridad-y-zonas (TypeScript puro) | 2 | 003 |
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

El trabajo va por olas. Dentro de una ola, cada persona trabaja en carpetas distintas, así nunca coinciden. Una persona pasa a la siguiente ola cuando sus specs están terminadas y registradas en `MEMORY.md`.

| Ola | Persona 1 (malla y servicio) | Persona 2 (datos, sensores, UI) | Persona 3 (web) |
|---|---|---|---|
| **0** | 001 (revisa 000, 002, 003) | 000, 003 (revisa 001, 002) | 002 (revisa 000, 001, 003) |
| **1** | 100 | vectores de `specs/shared/vectors/` | 200 |
| **2** | 101 → 103 → 104 | 105, 102 | 201, 202, 203, 205 |
| **3** | 107 | 106, 108, 109 | 204 |
| **4** | aporta a 400 | aporta a 400 | 400 (dueño) |
| **5** | 301 | | 300 |

Carga: Persona 1 tiene 7 specs (5 de código, 001 y 301), Persona 2 tiene 7 (5 de código y 2 documentos cortos) y Persona 3 tiene 9 (las web son pequeñas, más 002, 300 y 400). Los documentos son breves.

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
