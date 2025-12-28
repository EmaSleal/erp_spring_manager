package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.models.records.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.services.impl.UserDetailsServiceImpl;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class LineaFacturaServiceImpl implements LineaFacturaService {
    @Autowired
    private LineaFacturaRepository lineaFacturaRepository;


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
        return true;
    }

    @Override
    public Boolean existsByFacturaId(Integer id) {
        Long count = lineaFacturaRepository.existsByFacturaId(id);
        return count != null && count > 0;
    }
}