package api.astro.whats_orders_manager.modules.facturacion.enums;

/**
 * Estado de pago de una factura (calculado según pagos aplicados).
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
public enum EstadoPagoFactura {
    
    /**
     * Factura sin pagos o con saldo total pendiente.
     */
    PENDIENTE("Pendiente de pago"),
    
    /**
     * Factura con al menos un pago pero saldo pendiente.
     */
    PAGADO_PARCIAL("Pagado parcialmente"),
    
    /**
     * Factura pagada en su totalidad.
     */
    PAGADO_TOTAL("Pagado totalmente"),
    
    /**
     * Factura con pago pendiente y fecha de vencimiento superada.
     */
    VENCIDO("Vencido");
    
    private final String descripcion;
    
    EstadoPagoFactura(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    /**
     * Verifica si la factura está completamente pagada.
     * @return true si está pagada totalmente
     */
    public boolean estaPagada() {
        return this == PAGADO_TOTAL;
    }
    
    /**
     * Verifica si la factura tiene saldo pendiente.
     * @return true si tiene saldo pendiente
     */
    public boolean tieneSaldoPendiente() {
        return this == PENDIENTE || this == PAGADO_PARCIAL || this == VENCIDO;
    }
    
    @Override
    public String toString() {
        return descripcion;
    }
}
