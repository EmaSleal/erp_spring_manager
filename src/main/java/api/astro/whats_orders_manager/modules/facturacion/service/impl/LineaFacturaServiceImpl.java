package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import api.astro.whats_orders_manager.modules.seguridad.service.impl.UserDetailsServiceImpl;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LineaFacturaServiceImpl implements LineaFacturaService {
    @Autowired
    private LineaFacturaRepository lineaFacturaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AsientoContableService asientoContableService;

    @Override
    public List<LineaFactura> findAll() { return lineaFacturaRepository.findAll(); }

    @Override
    public Optional<LineaFactura> findById(Integer id) { return lineaFacturaRepository.findById(id); }

    @Override
    public LineaFactura save(LineaFactura lineaFactura) { return lineaFacturaRepository.save(lineaFactura); }

    @Override
    public Boolean deleteById(Integer id) { 
        lineaFacturaRepository.deleteById(id); 
        return true;
    }

    @Override
    public List<LineaFacturaR> findLineasByFacturaId(Integer idFactura) {
        return lineaFacturaRepository.findEntitiesByFacturaId(idFactura)
                .stream()
                .map(LineaFacturaR::new)
                .toList();
    }

    @Override
    @Transactional
    public Boolean updateLineas(List<LineaFacturaR> lineas) {

        var userId = userDetailsService.getCurrentUserID()
                .orElseThrow(() -> new RuntimeException("No se pudo obtener el ID del usuario actual"));

        log.info("Actualizando líneas: {}", lineas);

        // Facturas que aún no tenían líneas (flujo de creación): su asiento de venta se genera al guardar las líneas
        Set<Integer> facturasSinLineasPrevias = lineas.stream()
                .map(LineaFacturaR::id_factura)
                .filter(idFactura -> !existsByFacturaId(idFactura))
                .collect(Collectors.toSet());

        for (LineaFacturaR linea : lineas) {
            lineaFacturaRepository.updateLinea(linea.id_linea_factura(),
                    linea.numero_linea(),
                    linea.id_producto(),
                    linea.cantidad(),
                    linea.precioUnitario().doubleValue(),
                    linea.subtotal().doubleValue(),
                    userId,
                    linea.id_factura());
        }

        // Recalcular totales por factura
        Map<Integer, BigDecimal> subtotalesPorFactura = lineas.stream()
                .collect(Collectors.groupingBy(
                        LineaFacturaR::id_factura,
                        Collectors.reducing(BigDecimal.ZERO, LineaFacturaR::subtotal, BigDecimal::add)
                ));

        subtotalesPorFactura.forEach((idFactura, subtotal) ->
                facturaRepository.findByIdFactura(idFactura).ifPresent(factura -> {
                    if (factura.estaAnulada()) {
                        throw new IllegalStateException("No se pueden modificar las líneas de una factura anulada");
                    }
                    factura.setSubtotal(subtotal);
                    factura.setIgv(BigDecimal.ZERO);
                    factura.setTotal(subtotal);
                    facturaRepository.save(factura);
                    // ADR-008: primera carga de líneas → generar el asiento de venta;
                    // si ya estaba contabilizada → revertir y regenerar su asiento de venta
                    asientoContableService.contabilizarVentaTrasGuardarLineas(factura,
                            facturasSinLineasPrevias.contains(idFactura));
                    log.info("Totales actualizados para factura {}: subtotal={}, total={}", idFactura, subtotal, subtotal);
                })
        );

        return true;
    }

    @Override
    public Boolean existsByFacturaId(Integer id) {
        Long count = lineaFacturaRepository.existsByFacturaId(id);
        return count != null && count > 0;
    }
}