package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.CondicionVentaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MedioPagoFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-039 (criterio 2 y 3): creación de la factura sustituta, razón, 409 y datos del detalle. */
@DisplayName("T-039 — servicio de la factura sustituta")
class FacturaSustitutaServiceTest {

    private FacturaRepository facturas;
    private FacturaService facturaService;
    private LineaFacturaService lineas;
    private SustitucionElegibilidad elegibilidad;
    private FacturaSustitutaService servicio;
    private Factura original;
    private Producto producto;

    private static SustitucionElegibilidad.Evaluacion ok() {
        return new SustitucionElegibilidad.Evaluacion(true, null, null, "00100001030000000003", "15", null);
    }

    @BeforeEach
    void setUp() {
        facturas = mock(FacturaRepository.class);
        facturaService = mock(FacturaService.class);
        lineas = mock(LineaFacturaService.class);
        elegibilidad = mock(SustitucionElegibilidad.class);
        servicio = new FacturaSustitutaService(facturas, facturaService, lineas, elegibilidad);

        Cliente cliente = new Cliente();
        cliente.setIdCliente(3);
        producto = new Producto();
        producto.setIdProducto(11);
        original = new Factura();
        original.setIdFactura(5);
        original.setNumeroFactura("FA01-00005");
        original.setCliente(cliente);
        original.setMonedaFE(MonedaFE.USD);
        original.setTipoCambio(new BigDecimal("512.30000"));
        original.setCondicionVentaFE(CondicionVentaFE.CREDITO);
        original.setPlazoCredito(30);
        original.setMedioPagoFE(MedioPagoFE.TRANSFERENCIA_DEPOSITO);
        LineaFactura l = new LineaFactura();
        l.setIdLineaFactura(100);
        l.setNumeroLinea(1);
        l.setFactura(original);
        l.setProducto(producto);
        l.setCantidad(4);
        l.setPrecioUnitario(new BigDecimal("25.00"));
        l.setSubtotal(new BigDecimal("100.00"));
        original.getLineas().add(l);
        when(facturas.findByIdParaActualizar(5)).thenReturn(Optional.of(original));
        when(elegibilidad.evaluar(original)).thenReturn(ok());
        when(facturaService.save(any())).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            f.setIdFactura(6);
            f.setNumeroFactura("FA01-00006");
            return f;
        });
    }

    @Test
    @DisplayName("copia cliente, moneda, tipo de cambio y condiciones; liga a la original; guarda por save y las líneas por updateLineas")
    void crea_copiaYLiga() {
        Factura nueva = servicio.crear(5, "  Corrige el precio del producto ");

        ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaService).save(captor.capture());
        Factura guardada = captor.getValue();
        assertThat(nueva).isSameAs(guardada);
        assertThat(guardada.getIdFactura()).isEqualTo(6);
        assertThat(guardada.getCliente()).isSameAs(original.getCliente());
        assertThat(guardada.getMonedaFE()).isEqualTo(MonedaFE.USD);
        assertThat(guardada.getTipoCambio()).isEqualByComparingTo("512.3");
        assertThat(guardada.getCondicionVentaFE()).isEqualTo(CondicionVentaFE.CREDITO);
        assertThat(guardada.getPlazoCredito()).isEqualTo(30);
        assertThat(guardada.getMedioPagoFE()).isEqualTo(MedioPagoFE.TRANSFERENCIA_DEPOSITO);
        assertThat(guardada.getFacturaSustituida()).isSameAs(original);
        assertThat(guardada.getCodigoSustitucion()).isEqualTo("15");
        assertThat(guardada.getRazonSustitucion()).isEqualTo("Corrige el precio del producto");
        assertThat(guardada.getComprobantes()).isEmpty();
        // Sin líneas en la cabecera (no repite las salidas de inventario); las líneas van por updateLineas, nuevas
        assertThat(guardada.getLineas()).isEmpty();
        ArgumentCaptor<java.util.List<LineaFacturaR>> enviadas = ArgumentCaptor.forClass(java.util.List.class);
        verify(lineas).updateLineas(enviadas.capture());
        assertThat(enviadas.getValue()).hasSize(1);
        LineaFacturaR copia = enviadas.getValue().get(0);
        assertThat(copia.id_linea_factura()).isNegative();
        assertThat(copia.id_factura()).isEqualTo(6);
        assertThat(copia.numero_linea()).isEqualTo(1);
        assertThat(copia.id_producto()).isEqualTo(11);
        assertThat(copia.cantidad()).isEqualTo(4);
        assertThat(copia.precioUnitario()).isEqualByComparingTo("25.00");
        // La original no se toca
        assertThat(original.getLineas()).hasSize(1);
        assertThat(original.getLineas().get(0).getFactura()).isSameAs(original);
    }

    @Test
    @DisplayName("sin razón usa «Sustituye la factura <número> anulada por la nota <número NC>»")
    void razonPorDefecto() {
        Factura nueva = servicio.crear(5, null);

        assertThat(nueva.getRazonSustitucion())
                .isEqualTo("Sustituye la factura FA01-00005 anulada por la nota 00100001030000000003");
    }

    @Test
    @DisplayName("la razón por defecto se trunca con seguridad a 180 caracteres")
    void razonPorDefecto_seTrunca() {
        String larga = FacturaSustitutaService.razonFinal(" ", "F".repeat(200), "N");

        assertThat(larga).hasSize(180);
    }

    @Test
    @DisplayName("razón de menos de 3 o más de 180 caracteres → 400 y no se crea nada")
    void razonInvalida_400() {
        for (String razon : new String[]{"ab", " a ", "x".repeat(181)}) {
            assertThatThrownBy(() -> servicio.crear(5, razon))
                    .isInstanceOf(NotaFacturaPedidoInvalidoException.class)
                    .hasMessage("La razón debe tener entre 3 y 180 caracteres");
        }
        verify(facturaService, never()).save(any());
        assertThat(FacturaSustitutaService.razonFinal("x".repeat(180), "a", "b")).hasSize(180);
        assertThat(FacturaSustitutaService.razonFinal("abc", "a", "b")).isEqualTo("abc");
    }

    @Test
    @DisplayName("no elegible o con sustituta → 409 con el mensaje fijo y no se crea nada")
    void noElegible_409() {
        when(elegibilidad.evaluar(original)).thenReturn(new SustitucionElegibilidad.Evaluacion(false,
                "La factura ya tiene una sustituta: FA01-00006", null, null, null, null));

        assertThatThrownBy(() -> servicio.crear(5, null))
                .isInstanceOf(NotaFacturaConflictoException.class)
                .hasMessage("La factura ya tiene una sustituta: FA01-00006");
        verify(facturaService, never()).save(any());
    }

    @Test
    @DisplayName("la restricción única de la BD (carrera) se traduce al mismo 409")
    void violacionUnica_mismoMensaje() {
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("uk_factura_una_sustituta_vigente"))
                .when(facturaService).save(any());
        Factura otra = new Factura();
        otra.setNumeroFactura("FA01-00009");
        when(facturas.findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(5)).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> servicio.crear(5, null))
                .isInstanceOf(NotaFacturaConflictoException.class)
                .hasMessage("La factura ya tiene una sustituta: FA01-00009");
    }

    @Test
    @DisplayName("factura inexistente → NoSuchElementException (404)")
    void inexistente() {
        when(facturas.findByIdParaActualizar(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(99, null)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("el detalle trae la bandera, «sustituye» (con la nota) y «sustituida por»")
    void completarDetalle() {
        Factura sustituta = new Factura();
        sustituta.setIdFactura(6);
        sustituta.setFacturaSustituida(original);
        when(elegibilidad.puedeEmitirSustituta(sustituta)).thenReturn(false);
        when(elegibilidad.evaluarSinSustituta(original)).thenReturn(ok());
        Factura tercera = new Factura();
        tercera.setIdFactura(7);
        tercera.setNumeroFactura("FA01-00007");
        when(facturas.findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(6)).thenReturn(Optional.of(tercera));
        FacturaDetalleDTO detalle = new FacturaDetalleDTO();

        servicio.completarDetalle(sustituta, detalle);

        assertThat(detalle.getPuedeEmitirSustituta()).isFalse();
        assertThat(detalle.getSustituye().idFactura()).isEqualTo(5);
        assertThat(detalle.getSustituye().numeroFactura()).isEqualTo("FA01-00005");
        assertThat(detalle.getSustituye().numeroNota()).isEqualTo("00100001030000000003");
        assertThat(detalle.getSustituidaPor().idFactura()).isEqualTo(7);
        assertThat(detalle.getSustituidaPor().numeroFactura()).isEqualTo("FA01-00007");
    }
}
