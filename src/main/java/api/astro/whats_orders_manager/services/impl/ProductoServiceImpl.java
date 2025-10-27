package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Producto;
import api.astro.whats_orders_manager.models.ProductoRecord;
import api.astro.whats_orders_manager.repositories.ProductoRepository;
import api.astro.whats_orders_manager.services.ProductoService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoServiceImpl implements ProductoService {
    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    public List<Producto> findAll() { return productoRepository.findAll(); }

    @Override
    public Page<Producto> findAll(Pageable pageable) { 
        return productoRepository.findAll(pageable); 
    }

    @Override
    public Optional<Producto> findById(Integer id) { return productoRepository.findById(id); }

    @Override
    public Producto save(Producto producto) { return productoRepository.save(producto); }

    @Override
    @Transactional
    public void deleteById(Integer id) { productoRepository.desactivarProducto(id); }

    @Override
    public List<ProductoRecord> findAllRecords() {
        return productoRepository.findAllRecords();
    }

    @Override
    public Producto EditProducto(Producto producto) {
        var userId = userDetailsService.getCurrentUserID()
                .orElseThrow(() -> new RuntimeException("No se pudo obtener el ID del usuario actual"));

        Optional<Producto> existingProducto = productoRepository.findById(producto.getIdProducto());
        if (existingProducto.isPresent()) {
            Producto updatedProducto = existingProducto.get();
            updatedProducto.setDescripcion(producto.getDescripcion());
            updatedProducto.setPrecioInstitucional(producto.getPrecioInstitucional());
            updatedProducto.setPrecioMayorista(producto.getPrecioMayorista());
            updatedProducto.setUpdateBy(userId);
            // Set the update date to the current Timestamp
            updatedProducto.setUpdateDate(new java.sql.Timestamp(System.currentTimeMillis()));
            return productoRepository.save(updatedProducto);
        }
        return null; // or throw an exception if the product does not exist
    }

    @Override
    public long count() { return productoRepository.count(); }

}
