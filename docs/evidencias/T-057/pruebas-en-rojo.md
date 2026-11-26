# T-057 — pruebas en rojo y luego en verde (parte 1: criterios 2 a 11 y 14, y la parte de emisión del 17)

Las pruebas se escribieron antes del código de producción. Comando del paso en rojo (desde la copia sin `.env`):

```
DB_URL=jdbc:mysql://127.0.0.1:1/none ./mvnw -o test-compile
```

## Paso en rojo

Con todas las clases de prueba nuevas y ninguna clase de producción nueva, la compilación de pruebas falla (extracto):

```
VentaRegimenFronteraIT.java:[33,56]      cannot find symbol  class PoliticaEmision          (package ...modules.politicas)
VentaRegimenFronteraIT.java:[34,56]      cannot find symbol  class PoliticaRegimenDocumento (package ...modules.politicas)
VentaRegimenCalculoTest.java:[7,56]      cannot find symbol  class PoliticaImpuestoVenta
PoliticaEmisionTest.java:[40,20]         cannot find symbol  class PoliticaEmision
PoliticaRegimenDocumentoTest.java:[3,68] cannot find symbol  class FacturaRegimenConflictoException (package ...facturacion.exception)
PoliticaRegimenDocumentoTest.java:[41,20] cannot find symbol class PoliticaRegimenDocumento
```

javac se detiene en los símbolos de políticas; el resto de las clases nuevas dependen además de `Factura.setRegimenEmisor`,
`CalculadoraImpuestoService.desglosarLinea(Factura, Producto, BigDecimal)`, `NotaFacturaService.MENSAJE_REGIMEN_SIMPLIFICADO`,
`PdfFiscalDecision.deSimplificado/mostrarIva` y el archivo `V60__factura_regimen_emisor.sql`, que tampoco existían.

| Clase de prueba | Criterios | Rojo |
|---|---|---|
| `modules/facturacion/service/VentaRegimenCalculoTest` | 4 | no compila (`PoliticaImpuestoVenta`, `setRegimenEmisor`, `desglosarLinea` con factura) |
| `modules/politicas/PoliticaEmisionTest` | 5, 14, caso 13 | no compila (`PoliticaEmision`) |
| `modules/politicas/PoliticaRegimenDocumentoTest` | 3, 9, casos 1, 2, 12, 13 | no compila (`PoliticaRegimenDocumento`, `FacturaRegimenConflictoException`) |
| `modules/facturacion/service/NotaFacturaRegimenTest` | 6, casos 4 y 5 | no compila (`Factura.setRegimenEmisor`, `MENSAJE_REGIMEN_SIMPLIFICADO`) |
| `modules/facturacion/service/impl/FacturaPdfRegimenSimplificadoTest` | 8 | no compila (`PdfFiscalDecision.deSimplificado`, `mostrarIva`) |
| `modules/contabilidad/service/AsientoVentaCobroServiceTest$RegimenSellado` | 4, casos 7 y 11 | no compila (`Factura.setRegimenEmisor`) |
| `modules/facturacion/PanelFacturacionIT#simplificadoNoCuentaComoSinComprobante` | 7 | no compila (`Factura.setRegimenEmisor`) |
| `modules/facturacion/MigracionFacturaRegimenEmisorIT` | 2 | no existe `db/migration/V60__factura_regimen_emisor.sql` |
| `modules/facturacion/VentaRegimenFronteraIT` | 3, 4, 5, 9, 14, 17, casos 1, 2, 3, 6, 11, 12, 13 | no compila (políticas, `Factura.regimenEmisor`, `FacturaRegimenConflictoException`) |

Pruebas de JavaScript (assertion, no compilación), antes de tocar `accion-envio-hacienda.js`:

```
node --test src/test/js/accion-envio-hacienda.test.mjs
✖ T-057: régimen SIMPLIFICADO → sin botón de envío en ningún estado
  AssertionError: 'emitir' !== null
ℹ pass 6   ℹ fail 1
```

## Paso en verde

Tras implementar:

```
DB_URL=jdbc:mysql://127.0.0.1:1/none ./mvnw -o test -Dtest='VentaRegimenCalculoTest,Politica*Test,NotaFacturaRegimenTest,FacturaPdfRegimenSimplificadoTest,AsientoVentaCobroServiceTest,CalculadoraImpuestoServiceTest,FacturaServiceAsientoTest,TotalesFacturaRutasEquivalentesTest,ArquitecturaRegimenTest,ArquitecturaRelojBaseTest'
Tests run: 123, Failures: 0, Errors: 0, Skipped: 0

./mvnw -o verify -Dit.test='VentaRegimenFronteraIT,MigracionFacturaRegimenEmisorIT,LineaFacturaSincronizacionIT,EstadoFeYConsultaHaciendaIT,BloqueoFiscalFacturaIT,NotaCreditoIT,PanelFacturacionIT,FacturaRepositoryIT' -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false
VentaRegimenFronteraIT 15/15, MigracionFacturaRegimenEmisorIT 3/3, BloqueoFiscalFacturaIT 85/85, PanelFacturacionIT 11/11,
NotaCreditoIT 64/64, EstadoFeYConsultaHaciendaIT 18/18, LineaFacturaSincronizacionIT 7/7, FacturaRepositoryIT 5/5

node --test 'src/test/js/*.test.mjs'
ℹ pass 235   ℹ fail 0
```

Suite unitaria completa (`DB_URL=jdbc:mysql://127.0.0.1:1/none ./mvnw test`): 1738 pruebas; solo fallan las 7 de login ya conocidas
(`UserDetailsServiceImplTest`, `LoginSuccessHandlerTest`, `LoginFailureHandlerTest`).

---

# Parte 2: clave externa, aviso de cierre, sustituta y saldo a favor (criterios 15 a 17, ADR-076 §1, P-132 caso 8)

## Paso en rojo (pruebas escritas antes del código)

`ComprobanteExternoIT` (MySQL, Clock movible) se escribió contra el endpoint y la página aún inexistentes, solo con SQL y
HTTP (sin tipos nuevos), para que el rojo sea de aserción:

```
./mvnw -o verify -Dit.test=ComprobanteExternoIT ...
Tests run: 13, Failures: 11, Errors: 0
  registraClaveValida, claveDuplicada, validacionesDeEntrada, facturasQueNoLaAceptan, conComprobanteDelErp,
  corregirYQuitar, panelSinLaFacturaConClave, avisoElVeinte, avisoElTreintaYUno, listaFiltradaDelAviso, avisoSinPendientes
  (los 2 que pasan, avisoFueraDelRango y avisoSinTramoSiguiente, esperan que el aviso NO aparezca: pasan sin código)
```

Con la migración V61 y los campos de `Factura` ya agregados (necesarios para compilar), pero sin política ni PDF ni JS:

```
PoliticaEmisionTest.general_conClaveExterna_409        Expecting code to raise a throwable   (Tests run: 7, Failures: 1)
FacturaPdfRegimenSimplificadoTest.pdfGeneralConClaveExterna  Expecting actual: ... to contain "Comprobante emitido fuera del ERP"
node --test accion-envio-hacienda.test.mjs
✖ T-057: con clave externa → sin botón de envío en ningún estado   AssertionError: null
VentaRegimenFronteraIT#sustituta_heredaGeneralDelOrigen / #sustituta_heredaSimplificadoDelOrigen   Tests run: 2, Failures: 2
  (sin herencia, la sustituta de enero se sella SIMPLIFICADO y la de julio GENERAL por su fecha)
```

`SaldoFavorIT#aplicarASimplificado_sinMovimientoEnIvaPorPagar` (P-132 caso 8) es una prueba de caracterización: el asiento de la
aplicación de un saldo a favor nunca tocó IVA por pagar, así que pasa desde el primer momento; queda como protección.

## Paso en verde

```
./mvnw -o verify -Dit.test='ComprobanteExternoIT,VentaRegimenFronteraIT,SaldoFavorIT,PanelFacturacionIT,BloqueoFiscalFacturaIT,
  FacturaRepositoryIT,NotaCreditoIT,EstadoFeYConsultaHaciendaIT,MigracionFacturaRegimenEmisorIT,MigracionFacturaComprobanteExternoIT'
ComprobanteExternoIT 13/13, VentaRegimenFronteraIT 17/17 (15 + 2 de sustituta), MigracionFacturaComprobanteExternoIT 2/2,
SaldoFavorIT 28/28 (con el caso 8), PanelFacturacionIT 11/11, BloqueoFiscalFacturaIT 85/85, NotaCreditoIT 64/64,
EstadoFeYConsultaHaciendaIT 18/18, FacturaRepositoryIT 5/5, MigracionFacturaRegimenEmisorIT 3/3

DB_URL=jdbc:mysql://127.0.0.1:1/none ./mvnw -o test
Tests run: 1740, Failures: 5, Errors: 2, Skipped: 1   (solo las 7 de login ya conocidas: UserDetailsServiceImplTest,
LoginSuccessHandlerTest, LoginFailureHandlerTest)

node --test 'src/test/js/*.test.mjs'   ℹ pass 238   ℹ fail 0
```

Un fallo intermedio propio: `VentaRegimenFronteraIT#sinTramo_error` contaba la serie `T057SOLO` y las dos pruebas de sustituta
nuevas dejaban facturas con esa serie; se les dio la serie `T057SUS`.

## Correcciones de revisión (sustituta y RECHAZADO en el aviso)

Rojo antes del código:
```
PoliticaEmisionTest.sustituta_usaLaFechaDelOrigenRaiz   fallo (la sustituta de enero se medía por su propia fecha)
VentaRegimenFronteraIT#sustituta_heredaGeneralDelOrigen  fallo en la aserción de 409 (fecha de la sustituta fijada a enero)
ComprobanteExternoIT#avisoElVeinte, #listaFiltradaDelAviso  fallo: con un RECHAZADO solo se esperaba 3 pendientes y se contaban 2
```
Verde: ver el resultado de la corrida completa en el informe (PoliticaEmision: origen raíz con protección de ciclos;
PREDICADO_PENDIENTE_CIERRE: sin comprobante vigente distinto de RECHAZADO; PREDICADO_SIN_COMPROBANTE sin cambios).
