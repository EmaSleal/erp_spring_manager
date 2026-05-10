package api.astro.whats_orders_manager.modules.facturacion.enums;

import lombok.Getter;

/**
 * Tipo de pago según su aplicación a factura.
 * Define cómo se aplica el pago y su relación con facturas.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Getter
public enum TipoPago {
    
    /**
     * Pago total que cubre el 100% del monto de la factura.
     */
    TOTAL("Pago Total", "El pago cubre la totalidad de la factura"),
    
    /**
     * Pago parcial que cubre solo una parte del monto de la factura.
     */
    PARCIAL("Pago Parcial", "El pago cubre parte del monto de la factura"),
    
    /**
     * Adelanto/anticipo realizado por el cliente sin factura asignada.
     * Se aplicará posteriormente a una o más facturas.
     */
    ADELANTO("Adelanto", "Pago anticipado sin factura asignada"),
    
    /**
     * Aplicación de una nota de crédito como medio de pago.
     */
    NOTA_CREDITO("Nota de Crédito", "Aplicación de nota de crédito como pago");
    
    private final String descripcion;
    private final String detalle;
    
    TipoPago(String descripcion, String detalle) {
        this.descripcion = descripcion;
        this.detalle = detalle;
    }
    
    /**
     * Verifica si el tipo de pago requiere tener una factura asignada.
     * @return true si requiere factura, false si no
     */
    public boolean requiereFactura() {
        return this != ADELANTO;
    }
    
    /**
     * Verifica si el tipo de pago es un adelanto.
     * @return true si es adelanto
     */
    public boolean esAdelanto() {
        return this == ADELANTO;
    }
    
    /**
     * Verifica si el tipo de pago permite aplicarse a una factura existente.
     * @return true si se puede aplicar a factura
     */
    public boolean permiteAplicarAFactura() {
        return this == TOTAL || this == PARCIAL || this == NOTA_CREDITO;
    }
}
