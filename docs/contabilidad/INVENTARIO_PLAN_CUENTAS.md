# Inventario del plan de cuentas y de los parámetros contables (T-013)

Insumo para el ADR y para la corrección de P-022 / P-012. Tarea de solo lectura: no se modificó código, migraciones, parámetros ni datos.

## Fuente y método

| Elemento | Valor |
|---|---|
| Código analizado | rama `t-013-inventario-del-plan-de-cuentas-parametro`, base `prod` `cd4a071` (incluye V28) |
| Datos | dump de producción `facturas_monrachem_20261011_173406.sql` (`flyway_schema_history` en V18) |
| Motor | contenedor aislado `t013-mysql` (MySQL 8.0.32, `--tmpfs /var/lib/mysql`, puerto solo en `127.0.0.1:13313`), eliminado al terminar |
| Migraciones aplicadas | V19..V28 con la API Java de Flyway 11.7.2, sin arrancar la aplicación: `initialVersion=18 targetVersion=28 executed=10 success=true` |
| Datos agregados | ninguno (sin usuarios, sin cambios de triggers ni filas manuales) |
| Producción | no se usó `.env` ni se conectó a la BD de producción |

Las secciones 1, 3, 4, 5 y 7 salen de consultas SQL reales sobre esa base; las consultas están en el [Anexo A](#anexo-a-consultas-sql). Las referencias `archivo:línea` de las secciones 3 y 6 corresponden al commit `cd4a071`.

Abreviaturas de rutas usadas en el documento (todas bajo `src/main/java/api/astro/whats_orders_manager/modules/`):

| Abreviatura | Archivo |
|---|---|
| `ACS` | `contabilidad/service/AsientoContableService.java` |
| `CCS` | `contabilidad/service/CuentaContableService.java` |
| `CC` | `contabilidad/model/CuentaContable.java` |
| `CCR` | `contabilidad/repository/CuentaContableRepository.java` |
| `DAR` | `contabilidad/repository/DetalleAsientoRepository.java` |
| `CCtrl` | `contabilidad/controller/CuentaContableController.java` |
| `CCMap` | `contabilidad/dto/mapper/CuentaContableMapper.java` |
| `PCS` | `contabilidad/service/ParametroContableService.java` |
| `PCCtrl` | `contabilidad/controller/ParametroContableController.java` |
| `CalcS` | `contabilidad/service/CalculoContableService.java` |
| `NS` | `nomina/service/NominaService.java` |

---

## 1. Plan de cuentas tras aplicar V19..V28 sobre el dump

**Total: 31 filas, igual a `SELECT COUNT(*) FROM cuentas_contables` = 31.**

Criterios de las columnas calculadas:

- **Padre FK**: código de la cuenta apuntada por `cuenta_padre_id`; `NULL` si la columna es nula.
- **Padre prefijo**: código que resulta de quitar el último `.segmento`; `—` si no existe ninguna cuenta con ese código.
- **Hijas prefijo**: solo hijas **directas** por prefijo (código `<código>.<un segmento más>`).
- **Detalles**: filas de `detalles_asiento` con `cuenta_id` igual al id de la cuenta.
- **Origen**: `dump` = la fila ya estaba en el dump antes de Flyway (instantánea de la consulta 0 del anexo); `Vnn` = la insertó esa migración. Las filas `dump (V16)` estaban en el dump y coinciden con la semilla de `V16__seed_nomina_parametros_contables.sql` (V16 es anterior a V18, así que ya estaba aplicada en producción). Las filas `dump` de ids 1–5 no las inserta ninguna migración del repositorio: se cargaron por fuera de Flyway.

| id | código | nombre | tipo | naturaleza | nivel | activa | acepta mov. | padre FK | padre prefijo | hijas prefijo | detalles | origen |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1.1.01 | Caja | ACTIVO | DEUDORA | 1 | sí | sí | NULL | — | 5 | 0 | dump |
| 16 | 1.1.01.001 | Caja General | ACTIVO | DEUDORA | 4 | sí | sí | NULL | 1.1.01 | 0 | 0 | V19 |
| 17 | 1.1.01.002 | Bancos | ACTIVO | DEUDORA | 4 | sí | sí | NULL | 1.1.01 | 0 | 0 | V19 |
| 18 | 1.1.01.003 | Tarjetas por Liquidar | ACTIVO | DEUDORA | 4 | sí | sí | NULL | 1.1.01 | 0 | 0 | V19 |
| 19 | 1.1.01.004 | Cheques por Depositar | ACTIVO | DEUDORA | 4 | sí | sí | NULL | 1.1.01 | 0 | 0 | V19 |
| 20 | 1.1.01.005 | Recaudación por Terceros | ACTIVO | DEUDORA | 4 | sí | sí | NULL | 1.1.01 | 0 | 0 | V19 |
| 2 | 1.1.02 | Banco Cuenta Corriente | ACTIVO | DEUDORA | 1 | sí | sí | NULL | — | 1 | 0 | dump |
| 12 | 1.1.02.001 | Cuentas por Cobrar Clientes | ACTIVO | DEUDORA | 4 | sí | sí | NULL | 1.1.02 | 0 | 0 | V19 |
| 3 | 1.1.03 | IVA Crdito Fiscal | ACTIVO | DEUDORA | 1 | sí | sí | NULL | — | 0 | 0 | dump |
| 23 | 2 | Pasivo | PASIVO | ACREEDORA | 1 | sí | no | NULL | — | 1 | 0 | V27 |
| 24 | 2.1 | Pasivo Corriente | PASIVO | ACREEDORA | 2 | sí | no | 2 | 2 | 2 | 0 | V27 |
| 4 | 2.1.01 | Cuentas por Pagar Proveedores | PASIVO | ACREEDORA | 1 | sí | sí | NULL | 2.1 | 5 | 0 | dump |
| 7 | 2.1.01.001 | Salarios por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | NULL | 2.1.01 | 0 | 0 | dump (V16) |
| 8 | 2.1.01.002 | CCSS Patronal por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | NULL | 2.1.01 | 0 | 0 | dump (V16) |
| 9 | 2.1.01.003 | CCSS Empleado por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | NULL | 2.1.01 | 0 | 0 | dump (V16) |
| 10 | 2.1.01.004 | INS por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | NULL | 2.1.01 | 0 | 0 | dump (V16) |
| 11 | 2.1.01.005 | Retención de Renta por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | NULL | 2.1.01 | 0 | 0 | dump (V16) |
| 14 | 2.1.02.001 | IVA por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | NULL | — | 0 | 0 | V19 |
| 15 | 2.1.03.001 | Anticipos de Clientes | PASIVO | ACREEDORA | 4 | sí | sí | NULL | — | 0 | 0 | V19 |
| 25 | 2.1.04 | Obligaciones Laborales por Pagar | PASIVO | ACREEDORA | 3 | sí | no | 2.1 | 2.1 | 2 | 0 | V27 |
| 29 | 2.1.04.001 | Asociación Solidarista por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | 2.1.04 | 2.1.04 | 0 | 0 | V27 |
| 30 | 2.1.04.002 | Pensiones Alimentarias por Pagar | PASIVO | ACREEDORA | 4 | sí | sí | 2.1.04 | 2.1.04 | 0 | 0 | V27 |
| 13 | 4.1.01.001 | Ingresos por Ventas | INGRESO | ACREEDORA | 4 | sí | sí | NULL | — | 0 | 0 | V19 |
| 21 | 4.2.01.001 | Diferencial Cambiario Ganado | INGRESO | ACREEDORA | 4 | sí | sí | NULL | — | 0 | 0 | V25 |
| 26 | 5 | Gastos | EGRESO | DEUDORA | 1 | sí | no | NULL | — | 1 | 0 | V27 |
| 27 | 5.1 | Gastos de Operación | EGRESO | DEUDORA | 2 | sí | no | 5 | 5 | 2 | 0 | V27 |
| 5 | 5.1.01 | Compras | EGRESO | DEUDORA | 1 | sí | sí | NULL | 5.1 | 1 | 0 | dump |
| 6 | 5.1.01.001 | Gasto en Sueldos y Salarios | EGRESO | DEUDORA | 4 | sí | sí | NULL | 5.1.01 | 0 | 0 | dump (V16) |
| 28 | 5.1.02 | Gastos de Personal | EGRESO | DEUDORA | 3 | sí | no | 5.1 | 5.1 | 1 | 0 | V27 |
| 31 | 5.1.02.001 | Aporte Patronal Asociación Solidarista | EGRESO | DEUDORA | 4 | sí | sí | 5.1.02 | 5.1.02 | 0 | 0 | V27 |
| 22 | 5.2.01.001 | Diferencial Cambiario Perdido | EGRESO | DEUDORA | 4 | sí | sí | NULL | — | 0 | 0 | V25 |

Resumen por origen: 11 del dump (5 sin migración + 6 de V16), 9 de V19, 2 de V25, 9 de V27. Por tipo: ACTIVO 9, PASIVO 13, INGRESO 2, EGRESO 7, CAPITAL 0. Todas las cuentas están activas. Ninguna cuenta tiene detalles de asiento.

---

## 2. Inconsistencias detectadas

### (a) Cuentas con hijas que aceptan movimientos

Cuentas con `acepta_movimientos = 1` que tienen descendientes por prefijo de código (ninguna tiene hijas por FK):

| Cuenta | Nombre | Hijas por prefijo | Hijas por FK |
|---|---|---|---|
| 1.1.01 | Caja | 1.1.01.001, 1.1.01.002, 1.1.01.003, 1.1.01.004, 1.1.01.005 | 0 |
| 1.1.02 | Banco Cuenta Corriente | 1.1.02.001 | 0 |
| 2.1.01 | Cuentas por Pagar Proveedores | 2.1.01.001, 2.1.01.002, 2.1.01.003, 2.1.01.004, 2.1.01.005 | 0 |
| 5.1.01 | Compras | 5.1.01.001 | 0 |

Las cuatro son destino de claves de `parametros_contables` (ver sección 3). Las cuentas creadas por V27 (`2`, `2.1`, `2.1.04`, `5`, `5.1`, `5.1.02`) tienen `acepta_movimientos = 0`, como corresponde a cuentas de agrupación.

### (b) `cuenta_padre_id` nulo o distinto del padre por prefijo

22 cuentas con más de un segmento cumplen la condición. En todas, `cuenta_padre_id` es `NULL`; no hay ningún caso de FK que apunte a un padre distinto del de prefijo.

| Situación | Cuentas |
|---|---|
| FK `NULL` y el padre por prefijo **existe** | 1.1.01.001, 1.1.01.002, 1.1.01.003, 1.1.01.004, 1.1.01.005 (padre 1.1.01); 1.1.02.001 (padre 1.1.02); 2.1.01 (padre 2.1); 2.1.01.001 … 2.1.01.005 (padre 2.1.01); 5.1.01 (padre 5.1); 5.1.01.001 (padre 5.1.01) |
| FK `NULL` y el padre por prefijo **no existe** | 1.1.01, 1.1.02, 1.1.03 (falta `1.1`); 2.1.02.001 (falta `2.1.02`); 2.1.03.001 (falta `2.1.03`); 4.1.01.001 (falta `4.1.01`); 4.2.01.001 (falta `4.2.01`); 5.2.01.001 (falta `5.2.01`) |

Solo 7 cuentas tienen la FK correcta: 2.1, 2.1.04, 2.1.04.001, 2.1.04.002, 5.1, 5.1.02, 5.1.02.001 (todas de V27). En consecuencia, `2.1` tiene 2 hijas por prefijo (2.1.01 y 2.1.04) pero solo 1 por FK (2.1.04), y `5.1` tiene 2 por prefijo (5.1.01 y 5.1.02) pero 1 por FK.

Tampoco existen las raíces `1` (Activo), `1.1`, `3` (Capital) ni `4` (Ingresos).

### (c) Nivel inconsistente con el número de segmentos del código

| Cuenta | Nombre | nivel | segmentos |
|---|---|---|---|
| 1.1.01 | Caja | 1 | 3 |
| 1.1.02 | Banco Cuenta Corriente | 1 | 3 |
| 1.1.03 | IVA Crdito Fiscal | 1 | 3 |
| 2.1.01 | Cuentas por Pagar Proveedores | 1 | 3 |
| 5.1.01 | Compras | 1 | 3 |

Son las cinco cuentas del dump sin migración (ids 1–5). Las demás 26 cuentas tienen `nivel` igual al número de segmentos. Ver la consecuencia en la sección 6 (la vista de árbol trata `nivel = 1` como raíz).

### (d) Series con tipo, naturaleza o significado distinto al del padre

La comparación mecánica de tipo y naturaleza contra el ancestro por prefijo más cercano no encuentra diferencias: todas las cuentas comparten tipo y naturaleza con su ancestro. Las inconsistencias son de **significado**: la cuenta hija no pertenece al concepto del padre por prefijo.

| Cuenta hija | Padre por prefijo | Observación |
|---|---|---|
| 1.1.02.001 Cuentas por Cobrar Clientes | 1.1.02 Banco Cuenta Corriente | Una cuenta por cobrar dentro de la serie de un banco. Según el usuario, 1.1.02 no es una cuenta real de la empresa. |
| 1.1.01.002 Bancos | 1.1.01 Caja | Bancos dentro de la serie de Caja. |
| 1.1.01.003 Tarjetas por Liquidar, 1.1.01.004 Cheques por Depositar, 1.1.01.005 Recaudación por Terceros | 1.1.01 Caja | Partidas en tránsito y por cobrar a terceros dentro de Caja. |
| 2.1.01.001 Salarios por Pagar, 2.1.01.002 CCSS Patronal, 2.1.01.003 CCSS Empleado, 2.1.01.004 INS, 2.1.01.005 Retención de Renta | 2.1.01 Cuentas por Pagar Proveedores | Pasivos laborales y fiscales dentro de la serie de proveedores. V27 creó aparte `2.1.04 Obligaciones Laborales por Pagar` para los pasivos laborales nuevos, de modo que los pasivos de nómina quedan repartidos entre 2.1.01.* y 2.1.04.*. |
| 5.1.01.001 Gasto en Sueldos y Salarios | 5.1.01 Compras | Gasto de personal dentro de Compras. V27 creó aparte `5.1.02 Gastos de Personal` (con 5.1.02.001), de modo que el gasto de personal queda repartido entre 5.1.01.001 y 5.1.02.*. |

### (e) Nombres y descripciones con codificación rota

Los textos rotos ya vienen así en el dump: la vocal acentuada se perdió por completo (no hay bytes de reemplazo, el carácter falta).

| Campo | Fila | Valor almacenado | HEX relevante |
|---|---|---|---|
| `cuentas_contables.nombre` | 1.1.03 | `IVA Crdito Fiscal` | `…4372 6469…` (`Cr`+`dito`, sin `é`) |
| `parametros_contables.descripcion` | CPP_CUENTA_COMPRAS | `Dbito compras a proveedores` | `4462 6974 6F…` (`D`+`bito`) |
| `parametros_contables.descripcion` | CPP_IVA_CREDITO_FISCAL | `IVA crdito fiscal compras` | `…6372 6469…` |

No se encontraron caracteres de reemplazo U+FFFD, `?` literales ni doble codificación (`Ã`, `Â`) en `cuentas_contables` ni en `parametros_contables`. Los nombres con tilde creados por migraciones (por ejemplo `Recaudación por Terceros`, `Retención de Renta por Pagar`) están en UTF-8 correcto.

---

## 3. Claves de `parametros_contables`

**Total: 30 filas, igual a `SELECT COUNT(*) FROM parametros_contables` = 30.**

Clase de cuenta: `hoja` = sin hijas por prefijo ni por FK; `con hijas` = tiene al menos una; `inactiva` = `activa = 0` (no hay ninguna). Origen de la clave: ids 1–8 del dump sin migración, 9–14 del dump (semilla V16), 15–24 V19, 25–26 V25, 27–30 V27.

| id | Clave | Cuenta | Nombre de la cuenta | Clase | Usada en el código (archivo:línea) |
|---|---|---|---|---|---|
| 1 | CPP_CUENTA_COMPRAS | 5.1.01 | Compras | **con hijas** | `ACS:916` (constante); `generarAsientoCompra` `ACS:998` (CxP con OC); `obtenerIdCuentaComprasCpp` `ACS:1056-1057` (preselección en CxP manual) |
| 2 | CPP_CUENTA_POR_PAGAR | 2.1.01 | Cuentas por Pagar Proveedores | **con hijas** | `ACS:918`; `generarAsientoCompra` `ACS:1000`; `generarAsientoPagoProveedor` `ACS:1164`; exclusión de contrapartida `ACS:1105` |
| 3 | CPP_IVA_CREDITO_FISCAL | 1.1.03 | IVA Crdito Fiscal | hoja | `ACS:920`; `generarAsientoCompra` `ACS:1020`; exclusión `ACS:1106` |
| 4 | CPP_BANCO_TRANSFERENCIA | 1.1.02 | Banco Cuenta Corriente | **con hijas** | `generarAsientoPagoProveedor` `ACS:1157`, resuelta en `ACS:1165`; exclusión por prefijo `ACS:922`, `ACS:1102` |
| 5 | CPP_BANCO_CHEQUE | 1.1.02 | Banco Cuenta Corriente | **con hijas** | `ACS:1158`, `ACS:1165`; exclusión `ACS:1102` |
| 6 | CPP_BANCO_EFECTIVO | 1.1.01 | Caja | **con hijas** | `ACS:1159`, `ACS:1165`; exclusión `ACS:1102` |
| 7 | CPP_BANCO_SINPE | 1.1.02 | Banco Cuenta Corriente | **con hijas** | `ACS:1160`, `ACS:1165`; exclusión `ACS:1102` |
| 8 | CPP_BANCO_DEPOSITO | 1.1.02 | Banco Cuenta Corriente | **con hijas** | `ACS:1161`, `ACS:1165`; exclusión `ACS:1102` |
| 9 | NOMINA_GASTO_SUELDOS | 5.1.01.001 | Gasto en Sueldos y Salarios | hoja | `generarAsientoNomina` `ACS:1293`; validación previa `NS:55`, `NS:288-291` |
| 10 | NOMINA_SALARIOS_POR_PAGAR | 2.1.01.001 | Salarios por Pagar | hoja | `ACS:1298`; `NS:60`, `NS:288-291` |
| 11 | NOMINA_CCSS_PATRONAL_POR_PAGAR | 2.1.01.002 | CCSS Patronal por Pagar | hoja | `ACS:1294`; `NS:56`, `NS:288-291` |
| 12 | NOMINA_CCSS_POR_PAGAR | 2.1.01.003 | CCSS Empleado por Pagar | hoja | `ACS:1295`; `NS:57`, `NS:288-291` |
| 13 | NOMINA_INS_POR_PAGAR | 2.1.01.004 | INS por Pagar | hoja | `ACS:1296`; `NS:58`, `NS:288-291` |
| 14 | NOMINA_RENTA_POR_PAGAR | 2.1.01.005 | Retención de Renta por Pagar | hoja | `ACS:1297`; `NS:59`, `NS:288-291` |
| 15 | VENTA_CXC_CLIENTES | 1.1.02.001 | Cuentas por Cobrar Clientes | hoja | `ACS:363`; `construirAsientoVenta` `ACS:801`; `generarAsientoPago` `ACS:473`; exclusión `ACS:1104` |
| 16 | VENTA_INGRESOS | 4.1.01.001 | Ingresos por Ventas | hoja | `ACS:365`; `construirAsientoVenta` `ACS:802` |
| 17 | VENTA_IVA_POR_PAGAR | 2.1.02.001 | IVA por Pagar | hoja | `ACS:367`; `construirAsientoVenta` `ACS:836` |
| 18 | COBRO_ANTICIPOS_CLIENTES | 2.1.03.001 | Anticipos de Clientes | hoja | `ACS:369`; `generarAsientoPago` `ACS:474` |
| 19 | COBRO_CAJA_EFECTIVO | 1.1.01.001 | Caja General | hoja | prefijo `ACS:371` + `MetodoPago.EFECTIVO` en `generarAsientoPago` `ACS:471`; exclusión `ACS:1103` |
| 20 | COBRO_CAJA_TRANSFERENCIA | 1.1.01.002 | Bancos | hoja | `ACS:471` con `MetodoPago.TRANSFERENCIA`; exclusión `ACS:1103` |
| 21 | COBRO_CAJA_TARJETA | 1.1.01.003 | Tarjetas por Liquidar | hoja | `ACS:471` con `MetodoPago.TARJETA`; exclusión `ACS:1103` |
| 22 | COBRO_CAJA_CHEQUE | 1.1.01.004 | Cheques por Depositar | hoja | `ACS:471` con `MetodoPago.CHEQUE`; exclusión `ACS:1103` |
| 23 | COBRO_CAJA_RECAUDADO | 1.1.01.005 | Recaudación por Terceros | hoja | `ACS:471` con `MetodoPago.RECAUDADO`; exclusión `ACS:1103` |
| 24 | COBRO_CAJA_OTROS | 1.1.01.001 | Caja General | hoja | `ACS:471` con `MetodoPago.OTROS`; exclusión `ACS:1103` |
| 25 | DIFERENCIAL_CAMBIARIO_INGRESO | 4.2.01.001 | Diferencial Cambiario Ganado | hoja | `ACS:374`; `agregarDiferencialCambiario` `ACS:569` |
| 26 | DIFERENCIAL_CAMBIARIO_GASTO | 5.2.01.001 | Diferencial Cambiario Perdido | hoja | `ACS:376`; `agregarDiferencialCambiario` `ACS:572` |
| 27 | NOMINA_SOLIDARISTA_POR_PAGAR | 2.1.04.001 | Asociación Solidarista por Pagar | hoja | `generarAsientoNomina` `ACS:1310` (solo si hay monto) |
| 28 | NOMINA_PENSION_ALIMENTARIA_POR_PAGAR | 2.1.04.002 | Pensiones Alimentarias por Pagar | hoja | `ACS:1312` (solo si hay monto) |
| 29 | NOMINA_APORTE_PATRONAL_ASE_GASTO | 5.1.02.001 | Aporte Patronal Asociación Solidarista | hoja | `ACS:1314` (solo si hay monto) |
| 30 | NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR | 2.1.04.001 | Asociación Solidarista por Pagar | hoja | `ACS:1316` (solo si hay monto) |

Todas las claves se resuelven con `resolverCuenta` (`ACS:1429-1433`), que lanza `IllegalStateException` si la clave no existe.

**Claves que el código usa y no existen:** ninguna. Los seis valores de `MetodoPago` (`facturacion/enums/MetodoPago.java:13-18`) tienen su `COBRO_CAJA_*` y los cinco valores de `MetodoPagoProveedor` (`proveedores/enums/MetodoPagoProveedor.java:4-8`) tienen su `CPP_BANCO_*`.

**Claves que existen y el código no usa:** ninguna. Las 30 claves tienen al menos un uso.

**Resumen de clases:** 7 claves apuntan a cuentas **con hijas** (CPP_CUENTA_COMPRAS, CPP_CUENTA_POR_PAGAR y los cinco CPP_BANCO_*); 23 apuntan a hojas; ninguna a cuentas inactivas. Cuentas compartidas por varias claves: 1.1.02 (4 claves CPP_BANCO_*), 1.1.01.001 (COBRO_CAJA_EFECTIVO y COBRO_CAJA_OTROS) y 2.1.04.001 (NOMINA_SOLIDARISTA_POR_PAGAR y NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR, compartida a propósito según la descripción de V27).

---

## 4. Matriz medio de pago → cuenta contable

Cobros: `generarAsientoPago` arma la clave como `COBRO_CAJA_` + `MetodoPago.name()` (`ACS:471`). Pagos a proveedor: `generarAsientoPagoProveedor` usa un `switch` sobre `MetodoPagoProveedor` (`ACS:1156-1162`).

| Medio físico | Cobro: `MetodoPago` → clave → cuenta | Pago a proveedor: `MetodoPagoProveedor` → clave → cuenta | ¿Misma cuenta? |
|---|---|---|---|
| Efectivo | EFECTIVO → COBRO_CAJA_EFECTIVO → **1.1.01.001** Caja General | EFECTIVO → CPP_BANCO_EFECTIVO → **1.1.01** Caja (con hijas) | **No**: hija vs. padre de la misma serie |
| Transferencia | TRANSFERENCIA → COBRO_CAJA_TRANSFERENCIA → **1.1.01.002** Bancos | TRANSFERENCIA → CPP_BANCO_TRANSFERENCIA → **1.1.02** Banco Cuenta Corriente | **No**: series distintas |
| SINPE | no existe en `MetodoPago` (por convención se registra como TRANSFERENCIA → 1.1.01.002) | SINPE → CPP_BANCO_SINPE → **1.1.02** | **No** |
| Depósito | no existe en `MetodoPago`; el enum agrupa "Transferencia bancaria - depósito bancario" en TRANSFERENCIA (`MetodoPago.java:16`) → 1.1.01.002 | DEPOSITO → CPP_BANCO_DEPOSITO → **1.1.02** | **No** |
| Cheque | CHEQUE → COBRO_CAJA_CHEQUE → **1.1.01.004** Cheques por Depositar | CHEQUE → CPP_BANCO_CHEQUE → **1.1.02** | **No** (en el cobro es un activo en tránsito; en el pago sale del banco) |
| Tarjeta | TARJETA → COBRO_CAJA_TARJETA → **1.1.01.003** Tarjetas por Liquidar | no existe en `MetodoPagoProveedor` | n/a |
| Recaudado por terceros | RECAUDADO → COBRO_CAJA_RECAUDADO → **1.1.01.005** Recaudación por Terceros | no existe | n/a |
| Otros | OTROS → COBRO_CAJA_OTROS → **1.1.01.001** Caja General | no existe | n/a |

Valores permitidos por las columnas de la BD: `pagos.metodo_pago` = `CHEQUE, EFECTIVO, OTROS, RECAUDADO, TARJETA, TRANSFERENCIA`; `pagos_proveedor.metodo_pago` = `CHEQUE, DEPOSITO, EFECTIVO, SINPE, TRANSFERENCIA`. En el dump no hay filas en `pagos` ni en `pagos_proveedor`, así que ningún medio tiene uso histórico.

Divergencias: para los cuatro medios bancarios (transferencia, SINPE, depósito, cheque) el dinero que entra cae en la serie 1.1.01 y el que sale cae en 1.1.02; para efectivo, entra en la hoja 1.1.01.001 y sale de su padre 1.1.01. Ninguna combinación usa la misma cuenta en ambos sentidos. Además, cuatro medios de pago a proveedor distintos comparten una sola cuenta (1.1.02), y ninguno identifica una cuenta bancaria concreta.

---

## 5. Filas de `cuenta_bancaria` en el dump

**Total: 4 filas.** IBAN y número enmascarados en la propia consulta SQL (solo se muestran los últimos 4 dígitos; los valores completos no se extrajeron).

| id | Entidad | Moneda | Activa | Orden | IBAN | Número de cuenta |
|---|---|---|---|---|---|---|
| 1 | Banco Nacional - Colones | CRC | sí | 1 | ****4988 | ****4985 |
| 2 | Banco Nacional - Dólares | USD | sí | 2 | ****1319 | ****1312 |
| 3 | BAC San José - Colones | CRC | sí | 3 | ****8416 | ****9084 |
| 4 | BAC San José - Dólares | USD | sí | 4 | ****9879 | ****5598 |

Las cuatro pertenecen a la misma empresa (`empresa_id` = 1). La tabla no tiene ninguna columna que la vincule con `cuentas_contables`.

Usos actuales de `CuentaBancaria` en el código:

| Uso | Referencia | Qué hace |
|---|---|---|
| Pantalla Configuración → Empresa, bloque "Cuentas Bancarias (aparecen en el PDF)" | `src/main/resources/templates/modules/configuracion/empresa.html:505-509`; `src/main/resources/static/modules/configuracion/js/configuracion-empresa.js:515-640` | Lista, crea, edita y elimina cuentas bancarias |
| API REST `/api/empresa/cuentas-bancarias` | `configuracion/controller/EmpresaRestController.java:166` (GET), `:188` (POST), `:221` (PUT), `:255` (DELETE) | CRUD usado por la pantalla anterior |
| PDF de factura | `facturacion/service/impl/FacturaPdfServiceImpl.java:329-342` | Imprime entidad, IBAN y número de las cuentas activas ("Por favor depositar en las siguientes cuentas bancarias") |
| Repositorio | `configuracion/repository/CuentaBancariaRepository.java:12,14` | `findByEmpresaIdEmpresaAndActivaTrue`, `findByEmpresaIdEmpresaOrderByOrdenAsc` |

Ningún servicio contable, de cobros ni de pagos a proveedor usa `CuentaBancaria`. El cobro (`Pago`) guarda el banco y la cuenta como texto libre (`facturacion/model/Pago.java:150-157`, formulario `templates/modules/facturacion/pagos/form.html:241-249`), sin relación con `cuenta_bancaria` ni con el asiento. `PagoProveedor` no tiene ningún campo de banco o cuenta (`proveedores/model/PagoProveedor.java`, solo `metodoPago` en la línea 69).

---

## 6. Reglas actuales del código sobre la jerarquía

### 6.1 Asientos: ¿se rechazan cuentas que no aceptan movimientos o inactivas?

| Ruta | ¿Valida? | Evidencia |
|---|---|---|
| Asiento manual: `crear` y `actualizar` | Sí, con `validarDetalles` | `ACS:183`, `ACS:221` → `ACS:1462-1481`, que rechaza si `!cuenta.puedeRecibirMovimientos()` (`ACS:1475`) |
| Asiento manual: `agregarDetalle` | Sí | `ACS:255` |
| Regla aplicada | `activa && aceptaMovimientos && !esCuentaDeAgrupacion()` | `CC:173-175` |
| Qué considera "agrupación" | Solo hijas por **FK** (`subcuentas`, relación `cuenta_padre_id`), no por prefijo de código | `CC:165-167`, `CC:100-101` |
| Asientos automáticos (venta, cobro, compra, pago a proveedor, nómina, reversión) | **No.** Ninguno llama a `validarDetalles`; las líneas se arman con `linea(...)` y solo se valida el cuadre | `linea` `ACS:577-590`; `validarCuadre` `ACS:887-893`; guardado directo en `ACS:419`, `525`, `651`, `1032`, `1215`, `1366` |
| Resolución de la cuenta por clave | **No** revisa `activa`, `aceptaMovimientos` ni hijas | `resolverCuenta` `ACS:1429-1433` |
| Contrapartida de CxP manual | Sí: activa, acepta movimientos, tipo EGRESO/ACTIVO y no configurada como banco/caja, CxC, CxP o IVA. **No** revisa que sea hoja | `validarContrapartidaCxpManual` `ACS:1069-1093`; listado `ACS:1046-1053` |

Consecuencia con los datos actuales: 1.1.01, 1.1.02, 2.1.01 y 5.1.01 tienen `acepta_movimientos = 1` y ninguna hija por FK, así que también pasan la validación manual (`puedeRecibirMovimientos` = verdadero) aunque tienen hijas por prefijo.

### 6.2 `CuentaContableService`: creación de hijas y `acepta_movimientos` del padre

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿Impide crear una hija bajo una cuenta con movimientos? | **No.** Solo valida que el padre exista y que el código de la hija empiece con el del padre | `CCS:152-164` (no hay llamada a `tieneMovimientos`, que sí se usa al cambiar código o eliminar: `CCS:188`, `CCS:256`) |
| ¿La validación de prefijo exige el punto separador? | **No.** Usa `startsWith(padre.getCodigo())`, por lo que `1.1.010` pasaría bajo `1.1.01` | `CCS:157` |
| ¿Actualiza `acepta_movimientos` del padre al crear una hija? | **Sí**, si la hija se crea con `cuentaPadre`: `agregarSubcuenta` pone `aceptaMovimientos = false` en el padre (entidad gestionada, se persiste al cerrar la transacción) | `CCS:163` → `CC:192-197` |
| ¿Y al eliminar una hija? | `eliminar` no llama a `eliminarSubcuenta`, por lo que el padre no recupera `aceptaMovimientos = true`; `eliminarSubcuenta` (`CC:203-210`) no tiene llamadores | `CCS:241-264` |
| ¿Impide marcar como "acepta movimientos" una cuenta con hijas? | **No.** `actualizar` copia `aceptaMovimientos` y `activa` sin revisar hijas ni parámetros | `CCS:205-206` |
| ¿Impide desactivar una cuenta usada por `parametros_contables`? | **No** | `cambiarEstado` `CCS:221-233`; `actualizar` `CCS:205` |
| Nivel | Lo recalcula la entidad desde el código en `@PrePersist/@PreUpdate` (puntos + 1). Las filas con nivel incorrecto (sección 2c) se insertaron por SQL; se corregirían al guardarlas por JPA | `CC:147-153` |
| Cuentas creadas por migraciones/SQL | No pasan por estas reglas (por ejemplo, V19 inserta hojas con `cuenta_padre_id = NULL`) | `V19__seed_ventas_cobros_parametros_contables.sql:59-64` |

### 6.3 Parámetros contables: ¿se valida que la cuenta sea hoja?

| Capa | ¿Valida hoja, activa o acepta movimientos? | Evidencia |
|---|---|---|
| Servicio `crear` | **No.** Solo que la clave no exista y que la cuenta exista | `PCS:28-44` (búsqueda de cuenta en `PCS:32-33`) |
| Servicio `actualizar` | **No.** Igual que crear | `PCS:47-68` (cuenta en `PCS:58-59`) |
| Controlador REST | **No.** Pasa `cuentaContableId` al servicio | `PCCtrl:68-93`, `PCCtrl:97-123` |
| Pantalla (`parametros.js`) | **No.** El selector carga `/contabilidad/cuentas/api/todas`, que devuelve todas las cuentas **activas**, incluidas las de agrupación y las que tienen hijas | `src/main/resources/static/modules/contabilidad/js/parametros.js:6-23`; `CCtrl:110-117` → `CCS:45-48` |
| Migraciones de siembra | V19 valida tipo y naturaleza, no que sea hoja | `V19__seed_ventas_cobros_parametros_contables.sql:50-54`, `:72-76` |
| Detalle menor | `crear` busca duplicados con la clave sin normalizar, pero guarda la clave en mayúsculas | `PCS:29` vs. `PCS:36` |

### 6.4 Reportes y consultas que usan `cuenta_padre` o el prefijo de código

| Componente | ¿Usa la jerarquía? | Evidencia |
|---|---|---|
| Balance general (`generarBalanceGeneral`) | **No.** Lista cada cuenta activa del tipo con su saldo propio y suma en plano | `CalcS:207-247`, `calcularSaldosPorTipo` `CalcS:326-345` |
| Estado de resultados | **No.** Mismo mecanismo por período | `CalcS:255-284`, `calcularSaldosPorTipoPeriodo` `CalcS:354` |
| Utilidad del ejercicio | **No.** Suma todas las cuentas INGRESO/EGRESO (incluye inactivas, usa `findByTipo`) | `CalcS:292-318` |
| Balance de comprobación | **No.** Agrupa por `cuenta_id`, ordena por código | `CalcS:179-200`; `DAR:167-173` |
| Libro mayor | **No.** Una cuenta a la vez, sin incluir hijas | `CalcS:126-139` |
| Vista de árbol del plan de cuentas | Usa `nivel = 1` como raíz y la FK para las hijas. Con los datos actuales devuelve como raíces 1.1.01, 1.1.02, 1.1.03, 2, 2.1.01, 5 y 5.1.01; el DTO de lista no incluye subcuentas | `CCR:71-72`; `CCtrl:122-129`; `CCMap:32-61`, `CCMap:130-136`; `src/main/resources/static/modules/contabilidad/js/cuentas.js:44`, `:101` |
| Detalle de cuenta (subcuentas y ruta) | FK `cuenta_padre_id` | `CCtrl:98-99`; `CCR:79-80`; `CCS:339-355` |
| Eliminación de cuenta (bloquea si tiene hijas) | FK (`countSubcuentas`), no prefijo | `CCS:248`; `CCR:127-128` |
| Siguiente código sugerido | **Prefijo** (`findByCodigoStartingWith(codigoPadre + ".")`), cuenta todos los descendientes, no solo las hijas directas | `CCS:302-332`; `CCR:151-152` |
| Selector de cuentas del asiento manual | Endpoint `activas-movimientos`, que en realidad filtra solo `aceptaMovimientos = true` (no `activa`) | `src/main/resources/static/modules/contabilidad/js/asientos.js:246`; `CCtrl:170-177` → `CCS:121-124` |

---

## 7. Movimientos en el dump que dependen de estas cuentas

| Tabla | Filas en el dump (+ V19..V28) |
|---|---|
| `asientos_contables` | 0 |
| `detalles_asiento` | 0 |
| `pagos` | 0 |
| `pagos_proveedor` | 0 |
| `cuentas_por_pagar` | 0 (ninguna con `cuenta_gasto_id`) |
| `factura` | 3 |
| `nominas` | 0 |
| `ordenes_compra` | 0 |

Las 3 facturas están en `estado_pago = PENDIENTE`, sin pagos y sin asiento (`facturas_con_asiento` = 0). Ninguna cuenta contable tiene detalles de asiento (columna "detalles" de la sección 1). En el estado del dump no hay histórico contable que trasladar entre cuentas; los parámetros y el plan de cuentas son la única configuración afectada.

Limitación: el dump es del 2026-10-11. Si producción registró asientos, pagos o CxP después de esa fecha, este conteo no los refleja.

---

## 8. Observaciones

Hechos adicionales, sin proponer el mapeo final.

1. **Mezcla de dos esquemas de codificación.** Las cuentas del dump sin migración (ids 1–5) usan códigos de 3 segmentos con `nivel = 1` y sin padre; las de V16/V19/V25 usan 4 segmentos con `nivel = 4` y sin padre; solo las de V27 forman una jerarquía completa con FK. Las ramas `1`, `1.1`, `3`, `4`, `4.1`, `4.2`, `2.1.02`, `2.1.03`, `5.2` y sus intermedias no existen.
2. **No hay cuentas de CAPITAL.** El balance general (`CalcS:225`) siempre mostrará capital vacío.
3. **El selector de contrapartida de la CxP manual ofrece y preselecciona una cuenta con hijas.** Replicando en SQL el filtro de `obtenerCuentasContrapartidaCxpManual` (`ACS:1046-1053`), las cuentas elegibles son 5.1.01 Compras, 5.1.01.001 Gasto en Sueldos y Salarios, 5.1.02.001 Aporte Patronal Asociación Solidarista y 5.2.01.001 Diferencial Cambiario Perdido. La preselección es CPP_CUENTA_COMPRAS → 5.1.01 (`ACS:1056-1059`), que tiene una hija.
4. **Cuentas de nómina elegibles como gasto de compras.** Por el mismo filtro, 5.1.01.001 (gasto de sueldos) y 5.1.02.001 (aporte patronal) se pueden elegir como contrapartida de una CxP manual.
5. **Tratamiento distinto del cheque.** En cobros, el cheque va a "Cheques por Depositar" (activo en tránsito); en pagos, CPP_BANCO_CHEQUE descuenta del banco al registrar el pago.
6. **Los medios de pago de cobros y de pagos son enumeraciones distintas.** `MetodoPago` sigue el catálogo de Hacienda (sin SINPE ni depósito como valores propios); `MetodoPagoProveedor` sí los distingue. No hay un concepto común de "medio físico" en el código.
7. **La validación de nómina previa al asiento solo cubre las 6 claves base.** `NS:54-61` y `NS:288-291` no incluyen las 4 claves de V27; esas se resuelven bajo demanda en `ACS:1309-1316` y fallan en el momento del asiento si faltan.
8. **La migración V19 deja hojas huérfanas por diseño.** Su procedimiento inserta con `cuenta_padre_id = NULL` y `nivel = 4` fijo (`V19__seed_ventas_cobros_parametros_contables.sql:63`); V25 hace lo mismo para 4.2.01.001 y 5.2.01.001. V27 solo corrige el padre de sus propias tres hojas (`V27__nomina_solidarista_pension_aporte_ase.sql:146-155`).
9. **Textos rotos ya en producción.** Las tres cadenas de la sección 2e vienen del dump; no las introdujo ninguna migración del repositorio.
10. **Datos de `cuenta_bancaria` listos para un vínculo.** Hay 4 cuentas activas (2 bancos × 2 monedas, CRC y USD), con `orden` 1–4. Hoy el plan de cuentas no tiene ninguna cuenta por banco ni por moneda.

---

## Anexo A. Consultas SQL

Ejecutadas con `docker exec -i t013-mysql mysql -uroot -p… facturas_monrachem --default-character-set=utf8mb4 < consulta.sql` sobre el dump con V19..V28 aplicadas (salvo la consulta 0, ejecutada antes de Flyway).

### Consulta 0 — instantánea previa a Flyway (origen)

```sql
-- 0: instantánea tomada tras cargar el dump y ANTES de ejecutar Flyway (para determinar el origen)
SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;
SELECT id, codigo, nombre FROM cuentas_contables ORDER BY codigo;
SELECT id, clave, cuenta_contable_id FROM parametros_contables ORDER BY id;
```

### Consulta 1 — plan de cuentas (sección 1)

```sql
SELECT c.id, c.codigo, c.nombre, c.tipo, c.naturaleza, c.nivel,
       c.activa+0 AS activa, c.acepta_movimientos+0 AS acepta_mov,
       COALESCE(p.codigo,'NULL') AS padre_id_codigo,
       COALESCE(pp.codigo,'—') AS padre_prefijo,
       (SELECT COUNT(*) FROM cuentas_contables h
         WHERE h.codigo LIKE CONCAT(c.codigo,'.%')
           AND LENGTH(h.codigo)-LENGTH(REPLACE(h.codigo,'.','')) = LENGTH(c.codigo)-LENGTH(REPLACE(c.codigo,'.',''))+1) AS hijas_prefijo,
       (SELECT COUNT(*) FROM cuentas_contables h WHERE h.cuenta_padre_id = c.id) AS hijas_por_fk,
       (SELECT COUNT(*) FROM detalles_asiento d WHERE d.cuenta_id = c.id) AS detalles,
       LENGTH(c.codigo)-LENGTH(REPLACE(c.codigo,'.',''))+1 AS segmentos,
       HEX(c.nombre) AS nombre_hex
FROM cuentas_contables c
LEFT JOIN cuentas_contables p ON p.id = c.cuenta_padre_id
LEFT JOIN cuentas_contables pp ON pp.codigo = SUBSTRING(c.codigo,1,LENGTH(c.codigo)-LENGTH(SUBSTRING_INDEX(c.codigo,'.',-1))-1)
ORDER BY c.codigo;
SELECT COUNT(*) AS total_cuentas FROM cuentas_contables;
```

### Consulta 2 — inconsistencias (a)–(d) (sección 2)

```sql
-- 2a: cuentas con hijas (por prefijo o por FK) que aceptan movimientos
SELECT c.codigo, c.nombre, c.acepta_movimientos+0 AS acepta_mov,
  (SELECT GROUP_CONCAT(h.codigo ORDER BY h.codigo) FROM cuentas_contables h WHERE h.codigo LIKE CONCAT(c.codigo,'.%')) AS descendientes_prefijo,
  (SELECT COUNT(*) FROM cuentas_contables h WHERE h.cuenta_padre_id = c.id) AS hijas_fk
FROM cuentas_contables c
WHERE c.acepta_movimientos = 1
  AND (EXISTS (SELECT 1 FROM cuentas_contables h WHERE h.codigo LIKE CONCAT(c.codigo,'.%'))
       OR EXISTS (SELECT 1 FROM cuentas_contables h WHERE h.cuenta_padre_id = c.id))
ORDER BY c.codigo;
-- 2b: cuenta_padre_id NULL o distinto del padre por prefijo (excluye cuentas de un segmento)
SELECT c.codigo, COALESCE(p.codigo,'NULL') AS padre_fk,
       SUBSTRING(c.codigo,1,LENGTH(c.codigo)-LENGTH(SUBSTRING_INDEX(c.codigo,'.',-1))-1) AS prefijo_esperado,
       COALESCE(pp.codigo,'— (no existe)') AS padre_prefijo
FROM cuentas_contables c
LEFT JOIN cuentas_contables p ON p.id = c.cuenta_padre_id
LEFT JOIN cuentas_contables pp ON pp.codigo = SUBSTRING(c.codigo,1,LENGTH(c.codigo)-LENGTH(SUBSTRING_INDEX(c.codigo,'.',-1))-1)
WHERE c.codigo LIKE '%.%' AND (p.id IS NULL OR pp.id IS NULL OR p.id <> pp.id)
ORDER BY c.codigo;
-- 2c: nivel distinto del número de segmentos del código
SELECT c.codigo, c.nombre, c.nivel, LENGTH(c.codigo)-LENGTH(REPLACE(c.codigo,'.',''))+1 AS segmentos
FROM cuentas_contables c
WHERE c.nivel <> LENGTH(c.codigo)-LENGTH(REPLACE(c.codigo,'.',''))+1
ORDER BY c.codigo;
-- 2d: cuentas cuyo tipo/naturaleza difiere de la de su ancestro por prefijo más cercano,
--     o cuyo nombre semántico no corresponde a la serie (revisión manual sobre esta lista)
SELECT c.codigo, c.nombre, c.tipo, c.naturaleza, a.codigo AS ancestro, a.nombre AS nombre_ancestro, a.tipo AS tipo_anc, a.naturaleza AS nat_anc
FROM cuentas_contables c
JOIN cuentas_contables a ON c.codigo LIKE CONCAT(a.codigo,'.%')
WHERE NOT EXISTS (SELECT 1 FROM cuentas_contables m WHERE c.codigo LIKE CONCAT(m.codigo,'.%') AND m.codigo LIKE CONCAT(a.codigo,'.%'))
ORDER BY c.codigo;
```

### Consulta 2e — codificación rota (sección 2e)

```sql
-- 2e: textos con codificación rota: palabra con la vocal acentuada perdida ("Crdito", "Dbito"),
--     carácter de reemplazo U+FFFD (EFBFBD) o '?' literal
SELECT 'cuentas_contables.nombre' AS campo, codigo AS ref, nombre AS valor, HEX(nombre) AS hex FROM cuentas_contables
 WHERE LOWER(nombre) REGEXP 'crdito|dbito' OR HEX(nombre) LIKE '%EFBFBD%' OR nombre LIKE '%?%'
UNION ALL
SELECT 'cuentas_contables.descripcion', codigo, descripcion, HEX(descripcion) FROM cuentas_contables
 WHERE LOWER(descripcion) REGEXP 'crdito|dbito' OR HEX(descripcion) LIKE '%EFBFBD%' OR descripcion LIKE '%?%'
UNION ALL
SELECT 'parametros_contables.descripcion', clave, descripcion, HEX(descripcion) FROM parametros_contables
 WHERE LOWER(descripcion) REGEXP 'crdito|dbito' OR HEX(descripcion) LIKE '%EFBFBD%' OR descripcion LIKE '%?%';
-- 2e (complemento): doble codificación UTF-8 ("Ã", "Â") comparada en binario
SELECT 'cuentas_contables' AS tabla, COUNT(*) AS n FROM cuentas_contables
 WHERE HEX(nombre) LIKE '%C383%' OR HEX(nombre) LIKE '%C382%' OR HEX(descripcion) LIKE '%C383%' OR HEX(descripcion) LIKE '%C382%'
UNION ALL
SELECT 'parametros_contables', COUNT(*) FROM parametros_contables
 WHERE HEX(descripcion) LIKE '%C383%' OR HEX(descripcion) LIKE '%C382%';
```

### Consulta 3 — parámetros contables (sección 3)

```sql
SELECT p.id, p.clave, c.codigo, c.nombre, c.tipo, c.naturaleza,
       CASE WHEN c.activa = 0 THEN 'inactiva'
            WHEN EXISTS (SELECT 1 FROM cuentas_contables h WHERE h.codigo LIKE CONCAT(c.codigo,'.%') OR h.cuenta_padre_id = c.id) THEN 'con hijas'
            ELSE 'hoja' END AS clase,
       c.acepta_movimientos+0 AS acepta_mov, p.descripcion
FROM parametros_contables p JOIN cuentas_contables c ON c.id = p.cuenta_contable_id
ORDER BY p.id;
SELECT COUNT(*) AS total_parametros FROM parametros_contables;
```

### Consulta 4 — matriz de medios de pago (sección 4)

```sql
-- 4a: claves de medio de pago (cobros y pagos a proveedor) con su cuenta
SELECT p.clave, c.codigo, c.nombre
FROM parametros_contables p JOIN cuentas_contables c ON c.id = p.cuenta_contable_id
WHERE p.clave LIKE 'COBRO\_CAJA\_%' OR p.clave LIKE 'CPP\_BANCO\_%'
ORDER BY p.clave;
-- 4b: valores permitidos por las columnas enum de metodo_pago
SELECT table_name, column_type FROM information_schema.columns
WHERE table_schema = DATABASE() AND column_name = 'metodo_pago' AND table_name IN ('pagos','pagos_proveedor');
-- 4c: uso real de cada medio en el dump
SELECT 'pagos' AS tabla, metodo_pago, estado, COUNT(*) AS n FROM pagos GROUP BY metodo_pago, estado
UNION ALL
SELECT 'pagos_proveedor', metodo_pago, estado, COUNT(*) FROM pagos_proveedor GROUP BY metodo_pago, estado
ORDER BY 1,2,3;
```

### Consulta 5 — cuentas bancarias enmascaradas (sección 5)

```sql
SELECT cb.id_cuenta_bancaria AS id, cb.entidad, cb.moneda, cb.activa+0 AS activa, cb.orden,
       CASE WHEN cb.cuenta_iban IS NULL OR cb.cuenta_iban = '' THEN '(vacío)' ELSE CONCAT('****', RIGHT(REPLACE(cb.cuenta_iban,' ',''),4)) END AS iban_mask,
       CASE WHEN cb.cuenta_banco IS NULL OR cb.cuenta_banco = '' THEN '(vacío)' ELSE CONCAT('****', RIGHT(REPLACE(REPLACE(cb.cuenta_banco,' ',''),'-',''),4)) END AS numero_mask,
       cb.empresa_id
FROM cuenta_bancaria cb ORDER BY cb.orden, cb.id_cuenta_bancaria;
SELECT COUNT(*) AS total_cuenta_bancaria FROM cuenta_bancaria;
```

### Consulta 7 — movimientos (sección 7)

```sql
SELECT 'asientos_contables' AS tabla, COUNT(*) AS n FROM asientos_contables
UNION ALL SELECT 'detalles_asiento', COUNT(*) FROM detalles_asiento
UNION ALL SELECT 'pagos', COUNT(*) FROM pagos
UNION ALL SELECT 'pagos_proveedor', COUNT(*) FROM pagos_proveedor
UNION ALL SELECT 'cuentas_por_pagar', COUNT(*) FROM cuentas_por_pagar
UNION ALL SELECT 'factura', COUNT(*) FROM factura
UNION ALL SELECT 'nominas', COUNT(*) FROM nominas
UNION ALL SELECT 'cuentas_por_pagar.cuenta_gasto_id NOT NULL', COUNT(*) FROM cuentas_por_pagar WHERE cuenta_gasto_id IS NOT NULL;
SELECT tipo, estado, COUNT(*) AS n FROM asientos_contables GROUP BY tipo, estado ORDER BY tipo, estado;
SELECT c.codigo, COUNT(d.id) AS detalles, COALESCE(SUM(d.debe),0) AS debe, COALESCE(SUM(d.haber),0) AS haber
FROM cuentas_contables c JOIN detalles_asiento d ON d.cuenta_id = c.id GROUP BY c.codigo ORDER BY c.codigo;
SELECT tipo_factura, estado_pago, condicion_venta_fe, medio_pago_fe, moneda_fe, COUNT(*) AS n FROM factura GROUP BY tipo_factura, estado_pago, condicion_venta_fe, medio_pago_fe, moneda_fe;
SELECT estado, COUNT(*) AS n FROM cuentas_por_pagar GROUP BY estado;
SELECT COUNT(*) AS facturas_con_asiento FROM factura f WHERE EXISTS (SELECT 1 FROM asientos_contables a WHERE a.factura_id = f.id_factura);
SELECT COUNT(*) AS ordenes_compra FROM ordenes_compra;
```

### Consulta 8 — observaciones (sección 8)

```sql
-- 8: réplica en SQL del filtro de AsientoContableService.obtenerCuentasContrapartidaCxpManual()
SELECT c.codigo, c.nombre, c.tipo
FROM cuentas_contables c
WHERE c.acepta_movimientos = 1 AND c.activa = 1 AND c.tipo IN ('EGRESO','ACTIVO')
  AND c.id NOT IN (SELECT p.cuenta_contable_id FROM parametros_contables p
                   WHERE p.clave LIKE 'CPP\_BANCO\_%' OR p.clave LIKE 'COBRO\_CAJA\_%'
                      OR p.clave IN ('VENTA_CXC_CLIENTES','CPP_CUENTA_POR_PAGAR','CPP_IVA_CREDITO_FISCAL'))
ORDER BY c.codigo;
-- 8b: cuentas devueltas por findCuentasRaiz() (nivel = 1)
SELECT codigo, nombre FROM cuentas_contables WHERE nivel = 1 ORDER BY codigo;
-- 8c: distribución por tipo
SELECT tipo, COUNT(*) AS n FROM cuentas_contables GROUP BY tipo ORDER BY tipo;
-- 8d: cuentas compartidas por más de una clave
SELECT c.codigo, c.nombre, GROUP_CONCAT(p.clave ORDER BY p.clave) AS claves, COUNT(*) AS n
FROM parametros_contables p JOIN cuentas_contables c ON c.id = p.cuenta_contable_id
GROUP BY c.codigo, c.nombre HAVING COUNT(*) > 1 ORDER BY c.codigo;
```

## Anexo B. Entorno reproducible

```bash
# Contenedor aislado (sin volúmenes, solo en localhost)
docker run -d --name t013-mysql --tmpfs /var/lib/mysql:rw \
  -e MYSQL_ROOT_PASSWORD=<clave-local> -e MYSQL_DATABASE=facturas_monrachem \
  -p 127.0.0.1:13313:3306 mysql:8.0.32
docker exec -i t013-mysql mysql -uroot -p<clave-local> facturas_monrachem < facturas_monrachem_20261011_173406.sql

# V19..V28 con la API de Flyway 11.7.2 (classpath offline del proyecto), sin arrancar la aplicación,
# sobre una copia de src/main/resources/db/migration:
#   Flyway.configure().dataSource("jdbc:mysql://127.0.0.1:13313/facturas_monrachem", "root", <clave-local>)
#         .locations("filesystem:<copia de db/migration>").load().migrate();
# Resultado: initialVersion=18 targetVersion=28 executed=10 success=true

# Limpieza (solo por nombre exacto)
docker rm -f t013-mysql
```
