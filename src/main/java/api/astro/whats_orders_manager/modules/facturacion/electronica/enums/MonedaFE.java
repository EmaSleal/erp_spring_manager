package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

/**
 * Monedas para Facturación Electrónica Costa Rica v4.4
 * Según catálogo de Hacienda
 */
@Getter
public enum MonedaFE {
    
    CRC("CRC", "Colón costarricense", "₡"),
    USD("USD", "Dólar estadounidense", "$"),
    EUR("EUR", "Euro", "€");
    
    private final String codigo;
    private final String descripcion;
    private final String simbolo;
    
    MonedaFE(String codigo, String descripcion, String simbolo) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.simbolo = simbolo;
    }
    
    /**
     * Obtiene el enum a partir del código ISO
     */
    public static MonedaFE fromCodigo(String codigo) {
        for (MonedaFE moneda : values()) {
            if (moneda.codigo.equals(codigo)) {
                return moneda;
            }
        }
        return CRC; // Por defecto Colones
    }
    
    /**
     * Formato para mostrar en UI: "CRC - Colón costarricense (₡)"
     */
    public String getFormatoCompleto() {
        return codigo + " - " + descripcion + " (" + simbolo + ")";
    }
}
