package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.proveedores.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.model.CuentaPorPagar;

import java.util.List;

public interface CuentaPorPagarService {

    CuentaPorPagar crear(CuentaPorPagarDTO dto);

    CuentaPorPagar crearDesdeOrdenCompra(Long ordenCompraId);

    CuentaPorPagar findById(Long id);

    List<CuentaPorPagar> findAll();

    List<CuentaPorPagar> findByEstado(EstadoCuentaPorPagar estado);

    List<CuentaPorPagar> findVencidas();

    int actualizarVencidas();
}
