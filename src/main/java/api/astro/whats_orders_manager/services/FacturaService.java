package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public interface FacturaService {
    List<Factura> findAll();
    Page<Factura> findAll(Pageable pageable);
    Optional<Factura> findById(Integer id);
    Factura save(Factura factura);
    void deleteById(Integer id);
    Boolean existsById(Integer id);
    long count();
    long countByFechaToday();
    BigDecimal sumTotalPendiente();
}
