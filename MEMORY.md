# MEMORY.md

Índice de la memoria compartida del proyecto. Cada persona (o agente) lleva su propio registro en `memory/`:

| Archivo | Quién | Specs |
|---|---|---|
| `memory/P1.md` | Persona 1: malla y servicio | 001, 100-A, 101, 103, 107, 301 |
| `memory/P2.md` | Persona 2: interfaz y prototipo visual | 000, 003, 100-B, 106, 108, 109, vectores |
| `memory/P3.md` | Persona 3: radio, sensores y datos (fase Android) · web y Supabase (después) | 002, 102, 104, 105, 200-205, 300, 400 |

> Reparto temporal "prioridad Android" desde el 2026-10-16: ver `specs/plan-prioridades-android.md`. Las specs ya escritas de 102, 104 y 105 tienen su historial en `memory/P1.md` y `memory/P2.md`; desde ahora se registran en `memory/P3.md`.

**Coordinador del índice:** por definir. Es la única persona que edita este archivo.

## Reglas de uso

1. **Cada persona escribe solo en su propio `memory/Pn.md`.** Nunca en el de otra. Así los PRs no chocan.
2. **Este archivo (`MEMORY.md`) lo edita solo el coordinador.** Los demás proponen cambios escribiendo `[DECISIÓN]` en su archivo; el coordinador los pasa aquí.
3. **Al empezar una tarea:** `git pull` y leer este archivo y los tres `memory/Pn.md`. Sin pull, se trabaja con información vieja.
4. **Al terminarla:** actualizar el estado de la spec y agregar una entrada al registro de tu archivo.
5. **El registro solo se agrega al final.** No se borran entradas; las correcciones van como entrada nueva.
6. **Etiquetas** para que los demás encuentren lo importante:
   - `[DECISIÓN]` acuerdo o cambio que afecta a otros.
   - `[CAMBIO COMPARTIDO]` se tocó un archivo compartido o un contrato.
   - `[PIDE a Pn]` necesitas algo de otra persona. Ella responde en su archivo con `[RESPONDE a Pn]`.
   - `[HALLAZGO]` descubrimiento técnico que afecta a varias specs (Samsung, límites de BLE, Android 16/17).
7. **Qué registrar:** interfaces entregadas, archivos compartidos modificados, decisiones, bloqueos, problemas y lo que no se pudo verificar. No el detalle del código ni lo que ya está en el repositorio.
8. **Formato:** `AAAA-MM-DD · spec NNN · [ETIQUETA] texto`. Se declara qué se verificó y qué no.
9. **Un cambio a un contrato compartido** (specs 000-003, vectores) se avisa con `[DECISIÓN]` antes de aplicarlo, y lo hace su dueño.

## Guía mínima de git

Cada persona trabaja en su rama (`p1`, `p2`, `p3`) y edita solo sus carpetas y su archivo de memoria. Por eso los PRs no chocan, sin importar el orden en que se fusionen.

1. **Antes de empezar:** `git switch pN` y `git pull origin main`.
2. **Mientras trabajas:** `git add .` y `git commit -m "spec NNN: qué hiciste"`, con commits pequeños y frecuentes.
3. **Al terminar:** `git push origin pN` y abrir un Pull Request hacia `main` en GitHub. El código y la memoria de la tarea van en el mismo PR.
4. **Después de que se fusione tu PR:** `git pull origin main` antes de seguir.
5. **Si git avisa de un conflicto, no lo resuelvas a ciegas.** Significa que alguien tocó un archivo que no era suyo. Pide ayuda y anótalo con `[HALLAZGO]`.
6. **Nunca** subir secretos, claves ni archivos de `local.properties` o `.env` (están en `.gitignore`).

## Decisiones vigentes

Las decisiones tomadas antes de empezar a programar. Las nuevas las agrega el coordinador.

- 2026-10-16 · BLE gossip por advertising como transporte; Nearby Connections como respaldo.
- 2026-10-16 · `minSdk 34`, solo Samsung Galaxy.
- 2026-10-16 · Spec 001: la deduplicación usa campos inmutables, nunca un hash del paquete completo.
- 2026-10-16 · Ubicación exacta visible para cualquier teléfono con la app (opción A, MVP).
- 2026-10-16 · Repositorio en GitHub; una rama por persona; memoria dividida por persona.
- 2026-10-16 · **Fase de especificación:** una sola persona redacta todas las specs (y registra en el `memory/Pn.md` del dueño nominal). El reparto entre P1, P2 y P3 aplica a la ejecución de las tasks, no a la redacción de las specs.
- 2026-10-16 · **Fase prioridad Android (temporal):** los tres trabajan en Android. P1: 100-A, 101, 103, 107 · P2: 100-B, 108, 109, 106 y vectores · P3: 104, 105, 102. Al terminar, P3 vuelve a la web. Detalle en `specs/plan-prioridades-android.md`.
- 2026-10-16 · **Estructura de specs:** una carpeta por spec (`specs/<área>/NNN-nombre/`) con `spec.md`, `plan.md` y `task.md`. Las rutas viejas de los registros de `memory/` se traducen con la equivalencia anotada en cada `memory/Pn.md`.
- 2026-10-16 · **Fin de la fase de especificación.** Desde ahora cada persona crea su `WHOAMI.md` al clonar (plantillas en `ENTORNO.md`) y trabaja **solo** en sus specs y carpetas (`ARCHITECTURE.md` 6.1, regla 32 ampliada). Lo de otra persona se pide con `[PIDE a Pn]`.
