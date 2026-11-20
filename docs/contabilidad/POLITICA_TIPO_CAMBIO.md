# Política de tipo de cambio

Vigente desde: 2026-11-20 · Decisiones: ADR-004, ADR-011 §1–2, ADR-053, ADR-055, ADR-056, ADR-057 · Base: informe P-103 · Implementación de B1: T-048.

## 1. Criterio

Toda conversión de moneda extranjera a colones usa la **tasa de referencia de VENTA del BCCR** de la fecha de la
operación. No se usa la tasa de compra en ningún documento ni asiento.

## 2. Fuente

- Automática: API de indicadores del Ministerio de Hacienda (`api.hacienda.go.cr/indicadores/tc/dolar`), que publica la
  referencia del BCCR. En el soporte se cita como «BCCR, referencia de venta, obtenida vía API de Hacienda».
- Sincronización: diaria a las 06:15 (America/Costa_Rica), solo USD. La API entrega únicamente la tasa del día.
- Manual: un usuario con permiso de contabilidad puede registrar la tasa de una fecha. Una tasa manual activa no la
  reemplaza la sincronización.
- Mejora pendiente: integración directa con el SDDE del BCCR para histórico (P-107).

## 3. Selección de la tasa

- Documentos generales (factura, cobro, orden de compra, CxP, pago a proveedor): se sugiere la tasa activa más reciente
  con fecha ≤ fecha de la operación; el usuario puede modificarla. Cada documento guarda su TC y nunca se recalcula.
  Registrar el origen (tabla o digitado) está pendiente (P-106).
- Nota de crédito y factura sustituta: copian el TC del documento de origen.
- Saldos a favor (aplicación en B1, devolución en B2): la tasa sale solo de la tabla `tipos_cambio` para la fecha exacta
  de la operación (fila activa, tasa de venta) y no es editable; no se usa la última tasa anterior. Solo aplica a USD (en
  CRC no hay tasa ni diferencial). Si no existe, la aplicación se bloquea con el 409 «No hay tipo de cambio registrado
  para <fecha>; regístrelo antes de aplicar el saldo» hasta que se registre la tasa de ese día (`SaldoFavorService.aplicar`,
  `TipoCambioService.obtenerTipoCambioDeLaFecha`). La B2 (devolución) aplicará la misma regla.

## 4. Días sin tasa

- Si el job falla o la aplicación está apagada, la tasa del día debe registrarse manualmente con el valor publicado por
  el BCCR para esa fecha.
- Hoy los documentos generales usan en silencio la última tasa anterior; el aviso está pendiente (P-107).

## 5. Diferencial cambiario

- Realizado (cobros, pagos, aplicación o devolución de saldos a favor): diferencia contra el TC de origen del documento,
  a 4.2.01.001 / 5.2.01.001 «– realizado». En la aplicación de un saldo a favor (B1) se calcula por tramos con el TC del
  día: saldo = monto × (TC día − TC origen del saldo); cuentas por cobrar = monto × (TC día − TC de la factura destino);
  neto = cuentas por cobrar − saldo, igual a la diferencia del asiento. Neto positivo: ingreso (4.2.01.001); negativo:
  gasto (5.2.01.001). La reversión de la aplicación no recalcula: revierte el asiento con un espejo.
- No realizado (revaluación de cierre mensual, reverso el día 1): pendiente en v1.2 (P-011), contra 4.2.01.002 /
  5.2.01.002.

## 6. Soporte que se conserva

Por aplicación o devolución de saldo a favor: monto en moneda original; TC de origen (fecha y valor); TC de aplicación
(fecha, valor, tipo VENTA, fuente, origen API o MANUAL); diferencial; claves de la nota y de la factura, o comprobante
bancario. En B1 esto se guarda en `aplicaciones_saldo_favor` (`tipo_cambio_origen`, `tipo_cambio_factura`, `tc_dia_fecha`,
`tc_dia_valor`, `tc_dia_tipo`, `tc_dia_fuente`, `tc_dia_origen`, `diferencial_saldo`, `diferencial_cxc`,
`diferencial_neto`) y la clave de la nota en `saldos_favor_cliente`; el origen API/MANUAL se deriva de la fuente de la
fila de `tipos_cambio` usada. Por revaluación: TC de cierre y detalle por saldo. Conservación permanente (mínimo el plazo del art. 51 del
CNPT).

## 7. Responsables

- Registro o corrección de una tasa manual: hoy `POST /api/tipos-cambio` exige el rol ADMIN (no existe un permiso de
  contabilidad para esto). La corrección inactiva la tasa anterior de esa fecha y moneda y deja la nueva como activa; no
  borra la anterior (`TipoCambioService.registrar` llama a `TipoCambioRepository.deactivateExisting`, un UPDATE
  `activo = false`) y no hay ningún endpoint de borrado de tipos de cambio. La sincronización automática, en cambio,
  conserva una tasa activa de otra fuente (p. ej. MANUAL) y actualiza en el lugar solo una activa de su misma fuente.
- Revisión de la política: ante cambios normativos o del contador; se registra como ADR.
