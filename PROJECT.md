# PROPUESTA INICIAL DEL PROYECTO
## OPTIMIZACIÓN Y PRIORIZACIÓN DE RECURSOS Y ESFUERZOS EN LA POST-EMERGENCIA

Tras un evento catastrófico (como un terremoto, alud o inundación severa), las comunidades afectadas sufren cortes de comunicación, bloqueo de rutas y una demanda masiva y simultánea de asistencia que desborda por completo la capacidad operativa inicial de los municipios y equipos de rescate. En ese escenario, con recursos limitados e información fragmentada, definir a quién atender primero, dónde concentrar los esfuerzos y qué recursos asignar se convierte en una decisión crítica, que se toma con rapidez, bajo presión y muchas veces con datos incompletos.

El desafío consiste en proponer una forma de ayudar a las autoridades y organismos que coordinan la respuesta ante un desastre a decidir con rapidez, justificando la viabilidad técnica de la propuesta y su utilidad.

## Propuesta

A través de Bluetooth Low Energy, las alertas se propagan entre dispositivos móviles formando una malla P2P: cada teléfono que recibe una alerta la guarda, la muestra a su usuario y la vuelve a emitir con una marca de salto. Cuando algún dispositivo recupera internet, sube las alertas al servidor central, donde las autoridades las ven priorizadas en un panel con lista y mapa de calor. Con ello definen dónde concentrar sus recursos o cómo administrarlos mejor.

La misma red permite que otras personas vean la alerta y acudan. Los intermediarios de propagación pueden ayudar en las emergencias a través de la aplicación; los intermediarios son personas con conocimientos de primeros auxilios o rescate. Quien decide acudir lo anuncia con un paquete "voy en camino", que también se propaga, para que los voluntarios se repartan.

## Primeras ideas

Las ideas están marcadas como **[Decidido]**, **[Posible]** o **[Descartado]**. Ver `AGENTS.md` para su probabilidad de uso.

### Aplicación
- **[Decidido]** App nativa Android, para usar los sensores y la comunicación entre teléfonos en segundo plano. Por ahora solo teléfonos Samsung Galaxy.
- **[Decidido]** Offline-first: toda alerta se guarda primero en el dispositivo y después se intenta enviar.
- **[Decidido]** Bajo consumo de energía. La app se mantiene activa mediante un servicio en primer plano y solicita la exclusión de la optimización de batería. No puede garantizarse en el modo de ultra ahorro.
- **[Descartado para el MVP]** Wi-Fi Direct: es un esquema hub and spoke que no escala como malla. Posible apoyo futuro para transferencias grandes.

### Tipos de emergencia (dos modos de alerta)
1. **Formulario rápido:** el usuario indica su situación en pocos toques.
2. **Alerta sin interacción:** no se puede rellenar nada, y se prioriza enviar la alerta al detectar un movimiento brusco. Lleva una cuenta regresiva de cancelación para evitar falsas alertas.

### Propagación y deduplicación
- **[Decidido]** Topología P2P por gossip BLE, sin conexiones entre teléfonos. Hub and spoke no se usa para propagar; solo aparece el rol de gateway (el dispositivo con internet que sube los paquetes).
- **[Decidido]** El dispositivo origen genera el ID de la alerta, y los demás dispositivos lo conservan al reenviarla. Cada relay agrega solo su marca de salto (TTL).
- **[Decidido]** La deduplicación usa únicamente el ID inmutable (`deviceId + contador`), no el contenido completo del paquete, porque el TTL cambia en cada salto.
- **[Decidido]** Al llegar a una zona con internet, el dispositivo sube el paquete al servidor central. El mismo ID evita duplicación o saturación de alertas.
- **[Decidido]** Para no saturar la malla se reenvía una vez por ID, con retardo aleatorio y límite de saltos. Como BLE advertising no confirma recepción, cada alerta se re-anuncia durante un tiempo.

### Acudir a una alerta
- **[Decidido]** Los dispositivos que propagan ven la alerta y su **ubicación exacta**. Quien envía una alerta de emergencia comparte su ubicación voluntariamente, y la ubicación exacta es lo más importante para poder ayudar.
- **[Decidido]** La app muestra la **distancia y el rumbo** hasta la alerta usando GPS y brújula. Funciona sin internet.
- **[Decidido]** Botón para abrir la ubicación en **Google Maps**, útil cuando hay datos. Sin internet, Maps no calcula rutas salvo que el área esté descargada.
- **[Decidido]** Paquete **"voy en camino"**: el voluntario anuncia que acude. Se propaga como las alertas, y cada alerta muestra cuántas personas van en camino.
- **Limitación declarada:** al mostrar la ubicación exacta a cualquier dispositivo, alguien podría aprovecharse de una víctima. La visibilidad por niveles y la verificación de voluntarios son trabajo futuro.

### Panel de administración
- **[Decidido]** Para los servicios de emergencia: lista de emergencias ordenada por prioridad y mapa de calor.
- **[Decidido]** La ubicación de cada alerta agrupa las alertas en zonas. Se cuenta el número de alertas y de personas por zona, para ver dónde hay más población que necesita ayuda.
- **[Decidido]** El mapa de calor usa colores según el tipo de emergencia:
  - **Amarillo:** necesita comida o agua.
  - **Naranja:** asistencia médica o primeros auxilios.
  - **Rojo:** atrapados en escombros o situación grave.
  - Si una zona mezcla tipos, toma el color del más grave.
- **[Decidido]** El panel también muestra cuántos voluntarios van en camino a cada alerta.
- **[Posible]** Rutas seguras mapeadas, inventario y asignación de recursos.

### Voluntarios
- **[Decidido]** Cualquier usuario con la app puede ver la alerta y acudir, anunciándolo con "voy en camino".
- **[Posible]** Verificación de intermediarios con conocimientos de primeros auxilios.

## Datos que se piden al usuario

El paquete de alerta debe ser muy pequeño (límite de 27 bytes del anuncio BLE), por lo que no admite texto libre. Los datos se dividen en lo que responde el usuario y lo que el teléfono completa solo.

### Formulario rápido (el usuario responde)
1. **Tipo de emergencia** (obligatorio): comida o agua / asistencia médica / atrapado o situación grave.
2. **¿Cuántas personas necesitan ayuda?** (obligatorio): número entero.
3. **¿Hay personas vulnerables?** (opcional, selección múltiple): niños, adultos mayores, personas heridas, movilidad reducida.

### Automático (sin intervención del usuario)
- Ubicación GPS (funciona sin internet).
- ID de la alerta (`deviceId + contador`).
- Fecha y hora de creación.
- Número de saltos / TTL.
- Nivel de batería del dispositivo (opcional, si cabe en el paquete).

### Alerta sin interacción
Solo se envían los datos automáticos y el tipo se marca como **atrapado o grave**, con una persona. Lleva una cuenta regresiva de 10 a 15 segundos para cancelar.

### Respuesta de un voluntario ("voy en camino")
Un toque sobre una alerta cercana. El teléfono completa: ID de la alerta, ID del voluntario, hora y TTL.

### Estado posterior
Después de enviar, el usuario puede **cancelar** la alerta o indicar que **ya está a salvo**.
