package api.astro.whats_orders_manager.modules.contabilidad.enums;

/**
 * Estados de un asiento contable en su ciclo de vida.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
public enum EstadoAsiento {
    
    BORRADOR("Borrador", "Asiento en proceso de creación, puede modificarse"),
    CONTABILIZADO("Contabilizado", "Asiento finalizado y contabilizado, afecta los saldos"),
    ANULADO("Anulado", "Asiento anulado, no afecta los saldos");
    
    private final String nombre;
    private final String descripcion;
    
    EstadoAsiento(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    /**
     * Verifica si el asiento puede ser modificado.
     * @return true solo si está en BORRADOR
     */
    public boolean puedeModificarse() {
        return this == BORRADOR;
    }
    
    /**
     * Verifica si el asiento afecta los saldos de las cuentas.
     * @return true si está CONTABILIZADO
     */
    public boolean afectaSaldos() {
        return this == CONTABILIZADO;
    }
    
    @Override
    public String toString() {
        return nombre;
    }
}
