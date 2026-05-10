package api.astro.whats_orders_manager.modules.contabilidad.enums;

/**
 * Tipos de asientos contables según su origen y propósito.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
public enum TipoAsiento {
    
    MANUAL("Manual", "Asiento registrado manualmente por el usuario"),
    APERTURA("Apertura", "Asiento de apertura del período contable"),
    CIERRE("Cierre", "Asiento de cierre del período contable"),
    AJUSTE("Ajuste", "Asiento de ajuste o corrección"),
    INGRESO("Ingreso", "Asiento por ingresos diversos"),
    AUTOMATICO_VENTA("Automático - Venta", "Generado automáticamente al contabilizar una factura"),
    AUTOMATICO_PAGO("Automático - Pago", "Generado automáticamente al registrar un pago"),
    AUTOMATICO_COMPRA("Automático - Compra", "Generado automáticamente al contabilizar una compra"),
    AUTOMATICO_GASTO("Automático - Gasto", "Generado automáticamente al registrar un gasto"),
    AUTOMATICO_NOMINA("Automático - Nómina", "Generado automáticamente al procesar nómina");
    
    private final String nombre;
    private final String descripcion;
    
    TipoAsiento(String nombre, String descripcion) {
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
     * Verifica si el asiento es generado automáticamente.
     * @return true si el tipo comienza con AUTOMATICO_
     */
    public boolean esAutomatico() {
        return this.name().startsWith("AUTOMATICO_");
    }
    
    /**
     * Verifica si el asiento puede ser editado.
     * @return true si es MANUAL, AJUSTE o APERTURA
     */
    public boolean esEditable() {
        return this == MANUAL || this == AJUSTE || this == APERTURA;
    }
    
    @Override
    public String toString() {
        return nombre;
    }
}
