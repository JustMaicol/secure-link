# 203 web-mapa-celdas-y-colores

Capa: 2 · Persona: P3
Depende de: 200, 201
Entrega: columna derecha del panel: mapa Leaflet con zonas por color, marcadores, leyenda y selección compartida, en `web/src/features/map/`; función pura `buildMapLayers`
Consume: `useDashboard`, `createFakeDashboard`, configuración, tema, componentes y textos (200); `buildZones`, `zoneStyle`, `cellBounds` y `sortAlerts` (201)

## Objetivo

Mostrar dónde se concentra la necesidad: cada zona con el color de su caso más grave y más intensa cuanta más gente hay, y llevar a la autoridad hasta una alerta concreta.

## Alcance

- Mapa base CARTO, claro u oscuro según el tema.
- Zonas (celdas de la 003) con color e intensidad.
- Marcadores individuales al acercarse.
- Leyenda, tooltips y controles.
- Selección compartida con la lista (store de la 200).
- Animaciones: acercarse a una selección, resaltar lo seleccionado y lo nuevo.
- Catálogo de contenido visible (200, sección 9).

## Fuera de alcance

- Calcular zonas y estilos (201), la lista (202), decidir qué es nuevo (204) y generar datos de demo (205).
- Editar el estilo del mapa base: CARTO entrega imágenes ya dibujadas; solo se elige entre claro y oscuro.

## 1. Mapa base

| Tema | URL de teselas |
|---|---|
| claro | `https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png` |
| oscuro | `https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png` |

- Atribución visible y obligatoria: `© OpenStreetMap contributors © CARTO`.
- **Pendiente antes de la demo:** confirmar en los términos de CARTO que el uso gratuito cubre un MVP no comercial de un rally. Se registra en `memory/P3.md`. Si no lo cubre, se cambia a las teselas estándar de OpenStreetMap, que solo implica cambiar la URL.

## 2. Configuración

`features/map/mapConfig.ts` (regla 17):

| Constante | Valor | Para |
|---|---|---|
| `TILE_URL_LIGHT`, `TILE_URL_DARK`, `TILE_ATTRIBUTION` | sección 1 | mapa base |
| `MARKER_MIN_ZOOM` | 15 | desde este zoom se ven los marcadores individuales |
| `FLY_TO_ZOOM` | 16 | zoom mínimo al ir a una alerta seleccionada |
| `FLY_TO_DURATION_S` | 0.8 | duración de la animación |
| `TILE_ERROR_THRESHOLD` | 3 | errores de teselas seguidos antes de mostrar el aviso |

## 3. Capas (pura)

```ts
// spec 203
export interface MapLayers {
  zones: Array<{ cellId: string; bounds: { southE7; westE7; northE7; eastE7 }; color: string;
                 opacityPercent: number; simulated: boolean; selected: boolean; isNew: boolean; tooltip: ZoneTooltip }>
  markers: Array<{ alertId: string; latE7: number; lonE7: number; tier: 0 | 1 | 2;
                   simulated: boolean; selected: boolean; isNew: boolean }>
}
export function buildMapLayers(view: ReturnType<typeof useDashboard>): MapLayers
```

- Las zonas salen de `buildZones` y `zoneStyle` (201). Las alertas resueltas no aparecen (003).
- `simulated` es verdadero si `simulatedCount > 0`.
- `isNew` es verdadero si alguna alerta de la zona, o la del marcador, está en `newSince` (204).
- Los marcadores se dibujan en orden inverso a `sortAlerts`: la alerta más prioritaria queda encima.
- El componente de Leaflet solo aplica `MapLayers`: agrega, actualiza o quita capas por `cellId` y `alertId`, sin recrear todo el mapa.

## 4. Comportamiento

1. **Vista inicial:** si hay zonas, ajusta la vista a todas ellas; si no, usa el centro y el zoom de la configuración (200, sección 3).
2. **Zonas:** rectángulos con el color y la opacidad de `zoneStyle`, y un borde del mismo color. Una zona con alertas simuladas lleva un borde **discontinuo** y su tooltip lo dice.
3. **Marcadores:** desde `MARKER_MIN_ZOOM`, un círculo por alerta con el color de su tipo.
4. **Tooltip de zona** (al pasar el ratón): conteos y tipo más grave (sección 6).
5. **Clic en una zona:** `select({ cellId })`. La zona queda resaltada (borde más grueso) y la lista (202) se filtra.
6. **Clic en un marcador:** `select({ alertId })`.
7. **Selección de una alerta** (desde la lista o el mapa): `flyTo` hasta la alerta con zoom `max(actual, FLY_TO_ZOOM)` durante `FLY_TO_DURATION_S`, y el marcador se agranda.
8. **Nuevo** (`isNew`): la zona o el marcador muestra un pulso con CSS mientras dure en `newSince`. La duración la decide la 204.
9. **Botón "Ver todas":** vuelve a ajustar la vista a todas las zonas.
10. **Tema:** al cambiar de tema, cambia la URL de teselas sin perder la vista.
11. **Teselas que fallan:** tras `TILE_ERROR_THRESHOLD` errores seguidos, aviso de error. Las zonas y los marcadores se siguen dibujando sobre el fondo vacío.
12. **Accesibilidad:** el color nunca va solo. El tooltip y la leyenda nombran el tipo, y la lista (202) siempre está al lado con la misma información en texto.

## 5. Leyenda (esquina inferior izquierda)

| Variable | Contenido |
|---|---|
| tipos | tres muestras de color con su texto: Atrapado o grave · Asistencia médica · Comida o agua |
| intensidad | tres tramos de la 003: 1-4 · 5-19 · 20 o más personas |
| simulación | muestra de borde discontinuo: "Zona con alertas simuladas" (solo si hay) |
| escala | "Cada celda mide unos 280 m" |

## 6. Variables visibles

| Variable | Origen | Formato | Ejemplo | Si falta |
|---|---|---|---|---|
| `zone.alertCount` | 201 | número | `5 alertas` | — |
| `zone.peopleSum` | 201 | número | `12 personas` | — |
| `zone.volunteersSum` | 201 | número | `2 voluntarios en camino` | `Sin voluntarios` |
| `zone.maxTier` | 201 | `TypeLabel` | Más grave: Atrapado o grave | — |
| `zone.simulatedCount` | 201 | número | `Incluye 3 simuladas` | se oculta |
| `marker` | fila | color de tipo; tooltip con tipo y personas | Atrapado o grave · 3 personas | — |
| `tileErrors` | eventos `tileerror` de Leaflet | contador interno | — | — |

## 7. Mensajes

| id | Cuándo | Dónde | Tono | Texto provisional | Acción |
|---|---|---|---|---|---|
| `map_zone_tooltip` | al pasar sobre una zona | tooltip | — | 5 alertas · 12 personas · 2 voluntarios en camino · Más grave: Atrapado o grave | — |
| `map_zone_tooltip_no_volunteers` | `volunteersSum = 0` | tooltip | WARNING | Sin voluntarios | — |
| `map_zone_tooltip_simulated` | `simulatedCount > 0` | tooltip | SIMULATION | Incluye 3 simuladas | — |
| `map_marker_tooltip` | al pasar sobre un marcador | tooltip | — | Atrapado o grave · 3 personas · hace 30 min | — |
| `map_fit_all` | hay zonas | control | — | Ver todas | ajusta la vista |
| `map_empty` | sin zonas activas | capa sobre el mapa | INFO | Sin alertas activas en el mapa | — |
| `map_legend_title` | siempre | leyenda | — | Leyenda | — |
| `map_legend_intensity` | siempre | leyenda | — | Personas por zona: 1-4 · 5-19 · 20 o más | — |
| `map_legend_simulated` | hay zonas simuladas | leyenda | SIMULATION | Zona con alertas simuladas | — |
| `map_legend_scale` | siempre | leyenda | INFO | Cada celda mide unos 280 m | — |
| `map_tiles_error` | `TILE_ERROR_THRESHOLD` errores seguidos | banner sobre el mapa | ERROR | No se pudo cargar el mapa base. Las zonas se siguen mostrando. | Reintentar |
| `map_attribution` | siempre | esquina inferior derecha | — | © OpenStreetMap contributors © CARTO | — |

## Criterios de aceptación

1. Con los datos de la 003, sección 8, `buildMapLayers` produce las 4 zonas de su tabla con su color y su opacidad, sin la alerta resuelta.
2. Una zona con una alerta simulada tiene `simulated = true`; sin alertas simuladas, `false`.
3. Las selecciones del store se reflejan en `selected` de zonas y marcadores, y `isNew` en `newSince`.
4. Clic en una zona y en un marcador despachan `select` con `cellId` y `alertId` (prueba del componente con Leaflet en jsdom o con un adaptador falso del mapa).
5. Al cambiar el tema, cambia la URL de teselas (prueba del componente).
6. Tras `TILE_ERROR_THRESHOLD` errores, aparece `map_tiles_error`.
7. La atribución de CARTO y OpenStreetMap está siempre visible.
8. Cada `id` de la sección 7 existe en `src/strings/es.ts`.
9. Los términos de uso de CARTO quedan revisados y registrados en `memory/P3.md` antes de la demo (manual).

## Tareas

1. `mapConfig.ts` y textos.
2. `buildMapLayers`, con sus pruebas (criterios 1-3).
3. Componente de Leaflet que aplica las capas, con selección, `flyTo` y tema (criterios 4 y 5).
4. Leyenda, tooltips, "Ver todas" y aviso de teselas (criterios 6 y 7).
5. Prueba del catálogo (criterio 8) y revisión de los términos de CARTO (criterio 9).
