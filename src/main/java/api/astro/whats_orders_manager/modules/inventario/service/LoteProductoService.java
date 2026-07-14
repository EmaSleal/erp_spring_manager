package api.astro.whats_orders_manager.modules.inventario.service;

import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;

import java.time.LocalDate;
import java.util.List;

public interface LoteProductoService {

    LoteProducto crearLote(
        Integer productoId,
        String codigo,
        Integer cantidad,
        LocalDate fechaVencimiento,
        Proveedor proveedor
    );

    List<LoteProducto> findByProducto(Integer productoId);

    List<LoteProducto> findLotesPorVencer(int diasAlerta);

    int marcarVencidos();
}
