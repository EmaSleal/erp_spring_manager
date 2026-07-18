package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.proveedores.dto.OrdenCompraDTO;
import api.astro.whats_orders_manager.modules.proveedores.dto.RecepcionDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;

import java.util.List;

public interface OrdenCompraService {

    OrdenCompra crear(OrdenCompraDTO dto, Usuario usuario);

    OrdenCompra aprobar(Long id, Usuario usuario);

    OrdenCompra cancelar(Long id, Usuario usuario);

    OrdenCompra recibirParcial(RecepcionDTO dto, Usuario usuario);

    List<OrdenCompra> findAll();

    OrdenCompra findById(Long id);

    List<OrdenCompra> findByEstado(EstadoOrdenCompra estado);
}
