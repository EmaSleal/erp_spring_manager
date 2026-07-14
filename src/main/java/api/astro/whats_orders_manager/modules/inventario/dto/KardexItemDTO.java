package api.astro.whats_orders_manager.modules.inventario.dto;

import api.astro.whats_orders_manager.modules.inventario.model.MovimientoInventario;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class KardexItemDTO {

    private final Long id;
    private final String tipo;
    private final Integer cantidad;
    private final BigDecimal costoUnitario;
    private final BigDecimal costoTotal;
    private final Integer stockAnterior;
    private final Integer stockNuevo;
    private final LocalDateTime fecha;
    private final String documentoOrigen;
    private final String observaciones;

    public KardexItemDTO(MovimientoInventario m) {
        this.id = m.getId();
        this.tipo = m.getTipo().name();
        this.cantidad = m.getCantidad();
        this.costoUnitario = m.getCostoUnitario();
        this.costoTotal = m.getCostoTotal();
        this.stockAnterior = m.getStockAnterior();
        this.stockNuevo = m.getStockNuevo();
        this.fecha = m.getFecha();
        this.documentoOrigen = m.getDocumentoOrigen();
        this.observaciones = m.getObservaciones();
    }
}
