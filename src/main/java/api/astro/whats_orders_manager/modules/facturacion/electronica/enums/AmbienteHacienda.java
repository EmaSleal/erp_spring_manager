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
    
    /*
     * Constantes del cliente de Hacienda por ambiente (T-037, P-091). Fuente: Anexo 3 «API de comprobantes»
     * (docs/referencias/ANEXOS_Y_ESTRUCTURAS_V4.4/anexo-3-api-comprobantes.md): IdP en
     * https://idp.comprobanteselectronicos.go.cr/auth, realm "rut" y Client Id "api-prod" en producción (l.60-62), API de
     * recepción en https://api.comprobanteselectronicos.go.cr/recepcion/v1/ (l.25). Para SANDBOX se conservan los valores
     * con los que la aplicación ya emite y Hacienda acepta: realm "rut-stag", Client Id "api-stag" y
     * https://api-sandbox.comprobanteselectronicos.go.cr (el Anexo menciona además /recepcion-sandbox/v1/, l.31).
     */

    /** Ambiente de pruebas (Sandbox) para desarrollo y testing. */
    SANDBOX("sandbox",
            "https://api-sandbox.comprobanteselectronicos.go.cr",
            "rut-stag",
            "api-stag"),

    /** Ambiente de producción: los comprobantes tienen validez fiscal. */
    PRODUCCION("produccion",
               "https://api.comprobanteselectronicos.go.cr",
               "rut",
               "api-prod");

    /** Identity Provider (Keycloak) de Hacienda, común a los dos ambientes. */
    public static final String URL_IDP = "https://idp.comprobanteselectronicos.go.cr/auth";
    /** Ruta del recurso de recepción (POST) y de consulta (GET .../{clave}) sobre {@link #getUrlBase()}. */
    public static final String RUTA_RECEPCION = "/recepcion/v1/recepcion";
    /** Ruta de los mensajes del receptor sobre {@link #getUrlBase()}. */
    public static final String RUTA_MENSAJES_RECEPTOR = "/mensajes-receptor/v1/mensajes/";

    private final String codigo;
    private final String urlBase;
    /** Realm del IdP: "rut-stag" (SANDBOX) o "rut" (PRODUCCION). */
    private final String realm;
    /** Client Id del POST de token (password y refresh_token): "api-stag" o "api-prod". */
    private final String clientId;

    AmbienteHacienda(String codigo, String urlBase, String realm, String clientId) {
        this.codigo = codigo;
        this.urlBase = urlBase;
        this.realm = realm;
        this.clientId = clientId;
    }

    /** URL del token OAuth2 del realm del ambiente. */
    public String getUrlToken() {
        return URL_IDP + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /** URL de recepción (POST) del ambiente. */
    public String getUrlRecepcion() {
        return urlBase + RUTA_RECEPCION;
    }

    public static AmbienteHacienda fromCodigo(String codigo) {
        for (AmbienteHacienda ambiente : values()) {
            if (ambiente.codigo.equalsIgnoreCase(codigo)) {
                return ambiente;
            }
        }
        throw new IllegalArgumentException("Código de ambiente inválido: " + codigo);
    }
}
