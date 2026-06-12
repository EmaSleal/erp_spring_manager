package api.astro.whats_orders_manager.modules.facturacion.service.impl;

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
        return lineaFacturaRepository.findLineasByFacturaId(idFactura);
    }

    @Override
    @Transactional
    public Boolean updateLineas(List<LineaFacturaR> lineas) {

        var userId = userDetailsService.getCurrentUserID()
                .orElseThrow(() -> new RuntimeException("No se pudo obtener el ID del usuario actual"));

        log.info("Actualizando líneas: {}", lineas);
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
                    factura.setSubtotal(subtotal);
                    factura.setIgv(BigDecimal.ZERO);
                    factura.setTotal(subtotal);
                    facturaRepository.save(factura);
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