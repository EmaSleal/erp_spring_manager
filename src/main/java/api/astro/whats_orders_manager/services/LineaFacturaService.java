package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.LineaFactura;
import api.astro.whats_orders_manager.models.LineaFacturaR;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface LineaFacturaService {
    List<LineaFactura> findAll();
    Optional<LineaFactura> findById(Integer id);
    LineaFactura save(LineaFactura lineaFactura);
    Boolean deleteById(Integer id);
    List<LineaFacturaR> findLineasByFacturaId(Integer idFactura);
    Boolean updateLineas(List<LineaFacturaR> lineas);
    Boolean existsByFacturaId(Integer id);
}
