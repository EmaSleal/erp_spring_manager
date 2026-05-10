package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

/**
 * Tipos de comprobantes electrónicos según Ministerio de Hacienda de Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Getter
public enum TipoComprobanteElectronico {
    
    FACTURA_ELECTRONICA("01", "Factura Electrónica"),
    NOTA_DEBITO("02", "Nota de Débito Electrónica"),
    NOTA_CREDITO("03", "Nota de Crédito Electrónica"),
    TIQUETE_ELECTRONICO("04", "Tiquete Electrónico"),
    CONFIRMACION_ACEPTACION("05", "Confirmación de Aceptación"),
    CONFIRMACION_ACEPTACION_PARCIAL("06", "Confirmación de Aceptación Parcial"),
    CONFIRMACION_RECHAZO("07", "Confirmación de Rechazo"),
    FACTURA_ELECTRONICA_COMPRA("08", "Factura Electrónica de Compra"),
    FACTURA_ELECTRONICA_EXPORTACION("09", "Factura Electrónica de Exportación");
    
    private final String codigo;
    private final String descripcion;
    
    TipoComprobanteElectronico(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    /**
     * Obtiene el tipo de comprobante desde su código.
     * 
     * @param codigo Código del comprobante (01-09)
     * @return Tipo de comprobante correspondiente
     * @throws IllegalArgumentException si el código no es válido
     */
    public static TipoComprobanteElectronico fromCodigo(String codigo) {
        for (TipoComprobanteElectronico tipo : values()) {
            if (tipo.codigo.equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de comprobante inválido: " + codigo);
    }
    
    /**
     * Verifica si el comprobante es de tipo factura.
     */
    public boolean esFactura() {
        return this == FACTURA_ELECTRONICA || 
               this == FACTURA_ELECTRONICA_COMPRA || 
               this == FACTURA_ELECTRONICA_EXPORTACION;
    }
    
    /**
     * Verifica si el comprobante es de tipo confirmación.
     */
    public boolean esConfirmacion() {
        return this == CONFIRMACION_ACEPTACION || 
               this == CONFIRMACION_ACEPTACION_PARCIAL || 
               this == CONFIRMACION_RECHAZO;
    }
}
