package api.astro.whats_orders_manager.modules.inventario.service;

import api.astro.whats_orders_manager.modules.inventario.dto.AjusteRequest;
import api.astro.whats_orders_manager.modules.inventario.enums.EstadoAjuste;
import api.astro.whats_orders_manager.modules.inventario.model.AjusteInventario;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;

import java.util.List;

public interface AjusteInventarioService {

    AjusteInventario crear(AjusteRequest request, Usuario usuario);

    AjusteInventario aprobar(Long id, Usuario aprobadoPor);

    AjusteInventario rechazar(Long id, Usuario aprobadoPor);

    List<AjusteInventario> findAll();

    List<AjusteInventario> findByEstado(EstadoAjuste estado);
}
