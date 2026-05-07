package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

/**
 * Condición de venta para Facturación Electrónica Costa Rica v4.4
 * Según catálogo de Hacienda
 */
@Getter
public enum CondicionVentaFE {
    
    CONTADO("01", "Contado"),
    CREDITO("02", "Crédito"),
    CONSIGNACION("03", "Consignación"),
    APARTADO("04", "Apartado"),
    ARRENDAMIENTO_OPCION_COMPRA("05", "Arrendamiento con opción de compra"),
    ARRENDAMIENTO_FUNCION_FINANCIERA("06", "Arrendamiento en función financiera"),
    COBRO_FAVOR_TERCERO("07", "Cobro a favor de un tercero"),
    SERVICIOS_PRESTADOS_ESTADO("08", "Servicios prestados al Estado a crédito"),
    PAGO_TERCERO("09", "Pago del servicio o bien por cuenta de un tercero"),
    OTROS("99", "Otros");
    
    private final String codigo;
    private final String descripcion;
    
    CondicionVentaFE(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    /**
     * Obtiene el enum a partir del código de Hacienda
     */
    public static CondicionVentaFE fromCodigo(String codigo) {
        for (CondicionVentaFE condicion : values()) {
            if (condicion.codigo.equals(codigo)) {
                return condicion;
            }
        }
        return OTROS;
    }
    
    /**
     * Formato para mostrar en UI: "01 - Contado"
     */
    public String getFormatoCompleto() {
        return codigo + " - " + descripcion;
    }
}
