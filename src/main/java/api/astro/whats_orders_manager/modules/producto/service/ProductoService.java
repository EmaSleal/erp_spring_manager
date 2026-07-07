package api.astro.whats_orders_manager.modules.producto.service;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.model.ProductoRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface ProductoService {
    List<Producto> findAll();
    Page<Producto> findAll(Pageable pageable);
    Optional<Producto> findById(Integer id);
    Producto save(Producto producto);
    void deleteById(Integer id);
    List<ProductoRecord> findAllRecords();
    Producto EditProducto(Producto producto);
    long count();

    /**
     * Return all Producto variants linked to the given ArticuloMaestro ID.
     */
    List<Producto> findByArticuloMaestro(Integer maestroId);
}