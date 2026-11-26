package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.proveedores.dto.ProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.ProveedorItem;

import java.util.List;
import java.util.Map;

public interface ProveedorService {

    Proveedor crear(ProveedorDTO dto);

    Proveedor actualizar(Long id, ProveedorDTO dto);

    void desactivar(Long id);

    /**
     * Vuelve a consultar el régimen del proveedor en Hacienda (/fe/ae) y lo guarda con su fecha (T-058, ADR-074 §2).
     *
     * @throws java.util.NoSuchElementException el proveedor no existe
     * @throws api.astro.whats_orders_manager.modules.proveedores.exception.RegimenNoVerificableException Hacienda no respondió o
     *         no informó el régimen; lo guardado no cambia
     */
    Proveedor verificarRegimen(Long id);

    List<Proveedor> findAll();

    Proveedor findById(Long id);

    List<Proveedor> findActivos();

    List<Proveedor> buscar(String q);

    List<ProveedorItem> findItems(Long proveedorId, CategoriaProveedor tipo);

    Map<Long, List<Integer>> findItemIdsByCategoria(CategoriaProveedor tipo);
}
