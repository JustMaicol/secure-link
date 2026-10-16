# AGENTS.md

Guía para cualquier agente de IA que trabaje en este repositorio. Leer primero `CONSTITUTION.md`: sus reglas son obligatorias.

## Qué es el proyecto

Sistema de alertas y priorización post-emergencia (terremoto, alud, inundación). Cuando cae la red celular, las alertas viajan de teléfono a teléfono por Bluetooth Low Energy. Los demás usuarios las ven y pueden acudir, y el primer dispositivo que recupere internet las sube al servidor. Un panel permite a las autoridades ver y priorizar dónde atender. Detalle en `PROJECT.md`.

**Contexto del rally:** MVP para un rally internacional de un día. Se construye lo mínimo que demuestra la tesis y se simula la escala.

## Notas importantes

- **El nombre de la carpeta (`pwa-comunicacion`) viene de una primera idea que no cuajó. Ignorarlo.** No es una PWA: la app es **Android nativo**.
- `PROJECT.md` mezcla decisiones y lluvia de ideas. Las decisiones están marcadas como tales.
- Metodología: **SDD (Spec-Driven Development)**. Sin spec no hay código.
- **Dispositivos objetivo: solo Samsung Galaxy por ahora.** **`minSdk 34` (Android 14)**, `compileSdk`/`targetSdk` al más reciente disponible. Modelos exactos por confirmar.

## Stack

| Parte | Tecnología |
|---|---|
| App Android | Kotlin, Jetpack Compose (UI), ViewModel, Room (almacenamiento local) |
| Malla entre teléfonos | **BLE por advertising/scan (gossip), sin conexiones**: `BluetoothLeAdvertiser` y `BluetoothLeScanner` |
| Segundo plano | Foreground Service |
| Ubicación y orientación | FusedLocationProvider, acelerómetro y brújula (SensorManager) |
| Servidor | Supabase (Postgres, API REST, Realtime, RLS) |
| Panel web | HTML + JavaScript/TypeScript, Leaflet, `supabase-js` |
| Simulación | JavaScript en el navegador |

La malla va detrás de una interfaz `MeshTransport`. **Nearby Connections queda como respaldo**: si el prototipo BLE de la primera hora no funciona en los Samsung de la demo, se cambia la implementación sin tocar la lógica de reenvío. iOS está fuera de alcance.

## Estructura y specs

La estructura de carpetas, la modularización, el mapa de specs y sus reglas de independencia están en **`ARCHITECTURE.md`** (obligatorio). Resumen:

```
android/  web/ (con web/supabase/)  simulation/
specs/  shared/ (000-003 + vectors/)  android/  web/  simulation/  integration/
```

- Las specs se nombran `NNN-nombre-de-la-spec` y avanzan por capas: contratos (0xx) → base de cada área (x00) → módulos independientes → composición → integración.
- Cada spec declara de qué depende, qué entrega (interfaz + fake) y qué consume. Solo depende de interfaces, nunca de implementaciones.
- Reparto (detalle y olas en `ARCHITECTURE.md`): **Persona 1** malla y servicio (100, 101, 103, 104, 107) · **Persona 2** datos, sensores y UI (102, 105, 106, 108, 109) · **Persona 3** web y Supabase (200-205) y la simulación (300). Todos: 000-003 y 400.
- El trabajo va por olas: dentro de una ola nadie toca la misma carpeta. Cada spec es dueña de su carpeta.
- **Memoria obligatoria:** al empezar una tarea, `git pull` y leer `MEMORY.md` y `memory/P1.md`, `P2.md`, `P3.md`. Al terminar, registrar **solo en tu propio `memory/Pn.md`**. Nunca editar el de otro.
- Git: una rama por persona (`p1`, `p2`, `p3`), cada una edita solo sus carpetas y su archivo de memoria, así los PRs no chocan. Guía en `MEMORY.md`.
- La simulación (3xx) se trabaja al final y es lo primero que se recorta.
- El protocolo se comparte entre Android y el simulador mediante vectores de prueba en `specs/shared/vectors/`.

## Decisiones de diseño ya tomadas

### Propagación
- **P2P por gossip BLE** es la base. Cada teléfono recibe, guarda y vuelve a emitir.
- **Hub and spoke no es la topología de propagación** (un hub es un punto único de falla). El hub solo existe como **rol de gateway**: el teléfono que tiene internet sube los paquetes a Supabase. Un nodo fijo de las autoridades sería un gateway permanente (trabajo futuro).
- **Wi-Fi Direct se descarta para el MVP.** Es en la práctica un hub and spoke y no escala como malla. Posible uso futuro para transferencias grandes.
- Reenvío: una sola vez por ID, retardo aleatorio con semilla, límite de saltos (TTL). Cada alerta se re-anuncia varias veces porque BLE advertising no confirma recepción. La supresión por contador queda como mejora.

### Paquetes (spec 001)
- Límite duro: **27 bytes** útiles por anuncio legacy.
- Tipos: `ALERTA` (≈22 bytes: tipo de paquete, deviceId, contador, tipo de emergencia, vulnerables, personas, latitud, longitud, hora, TTL) y `VOY_EN_CAMINO` (≈16 bytes: ID de la alerta, deviceId del voluntario, hora, TTL). Una posible tercera clase `CANCELADA / A_SALVO` se define en el spec 001.
- **La deduplicación usa solo campos inmutables.** `ALERTA`: `(deviceId, contador)`. `VOY_EN_CAMINO`: `(alertId, deviceId del voluntario)`. Nunca un hash del paquete completo, porque el TTL y la marca de relay cambian en cada salto.
- Marca del relay: contador de saltos (TTL). Una etiqueta de 2 bytes del último relay es opcional. No se acumula la ruta completa.

### Zonas y mapa
- La ubicación del mensaje agrupa en celdas. Se cuentan IDs distintos por celda.
- Colores: amarillo (comida/agua), naranja (asistencia médica), rojo (atrapado o grave). La celda toma el color del tipo más grave.

### Acudir a una alerta
- La lista de alertas cercanas muestra tipo, personas, hora, distancia, rumbo y cuántos voluntarios van en camino.
- **Distancia y rumbo** se calculan con GPS y brújula y **funcionan sin internet**. Es la ayuda de navegación principal.
- **Botón "Abrir en Google Maps"** como comodidad cuando hay datos. Sin internet, Maps no calcula rutas salvo que el área esté descargada.
- **"Voy en camino"** es un paquete propio que se propaga igual que las alertas, para evitar que muchos voluntarios acudan a la misma.

### Privacidad (opción A, MVP)
- **Cualquier teléfono con la app ve la ubicación exacta de las alertas.** La ubicación es lo más importante para poder acudir, y el usuario la comparte voluntariamente por una emergencia.
- Riesgo asumido y declarado como limitación: alguien podría usar una ubicación para aprovecharse de una víctima. La visibilidad por niveles y la verificación de voluntarios son trabajo futuro.

## Qué sirve, qué no y qué se usará

| Idea | Probabilidad | Nota |
|---|---|---|
| App nativa Android + foreground service | Alta | Base de todo |
| Gossip BLE por advertising | Alta | Con prototipo inicial de una hora |
| Formulario rápido con tipo de emergencia | Alta | Define el color del mapa |
| Alerta por movimiento brusco | Alta | Con cuenta regresiva de cancelación |
| ID único propagado, dedup por campos inmutables | Alta | Ya decidido |
| Panel + mapa de calor por colores | Alta | Ya decidido |
| Subida al servidor al recuperar internet | Alta | Cualquier dispositivo actúa de gateway |
| Distancia y rumbo con brújula y GPS | Alta | Respaldo offline |
| Paquete "voy en camino" | Alta | Coordina a los voluntarios |
| Botón de Google Maps | Media | Solo útil con datos |
| Nearby Connections | Media | Respaldo si falla el BLE |
| Simulación del protocolo | Media | Se hace al final |
| Voluntarios verificados, rutas seguras, logística | Baja | Trabajo futuro |
| Wi-Fi Direct | Baja | No escala |
| Soporte iOS u otras marcas | Muy baja | Fuera de alcance |

## Límites conocidos de la tecnología

- BLE alcanza unos 10-50 m, menos entre escombros.
- BLE advertising no confirma recepción: hay que re-anunciar. Un teléfono sostiene pocos anuncios a la vez, por lo que se rota el contenido en una cola.
- Android limita los reinicios de escaneo (unos 5 cada 30 s) y con la pantalla apagada exige filtros (filtro por ID de fabricante).
- Android no garantiza actividad en segundo plano en ultra ahorro. Samsung One UI suspende apps ("Aplicaciones en suspensión"): se desactiva a mano para la demo.
- Escanear y anunciar es lo que más consume batería: se usa ciclo de trabajo.
- La malla solo funciona con densidad de instalación previa al evento.
- El fabricante del teléfono importa; el del sensor no.

## Pendientes por definir

- Modelos Samsung exactos (verificar soporte de anuncios BLE en cada uno).
- Cambios de Android 16/17 en servicios en primer plano y BLE (revisar documentación oficial en el spec 104).
- Formato exacto de los paquetes (spec 001), incluido `CANCELADA / A_SALVO`, y fórmula de prioridad (spec 003).
- Seguridad: firmas, límite de envíos, verificación de voluntarios.
- Ciclo de vida de la alerta y confirmaciones.
- Nombre del proyecto.
