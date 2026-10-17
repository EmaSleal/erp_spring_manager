package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

/**
 * Estados de un comprobante electrónico en el ciclo de vida.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Getter
public enum EstadoComprobante {
    
    /**
     * Comprobante generado pero no enviado a Hacienda.
     */
    GENERADO("Generado", "Comprobante XML generado", "warning", "bi-file-earmark-text"),
    
    /**
     * Comprobante firmado digitalmente.
     */
    FIRMADO("Firmado", "Comprobante firmado digitalmente", "info", "bi-shield-check"),
    
    /**
     * Se intentó enviar y es seguro que Hacienda no lo tiene: falló la conexión (rechazada, DNS, timeout de
     * conexión, token) o la recepción respondió un 4xx que no indica recepción previa (ADR-030).
     * Se reenvía con la misma clave.
     */
    NO_TRANSMITIDO("No transmitido", "Hacienda no lo recibió; se puede corregir y reenviar", "secondary",
            "bi-cloud-slash"),

    /**
     * Hacienda pudo recibirlo (timeout de lectura, conexión cortada tras enviar, 5xx o 4xx de documento ya recibido
     * o sin causa clasificable). Se resuelve con "verificar con Hacienda" (ADR-030).
     */
    ENVIO_INCIERTO("Envío incierto", "Hacienda pudo recibirlo; verifique con Hacienda", "warning",
            "bi-question-circle-fill"),

    /**
     * Hacienda confirmó la recepción (202) y aún no informa el resultado.
     */
    ENVIADO("Enviado", "Enviado a Hacienda", "primary", "bi-send"),
    
    /**
     * Hacienda informa que lo está procesando o que lo recibió sin resolver.
     */
    PROCESANDO("Procesando", "Procesando en Hacienda", "primary", "bi-hourglass-split"),
    
    /**
     * Comprobante aceptado por Hacienda.
     */
    ACEPTADO("Aceptado", "Aceptado por Hacienda", "success", "bi-check-circle-fill"),
    
    /**
     * Hacienda procesó el documento y lo rechazó (ind-estado). Historial inmutable: se reemite con clave nueva.
     */
    RECHAZADO("Rechazado", "Rechazado por Hacienda", "danger", "bi-x-circle-fill"),
    
    /**
     * Falla local antes de transmitir (XML, firma, configuración). Se reenvía con la misma clave.
     */
    ERROR("Error", "Error en el proceso", "danger", "bi-exclamation-triangle-fill"),
    
    /**
     * Comprobante anulado.
     */
    ANULADO("Anulado", "Comprobante anulado", "secondary", "bi-slash-circle");
    
    private final String codigo;
    private final String descripcion;
    private final String color;
    private final String icono;
    
    EstadoComprobante(String codigo, String descripcion, String color, String icono) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.color = color;
        this.icono = icono;
    }
    
    /**
     * Verifica si el estado es terminal (no cambiará).
     */
    public boolean esTerminal() {
        return this == ACEPTADO || this == RECHAZADO || this == ANULADO;
    }
    
    /**
     * true si Hacienda no tiene el documento y se puede (re)enviar con la misma clave: GENERADO, FIRMADO,
     * ERROR o NO_TRANSMITIDO (ADR-030).
     */
    public boolean esReenviable() {
        return this == GENERADO || this == FIRMADO || this == ERROR || this == NO_TRANSMITIDO;
    }

    /**
     * Verifica si el estado indica éxito.
     */
    public boolean esExitoso() {
        return this == ACEPTADO;
    }
    
    /**
     * Verifica si el estado indica error.
     */
    public boolean esError() {
        return this == RECHAZADO || this == ERROR;
    }
}
