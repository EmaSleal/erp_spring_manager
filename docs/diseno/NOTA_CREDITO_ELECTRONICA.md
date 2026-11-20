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

| Código | Nombre | Periodo del efecto (nota 9; solo atribución del IVA desde ADR-050, ver §4 punto 4) | Fuente |
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
4. **Asiento, fecha contable y período fiscal (ADR-050, que reemplaza la regla de fechas de ADR-037 §4 y de ADR-045 §4):** nota total = espejo del asiento de venta; nota parcial = proporcional por línea. Cada nota y cada factura sustituta guarda **por separado** `fecha_contable` (la fecha de su asiento) y `periodo_fiscal_atribucion` (AAAA-MM, el mes al que se atribuye el IVA), fijados al crearla y sin cambio posterior (migración `V45__fecha_contable_y_periodo_fiscal_atribucion.sql`). La **fecha contable es la fecha de emisión del documento para todos los códigos**, incluidos el 13 y el 15; cuando exista el cierre de períodos (P-018) será el primer período abierto, y `AtribucionFiscalService.fechaContable` es el único punto que cambiará. El período de atribución es configuración (`AtribucionFiscalService`, `CodigoReferenciaNota.periodoAtribucion` y `facturacion.notas.atribucion.codigo-15`):

   | Código | Fecha contable (asiento) | Período fiscal de atribución (IVA) |
   |---|---|---|
   | 01, 02, 06, 12 | Fecha de emisión de la nota | Mes de emisión de la nota |
   | 13, 14 | Fecha de emisión de la nota | Mes de la factura original (fecha de emisión de su comprobante ACEPTADO) |
   | Sustituta 07 | Fecha de emisión de la sustituta | Mes de emisión de la sustituta |
   | Sustituta 15 | Fecha de emisión de la sustituta | Mes de la factura original; parámetro `facturacion.notas.atribucion.codigo-15` = `ORIGINAL` (por defecto) o `EMISION` |

   Un cambio en el parámetro solo afecta a los documentos nuevos. Se advierte, sin bloquear, cuando el 13 se emite sobre una factura del mismo mes («La factura es de este mismo mes: el código 01 tiene el mismo efecto»). El reporte de IVA por período de atribución, la rectificativa y la imputación a utilidades acumuladas quedan para v1.2. Fuentes: el contador respondió en P-074 (cerrado, 2026-11-19) que la fecha contable es la fecha del documento y que 13, 14 y 15 se atribuyen al mes original; el análisis está en `docs/referencias/Notas de crédito por error material (códigos 13, 14 y 15, Anexo v4.4)_ fecha contable, período fiscal y diseño del módulo.md`.
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

- **Sustitutos con referencia (P-073): RESUELTO en T-039 (ADR-045).**
  - Modelo: migración `V44__factura_sustituta.sql` (FK `sustituye_factura_id` a `factura`, `razon_sustitucion`, índice único que impide más de una sustituta no anulada por original); la entidad `Factura` expone `facturaSustituida`, `razonSustitucion` y `codigoSustitucion`.
  - Regla: `SustitucionElegibilidad` (factura ANULADA por nota de crédito total APLICADA con código 01 o 13, comprobante de la nota y del origen ACEPTADOS con clave; 07 si la nota es 01, 15 si es 13). `FacturaSustitutaService` crea la sustituta (copia cliente, líneas y condiciones) y `FacturaSustitutaRestController` expone `POST /api/facturas/{id}/sustituta` (409 con mensaje fijo si no procede o si ya hay una sustituta).
  - XML: `XmlGeneratorServiceImpl` agrega `InformacionReferencia` con `InformacionReferenciaXml.construir`: 01 + código 07/15 para la sustituta, y 10 + código 16 («Sustituye comprobante rechazado por Hacienda») cuando la reemisión sigue a un RECHAZADO; si es ambas, 07/15 primero y 16 después. La etapa a de `EnvioHaciendaService` revisa la regla antes del consecutivo (409 sin consumirlo).
  - Contabilidad: `AsientoContableService` fecha el asiento de la sustituta (07 y 15) con su `fecha_contable`, que es su fecha de emisión (ADR-050 §1, T-046; antes el 15 usaba la fecha del comprobante original); el mes del IVA va aparte en `periodo_fiscal_atribucion` (P-074 cerrado). La fecha de emisión del XML es siempre la actual.
  - PDF (P-030): `FacturaPdfServiceImpl` decide con `PdfFiscalDecision` según el comprobante vigente: sin comprobante, GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO o ANULADO es «Documento no fiscal (proforma)» sin clave ni consecutivo; ENVIADO, PROCESANDO, ENVIO_INCIERTO y ERROR_HACIENDA es «Factura electrónica» con «En trámite ante Hacienda»; ACEPTADO es el tipo real (tiquete si es 04); RECHAZADO es «Documento no fiscal (rechazado por Hacienda)». La leyenda MH-DGT-RES-0027-2024 solo sale en los documentos fiscales. Se agregan la marca «ANULADA por la nota …» y «Sustituye a …». El correo manual del comprobante adjunta el PDF solo si está ACEPTADO (409 en otro estado).
  - Inventario de la sustituta (T-039 review 1, ADR-037 §6): la nota total 01/13 no reingresa inventario, así que la mercadería salió una sola vez, con la original (`SALIDA_VENTA`). La sustituta **nunca** registra salida de inventario (guard único en `FacturaServiceImpl.registrarSalidasInventario`, que ya solo corre al crear una factura con líneas en el cuerpo), ni al crearla ni al editarla; cambiar cantidades o productos de la sustituta tampoco mueve stock automáticamente. Si la corrección implica un movimiento físico, se registra un ajuste de inventario manual; la pantalla de edición de la sustituta lo avisa («La sustituta no mueve inventario; si la corrección implica un movimiento físico, registre un ajuste de inventario»). El asiento de ingreso e IVA de la sustituta se mantiene (ADR-045 §4). Hoy el sistema **no genera asiento de costo de venta** para ninguna factura (`TipoAsiento` no tiene tipo de costo; las salidas de inventario no contabilizan), de modo que no hay costo que duplicar; si se implementa, deberá excluir a la sustituta. IT: `NotaCreditoIT.sustituta_noMueveInventario` (01→07 y 13→15).
- **Fecha contable por error material (P-074):** confirmar con el contador.
- **Nota de débito (P-075):** diferida.
- **Prueba real:** el flujo completo debe probarse en el ambiente de pruebas de Hacienda antes de PRODUCCIÓN; las pruebas automáticas usan un cliente simulado.


## 8. Verificación contra el Anexo oficial (2026-11-12)

Fuente: `docs/referencias/ANEXOS_Y_ESTRUCTURAS_V4.4/` (Anexo convertido a Markdown; bitácora al 22/04/2026). Esta sección prevalece sobre §2 y §3 donde difieran.

**Catálogo de la nota 9 (oficial, completo):** 01 Anula documento de referencia · 02 Corrige monto · 04 Referencia a otro documento · 05 Sustituye comprobante provisional por contingencia · 06 Devolución de mercancía · 07 Sustituye comprobante electrónico · 08 Factura endosada · 09 Nota de crédito financiera · 10 Nota de débito financiera · 11 Proveedor no domiciliado · 12 Nota de crédito financiera por exoneración posterior a la facturación · 13 Anula documento de referencia por error material · 14 Corrige monto por error material · 15 Sustituye comprobante electrónico por error material · 16 Sustituye comprobante electrónico rechazado · 17 Pago a comprobante electrónico (exclusivo del recibo electrónico de pago) · 99 Otros.

- **El código 03 no existe.** «Corrige monto» es el 02. Queda resuelta la duda de §3.
- **Periodo del efecto** (nota al pie de la nota 9): 01, 02 y 06 → periodo en que se genera la nota; 13 y 14 → periodo del comprobante que se modifica; 12 → periodo de la nota. Desde ADR-050 (T-046) este periodo decide solo el **período fiscal de atribución** del IVA; la fecha del asiento es siempre la fecha del documento (§4, punto 4).
- **El 15 no es un código de nota de crédito.** Es el código que lleva el comprobante nuevo que reemplaza a uno anulado con 13. Igual el 07, que lleva el comprobante nuevo que reemplaza a uno anulado con 01. ADR-037 §4 mencionaba el 15 junto a 13 y 14; ADR-050 lo resuelve: el 15 (sustituta) se atribuye por defecto al mes de la factura original, con parámetro.

**Nota 10 (tipo de documento de referencia), códigos relevantes:** 01 Factura electrónica · 02 Nota de débito · 03 Nota de crédito · 04 Tiquete · 10 Comprobante electrónico rechazado por el Ministerio de Hacienda · 19 Factura electrónica de exportación · 20 Recibo electrónico de pago.

**Apartado d, InformacionReferencia:**
- Obligatoria en la nota de crédito; admite hasta 10 repeticiones.
- `TipoDocIR` (nota 10), `Numero` (clave numérica o consecutivo, 50), `FechaEmisionIR` (fecha del documento de referencia, con antigüedad máxima de 10 años), `Codigo` (nota 9) y `Razon` (3 a 180 caracteres).
- Hacienda verifica que la clave sea válida y que los datos de la nota (encabezado, tipo de cambio, CAByS, entre otros) correspondan al comprobante referenciado.
- **Tope acumulado:** la suma de las notas de crédito no puede superar el monto del comprobante referenciado más sus notas de débito. T-033 debe controlar el acumulado por factura.

**Impacto en P-073:** la nota 9 dice que el comprobante nuevo que reemplaza a uno anulado «deberá» llevar el código 07 (o 15). Para el sustituto de un rechazado existen el tipo 10 y el código 16, pero el apartado d no declara obligatoria la referencia en la factura electrónica salvo un caso de exoneración. Conclusión: obligatorio para la factura sustituta tras una anulación; para la reemisión tras rechazo, recomendable y a confirmar.
