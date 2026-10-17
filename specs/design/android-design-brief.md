# Design brief · app Android

Para: quien diseña la app en Figma.
Sale de: specs 100 (permisos, componentes y formato), 108 (enviar alerta) y 109 (cercanas y estado). **Si este brief y una spec no coinciden, manda la spec**: avisa para corregir el brief.
Todos los textos con su identificador están en el **anexo A**: no hace falta abrir las specs para diseñar.
Fuera de este brief: el panel web de las autoridades y el simulador.

## 1. Qué es y en qué situación se usa

**secure-link** (nombre provisional) es una app Android para pedir y ofrecer ayuda después de un desastre (terremoto, alud, inundación), cuando ya no hay red celular. Las alertas viajan de teléfono en teléfono por Bluetooth, sin internet. Cuando un teléfono recupera la conexión, las sube a un servidor que usan las autoridades.

La app se usa en las peores condiciones posibles. El diseño parte de ahí:

| Condición | Qué implica para el diseño |
|---|---|
| Miedo, estrés, quizá heridas | Cada pantalla se entiende en 2 segundos. Una sola acción principal por pantalla |
| Una mano, temblor, guantes, polvo en la pantalla | Áreas táctiles grandes (mínimo 48 dp; las acciones principales, mucho más). Nada que exija precisión |
| De noche, sin luz, o a pleno sol | Tema oscuro prioritario y tema claro, ambos con buen contraste |
| Batería que se agota | El oscuro ayuda (los Samsung tienen pantallas AMOLED). Sin animaciones continuas innecesarias |
| Sin internet | La app nunca promete lo que no pasó: cada estado dice exactamente qué ocurrió con la alerta |
| Demo en un rally internacional | Lo simulado se ve rotulado como simulación, siempre |

Dispositivos: solo Samsung Galaxy con Android 14 o superior. Idioma: español.

## 2. Quién la usa

| Persona | Qué necesita | Pantallas |
|---|---|---|
| **Persona afectada** | Pedir ayuda en pocos toques, o sin tocar nada si sufre un golpe; saber que su alerta salió y si alguien viene | Enviar alerta, Mi alerta, Cuenta regresiva |
| **Voluntario** | Ver qué alertas hay cerca, ir hacia ellas sin internet y avisar que va | Cercanas, Detalle |
| **Cualquier teléfono (relay)** | Nada: retransmite solo en segundo plano. Solo ve la notificación del servicio | Notificaciones |
| **Quien prepara la demo** | Ver si la malla y la subida funcionan, y simular distancia entre teléfonos | Estado |

## 3. Dirección visual: calma y claridad

1. **Sobria por defecto.** Fondo y superficies neutros. Nada decorativo que compita con la información.
2. **El color tiene dos únicos trabajos:**
   - **Tipo de emergencia:** rojo (atrapado o grave), naranja (asistencia médica) y amarillo (comida o agua). Es el código que también usa el mapa del panel web.
   - **Tono de estado:** información, éxito, advertencia, error y simulación.
3. **Nunca solo color.** El tipo de emergencia siempre va con su texto, y un estado con su texto o un ícono. Debe entenderse con daltonismo y en blanco y negro.
4. **Honestidad.** Los estados distinguen "guardada", "anunciándose", "retransmitida" y "subida". El diseño no debe hacerlos parecer lo mismo ni exagerar el progreso.
5. **Jerarquía evidente.** Lo más importante de cada pantalla (la acción de pedir ayuda, la flecha hacia la alerta) es claramente lo más grande.
6. **Texto escalable.** Todo debe funcionar con el tamaño de letra del sistema al 130 % o más, sin cortar textos.

Los colores, la tipografía y el espaciado finales los decide el diseño. Valores de partida que ya existen en el código temporal:
- Tipos de emergencia: rojo `#DC2626`, naranja `#F97316` y amarillo `#EAB308`. Son los mismos del panel web (spec 003): si el diseño los cambia, avisa para cambiarlos también ahí.
- Espaciado temporal: escala de 4, 8, 16, 24 y 32 dp.

## 4. Reglas del contenido (no negociables)

Vienen del protocolo: una alerta cabe en 27 bytes y viaja sin internet.

- **No hay texto libre.** La persona no escribe mensajes: elige entre opciones fijas.
- **Tipos de emergencia:** exactamente tres. Comida o agua · Asistencia médica · Atrapado o situación grave.
- **Personas:** de 1 a 255. Con 255 se muestra "255 o más personas".
- **Vulnerables:** cuatro opciones, opcionales: Niños · Adultos mayores · Personas heridas · Movilidad reducida.
- **Alerta automática** (por movimiento brusco): siempre es atrapado o grave, con 1 persona. Se rotula "Automática".
- **"Voy en camino" no se puede deshacer**, y tampoco cancelar una alerta ni marcar "a salvo". Por eso llevan confirmación.
- **Los textos de este brief son provisionales.** Mejóralos con libertad, sin cambiar lo que significan. Cada uno tiene un `id`: úsalo como nombre de la capa en Figma para que el paso al código sea directo.

### Formato de los valores

| Valor | Cómo se muestra |
|---|---|
| Distancia | `850 m` (de 10 en 10) · `1,2 km` · `123 km` · sin dato: `—` |
| Tiempo transcurrido | `ahora` · `hace 5 min` · `hace 3 h` · `hace 2 d` |
| Hora | `07:33` |
| Personas | `1 persona` · `3 personas` · `255 o más personas` |
| Precisión de ubicación | `±15 m` |
| Rumbo | `N`, `NE`, `E`, `SE`, `S`, `SO`, `O`, `NO`, y en el detalle también grados: `45° NE` |
| Cuenta atrás | `12` (segundos) · `0:42` (minutos) |
| ID del teléfono | `a1b2 c3d4 e5f6` |
| Etiqueta de relay | `e5f6` |

## 5. Sistema base

### 5.1 Tonos

| Tono | Uso | Ejemplo |
|---|---|---|
| `INFO` | información neutra o un proceso en curso | "Buscando tu ubicación…" |
| `SUCCESS` | algo se logró | "Retransmitida por 2 teléfonos cercanos" |
| `WARNING` | algo puede fallar o falta | "Tu última ubicación es de hace 40 min" |
| `ERROR` | algo falló o impide la acción | "El Bluetooth está apagado" |
| `SIMULATION` | herramienta de demo; debe verse distinta a todo lo demás | "Filtro de depuración · SIMULACIÓN" |

### 5.2 Componentes que necesitan diseño (con sus variantes)

| Componente | Variantes |
|---|---|
| Barra superior de pantalla | con y sin acción de volver; con chip de estado de la malla |
| Barra de navegación inferior | 3 destinos: Enviar alerta · Cercanas · Estado |
| Tarjeta de sección (contenedor con título) | normal y apagada (alerta resuelta) |
| Fila etiqueta-valor | normal y con tono |
| Chip de estado | 5 tonos |
| Banner de mensaje | 5 tonos; con y sin acción |
| Botón principal y botón secundario | normal, deshabilitado y cargando |
| **Botón de mantener presionado** | reposo, deshabilitado, llenándose (0-100 %), completado |
| Opción de tipo de emergencia | 3 tipos × (sin elegir, elegida) |
| Selector de personas | − / número / +; en 1 el − está deshabilitado; en 255 se ve "255 o más" |
| Opción de vulnerable | activada y desactivada |
| Tarjeta de alerta cercana | ver C1 |
| **Flecha de rumbo** | con brújula (gira), sin brújula (solo el rumbo cardinal) y sin ubicación (oculta) |
| Línea de progreso de "Mi alerta" | pasos hecho, en curso, pendiente y con advertencia |
| Estado vacío | título, texto y acción opcional |
| Diálogo de confirmación | título, texto y dos botones |
| Etiqueta "SIMULACIÓN" | — |
| Interruptor con texto de ayuda | encendido y apagado |
| Campo de texto con error | normal y con error |
| Etiqueta eliminable (filtro) | `e5f6 ✕` |

## 6. Mapa de navegación

```
Abrir la app
 └─ ¿Faltan permisos? ── sí ──> P1 Permisos ── todo listo ──┐
                         no ───────────────────────────────┤
                                                           v
   ┌──────────────────── Barra inferior ─────────────────────┐
   │ Enviar alerta         │ Cercanas            │ Estado      │
   │  E1 Formulario        │  C1 Lista           │  S1 Estado  │
   │   └─> E2 Mi alerta    │   └─> C2 Detalle    │   └─> P1    │
   │        └─> E1 (otra)  │                     │  (revisar)  │
   └─────────────────────────────────────────────────────────┘
 E3 Cuenta regresiva: capa a pantalla completa, encima de cualquier pestaña
 N  Notificaciones del sistema: abren E2, E3 o C2
```

## 7. Pantallas

Cada pantalla trae su objetivo, el orden de importancia del contenido, los estados que hay que dibujar (con un nombre de frame sugerido) y sus textos.

### P1 · Permisos (spec 100)

**Objetivo:** que la persona conceda lo necesario entendiendo para qué sirve cada cosa. Es la primera pantalla que ve.

**Jerarquía:**
1. Título y una frase de por qué.
2. Lista de requisitos: nombre, para qué sirve, estado y botón.
3. Botón "Continuar", fijo abajo.

**Requisitos, en orden:**

| Requisito | Texto | Botón | ¿Bloquea? |
|---|---|---|---|
| Bluetooth cercano | Para enviar y recibir alertas de teléfonos cercanos sin internet. | Conceder | sí |
| Ubicación | Para compartir dónde estás cuando pides ayuda y calcular la distancia a otras alertas. | Conceder | sí |
| Notificaciones | Para avisarte de alertas cercanas y mantener la app activa. | Conceder | sí |
| Bluetooth encendido | El Bluetooth está apagado. | Encender | sí |
| Ubicación del sistema | La ubicación del teléfono está desactivada. | Activar | sí |
| Batería (recomendado) | Quita la restricción de batería para que el sistema no detenga la app. | Permitir | no |
| Samsung | En Ajustes > Batería > Límites de uso en segundo plano, quita secure-link de "Aplicaciones en suspensión". | Abrir ajustes · Ya lo hice | no |

Estado de cada fila: **Listo** (éxito) · **Falta** (advertencia) · **Lo negaste antes: actívalo en Ajustes.** (error, con el botón "Abrir ajustes").

| Frame | Estado |
|---|---|
| `P1/01` | Todo falta. "Continuar" deshabilitado y debajo: "Faltan permisos obligatorios." |
| `P1/02` | Mitad listos y mitad faltan |
| `P1/03` | Un permiso negado de forma permanente |
| `P1/04` | Permisos dados, pero el Bluetooth y la ubicación del sistema están apagados |
| `P1/05` | Todo listo, "Continuar" habilitado; el paso de Samsung sigue visible |
| `P1/06` | Volvió aquí porque se perdió un permiso: banner de error "Se perdió un permiso necesario. Concédelo de nuevo para seguir." |

Textos fijos: título "Antes de empezar" · intro "secure-link necesita estos permisos para enviar y recibir alertas sin internet." · botón "Continuar".

### E1 · Enviar alerta: formulario (spec 108)

**Objetivo:** pedir ayuda en menos de 10 segundos.

**Jerarquía:**
1. **Tipo de emergencia**: tres opciones grandes, una por fila, con color y texto.
2. Número de personas.
3. Vulnerables (opcional; puede verse más liviano).
4. Estado de la ubicación.
5. **Botón de mantener presionado**: fijo abajo y siempre visible, incluso al desplazar.

**Contenido:**

| Dato | Ejemplo |
|---|---|
| Tipo elegido | Atrapado o situación grave |
| Personas | 3 personas |
| Vulnerables | Niños · Personas heridas |
| Ubicación | Ubicación lista · ±15 m · `-33.50000, -70.65000` |
| Chip de la malla (barra superior) | Activa |

**Cómo se envía:** se mantiene presionado el botón 1,5 s. El botón se llena mientras tanto y, al completarse, el teléfono vibra. Si se suelta antes, no se envía.

| Frame | Estado | Textos clave |
|---|---|---|
| `E1/01` | Inicial: nada elegido, buscando ubicación | "Buscando tu ubicación…" · bajo el botón: "Elige qué necesitas para poder enviar." |
| `E1/02` | Completo y listo | "Ubicación lista · ±15 m" · botón: "Mantén presionado para enviar" |
| `E1/03` | Manteniendo presionado, 60 % | botón llenándose |
| `E1/04` | Se soltó antes de tiempo | "Mantén presionado hasta que se llene." |
| `E1/05` | Con la última ubicación conocida | "Se usará tu última ubicación (±45 m, hace 3 min)." (info) |
| `E1/06` | Última ubicación antigua | "Tu última ubicación es de hace 40 min. Si te moviste, puede no ser exacta." (advertencia) |
| `E1/07` | Ubicación desactivada | "La ubicación del teléfono está desactivada. Sin ubicación no se puede enviar la alerta." + "Activar" (error) · botón deshabilitado |
| `E1/08` | Sin ninguna ubicación todavía | "Todavía no hay ninguna ubicación. Sal a un lugar abierto si puedes; la alerta se podrá enviar en cuanto aparezca." · bajo el botón: "Falta tu ubicación para poder enviar." |
| `E1/09` | Guardando | botón: "Guardando…" |
| `E1/10` | No se pudo guardar | banner de error: "No se pudo guardar la alerta en el teléfono. Inténtalo de nuevo." + "Reintentar" |
| `E1/11` | 255 o más personas | el − habilitado, el + deshabilitado, "255 o más personas" |

Textos fijos: título "Pedir ayuda" · "¿Qué necesitas?" · "¿Cuántas personas necesitan ayuda?" · "¿Hay personas vulnerables? (opcional)" · "Tu ubicación". Aviso opcional en el encabezado, si la detección de movimiento está activa: "Si detectamos un movimiento brusco, te preguntaremos antes de enviar una alerta."

Errores raros (basta con el banner de error genérico): "Este teléfono alcanzó el máximo de alertas. Reinstala la app para seguir." · "Hay un dato inválido en la alerta. Revisa el formulario."

### E2 · Mi alerta (spec 108)

**Objetivo:** que la persona sepa, con honestidad, qué pasó con su alerta y si viene alguien.

**Jerarquía:**
1. Encabezado: tipo (color y texto), personas y hora.
2. **"Qué pasó con tu alerta"**: progreso con 4 pasos.
3. **Ayuda**: cuántas personas van en camino, el dato más esperado.
4. Detalles: vulnerables, ubicación usada y clave (`a1b2c3d4e5f6-0001`, pequeña).
5. Acciones: "Enviar otra alerta". Opcional: "Estoy a salvo" y "Cancelar alerta".

**Pasos del progreso:**

| Paso | En curso o hecho | Si no ocurre |
|---|---|---|
| 1 · Guardada | "Guardada en tu teléfono" (siempre hecho) | — |
| 2 · Anuncio | "Anunciándose a teléfonos cercanos · 0:42" (en curso) | antes de empezar: "Preparando el anuncio…" |
| 3 · Retransmisión | "Retransmitida por 2 teléfonos cercanos" (hecho) | si el anuncio terminó sin eco: "El anuncio terminó y ningún teléfono cercano la retransmitió todavía." (advertencia) |
| 4 · Servidor | "Este teléfono la subió al servidor a las 07:36" (hecho) | "Se subirá al servidor cuando este teléfono u otro tengan internet." (pendiente) |

Ayuda: "Aún nadie avisó que va en camino." · "2 personas van en camino" (éxito).

| Frame | Estado |
|---|---|
| `E2/01` | Recién guardada: preparando el anuncio |
| `E2/02` | Anunciándose (0:42), sin eco, nadie en camino |
| `E2/03` | Retransmitida por 2 teléfonos, nadie en camino |
| `E2/04` | Retransmitida, subida y 2 personas en camino: el mejor caso |
| `E2/05` | El anuncio terminó sin eco (advertencia) |
| `E2/06` | Bluetooth apagado: "Tu alerta está guardada, pero no se está anunciando: el Bluetooth está apagado." + "Encender" (error) |
| `E2/07` | El teléfono no puede anunciar: "Tu alerta está guardada, pero este teléfono no puede anunciarla. Pide a alguien cercano que abra la app." (error) |
| `E2/08` | Malla detenida: "La malla está detenida. Actívala en Estado para anunciar tu alerta." + "Ir a Estado" (advertencia) |
| `E2/09` | Opcional: confirmación de "Estoy a salvo": "¿Confirmas que ya estás a salvo? Los voluntarios dejarán de ver tu alerta como pendiente. No se puede deshacer." |
| `E2/10` | Opcional: confirmación de "Cancelar alerta": "¿Cancelar la alerta? Úsalo si la enviaste por error. No se puede deshacer." |
| `E2/11` | Opcional: resuelta: "Marcaste que estás a salvo" o "Cancelaste esta alerta" |
| `E2/12` | Alerta automática: con la etiqueta "Automática" |

Título: "Tu alerta".

### E3 · Cuenta regresiva de la alerta automática (spec 108)

**Objetivo:** dar 15 segundos para cancelar una alerta disparada por un golpe o una caída. Puede aparecer sobre cualquier pestaña.

**Jerarquía:**
1. **El número**, enorme.
2. Qué pasó y qué va a pasar.
3. **Botón "Cancelar, estoy bien"**: ancho y grande, abajo, fácil de tocar aun temblando.

| Frame | Estado | Textos |
|---|---|---|
| `E3/01` | Contando (12 s) | "Detectamos un movimiento brusco" · "Si no cancelas, enviaremos una alerta de emergencia en 12 s." · "Se enviará: Atrapado o grave · 1 persona · tu ubicación" · "Cancelar, estoy bien" |
| `E3/02` | Terminó, pero no hay ubicación | "Esperando tu ubicación para enviar la alerta…" · "Cancelar" |
| `E3/03` | Enviada | "Alerta enviada" → pasa a E2 |
| `E3/04` | Cancelada | aviso breve: "Alerta cancelada. No se envió nada." |
| `E3/05` | No se pudo guardar | "No se pudo guardar la alerta automática. Envíala a mano desde el formulario." · "Ir al formulario" (error) |

Debe sentirse urgente sin alarmar de más: es el único lugar donde la advertencia domina toda la pantalla. El teléfono vibra en cada segundo.

### C1 · Alertas cercanas: lista (spec 109)

**Objetivo:** que un voluntario elija a qué alerta acudir.

**Jerarquía:**
1. Banners activos (solo los que apliquen), arriba.
2. Resumen: "5 alertas cerca".
3. **Tarjetas**, ordenadas de la más cercana a la más lejana. Cada tarjeta tiene tres zonas:
   - **Izquierda:** tipo (color y texto) y personas.
   - **Centro:** vulnerables, tiempo transcurrido, voluntarios en camino y etiquetas ("Automática", "Vas en camino").
   - **Derecha:** **flecha y distancia**, lo que más se mira.

**Datos de cada tarjeta:**

| Dato | Ejemplo | Si falta |
|---|---|---|
| Tipo | Atrapado o grave | — |
| Personas | 3 personas | — |
| Vulnerables | Niños · Heridos | se oculta |
| Tiempo | hace 30 min | — |
| Distancia | 120 m | `—` |
| Flecha y rumbo | ↗ NE | flecha oculta, `—` |
| Voluntarios | 2 en camino | "Nadie en camino" (información útil: ahí hace falta alguien) |
| Etiquetas | Automática · Vas en camino · (opcional) A salvo | se ocultan |

**Banners posibles:**

| Banner | Tono | Texto |
|---|---|---|
| Bluetooth apagado | error | El Bluetooth está apagado: no llegarán alertas nuevas. · Encender |
| No puede escuchar | error | Este teléfono no puede escuchar alertas ahora. Reinicia la app. |
| Malla detenida | advertencia | La malla está detenida. · Activar |
| Sin ubicación | advertencia | Sin tu ubicación no podemos calcular distancias. |
| Ubicación antigua | info | Las distancias usan tu ubicación de hace 12 min. |
| Calibrar brújula | advertencia | La brújula necesita calibrarse: mueve el teléfono dibujando un 8. |
| Sin brújula | info | Este teléfono no tiene brújula: guíate por el rumbo (N, NE, E…). |
| Filtro de depuración | simulación | Filtro de depuración activo: se ignoran 2 teléfonos. · Ir a Estado |

| Frame | Estado |
|---|---|
| `C1/01` | 5 alertas variadas (ver la sección 8) |
| `C1/02` | Vacía, escuchando: "No hay alertas cercanas" · "La app sigue escuchando. Te avisaremos cuando llegue una." |
| `C1/03` | Vacía, con la malla sin funcionar: "No estás recibiendo alertas: revisa el estado de la malla." · "Ir a Estado" |
| `C1/04` | Sin ubicación propia: todas las distancias en `—` y sin flechas |
| `C1/05` | Brújula por calibrar |
| `C1/06` | Bluetooth apagado + lista con alertas viejas |
| `C1/07` | Filtro de depuración activo (banner de simulación) |
| `C1/08` | Opcional: alertas resueltas al final, en tono apagado |

Título: "Alertas cercanas".

### C2 · Detalle de una alerta (spec 109)

**Objetivo:** llegar hasta la persona sin internet y avisar que se va en camino.

**Jerarquía:**
1. Encabezado: tipo, personas y etiquetas.
2. **Bloque de navegación**, el más grande de la pantalla: flecha que gira con el teléfono, **distancia en grande** y "Hacia el NE (45°)".
3. Ayuda: voluntarios en camino.
4. Detalles: hora (`07:33`), vulnerables, coordenadas, "Llegó por 2 saltos" y la clave (pequeña).
5. **Acciones fijas abajo**: "Voy en camino" (principal) y "Abrir en Google Maps" (secundaria).

Nota fija bajo la navegación: "La ubicación de la alerta es la que envió su teléfono al pedir ayuda." También se muestra la precisión propia: "Tu ubicación: ±15 m".

| Frame | Estado | Textos clave |
|---|---|---|
| `C2/01` | Normal, con brújula | flecha, "120 m", "Hacia el NE (45°)" |
| `C2/02` | Sin brújula usable | sin flecha giratoria; "Gira hasta mirar al NE." |
| `C2/03` | Sin ubicación propia | distancia `—`; se mantienen las coordenadas y el botón de Maps |
| `C2/04` | Confirmación de "Voy en camino" | "¿Confirmas que vas en camino? Los demás voluntarios y la persona que pidió ayuda lo verán. No se puede deshacer." · Confirmar · Volver |
| `C2/05` | Ya vas en camino | botón deshabilitado en éxito: "Vas en camino" |
| `C2/06` | Sin internet | bajo Maps: "Google Maps necesita datos para calcular la ruta. Sin internet, guíate con la flecha." |
| `C2/07` | Error al guardar el aviso | "No se pudo guardar tu aviso. Inténtalo de nuevo." · Reintentar |
| `C2/08` | No hay app de mapas | aviso breve: "No hay una app de mapas instalada." |
| `C2/09` | Opcional: resuelta | "Esta persona marcó que está a salvo." |

Título: "Detalle de la alerta".

### S1 · Estado (spec 109)

**Objetivo:** saber si todo funciona y preparar la demo. Es una pantalla técnica, pero debe leerse sin conocimientos técnicos.

**Secciones, en orden:**

| Sección | Contenido |
|---|---|
| **Malla Bluetooth** | chip (Activa / Iniciando… / Detenida / Bluetooth apagado) · interruptor "Malla activa" · Escuchando: Sí · Anunciando: Sí · "3 anuncios activos" · "Teléfonos cerca: 4" · "Último paquete: hace 8 s" · "12 alertas · 5 avisos" guardados |
| **Servidor** | chip (Con internet / Sin internet) · "4 pendientes de subir" · "Última subida: 07:36" · banner de error si hay · "Reintento en 20 s" |
| **Este teléfono** | "ID: a1b2 c3d4 e5f6" · "Etiqueta de relay: e5f6" (se usa en la demo para bloquear teléfonos) |
| **Ajustes** | interruptor "Alerta por movimiento brusco" + "Si se detecta un golpe o una caída fuerte, la app enviará una alerta tras 15 s si no la cancelas." · botón "Revisar permisos" |
| **Filtro de depuración · SIMULACIÓN** | "Ignora los anuncios emitidos por estos teléfonos. Sirve para simular distancia en la demo." · etiquetas bloqueadas (`e5f6 ✕`) · campo "Etiqueta (4 caracteres)" + "Bloquear" · vacío: "No hay teléfonos bloqueados." · botón "Simular movimiento brusco" (solo en versiones de depuración; dispara la cuenta regresiva E3 para la demo) · **medidor de movimiento** (solo en depuración): g actual, pico de 2 s, última caída libre, estado del detector y caídas detectadas |

Esta sección debe verse claramente distinta: no es una función real de la app.

| Frame | Estado |
|---|---|
| `S1/01` | Todo bien: malla activa, con internet, sin pendientes |
| `S1/02` | Sin internet, 4 pendientes: "No se pudo conectar con el servidor. Reintento en 20 s." (advertencia) |
| `S1/03` | Error del servidor, una variante por caso: "Falta configurar el servidor en esta compilación. Las alertas se guardan, pero no se suben." · "El servidor rechazó la clave de la app. Las alertas se guardan, pero no se suben." · "El servidor rechazó un paquete inválido. Se descartó y se sigue con los demás." |
| `S1/04` | Bluetooth apagado (chip y banner) |
| `S1/05` | No puede anunciar: "Este teléfono no puede anunciar por Bluetooth: solo recibirá alertas." · variantes: "No se pudo anunciar por Bluetooth. Reinicia la app." · "No se pudo escuchar por Bluetooth. Reinicia la app." |
| `S1/06` | Apagando la malla: "Si detienes la malla, no recibirás ni retransmitirás alertas." · Detener · Volver |
| `S1/07` | Filtro con 2 etiquetas bloqueadas |
| `S1/08` | Errores del campo del filtro, una variante por caso: "Escribe 4 caracteres entre 0-9 y a-f." · "Esa etiqueta ya está bloqueada." · "Esa es la etiqueta de este teléfono." |
| `S1/09` | Medidor de movimiento activo (depuración): "Ahora: 1,02 g" · "Pico (2 s): 2,87 g" · "Última caída libre: 320 ms" · "Detector: esperando impacto" · "Caídas detectadas: 1" |

Título: "Estado".

### N · Notificaciones del sistema (specs 108 y 109)

Se diseñan como notificaciones estándar de Android: el ícono de la app, más el título, el texto y las acciones.

| Frame | Título | Texto | Acciones |
|---|---|---|---|
| `N/01` | Nueva alerta: Atrapado o grave | 3 personas · a 120 m · hace 1 min | Abrir |
| `N/02` | Movimiento brusco detectado | Se enviará una alerta en 12 s. | Cancelar · Abrir |
| `N/03` | Alerta enviada | Tu alerta de emergencia se está anunciando. | Abrir |
| `N/04` | Ayuda en camino | 1 persona va en camino a tu alerta. | Abrir |
| `N/05` | secure-link activo (fija, discreta) | Escuchando alertas cercanas · 4 teléfonos cerca | Abrir |
| `N/06` | secure-link en pausa | Bluetooth apagado: no se reciben alertas. | Abrir |

Si no se conoce la distancia, el texto de `N/01` dice "distancia desconocida".

## 8. Datos de ejemplo (usar los mismos en todos los frames)

**Este teléfono:** ID `a1b2 c3d4 e5f6` · etiqueta `e5f6` · ubicación `-33.50000, -70.65000` · ±15 m.

**Mi alerta:** Atrapado o situación grave · 3 personas · Niños, Personas heridas · enviada a las 07:33 · clave `a1b2c3d4e5f6-0001`.

**Alertas cercanas (C1/01)**, en el orden en que aparecen:

| # | Tipo | Personas | Extras | Tiempo | Distancia y rumbo | Voluntarios | Saltos |
|---|---|---|---|---|---|---|---|
| 1 | Atrapado o grave | 3 | Niños · Heridos | hace 30 min | 120 m · NE | 2 en camino | 2 |
| 2 | Atrapado o grave | 1 | Automática | hace 5 min | 340 m · S | Nadie en camino | 1 |
| 3 | Asistencia médica | 2 | Adultos mayores · Vas en camino | hace 45 min | 850 m · O | 1 en camino | 3 |
| 4 | Comida o agua | 50 | — | hace 2 h | 1,2 km · SE | Nadie en camino | 4 |
| 5 | Comida o agua | 255 o más | Movilidad reducida | hace 3 h | 2,4 km · NO | Nadie en camino | 5 |

La lista va por distancia, no por gravedad: así lo decide la spec 109, porque el voluntario necesita saber qué tiene más cerca. Que los colores queden mezclados en el ejemplo es intencional.

## 9. Accesibilidad

- Contraste mínimo 4,5:1 para el texto y 3:1 para los íconos y bordes, en los dos temas.
- Áreas táctiles de 48 dp como mínimo. El botón de enviar y el de cancelar la cuenta regresiva, mucho mayores.
- Tamaño de letra del sistema al 130 % o más sin cortar textos. Probar al menos E1, C1 y E3.
- Lectores de pantalla (TalkBack): la flecha se describe con palabras ("Alerta hacia el noreste, a 120 metros"), y el botón de mantener presionado explica cómo usarlo.
- Las vibraciones acompañan, nunca reemplazan, lo que se ve en pantalla.

## 10. Entregables

1. **Componentes** de la sección 5.2, con todas sus variantes, en tema oscuro y claro.
2. **Frames** de la sección 7 en tema oscuro. En tema claro, como mínimo: `P1/05`, `E1/02`, `E2/04`, `E3/01`, `C1/01`, `C2/01` y `S1/01`.
3. **Prototipo navegable** con tres recorridos:
   - Pedir ayuda: `E1/01` → `E1/02` → `E1/03` → `E2/01` → `E2/04`.
   - Acudir: `N/01` → `C2/01` → `C2/04` → `C2/05`.
   - Alerta automática cancelada: `E3/01` → `E3/04`.
4. **Capas con el `id` del texto** (por ejemplo `sendalert_hold_to_send`) en los textos, para pasarlos directo a `strings.xml`. Los identificadores están en el anexo A.
5. **Lista de cambios de texto**, si el diseño mejora la redacción, para actualizar las specs.

## 11. Fuera de alcance

- Panel web de las autoridades y simulador.
- Logo y nombre definitivo (`secure-link` es provisional).
- Onboarding más allá de la pantalla de permisos.
- Mapas dentro de la app: la navegación es con la flecha. Google Maps se abre aparte.
- iOS y otras marcas.

## 12. Preguntas abiertas para el diseño

1. Íconos para los tres tipos de emergencia y los cuatro vulnerables: el diseño los propone.
2. Cómo representar el paso de "Mi alerta" que no ocurrió (el anuncio terminó sin eco) sin asustar, pero sin esconderlo.
3. Las funciones opcionales ("Estoy a salvo" y "Cancelar alerta") se diseñan igual, marcadas como opcionales en Figma, por si entran al MVP.

## Anexo A. Catálogo de textos

Copia exacta de las tablas de mensajes de las specs 100, 108 y 109: **153 identificadores**. Se generó con un script a partir de las specs, no a mano. Si una spec cambia, se vuelve a generar.

Cómo leer las columnas:
- **id:** nombre de la capa de texto en Figma y clave en `strings.xml`.
- **Cuándo:** la condición interna que muestra el texto. Los nombres en mayúsculas (`BLUETOOTH_OFF`, `FIX`…) son estados de la app; lo importante es la condición que describen.
- **Dónde:** el componente o la zona de la pantalla.
- **Tono:** `INFO`, `SUCCESS`, `WARNING`, `ERROR` o `SIMULATION` (sección 5.1). `—` es texto neutro.
- **Texto:** provisional. Los números (`12`, `0:42`, `120 m`, `2`) son valores de ejemplo que la app reemplaza.
- **Acción:** el botón o el gesto asociado. `—` significa que no hay ninguno.
- **opcional:** pertenece a "Estoy a salvo" o "Cancelar alerta", que se diseñan igual pero pueden no entrar al MVP.

Dos casos de forma:
- En las filas de permisos (`permissions_bluetooth`, `permissions_location`, …), lo que va antes de los dos puntos es el **nombre** de la fila y lo que sigue es su **explicación**. En el código es un solo texto; en Figma pueden ser dos capas con el mismo `id` y los sufijos `_title` y `_body`.
- `status_debug_add` reúne el texto de ayuda del campo ("Etiqueta (4 caracteres)") y el del botón ("Bloquear"). En Figma se nombran `status_debug_add_hint` y `status_debug_add_button`.

Si en Figma separas un texto en dos capas, o necesitas uno nuevo, anótalo en la lista de cambios de texto (entregable 5) para agregarlo a la spec.

### P1 · Permisos (spec 100)

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `permissions_title` | siempre | barra superior | — | Antes de empezar | — |
| `permissions_intro` | siempre | encabezado | INFO | secure-link necesita estos permisos para enviar y recibir alertas sin internet. | — |
| `permissions_bluetooth` | fila `BLUETOOTH` | fila | — | Bluetooth cercano: para enviar y recibir alertas de teléfonos cercanos sin internet. | Conceder |
| `permissions_location` | fila `LOCATION` | fila | — | Ubicación: para compartir dónde estás cuando pides ayuda y calcular la distancia a otras alertas. | Conceder |
| `permissions_notifications` | fila `NOTIFICATIONS` | fila | — | Notificaciones: para avisarte de alertas cercanas y mantener la app activa. | Conceder |
| `permissions_bluetooth_on` | fila `BLUETOOTH_ON` sin cumplir | fila | WARNING | El Bluetooth está apagado. | Encender |
| `permissions_location_on` | fila `LOCATION_ON` sin cumplir | fila | WARNING | La ubicación del teléfono está desactivada. | Activar |
| `permissions_battery` | fila `BATTERY_UNRESTRICTED` | fila | INFO | Recomendado: quita la restricción de batería para que el sistema no detenga la app. | Permitir |
| `permissions_samsung` | mientras `samsungAcknowledged = false` | fila | INFO | Samsung: en Ajustes > Batería > Límites de uso en segundo plano, quita secure-link de "Aplicaciones en suspensión". | Abrir ajustes · Ya lo hice |
| `permissions_state_granted` | `row.state = GRANTED` | chip de la fila | SUCCESS | Listo | — |
| `permissions_state_missing` | `row.state = MISSING` | chip de la fila | WARNING | Falta | — |
| `permissions_state_denied` | `row.state = DENIED_PERMANENTLY` | chip de la fila | ERROR | Lo negaste antes: actívalo en Ajustes. | Abrir ajustes |
| `permissions_continue` | siempre | botón principal | — | Continuar | entra a la navegación; deshabilitado si `canContinue = false` |
| `permissions_missing_hint` | `canContinue = false` | bajo el botón | WARNING | Faltan permisos obligatorios. | — |
| `common_lost_permission` | una operación falló por falta de permiso (sección 5.2) y la app volvió a esta pantalla | banner superior | ERROR | Se perdió un permiso necesario. Concédelo de nuevo para seguir. | — |

### E1 · Formulario (spec 108)

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `sendalert_title` | siempre | barra superior | — | Pedir ayuda | — |
| `sendalert_type_header` | siempre | tarjeta de tipo | — | ¿Qué necesitas? | — |
| `sendalert_type_food_water` | opción 0 | tarjeta de tipo | — | Comida o agua | seleccionar |
| `sendalert_type_medical` | opción 1 | tarjeta de tipo | — | Asistencia médica | seleccionar |
| `sendalert_type_trapped` | opción 2 | tarjeta de tipo | — | Atrapado o situación grave | seleccionar |
| `sendalert_people_header` | siempre | tarjeta de personas | — | ¿Cuántas personas necesitan ayuda? | − / + |
| `sendalert_people_max` | `people = 255` | tarjeta de personas | INFO | 255 o más personas | — |
| `sendalert_vulnerable_header` | siempre | tarjeta de vulnerables | — | ¿Hay personas vulnerables? (opcional) | — |
| `sendalert_vulnerable_children` | siempre | opciones | — | Niños | activar / desactivar |
| `sendalert_vulnerable_elderly` | siempre | opciones | — | Adultos mayores | activar / desactivar |
| `sendalert_vulnerable_injured` | siempre | opciones | — | Personas heridas | activar / desactivar |
| `sendalert_vulnerable_mobility` | siempre | opciones | — | Movilidad reducida | activar / desactivar |
| `sendalert_location_header` | siempre | tarjeta de ubicación | — | Tu ubicación | — |
| `sendalert_location_searching` | `SEARCHING` | tarjeta de ubicación | INFO | Buscando tu ubicación… | — |
| `sendalert_location_fix` | `FIX` | tarjeta de ubicación | SUCCESS | Ubicación lista · ±15 m | — |
| `sendalert_location_last_known` | `LAST_KNOWN` y no `isStale` | tarjeta de ubicación | INFO | Se usará tu última ubicación (±45 m, hace 3 min). | — |
| `sendalert_location_stale` | `LAST_KNOWN` e `isStale` | tarjeta de ubicación | WARNING | Tu última ubicación es de hace 40 min. Si te moviste, puede no ser exacta. | — |
| `sendalert_location_disabled` | `DISABLED` | tarjeta de ubicación | ERROR | La ubicación del teléfono está desactivada. Sin ubicación no se puede enviar la alerta. | Activar |
| `sendalert_location_no_permission` | `NO_PERMISSION` | — | — | (vuelve a la pantalla de permisos, 100) | — |
| `sendalert_location_unavailable` | `UNAVAILABLE`: sin ubicación actual ni anterior | tarjeta de ubicación | ERROR | Todavía no hay ninguna ubicación. Sal a un lugar abierto si puedes; la alerta se podrá enviar en cuanto aparezca. | — |
| `sendalert_hold_to_send` | `canSend` | botón | — | Mantén presionado para enviar | mantener `SEND_HOLD_MS` |
| `sendalert_hold_release_early` | se soltó antes de tiempo | bajo el botón | INFO | Mantén presionado hasta que se llene. | — |
| `sendalert_cannot_send_type` | `selectedType = null` | bajo el botón | INFO | Elige qué necesitas para poder enviar. | — |
| `sendalert_cannot_send_location` | sin `FIX` ni `LAST_KNOWN` | bajo el botón | WARNING | Falta tu ubicación para poder enviar. | — |
| `sendalert_sending` | guardando | botón | INFO | Guardando… | — |
| `sendalert_error_storage` | la 102 falló al guardar | banner | ERROR | No se pudo guardar la alerta en el teléfono. Inténtalo de nuevo. | Reintentar |
| `sendalert_error_counter` | contador agotado (101) | banner | ERROR | Este teléfono alcanzó el máximo de alertas. Reinstala la app para seguir. | — |
| `sendalert_error_invalid` | la 101 rechazó un campo | banner | ERROR | Hay un dato inválido en la alerta. Revisa el formulario. | — |
| `sendalert_motion_on` | detección de movimiento activa (ajuste en 109) | encabezado | INFO | Si detectamos un movimiento brusco, te preguntaremos antes de enviar una alerta. | — |

### E2 · Mi alerta (spec 108)

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `sendalert_mine_title` | siempre | barra superior | — | Tu alerta | — |
| `sendalert_mine_saved` | siempre | progreso | SUCCESS | Guardada en tu teléfono | — |
| `sendalert_mine_waiting` | sin `announceUntilMs` | progreso | INFO | Preparando el anuncio… | — |
| `sendalert_mine_announcing` | `now < announceUntilMs` | progreso | INFO | Anunciándose a teléfonos cercanos · 0:42 | — |
| `sendalert_mine_announce_ended` | `now ≥ announceUntilMs` y `echoCount = 0` | progreso | WARNING | El anuncio terminó y ningún teléfono cercano la retransmitió todavía. | — |
| `sendalert_mine_echoed` | `echoCount > 0` | progreso | SUCCESS | Retransmitida por 2 teléfonos cercanos | — |
| `sendalert_mine_uploaded` | `uploadedAtMs != null` | progreso | SUCCESS | Este teléfono la subió al servidor a las 07:36 | — |
| `sendalert_mine_not_uploaded` | `uploadedAtMs = null` | progreso | INFO | Se subirá al servidor cuando este teléfono u otro tengan internet. | — |
| `sendalert_mine_volunteers_none` | `volunteers = 0` | tarjeta de ayuda | INFO | Aún nadie avisó que va en camino. | — |
| `sendalert_mine_volunteers` | `volunteers > 0` | tarjeta de ayuda | SUCCESS | 2 personas van en camino | — |
| `sendalert_mine_mesh_bt_off` | `meshState = BLUETOOTH_OFF` | banner | ERROR | Tu alerta está guardada, pero no se está anunciando: el Bluetooth está apagado. | Encender |
| `sendalert_mine_mesh_failed` | `meshState ∈ {ADVERTISE_UNSUPPORTED, ADVERTISE_FAILED}` | banner | ERROR | Tu alerta está guardada, pero este teléfono no puede anunciarla. Pide a alguien cercano que abra la app. | — |
| `sendalert_mine_mesh_stopped` | `meshState = STOPPED` | banner | WARNING | La malla está detenida. Actívala en Estado para anunciar tu alerta. | Ir a Estado |
| `sendalert_mine_another` | siempre | botón secundario | — | Enviar otra alerta | → Formulario |
| `sendalert_mine_safe` | opcional | botón principal | — | Estoy a salvo | `ConfirmDialog` |
| `sendalert_mine_cancel` | opcional | botón secundario | — | Cancelar alerta | `ConfirmDialog` |
| `sendalert_mine_confirm_safe` | al tocar "Estoy a salvo" | diálogo | WARNING | ¿Confirmas que ya estás a salvo? Los voluntarios dejarán de ver tu alerta como pendiente. No se puede deshacer. | Confirmar · Volver |
| `sendalert_mine_confirm_cancel` | al tocar "Cancelar alerta" | diálogo | WARNING | ¿Cancelar la alerta? Úsalo si la enviaste por error. No se puede deshacer. | Cancelar alerta · Volver |
| `sendalert_mine_resolved_safe` | `latestStatus` | encabezado | SUCCESS | Marcaste que estás a salvo | — |
| `sendalert_mine_resolved_cancelled` | `latestStatus` | encabezado | INFO | Cancelaste esta alerta | — |

### E3 · Cuenta regresiva (spec 108)

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `sendalert_auto_title` | `COUNTING` | capa | WARNING | Detectamos un movimiento brusco | — |
| `sendalert_auto_body` | `COUNTING` | capa | — | Si no cancelas, enviaremos una alerta de emergencia en 12 s. | — |
| `sendalert_auto_summary` | `COUNTING` | capa | INFO | Se enviará: Atrapado o grave · 1 persona · tu ubicación | — |
| `sendalert_auto_cancel` | `COUNTING` o `WAITING_LOCATION` | botón | — | Cancelar, estoy bien | cancela |
| `sendalert_auto_waiting_location` | terminó la cuenta y no hay ubicación | capa | WARNING | Esperando tu ubicación para enviar la alerta… | Cancelar |
| `sendalert_auto_sent` | `SENT` | capa, luego "Mi alerta" | SUCCESS | Alerta enviada | — |
| `sendalert_auto_cancelled` | `CANCELLED` | aviso breve | INFO | Alerta cancelada. No se envió nada. | — |
| `sendalert_auto_failed` | `FAILED`: no se pudo guardar la alerta automática | capa | ERROR | No se pudo guardar la alerta automática. Envíala a mano desde el formulario. | Ir al formulario |

### N · Notificaciones de la 108 (spec 108)

| id | Cuándo | Título | Texto | Acciones |
|---|---|---|---|---|
| `sendalert_notif_countdown` | cuenta regresiva con la app en segundo plano | Movimiento brusco detectado | Se enviará una alerta en 12 s. | Cancelar · Abrir |
| `sendalert_notif_sent` | se envió la alerta automática | Alerta enviada | Tu alerta de emergencia se está anunciando. | Abrir |
| `sendalert_notif_volunteer` | llega un `ON_THE_WAY` para una alerta propia | Ayuda en camino | 1 persona va en camino a tu alerta. | Abrir |

### C1 · Lista de cercanas (spec 109)

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `nearby_title` | siempre | barra superior | — | Alertas cercanas | — |
| `nearby_count` | hay alertas | resumen | — | 5 alertas cerca | — |
| `nearby_empty_title` | lista vacía | estado vacío | — | No hay alertas cercanas | — |
| `nearby_empty_body` | lista vacía y `meshState = RUNNING` | estado vacío | INFO | La app sigue escuchando. Te avisaremos cuando llegue una. | — |
| `nearby_empty_mesh_off` | lista vacía y malla sin funcionar | estado vacío | WARNING | No estás recibiendo alertas: revisa el estado de la malla. | Ir a Estado |
| `nearby_banner_bt_off` | `meshState = BLUETOOTH_OFF` | banner | ERROR | El Bluetooth está apagado: no llegarán alertas nuevas. | Encender |
| `nearby_banner_scan_failed` | `meshState = SCAN_FAILED` | banner | ERROR | Este teléfono no puede escuchar alertas ahora. Reinicia la app. | — |
| `nearby_banner_mesh_stopped` | `meshState = STOPPED` | banner | WARNING | La malla está detenida. | Activar |
| `nearby_banner_no_location` | ubicación sin `FIX` ni `LAST_KNOWN` | banner | WARNING | Sin tu ubicación no podemos calcular distancias. | — |
| `nearby_banner_location_stale` | ubicación antigua (108, `LOCATION_STALE_WARN_MS`) | banner | INFO | Las distancias usan tu ubicación de hace 12 min. | — |
| `nearby_banner_compass_calibrate` | `Compass` con precisión `UNRELIABLE` o `LOW` | banner | WARNING | La brújula necesita calibrarse: mueve el teléfono dibujando un 8. | — |
| `nearby_banner_compass_missing` | el teléfono no tiene brújula | banner | INFO | Este teléfono no tiene brújula: guíate por el rumbo (N, NE, E…). | — |
| `nearby_item_volunteers_none` | `volunteers = 0` | tarjeta | INFO | Nadie en camino | — |
| `nearby_item_volunteers` | `volunteers > 0` | tarjeta | SUCCESS | 2 en camino | — |
| `nearby_item_going` | `iAmGoing` | tarjeta | SUCCESS | Vas en camino | — |
| `nearby_item_automatic` | `automatic` | tarjeta | INFO | Automática | — |
| `nearby_item_resolved_safe` | opcional | tarjeta | INFO | A salvo | — |
| `nearby_item_resolved_cancelled` | opcional | tarjeta | INFO | Cancelada | — |

### C2 · Detalle (spec 109)

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `nearby_detail_title` | siempre | barra superior | — | Detalle de la alerta | Volver |
| `nearby_detail_distance` | distancia conocida | navegación | — | 120 m | — |
| `nearby_detail_direction` | rumbo conocido | navegación | — | Hacia el NE (45°) | — |
| `nearby_detail_no_compass_hint` | sin brújula usable | navegación | INFO | Gira hasta mirar al NE. | — |
| `nearby_detail_location_note` | siempre | navegación | INFO | La ubicación de la alerta es la que envió su teléfono al pedir ayuda. | — |
| `nearby_detail_hops` | siempre | detalles | INFO | Llegó por 2 saltos | — |
| `nearby_detail_going_button` | no `iAmGoing` | botón principal | — | Voy en camino | `ConfirmDialog` |
| `nearby_detail_going_confirm` | al tocar "Voy en camino" | diálogo | WARNING | ¿Confirmas que vas en camino? Los demás voluntarios y la persona que pidió ayuda lo verán. No se puede deshacer. | Confirmar · Volver |
| `nearby_detail_going_done` | `iAmGoing` | botón (deshabilitado) | SUCCESS | Vas en camino | — |
| `nearby_detail_going_error` | la 102 falló al guardar | banner | ERROR | No se pudo guardar tu aviso. Inténtalo de nuevo. | Reintentar |
| `nearby_detail_maps_button` | siempre | botón secundario | — | Abrir en Google Maps | abre Maps |
| `nearby_detail_maps_hint` | sin internet (`SyncStatus.online = false`) | bajo el botón | INFO | Google Maps necesita datos para calcular la ruta. Sin internet, guíate con la flecha. | — |
| `nearby_detail_maps_missing` | `MapsLauncher` devuelve `NO_APP` | aviso breve | ERROR | No hay una app de mapas instalada. | — |
| `nearby_detail_resolved` | opcional, alerta resuelta | banner | INFO | Esta persona marcó que está a salvo. | — |

### S1 · Estado (spec 109)

| id | Cuándo | Dónde | Tono | Texto | Acción |
|---|---|---|---|---|---|
| `status_title` | siempre | barra superior | — | Estado | — |
| `status_mesh_header` | siempre | malla | — | Malla Bluetooth | — |
| `status_mesh_running` | `RUNNING` | chip | SUCCESS | Activa | — |
| `status_mesh_starting` | `STARTING` | chip | INFO | Iniciando… | — |
| `status_mesh_stopped` | `STOPPED` | chip | WARNING | Detenida | — |
| `status_mesh_bt_off` | `BLUETOOTH_OFF` | chip y banner | ERROR | Bluetooth apagado | Encender |
| `status_mesh_adv_unsupported` | `ADVERTISE_UNSUPPORTED` | banner | ERROR | Este teléfono no puede anunciar por Bluetooth: solo recibirá alertas. | — |
| `status_mesh_adv_failed` | `ADVERTISE_FAILED` | banner | ERROR | No se pudo anunciar por Bluetooth. Reinicia la app. | — |
| `status_mesh_scan_failed` | `SCAN_FAILED` | banner | ERROR | No se pudo escuchar por Bluetooth. Reinicia la app. | — |
| `status_mesh_toggle` | siempre | interruptor | — | Malla activa | encender / apagar |
| `status_mesh_off_confirm` | al apagar | diálogo | WARNING | Si detienes la malla, no recibirás ni retransmitirás alertas. | Detener · Volver |
| `status_server_header` | siempre | servidor | — | Servidor | — |
| `status_online` | `online` | chip | SUCCESS | Con internet | — |
| `status_offline` | `online` | chip | WARNING | Sin internet | — |
| `status_sync_pending` | `pendingUploads > 0` | fila | INFO | 4 pendientes de subir | — |
| `status_sync_error_network` | `syncError = NETWORK` o `TIMEOUT` | banner | WARNING | No se pudo conectar con el servidor. Reintento en 20 s. | — |
| `status_sync_error_config` | `syncError = CONFIG_MISSING` | banner | ERROR | Falta configurar el servidor en esta compilación. Las alertas se guardan, pero no se suben. | — |
| `status_sync_error_auth` | `syncError = AUTH` (401, 403) | banner | ERROR | El servidor rechazó la clave de la app. Las alertas se guardan, pero no se suben. | — |
| `status_sync_error_rejected` | `syncError = REJECTED` (400, 409) | banner | ERROR | El servidor rechazó un paquete inválido. Se descartó y se sigue con los demás. | — |
| `status_device_header` | siempre | este teléfono | — | Este teléfono | — |
| `status_device_id` | siempre | fila | — | ID: a1b2 c3d4 e5f6 | — |
| `status_relay_tag` | siempre | fila | — | Etiqueta de relay: e5f6 | — |
| `status_settings_header` | siempre | ajustes | — | Ajustes | — |
| `status_motion_toggle` | siempre | interruptor | — | Alerta por movimiento brusco | encender / apagar |
| `status_motion_hint` | siempre | bajo el interruptor | INFO | Si se detecta un golpe o una caída fuerte, la app enviará una alerta tras 15 s si no la cancelas. | — |
| `status_permissions_button` | siempre | botón | — | Revisar permisos | → pantalla de permisos (100) |
| `status_debug_header` | siempre | filtro | SIMULATION | Filtro de depuración · SIMULACIÓN | — |
| `status_debug_explain` | siempre | filtro | SIMULATION | Ignora los anuncios emitidos por estos teléfonos. Sirve para simular distancia en la demo. | — |
| `status_debug_empty` | lista vacía | filtro | — | No hay teléfonos bloqueados. | — |
| `status_debug_add` | siempre | campo + botón | — | Etiqueta (4 caracteres) · Bloquear | agrega |
| `status_debug_remove` | por etiqueta | chip | — | e5f6 ✕ | quita |
| `status_debug_error_format` | el texto no son 4 hexadecimales | bajo el campo | ERROR | Escribe 4 caracteres entre 0-9 y a-f. | — |
| `status_debug_error_duplicate` | la etiqueta ya está | bajo el campo | ERROR | Esa etiqueta ya está bloqueada. | — |
| `status_debug_error_self` | es mi propio `relayTag` | bajo el campo | ERROR | Esa es la etiqueta de este teléfono. | — |
| `status_debug_simulate_shake` | solo en versiones de depuración | filtro | SIMULATION | Simular movimiento brusco | emite un evento de caída por `MotionSource` (105) |
| `status_debug_meter_title` | solo en depuración | filtro | SIMULATION | Medidor de movimiento | — |
| `status_debug_meter_current` | solo en depuración | medidor | — | Ahora: 1,02 g | — |
| `status_debug_meter_peak` | solo en depuración | medidor | — | Pico (2 s): 2,87 g | — |
| `status_debug_meter_freefall` | solo en depuración; hubo caída libre | medidor | — | Última caída libre: 320 ms | — |
| `status_debug_meter_state` | solo en depuración | medidor | INFO | Detector: en reposo · en caída · esperando impacto · en pausa | — |
| `status_debug_meter_events` | solo en depuración | medidor | — | Caídas detectadas: 1 | — |
| `status_debug_meter_hint` | solo en depuración | medidor | INFO | Umbrales: caída libre < 0,4 g durante 300 ms e impacto > 2,5 g antes de 1 s. | — |
| `status_debug_active_banner` | lista no vacía | banner en Cercanas y Estado | SIMULATION | Filtro de depuración activo: se ignoran 2 teléfonos. | Ir a Estado |

### N · Notificaciones de la 109 (spec 109)

| id | Cuándo | Título | Texto | Acciones |
|---|---|---|---|---|
| `nearby_notif_new_alert` | `RECEIVED` de un `ALERT` ajeno y no resuelto | Nueva alerta: Atrapado o grave | 3 personas · a 120 m · hace 1 min (sin ubicación: "distancia desconocida") | Abrir (→ detalle) |
| `status_notif_service` | el servicio en primer plano funciona | secure-link activo | Escuchando alertas cercanas · 4 teléfonos cerca | Abrir |
| `status_notif_service_bt_off` | servicio con `BLUETOOTH_OFF` | secure-link en pausa | Bluetooth apagado: no se reciben alertas. | Abrir |
