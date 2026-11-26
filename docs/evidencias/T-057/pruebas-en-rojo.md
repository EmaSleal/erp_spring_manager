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
