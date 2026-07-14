package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.proveedores.dto.ProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;

import java.util.List;

public interface ProveedorService {

    Proveedor crear(ProveedorDTO dto);

    Proveedor actualizar(Long id, ProveedorDTO dto);

    void desactivar(Long id);

    List<Proveedor> findAll();

    Proveedor findById(Long id);

    List<Proveedor> findActivos();

    List<Proveedor> buscar(String q);
}
