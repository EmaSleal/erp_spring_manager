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

Restricción temporal de T-032: una factura con pagos aplicados no admitía nota total (409 con mensaje claro). Levantada en T-048 (§10).

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

Aplicaba a facturas **sin pagos**; desde T-048 (§10) también procede sobre facturas con pagos: el 409 de T-032 («factura con pagos») se levantó y el excedente de la nota sobre el saldo pendiente pasa a un saldo a favor del cliente.

**Pedido.** `POST /api/facturas/notas-credito` con `codigoReferencia` 02 o 14, `razon` de 3 a 180 caracteres y `lineas: [{idLineaFactura, montoAcreditar}]` (monto > 0, dos decimales como máximo, IVA incluido, en la moneda de la factura, sin repetir línea). Los códigos 01 y 13 siguen siendo la nota total y no llevan líneas. Códigos y modalidad incoherentes (02/14 sin líneas, 01/13 con líneas) responden 400 con mensaje fijo. El 06 y el 12 siguen sin habilitar.

**Validaciones 409 (antes del consecutivo; sin nota ni comprobante).** La factura tiene comprobante ACEPTADO del ambiente activo (T-044), no está anulada ni «acreditada totalmente» y no tiene otra nota en trámite (los pagos aplicados ya no bloquean, §10); cada línea pertenece a la factura; por línea, lo acreditado por notas APLICADAS y EN_TRAMITE más el monto nuevo no supera el total de la línea (el `MontoTotalLinea` de su XML); y la suma de las notas de la factura, incluida la nueva, no supera el `TotalComprobante` del comprobante ACEPTADO. Una nota RECHAZADA no cuenta. La nota total (01/13) solo procede si no hay notas parciales APLICADAS ni EN_TRAMITE: «La factura tiene notas parciales; acredite el resto con una nota parcial». El cálculo de topes es único (`NotaCreditoTopes`) y lo usan la emisión y los datos del modal.

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

## 10. Saldo a favor del cliente (T-048, B1, ADR-053/054/055/056/057)

Se levanta el bloqueo de notas de crédito sobre facturas con pagos (`NotaCreditoElegibilidad` ya no lo evalúa y se retiró del botón el texto «llega en una próxima versión»). Lo que la nota acredita por encima del saldo pendiente de la factura no se reembolsa: queda como **saldo a favor** del cliente, que se aplica a mano a otra factura. Fuera de esta fase: devolución en dinero y antigüedad de saldos (B2), cruce de monedas, revaluación NIC 21 y cuentas «no realizado» (P-011), notas 06 (P-094), 12 (P-105) y débito (P-075).

**Cuentas (ADR-054).** Migración `V48__saldo_a_favor_clientes.sql`: grupo `2.1.05 Saldos a favor de clientes` (PASIVO, no acepta movimientos) con las hojas `2.1.05.001` (CRC) y `2.1.05.002` (USD), ACREEDORAS, que aceptan movimientos. Los parámetros contables `SALDO_FAVOR_CLIENTES_CRC` y `SALDO_FAVOR_CLIENTES_USD` quedan apuntando a ellas; si falta uno, la operación falla con «Parámetro contable no configurado: <clave>» y no deja nada a medias. La migración es idempotente frente a códigos o claves existentes.

**Reparto al aplicar la nota (`NotaFacturaEfectos`).** El reparto se calcula cuando la nota pasa a APLICADA, no al emitirla: con la factura bloqueada `FOR UPDATE` (`entityManager.refresh(..., PESSIMISTIC_WRITE)`) se lee su saldo pendiente y `creditoCxc = min(total de la nota, saldo pendiente)`, `excedente = total − creditoCxc`. El asiento de la nota (§9) acredita `creditoCxc` a cuentas por cobrar y el `excedente` a la cuenta 2.1.05.00x de la moneda; la nota guarda `monto_credito_cxc` y `monto_saldo_favor` (el detalle de la nota los devuelve como `montoCreditoCxc`, `montoSaldoFavor` e `idSaldoFavor`, y la pantalla muestra «Reparto: a cuentas por cobrar X · a saldo a favor Y»). Si hay excedente se crea el saldo a favor. Los pagos de la factura no se tocan. Una **nota total sobre una factura pagada** anula la factura como siempre y todo lo ya pagado pasa a saldo a favor (el reparto da `creditoCxc = 0` si no hay saldo pendiente). Con pagos, lo acreditado puede dejar la factura pagada del todo y se recalcula su estado de pago. Mientras una nota está EN_TRAMITE la factura sigue sin admitir pagos nuevos (ADR-037 §3), tampoco por saldo a favor.

**Tablas auxiliares (V48).**
- `saldos_favor_cliente`: un saldo por nota (`UNIQUE nota_factura_id`), con cliente, moneda (CRC o USD), `monto_original`, `saldo_vivo` (`CHECK >= 0`), fecha y tipo de cambio de origen, clave de la nota, factura de origen y estado `VIGENTE` / `AGOTADO` (saldo vivo 0; una reversión lo devuelve a VIGENTE).
- `aplicaciones_saldo_favor`: una fila por aplicación, con el monto, el pago `NOTA_CREDITO` y el asiento que genera, el TC de origen y el de la factura, el soporte del TC del día (`tc_dia_fecha`, `tc_dia_valor`, `tc_dia_tipo`, `tc_dia_fuente`, `tc_dia_origen`), los diferenciales, el usuario y la fecha, el estado `VIGENTE` / `REVERSADA` y los datos de la reversión (razón, usuario, fecha, asiento). Nada se borra.

**Aplicación (`SaldoFavorService.aplicar`).** `POST /api/facturas/saldos-favor/{idSaldo}/aplicaciones` con `{idFactura, monto}` (permiso `PAGO_CREAR`; 201). Bloquea el saldo y luego la factura y valida, con 409 y mensaje fijo: «El saldo a favor no está vigente», «La factura destino está anulada», «La factura destino está acreditada totalmente», «El saldo a favor no se aplica a la factura que lo originó», «El saldo a favor solo se aplica a facturas del mismo cliente», «El saldo a favor solo se aplica a facturas en la misma moneda» (sin cruce de monedas), «El monto debe ser mayor a 0 y tener a lo sumo dos decimales», «El monto excede el saldo vivo del saldo a favor», «El monto excede el saldo pendiente de la factura destino», y la nota EN_TRAMITE en la destino. Crea un pago `NOTA_CREDITO` (método `OTROS`, estado CONFIRMADO, `monto_aplicado` = monto), que se guarda directo y **no** genera el asiento de cobro en caja; ese pago queda fuera de caja, ingresos y conciliación bancaria (`PagoRepository`). El asiento de la aplicación es `AUTOMATICO_PAGO`, enlazado al pago: débito a 2.1.05.00x por `monto × TC origen`, crédito a cuentas por cobrar por `monto × TC de la factura destino` y la diferencia al diferencial realizado. Los pagos `NOTA_CREDITO` están protegidos: `conciliar`, `rechazar` y `anular` de pagos los rechazan («Un pago por saldo a favor no se registra, concilia, rechaza ni anula desde los pagos: …», 400 en `PUT /pagos/{id}/anular`); solo se anulan por la reversión.

**Tipo de cambio y diferencial en USD (ADR-055/056, `docs/contabilidad/POLITICA_TIPO_CAMBIO.md`).** El TC del día sale solo de `tipos_cambio` para la **fecha exacta** de la aplicación (fila activa, tasa de venta); no es editable ni se usa la última tasa anterior. Si falta: 409 «No hay tipo de cambio registrado para <fecha>; regístrelo antes de aplicar el saldo». Se guardan fecha, valor, tipo (VENTA), fuente y origen (API o MANUAL). El diferencial se calcula por tramos, en CRC (`CalculoAplicacionSaldoFavor`):
- tramo del saldo: `diferencialSaldo = monto × (TC día − TC origen)`;
- tramo de cuentas por cobrar: `diferencialCxc = monto × (TC día − TC factura destino)`;
- neto: `diferencialCxc − diferencialSaldo`, que es exactamente la diferencia del asiento (`debe saldo − haber CxC`). Neto positivo: ingreso a 4.2.01.001 «Diferencial cambiario ganado – realizado»; negativo: gasto a 5.2.01.001 «Diferencial cambiario perdido – realizado».
Cada tramo es la diferencia de importes ya redondeados a dos decimales, de modo que el neto cuadra al céntimo. En CRC no hay TC ni diferencial.

**Reversión.** `POST /api/facturas/saldos-favor/aplicaciones/{idAplicacion}/reversion` con `{razon}` (3 a 180 caracteres tras recortar; 400 si no; permiso `PAGO_ANULAR`, el mismo que anular un pago). Anula el pago `NOTA_CREDITO` con el mecanismo de anulación de `PagoServiceImpl` (`anularPagoDeSaldoFavor`), cuyo `revertirAsientoPago` genera una sola vez el asiento espejo del de la aplicación con la fecha de hoy (ADR-023); recalcula el estado de pago de la factura, restituye el saldo vivo (el saldo vuelve a VIGENTE) y guarda razón, usuario, fecha y asiento de reversión. 409 «La aplicación ya fue reversada» y «No se puede reversar: la factura destino está anulada» (su pagado ya pasó a un saldo nuevo con la nota que la anuló; restituir este lo duplicaría).

**Consultas y pantallas.** `GET /api/facturas/saldos-favor?idCliente=&moneda=` (saldos VIGENTES), `GET .../para-factura/{idFactura}` (saldos vigentes del mismo cliente y moneda, sin los que originó esa factura, con su saldo pendiente y `admiteAplicacion`) y `GET .../cliente/{idCliente}` (VIGENTES y AGOTADOS con sus aplicaciones), con `PAGO_VER` o `PAGO_CREAR` según el caso. La interfaz está en `saldo-favor.js`: la opción «Aplicar saldo a favor» del formulario de registro de pago, la sección «Saldos a favor» del estado de cuenta, el historial de pagos del detalle de la factura (los pagos `NOTA_CREDITO` aparecen como «Saldo a favor» con la nota de origen y la acción «Reversar», solo con `PAGO_ANULAR`, que pide la razón) y el reparto en el detalle de la nota. Todo texto dinámico se pinta con `textContent`.

**Factura con pagos sin asiento de venta vigente (revisión 1).** Un saldo a favor solo nace con el asiento de la nota que lo acredita a 2.1.05.00x; sin él, al aplicarlo se debitaría 2.1.05 sin crédito previo y el auxiliar dejaría de cuadrar con el mayor. Por eso, antes del consecutivo, la emisión de una nota (total o parcial) sobre una factura con pagos válidos (incluidos los `NOTA_CREDITO`) y sin asiento de venta vigente responde **409** «La factura tiene pagos y no tiene asiento de venta vigente; regularice su contabilidad antes de emitir la nota», sin crear nota ni comprobante. Defensa adicional en `NotaFacturaEfectos`: si al aplicar la nota hubiera excedente y no hubiera asiento de venta, se lanza «La nota <id> tiene excedente sobre una factura sin asiento de venta vigente: no se crea el saldo a favor sin asiento»; no se crea el saldo y la nota queda EN_TRAMITE (como con un parámetro contable ausente). Una factura sin pagos y sin asiento conserva el comportamiento de T-047 (asiento omitido con un aviso en el log, nota aplicada).

**Caja y pagos del día (revisión 1).** Un pago `NOTA_CREDITO` no es dinero recibido. Las consultas de `PagoRepository` que listan o cuentan pagos como ingreso lo excluyen: `findValidosByFechaPagoRango` (totales de caja e ingresos), `findByFechaPagoBetween` (detalle del reporte de caja, de modo que el detalle suma igual que el total), `findPagosDelDia` y `countPagosDelDia` (pagos del día del listado de pagos), `countByMetodoPagoAndFecha` y `findPagosPendientesConciliacion`. Se mantienen con los `NOTA_CREDITO` las que describen los pagos de una factura o de un cliente (`findPagosValidosByFacturaId`, `calcularTotalPagadoPorFactura`, `findByFacturaId`, `findByClienteId`, `findAllAsDTO`): así el saldo pendiente cuenta lo aplicado y el estado de cuenta y el historial de la factura lo muestran. En el detalle de la factura, `numeroNotaOrigen` del pago `NOTA_CREDITO` es el consecutivo de la nota de origen (la clave de 50 dígitos solo si no hay consecutivo).

**Pendiente.** Ninguno de B1: la devolución en dinero y el reporte de antigüedad están en el §11 (B2).

## 11. Devolución en dinero del saldo a favor y antigüedad (T-049, B2, ADR-058)

**Alcance.** Devolver en dinero, total o parcialmente, un saldo a favor vigente, y un reporte de antigüedad de los saldos vivos. Fuera de alcance: verificar la titularidad de la cuenta con un servicio externo, reversar una devolución PAGADA, revertir saldos antiguos a ingresos, cruce de monedas, revaluación NIC 21 (P-011), notas 06 y 12 y nota de débito.

**Modelo (V49).** `devoluciones_saldo_favor` (entidad `DevolucionSaldoFavor`) y `saldos_favor_cliente.monto_reservado`. La devolución guarda el saldo (FK), el monto en la moneda original, el estado, el medio (CORREO, CARTA, WHATSAPP, OTRO) y la fecha de la solicitud del cliente, el IBAN normalizado, el titular y su identificación normalizada, el usuario y la fecha de cada transición (`solicitado_por`/`fecha_creacion`, `aprobado_por`/`fecha_aprobacion`, `rechazado_por`/`fecha_rechazo`, `anulado_por`/`fecha_anulacion`, `pagado_por`/`fecha_pago`), la razón del rechazo o la anulación (`razon`), la declaración del aprobador (`verifico_cuenta`, `fecha_verificacion`), la cuenta bancaria de la empresa (FK a `cuenta_bancaria`, ADR-018), el número y la fecha del comprobante bancario, el TC de origen, el TC del día (fecha, valor, tipo, fuente, origen), `diferencial_saldo`, `diferencial_neto` y el asiento (FK). Nada se borra.

**Reserva.** Se eligió una columna, no un cálculo: `monto_reservado DECIMAL(12,2) NOT NULL DEFAULT 0` con `CHECK (monto_reservado >= 0 AND saldo_vivo - monto_reservado >= 0)` además del `CHECK saldo_vivo >= 0` de B1. Disponible = `saldo_vivo − monto_reservado`. Solicitar reserva el monto; rechazar y anular lo liberan; pagar lo libera y baja el saldo vivo en el mismo monto (AGOTADO al llegar a 0). La aplicación de B1 (`SaldoFavorService.aplicar`) y las solicitudes nuevas solo pueden usar el disponible: 409 «El monto excede el saldo disponible del saldo a favor (saldo vivo menos lo reservado por devoluciones en trámite)». Toda operación bloquea el saldo FOR UPDATE antes que la devolución (nunca al revés) y la aplicación de B1 lo bloquea antes que la factura, de modo que una solicitud y una aplicación simultáneas que juntas exceden el disponible se serializan: una pasa y la otra recibe 409.

**Ciclo de vida.** SOLICITADA → APROBADA → PAGADA, más RECHAZADA y ANULADA.
- Solicitar (`POST /api/facturas/saldos-favor/{idSaldo}/devoluciones`, permiso `PAGO_CREAR` o `SALDO_FAVOR_DEVOLUCION_APROBAR`): con el saldo bloqueado y VIGENTE, 409 con mensaje fijo y sin crear nada si el monto no es > 0 con a lo sumo dos decimales o excede el disponible; si el IBAN no es costarricense válido (`CR` + 20 dígitos y dígito de control ISO 13616 mod 97; se acepta con o sin espacios y minúsculas y se guarda normalizado: `IbanCostaRica`); si la identificación del titular no es la del cliente del saldo; si el nombre del titular no tiene de 3 a 100 caracteres; si faltan el medio o la fecha de la solicitud o la fecha es futura. La identificación del cliente es `numeroIdentificacion` y, si es nula, la `identificacion` heredada; ambas se comparan sin guiones, espacios ni otros signos y en mayúsculas.
- Aprobar (`.../devoluciones/{id}/aprobar`, `{verificoCuenta: true}`, permiso `SALDO_FAVOR_DEVOLUCION_APROBAR`): 400 sin la declaración «Verifiqué que la cuenta pertenece al cliente». 409 «La devolución debe aprobarla un usuario distinto del que la solicitó y del que emitió la nota».
- Rechazar (`.../rechazar`, `{razon}`, solo desde SOLICITADA, permiso de aprobar) y anular (`.../anular`, `{razon}`, desde SOLICITADA o APROBADA, permiso `PAGO_ANULAR` o el de aprobar): razón de 3 a 180 caracteres (400 si no), liberan la reserva. Cualquier otra transición: 409 «La devolución no está en un estado que permita esta operación».
- Pagar (`.../pagar`, `{idCuentaBancaria, numeroComprobante, fechaComprobante}`, desde APROBADA, permiso de aprobar): 409 si el pagador es el solicitante; la cuenta debe ser una cuenta bancaria activa de la empresa y de la moneda del saldo; el número del comprobante es obligatorio y su fecha no puede ser futura.

**Segregación de funciones.** El permiso `SALDO_FAVOR_DEVOLUCION_APROBAR` (aprobar, rechazar, pagar) es propio y no depende de nombres de rol. El aprobador no es el solicitante (por id de usuario) ni quien emitió la nota de origen. `nota_factura.usuario_creador` es texto (el nombre de autenticación del emisor: nombre o correo), no un id; se compara sin distinguir mayúsculas contra el correo y el nombre del aprobador. El pagador no es el solicitante (el aprobador sí puede pagar). La identidad del usuario autenticado se resuelve por nombre y luego por correo, igual que `tienePermisoPorCodigo`.

**Asiento (solo al pagar).** Una sola transacción, con el saldo bloqueado: el saldo vivo baja, la reserva se libera y se genera un único asiento CONTABILIZADO de tipo propio `AUTOMATICO_DEVOLUCION_SALDO_FAVOR` (V50, que amplía el ENUM `asientos_contables.tipo` agregando el valor al final, sin tocar filas; como V47 con `AUTOMATICO_NOTA_CREDITO`), sin pago ni otro documento enlazado (la devolución guarda el asiento). Una devolución es una salida de dinero, no un cobro: ninguna consulta por `AUTOMATICO_PAGO` la cuenta. El tipo es automático (`esAutomatico()`, ADR-023: no se crea ni reversa desde Contabilidad) y aparece en el filtro y la insignia de la lista de asientos:
- DR `2.1.05.00x` (`SALDO_FAVOR_CLIENTES_<moneda>`) por `monto × TC de origen`;
- CR hoja contable de la cuenta bancaria de la empresa (ADR-018, bajo 1.1.01.002) por `monto × TC del día`;
- la diferencia, a `DIFERENCIAL_CAMBIARIO_INGRESO` (haber, si el débito excede al crédito: ganancia) o `DIFERENCIAL_CAMBIARIO_GASTO` (debe: pérdida), el diferencial realizado «– realizado» de ADR-055.
Se guarda un solo tramo, `diferencial_saldo = monto × (TC día − TC origen)`, y `diferencial_neto = débito − crédito` del asiento (positivo = ganancia; es `−diferencial_saldo`), con importes redondeados a dos decimales (`CalculoDevolucionSaldoFavor`). En USD el TC del día sale solo de `tipos_cambio` para la fecha del pago (ADR-056 §3, tasa de venta, fila activa); si falta, 409 «No hay tipo de cambio registrado para <fecha>; regístrelo antes de pagar la devolución» y no se crea nada. En CRC no hay TC ni diferencial. Ningún estado anterior a PAGADA genera asiento.

**Una devolución PAGADA no se reversa en B2.** No hay endpoint ni acción para anular o reversar una devolución PAGADA (anular desde PAGADA es 409). Si hubo un error (cuenta equivocada, monto, comprobante), se corrige con un **asiento manual documentado** (módulo de contabilidad, tipo MANUAL o AJUSTE) que revierta o reclasifique el movimiento, con el número de la devolución y el motivo en el concepto, y el saldo vivo se ajusta por la vía autorizada. Reversar una devolución pagada es un trabajo futuro (fuera del alcance de ADR-058).

**Dónde aparece el pago de la devolución.**
- Salida de bancos: no hay un reporte de movimientos bancarios. El crédito aparece en el **libro mayor de la hoja contable de la cuenta bancaria** (`GET /contabilidad/reportes/libro-mayor?cuentaId=…`, `CalculoContableService.obtenerLibroMayor`) (`DevolucionSaldoFavorIT.salidaDeBancosYNoEnCaja`).
- No aparece como cobro en el reporte de caja ni en los pagos del día: la devolución no es un `Pago` (no hay fila en `pagos`), así que `PagoServiceImpl.buildReporteCaja`, `findByFechaPagoBetween`, `findPagosDelDia` y demás consultas de pagos no la ven (la prueba compara el reporte de caja antes y después).

**Comprobante bancario.** El sistema no tiene almacenamiento genérico de adjuntos (solo logo, favicon y sello de la empresa): el comprobante queda solo con **número y fecha**. Pendiente aparte: adjuntar el archivo del comprobante cuando exista almacenamiento de adjuntos.

**Antigüedad de saldos a favor.** `GET /reportes/saldos-favor-antiguedad?fechaCorte=yyyy-MM-dd` (hoy por defecto; módulo de reportes, ADMIN y USER) con exportaciones `GET /reportes/saldos-favor-antiguedad/exportar/{excel|csv|pdf}` y datos JSON en `GET /reportes/api/saldos-favor-antiguedad`. Muestra los saldos VIGENTES con saldo vivo por cliente y moneda (sin mezclar monedas) en los tramos 0-7, 8-30, 31-90, 91-180 y más de 180 días, contados desde `fecha_origen` hasta la fecha de corte (`TramoAntiguedad`), con totales por tramo y moneda y las columnas de reservado y disponible. Un saldo con `fecha_origen` posterior a la fecha de corte no aparece (ninguna edad es negativa); una fecha de corte futura responde HTTP 400 con el mensaje fijo «La fecha de corte no puede ser futura» en la página, el JSON y las tres exportaciones. Los montos son el saldo vivo de hoy, no el de la fecha de corte; la página y cada exportación lo rotulan: «Importes al saldo vivo de hoy; la fecha de corte solo define la antigüedad». El reporte es solo de lectura: ni expone un endpoint ni un botón para revertir saldos a ingresos ni ninguna otra acción contable (ADR-053 §5, ADR-058 §6; enmienda 1 de T-049).

**Estado de cuenta (API).** `GET /api/facturas/saldos-favor/cliente/{idCliente}` trae por saldo `reservado` y `disponible` y la lista `devoluciones` con su historial (estados, usuarios, fechas, razón, comprobante, TC, diferencial y asiento); `GET .../{idSaldo}/devoluciones` devuelve solo las devoluciones. `GET /api/cuentas-bancarias/activas?moneda=` admite también el permiso de aprobar devoluciones.

**Permiso y migración.** `SALDO_FAVOR_DEVOLUCION_APROBAR` (categoría Pagos, crítico) está en el enum `Permiso`; V49 lo crea (`INSERT IGNORE`) y lo asigna al rol ADMIN. Los roles son ADMIN, GERENTE, VENDEDOR, USER, VISUALIZADOR y CLIENTE: no hay rol de contabilidad, así que no se asigna a otros; se puede conceder a otro rol o usuario con el mecanismo de permisos existente. Pruebas: `IbanCostaRicaTest`, `CalculoDevolucionSaldoFavorTest`, `TramoAntiguedadTest`, `DevolucionSaldoFavorIT`, `MigracionDevolucionSaldoFavorIT`.
