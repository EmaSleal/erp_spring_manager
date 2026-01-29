package api.astro.whats_orders_manager.modules.contabilidad.enums;

/**
 * Tipos de cuentas contables según nomenclatura estándar.
 * Clasificación básica del plan de cuentas.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
public enum TipoCuenta {
    
    ACTIVO("Activo", "Recursos económicos controlados por la empresa"),
    PASIVO("Pasivo", "Obligaciones financieras de la empresa"),
    CAPITAL("Capital", "Patrimonio neto de los propietarios"),
    INGRESO("Ingreso", "Aumentos en beneficios económicos"),
    EGRESO("Egreso", "Disminuciones en beneficios económicos");
    
    private final String nombre;
    private final String descripcion;
    
    TipoCuenta(String nombre, String descripcion) {
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
     * Determina si el tipo de cuenta aparece en el Balance General.
     * @return true si es ACTIVO, PASIVO o CAPITAL
     */
    public boolean esDeBalance() {
        return this == ACTIVO || this == PASIVO || this == CAPITAL;
    }
    
    /**
     * Determina si el tipo de cuenta aparece en el Estado de Resultados.
     * @return true si es INGRESO o EGRESO
     */
    public boolean esDeResultados() {
        return this == INGRESO || this == EGRESO;
    }
    
    @Override
    public String toString() {
        return nombre;
    }
}
