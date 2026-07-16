package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface CuentaPorPagarService {

    CuentaPorPagar crear(CuentaPorPagarDTO dto);

    CuentaPorPagar crearDesdeOrdenCompra(Long ordenCompraId);

    CuentaPorPagar findById(Long id);

    List<CuentaPorPagar> findAll();

    List<CuentaPorPagar> findByEstado(EstadoCuentaPorPagar estado);

    List<CuentaPorPagar> findVencidas();

    int actualizarVencidas();

    Set<Long> findOrdenCompraIdsConCpp();

    void aplicarPago(CuentaPorPagar cuenta, BigDecimal monto);

    void revertirPago(CuentaPorPagar cuenta, BigDecimal monto);
}
