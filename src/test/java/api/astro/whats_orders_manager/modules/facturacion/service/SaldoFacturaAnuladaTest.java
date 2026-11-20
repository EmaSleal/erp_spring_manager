package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.dto.EstadoCuentaClienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPagoFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.FacturaServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.PagoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-047 (review 1, criterio 30): el saldo de una factura anulada es 0 en el cálculo único. */
@DisplayName("T-047 — saldo de una factura anulada")
class SaldoFacturaAnuladaTest {

    private static Factura factura(int id, String total, boolean anulada) {
        Factura f = new Factura();
        f.setIdFactura(id);
        f.setNumeroFactura("F-" + id);
        f.setTotal(new BigDecimal(total));
        f.setMontoAcreditado(BigDecimal.ZERO);
        f.setAnulada(anulada);
        f.setAcreditadaTotalmente(false);
        f.setEstadoPago(EstadoPagoFactura.PENDIENTE);
        return f;
    }

    @Test
    @DisplayName("anulada sin pagos: saldo 0 y no aparece en las pendientes del cliente; la vigente sí")
    void anuladaNoEsPendiente() {
        Factura anulada = factura(1, "500.00", true);
        Factura vigente = factura(2, "800.00", false);
        assertThat(anulada.calcularSaldoPendiente()).isEqualByComparingTo("0");
        assertThat(anulada.admiteRecordatorioDePago()).isFalse();

        FacturaRepository repo = mock(FacturaRepository.class);
        when(repo.findByClienteId(anyInt())).thenReturn(Optional.of(List.of(anulada, vigente)));
        FacturaServiceImpl servicio = new FacturaServiceImpl();
        ReflectionTestUtils.setField(servicio, "facturaRepository", repo);
        FacturaMapper mapper = mock(FacturaMapper.class);
        when(mapper.toPendingDTO(any())).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            return new api.astro.whats_orders_manager.modules.facturacion.dto.FacturaPendienteDTO(
                    f.getIdFactura(), f.getNumeroFactura(), f.getTotal(), f.calcularSaldoPendiente(), "CRC", null);
        });
        ReflectionTestUtils.setField(servicio, "facturaMapper", mapper);

        var pendientes = servicio.obtenerPendientesPorCliente(7);

        assertThat(pendientes).extracting(p -> p.numeroFactura()).containsExactly("F-2");
    }

    @Test
    @DisplayName("estado de cuenta: totalFacturado, saldo y facturas pendientes no cuentan la anulada")
    void estadoDeCuentaSinAnuladas() {
        Factura anulada = factura(1, "500.00", true);
        Factura vigente = factura(2, "800.00", false);
        FacturaRepository facturas = mock(FacturaRepository.class);
        when(facturas.findByClienteId(anyInt())).thenReturn(Optional.of(List.of(anulada, vigente)));
        ClienteRepository clientes = mock(ClienteRepository.class);
        when(clientes.findById(anyInt())).thenReturn(Optional.of(new Cliente()));
        PagoRepository pagos = mock(PagoRepository.class);
        when(pagos.findByClienteId(anyInt())).thenReturn(List.of());
        FacturaMapper facturaMapper = mock(FacturaMapper.class);
        PagoMapper pagoMapper = mock(PagoMapper.class);
        when(pagoMapper.toDTOList(any())).thenReturn(List.of());
        PagoServiceImpl servicio = new PagoServiceImpl();
        ReflectionTestUtils.setField(servicio, "facturaRepository", facturas);
        ReflectionTestUtils.setField(servicio, "clienteRepository", clientes);
        ReflectionTestUtils.setField(servicio, "pagoRepository", pagos);
        ReflectionTestUtils.setField(servicio, "facturaMapper", facturaMapper);
        ReflectionTestUtils.setField(servicio, "pagoMapper", pagoMapper);

        EstadoCuentaClienteDTO e = servicio.buildEstadoCuenta(7);

        assertThat(e.getTotalFacturado()).isEqualByComparingTo("800.00");
        assertThat(e.getSaldoPendiente()).isEqualByComparingTo("800.00");
        assertThat(e.getFacturasPendientes()).isEqualTo(1);
    }
}
