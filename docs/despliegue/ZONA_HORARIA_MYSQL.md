# Zona horaria de MySQL: America/Costa_Rica (T-050, ADR-060)

Procedimiento de despliegue, con inventario, decisiones y plan de reversión. Resuelve P-112 («Pagos del día» contaba con
`CURRENT_DATE` de la base, que está en UTC: después de las 18:00 de Costa Rica el día de la base ya era el siguiente).

## 1. Qué cambia

| Pieza | Antes | Después |
|---|---|---|
| Servidor MySQL (compose, Testcontainers, smoke) | `@@global.time_zone = SYSTEM` (UTC) | `--default-time-zone=America/Costa_Rica` |
| Reloj de la aplicación (`ClockConfig`) | `Clock.systemDefaultZone()` | `Clock.system(HoraFiscal.ZONA)` = America/Costa_Rica |
| «Pagos del día», conteo y facturas vencidas | `CURRENT_DATE` de la base | rango `[inicio, fin)` y fecha calculados con el `Clock` y pasados como parámetro |
| TIMESTAMP escritos por la aplicación | hora local rotulada como UTC | corregidos una sola vez por la migración V51 (+6 h) |
| `DB_URL` | `serverTimezone=UTC` | `serverTimezone=America/Costa_Rica` (ver §5) |
| Prueba de arquitectura | no había | `ArquitecturaRelojBaseTest` falla si un literal SQL de `src/main/java` usa el reloj de la base |

Por qué hay que corregir datos: desde T-033 la conexión usa `preserveInstants=false`, así que la aplicación guarda su hora
local sin conversión. Con el servidor en UTC, una columna TIMESTAMP escrita por la aplicación contiene internamente «hora
local rotulada como UTC». Al pasar el servidor a America/Costa_Rica esos valores se leerían 6 horas antes. La migración
V51 suma 6 horas a cada una (ejecutada con la zona nueva), y se leen de nuevo como antes. Las columnas DATETIME nunca se
convierten por zona, así que **ninguna fecha fiscal cambia** (ADR-030).

## 2. Inventario (dump `facturas_monrachem_20261118_205706`, cargado en una base aislada)

Fuente: `information_schema.COLUMNS` / `ROUTINES` / `TRIGGERS`. Totales: **8 TIMESTAMP**, 120 DATETIME y 32 DATE. Las
migraciones V45–V51 (posteriores al dump) no agregan columnas TIMESTAMP.

### 2.1 Columnas TIMESTAMP y clasificación

| Tabla.columna | DEFAULT | ON UPDATE | Filas no nulas | Clase | Respaldo |
|---|---|---|---|---|---|
| `persistent_logins.last_used` | (ninguno) | no | 2 | **APP** | `SecurityConfig.jdbcTokenRepository()` (línea 237) usa `JdbcTokenRepositoryImpl`, que escribe `new Date()` de la JVM; el DDL (`V2__persistent_logins.sql`) no tiene DEFAULT |
| `flyway_schema_history.installed_on` | `CURRENT_TIMESTAMP` | no | 45 | **BASE** | la escribe Flyway con el DEFAULT de la base; ninguna entidad la mapea |
| `ausencias.created_at` | `CURRENT_TIMESTAMP` | no | 0 | **MIXTA** | `Ausencia.java:93` y `@PrePersist` (:99-101, `LocalDateTime.now()`) + DEFAULT en `V6__rrhh_contratos_ausencias.sql:47` |
| `ausencias.updated_at` | `CURRENT_TIMESTAMP` | `ON UPDATE CURRENT_TIMESTAMP` | 0 | **MIXTA** | `Ausencia.java:96`, `@PreUpdate` (:104-106) + DDL `V6:48` |
| `contratos_empleado.created_at` | `CURRENT_TIMESTAMP` | no | 0 | **MIXTA** | `ContratoEmpleado.java:92`, `@PrePersist` (:98-100) + DDL `V6:22` |
| `contratos_empleado.updated_at` | `CURRENT_TIMESTAMP` | `ON UPDATE CURRENT_TIMESTAMP` | 0 | **MIXTA** | `ContratoEmpleado.java:95`, `@PreUpdate` (:103-105) + DDL `V6:23` |
| `saldo_vacaciones.created_at` | `CURRENT_TIMESTAMP` | no | 0 | **MIXTA** | `SaldoVacaciones.java:51`, `@PrePersist` (:57-59) + DDL `V10__rrhh_saldo_vacaciones.sql:11` |
| `saldo_vacaciones.updated_at` | `CURRENT_TIMESTAMP` | `ON UPDATE CURRENT_TIMESTAMP` | 0 | **MIXTA** | `SaldoVacaciones.java:54`, `@PreUpdate` (:62-64) + DDL `V10:12` |

Criterio: las tres entidades de RRHH asignan `createdAt`/`updatedAt` con `LocalDateTime.now()` (camino de la aplicación) y
las tablas conservan `DEFAULT CURRENT_TIMESTAMP` / `ON UPDATE` (camino de la base: SQL manual, semillas, cualquier
`UPDATE` fuera de JPA). No hay `INSERT`/`UPDATE` nativos sobre esas tablas en `src/main` ni en `database/`, y hoy
tienen 0 filas.

**Corrige V51 (APP): `persistent_logins.last_used`.** BASE (`flyway_schema_history.installed_on`) y MIXTA no se tocan.

### 2.2 Columnas MIXTA: conteo y propuesta (no se corrigen automáticamente)

Conteo en el dump: **0 filas** en las 6 columnas (los módulos de RRHH no se usan en producción). V51 vuelve a contarlas al
correr y escribe un `WARN` si alguna tiene filas.

Propuesta, a decidir por Emanuel solo si el conteo antes del despliegue deja de ser 0 (consulta en §6, paso 0):
(a) si todas las filas las creó la aplicación, sumar 6 h con `UPDATE ... SET c = c + INTERVAL 6 HOUR` en una sesión con la
zona nueva (y `updated_at = updated_at` para no disparar `ON UPDATE`); (b) si hay filas creadas por la base, separarlas
por origen antes; (c) a futuro, dejar un solo camino por columna (que la entidad no asigne y use el DEFAULT, o quitar el
DEFAULT/ON UPDATE). Mientras sea 0 no hay nada que decidir.

### 2.3 Columnas DATETIME y DATE (no afectadas)

La zona del servidor no convierte DATETIME ni DATE. Son 120 DATETIME y 32 DATE; entre ellas todas las fechas fiscales:
`comprobante_electronico.fecha_emision/fecha_envio/fecha_respuesta`, `factura.create_date/fecha_pago/fecha_vencimiento`,
`nota_factura.fecha_emision/fecha_aplicacion/...`, `pagos.fecha_pago`. DATETIME por tabla:

- `ajustes_inventario`: created_at,fecha,fecha_aprobacion,updated_at
- `articulo_maestro`: create_date,update_date
- `asientos_contables`: fecha_creacion,fecha_modificacion
- `auditoria_producto_iva`: fecha
- `ausencias`: fecha_aprobacion
- `cliente`: create_date,fecha_registro,update_date
- `comprobante_electronico`: created_at,fecha_emision,fecha_envio,fecha_envio_email,fecha_respuesta,updated_at
- `configuracion_email`: create_date,ultimo_test,update_date
- `configuracion_empresa`: create_date,update_date
- `configuracion_facturacion`: create_date,update_date
- `configuracion_hacienda`: created_at,token_expira,updated_at
- `configuracion_notificaciones`: create_date,update_date
- `conversacion_pedido`: create_date,update_date
- `cuenta_bancaria`: create_date,update_date
- `cuentas_contables`: fecha_creacion,fecha_modificacion
- `cuentas_por_pagar`: created_at,updated_at
- `departamentos`: created_at,updated_at
- `detalles_asiento`: create_date,update_date
- `empleados`: created_at,updated_at
- `empresa`: create_date,update_date
- `factura`: create_date,update_date,fecha_anulacion
- `linea_factura`: create_date,update_date
- `lotes_productos`: created_at,updated_at
- `mensaje_whatsapp`: create_date,fecha_entrega,fecha_envio,fecha_lectura,update_date
- `movimientos_inventario`: created_at,fecha
- `nominas`: created_date,last_modified_date
- `nota_factura`: fecha_emision,fecha_aplicacion,fecha_emision_referencia,create_date,update_date
- `notificacion`: fecha_envio,fecha_lectura
- `ordenes_compra`: created_at,fecha_aprobacion,updated_at
- `pagos`: anulado_en,fecha_conciliacion,fecha_creacion,fecha_modificacion,fecha_pago
- `pagos_proveedor`: created_at
- `parametro_sistema`: create_date,update_date
- `parametros_ccss`: created_at
- `parametros_nomina_empresa`: created_at
- `permiso`: create_date,update_date
- `plantilla_notificacion`: create_date,update_date
- `plantilla_whatsapp`: create_date,fecha_aprobacion,update_date
- `preferencia_notificacion`: create_date,update_date
- `presentacion`: create_date,update_date
- `producto`: create_date,update_date
- `proveedores`: created_at,updated_at
- `puestos`: created_at,updated_at
- `respuesta_hacienda`: created_at,fecha_respuesta
- `rol`: create_date,update_date
- `salarios_minimos`: created_at
- `tipos_cambio`: fecha_creacion
- `tramos_impuesto_salario`: created_at
- `usuario`: create_date,fecha_bloqueo,fecha_expiracion_password,ultimo_acceso,update_date
- `usuario_actividad`: fecha_actividad
- `usuario_permiso`: create_date,update_date
- `usuario_sesion`: fecha_cierre,fecha_expiracion,fecha_inicio,ultima_actividad
- `webhooklogs`: timestamp

**Nota: 7 columnas DATETIME con `DEFAULT CURRENT_TIMESTAMP`** (las llena la base, no la aplicación):
`auditoria_producto_iva.fecha`, `empleados.created_at` y `empleados.updated_at` (esta con `ON UPDATE`),
`parametros_ccss.created_at`, `parametros_nomina_empresa.created_at`, `salarios_minimos.created_at` y
`tramos_impuesto_salario.created_at`. Las filas que la base llenó con el servidor en UTC guardan hora UTC; las que llene
después del cambio guardarán hora de Costa Rica. V51 no las toca (son DATETIME y la zona no las convierte; quedan fuera de la
corrección del alcance). No se contaron filas por tabla aquí: el conteo debe verificarse en producción con una consulta de
solo lectura, por ejemplo:

```sql
SELECT 'auditoria_producto_iva' t, COUNT(fecha) n FROM auditoria_producto_iva
UNION ALL SELECT 'empleados', COUNT(created_at) FROM empleados
UNION ALL SELECT 'parametros_ccss', COUNT(created_at) FROM parametros_ccss
UNION ALL SELECT 'parametros_nomina_empresa', COUNT(created_at) FROM parametros_nomina_empresa
UNION ALL SELECT 'salarios_minimos', COUNT(created_at) FROM salarios_minimos
UNION ALL SELECT 'tramos_impuesto_salario', COUNT(created_at) FROM tramos_impuesto_salario;
```

Propuesta: dejarlas como están (son marcas de auditoría, sin efecto fiscal). Si alguna se usara para lógica de negocio (por
ejemplo ordenar o filtrar por día), decidir entonces, caso por caso, si se suman 6 h a las filas anteriores al cambio.

## 3. Rutinas y triggers que usan el reloj de la base (para P-009 / P-038; no se eliminan aquí)

Dump: 33 procedimientos y 2 triggers (`information_schema.ROUTINES` / `TRIGGERS`).

| Rutina o trigger | Función del reloj de la base |
|---|---|
| `CrearFactura` | `NOW()` |
| `ActualizarCliente`, `sp_actualizar_linea_factura`, `sp_crear_preferencias_usuario` | `CURRENT_TIMESTAMP` |
| `RegistrarWebhook`, `SP_CLIENTES_NUEVOS_POR_MES`, `SP_ESTADISTICAS_DASHBOARD`, `sp_obtener_clientes_nuevos_por_mes`, `sp_obtener_ventas_por_mes`, `sp_resumen_comprobantes_empresa` | `CURDATE()` |
| trigger `trg_after_update_usuario` | `CURRENT_TIMESTAMP` (`preferencia_notificacion.create_date/update_date`) |
| trigger `trg_after_insert_usuario` | no usa el reloj (llama a `sp_crear_preferencias_usuario`) |

Java llama hoy a `CALL sp_obtener_ventas_por_mes` (`FacturaRepository:75`) y `CALL sp_obtener_clientes_nuevos_por_mes`
(`ClienteRepository:20`). Efecto del cambio de zona: lo que esas rutinas escriben o calculan con la base pasa de «UTC» a
«hora de Costa Rica» (la misma de la aplicación). La prueba de arquitectura solo recorre el código Java; el SQL de
`database/routines/` queda fuera y se resuelve al retirar las rutinas (P-009/P-038).

## 4. Consultas movidas al `Clock` (búsqueda de `CURRENT_DATE|CURRENT_TIMESTAMP|CURDATE|NOW|SYSDATE` en `src/main/java`)

| Consulta | Antes | Ahora |
|---|---|---|
| `PagoRepository.findPagosDelDia` | `CAST(p.fechaPago AS date) = CURRENT_DATE` | `findPagosDelDia(inicio, fin)` con `fechaPago >= :inicio AND < :fin` |
| `PagoRepository.countPagosDelDia` | ídem | `countPagosDelDia(inicio, fin)` |
| `FacturaRepository.findFacturasConPagoVencido` | `f.fechaPago < CURRENT_DATE` | `findFacturasConPagoVencido(java.sql.Date hoy)`, `f.fechaPago < :hoy` |

Son las únicas tres que encontró la búsqueda (las demás consultas `@Query` y el SQL nativo no usan el reloj de la base). Los
servicios las llaman con la fecha del `Clock`: `PagoServiceImpl` (`findPagosDelDia`, `countPagosDelDia`,
`sumIngresosDiaActual` y la `fechaPago` por defecto), `PagoController` (fecha por defecto del reporte de caja) y
`RecordatorioPagoScheduler` (vencidas y por vencer). La API de `PagoService` no cambia.

## 5. Parámetros de zona de la `DB_URL`

Se probó, con el servidor MySQL en America/Costa_Rica y la JVM en America/Costa_Rica (`ZonaMysqlIdaYVueltaIT`, que escribe
un `LocalDateTime` de las 20:00 en TIMESTAMP y DATETIME, como `LocalDateTime` y como `Timestamp`, y lee las 20:00 en
ambas), cada variante de la URL: `serverTimezone=UTC`, `serverTimezone=America/Costa_Rica` y `connectionTimeZone=SERVER`.
**Las tres dan el mismo resultado**: con `preserveInstants=false` (T-033, `MysqlFechasSinConversionConfig`) el conector no
convierte entre la zona de la JVM y la de la conexión, así que la hora local viaja tal cual y el servidor, que ahora
también está en hora local, interpreta el TIMESTAMP en su zona. Además `NOW()` de la base coincide con
`LocalDateTime.now(clock)` (tolerancia de 1 minuto).

Decisión: `serverTimezone=America/Costa_Rica` en `docker-compose.yml` y `.env.example`. La URL ya no dice una zona que no es
la del servidor y el comportamiento no cambia. `preserveInstants=false` se mantiene (lo agrega el código, no depende de la
URL). **Qué cambia el operador en su `.env`:** nada es obligatorio. Si su `DB_URL` conserva `serverTimezone=UTC` sigue
funcionando (verificado); se recomienda cambiarlo a `serverTimezone=America/Costa_Rica` para que coincida con el
compose (el servicio `app` del compose define `DB_URL` por su cuenta y tiene prioridad sobre el `.env`).

`-06:00` en lugar del nombre: solo si en algún entorno MySQL no tiene las tablas de zonas con nombre
(`SELECT COUNT(*) FROM mysql.time_zone_name` = 0; en producción son 1789 filas). Es equivalente porque Costa Rica no tiene
horario de verano, y tanto la guarda de V51 como esta guía aceptan ambos valores.

## 6. Migración V51 (`V51__zona_costa_rica_corregir_timestamps`)

1. Verifica `@@global.time_zone` ∈ {`America/Costa_Rica`, `-06:00`} y que `@@session.time_zone` sea el mismo (Flyway no lo
   cambia). Si no, lanza `FlywayException` («el servidor MySQL no está en America/Costa_Rica ... reinicie mysql con
   `--default-time-zone=America/Costa_Rica`») sin modificar nada.
2. Para cada columna APP cuya tabla y columna existen (metadatos; las ausentes se omiten, así una base vacía pasa):
   `UPDATE t SET c = c + INTERVAL 6 HOUR WHERE c IS NOT NULL`, y registra las filas por columna.
3. Para las MIXTA solo cuenta y avisa (`WARN`) si hay filas. No toca las BASE ni las DATETIME.

Corre una sola vez porque Flyway la registra en `flyway_schema_history`; la guarda impide que corra antes del cambio de zona.
Todo ocurre en la transacción de Flyway. Pruebas: `V51ZonaTimestampsIT` (MySQL real: corrige las APP, deja BASE, MIXTA y
DATETIME; con el servidor en UTC aborta sin cambiar nada; base vacía no falla).

## 7. Procedimiento de despliegue en producción

Contenedores: `whats_orders_mysql` (mysql:8.0.32) y `whats_orders_app`. Todas las consultas de verificación son de solo
lectura y ninguna hace `SELECT *` sobre `configuracion_hacienda` ni `configuracion_email`. En los comandos, la contraseña
de root la toma el contenedor de su propia variable (`$MYSQL_ROOT_PASSWORD`); no se escribe en la línea de comandos.

Atajo para las consultas: `Q() { docker exec -i whats_orders_mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N "$MYSQL_DATABASE"' ; }`

**Paso 0. Antes de tocar nada (solo lectura)**

```sql
SELECT @@global.time_zone, @@system_time_zone, NOW(), UTC_TIMESTAMP();   -- hoy: SYSTEM, UTC, NOW = UTC
SELECT COUNT(*) FROM mysql.time_zone_name;                               -- > 0 (1789): sirve el nombre de zona
-- MIXTA: deben ser 0 (si no, ver §2.2 antes de seguir)
SELECT (SELECT COUNT(ausencias.created_at) FROM ausencias) + (SELECT COUNT(updated_at) FROM ausencias)
     + (SELECT COUNT(created_at) FROM contratos_empleado) + (SELECT COUNT(updated_at) FROM contratos_empleado)
     + (SELECT COUNT(created_at) FROM saldo_vacaciones) + (SELECT COUNT(updated_at) FROM saldo_vacaciones) AS filas_mixta;
-- Valores de referencia (guardar la salida): fechas fiscales y TIMESTAMP de la app
SELECT id, fecha_emision, fecha_envio, fecha_respuesta,
       SUBSTRING_INDEX(SUBSTRING_INDEX(xml_comprobante,'<FechaEmision>',-1),'</FechaEmision>',1) AS fecha_en_xml
  FROM comprobante_electronico ORDER BY id;
SELECT id_factura, create_date, fecha_vencimiento, fecha_pago, fecha_entrega FROM factura ORDER BY id_factura;
SELECT id, fecha_emision, fecha_aplicacion, fecha_emision_referencia, create_date FROM nota_factura ORDER BY id;
SELECT series, last_used FROM persistent_logins ORDER BY series;
SELECT COUNT(*), MIN(installed_on), MAX(installed_on) FROM flyway_schema_history;
```

**Paso 1. Respaldo** (con la app detenida no habrá escrituras: ver paso 2; si prefiere, deténgala antes)

```sh
docker compose stop app
docker exec whats_orders_mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --events --set-gtid-purged=OFF "$MYSQL_DATABASE"' > respaldo_pre_t050_$(date +%Y%m%d_%H%M%S).sql
ls -l respaldo_pre_t050_*.sql && grep -c '^CREATE TABLE' respaldo_pre_t050_*.sql      # tamaño razonable y tablas presentes
cp docker-compose.yml docker-compose.yml.pre-t050 && cp .env .env.pre-t050            # configuración anterior (para la reversión; no versionarlos: el .env tiene credenciales)
```

**Paso 2. Confirmar que la app está detenida** (el paso 1 ya la detuvo): `docker compose ps` no debe mostrar `whats_orders_app` en ejecución.

**Paso 3. Cambiar el compose y reiniciar mysql.** Traer la versión con T-050 (el servicio `mysql` lleva
`--default-time-zone=America/Costa_Rica`). Si aplica la guía de §5, cambiar también `serverTimezone` en el `DB_URL` de su `.env`.

```sh
docker compose up -d mysql        # recrea el contenedor por el cambio de command; el volumen y los datos se conservan
docker compose ps                 # esperar "healthy"
```

**Paso 4. Verificar la zona (solo lectura)**

```sql
SELECT @@global.time_zone, @@system_time_zone, NOW(), UTC_TIMESTAMP();
```

Esperado: `America/Costa_Rica`, `UTC`, `NOW()` igual a la hora local (`date` en el servidor) y `UTC_TIMESTAMP()` 6 horas
adelante. Si `@@global.time_zone` no es ese valor **no arranque la app** (V51 abortaría de todos modos): corrija y repita.
Aún antes de V51, `SELECT series, last_used FROM persistent_logins` mostrará los valores 6 horas antes: es lo esperado.

**Paso 5. Arrancar la app** (aplica Flyway)

```sh
docker compose up -d --build app
docker logs whats_orders_app 2>&1 | grep -E 'V51|Migrating schema|Successfully applied'
```

**Paso 6. Verificar el log y los datos**

Log esperado: `V51: persistent_logins.last_used corregida (+6 h): N fila(s)` (N = filas de `persistent_logins`, 2 en el dump) y
`V51: columnas TIMESTAMP corregidas: {persistent_logins.last_used=N}`, más una línea por cada columna MIXTA («sin filas»; un
`WARN` si tuviera). Un `FlywayException` de V51 significa que la zona no es la correcta (volver al paso 3); no se modificó nada.

Repetir las consultas del paso 0 y comparar. Esperado:
- `comprobante_electronico` (todos los comprobantes, en particular 10, 11 y 12: `fecha_emision` 2026-11-18 13:49:26.231523,
  13:54:27.138309 y 14:50:15.716621 en el dump), `factura`, `nota_factura`: **idénticos** al paso 0, y `fecha_en_xml` igual
  (p. ej. `2026-11-18T13:49:26-06:00`).
- `persistent_logins.last_used`: igual al paso 0 (V51 sumó las 6 horas).
- `flyway_schema_history.installed_on`: los registros viejos se leen 6 horas antes que en el paso 0 (BASE: eran UTC reales y
  ahora se muestran en hora local; correcto), y aparecen los de las migraciones nuevas en hora local.

**Paso 7. «Pagos registrados hoy» en `/pagos`.** Iniciar sesión, abrir `/pagos`: la tarjeta «Pagos registrados hoy» cuenta los
pagos del día de Costa Rica (también después de las 18:00). Comprobación independiente (solo lectura):

```sql
SELECT COUNT(*) FROM pagos WHERE fecha_pago >= CURDATE() AND fecha_pago < CURDATE() + INTERVAL 1 DAY AND tipo_pago <> 'NOTA_CREDITO';
```

(esta consulta manual usa el reloj de la base porque es de verificación y ya coincide con el de Costa Rica; no está en el código).

## 8. Reversión

Si algo sale mal (V51 aplicada o no), se vuelve al estado previo con el respaldo del paso 1: la restauración trae también
`flyway_schema_history` sin V51, así que la migración podrá volver a correr limpia más adelante.

```sh
docker compose stop app
# 1) configuración anterior: restaurar docker-compose.yml y .env previos (sin --default-time-zone)
cp docker-compose.yml.pre-t050 docker-compose.yml && cp .env.pre-t050 .env
# 2) restaurar la base desde el respaldo, con mysql ya en la configuración anterior
docker compose up -d mysql                       # vuelve a UTC (SYSTEM)
docker exec whats_orders_mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE \`$MYSQL_DATABASE\`; CREATE DATABASE \`$MYSQL_DATABASE\`"'
docker exec -i whats_orders_mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < respaldo_pre_t050_AAAAMMDD_HHMMSS.sql
# 3) volver a la imagen/código anterior de la app y arrancarla
docker compose up -d --build app
```

Verificar con las consultas del paso 0: `@@global.time_zone = SYSTEM`, y fechas fiscales, `persistent_logins` y
`flyway_schema_history` iguales a las del paso 0. Importante: no revertir solo la zona sin restaurar (con V51 aplicada, los
TIMESTAMP de la app quedarían 6 horas adelantados) ni dejar la versión nueva de la app con el servidor en UTC (V51 abortaría y
la «fecha de hoy» seguiría saliendo del `Clock`, que es independiente de la base).

## 9. Ensayo del despliegue (smoke con el dump, 2026-11-21)

Protocolo de `CLAUDE.md` (copia sin `.env`, MySQL aislado `t050-smoke-mysql` con volumen con nombre, `APP_SCHEDULING_ENABLED=false`,
`-Djdk.net.hosts.file` solo localhost, `TZ=America/Costa_Rica`). Credenciales ficticias y solo en esa base: usuario root de
MySQL con la clave desechable `t050root`, y para iniciar sesión la contraseña ficticia `Smoke-T050-ficticia` asignada (BCrypt)
al usuario 1 solo en la base aislada. Pasos: (1) dump cargado en MySQL en UTC (`@@global.time_zone = SYSTEM`) y valores
registrados; (2) contenedor recreado sobre el mismo volumen con `--default-time-zone=America/Costa_Rica`
(`@@global.time_zone = America/Costa_Rica`, `NOW()` 16:17:07, `UTC_TIMESTAMP()` 22:17:07); (3) app arrancada: Flyway aplicó
V45 a V51.

Log de V51:

```
V51: persistent_logins.last_used corregida (+6 h): 2 fila(s)
V51: columna MIXTA ausencias.created_at sin filas; nada que corregir   (idem updated_at, contratos_empleado, saldo_vacaciones)
V51: columnas TIMESTAMP corregidas: {persistent_logins.last_used=2}
```

Comparación de valores visibles (columna por columna sobre las 160 columnas DATETIME/DATE/TIMESTAMP, con huella MD5 de los
valores ordenados, más el listado explícito de fechas fiscales):

| Valor | UTC (antes) | CR sin V51 | CR con V51 (después) |
|---|---|---|---|
| `comprobante_electronico` 1–12: `fecha_emision`, `fecha_envio`, `fecha_respuesta` (DATETIME) y `<FechaEmision>` del XML firmado (p. ej. id 10: `2026-11-18 13:49:26.231523`, `2026-11-18T13:49:26-06:00`) | iguales | iguales | **idénticos** |
| `factura.create_date/fecha_pago/fecha_entrega`, `nota_factura.fecha_emision/fecha_aplicacion/...` | iguales | iguales | **idénticos** |
| las demás columnas DATETIME/DATE que ya existían | huella MD5 | igual | **huella idéntica** (salvo tablas con filas nuevas sembradas por las migraciones V45–V51: `cuentas_contables`, `permiso`, `nota_factura.fecha_contable`, tablas nuevas) |
| `persistent_logins.last_used` (APP) | `2026-08-10 06:12:40` / `2026-08-27 03:23:57` | `2026-08-10 00:12:40` / `2026-08-26 21:23:57` | `2026-08-10 06:12:40` / `2026-08-27 03:23:57` (**igual que antes**) |
| `flyway_schema_history.installed_on` (BASE) | 2026-07-20 19:23:10 … 2026-11-18 19:24:54 | 13:23:10 … 13:24:54 | 13:23:10 … (se muestra en hora local; 7 filas nuevas) |
| MIXTA (6 columnas) | 0 filas | 0 | 0 |

`/pagos` (corrida a las 16:19 CST, `Clock` real): se registró por `POST /pagos/registrar` un pago de ₡500
(`PAG-20261121-0001`, `fecha_pago` 2026-11-21 16:19:22); la página mostró «Ingresos del Día ₡ 500.00» y «Pagos Registrados
Hoy 1». Con el servidor ya en America/Costa_Rica el día de la base y el de la aplicación coinciden a cualquier hora, así que
el caso de las 20:00 no se distingue en esta corrida de día; lo cubre `RelojFijoNegocioIT` (reloj fijo a las 20:00 de Costa
Rica = 02:00 UTC del día siguiente), que fallaba antes del cambio.

**Segunda corrida independiente, después de las 18:00 CST (2026-11-21).** Mismo protocolo, base nueva a partir del dump
(primero en UTC, luego recreada con la zona nueva). Credenciales ficticias, solo en la base aislada: usuario id 1, contraseña
`Smoke-T050v-ficticia`.
- Cambio de zona: `NOW()` = 2026-11-21 18:00:10 y `UTC_TIMESTAMP()` = 2026-11-22 00:00:10.
- Log de V51: `V51: persistent_logins.last_used corregida (+6 h): 2 fila(s)` y
  `V51: columnas TIMESTAMP corregidas: {persistent_logins.last_used=2}`.
- `persistent_logins.last_used`: antes de V51 se leía 2026-08-26 21:23:57 / 2026-08-10 00:12:40; después de V51,
  2026-08-27 03:23:57 / 2026-08-10 06:12:40, idénticos a la línea base en UTC.
- Fiscales de los comprobantes 1–12, facturas y notas: idénticos; MD5 de las columnas de fecha de `comprobante_electronico`
  `a717413437374fa77a3246e322733915` antes y después.
- `/pagos`: «Pagos Registrados Hoy» mostró 0 antes del pago; tras registrar un pago a las 18:01:07 CST (en UTC ya era
  2026-11-22 00:01, donde el `CURRENT_DATE` anterior habría mostrado 0) mostró «Pagos Registrados Hoy» = 1 e «Ingresos del Día»
  ₡ 500.00.
