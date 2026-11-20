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
4. **Asiento, fecha contable y período fiscal (ADR-050, que reemplaza la regla de fechas de ADR-037 §4 y de ADR-045 §4):** nota total = espejo del asiento de venta; nota parcial = proporcional por línea. Cada nota y cada factura sustituta guarda **por separado** `fecha_contable` (la fecha de su asiento) y `periodo_fiscal_atribucion` (AAAA-MM, el mes al que se atribuye el IVA), fijados al crearla y sin cambio posterior (migración `V45__fecha_contable_y_periodo_fiscal_atribucion.sql`). La **fecha contable es la fecha de emisión del documento para todos los códigos**, incluidos el 13 y el 15; cuando exista el cierre de períodos (P-018) será el primer período abierto, y `AtribucionFiscalService.fechaContable` es el único punto que cambiará. El período de atribución es configuración (`AtribucionFiscalService`): cada código con regla tiene su propiedad `facturacion.notas.atribucion.codigo-NN` (`ORIGINAL` o `EMISION`, variable de entorno `FACTURACION_NOTAS_ATRIBUCION_CODIGO_NN`), declarada en `application.yml` con el valor por defecto de la tabla; `CodigoReferenciaNota.periodoAtribucion` es el valor por defecto si la propiedad falta y un valor inválido detiene el arranque:

   | Código | Fecha contable (asiento) | Período fiscal de atribución (IVA) |
   |---|---|---|
   | 01, 02, 06, 12 | Fecha de emisión de la nota | Mes de emisión de la nota (por defecto `EMISION`) |
   | 13, 14 | Fecha de emisión de la nota | Mes de la factura original (fecha de emisión de su comprobante ACEPTADO; por defecto `ORIGINAL`) |
   | Sustituta 07 | Fecha de emisión de la sustituta | Mes de emisión de la sustituta (por defecto `EMISION`) |
   | Sustituta 15 | Fecha de emisión de la sustituta | Mes de la factura original (por defecto `ORIGINAL`; `EMISION` = mes de la sustituta) |

   Un cambio en una propiedad solo afecta a los documentos nuevos. Se advierte, sin bloquear, cuando el 13 se emite sobre una factura del mismo mes («La factura es de este mismo mes: el código 01 tiene el mismo efecto»). El reporte de IVA por período de atribución, la rectificativa y la imputación a utilidades acumuladas quedan para v1.2. Fuentes: el contador respondió en P-074 (cerrado, 2026-11-19) que la fecha contable es la fecha del documento y que 13, 14 y 15 se atribuyen al mes original; el análisis está en `docs/referencias/Notas de crédito por error material (códigos 13, 14 y 15, Anexo v4.4)_ fecha contable, período fiscal y diseño del módulo.md`.
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
- **Fecha contable por error material (P-074): RESUELTO (cerrado 2026-11-19, ADR-050, T-046).** El contador confirmó que la fecha contable es la fecha del documento y que 13, 14 y 15 se atribuyen al mes de la factura original; ver §4 punto 4.
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

## 9. Nota de crédito parcial (T-047, ADR-051)

Aplica a facturas **sin pagos** (la factura con pagos y el saldo a favor son la tarea siguiente, ADR-051 §6; sigue el 409 de T-032).

**Pedido.** `POST /api/facturas/notas-credito` con `codigoReferencia` 02 o 14, `razon` de 3 a 180 caracteres y `lineas: [{idLineaFactura, montoAcreditar}]` (monto > 0, dos decimales como máximo, IVA incluido, en la moneda de la factura, sin repetir línea). Los códigos 01 y 13 siguen siendo la nota total y no llevan líneas. Códigos y modalidad incoherentes (02/14 sin líneas, 01/13 con líneas) responden 400 con mensaje fijo. El 06 y el 12 siguen sin habilitar.

**Validaciones 409 (antes del consecutivo; sin nota ni comprobante).** La factura tiene comprobante ACEPTADO del ambiente activo (T-044), no está anulada ni «acreditada totalmente», no tiene otra nota en trámite ni pagos aplicados; cada línea pertenece a la factura; por línea, lo acreditado por notas APLICADAS y EN_TRAMITE más el monto nuevo no supera el total de la línea (el `MontoTotalLinea` de su XML); y la suma de las notas de la factura, incluida la nueva, no supera el `TotalComprobante` del comprobante ACEPTADO. Una nota RECHAZADA no cuenta. La nota total (01/13) solo procede si no hay notas parciales APLICADAS ni EN_TRAMITE: «La factura tiene notas parciales; acredite el resto con una nota parcial». El cálculo de topes es único (`NotaCreditoTopes`) y lo usan la emisión y los datos del modal.

**Foto de líneas.** Cada línea conserva producto, CAByS, unidad, descripción y cantidad de la línea original y la referencia (`linea_factura_id`). Base e IVA salen del monto con IVA incluido y la tarifa de esa línea (ADR-009): base = monto / (1 + tarifa) al céntimo, IVA = monto − base, de modo que base + IVA es exactamente el monto pedido; precio unitario = base / cantidad a 5 decimales; los totales de la nota son la suma de sus líneas. El XML (`NotaCreditoElectronica` v4.4) lleva la misma `InformacionReferencia` que la nota total (TipoDocIR 01, clave, fecha original, código) y el resumen es la suma de las líneas.

**Atribución y fecha (ADR-050).** El 02 atribuye al mes de la nota y el 14 al mes de la factura original; la fecha contable es siempre la de la nota (`AtribucionFiscalService`).

**Efectos al ser ACEPTADA (`NotaFacturaEfectos`).** Asiento propio de la nota (ya no se revierte el asiento de venta; los asientos de reversión de las notas 1 y 2 de producción no se tocan): débito a la cuenta de la base según el código, débito a IVA por pagar por el impuesto y crédito a cuentas por cobrar por el total, a los montos de la nota × el tipo de cambio de la factura (el del asiento de venta, ADR-011), con la fecha contable de la nota, enlazado a la nota y a la factura; el residuo de redondeo (un céntimo como máximo) va a la línea de la base. El asiento es de tipo `AUTOMATICO_NOTA_CREDITO` (migración `V47__tipo_asiento_nota_credito.sql`: `asientos_contables.tipo` es un ENUM de MySQL en producción y se amplía conservando sus diez valores), por lo que ninguna consulta por tipo `AUTOMATICO_VENTA` (venta vigente, reportes presentes o futuros) lo cuenta como venta; los asientos ya registrados no se modifican. Sin asiento de venta vigente (facturas históricas) se omite, como antes. Si falta un parámetro contable la aplicación falla con «Parámetro contable no configurado: <clave>» y la nota sigue EN_TRAMITE hasta configurarlo.

| Código | Cuenta del débito por la base (único punto de decisión: `AsientoContableService.resolverCuentaBaseNota`) |
|---|---|
| 01, 02 | parámetro `NOTA_CREDITO_ANULACIONES_DESCUENTOS` → 4.1.02.002 «Anulaciones, descuentos y rebajas sobre ventas» |
| 06 | parámetro `NOTA_CREDITO_DEVOLUCIONES` → 4.1.02.001 «Devoluciones sobre ventas» (solo en el punto de decisión; la 06 no se emite todavía) |
| 13, 14 | la cuenta de ventas del asiento de venta original (línea de crédito que no es IVA por pagar) |
| 12 y cualquier otro | rechazo («Nota 12 por exoneración aún no soportada», P-105); no hay rama por defecto |

Jerarquía (migración `V46__nota_credito_parcial.sql`, ADR-054): `4.1 Ingresos Operativos` → `4.1.01 Ventas` → `4.1.01.001 Ingresos por Ventas`; `4.1.02 Devoluciones y descuentos sobre ventas` (grupo, no acepta movimientos) → `4.1.02.001 Devoluciones sobre ventas` y `4.1.02.002 Anulaciones, descuentos y rebajas sobre ventas` (hojas, INGRESO, DEUDORA, aceptan movimientos). La migración es idempotente frente a códigos existentes, no toca otras cuentas y renombra, solo en el nombre, 4.2.01.001 a «Diferencial Cambiario Ganado – realizado» y 5.2.01.001 a «Diferencial Cambiario Perdido – realizado» (ADR-055 §1).

**Saldo y «acreditada totalmente».** Modelo almacenado: `factura.monto_acreditado` (suma de las notas parciales APLICADAS, IVA incluido) y `factura.acreditada_totalmente`, que `NotaFacturaEfectos` actualiza en la misma transacción en que la nota pasa a APLICADA. `Factura.calcularSaldoPendiente()` = total − acreditado − pagos (0 si está acreditada totalmente); el detalle de la factura, el estado de cuenta del cliente y los pagos usan ese cálculo. Si la suma de las parciales APLICADAS alcanza el `TotalComprobante`, la factura queda «acreditada totalmente»: saldo 0, **sin anular**, sin admitir más notas ni pagos y no elegible para sustituta (que exige factura anulada). Una nota total anula la factura como hasta ahora y no toca `monto_acreditado`.

**Pendiente de pago y recordatorios (revisión 1).** Una factura acreditada totalmente conserva su `estadoPago` (el estado de pago describe solo los pagos; no se agrega un valor al enum). En su lugar, toda vista de «pendiente» usa el mismo cálculo, `Factura.calcularSaldoPendiente()` (o su indicador `acreditada_totalmente`): el filtro `pagada=false` del listado excluye las acreditadas totalmente; el panel de reportes de ventas suma `Factura.getSaldoPendienteEnCRC()` (saldo × tipo de cambio; 0 si está anulada o acreditada totalmente) como `totalPendiente`; el total pendiente del panel (`sumTotalPendiente`) es neto de lo acreditado y excluye anuladas y acreditadas; el gráfico de estado de facturas no cuenta las acreditadas totalmente. Los recordatorios de pago (`RecordatorioPagoScheduler`, `findFacturasConPagoVencido`, el recordatorio y el resumen de WhatsApp) excluyen facturas anuladas, acreditadas totalmente o con saldo 0 (`Factura.admiteRecordatorioDePago()`), y el monto que muestran es el saldo pendiente, no el total.

**API para la pantalla (el servidor decide).** `GET /facturas/detalle/{id}` agrega `puedeEmitirNotaTotal`, `motivoNotaTotalNoDisponible`, `puedeEmitirNotaParcial`, `notaCreditoTotalComprobante`, `notaCreditoTotalAcreditado`, `notaCreditoDisponible` y `notaCreditoLineas[{idLineaFactura, numeroLinea, descripcion, total, acreditado, disponible}]`, más `totalAcreditado`, `saldoPendiente` y `acreditadaTotalmente`. El listado y el detalle de notas llevan `parcial`. El PDF de la nota parcial muestra sus líneas y montos y la modalidad «parcial».
