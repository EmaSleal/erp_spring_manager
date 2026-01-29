package api.astro.whats_orders_manager.modules.contabilidad.enums;

/**
 * Naturaleza de las cuentas contables según el sistema de partida doble.
 * Determina cómo afectan los débitos y créditos al saldo de la cuenta.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
public enum NaturalezaCuenta {
    
    DEUDORA("Deudora", "Aumenta con débito, disminuye con crédito"),
    ACREEDORA("Acreedora", "Aumenta con crédito, disminuye con débito");
    
    private final String nombre;
    private final String descripcion;
    
    NaturalezaCuenta(String nombre, String descripcion) {
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
     * Obtiene la naturaleza de cuenta según el tipo.
     * @param tipoCuenta Tipo de cuenta
     * @return Naturaleza correspondiente
     */
    public static NaturalezaCuenta fromTipoCuenta(TipoCuenta tipoCuenta) {
        switch (tipoCuenta) {
            case ACTIVO:
            case EGRESO:
                return DEUDORA;
            case PASIVO:
            case CAPITAL:
            case INGRESO:
                return ACREEDORA;
            default:
                throw new IllegalArgumentException("Tipo de cuenta no válido: " + tipoCuenta);
        }
    }
    
    @Override
    public String toString() {
        return nombre;
    }
}
