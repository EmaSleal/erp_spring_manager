# T-057: diagnóstico del criterio 1 y smoke de los criterios 13 y 18

Fecha de la ejecución: 2026-11-25. Código: commits `201bc5c` (parte 1) y `c5a51e9` (parte 2). Datos: copia de producción
`facturas_monrachem_20261118_205706.sql` (leída en el sitio, sin copiarla), historial Flyway hasta V44.

## 1. Diagnóstico del criterio 1 (solo lectura, base aislada con el volcado y Flyway V45..V61)

### 1.1 Qué significa «gravado» frente a `precioIncluyeIva`

| Indicador | Dónde | Qué dice |
|---|---|---|
| `gravado` (+ `porcentajeImpuesto`) | `ArticuloMaestro.java:49-56`; respaldo deprecado en `Producto.java:93-103` | Si el artículo lleva IVA y con qué tarifa. Se resuelve en `CalculadoraImpuestoService.resolveGravado` (`:135-139`: maestro primero, producto como respaldo) y `resolverTarifa` (`:109-120`: no gravado → 0; si no, su porcentaje; si no, tarifa general). Un `gravado` nulo se trata como gravado (ADR-009). |
| `precioIncluyeIva` | solo `Producto.java:119` (`NOT NULL`, por defecto `true`); `ArticuloMaestro` no lo tiene | Si el precio registrado del producto ya trae el IVA dentro. Lo consume `CalculadoraImpuestoService.desglosar` (`:75-89`): con `true` desglosa (`base = importe / (1 + tarifa)`), con `false` suma el IVA encima (`importe × tarifa`). `desglosarLinea` (`:94-97`) trata nulo como `true`. |

Son independientes: `gravado` decide si hay IVA y a qué tarifa; `precioIncluyeIva` decide si el IVA se desglosa del precio o se suma a él.
En el régimen simplificado la tarifa efectiva es 0 (`PoliticaImpuestoVenta.tarifaAplicable`), así que el precio del cliente es el total sin importar ninguno de los dos indicadores.

### 1.2 Números sobre el volcado

| Concepto | Valor |
|---|---|
| Productos | 172 (172 activos) |
| Productos con `precio_incluye_iva = false` | **0** |
| Productos con `precio_incluye_iva = true` | 172 |
| Artículos maestros | 91 (88 activos) |
| Maestros gravados / no gravados | 91 / 0 |
| Productos con maestro | 172 de 172 (el `gravado` del producto es nulo en 1 y verdadero en 171; manda el del maestro) |
| Maestros por tarifa | 80 al 13 % (161 productos), 11 al 1 % (11 productos) |

### 1.3 Total al cliente por unidad, RTS frente a general

Como ningún producto tiene `precio_incluye_iva = false`, no hay filas que listar: el precio registrado ya incluye el IVA en los 172 productos.

| Grupo (productos) | Total por unidad en el general | Total por unidad en el RTS | Diferencia al cliente |
|---|---|---|---|
| Gravados al 13 % con precio incluido (161) | precio | precio | 0 (el general desglosa precio/1,13 + IVA; el RTS lo registra todo como ingreso) |
| Gravados al 1 % con precio incluido (11) | precio | precio | 0 |
| Con `precio_incluye_iva = false` (0) | precio × (1 + tarifa) | precio | no aplica hoy |

Consecuencia para ADR-073 §4: con los datos actuales el cliente paga lo mismo en ambos regímenes; lo que cambia es el desglose y que el titular no traslada IVA. Si algún producto se creara con `precio_incluye_iva = false`, el RTS lo vendería a `precio` y el general a `precio × (1 + tarifa)`.
Ejemplo medido en el smoke (producto gravado 13 %, 2 × 500,00): general 884,96 + IVA 115,04 = 1.000,00; RTS 1.000,00 con IGV 0,00.

### 1.4 Hallazgo del criterio 2 (relleno de las facturas existentes)

La migración V60 selló las 12 facturas del volcado como GENERAL. El tramo histórico (semilla de T-056) dice SIMPLIFICADO antes del 2026-09-01:

| Facturas | Fechas | IGV | Régimen sellado | Régimen según el tramo |
|---|---|---|---|---|
| 1, 2, 3 | 2026-07-04, 07-25, 07-31 | 0,00 / 0,00 / sin totales | GENERAL | SIMPLIFICADO |
| 4 a 12 | 2026-11-13 a 11-18 | con IVA | GENERAL | GENERAL |

Las tres facturas de julio se calcularon sin IVA (como RTS) pero quedan selladas GENERAL, como pide el criterio 2 («como se calcularon» no se cumple para ellas). Efecto: son GENERAL emisibles hasta el cierre y cuentan hoy en «Facturas sin comprobante» del panel; el aviso de cierre cuenta solo desde 2026-09-01 (ver 3.8), de modo que no las incluye. No se probó editarlas; una edición recalcularía con IVA por ser GENERAL. Decisión pendiente para Emanuel.

## 2. Preparación del smoke (declaración)

| Elemento | Qué se hizo |
|---|---|
| Copia sin secretos | Jar construido desde el worktree (sin `.env`); nada se ejecutó en la raíz del repositorio. |
| Base de datos | Contenedor `t057-mysql` (mysql:8.0.32), `--tmpfs /var/lib/mysql`, `--default-time-zone=America/Costa_Rica` (`@@global.time_zone` = America/Costa_Rica), puerto 127.0.0.1:33657. Volcado cargado con `mysql < volcado`. |
| Arranque | Protocolo de CLAUDE.md: `env -i`, `TZ=America/Costa_Rica`, `APP_SCHEDULING_ENABLED=false`, `TIPO_CAMBIO_SYNC_ENABLED=false`, `-Djdk.net.hosts.file` solo con localhost, directorios en un temporal propio, `LOGGING_LEVEL_ORG_FLYWAYDB=INFO`. |
| Reloj (opción b) | Lanzador solo de smoke, **no versionado**: `PropertiesLauncher` con `-Dloader.path` a una clase `@Configuration` con un `Clock` `@Primary` que devuelve el instante escrito en un archivo (vacío = reloj del sistema). Sirve para mover el `Clock` de la aplicación sin reiniciar. |
| Tramo insertado (opción a) | `regimen_tributario`: `SIMPLIFICADO` desde `2027-01-01` (nota «T-057 smoke: tramo ficticio de prueba»), insertado por SQL antes del segundo arranque (el servicio cachea los tramos). Los tramos de la semilla (SIMPLIFICADO desde 2000-01-01, GENERAL desde 2026-09-01) no se tocaron. No hay tramo GENERAL posterior. |
| Datos ficticios | Un cliente «Cliente Ficticio T057» (id 4, correo `.invalid`, `requiere_factura_electronica = 0`); el usuario 1 de la base aislada con correo ficticio y contraseña restablecida con BCrypt para iniciar sesión (rol ADMIN). Productos: el producto 1 del catálogo (gravado 13 %), 2 × 500,00 por factura. Claves externas: una de 50 dígitos inventada. |
| Cliente HTTP | `curl` con sesión (login por `/auth/login`) y cabecera `X-CSRF-TOKEN`. Facturas creadas con `POST /facturas/guardar` y `PUT /lineas-factura/actualizar`, como lo hace la pantalla. |
| Instantes del Clock | Se usaron: 2026-11-25 (reloj real), 2026-12-19 12:00 CR, 2026-12-20 12:00 CR, 2026-12-22 12:00 CR, 2026-12-31 12:00 CR, 2027-01-01 12:00 CR y 2027-01-05 12:00 CR. |

Limitación de la herramienta, no del código: `createDate` y la fecha del asiento salen del reloj del sistema (aparecen como 2026-11-25), mientras que el sellado del régimen y las reglas de emisión leen el `Clock` de la aplicación. Por eso las «facturas de diciembre» (14 y 15) son facturas creadas con el Clock en 2026-12-22 y la factura SIMPLIFICADO (16) se creó con el Clock en 2027-01-05.

### Log

```
Migrating schema `smoke` to version "60 - factura regimen emisor"
Migrating schema `smoke` to version "61 - factura comprobante externo"
Successfully applied 17 migrations to schema `smoke`, now at version v61
Started WhatsOrdersManagerApplication in 10.9 seconds
```

Con `APP_SCHEDULING_ENABLED=false`: el volcado de hilos de la JVM no tiene ningún hilo `tareas-*` (el `TaskScheduler` no existe, `SchedulingConfig`) y el log no registra ningún job. `flyway_schema_history` queda con V60 y V61 en `success = 1`; el segundo arranque dice «Schema is up to date. No migration necessary».
Tras V60: las 12 facturas del volcado tienen `regimen_emisor = GENERAL`.

## 3. Pasos y resultados

Línea base del panel `/facturas/inicio` (Clock real, antes de crear nada): «Facturas sin comprobante» = 3 (las facturas 1, 2 y 3 del volcado); el aviso de cierre no aparece.

| # | Criterio | Paso (resumen) | Resultado observado | Estado |
|---|---|---|---|---|
| 3.1 | 13 (control GENERAL) | Factura 13, Clock real 2026-11-25, tramo vigente GENERAL: crear con 2 × 500,00 gravado | `regimen_emisor = GENERAL`, subtotal 884,96, **igv 115,04**, total 1.000,00 | Pasa |
| 3.2 | 13 | Factura 16 con el Clock en 2027-01-05 (tramo SIMPLIFICADO desde 2027-01-01) | `regimen_emisor = SIMPLIFICADO`, subtotal 1.000,00, **igv 0,00**, total 1.000,00 | Pasa |
| 3.3 | 13 | Asiento de la factura 16 | Solo CxC al debe e ingreso al haber por el total; sin línea de IVA por pagar (ver tabla de asientos) | Pasa |
| 3.4 | 13 | `POST /api/facturas/electronica/comprobantes/procesar/16` | **409** `{"message":"Las ventas del régimen simplificado no se emiten a Hacienda"}`; comprobantes del sistema 12 antes y después, `max(consecutivo)` = 1003 antes y después, 0 comprobantes de la factura | Pasa |
| 3.5 | 13 | Panel antes y después de crear la factura 16 (Clock 2027-01-05) | «Facturas sin comprobante» = 5 antes y 5 después: no la cuenta | Pasa |
| 3.6 | 13 | `GET /facturas/pdf/16` y `pdftotext` | Título «Comprobante de venta — Régimen de Tributación Simplificada»; sin columna «%IVA», sin fila «IVA:», sin clave ni consecutivo; `Sub Total ¢1,000.00` y `Total ¢1,000.00`. La factura 13 (GENERAL) conserva «%IVA» e «IVA: ¢115.04» | Pasa |
| 3.7 | 18 (emitir GENERAL de diciembre en enero) | Factura 14 (GENERAL, creada con el Clock en 2026-12-22); Clock en 2027-01-05; `procesar/14` | **409** `{"message":"Esta venta es del régimen general; su comprobante se emite en la herramienta de Hacienda y se registra aquí su clave"}`; sin comprobante ni consecutivo nuevos. Control: con el Clock en 2026-12-22, `procesar/13` no recibe ese 409 (la detiene otra validación, el cliente ficticio no requiere FE); con el Clock en 2027-01-05 sí lo recibe | Pasa |
| 3.8 | 18 (aviso de cierre) | Panel con el Clock en 12-19, 12-20, 12-22, 12-31 y 01-01 | 12-19: ausente. 12-20, 12-22 y 12-31: visible. 01-01: ausente. Con el Clock en 12-22: cantidad 3 antes de crear las facturas 14 y 15 (la 13 y las facturas 4, 5 y 10 del volcado, cuyo comprobante vigente está RECHAZADO; una RECHAZADO no es comprobante válido) y 5 después; enlace `/facturas?estadoFE=pendiente_cierre&desdeCierre=2026-09-01&hastaCierre=2026-12-22` | Pasa |
| 3.9 | 18 (clave externa) | `PUT /api/facturas/15/comprobante-externo` con una clave ficticia de 50 dígitos y fecha 2026-12-22 | 200 «Clave del comprobante externo registrada»; el aviso baja de 5 a 4 y «Facturas sin comprobante» de 6 a 5; el log registra «Clave de comprobante externo registrada en la factura FA-00038» (auditoría); el PDF de la factura 15 muestra «Comprobante emitido fuera del ERP — Clave:» seguido de la clave completa de 50 dígitos. `procesar/15` con el Clock en enero: **409** «Esta venta ya tiene un comprobante emitido fuera del ERP; no se emite otro desde el sistema» | Pasa |

Panel con el Clock en 2027-01-05 tras todo lo anterior: «Facturas sin comprobante» = 5: las facturas 1, 2 y 3 del volcado (julio) más la 13 y la 14; no cuenta la 15 (clave externa) ni la 16 (SIMPLIFICADO).

### Asientos del smoke (cuenta contable, debe / haber)

| Factura | Régimen | Asiento | 1.1.02.001 CxC | 4.1.01.001 Ingresos | 2.1.02.001 IVA por pagar |
|---|---|---|---|---|---|
| 13 (control) | GENERAL | AUTOMATICO_VENTA, CONTABILIZADO | debe 1.000,00 | haber 884,96 | haber 115,04 |
| 16 | SIMPLIFICADO | AUTOMATICO_VENTA, CONTABILIZADO | debe 1.000,00 | haber 1.000,00 | sin línea |

## 4. Fuera de lo probado

- Emisión exitosa de una factura GENERAL antes del cierre contra Hacienda: la salida a Hacienda está cortada por el protocolo (solo se comprobó que la regla de frontera no la bloquea antes del cierre).
- Editar las facturas de julio selladas GENERAL (ver 1.4).

## 5. Limpieza

Contenedor `t057-mysql` eliminado por su nombre exacto, procesos de la aplicación detenidos y directorio temporal del smoke borrado; el estado de Docker queda igual al inicial.
