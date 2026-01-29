package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

/**
 * Ambiente de operación para Facturación Electrónica de Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Getter
public enum AmbienteHacienda {
    
    /**
     * Ambiente de pruebas (Sandbox) para desarrollo y testing.
     */
    SANDBOX("sandbox", 
            "https://api-sandbox.comprobanteselectronicos.go.cr",
            "https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-stag/protocol/openid-connect/token"),
    
    /**
     * Ambiente de producción para operaciones reales.
     */
    PRODUCCION("produccion", 
               "https://api.comprobanteselectronicos.go.cr",
               "https://idp.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token");
    
    private final String codigo;
    private final String urlBase;
    private final String urlToken;
    
    AmbienteHacienda(String codigo, String urlBase, String urlToken) {
        this.codigo = codigo;
        this.urlBase = urlBase;
        this.urlToken = urlToken;
    }
    
    /**
     * Obtiene el ambiente desde su código.
     * 
     * @param codigo Código del ambiente
     * @return Ambiente correspondiente
     * @throws IllegalArgumentException si el código no es válido
     */
    public static AmbienteHacienda fromCodigo(String codigo) {
        for (AmbienteHacienda ambiente : values()) {
            if (ambiente.codigo.equalsIgnoreCase(codigo)) {
                return ambiente;
            }
        }
        throw new IllegalArgumentException("Código de ambiente inválido: " + codigo);
    }
}
