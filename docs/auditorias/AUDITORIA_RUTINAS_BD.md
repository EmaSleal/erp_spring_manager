# Auditoría de rutinas de base de datos: procedimientos, funciones y triggers (T-023)

Insumo para el ADR que resolverá P-009 (requisitos de despliegue: CREATE ROUTINE para Flyway y rutinas fuera de Flyway), P-038 (los respaldos no incluyen las rutinas) y, en parte, P-017 (Flyway no puede construir la BD desde cero). Corresponde al bloque «Base operable» de ADR-005.

Es una tarea de solo lectura: no se modificó código, migraciones, rutinas ni datos del repositorio. Este documento no decide nada; la decisión va en un ADR posterior.

## Fuente y método

| Elemento | Valor |
|---|---|
| Código analizado | rama `t-023-auditoria-de-procedimientos-almacenados-`, base `prod` `b8ca984` (incluye V35) |
| Rutinas del repositorio | `database/routines/stored_procedures.sql` (1360 líneas) y `database/routines/triggers.sql` (162 líneas) |
| Datos | dump local `facturas_monrachem_20261011_173406.sql` (copia de producción del 2026-10-11, `flyway_schema_history` en V18) |
| Motor | contenedor aislado `t023-mysql` (MySQL 8.0.32, `--tmpfs /var/lib/mysql`, puerto solo en `127.0.0.1:13323`, `log_bin=1`, `log_bin_trust_function_creators=0`), eliminado al terminar |
| Base creada con | `CREATE DATABASE facturas_monrachem` (sin intercalación explícita) |
| Usuario de aplicación | `t023app@'%'` con `GRANT ALL PRIVILEGES ON facturas_monrachem.*` (sin SUPER ni privilegios globales), para medir privilegios reales |
| Migraciones aplicadas | V19..V35 arrancando el jar construido (`./mvnw -o package -DskipTests` en una copia sin `.env`), con `env -i`, DNS bloqueado (`-Djdk.net.hosts.file` vacío), `TIPO_CAMBIO_SYNC_ENABLED=false`, conectado como `t023app`. Resultado: `Successfully applied 17 migrations ... now at version v35`; la aplicación arrancó (`Started WhatsOrdersManagerApplication`) |
| Rutinas cargadas | `stored_procedures.sql` como `t023app` (sin errores); `triggers.sql` como `t023app` (falla, ver §6) y luego como `root` |
| Datos agregados (solo en el contenedor) | 1 usuario, 1 presentación, 2 productos, 1 cliente y 3 facturas, todos sintéticos con nombre `T023 ...` e importes redondos |
| Producción | no se usó `.env` ni se conectó a producción |

Abreviaturas de rutas (Java bajo `src/main/java/api/astro/whats_orders_manager/modules/`, estáticos bajo `src/main/resources/static/modules/`):

| Abreviatura | Archivo |
|---|---|
| `SP` | `database/routines/stored_procedures.sql` |
| `TRG` | `database/routines/triggers.sql` |
| `CliR` | `cliente/repository/ClienteRepository.java` |
| `CER` | `facturacion/electronica/repository/ComprobanteElectronicoRepository.java` |
| `CES` | `facturacion/electronica/service/impl/ComprobanteElectronicoServiceImpl.java` |
| `CEC` | `facturacion/electronica/controller/ComprobanteElectronicoController.java` |
| `FR` | `facturacion/repository/FacturaRepository.java` |
| `FS` | `facturacion/service/impl/FacturaServiceImpl.java` |
| `F` | `facturacion/model/Factura.java` |
| `LFR` | `facturacion/repository/LineaFacturaRepository.java` |
| `LFS` | `facturacion/service/impl/LineaFacturaServiceImpl.java` |
| `LFC` | `facturacion/controller/LineaFacturaController.java` |
| `CIS` | `facturacion/service/CalculadoraImpuestoService.java` |
| `PR` | `producto/repository/ProductoRepository.java` |
| `PS` | `producto/service/impl/ProductoServiceImpl.java` |
| `PC` | `producto/controller/ProductoController.java` |
| `AMS` | `producto/service/impl/ArticuloMaestroServiceImpl.java` |
| `RC` | `reportes/controller/ReporteController.java` |
| `RS` | `reportes/service/impl/ReporteServiceImpl.java` |
| `PNS` | `notificacion/service/impl/PreferenciaNotificacionServiceImpl.java` |
| `NRC` | `notificacion/controller/NotificacionRestController.java` |
| `EFjs` | `facturacion/js/editar-factura.js` |
| `REPjs` | `reportes/js/reportes.js` |
| `CEhtml` | `src/main/resources/templates/modules/facturacion/electronica/comprobantes.html` |

---

## Resumen

- **Fuentes**: el repositorio define **33 procedimientos** y **2 triggers** (0 funciones). El código Java referencia **18 nombres** (15 `CALL` nativos y 2 `@Procedure`, más el comentario de una de ellas); **3 de esos nombres no existen** en ninguna fuente (`sp_obtener_top_clientes`, `sp_obtener_ventas_por_dia`, `sp_obtener_estadisticas_ventas`). Flyway solo crea y borra **procedimientos auxiliares temporales** (V19, V29–V32, V35); ninguna migración crea rutinas de la aplicación. El dump del 2026-10-11 contiene los **2 triggers** y **ningún procedimiento**.
- **Vivas de verdad**: 11 rutinas llegan a ejecutarse desde una pantalla o endpoint (10 llamadas desde Java y `ActualizarTotalFactura`, que se llama desde otra rutina), más el trigger de alta de usuario, que llama a `sp_crear_preferencias_usuario`. Las demás son código muerto. Además, `sp_obtener_comprobante_por_clave` y `sp_obtener_comprobante_por_factura` **nunca se ejecutan**: Spring Data busca procedimientos llamados `findByClaveNumerica` y `findByFacturaId` y los endpoints responden 500 (§3.3, verificado).
- **Rutinas rotas**: 7 procedimientos fallan al ejecutarse sobre el esquema actual (columnas inexistentes, argumentos incorrectos, `ONLY_FULL_GROUP_BY`, `NOT NULL`) y otros 2 devuelven resultados erróneos sin fallar (`InsertarPresentacion`, `SP_ESTADISTICAS_DASHBOARD`). Los 9 son código muerto.
- **Ruta de factura**: (a) sí hay dos cálculos de total; (b) ninguna ruta persiste hoy un total de rutina, porque Java lo sobrescribe en la misma transacción, pero **se reprodujo** una factura cuyo total persistido no coincide con sus líneas (quitar una línea en la UI); (c) ninguna prueba ejecuta la rutina (se simula con mocks y H2).
- **Moneda (ADR-011)**: `sp_obtener_ventas_por_mes` suma `factura.total` sin convertir a CRC y excluye las facturas del día. Ambas cosas se **reprodujeron** (§4.4).
- **Privilegios**: los procedimientos se crean con `CREATE ROUTINE`; **los triggers fallan sin SUPER** cuando hay binlog (`ERROR 1419`), y `triggers.sql` ejecutado sin SUPER **borra los triggers existentes y no los recrea** (§6).

---

## 1. Inventario por fuente

### 1(a) `database/routines/*.sql`

`SP` es un `mysqldump` del servidor de producción (MySQL 8.0.46, 2026-06-08) sin cláusulas `DEFINER`. `TRG` proviene del mismo origen; su `DEFINER` también se quitó (`/*!50017 */` vacío).

| # | Rutina | Tipo | `SP`/`TRG`:línea |
|---|---|---|---|
| 1 | `ActualizarCliente` | PROCEDURE | `SP:31` |
| 2 | `ActualizarTotalFactura` | PROCEDURE | `SP:61` |
| 3 | `CrearFactura` | PROCEDURE | `SP:88` |
| 4 | `InsertarCliente` | PROCEDURE | `SP:116` |
| 5 | `InsertarLineaFactura` | PROCEDURE | `SP:154` |
| 6 | `InsertarPresentacion` | PROCEDURE | `SP:184` |
| 7 | `InsertarProducto` | PROCEDURE | `SP:213` |
| 8 | `InsertarUsuario` | PROCEDURE | `SP:263` |
| 9 | `ObtenerClientes` | PROCEDURE | `SP:291` |
| 10 | `ObtenerFacturaCompleta` | PROCEDURE | `SP:310` |
| 11 | `ObtenerHistorialMensajes` | PROCEDURE | `SP:363` |
| 12 | `ObtenerProductos` | PROCEDURE | `SP:389` |
| 13 | `ObtenerReportePorArticulo` | PROCEDURE | `SP:410` |
| 14 | `RegistrarWebhook` | PROCEDURE | `SP:442` |
| 15 | `sp_actualizar_linea_factura` | PROCEDURE | `SP:476` |
| 16 | `SP_CLIENTES_NUEVOS_POR_MES` | PROCEDURE | `SP:550` |
| 17 | `sp_contar_comprobantes_por_estado` | PROCEDURE | `SP:588` |
| 18 | `sp_crear_preferencias_usuario` | PROCEDURE | `SP:613` |
| 19 | `sp_desactivar_producto` | PROCEDURE | `SP:825` |
| 20 | `SP_ESTADISTICAS_DASHBOARD` | PROCEDURE | `SP:848` |
| 21 | `sp_get_lineas_factura` | PROCEDURE | `SP:895` |
| 22 | `sp_listar_comprobantes_pendientes` | PROCEDURE | `SP:920` |
| 23 | `sp_listar_comprobantes_por_empresa` | PROCEDURE | `SP:956` |
| 24 | `sp_listar_comprobantes_por_estado` | PROCEDURE | `SP:1000` |
| 25 | `sp_listar_comprobantes_por_fechas` | PROCEDURE | `SP:1046` |
| 26 | `sp_obtener_clientes_nuevos_por_mes` | PROCEDURE | `SP:1093` |
| 27 | `sp_obtener_comprobante_por_clave` | PROCEDURE | `SP:1123` |
| 28 | `sp_obtener_comprobante_por_factura` | PROCEDURE | `SP:1153` |
| 29 | `sp_obtener_productos_mas_vendidos` | PROCEDURE | `SP:1183` |
| 30 | `sp_obtener_ventas_por_mes` | PROCEDURE | `SP:1212` |
| 31 | `SP_PRODUCTOS_MAS_VENDIDOS` | PROCEDURE | `SP:1242` |
| 32 | `sp_resumen_comprobantes_empresa` | PROCEDURE | `SP:1280` |
| 33 | `SP_VENTAS_POR_CLIENTE_TOP` | PROCEDURE | `SP:1323` |
| T1 | `trg_after_insert_usuario` | TRIGGER AFTER INSERT ON `usuario` | `TRG:27` |
| T2 | `trg_after_update_usuario` | TRIGGER AFTER UPDATE ON `usuario` | `TRG:49` |

Total: 33 procedimientos, 0 funciones, 2 triggers, 0 eventos.

### 1(b) Referencias en `src/main`

Comandos ejecutados desde la raíz del repositorio en `b8ca984`:

```sh
# 1. Barrido amplio, sin distinguir mayúsculas (incluye falsos positivos)
rg -n -i '\bcall\b' src/main                         # 111 líneas en 15 archivos

# 2. Invocación SQL de una rutina: CALL/call seguido de identificador y paréntesis
rg -n -i '\bcall\s+[A-Za-z_][A-Za-z0-9_]*\s*\(' src/main
#    106 líneas = 15 en src/main/java + 91 en src/main/resources/db/migration + 0 en el resto

# 3. APIs de JPA/JDBC para procedimientos
rg -n '@Procedure|NamedStoredProcedureQuer|createStoredProcedureQuery|StoredProcedureQuery|SimpleJdbcCall|prepareCall|CallableStatement' src/main
#    2 líneas (los dos @Procedure de CER); 0 @NamedStoredProcedureQuery, 0 SimpleJdbcCall, 0 JdbcTemplate.call
```

Conciliación del barrido amplio (111 líneas):

| Grupo | Líneas | Tratamiento |
|---|---|---|
| `CALL` a procedimientos auxiliares en migraciones V19, V29, V30, V31, V32 y V35 | 91 | Rutinas temporales de Flyway; se tratan en 1(c) |
| `CALL`/`call` a rutinas desde repositorios Java | 15 | Coincidencias verdaderas; todas en la tabla siguiente |
| `PR:19` `//call the sp ObtenerProductos()` | 1 | Comentario de la coincidencia `PR:21`; no es una invocación |
| `seguridad/dto/UsuarioAdminDTO.java:94` («to call EmpleadoService...») | 1 | Falso positivo: palabra inglesa en Javadoc |
| `[].slice.call(...)` en `shared/js/common.js:637`, `seguridad/js/usuarios-admin.js:536`, `presentacion/js/dashboard.js:348` | 3 | Falsos positivos: `Function.prototype.call` de JavaScript |
| **Total** | **111** | |

También se buscó cada uno de los 33 nombres de `SP`, los 2 triggers y los 3 nombres sin definición en `src/main/java` y `src/main/resources` (sin migraciones) con `rg -w -F`. Solo aparecen los nombres de la tabla siguiente. En `src/test` no aparece ninguno.

Coincidencias verdaderas (17 = 15 `CALL` + 2 `@Procedure`; 18 nombres):

| Rutina | Referencia | Mecanismo | ¿Existe en `SP`? |
|---|---|---|---|
| `sp_obtener_clientes_nuevos_por_mes` | `CliR:20` | `@Query` nativa `CALL` | sí |
| `sp_obtener_top_clientes` | `CliR:28` | `@Query` nativa `CALL` | **no** |
| `sp_obtener_comprobante_por_clave` | `CER:32` | `@Procedure(name=...)` | sí |
| `sp_obtener_comprobante_por_factura` | `CER:38` | `@Procedure(name=...)` | sí |
| `sp_listar_comprobantes_por_empresa` | `CER:44` | `@Query` nativa `CALL` | sí |
| `sp_listar_comprobantes_por_estado` | `CER:54` | `@Query` nativa `CALL` | sí |
| `sp_listar_comprobantes_pendientes` | `CER:65` | `@Query` nativa `CALL` | sí |
| `sp_listar_comprobantes_por_fechas` | `CER:71` | `@Query` nativa `CALL` | sí |
| `sp_contar_comprobantes_por_estado` | `CER:83` | `@Query` nativa `CALL` | sí |
| `sp_obtener_ventas_por_mes` | `FR:69` | `@Query` nativa `CALL` | sí |
| `sp_obtener_ventas_por_dia` | `FR:79` | `@Query` nativa `CALL` | **no** |
| `sp_obtener_estadisticas_ventas` | `FR:92` | `@Query` nativa `CALL` | **no** |
| `sp_get_lineas_factura` | `LFR:28` | `@Query` nativa `call` | sí |
| `sp_actualizar_linea_factura` | `LFR:42` | `@Modifying @Query` nativa `call` | sí |
| `ObtenerProductos` | `PR:21` (comentario en `PR:19`) | `@Query` nativa `call` | sí |
| `sp_desactivar_producto` | `PR:26` | `@Modifying @Query` nativa `call` | sí |
| `sp_obtener_productos_mas_vendidos` | `PR:34` | `@Query` nativa `CALL` | sí |

### 1(c) Migraciones Flyway

`rg -n -i '(CREATE|DROP)\s+(PROCEDURE|FUNCTION|TRIGGER|EVENT)' src/main/resources/db/migration` devuelve 48 líneas en 6 archivos. Todas siguen el mismo patrón: `DROP ... IF EXISTS`, `CREATE PROCEDURE`, `CALL` y `DROP` al final del mismo script.

| Migración | Procedimientos auxiliares (creados y borrados en el mismo script) |
|---|---|
| V19 | `v19_asegurar_parametro_contable` |
| V29 | `v29_fallar`, `v29_validar_cuenta_existente`, `v29_asegurar_cuenta`, `v29_plan_de_cuentas_coherente` |
| V30 | `v30_fallar`, `v30_validar`, `v30_agregar_columna`, `v30_agregar_restriccion`, `v30_cuentas_bancarias_contables` |
| V31 | `v31_fallar`, `v31_empresa_unica` |
| V32 | `v32_fallar`, `v32_identidad_fiscal_unica` |
| V35 | `v35_fallar`, `v35_configuracion_email_fila_unica` |

Ninguna migración crea, modifica ni borra las 33 rutinas de `SP` ni los 2 triggers. Tras aplicar V19..V35 en `t023-mysql`, `information_schema.ROUTINES` quedó vacío: los auxiliares no persisten. Su creación sí exige `CREATE ROUTINE` y `ALTER ROUTINE` al usuario de Flyway (§6).

### 1(d) Producción

No se accedió a producción. Las consultas que debe ejecutar el usuario están en §2. Mientras no haya resultado, el estado de producción de cada rutina es «sin verificar».

Lo único observable de producción es el dump del 2026-10-11:

| Objeto en el dump | Cantidad | Detalle |
|---|---|---|
| Procedimientos y funciones | 0 | El dump no tiene sección «Dumping routines»: se generó sin `--routines`. No prueba que producción no tenga rutinas, pero sí confirma P-038 para ese respaldo |
| Triggers | 2 | `trg_after_insert_usuario` y `trg_after_update_usuario`, ambos con `DEFINER=root@%` (`mysqldump` incluye triggers por defecto) |

Consecuencia observada: el dump trae un trigger que llama a `sp_crear_preferencias_usuario` y no trae ese procedimiento. Restaurado tal cual, **cualquier alta de usuario activo falla** (ver §5, reproducido).

`database/scripts/backup.sh` ya usa `--routines --triggers --events` (ver `database/README.md`); el dump auditado es anterior a ese cambio o se generó por otra vía.

---

## 2. Comparación con producción por nombre y hash

### 2.1 Consultas que debe ejecutar el usuario en producción

Solo leen metadatos. Se deben ejecutar con un usuario que pueda ver el cuerpo de las rutinas (el `DEFINER`, o uno con `SHOW_ROUTINE` o `SELECT` global): con otro usuario, `ROUTINE_DEFINITION` vuelve `NULL` y el hash sale `NULL`.

```sql
SELECT ROUTINE_TYPE, ROUTINE_NAME, DEFINER, LAST_ALTERED, SHA2(ROUTINE_DEFINITION,256) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() ORDER BY 2;
SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE, ACTION_TIMING, SHA2(ACTION_STATEMENT,256) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() ORDER BY 1;
```

No ejecutar `SHOW CREATE` de forma masiva ni consultar datos de tablas.

### 2.2 Resultado de producción (pendiente de que el usuario lo pegue)

> **PEGAR AQUÍ** la salida de las dos consultas de 2.1 ejecutadas en producción, con la fecha y el usuario de BD que las ejecutó.
>
> ```text
> (sin resultado al cierre de T-023)
> ```

### 2.3 Hashes locales y estado

Mismas consultas ejecutadas en `t023-mysql` después de cargar `SP` y `TRG` sobre el esquema V35. El hash se calcula sobre el cuerpo y no depende del `DEFINER` ni de `LAST_ALTERED`; en local el `DEFINER` es `t023app@%` (procedimientos) o `root@%` (triggers). Para comparar, buscar el nombre en 2.2 y marcar: **idéntica** (mismo hash), **distinta** (otro hash), **solo repo** (no aparece en producción) o **solo prod** (aparece en producción y no en esta tabla).

| Rutina | Tipo | SHA-256 del cuerpo (repo cargado en `t023-mysql`) | Dump 2026-10-11 | Producción |
|---|---|---|---|---|
| `ActualizarCliente` | PROCEDURE | `39f2ce92dd83248f6e659a0cfb42bcd5ac6058504855ea53af02cbb0ffb9f395` | ausente | sin verificar |
| `ActualizarTotalFactura` | PROCEDURE | `759a88f51b10d446171a830f56568d38087de175b4ae9acb2130205cb6215510` | ausente | sin verificar |
| `CrearFactura` | PROCEDURE | `5e3ac341bdc00a4e284ae24a798ffbce7c0a45a94ebb4552a89f884fba1b57f2` | ausente | sin verificar |
| `InsertarCliente` | PROCEDURE | `c6c4641d14c4c8acd41889822caa22b91413f3135cf423a06af57bb70b853fc1` | ausente | sin verificar |
| `InsertarLineaFactura` | PROCEDURE | `598e84b4d85d7b9bbe9777757ff6f51670cc13c15f778c03938824f0c9e42abb` | ausente | sin verificar |
| `InsertarPresentacion` | PROCEDURE | `2379d2678182c722710a61a0a7536ca27b6fdf0ffda31958f161b062b7142ab6` | ausente | sin verificar |
| `InsertarProducto` | PROCEDURE | `6190d6bb4120414b7edbbf1230865e8af7896547efc15d10cd710cefafe77f9f` | ausente | sin verificar |
| `InsertarUsuario` | PROCEDURE | `34bf8f956260d42fd6587415da3cb057d26f62f8f903a2d71569406c4ee72f1c` | ausente | sin verificar |
| `ObtenerClientes` | PROCEDURE | `8669d8b7d78ee61faa10f61b4b2502f3a49938bb142ed7c2329a0d29b79b0905` | ausente | sin verificar |
| `ObtenerFacturaCompleta` | PROCEDURE | `9d0e23c19e6445f8e67e983661de47564d660c067d6206250c04ae58d75f80c6` | ausente | sin verificar |
| `ObtenerHistorialMensajes` | PROCEDURE | `961e8e133d4c23e89ec56d5bbe90a91befd24b9cb02713d5ca31f294cff6d8e2` | ausente | sin verificar |
| `ObtenerProductos` | PROCEDURE | `732cd7da67ee21267bb43784242e50aef7dfacb9cc3ee6fcff416d17773abfb8` | ausente | sin verificar |
| `ObtenerReportePorArticulo` | PROCEDURE | `2466c9295d045953a20d292bb3ae53f188c5ec30b83f83157b881e609c8e6c88` | ausente | sin verificar |
| `RegistrarWebhook` | PROCEDURE | `0df1a949ee5a03a357c8bacfdf0e054b4dab84651562d4aeda7d62c34584afc2` | ausente | sin verificar |
| `sp_actualizar_linea_factura` | PROCEDURE | `df70c082e1c58348df649ba89efcd543efcd12045bbcef4af6deed7231c7af53` | ausente | sin verificar |
| `SP_CLIENTES_NUEVOS_POR_MES` | PROCEDURE | `aa73c361420d28fc974ac99cd7931f45e6d2d18322e45596f6d2e9639f6c046c` | ausente | sin verificar |
| `sp_contar_comprobantes_por_estado` | PROCEDURE | `174009f32a3c7d234d44742111ffd877ca1c302c3d440c5f8b958a4adf054e41` | ausente | sin verificar |
| `sp_crear_preferencias_usuario` | PROCEDURE | `4f21e16883df0e629130a79dc1675d2a43c4adf4032260f05de54e2db426c682` | ausente | sin verificar |
| `sp_desactivar_producto` | PROCEDURE | `743fa94aae54b130e8756e8f1bc2096ca17849e2bc28cf44c55f4338efaaafc0` | ausente | sin verificar |
| `SP_ESTADISTICAS_DASHBOARD` | PROCEDURE | `3fcb5aa36729ec21860deaeded4f865f54405e89b7a7fe3fa090b1576cb6509d` | ausente | sin verificar |
| `sp_get_lineas_factura` | PROCEDURE | `53161a7fca80fddac34071401f09c9c650cb1a8935cc67c5ac81328a60e787d8` | ausente | sin verificar |
| `sp_listar_comprobantes_pendientes` | PROCEDURE | `2125f86962a885511b61559e7b90b9728cc69c256285d867eeb5687215454672` | ausente | sin verificar |
| `sp_listar_comprobantes_por_empresa` | PROCEDURE | `15aae6043f19109bfac58025d1121f541511ded57a6bf75a53bbe96cc6e65210` | ausente | sin verificar |
| `sp_listar_comprobantes_por_estado` | PROCEDURE | `7cbf6fa8f633d0cb4b31adbaae69bd96fb9cd8b1c67f6de627e508439c4042fb` | ausente | sin verificar |
| `sp_listar_comprobantes_por_fechas` | PROCEDURE | `6e7b08512ed03a76666d606cb70049fd4a44c721882b2bdf8fc11c80be4cc862` | ausente | sin verificar |
| `sp_obtener_clientes_nuevos_por_mes` | PROCEDURE | `a251492ad3e266c91db6aaad8227b7730ded4e7d59082f4346811b5663ea0038` | ausente | sin verificar |
| `sp_obtener_comprobante_por_clave` | PROCEDURE | `e2c135c5bd3c9eed65cb0cb75a8e2860bc832c0a94b854cfbb4bfa8d7b2ca628` | ausente | sin verificar |
| `sp_obtener_comprobante_por_factura` | PROCEDURE | `ebd9f8aafa8ccbfc991d15b4c1f0818a527c3a8414e8a2faed13d6e8b9d12945` | ausente | sin verificar |
| `sp_obtener_productos_mas_vendidos` | PROCEDURE | `e9c48d512873a63a83ab90a6d1e27a9fba8bd5ebf3c8a13eba3a986e4e8f2149` | ausente | sin verificar |
| `sp_obtener_ventas_por_mes` | PROCEDURE | `73df668a8889405c704fb1b0e1d23dbe93cd003d85d57b477eabc0c56428c969` | ausente | sin verificar |
| `SP_PRODUCTOS_MAS_VENDIDOS` | PROCEDURE | `883b589ec5c021fc88664185b1c8a966121710965aeab6a409b911372b97ace3` | ausente | sin verificar |
| `sp_resumen_comprobantes_empresa` | PROCEDURE | `adbb619129618e985c4239d3e3c3e58d882c55a4077ebbeaeea6f6709e014418` | ausente | sin verificar |
| `SP_VENTAS_POR_CLIENTE_TOP` | PROCEDURE | `c87e70ec254e38404b092d61dff874f47da2eb1d7bc5812aa2517fe23d19667d` | ausente | sin verificar |
| `sp_obtener_top_clientes` | PROCEDURE (solo referenciada en Java) | no aplica (no existe en el repo) | ausente | sin verificar |
| `sp_obtener_ventas_por_dia` | PROCEDURE (solo referenciada en Java) | no aplica (no existe en el repo) | ausente | sin verificar |
| `sp_obtener_estadisticas_ventas` | PROCEDURE (solo referenciada en Java) | no aplica (no existe en el repo) | ausente | sin verificar |
| `trg_after_insert_usuario` | TRIGGER AFTER INSERT `usuario` | `c0379306fb0a5921907c95e36d976bc44fcc53c0bbb25db72d00e1e8e8736dba` | **idéntica** (mismo hash, `DEFINER=root@%`) | sin verificar |
| `trg_after_update_usuario` | TRIGGER AFTER UPDATE `usuario` | `0b80b4540e32dd153550407f20dbe6eb7dbeaeabac26d25c19fbe33edc155236` | **idéntica** (mismo hash, `DEFINER=root@%`) | sin verificar |

---

## 3. Análisis por rutina y trigger

### 3.1 Prueba de ejecución de cada rutina

Cada procedimiento se ejecutó una vez como `t023app`, con argumentos sintéticos y dentro de una transacción revertida (`ROLLBACK`). Los errores marcados como «por argumento» se debieron a ids inexistentes elegidos a propósito y no indican un defecto.

| Rutina | Resultado en el esquema V35 |
|---|---|
| `ActualizarCliente` | **ERROR 1054** `Unknown column 'id_usuario' in 'field list'` (la tabla `cliente` no tiene `id_usuario`) |
| `InsertarCliente` | **ERROR 1054** `Unknown column 'id_usuario'` |
| `InsertarLineaFactura` | **ERROR 1318** `Incorrect number of arguments for PROCEDURE ActualizarTotalFactura; expected 1, got 0` (con una factura sintética válida) |
| `InsertarPresentacion` | No falla, pero el parámetro `OUT` vuelve `NULL` en un alta nueva: `SELECT pIdPresentacion = LAST_INSERT_ID()` compara en vez de asignar (`SP:194`) |
| `InsertarUsuario` | **ERROR 1364** `Field 'email' doesn't have a default value` |
| `ObtenerFacturaCompleta` | **ERROR 1054** `Unknown column 'c.id_usuario'` |
| `SP_CLIENTES_NUEVOS_POR_MES` | **ERROR 1055** incompatible con `sql_mode=only_full_group_by` (el modo se guarda con la rutina) |
| `SP_PRODUCTOS_MAS_VENDIDOS` | **ERROR 1054** `Unknown column 'lf.factura_id'` |
| `SP_ESTADISTICAS_DASHBOARD` | No falla, pero compara `tipo_factura = 'PENDIENTE'` contra una columna `tinyint` (resultado sin sentido) y suma `total` sin convertir la moneda |
| `sp_obtener_top_clientes`, `sp_obtener_ventas_por_dia`, `sp_obtener_estadisticas_ventas` | **ERROR 1305** `PROCEDURE ... does not exist` |
| Resto (`ActualizarTotalFactura`, `CrearFactura`, `InsertarProducto`, `ObtenerClientes`, `ObtenerHistorialMensajes`, `ObtenerProductos`, `ObtenerReportePorArticulo`, `RegistrarWebhook`, `sp_actualizar_linea_factura`, `sp_contar_comprobantes_por_estado`, `sp_crear_preferencias_usuario`, `sp_desactivar_producto`, `sp_get_lineas_factura`, `sp_listar_comprobantes_*`, `sp_obtener_clientes_nuevos_por_mes`, `sp_obtener_comprobante_por_*`, `sp_obtener_productos_mas_vendidos`, `sp_obtener_ventas_por_mes`, `sp_resumen_comprobantes_empresa`, `SP_VENTAS_POR_CLIENTE_TOP`) | Se ejecutan sin error de esquema (en dos casos el error fue por argumento: una FK con id inexistente) |

### 3.2 Endpoints probados con la aplicación en marcha

Con la aplicación V35 conectada a `t023-mysql` y un usuario sintético con rol ADMIN:

| Endpoint | HTTP | Observación |
|---|---|---|
| `GET /reportes/api/ventas-por-mes`, `/clientes-nuevos`, `/productos-mas-vendidos` | 200 | Ejecutan sus rutinas |
| `GET /productos/records` | 200 | `ObtenerProductos` |
| `GET /lineas-factura/detalle/{id}` | 200 | Usa JPA (`findEntitiesByFacturaId`), no `sp_get_lineas_factura` |
| `GET /api/facturas/electronica/comprobantes/empresa/1`, `/estado/ACEPTADO`, `/fechas`, `/estadisticas`, `/pendientes` | 200 | Ejecutan sus rutinas (en el log general de MySQL aparece, p. ej., `CALL sp_listar_comprobantes_por_fechas(1, '2026-01-01 00:00:00', ...)`) |
| `GET /api/facturas/electronica/comprobantes/clave/{clave}` | **500** | Ver 3.3 |
| `GET /api/facturas/electronica/comprobantes/factura/{id}` | **500** | Ver 3.3 |
| `PUT /lineas-factura/actualizar` | 200 | `sp_actualizar_linea_factura`; ver §4 |

### 3.3 Los dos `@Procedure` nunca ejecutan su rutina

`CER:32` y `CER:38` usan `@Procedure(name = "sp_obtener_comprobante_por_...")`. En Spring Data JPA 3.5.0, `name` se refiere a una `@NamedStoredProcedureQuery` de la entidad, y el código no tiene ninguna (búsqueda 1(b), comando 3). Sin ella, `StoredProcedureAttributeSource.deriveProcedureNameFrom` toma `value`, luego `procedureName` y, si los dos están vacíos, **el nombre del método Java** (verificado con `javap` sobre `spring-data-jpa-3.5.0.jar`). El log general de MySQL lo confirma: el driver consulta

```text
SELECT SQL_DATA_ACCESS FROM information_schema.routines WHERE routine_schema = 'facturas_monrachem' AND routine_name = 'findByClaveNumerica'
SELECT SQL_DATA_ACCESS FROM information_schema.routines WHERE routine_schema = 'facturas_monrachem' AND routine_name = 'findByFacturaId'
```

y la llamada termina en `JpaSystemException: Error calling CallableStatement.getMoreResults [Connection is read-only. Queries leading to data modification are not allowed]`, porque los métodos de `CES:222-231` son `@Transactional(readOnly = true)` y el procedimiento `findBy...` no existe. Aunque se corrigiera el nombre, ambas rutinas hacen `INNER JOIN empresa e ON e.id_empresa = ce.empresa_id` (`SP:1133`, `SP:1163`), pero la FK real de `comprobante_electronico.empresa_id` apunta a `configuracion_empresa` (`FK69tpcsorqvmn2i3a920vi4m6o`). El parámetro Java `p_factura_id` (`CER:39`) tampoco coincide con el de la rutina (`p_id_factura`, `SP:1154`).

### 3.4 Tabla por rutina

Columnas: **Llamador** = archivo:línea → servicio → endpoint o pantalla. **L/E** = lee o escribe (tablas). **Montos** = si calcula totales, impuestos o saldos. **Duplica Java** = método Java equivalente. **Muerta** = sin llamador efectivo en `src/main`. **Riesgo ADR-009/011** = efecto sobre IVA incluido con tarifa por producto y sobre multimoneda/CRC. Las clasificaciones son propuestas; la decisión es del ADR.

| # | Rutina | Tipo | Llamador | L/E (tablas) | Montos | Duplica Java | Muerta | Riesgo ADR-009 / ADR-011 | Clasificación propuesta |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `ActualizarCliente` | PROC | ninguno | E: `cliente` | no | `ClienteRepository.save` (JPA) | sí; además falla (1054) | no aplica | ELIMINAR |
| 2 | `ActualizarTotalFactura` | PROC | solo desde rutinas: `sp_actualizar_linea_factura` (`SP:506`, `SP:532`) e `InsertarLineaFactura` (`SP:167`, mal invocada) | L: `linea_factura`; E: `factura.total` | **sí**: `total = SUM(subtotal)`; no toca `subtotal` ni `igv` | `LFS:96-122` y `FS:229-243` (totales ADR-009 con `CIS:59-77`) | no (indirecta vía #15) | ADR-009: deja `subtotal`/`igv` desalineados con `total` si no lo corrige Java (§4). ADR-011: suma en la moneda de la factura, sin conversión (correcto mientras todas las líneas estén en esa moneda) | REEMPLAZAR EN JAVA (ya existe; basta quitarla) |
| 3 | `CrearFactura` | PROC | ninguno | E: `factura` | crea con `total = 0` | `FS.save` | sí | ignora moneda, tipo de cambio e IVA | ELIMINAR |
| 4 | `InsertarCliente` | PROC | ninguno | L/E: `cliente` | no | `ClienteRepository.save` | sí; falla (1054) | no aplica | ELIMINAR |
| 5 | `InsertarLineaFactura` | PROC | ninguno | E: `linea_factura`, `factura` | **sí**: `subtotal = cantidad × precio` | `LFS.updateLineas` | sí; falla (1318) | ignora IVA, tarifa y moneda | ELIMINAR |
| 6 | `InsertarPresentacion` | PROC | ninguno | L/E: `presentacion` | no | JPA `save` de `Presentacion` | sí; `OUT` siempre `NULL` en altas | no aplica | ELIMINAR |
| 7 | `InsertarProducto` | PROC | ninguno | L/E: `producto` | no (escribe precios recibidos) | `PS`/`AMS` (`save` de producto y artículo maestro) | sí | no fija `gravado`, `porcentaje_impuesto` ni `precio_incluye_iva` (ADR-010) | ELIMINAR |
| 8 | `InsertarUsuario` | PROC | ninguno | E: `usuario` (dispara T1) | no | `UsuarioServiceImpl:58` | sí; falla (1364) | no aplica | ELIMINAR |
| 9 | `ObtenerClientes` | PROC | ninguno | L: `cliente` | no | `CliR.findAllByOrderByNombreAsc` | sí | no aplica | ELIMINAR |
| 10 | `ObtenerFacturaCompleta` | PROC | ninguno | L: `factura`, `cliente`, `linea_factura`, `producto`, `presentacion` | no (lee `total`) | `FR.findByIdFactura` + `LFR.findEntitiesByFacturaId` | sí; falla (1054) | devuelve `total` sin moneda | ELIMINAR |
| 11 | `ObtenerHistorialMensajes` | PROC | ninguno | L: `webhooklogs` | no | no hay | sí | no aplica | ELIMINAR |
| 12 | `ObtenerProductos` | PROC | `PR:21` → `PS:46` → `PC:113-116` `GET /productos/records` → pantalla de factura (`EFjs:46`), `proveedores/ordenes-compra.html:310`, `proveedores/proveedor-form.html:198` | L: `producto`, `presentacion` | no (devuelve `precio_institucional`/`precio_mayorista`) | constructor `ProductoRecord(Producto)` con `PR.findAll` | no | lista también productos **inactivos** (sin filtro `active`); no expone `gravado`/tarifa (la tarifa la resuelve Java al guardar) | REEMPLAZAR EN JAVA |
| 13 | `ObtenerReportePorArticulo` | PROC | ninguno | L: `linea_factura`, `factura`, `producto`, `presentacion` | no (suma cantidades) | no hay | sí | no filtra facturas anuladas | ELIMINAR |
| 14 | `RegistrarWebhook` | PROC | ninguno | L/E: `webhooklogs` | no | no hay (el webhook actual no la usa) | sí | no aplica | ELIMINAR |
| 15 | `sp_actualizar_linea_factura` | PROC | `LFR:41-52` → `LFS.updateLineas:86-94` → `LFC:42-49` `PUT /lineas-factura/actualizar` → `EFjs:715` (`guardarLineas`, pantallas `facturacion/form.html` y modal de `facturacion/facturas.html`) | L/E: `linea_factura` (alta o modificación); llama a #2, que escribe `factura.total` | **sí**: dispara el recálculo de `total`; guarda el `subtotal` que envía el navegador sin recalcularlo | `LFS:96-122` (totales ADR-009); el alta o modificación de la línea equivale a `LineaFactura` + `save` | no | ver §4: el total se sobrescribe en Java; la rutina no aplica IVA, tarifa ni moneda | REEMPLAZAR EN JAVA |
| 16 | `SP_CLIENTES_NUEVOS_POR_MES` | PROC | ninguno | L: `cliente` | no | duplica #26 | sí; falla (1055) | no aplica | ELIMINAR |
| 17 | `sp_contar_comprobantes_por_estado` | PROC | `CER:83` → `CES.contarPorEstado:622` → `CEC:349-361` `GET .../empresa/{id}/estadisticas` → `CEhtml:228` | L: `comprobante_electronico` | no | consulta derivada `countByEmpresaIdConfiguracionAndEstado` | no | no aplica | REEMPLAZAR EN JAVA |
| 18 | `sp_crear_preferencias_usuario` | PROC | solo desde T1 (`TRG:31`) | L: `usuario`; E: `preferencia_notificacion` (6 filas; la primera con `tipo_notificacion` y `canal` `NULL`) | no | `PNS.crearPreferenciasPredeterminadas:311` (conjunto distinto de filas), hoy solo invocado a mano desde `NRC:373` y `NRC:438` | no (vía trigger) | no aplica | REEMPLAZAR EN JAVA (junto con T1) |
| 19 | `sp_desactivar_producto` | PROC | `PR:25-27` → `PS.deleteById:42` → `PC:106-111` `GET /productos/eliminar/{id}` (permiso `PRODUCTO_ELIMINAR`) | E: `producto.active` | no | `AMS:77-81` (`setActive(false)` sobre el maestro y sus variantes) | no | no aplica | REEMPLAZAR EN JAVA |
| 20 | `SP_ESTADISTICAS_DASHBOARD` | PROC | ninguno | L: `factura`, `cliente`, `producto`, `usuario` | **sí**: `SUM(total)` del mes | `RS` (estadísticas generales en Java) | sí | ADR-011: suma monedas mezcladas; incluye anuladas | ELIMINAR |
| 21 | `sp_get_lineas_factura` | PROC | `LFR:28-29` declara `findLineasByFacturaId`, pero **nadie lo llama**: `LFS:63-68` usa `findEntitiesByFacturaId` (JPQL) | L: `linea_factura`, `producto`, `presentacion` | no | `LFS.findLineasByFacturaId:63-68` | sí (solo declaración) | no aplica | ELIMINAR |
| 22 | `sp_listar_comprobantes_pendientes` | PROC | `CER:65` → `CES.obtenerPendientesEnvio:256`, que **no tiene llamador** (solo la interfaz) | L: `comprobante_electronico` | no | `CER.findPendientesReintento:107-111`, usado por `ComprobanteReintentosJob:47` (límite de intentos 5 frente a 3 de la rutina) | sí (en la práctica) | no aplica | ELIMINAR |
| 23 | `sp_listar_comprobantes_por_empresa` | PROC | `CER:44` → `CES.listarPorEmpresa:235` → `CEC:299-305` `GET .../empresa/{id}` y `CEC:403-418` `GET .../empresa/{id}/pendientes` → `CEhtml:241`, `CEhtml:381`, `CEhtml:421` | L: `comprobante_electronico`, `configuracion_empresa`, `factura` (LEFT JOIN sin uso) | no | consulta JPA paginada (`Pageable`) | no | no aplica | REEMPLAZAR EN JAVA |
| 24 | `sp_listar_comprobantes_por_estado` | PROC | `CER:54` → `CES.listarPorEstado:242` → `CEC:313-320`, exportación Excel `CEC:565`, PDF `CEC:627` → `CEhtml:246` | L: igual que #23 | no | consulta JPA paginada | no | no aplica | REEMPLAZAR EN JAVA |
| 25 | `sp_listar_comprobantes_por_fechas` | PROC | `CER:71` → `CES.listarPorRangoFechas:249` → `CEC:328-340`, tendencia `CEC:489`, exportación `CEC:572` y `CEC:634` → `CEhtml:244` | L: igual que #23 | no | consulta JPA paginada | no | no aplica | REEMPLAZAR EN JAVA |
| 26 | `sp_obtener_clientes_nuevos_por_mes` | PROC | `CliR:20` → `RC:642-673` `GET /reportes/api/clientes-nuevos` → `REPjs:173` (pantalla Reportes) | L: `cliente` | no | parcial: `RS:227` (solo el mes en curso) | no | excluye el día en curso (`create_date <= CURDATE()` sobre `datetime`) | VERSIONAR EN FLYWAY (agregación pura; corregir el límite de fecha) |
| 27 | `sp_obtener_comprobante_por_clave` | PROC | `CER:32` (`@Procedure`) → `CES:223-225` → `CEC:275-278` `GET .../clave/{clave}`; **nunca se ejecuta** (3.3) | L: `comprobante_electronico`, `empresa` (join incorrecto), `factura` | no | consulta derivada `findByClaveNumerica` (la entidad tiene `claveNumerica`) | sí, de hecho; el endpoint responde 500 | no aplica | ELIMINAR (y cambiar el método a consulta derivada) |
| 28 | `sp_obtener_comprobante_por_factura` | PROC | `CER:38` (`@Procedure`) → `CES:229-231` → `CEC:287-290` `GET .../factura/{id}`; **nunca se ejecuta** (3.3) | L: igual que #27 | no | consulta derivada sobre `factura.idFactura` | sí, de hecho; el endpoint responde 500 | no aplica | ELIMINAR (y cambiar el método a consulta derivada) |
| 29 | `sp_obtener_productos_mas_vendidos` | PROC | `PR:34` → `RC:680-712` `GET /reportes/api/productos-mas-vendidos` → `REPjs:282` | L: `producto`, `linea_factura`, `factura` | no (suma cantidades) | `RS.obtenerProductosMasVendidos:346` es un stub que devuelve una lista vacía | no | cuenta líneas de facturas anuladas (sin filtro `anulada`) | VERSIONAR EN FLYWAY (agregación pura; agregar el filtro de anuladas) |
| 30 | `sp_obtener_ventas_por_mes` | PROC | `FR:69` → `RC:604-635` `GET /reportes/api/ventas-por-mes` → `REPjs:49` (gráfico «Ventas por mes») | L: `factura` | **sí**: `SUM(f.total)` por mes | `RS.obtenerVentasPorMes:362-388` usa `F.getTotalEnCRC:214-220`, expuesto en `RC:797` `/reportes/api/comparativa-ingresos` | no | **ADR-011: suma USD/EUR como si fueran colones** (reproducido, §4.4); excluye el día en curso; incluye anuladas | REEMPLAZAR EN JAVA |
| 31 | `SP_PRODUCTOS_MAS_VENDIDOS` | PROC | ninguno | L: `producto`, `linea_factura`, `factura` | no | duplica #29 | sí; falla (1054) | no aplica | ELIMINAR |
| 32 | `sp_resumen_comprobantes_empresa` | PROC | ninguno | L: `comprobante_electronico` | no | `CES.contarPorEstado` | sí | no aplica | ELIMINAR |
| 33 | `SP_VENTAS_POR_CLIENTE_TOP` | PROC | ninguno | L: `cliente`, `factura` | **sí**: `SUM(f.total)` | no hay | sí | ADR-011: suma monedas mezcladas | ELIMINAR |
| 34 | `sp_obtener_top_clientes` | PROC (inexistente) | `CliR:28-29` `obtenerTopClientes` sin llamador | no aplica | sin verificar (no hay definición) | no aplica | sí | no aplica | ELIMINAR (la declaración del repositorio) |
| 35 | `sp_obtener_ventas_por_dia` | PROC (inexistente) | `FR:79-84` `obtenerVentasPorDia` sin llamador | no aplica | sin verificar (no hay definición) | no aplica | sí | no aplica | ELIMINAR (la declaración del repositorio) |
| 36 | `sp_obtener_estadisticas_ventas` | PROC (inexistente) | `FR:92-96` `obtenerEstadisticasVentas` sin llamador | no aplica | sin verificar (no hay definición) | no aplica | sí | no aplica | ELIMINAR (la declaración del repositorio) |
| T1 | `trg_after_insert_usuario` | TRIGGER AFTER INSERT `usuario` | todo `INSERT` en `usuario`: `UsuarioServiceImpl:58`, `BootstrapAdminInitializer:87`, `DataInitializer:59` (pantalla de usuarios y arranque) | E (vía #18): `preferencia_notificacion` | no | `PNS.crearPreferenciasPredeterminadas:311` | no | no aplica | REEMPLAZAR TRIGGER (crear las preferencias en Java al dar de alta el usuario) |
| T2 | `trg_after_update_usuario` | TRIGGER AFTER UPDATE `usuario` | todo `UPDATE` en `usuario` (`UsuarioServiceImpl`, `UserDetailsServiceImpl:65` en cada login, `LoginFailureHandler:76`, `AuthController:124`); solo inserta cuando `email` o `telefono` pasan de vacío a informado | E: `preferencia_notificacion` (`INSERT IGNORE`) | no | parcial en `PNS` (no hay lógica equivalente «al agregar email/teléfono») | no | no aplica | REEMPLAZAR TRIGGER |

Conteo: 21 de los 33 procedimientos de `SP` son código muerto o nunca se ejecutan (1, 3–11, 13, 14, 16, 20–22, 27, 28 y 31–33), y 12 se ejecutan (10 llamados desde Java: 12, 15, 17, 19, 23–26, 29, 30; uno desde otra rutina: 2; uno desde un trigger: 18). Se suman 3 nombres referenciados sin definición y 2 triggers vivos.

---

## 4. Ruta de factura: rutinas frente al cálculo en Java

### 4.1 Qué hace cada parte

| Paso | Dónde | Efecto |
|---|---|---|
| 1 | `EFjs:655-701` | Envía **solo las filas visibles**, con el `subtotal` calculado en el navegador (`EFjs:672`, `EFjs:695`) |
| 2 | `LFS:85-94` | Por cada línea recibida, `CALL sp_actualizar_linea_factura(...)`. La rutina inserta o modifica la línea con ese `subtotal` y llama a `ActualizarTotalFactura`: `factura.total = SUM(linea_factura.subtotal)` sobre **todas las líneas en BD** (`SP:61-72`) |
| 3 | `LFS:96-104` | Java desglosa el IVA de **las líneas recibidas** con `CIS.desglosarLinea` (tarifa por producto, IVA incluido, HALF_UP; `CIS:59-77`) |
| 4 | `LFS:106-122` | Java escribe `factura.subtotal`, `igv` y `total` con esas sumas, **sobrescribiendo el total de la rutina**, y regenera el asiento de venta |
| — | `FS:229-243` | Mismo cálculo ADR-009 cuando la factura se guarda con sus líneas por `FacturaService` |
| — | `LFC:51-58` | `DELETE /lineas-factura/eliminar/{id}` borra una línea **sin recalcular totales**. La UI no lo usa: `removeLinea` (`EFjs:331-350`) solo quita la fila del DOM |

### 4.2 Respuestas

**(a) ¿Hay hoy dos cálculos de totales?** Sí. `ActualizarTotalFactura` (`SP:61-72`, invocada desde `SP:506`/`SP:532`) calcula `total = Σ subtotal` en SQL, y `LFS:96-122` calcula `subtotal`, `igv` y `total` en Java dentro de la misma transacción (`@Transactional`, `LFS:71`). Prevalece Java, que escribe después. En el log general de MySQL de la reproducción se ve `call sp_actualizar_linea_factura(13, 1, 4, 173, 1, 1130.0, 1130.0, 7)` seguido de `update factura set ...` emitido por Hibernate.

**(b) ¿Alguna ruta en producción persiste totales calculados por una rutina que ignore IVA, tarifa o moneda?** Con el código actual, no: `factura.total` escrito por la rutina siempre lo sobrescribe Java en la misma transacción (`LFS:111-114`). Si Java lanza una excepción (por ejemplo, una factura anulada, `LFS:108-110`), se revierte también lo escrito por la rutina. La rutina nunca escribe `subtotal` ni `igv`. Producción: «sin verificar» (no se pudo comprobar qué versión de las rutinas tiene). Sí se reprodujo una **inconsistencia persistida** en esta ruta: no la causa la rutina, sino la combinación UI + Java (4.3, caso 2). Fuera de la ruta de escritura, `sp_obtener_ventas_por_mes` **muestra** montos que ignoran la moneda (4.4); no los persiste.

**(c) ¿Qué pruebas cubren esa ruta?** Solo pruebas unitarias con Mockito: `LineaFacturaServiceAsientoTest` (`@ExtendWith(MockitoExtension.class)`, `LineaFacturaRepository` como `@Mock`, líneas 33-38) y `DesgloseIvaVentaTest` (`@Mock LineaFacturaRepository`, línea 80; `updateLineas` en las líneas 168-278). La configuración de pruebas usa H2 (`src/test/resources/application.yml:24`), donde las rutinas no existen. **Ninguna prueba ejecuta `sp_actualizar_linea_factura` ni `ActualizarTotalFactura`**, ni el caso de guardar menos líneas de las que hay en BD. De los 5 `*IT`, ninguno toca rutinas (`rg -i 'procedure|routines|stored' src/test` = 0 coincidencias).

### 4.3 Casos reproducidos en `t023-mysql` (datos sintéticos)

Datos: cliente «T023 Cliente Sintetico»; producto A gravado al 13 % con IVA incluido (id 173); producto B exento (id 174); factura 4 en CRC. Las llamadas se hicieron con `PUT /lineas-factura/actualizar` a la aplicación real.

**Caso 1: alta normal (sin discrepancia en el resultado).** Entrada: A ×1 con `subtotal` 1130.00 y B ×1 con `subtotal` 500.00.

| | subtotal | igv | total |
|---|---|---|---|
| Esperado según Java (ADR-009: 1130/1.13 = 1000.00 + 130.00; B exento 500.00) | 1500.00 | 130.00 | 1630.00 |
| Persistido | 1500.00 | 130.00 | 1630.00 |
| Σ `linea_factura.subtotal` | | | 1630.00 |

**Caso 2: el usuario quita una línea en la pantalla y guarda (discrepancia reproducida).** Entrada: solo la línea de A (id 13), porque la UI no llama al `DELETE` y la línea de B queda en BD.

| | subtotal | igv | total |
|---|---|---|---|
| Salida de la rutina (aislada, en una transacción revertida, con la misma entrada) | 1000.00 (sin cambio) | 130.00 (sin cambio) | **1630.00** (= Σ de las 2 líneas en BD) |
| Esperado según Java (solo la línea recibida) | 1000.00 | 130.00 | **1130.00** |
| **Persistido** | 1000.00 | 130.00 | **1130.00** |
| Líneas en BD | 2 líneas, Σ subtotal = **1630.00** | | |

La factura queda con `total` 1130.00 y dos líneas que suman 1630.00. El asiento de venta se regeneró sobre 1130.00 (log general: reversión `ASI-2026-0001` y asiento nuevo). El XML de comprobante electrónico lee las líneas persistidas (`LFR.findEntitiesByFacturaId`, comentario de `DesgloseIvaVentaTest:330`), es decir, las dos. Aquí la rutina calcula el total coherente con las líneas, y es Java el que persiste un total que no las representa. La causa es de la UI y de Java (`EFjs:331-350`, `LFS:96-104`), no de la rutina.

**Caso 3: efecto de la rutina sola sobre las columnas de IVA.** En una transacción revertida, `CALL sp_actualizar_linea_factura(13, 1, 4, 173, 2, 1130.00, 2260.00, 7)` dejó `subtotal` 1000.00, `igv` 130.00 y `total` 2760.00. Para esa entrada, Java esperaría `subtotal` 2500.00, `igv` 260.00 y `total` 2760.00. Si la rutina se usara sin el paso Java, el desglose quedaría desalineado con el total (ADR-009: `base + impuesto = importe`). Hoy no se persiste porque Java corrige (respuesta b).

Observación de código, **hipótesis no verificada**: ni la rutina ni Java recalculan `subtotal = cantidad × precio_unitario`; los dos confían en el valor que envía el navegador (`EFjs:695`, `SP:501`, `SP:526`, `LFS:102`).

### 4.4 Reporte de ventas por mes (ADR-011, reproducido)

`sp_obtener_ventas_por_mes(1)` se ejecutó antes y después de insertar facturas sintéticas. Solo se compararon diferencias del mes en curso, sin valores absolutos.

| Entrada sintética | Δ salida de la rutina | Esperado según Java (`RS:362-388`, `F:214-220`) |
|---|---|---|
| Factura USD, `total` 100.00, `tipo_cambio` 500.00000, creada ayer | **+100.00** | +50 000.00 (`total × tipoCambio`) |
| Factura CRC, `total` 1000.00, creada hoy | **+0.00** (`create_date <= CURDATE()` excluye hoy) | +1000.00 (`!fecha.isBefore(inicio)`) |

El gráfico «Ventas por mes» (`REPjs:49`) y la «Comparativa de ingresos» (`RC:797`) muestran cifras distintas para el mismo periodo. Está relacionado con P-013 (agregaciones con monedas mezcladas).

---

## 5. Dependencias para un entorno nuevo

Observado en `t023-mysql` (V35 sobre el dump, sin las rutinas de `SP` cargadas al principio):

| Función | Rutinas necesarias | Si faltan |
|---|---|---|
| Arranque de la aplicación | ninguna | **Arranca** (observado: `Started ...` sin rutinas; `ddl-auto=validate` no revisa rutinas) |
| Alta de usuario (pantalla de usuarios, admin de bootstrap) | `sp_crear_preferencias_usuario` **si existe el trigger T1** | Con T1 y sin el procedimiento: `ERROR 1305 PROCEDURE facturas_monrachem.sp_crear_preferencias_usuario does not exist` y el `INSERT` en `usuario` falla (**reproducido**; un usuario inactivo sí se inserta). El dump restaurado tal cual está en esta situación. Como `BootstrapAdminInitializer` inserta un usuario activo, en una BD sin ADMIN activo **la aplicación no podría crear su admin de bootstrap** (esperado; no se ejecutó ese arranque) |
| Facturar (crear o editar líneas) | `sp_actualizar_linea_factura`, `ActualizarTotalFactura`, `ObtenerProductos` (selector de productos) | Esperado: `/productos/records` y `PUT /lineas-factura/actualizar` fallan con `PROCEDURE ... does not exist` (SQLSyntaxErrorException 1305) → HTTP 500; no se pueden guardar líneas |
| Listar productos (catálogo) | ninguna: la lista `/productos` usa JPA; solo `/productos/records` usa `ObtenerProductos` | Esperado: falla solo el selector de las pantallas de factura y de proveedores |
| Desactivar producto | `sp_desactivar_producto` | Esperado: `/productos/eliminar/{id}` → error 1305 |
| Facturación electrónica | emisión, firma y envío: ninguna (JPA). Pantalla de comprobantes: `sp_listar_comprobantes_por_empresa`, `_por_estado`, `_por_fechas`, `sp_contar_comprobantes_por_estado` | Esperado: la pantalla `comprobantes.html`, sus exportaciones y la tendencia → 500. `GET .../clave/{}` y `.../factura/{}` ya fallan hoy con o sin rutinas (3.3) |
| Reportes | `sp_obtener_ventas_por_mes`, `sp_obtener_clientes_nuevos_por_mes`, `sp_obtener_productos_mas_vendidos` | Esperado: `RC` captura la excepción y responde 500 (`RC:631-634`, `RC:669-672`, `RC:708-711`); los tres gráficos muestran error |
| Reintentos de comprobantes (job) | ninguna (`findPendientesReintento` es JPQL) | sin efecto |

Imprescindibles hoy para operar: **8** (`ObtenerProductos`, `sp_actualizar_linea_factura`, `ActualizarTotalFactura`, `sp_desactivar_producto` y las 4 de comprobantes) y **3** más para los reportes. `sp_crear_preferencias_usuario` es imprescindible solo mientras exista T1. Hoy ninguna se crea con Flyway, así que un entorno nuevo construido solo con migraciones no factura (P-017).

---

## 6. Privilegios

Medidos en `t023-mysql` con `log_bin=1` y `log_bin_trust_function_creators=0` (valores por defecto de MySQL 8), con el usuario `t023app` (`ALL PRIVILEGES ON facturas_monrachem.*`, sin privilegios globales):

| Operación | Resultado como `t023app` |
|---|---|
| Flyway V19..V35 (crean y borran procedimientos auxiliares) | OK: bastan `CREATE ROUTINE` y `ALTER ROUTINE` (`DROP PROCEDURE`) del esquema |
| Cargar `SP` (33 `CREATE PROCEDURE`) | OK; `DEFINER` = `t023app@%` |
| Cargar `TRG` (2 `CREATE TRIGGER`) | **ERROR 1419** `You do not have the SUPER privilege and binary logging is enabled (you *might* want to use the less safe log_bin_trust_function_creators variable)` en las líneas 27 y 49 |
| Efecto colateral de cargar `TRG` sin SUPER | **Los `DROP TRIGGER IF EXISTS` sí se ejecutan**: tras el intento, `information_schema.TRIGGERS` quedó **vacío**. Se pierden los triggers y no se recrean |
| Restaurar el dump con triggers `DEFINER=root@%` | Requiere un usuario que pueda asignar ese `DEFINER` (`SET_USER_ID` o SUPER); se hizo como `root` |
| Ejecutar rutinas desde la aplicación | `EXECUTE` (incluido en `ALL` del esquema) |

Qué requiere cada opción del §7:

| Opción | Usuario de Flyway | Usuario de la aplicación | Deja de requerir |
|---|---|---|---|
| A. Todo a Java | `CREATE ROUTINE` y `ALTER ROUTINE` **siguen siendo necesarios** para aplicar V19 y V29–V35 en un entorno nuevo (no se pueden reescribir migraciones aplicadas sin romper el checksum), más `TRIGGER` para el `DROP TRIGGER` de la migración de limpieza | sin `EXECUTE` | SUPER, `SET_USER_ID` y `log_bin_trust_function_creators`; rutinas fuera de Flyway; respaldo de rutinas (P-038 queda sin objeto para la aplicación) |
| B. Mixto (R__ solo de consultas) | `CREATE ROUTINE` y `ALTER ROUTINE` de forma permanente; `TRIGGER` para borrar T1 y T2 | `EXECUTE` | SUPER y `log_bin_trust_function_creators` (sin triggers) |
| C. Todo versionado tal como está | `CREATE ROUTINE`, `ALTER ROUTINE`, `TRIGGER` **y** SUPER/`SET_USER_ID` o `log_bin_trust_function_creators=1` para los triggers | `EXECUTE` | nada |

---

## 7. Alternativas (sin decidir)

Esfuerzo contado sobre el código actual. «Métodos» son métodos de repositorio o servicio que cambian o se eliminan.

### A. Todo a Java/JPQL, sin rutinas de aplicación

- **Contenido**: reemplazar las 10 rutinas llamadas desde Java (`ObtenerProductos`, `sp_actualizar_linea_factura`, `sp_desactivar_producto`, `sp_contar_comprobantes_por_estado`, 3 `sp_listar_comprobantes_*`, `sp_obtener_clientes_nuevos_por_mes`, `sp_obtener_productos_mas_vendidos`, `sp_obtener_ventas_por_mes`) por JPA/JPQL. Pasar los 2 `@Procedure` a consultas derivadas. Borrar las 6 declaraciones muertas (`findLineasByFacturaId`, `findPendientesEnvio` + `obtenerPendientesEnvio`, `obtenerTopClientes`, `obtenerVentasPorDia`, `obtenerEstadisticasVentas`). Crear las preferencias de usuario en Java al dar de alta (sustituye T1 y `sp_crear_preferencias_usuario`) y al informar email o teléfono (sustituye T2). Agregar una migración `V36` que haga `DROP PROCEDURE IF EXISTS` de las 33 rutinas y `DROP TRIGGER IF EXISTS` de T1 y T2. Retirar `database/routines/`.
- **Esfuerzo**: unos 20 métodos en 12–14 archivos (`PR`, `PS`, `LFR`, `LFS`, `CER`, `CES` y su interfaz, `CliR`, `FR`, `RC`, `RS`, `UsuarioServiceImpl`, `BootstrapAdminInitializer`, `V36`) más pruebas (incluidas `*IT` de la ruta de líneas y de reportes con MySQL).
- **Riesgo**: medio. Cambia la ruta de facturación (hoy cubierta solo con mocks) y las pantallas de comprobantes. Hay que reproducir el orden y las columnas que esperan `CES.convertirResultadosADTO` y los gráficos. Corrige de paso el reporte en monedas mezcladas, el día en curso excluido y los endpoints `clave`/`factura` rotos.
- **Impacto**: P-009 se resuelve casi entero (solo quedan `CREATE`/`ALTER ROUTINE` para las migraciones históricas). P-038 deja de afectar a la aplicación. P-017 avanza: el entorno nuevo no depende de rutinas fuera de Flyway.
- **Orden sugerido**: (1) pruebas `*IT` de la ruta de líneas, incluido el caso 2 de §4.3; (2) `sp_actualizar_linea_factura` + `ActualizarTotalFactura` a JPA, y decidir el borrado de líneas quitadas en la UI; (3) T1/T2 a Java; (4) reportes; (5) comprobantes y `@Procedure`; (6) `ObtenerProductos` y `sp_desactivar_producto`; (7) `V36` de limpieza y retiro de `database/routines/`.

### B. Mixto: escrituras y montos a Java; consultas puras versionadas como migraciones repetibles `R__`

- **Contenido**: a Java, `sp_actualizar_linea_factura`, `ActualizarTotalFactura`, `sp_desactivar_producto`, `sp_obtener_ventas_por_mes` (montos, ADR-011), T1/T2 y `sp_crear_preferencias_usuario`; y los 2 `@Procedure` a consultas derivadas. A `R__rutinas_consulta.sql`, `ObtenerProductos` (con filtro `active`), `sp_listar_comprobantes_por_empresa`, `_por_estado`, `_por_fechas`, `sp_contar_comprobantes_por_estado`, `sp_obtener_clientes_nuevos_por_mes` y `sp_obtener_productos_mas_vendidos` (con las correcciones de fecha y anuladas). Las 21 muertas se borran en una `V36`.
- **Esfuerzo**: unos 10 métodos en 7–9 archivos (`LFR`, `LFS`, `PR`, `PS`, `FR`, `RC`, `CER`, `UsuarioServiceImpl`/`BootstrapAdminInitializer`) más 1 `R__` y 1 `V36`.
- **Riesgo**: medio-bajo en Java; las consultas quedan en SQL sin pruebas en H2 (necesitan `*IT` con MySQL). Las `R__` se reaplican con cada cambio de checksum, así que su definición queda versionada y respaldada por Flyway.
- **Impacto**: P-009 parcial (el despliegue sigue necesitando `CREATE ROUTINE`/`ALTER ROUTINE`, sin SUPER). P-038 se resuelve: un esquema nuevo recrea las rutinas desde Flyway sin depender del respaldo. P-017 avanza igual que en A.
- **Orden sugerido**: (1) `*IT` de la ruta de líneas; (2) escrituras y montos a Java; (3) T1/T2 a Java; (4) `R__` con las consultas corregidas; (5) `V36` de limpieza.

### C. Todo versionado en Flyway tal como está

- **Contenido**: una `V36` (o `R__`) con las 33 rutinas y los 2 triggers de `database/routines/` sin cambios.
- **Esfuerzo**: 1–2 archivos, sin cambios en Java.
- **Riesgo**: alto. Exige SUPER o `log_bin_trust_function_creators=1` en producción por los triggers (§6). Versiona 8 rutinas rotas y 21 muertas. Mantiene el doble cálculo de totales, el reporte de ventas en monedas mezcladas (ADR-011) y los endpoints `clave`/`factura` rotos. Si producción tiene versiones distintas de las rutinas (pendiente de 2.2), la migración las sobrescribe.
- **Impacto**: P-009 resuelto solo en cuanto a «rutinas fuera de Flyway», pero suma el requisito de SUPER. P-038 resuelto. P-017 avanza: un entorno nuevo tendría las rutinas.
- **Orden sugerido**: (1) obtener la salida de 2.2 y conciliar hashes; (2) decidir el `DEFINER` y el privilegio para los triggers; (3) migración; (4) `*IT` que ejecuten las rutinas vivas.

---

## 8. Estado de Docker antes y después

| | Contenedores | Volúmenes |
|---|---|---|
| Antes | `mongodb`, `mongo-express`, `omarchy-windows`, `pgadmin`, `postgres`, `rabbitmq` | `42b8c43865d78962043e6d669da50d13ec39806c1a3ef6bc0436ea446abb4df5`, `api_redis-data`, `mongodb_mongodb_data`, `postgres_pgadmin_data`, `postgres_postgres_data`, `rabbitmq_rabbitmq_data` (6) |
| Durante | los anteriores + `t023-mysql` (`--tmpfs`, sin volumen) | los mismos 6 (se comprobó que `--tmpfs` no creó un volumen anónimo) |
| Después (`docker rm -f t023-mysql`) | `mongodb`, `mongo-express`, `omarchy-windows`, `pgadmin`, `postgres`, `rabbitmq` | los mismos 6 |

La aplicación de prueba se detuvo, y se borraron las contraseñas temporales, la cookie de sesión y el hash del usuario sintético.
