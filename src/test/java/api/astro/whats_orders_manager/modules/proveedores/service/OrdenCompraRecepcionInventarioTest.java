package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.inventario.enums.TipoMovimientoInventario;
import api.astro.whats_orders_manager.modules.inventario.service.LoteProductoService;
import api.astro.whats_orders_manager.modules.inventario.service.MovimientoInventarioService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.proveedores.dto.RecepcionDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.DetalleOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.impl.OrdenCompraServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-053 (ADR-066 §5, ADR-061): con inventario.control.habilitado=false recibir una OC no registra entradas ni lotes ni
 * mueve stock, pero sí actualiza lo recibido y el estado (todas las líneas cuentan, tengan o no producto); con true se
 * conserva el comportamiento anterior (registrarEntrada por las líneas con producto).
 */
@DisplayName("T-053 — recepción de OC con y sin control de inventario")
class OrdenCompraRecepcionInventarioTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-20T21:00:00Z"), ZoneId.of("America/Costa_Rica"));

    private OrdenCompraRepository ocRepository;
    private MovimientoInventarioService movimientos;
    private LoteProductoService lotes;
    private Usuario usuario;
    private OrdenCompra oc;
    private DetalleOrdenCompra conProducto;
    private DetalleOrdenCompra sinProducto;

    @BeforeEach
    void setUp() {
        ocRepository = mock(OrdenCompraRepository.class);
        movimientos = mock(MovimientoInventarioService.class);
        lotes = mock(LoteProductoService.class);
        usuario = mock(Usuario.class);
        Producto producto = mock(Producto.class);
        when(producto.getIdProducto()).thenReturn(5);
        oc = OrdenCompra.builder().id(9L).numero("OC-2026-00009").estado(EstadoOrdenCompra.APROBADA).build();
        conProducto = DetalleOrdenCompra.builder().id(1L).ordenCompra(oc).producto(producto).cantidad(10)
                .precioUnitario(new BigDecimal("100.00")).build();
        sinProducto = DetalleOrdenCompra.builder().id(2L).ordenCompra(oc).descripcion("Flete").cantidad(1)
                .precioUnitario(new BigDecimal("500.00")).build();
        oc.getDetalles().add(conProducto);
        oc.getDetalles().add(sinProducto);
        when(ocRepository.findById(9L)).thenReturn(Optional.of(oc));
        when(ocRepository.save(any(OrdenCompra.class))).thenAnswer(i -> i.getArgument(0));
    }

    private OrdenCompraServiceImpl servicio(boolean control) {
        return new OrdenCompraServiceImpl(ocRepository, mock(ProveedorRepository.class), mock(ProductoRepository.class),
                movimientos, lotes, mock(TipoCambioService.class),
                mock(api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository.class), RELOJ, control);
    }

    private RecepcionDTO recepcion(int cantProducto, int cantSinProducto, String lote) {
        return RecepcionDTO.builder().ordenCompraId(9L).items(List.of(
                new RecepcionDTO.ItemRecepcionDTO(1L, cantProducto, lote, null, null, null),
                new RecepcionDTO.ItemRecepcionDTO(2L, cantSinProducto, null, null, null, null))).build();
    }

    @Test
    @DisplayName("control deshabilitado: no se crean movimientos ni lotes; lo recibido y el estado sí se actualizan (todas las líneas)")
    void sinControl_noMueveStock() {
        OrdenCompra r = servicio(false).recibirParcial(recepcion(10, 1, "L-1"), usuario);

        verifyNoInteractions(movimientos);
        verifyNoInteractions(lotes);
        assertThat(conProducto.getCantidadRecibida()).isEqualTo(10);
        assertThat(sinProducto.getCantidadRecibida()).isEqualTo(1);
        assertThat(r.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
        assertThat(r.getFechaRecepcion()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    @DisplayName("control deshabilitado: una recepción parcial deja la OC RECIBIDA_PARCIAL y la siguiente la completa")
    void sinControl_parcialYCompleta() {
        OrdenCompraServiceImpl servicio = servicio(false);
        OrdenCompra r = servicio.recibirParcial(recepcion(4, 1, null), usuario);
        assertThat(r.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA_PARCIAL);
        assertThat(r.getFechaRecepcion()).isNull();
        assertThat(conProducto.getCantidadRecibida()).isEqualTo(4);

        r = servicio.recibirParcial(recepcion(20, 0, null), usuario); // se limita a lo pendiente (6)
        assertThat(conProducto.getCantidadRecibida()).isEqualTo(10);
        assertThat(r.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
        verifyNoInteractions(movimientos);
    }

    @Test
    @DisplayName("control deshabilitado: una OC solo de descripciones (sin producto) se completa")
    void sinControl_ocSoloDeServicios() {
        oc.getDetalles().remove(conProducto);
        OrdenCompra r = servicio(false).recibirParcial(RecepcionDTO.builder().ordenCompraId(9L)
                .items(List.of(new RecepcionDTO.ItemRecepcionDTO(2L, 1, null, null, null, null))).build(), usuario);
        assertThat(r.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
        assertThat(r.getFechaRecepcion()).isEqualTo(LocalDate.of(2026, 11, 20));
        verifyNoInteractions(movimientos);
    }

    @Test
    @DisplayName("control habilitado: se mantiene el comportamiento actual (entrada de inventario por la línea con producto, la línea sin producto se omite)")
    void conControl_registraEntrada() {
        OrdenCompra r = servicio(true).recibirParcial(recepcion(10, 1, null), usuario);

        verify(movimientos).registrarEntrada(eq(5), eq(10), eq(new BigDecimal("100.00")),
                eq(TipoMovimientoInventario.ENTRADA_COMPRA), eq("OC"), eq(9L), eq(usuario), anyString(), any());
        assertThat(sinProducto.getCantidadRecibida()).isZero();
        assertThat(r.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA); // solo cuentan las líneas con producto
    }
}
