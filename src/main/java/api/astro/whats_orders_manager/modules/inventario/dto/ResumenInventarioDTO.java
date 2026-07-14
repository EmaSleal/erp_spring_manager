package api.astro.whats_orders_manager.modules.inventario.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResumenInventarioDTO {
    private final int totalProductosActivos;
    private final int productosStockCritico;
    private final int productosStockBajo;
    private final int productosPuntoReorden;
    private final int lotesVencidos;
    private final int lotesPorVencer;
}
