package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

/**
 * Códigos de mensajes de respuesta de Hacienda según documentación oficial.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Getter
public enum MensajeHacienda {
    
    ACEPTADO("1", "Aceptado"),
    
    // Estado procesando
    PROCESANDO("0", "En procesamiento"),
    
    // Códigos del campo Mensaje del Anexo v4.4 (nota 11): 1 aceptado, 2 aceptación parcial, 3 rechazado
    ACEPTADO_PARCIAL("2", "Aceptación parcial"),
    RECHAZADO("3", "Rechazado"),
    
    // Errores de validación XML (100-199)
    ERROR_XML_MAL_FORMADO("100", "XML mal formado"),
    ERROR_CLAVE_NUMERICA("101", "Clave numérica inválida"),
    ERROR_CEDULA_EMISOR("102", "Cédula del emisor inválida"),
    ERROR_CEDULA_RECEPTOR("103", "Cédula del receptor inválida"),
    ERROR_FECHA("104", "Fecha del comprobante inválida"),
    ERROR_CONSECUTIVO("105", "Consecutivo duplicado"),
    ERROR_FIRMA("106", "Firma digital inválida"),
    ERROR_MONTO("107", "Montos no coinciden"),
    
    // Errores de autenticación (200-299)
    ERROR_AUTENTICACION("200", "Error de autenticación"),
    ERROR_TOKEN_INVALIDO("201", "Token inválido o expirado"),
    ERROR_CREDENCIALES("202", "Credenciales incorrectas"),
    
    // Errores de servicio (300-399)
    ERROR_SERVICIO("300", "Error en servicio de Hacienda"),
    ERROR_TIMEOUT("301", "Timeout en conexión"),
    ERROR_CONEXION("302", "Error de conexión"),
    
    // Otros
    ERROR("998", "Error general"),
    DESCONOCIDO("999", "Error desconocido");
    
    private final String codigo;
    private final String descripcion;
    
    MensajeHacienda(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
    
    /**
     * Obtiene el mensaje desde su código.
     * 
     * @param codigo Código del mensaje
     * @return Mensaje correspondiente o DESCONOCIDO
     */
    public static MensajeHacienda fromCodigo(String codigo) {
        for (MensajeHacienda mensaje : values()) {
            if (mensaje.codigo.equals(codigo)) {
                return mensaje;
            }
        }
        return DESCONOCIDO;
    }
    
    /**
     * Verifica si el mensaje indica éxito.
     */
    public boolean esExitoso() {
        return this == ACEPTADO;
    }
    
    /**
     * Verifica si el mensaje indica rechazo.
     */
    public boolean esRechazo() {
        return this == RECHAZADO;
    }
    
    /**
     * Verifica si el mensaje indica error técnico: los códigos de tres dígitos 2xx (autenticación) y 3xx (servicio).
     * Los códigos 2 y 3 del Anexo (aceptación parcial, rechazado) son respuestas de Hacienda, no errores técnicos.
     */
    public boolean esErrorTecnico() {
        return codigo.length() == 3 && (codigo.startsWith("2") || codigo.startsWith("3"));
    }
}
