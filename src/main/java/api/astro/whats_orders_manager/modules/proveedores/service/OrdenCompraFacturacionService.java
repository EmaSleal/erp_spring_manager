package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.dto.OrdenCompraFacturacionDTO;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Vista de una orden de compra para facturar (T-053, ADR-066 §4): total, facturado (Σ de sus CxP no anuladas), pendiente de
 * facturar y exceso, calculados al consultar (no se guardan, así anular una CxP los recalcula). Arma un DTO dentro de la
 * transacción, sin entidades con relaciones diferidas.
 */
@Service
@RequiredArgsConstructor
public class OrdenCompraFacturacionService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final CuentaPorPagarRepository cuentaRepository;
    private final AsientoContableService asientoService;

    @Transactional(readOnly = true)
    public OrdenCompraFacturacionDTO detalle(Long ordenCompraId) {
        OrdenCompra oc = ordenCompraRepository.findById(ordenCompraId)
            .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + ordenCompraId));
        List<CuentaPorPagar> cuentas = cuentaRepository.findByOrdenCompraOrderByIdAsc(oc);

        BigDecimal total = oc.getTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal facturado = cuentas.stream().filter(c -> c.getEstado() != EstadoCuentaPorPagar.ANULADA)
            .map(CuentaPorPagar::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal diferencia = total.subtract(facturado);
        BigDecimal pendiente = diferencia.signum() > 0 ? diferencia : BigDecimal.ZERO.setScale(2);
        BigDecimal exceso = diferencia.signum() < 0 ? diferencia.negate() : BigDecimal.ZERO.setScale(2);

        Optional<CuentaContable> propuesta = asientoService.cuentaPropuestaMercaderia();
        var detalles = oc.getDetalles().stream()
            .sorted(Comparator.comparing(d -> d.getId() == null ? Long.MAX_VALUE : d.getId()))
            .map(d -> new OrdenCompraFacturacionDTO.Detalle(d.getId(),
                d.getDescripcion() != null ? d.getDescripcion()
                    : (d.getProducto() != null ? d.getProducto().getDescripcion() : null),
                d.getCantidad(), d.getPrecioUnitario(), d.getTotal()))
            .toList();
        var filas = cuentas.stream()
            .map(c -> new OrdenCompraFacturacionDTO.Cuenta(c.getId(), c.getNumero(), c.getNumeroComprobanteProveedor(),
                c.getFechaComprobante(), c.getMonto(), c.getEstado().name(),
                c.getEstadoAceptacion() != null ? c.getEstadoAceptacion().name() : null))
            .toList();
        MonedaFE moneda = oc.getMoneda() != null ? oc.getMoneda() : MonedaFE.CRC;
        return new OrdenCompraFacturacionDTO(oc.getId(), oc.getNumero(), oc.getProveedor().getId(),
            oc.getProveedor().getNombre(), oc.getProveedor().getDiasCredito(), oc.getEstado().name(), oc.getEstado().admiteFacturar(), moneda.getCodigo(),
            total, facturado, pendiente, exceso,
            propuesta.map(CuentaContable::getIdCuenta).orElse(null),
            propuesta.map(CuentaContable::getCodigo).orElse(null),
            propuesta.map(CuentaContable::getNombre).orElse(null),
            detalles, filas);
    }
}
