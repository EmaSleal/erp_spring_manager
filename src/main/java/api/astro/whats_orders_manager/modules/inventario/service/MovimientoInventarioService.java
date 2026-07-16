package api.astro.whats_orders_manager.modules.inventario.service;

import api.astro.whats_orders_manager.modules.inventario.enums.TipoMovimientoInventario;
import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.inventario.model.MovimientoInventario;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;

import java.math.BigDecimal;
import java.util.List;

public interface MovimientoInventarioService {

    MovimientoInventario registrarSalida(
        Integer productoId,
        Integer cantidad,
        TipoMovimientoInventario tipo,
        String documentoOrigen,
        Long documentoOrigenId,
        Usuario usuario,
        String observaciones
    );

    MovimientoInventario registrarEntrada(
        Integer productoId,
        Integer cantidad,
        BigDecimal costoUnitario,
        TipoMovimientoInventario tipo,
        String documentoOrigen,
        Long documentoOrigenId,
        Usuario usuario,
        String observaciones,
        LoteProducto lote
    );

    List<MovimientoInventario> obtenerKardex(Integer productoId);

    List<MovimientoInventario> obtenerKardexTodos();
}
