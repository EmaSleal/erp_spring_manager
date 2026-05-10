package api.astro.whats_orders_manager.modules.facturacion.enums;

/**
 * Estados posibles de un pago.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
public enum EstadoPago {
    
    /**
     * Pago pendiente de confirmación.
     */
    PENDIENTE("Pendiente de confirmación"),
    
    /**
     * Pago confirmado y registrado.
     */
    CONFIRMADO("Confirmado"),
    
    /**
     * Pago conciliado con el banco/caja.
     */
    CONCILIADO("Conciliado"),
    
    /**
     * Pago rechazado o anulado.
     */
    RECHAZADO("Rechazado"),
    /**
     * Pago anulado.
     */
    ANULADO("Anulado")
    ;
    
    private final String descripcion;
    
    EstadoPago(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    /**
     * Verifica si el pago es válido para cálculos de saldo.
     * @return true si el estado es CONFIRMADO o CONCILIADO
     */
    public boolean esValido() {
        return this == CONFIRMADO || this == CONCILIADO;
    }
    
    @Override
    public String toString() {
        return descripcion;
    }
}
