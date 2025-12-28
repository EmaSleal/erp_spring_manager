package api.astro.whats_orders_manager.models.enums;

/**
 * Tipos de datos que puede almacenar un parámetro del sistema.
 */
public enum TipoDatoParametro {
    
    STRING("Texto"),
    INTEGER("Número Entero"),
    LONG("Número Largo"),
    BOOLEAN("Booleano (Sí/No)"),
    DECIMAL("Número Decimal"),
    DATE("Fecha");

    private final String descripcion;

    TipoDatoParametro(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
