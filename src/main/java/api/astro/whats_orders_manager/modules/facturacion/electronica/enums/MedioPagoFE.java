package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

/**
 * Medio de pago para Facturación Electrónica Costa Rica v4.4
 * Según catálogo de Hacienda
 */
@Getter
public enum MedioPagoFE {
    
    EFECTIVO("01", "Efectivo"),
    TARJETA("02", "Tarjeta"),
    CHEQUE("03", "Cheque"),
    TRANSFERENCIA_DEPOSITO("04", "Transferencia - depósito bancario"),
    RECAUDADO_TERCEROS("05", "Recaudado por terceros"),
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    MedioPagoFE(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    /**
     * Obtiene el enum a partir del código de Hacienda
     */
    public static MedioPagoFE fromCodigo(String codigo) {
        for (MedioPagoFE medio : values()) {
            if (medio.codigo.equals(codigo)) {
                return medio;
            }
        }
        return OTROS;
    }
    
    /**
     * Formato para mostrar en UI: "01 - Efectivo"
     */
    public String getFormatoCompleto() {
        return codigo + " - " + descripcion;
    }
}
