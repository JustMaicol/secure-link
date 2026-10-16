# MEMORY.md

Índice de la memoria compartida del proyecto. Cada persona (o agente) lleva su propio registro en `memory/`:

| Archivo | Quién | Specs |
|---|---|---|
| `memory/P1.md` | Persona 1: malla y servicio | 001, 100, 101, 103, 104, 107, 301 |
| `memory/P2.md` | Persona 2: datos, sensores y UI | 000, 003, 102, 105, 106, 108, 109 |
| `memory/P3.md` | Persona 3: web y Supabase | 002, 200-205, 300, 400 |

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
