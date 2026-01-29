package api.astro.whats_orders_manager.modules.facturacion.enums;

/**
 * Métodos de Pago según catálogo de Hacienda de Costa Rica.
 * Códigos oficiales para Facturación Electrónica CR.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
public enum MetodoPago {
    
    EFECTIVO("01", "Efectivo"),
    TARJETA("02", "Tarjeta"),
    CHEQUE("03", "Cheque"),
    TRANSFERENCIA("04", "Transferencia bancaria - depósito bancario"),
    RECAUDADO("05", "Recaudado por terceros"),
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    MetodoPago(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    /**
     * Obtiene el código oficial de Hacienda CR.
     * @return Código de 2 dígitos (01-99)
     */
    public String getCodigo() {
        return codigo;
    }
    
    /**
     * Obtiene la descripción del método de pago.
     * @return Descripción legible
     */
    public String getDescripcion() {
        return descripcion;
    }
    
    /**
     * Busca un método de pago por su código de Hacienda.
     * @param codigo Código de 2 dígitos
     * @return MetodoPago correspondiente
     * @throws IllegalArgumentException si el código no existe
     */
    public static MetodoPago fromCodigo(String codigo) {
        for (MetodoPago metodo : values()) {
            if (metodo.codigo.equals(codigo)) {
                return metodo;
            }
        }
        throw new IllegalArgumentException("Código de método de pago inválido: " + codigo);
    }
    
    /**
     * Verifica si el método de pago requiere referencia obligatoria.
     * @return true si requiere referencia (cheque, transferencia, etc.)
     */
    public boolean requiereReferencia() {
        return this == CHEQUE || this == TRANSFERENCIA;
    }
    
    @Override
    public String toString() {
        return codigo + " - " + descripcion;
    }
}
