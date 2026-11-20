# Política de tipo de cambio

Vigente desde: 2026-11-20 · Decisiones: ADR-004, ADR-011 §1–2, ADR-055, ADR-056 · Base: informe P-103.

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
- Saldos a favor (aplicación y devolución, B1/B2): la tasa sale solo de la tabla para la fecha exacta de la
  operación y no es editable. Si no existe, la operación se bloquea hasta que se registre la tasa de ese día.

## 4. Días sin tasa

- Si el job falla o la aplicación está apagada, la tasa del día debe registrarse manualmente con el valor publicado por
  el BCCR para esa fecha.
- Hoy los documentos generales usan en silencio la última tasa anterior; el aviso está pendiente (P-107).

## 5. Diferencial cambiario

- Realizado (cobros, pagos, aplicación o devolución de saldos a favor): diferencia contra el TC de origen del documento,
  a 4.2.01.001 / 5.2.01.001 «– realizado».
- No realizado (revaluación de cierre mensual, reverso el día 1): pendiente en v1.2 (P-011), contra 4.2.01.002 /
  5.2.01.002.

## 6. Soporte que se conserva

Por aplicación o devolución de saldo a favor: monto en moneda original; TC de origen (fecha y valor); TC de aplicación
(fecha, valor, tipo VENTA, fuente, origen API o MANUAL); diferencial; claves de la nota y de la factura, o comprobante
bancario. Por revaluación: TC de cierre y detalle por saldo. Conservación permanente (mínimo el plazo del art. 51 del
CNPT).

## 7. Responsables

- Registro o corrección de una tasa manual: usuario con permiso de contabilidad; la corrección inactiva la tasa
  anterior, no la borra.
- Revisión de la política: ante cambios normativos o del contador; se registra como ADR.
