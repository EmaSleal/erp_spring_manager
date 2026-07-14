package api.astro.whats_orders_manager.modules.inventario.service;

import api.astro.whats_orders_manager.modules.inventario.dto.ResumenInventarioDTO;
import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.producto.model.Producto;

import java.util.List;

public interface ReporteInventarioService {

    List<Producto> getStockCritico();

    List<Producto> getStockBajo();

    List<Producto> getPuntoReorden();

    List<LoteProducto> getLotesPorVencer(int diasAlerta);

    ResumenInventarioDTO getResumen();
}
