# Nota de crédito electrónica (P-049): diseño

Fecha: 2026-11-12. Decisión: ADR-037. Tareas: T-032 (núcleo y nota total), T-033 (parcial, saldo a favor, inventario), T-034 (pantalla y PDF).

No es asesoría tributaria. Los puntos marcados «validar» deben confirmarse con el contador o contra el Anexo oficial antes de pasar la FE a PRODUCCIÓN.

## 1. Estado actual (prod, 2026-11-12)

- Solo existe `XmlGeneratorServiceImpl.generarXmlNotaCredito` (l.115-143). Ninguna ruta crea, firma, envía ni contabiliza una nota.
- `ComprobanteElectronicoServiceImpl.generarDesdeFactura` fija el tipo FACTURA_ELECTRONICA.
- Ya resuelto por T-025 (ADR-030, V36): una factura tiene N comprobantes con uno vigente (`uk_comprobante_un_vigente_por_factura`); anular o eliminar una factura con comprobante ENVIADO, PROCESANDO, ACEPTADO o ENVIO_INCIERTO responde 409 (`PoliticaMutabilidadFactura`); el comprobante no se borra en cascada.
- Consecuencia: hoy una factura ACEPTADA no tiene ninguna vía de corrección. La nota de crédito es esa vía.

## 2. Defectos del XML actual de nota de crédito

Contrastado con «Anexos y Estructuras v4.4» de Hacienda (bitácora al 22/04/2026; se leyó la primera mitad del documento: encabezado y detalle):

| Defecto | Regla |
|---|---|
| Falta `ProveedorSistemas` | Obligatorio en todos los comprobantes, incluida la NC |
| `<TipoDoc>` y `<FechaEmision>` dentro de InformacionReferencia | En 4.4 las etiquetas son `TipoDocIR` y `FechaEmisionIR` |
| `<Numero>` lleva el número interno de la factura | Debe llevar la clave de 50 dígitos del comprobante referenciado |
| La fecha de referencia es la de la nota | Debe ser la fecha de emisión del comprobante referenciado |
| Código y razón fijos («01», «Anulación») | Código de la nota 9 elegido por el usuario y razón escrita |
| Receptor tomado de la factura viva | En NC los datos del receptor deben coincidir con el comprobante original |
| Moneda y tipo de cambio | Obligatorios en NC |
| CAByS por línea | Obligatorio si el comprobante original lo trae |

Pendiente de verificar contra el XSD y el resto del Anexo (apartado d, notas 9 y 10): posición de InformacionReferencia en el documento, obligatoriedad de cada campo y catálogo exacto.

## 3. Códigos de referencia (nota 9)

Desde el 1 de noviembre de 2026 rigen los códigos 13 a 17 y el ajuste del 12. La versión del comprobante sigue siendo 4.4.

| Código | Nombre | Periodo del efecto | Fuente |
|---|---|---|---|
| 01 | Anula documento de referencia | El de la nota | secundaria |
| 02 / 03 | Corrige monto / corrige texto | El de la nota | **validar**: las fuentes no coinciden en la numeración |
| 04 | Referencia a otro documento | — | secundaria |
| 05 | Sustituye comprobante provisional por contingencia | — | secundaria |
| 06 | Devolución de mercancía | El de la nota | oficial |
| 07 | Sustituye comprobante electrónico | — | oficial |
| 08 | Factura endosada | — | oficial |
| 09 | Nota de crédito financiera | — | oficial |
| 10 | Nota de débito financiera | — | oficial |
| 11 | Proveedor no domiciliado | — | oficial |
| 12 | NC financiera por exoneración posterior a la facturación | El de la nota | oficial + secundaria |
| 13 | Anula documento de referencia por error material | El del comprobante original | oficial + secundaria |
| 14 | Corrige monto por error material | El del comprobante original | oficial + secundaria |
| 15 | Sustituye comprobante electrónico por error material | El del original, dentro de una corrección por error material | oficial + secundaria |
| 16 | Sustituye comprobante electrónico rechazado | — | oficial + secundaria |
| 17 | Pago a comprobante electrónico | Solo recibo electrónico de pago; no válido en NC/ND | secundaria |

Tipos de documento de referencia (nota 10) relevantes: 01 factura electrónica, 10 comprobante rechazado por Hacienda; nuevos 19 y 20 (**validar** su significado).

Fuentes: hacienda.go.cr/docs/ANEXOS_Y_ESTRUCTURAS_V4.4.pdf; PwC y KPMG (boletines 2026); Delfino, CRHoy; guías de Alegra, billingcr y facturaencr.

## 4. Decisiones (ADR-037)

1. **Alcance:** nota de crédito (03), total y parcial. La nota de débito (02) queda diferida; el modelo la admite.
2. **Modelo:** entidad propia `NotaFactura` (tipo CREDITO; DEBITO reservado) con líneas que son una foto de las líneas de la factura. El comprobante electrónico apunta a su documento de origen: factura **o** nota, nunca ambos. Una nota tiene N comprobantes con uno vigente, igual que la factura.
3. **Efectos al quedar ACEPTADA:** saldo, asiento, inventario y estado de la factura se aplican cuando Hacienda acepta la nota. Mientras está en trámite, la factura no admite otra nota. Una nota rechazada no deja efectos.
4. **Asiento:** nota total = espejo del asiento de venta; nota parcial = proporcional por línea. Fecha: la de emisión de la nota para 01, 02, 06 y 12; la de la factura original para 13, 14 y 15. **Validar** con el contador el tratamiento en libros de un mes ya declarado.
5. **Factura con pagos:** lo que exceda el saldo pendiente queda como saldo a favor del cliente, aplicable con `TipoPago.NOTA_CREDITO`. Sin reembolsos automáticos.
6. **Inventario:** solo la devolución de mercancía (06) reingresa stock, por línea.
7. **Anular y eliminar:** el bloqueo ya existe (ADR-030 §5). Se añade que la nota total aceptada es la única vía para anular una factura ACEPTADA y deja la factura anulada.
8. **Fases:** T-032, T-033 y T-034.

## 5. Flujo

```
Factura con comprobante ACEPTADO
  └─ crear nota (código, razón, líneas) ──► NotaFactura EN_TRAMITE
        └─ comprobante 03: generar XML → firmar → enviar → sondear   (misma maquinaria y estados de ADR-030)
              ├─ ACEPTADO  ──► aplicar efectos una sola vez ──► NotaFactura APLICADA
              ├─ RECHAZADO ──► NotaFactura RECHAZADA, sin efectos; se puede emitir otra
              └─ NO_TRANSMITIDO / ENVIO_INCIERTO / ERROR ──► reglas de ADR-030 (reenviar o verificar)
```

## 6. Alcance por tarea

| | T-032 | T-033 | T-034 |
|---|---|---|---|
| Modelo, migración, catálogo de códigos | ✔ | | |
| XML correcto, firma, envío, estados, consecutivo | ✔ | | |
| Nota total (códigos 01 y 13) con asiento y factura anulada | ✔ | | |
| API REST para emitir y consultar | ✔ | | |
| Nota parcial (02/03, 06, 14) | | ✔ | |
| Factura con pagos: saldo a favor | | ✔ | |
| Reingreso de inventario (06) | | ✔ | |
| Pantalla, listado y PDF de la nota | | | ✔ |

Restricción temporal de T-032: una factura con pagos aplicados no admite nota total (409 con mensaje claro) hasta T-033.

## 7. Hallazgos fuera del alcance

- **Sustitutos con referencia (P-073):** desde noviembre de 2026, el comprobante que sustituye a uno rechazado llevaría referencia tipo 10 + código 16, y la factura que reemplaza a una anulada, código 07 (si se anuló con 01) o 15 (si se anuló con 13). La reemisión tras RECHAZADO de ADR-030 no emite referencia. **Validar** si es obligatorio; si lo es, bloquea el paso a PRODUCCIÓN.
- **Fecha contable por error material (P-074):** confirmar con el contador.
- **Nota de débito (P-075):** diferida.
- **Prueba real:** el flujo completo debe probarse en el ambiente de pruebas de Hacienda antes de PRODUCCIÓN; las pruebas automáticas usan un cliente simulado.


## 8. Verificación contra el Anexo oficial (2026-11-12)

Fuente: `docs/referencias/ANEXOS_Y_ESTRUCTURAS_V4.4/` (Anexo convertido a Markdown; bitácora al 22/04/2026). Esta sección prevalece sobre §2 y §3 donde difieran.

**Catálogo de la nota 9 (oficial, completo):** 01 Anula documento de referencia · 02 Corrige monto · 04 Referencia a otro documento · 05 Sustituye comprobante provisional por contingencia · 06 Devolución de mercancía · 07 Sustituye comprobante electrónico · 08 Factura endosada · 09 Nota de crédito financiera · 10 Nota de débito financiera · 11 Proveedor no domiciliado · 12 Nota de crédito financiera por exoneración posterior a la facturación · 13 Anula documento de referencia por error material · 14 Corrige monto por error material · 15 Sustituye comprobante electrónico por error material · 16 Sustituye comprobante electrónico rechazado · 17 Pago a comprobante electrónico (exclusivo del recibo electrónico de pago) · 99 Otros.

- **El código 03 no existe.** «Corrige monto» es el 02. Queda resuelta la duda de §3.
- **Periodo del efecto** (nota al pie de la nota 9): 01, 02 y 06 → periodo en que se genera la nota; 13 y 14 → periodo del comprobante que se modifica; 12 → periodo de la nota.
- **El 15 no es un código de nota de crédito.** Es el código que lleva el comprobante nuevo que reemplaza a uno anulado con 13. Igual el 07, que lleva el comprobante nuevo que reemplaza a uno anulado con 01. ADR-037 §4 menciona el 15 junto a 13 y 14: en la práctica la regla de fecha aplica a 13 y 14.

**Nota 10 (tipo de documento de referencia), códigos relevantes:** 01 Factura electrónica · 02 Nota de débito · 03 Nota de crédito · 04 Tiquete · 10 Comprobante electrónico rechazado por el Ministerio de Hacienda · 19 Factura electrónica de exportación · 20 Recibo electrónico de pago.

**Apartado d, InformacionReferencia:**
- Obligatoria en la nota de crédito; admite hasta 10 repeticiones.
- `TipoDocIR` (nota 10), `Numero` (clave numérica o consecutivo, 50), `FechaEmisionIR` (fecha del documento de referencia, con antigüedad máxima de 10 años), `Codigo` (nota 9) y `Razon` (3 a 180 caracteres).
- Hacienda verifica que la clave sea válida y que los datos de la nota (encabezado, tipo de cambio, CAByS, entre otros) correspondan al comprobante referenciado.
- **Tope acumulado:** la suma de las notas de crédito no puede superar el monto del comprobante referenciado más sus notas de débito. T-033 debe controlar el acumulado por factura.

**Impacto en P-073:** la nota 9 dice que el comprobante nuevo que reemplaza a uno anulado «deberá» llevar el código 07 (o 15). Para el sustituto de un rechazado existen el tipo 10 y el código 16, pero el apartado d no declara obligatoria la referencia en la factura electrónica salvo un caso de exoneración. Conclusión: obligatorio para la factura sustituta tras una anulación; para la reemisión tras rechazo, recomendable y a confirmar.
