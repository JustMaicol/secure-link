# 000 visión y alcance del MVP

Capa: 0 · Redacta: P2 · Revisan: P1, P3
Depende de: ninguna
Entrega: este documento (contrato de alcance para todas las specs)
Consume: `PROJECT.md`, `AGENTS.md`

## Objetivo

Definir qué construye, qué simula y qué deja fuera el MVP de **Senda** para el rally de un día, de modo que cada spec posterior pueda derivarse de aquí sin reinterpretar la idea.

## Problema

Tras un desastre (terremoto, alud, inundación) cae la red celular y la demanda de ayuda supera a los equipos de respuesta. Las autoridades no saben a quién atender primero ni dónde concentrar recursos, y las personas afectadas no tienen cómo pedir ayuda.

## Tesis que el MVP debe demostrar

Una alerta nace en un teléfono sin internet, salta de teléfono en teléfono por BLE, llega una sola vez al servidor desde el primer dispositivo con conexión, y las autoridades la ven priorizada en un panel. Otros usuarios cercanos pueden verla y acudir.

## Usuarios

| Usuario | Qué hace |
|---|---|
| Persona afectada | Envía una alerta (formulario rápido o por movimiento brusco). |
| Relay | Cualquier teléfono con la app. Recibe, guarda y reenvía alertas. |
| Voluntario | Ve alertas cercanas, sigue la distancia y el rumbo, y anuncia "voy en camino". |
| Autoridad | Inicia sesión en el panel web para ver y priorizar emergencias. |

## Alcance del MVP

**Se construye (real)**
1. App Android (solo Samsung Galaxy, `minSdk 34`): enviar alerta, alertas cercanas con distancia y rumbo, "voy en camino", estado de la malla.
2. Alerta por movimiento brusco: cuenta regresiva de cancelación (duración en la configuración, spec 105), tipo "atrapado o grave", una persona.
3. Botón "Abrir en Google Maps" en cada alerta cercana. Solo útil con datos; no forma parte de los criterios de aceptación.
4. Propagación BLE por advertising (gossip) con deduplicación por ID inmutable y límite de saltos.
5. Subida idempotente a Supabase cuando hay internet.
6. Panel web: lista de emergencias por prioridad y mapa de celdas por colores. Acceso con Supabase Auth y un único usuario "autoridad"; RLS permite leer solo a usuarios autenticados. La clave pública de la app solo inserta (`CONSTITUTION.md`, regla 26). Detalle en la spec 002.
7. Filtro de depuración en ajustes: lista de etiquetas de relay (`relayTag`, spec 001) cuyos anuncios la app ignora al recibir. Bloquea a quien emite físicamente el anuncio, no al origen de la alerta. Sirve para forzar la topología de la demo en una sala y se rotula como simulación (regla 30).

**Opcional (solo si sobra tiempo; lo primero que se recorta junto con la simulación)**
- `CANCELADA / A_SALVO`: la spec 001 reserva su tipo de paquete y su formato, aunque no se implemente.

**Se simula (rotulado como simulación)**
- La escala: cientos de nodos y alertas, y las métricas de entrega, saltos y latencia.
- Los datos de demostración del panel (modo demo).
- La topología de la demo cuando se usa el filtro de depuración.

**Fuera de alcance (trabajo futuro)**
Firmas y cifrado, privacidad por niveles, verificación de voluntarios, rutas seguras, logística de recursos, presencia/densidad de dispositivos, Wi-Fi Direct, mapas offline, iOS y otras marcas, interfaz en otros idiomas.

## Idioma

- Interfaz de la app y del panel: **español** por ahora.
- Código (identificadores, nombres de archivos, clases, funciones, tablas y columnas): **inglés**.
- Comentarios, specs, memoria y documentación: **español**.

## Decisiones vigentes que esta spec fija

- Transporte: BLE advertising/scan; Nearby Connections solo como respaldo.
- La deduplicación usa solo campos inmutables (detalle en spec 001).
- Ubicación exacta visible para cualquier teléfono con la app (limitación declarada).
- Mapa por colores: amarillo (comida/agua), naranja (asistencia médica), rojo (atrapado o grave).
- Metodología SDD, reglas en `CONSTITUTION.md`.

## Criterios de aceptación (demo de extremo a extremo)

Los criterios no dependen del transporte: valen igual con BLE o con el respaldo Nearby.

1. Con 3 teléfonos A, B y C, sin internet, donde C bloquea a A con el filtro de depuración (A llega a B, B llega a C, A no llega a C), una alerta creada en A aparece en la lista de C.
2. C muestra la alerta con distancia y rumbo hacia A.
3. Cuando un teléfono recupera internet, la alerta aparece en el panel **una sola vez**, aunque haya llegado por varios caminos o la suban varios teléfonos.
4. El panel ordena las alertas por prioridad y las muestra en el mapa con el color de su tipo.
5. Un voluntario anuncia "voy en camino" y el panel y los teléfonos muestran el contador actualizado.
6. Un movimiento brusco en A inicia la cuenta regresiva. Si se cancela, no se emite nada; si termina, se emite una alerta "atrapado o grave" con una persona que llega a B.
7. El panel no muestra alertas sin iniciar sesión.
8. El modo demo, la simulación y el filtro de depuración están rotulados como simulación en la interfaz.
9. El simulador reproduce los vectores de prueba de la spec 001 con el mismo resultado que la app.

## Riesgos conocidos

| Riesgo | Mitigación |
|---|---|
| El gossip BLE no funciona en segundo plano en los Samsung | Prototipo de 1 hora en la spec 104; respaldo con Nearby detrás de `MeshTransport` |
| Android 16/17 cambian servicios en primer plano o BLE | Revisar documentación oficial en la spec 107 |
| One UI suspende la app | Desactivar a mano "Aplicaciones en suspensión" en los teléfonos de la demo |
| Sin densidad de instalación la malla no funciona | Declararlo como limitación; la simulación muestra el efecto de la densidad |
| Consumo de batería de escanear y anunciar | Ciclo de trabajo definido en la spec 104 |

## Pendientes que esta spec no resuelve

- Modelos Samsung exactos de la demo.
- Formato de paquetes y estado `CANCELADA / A_SALVO` (spec 001).
- Fórmula de prioridad (spec 003).
- Umbral del movimiento brusco (spec 105). La cuenta regresiva quedó en 15 s (spec 108).
- Esquema, políticas RLS y usuario "autoridad" (spec 002).
- Logo e identidad visual del proyecto (el nombre es **Senda**, decidido el 2026-10-17).

## Tareas

1. Revisión de P1 y P3 (comentarios en el PR).
2. Aprobación por los tres antes de iniciar la capa 1.
