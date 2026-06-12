package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaPendienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
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
    //findByClienteId
    Optional<List<Factura>> findByClienteId(Integer idCliente);

    /**
     * Validates business rules and deletes the factura and its line items.
     *
     * @throws NoSuchElementException when the factura does not exist
     * @throws IllegalStateException  for business rule violations (pagos or entregada)
     */
    void eliminarConValidacion(Integer id);

    /**
     * Updates the delivery state of a factura, persisting via the repository directly
     * to avoid side effects from {@link #save(Factura)}.
     */
    void actualizarEstado(Integer id, Boolean entregado, String descripcion, String fechaEntregaStr);

    /**
     * Returns all facturas for a given cliente that have a pending balance greater than zero.
     */
    List<FacturaPendienteDTO> obtenerPendientesPorCliente(Integer idCliente);
}
