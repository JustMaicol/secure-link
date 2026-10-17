# 002 esquema de datos Supabase

Capa: 0 · Redacta: P3 · Revisan: P1, P2
Depende de: 000, 001
Entrega: esquema de tablas, vista, permisos y políticas RLS, contrato de subida idempotente y contrato de lectura del panel
Consume: formato de paquetes, claves de deduplicación y forma canónica del `alertId` (001)

## Objetivo

Fijar cómo se guardan en Supabase los paquetes que suben los gateways y cómo los lee el panel, para que el sync de Android (106), la base (200) y el panel (202, 203, 204) trabajen contra el mismo contrato sin coordinarse.

## Alcance

- Tablas `alerts`, `on_the_way` y `alert_status`, que reflejan los paquetes `ALERT`, `ON_THE_WAY` y `STATUS` de la 001.
- Vista `alert_overview` para la carga inicial del panel.
- Permisos por columna y RLS: `anon` solo inserta y `authenticated` solo lee (regla 26).
- Contrato de subida idempotente (106) y de lectura y tiempo real (202-204).
- Entorno: Supabase cloud, plan gratis.

## Fuera de alcance

- Fórmula de prioridad, celdas y colores: se calculan en la web (003, 201), no en la base.
- Datos del modo demo: viven solo en el navegador (205). La base solo guarda datos reales.
- Límite de envíos, firmas y verificación del origen (trabajo futuro, spec 000).
- Cola de subida y reintentos en el teléfono (106).

## 1. Entorno

- Un proyecto en supabase.com, plan gratis.
- Migraciones versionadas en `web/supabase/migrations/` y aplicadas con la CLI de Supabase en versión fijada (regla 12; la versión la fija la 200).
- La URL y la clave `anon` van en archivos ignorados por git: `local.properties` en Android y `web/.env` en la web (regla 25). La clave `service_role` nunca sale del panel de Supabase.
- Auth: registro de usuarios **desactivado**. Un único usuario "autoridad" se crea a mano en el panel de Supabase; sus credenciales no van al repositorio.

## 2. Convenciones

- Nombres en inglés y `snake_case` (regla 34).
- Tiempos en epoch ms UTC como `bigint` (regla 11). No se usa `timestamptz`.
- Coordenadas en enteros grados × 10^7 (`integer`).
- Los `deviceId` de 48 bits van en `bigint`.
- **Sin claves foráneas entre tablas, a propósito:** un `ON_THE_WAY` o un `STATUS` puede llegar al servidor antes que su `ALERT`, por otro gateway. Una clave foránea rechazaría esa subida. El panel une por `alert_id`.
- Rangos copiados de la 001. Si la 001 cambia un rango o `TTL_MAX`, cambia esta spec y se crea una migración nueva.

## 3. Tablas

### 3.1 Del paquete a la fila

| Campo 001 | Columna | Nota |
|---|---|---|
| `deviceId` / `alertDeviceId` | `device_id` / `alert_device_id` | |
| `counter` / `alertCounter` | `counter` / `alert_counter` | |
| `info.emergencyType` | `emergency_type` | |
| `info.automatic`, vulnerables | `automatic`, `children`, `elderly`, `injured`, `reduced_mobility` | un booleano por bit |
| `people` | `people` | |
| `latE7`, `lonE7` | `lat_e7`, `lon_e7` | |
| `createdAtS` | `created_at_ms` | `createdAtS * 1000` |
| `ttl` | `ttl_at_upload` | el TTL con que el gateway lo tiene guardado |
| `relayTag` | — | no se sube: solo sirve en la malla |
| — | `uploaded_by_device_id` | `deviceId` del gateway que subió |
| — | `received_at_ms` | lo pone el servidor; el cliente no puede escribirlo |

**Gana la primera subida.** Con `ON CONFLICT DO NOTHING`, los metadatos de subida (`uploaded_by_device_id`, `ttl_at_upload`, `received_at_ms`) son los del primer gateway. Las subidas siguientes no cambian nada.

### 3.2 DDL (contrato; la 200 lo convierte en migración)

```sql
create table public.alerts (
  device_id              bigint   not null check (device_id between 0 and 281474976710655),
  counter                integer  not null check (counter between 1 and 65535),
  alert_id               text     generated always as
                           (lpad(to_hex(device_id), 12, '0') || '-' || lpad(to_hex(counter), 4, '0')) stored,
  emergency_type         smallint not null check (emergency_type between 0 and 2),
  automatic              boolean  not null,
  children               boolean  not null,
  elderly                boolean  not null,
  injured                boolean  not null,
  reduced_mobility       boolean  not null,
  people                 smallint not null check (people between 1 and 255),
  lat_e7                 integer  not null check (lat_e7 between -900000000 and 900000000),
  lon_e7                 integer  not null check (lon_e7 between -1800000000 and 1800000000),
  created_at_ms          bigint   not null check (created_at_ms >= 0 and created_at_ms % 1000 = 0),
  uploaded_by_device_id  bigint   not null check (uploaded_by_device_id between 0 and 281474976710655),
  ttl_at_upload          smallint not null check (ttl_at_upload between 1 and 8),
  received_at_ms         bigint   not null default (extract(epoch from clock_timestamp()) * 1000)::bigint,
  primary key (device_id, counter),
  unique (alert_id)
);

create table public.on_the_way (
  alert_device_id        bigint   not null check (alert_device_id between 0 and 281474976710655),
  alert_counter          integer  not null check (alert_counter between 1 and 65535),
  volunteer_device_id    bigint   not null check (volunteer_device_id between 0 and 281474976710655),
  alert_id               text     generated always as
                           (lpad(to_hex(alert_device_id), 12, '0') || '-' || lpad(to_hex(alert_counter), 4, '0')) stored,
  created_at_ms          bigint   not null check (created_at_ms >= 0 and created_at_ms % 1000 = 0),
  uploaded_by_device_id  bigint   not null check (uploaded_by_device_id between 0 and 281474976710655),
  ttl_at_upload          smallint not null check (ttl_at_upload between 1 and 8),
  received_at_ms         bigint   not null default (extract(epoch from clock_timestamp()) * 1000)::bigint,
  primary key (alert_device_id, alert_counter, volunteer_device_id)
);

create table public.alert_status (
  alert_device_id        bigint   not null check (alert_device_id between 0 and 281474976710655),
  alert_counter          integer  not null check (alert_counter between 1 and 65535),
  status                 smallint not null check (status in (1, 2)),  -- 1 cancelada, 2 a salvo
  alert_id               text     generated always as
                           (lpad(to_hex(alert_device_id), 12, '0') || '-' || lpad(to_hex(alert_counter), 4, '0')) stored,
  created_at_ms          bigint   not null check (created_at_ms >= 0 and created_at_ms % 1000 = 0),
  uploaded_by_device_id  bigint   not null check (uploaded_by_device_id between 0 and 281474976710655),
  ttl_at_upload          smallint not null check (ttl_at_upload between 1 and 8),
  received_at_ms         bigint   not null default (extract(epoch from clock_timestamp()) * 1000)::bigint,
  primary key (alert_device_id, alert_counter, status)
);
```

`alert_id` lo genera la base con la forma canónica de la 001 (`a1b2c3d4e5f6-0001`). El cliente nunca lo envía.

### 3.3 Vista `alert_overview`

Carga inicial del panel: cada alerta con su número de voluntarios y su último estado.

```sql
create view public.alert_overview with (security_invoker = on) as
select a.*,
       coalesce(v.volunteers, 0) as volunteers,
       s.status                  as latest_status
from public.alerts a
left join (
  select alert_device_id, alert_counter, count(*)::integer as volunteers
  from public.on_the_way
  group by alert_device_id, alert_counter
) v on v.alert_device_id = a.device_id and v.alert_counter = a.counter
left join lateral (
  select st.status
  from public.alert_status st
  where st.alert_device_id = a.device_id and st.alert_counter = a.counter
  order by st.created_at_ms desc, st.status desc
  limit 1
) s on true;
```

- `security_invoker = on` hace que la vista respete el RLS de quien consulta.
- `volunteers` cuenta `volunteer_device_id` distintos: la clave primaria ya impide repetidos (regla 22).
- Desempate del último estado: `created_at_ms` y luego `status` (regla 9).

### 3.4 Saltos (derivado, no se guarda)

El panel calcula los saltos con `TTL_MAX` de la 001 (en 201, no en SQL, regla 17):
- Si `uploaded_by_device_id = device_id`, la subió el propio origen: 0 saltos.
- Si no: `TTL_MAX - ttl_at_upload + 1`.

## 4. Permisos y RLS

```sql
alter table public.alerts       enable row level security;
alter table public.on_the_way   enable row level security;
alter table public.alert_status enable row level security;

revoke all on public.alerts, public.on_the_way, public.alert_status, public.alert_overview
  from anon, authenticated;

-- anon (la app): solo inserta, y solo las columnas del paquete y de la subida
grant insert (device_id, counter, emergency_type, automatic, children, elderly, injured,
              reduced_mobility, people, lat_e7, lon_e7, created_at_ms,
              uploaded_by_device_id, ttl_at_upload)
  on public.alerts to anon;
grant insert (alert_device_id, alert_counter, volunteer_device_id, created_at_ms,
              uploaded_by_device_id, ttl_at_upload)
  on public.on_the_way to anon;
grant insert (alert_device_id, alert_counter, status, created_at_ms,
              uploaded_by_device_id, ttl_at_upload)
  on public.alert_status to anon;

create policy app_insert on public.alerts       for insert to anon with check (true);
create policy app_insert on public.on_the_way   for insert to anon with check (true);
create policy app_insert on public.alert_status for insert to anon with check (true);

-- authenticated (la autoridad): solo lee
grant select on public.alerts, public.on_the_way, public.alert_status, public.alert_overview
  to authenticated;

create policy authority_read on public.alerts       for select to authenticated using (true);
create policy authority_read on public.on_the_way   for select to authenticated using (true);
create policy authority_read on public.alert_status for select to authenticated using (true);
```

- Nadie tiene `update` ni `delete`: los datos subidos no cambian.
- `anon` no puede escribir `received_at_ms` ni `alert_id` (no tiene permiso sobre esas columnas).
- Como el registro está desactivado, `authenticated` equivale al usuario "autoridad".

## 5. Tiempo real

```sql
alter publication supabase_realtime add table public.alerts, public.on_the_way, public.alert_status;
```

Realtime aplica el RLS: solo `authenticated` recibe eventos. La vista no emite eventos. El panel (204) se suscribe a los `INSERT` de las tres tablas y recalcula en el cliente.

## 6. Contrato de subida (spec 106)

- Petición: `POST /rest/v1/<tabla>?on_conflict=<columnas de la clave primaria>` con un arreglo JSON de filas.
- Cabeceras: `apikey: <anon>`, `Authorization: Bearer <anon>`, `Prefer: resolution=ignore-duplicates,return=minimal`.
- Equivale a `INSERT ... ON CONFLICT DO NOTHING`. Repetir la petición no cambia nada (regla 13).
- Con `supabase-js`: `upsert(rows, { onConflict, ignoreDuplicates: true })`.

| Tabla | `on_conflict` |
|---|---|
| `alerts` | `device_id,counter` |
| `on_the_way` | `alert_device_id,alert_counter,volunteer_device_id` |
| `alert_status` | `alert_device_id,alert_counter,status` |

Respuestas:
- `201`: aceptada, sea nueva o repetida. El paquete se marca como subido.
- `400` o `409`: un `check` rechazó la fila. No se reintenta; se registra como error explícito (regla 16). No debería ocurrir si el paquete pasó el `decode` de la 001.
- `401` o `403`: problema de configuración de claves. No se reintenta.
- `5xx` o tiempo agotado: se reintenta con límite (regla 23; valores en la 106).

Una fila inválida hace fallar todo el lote. Por eso la 106 envía solo paquetes ya validados.

## 7. Contrato de lectura (specs 202-204)

- El panel inicia sesión con email y contraseña (Supabase Auth) usando la clave `anon`.
- Carga inicial: `select` de las tres tablas (`alerts`, `on_the_way` y `alert_status`), en páginas de 1000 filas ordenadas por clave primaria. El panel deriva los voluntarios y el último estado en el cliente (200, sección 7) para fusionar sin duplicar con lo que llega por Realtime. `alert_overview` queda para consultas manuales y para el script de aceptación.
- Actualizaciones: Realtime sobre las tres tablas (sección 5).
- Prioridad, celdas, colores y saltos se calculan en `web/src/core/` (003, 201).

## Criterios de aceptación

Se verifican con un script contra el proyecto cloud (lo entrega la 200), usando la clave `anon` y el usuario autoridad:

1. Insertar dos veces el `ALERT` de ejemplo de la 001 (`a1b2c3d4e5f6`, contador 1) con `ignore-duplicates` devuelve `201` las dos veces y deja **una** fila con `alert_id = 'a1b2c3d4e5f6-0001'`.
2. Si un segundo gateway sube la misma alerta con otro `uploaded_by_device_id` y otro `ttl_at_upload`, la fila conserva los valores de la primera subida.
3. Con la clave `anon`, un `select` sobre cualquier tabla o sobre la vista no devuelve filas o es rechazado.
4. Con la clave `anon`, un `update` o un `delete` es rechazado, y también un insert que incluya `received_at_ms` o `alert_id`.
5. Una fila fuera de rango (`emergency_type = 3`, `people = 0`, `lat_e7 = 900000001`, `created_at_ms` no múltiplo de 1000) es rechazada con `400`.
6. Un `ON_THE_WAY` subido antes que su `ALERT` se acepta. Al llegar la alerta, `alert_overview` muestra `volunteers = 1`.
7. El usuario autoridad lee `alert_overview` y recibe por Realtime un `INSERT` nuevo en `alerts`. Sin sesión, no recibe nada.
8. No se puede crear un usuario nuevo desde la API pública (registro desactivado).

## Limitaciones declaradas

- La clave `anon` es pública: cualquiera puede insertar filas válidas falsas. Sin firmas ni límite de envíos en el MVP.
- `received_at_ms` y la latencia derivada dependen de la primera subida; las demás no se registran.
- `created_at_ms` viene del reloj del teléfono, que puede estar desfasado.

## Pendiente de verificar (spec 200)

- Que PostgREST con `resolution=ignore-duplicates` funcione con permiso de `insert` por columna y sin `select` para `anon`. Si no funciona, la alternativa es una función `SECURITY DEFINER` por tabla que haga el `insert ... on conflict do nothing`. Cambiarlo requiere actualizar esta spec.
- Que Realtime con `security_invoker` y RLS se comporte como se describe en el plan gratis.

## Tareas

1. Revisión de P1 y P2 (comentarios en el PR) y aprobación de los tres (regla 4).
2. Spec 200: migración con las secciones 3-5, desactivar el registro, crear el usuario autoridad y el script de los criterios de aceptación.
3. Spec 106: cliente de subida según la sección 6.
4. Specs 202-204: lectura según la sección 7.
