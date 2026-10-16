# CONSTITUTION

Reglas base para Android, web y simulación. Si una regla choca con una spec, se detiene el trabajo y se consulta; nunca se resuelve por intuición.

## 1. Spec primero
1. Todo código se implementa desde una spec en `specs/`. Sin spec, no hay código.
2. Cada commit y cada función pública referencian su spec (`spec 103`).
3. Si la spec es ambigua o incompleta, se pregunta. No se inventan requisitos ni valores.
4. Los contratos compartidos (specs 000-003) no se modifican sin acuerdo de los tres programadores.

## 2. Determinismo
5. Misma entrada, misma salida. La lógica de dominio son funciones puras: sin red, disco, reloj ni azar internos.
6. El tiempo se inyecta (`Clock`). Prohibido llamar al reloj del sistema dentro de la lógica.
7. El azar se inyecta con semilla (`Random(seed)`). El retardo de reenvío usa ese generador, nunca `Math.random()` ni `Random()` sin semilla.
8. Los IDs se generan con una función inyectable. Formato fijo: `deviceId + contador`.
9. Todo orden es estable y con desempate explícito (prioridad, luego marca de tiempo, luego ID).
10. Sin estado global oculto ni singletons mutables. El estado se pasa de forma explícita.
11. Tiempos en UTC como epoch en milisegundos. Coordenadas como enteros (grados × 10^7), sin comparar flotantes con `==`.
12. Dependencias con versión exacta fijada. Prohibido `latest`, `+` o rangos abiertos.
13. Cada operación de red o de base de datos es idempotente: repetirla no cambia el resultado.

## 3. Estructura
14. Se separa la lógica (pura) de los efectos (BLE, GPS, brújula, Supabase, UI). Los efectos se conectan por interfaces.
15. La lógica del protocolo es la misma en Android y en el simulador, y sale del spec 001.
16. Los errores son explícitos: tipos de resultado o excepciones declaradas. Prohibido tragar errores en silencio.
17. Sin valores mágicos: TTL, tamaños y umbrales viven en una sola configuración documentada.

## 4. Verificación
18. Cada criterio de aceptación tiene al menos una prueba automatizada. La lógica pura se prueba sin dispositivo, red ni reloj real.
19. Una tarea solo está terminada si compila, pasa las pruebas y cumple sus criterios.
20. Al corregir un bug, primero se escribe la prueba que lo reproduce.

## 5. Offline-first y datos
21. Toda alerta se guarda primero en local y después se intenta enviar. Nunca al revés.
22. La deduplicación es solo por ID. La suma por zona cuenta IDs distintos.
23. La app nunca asume internet disponible. Cada llamada de red tiene tiempo límite y reintento acotado.
24. El paquete de alerta respeta el tamaño máximo del spec 001. Sin texto libre en el paquete.

## 6. Seguridad y privacidad
25. Ningún secreto en el repositorio. Las claves van en variables de entorno o archivos ignorados por git.
26. La clave pública de la app solo puede insertar. Las políticas RLS se versionan en `web/supabase/`.
27. Se recolecta el mínimo de datos necesario. Todo dato adicional requiere spec.

## 7. Trabajo con IA
28. Cambios mínimos: solo lo que pide la spec, sin refactors ni extras. Antes de crear algo, se reutiliza lo que ya existe.
29. La IA declara lo que no pudo verificar. Nunca afirma que algo funciona sin haberlo ejecutado.
30. Lo simulado se rotula como simulación en el código, la interfaz y la presentación.
31. Estructura y specs: `ARCHITECTURE.md`. Memoria: leer `MEMORY.md` y `memory/`; escribir solo en tu `memory/Pn.md`.
32. Al clonar el repositorio se crea `WHOAMI.md` en la raíz (1-2 líneas: `Soy Pn; escribo solo en memory/Pn.md`). No se sube (`.gitignore`). Sin él, el agente lo pide antes de actuar.
33. Al completar cada task: se marca `terminada` en tu `memory/Pn.md` y el resumen final incluye el `git commit -m "spec NNN: ..."` listo para subir sus cambios.
