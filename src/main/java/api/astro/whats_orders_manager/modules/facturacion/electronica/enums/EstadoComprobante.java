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
     * Comprobante enviado a Hacienda, esperando respuesta.
     */
    ENVIADO("Enviado", "Enviado a Hacienda", "primary", "bi-send"),
    
    /**
     * Comprobante procesando en Hacienda.
     */
    PROCESANDO("Procesando", "Procesando en Hacienda", "primary", "bi-hourglass-split"),
    
    /**
     * Comprobante aceptado por Hacienda.
     */
    ACEPTADO("Aceptado", "Aceptado por Hacienda", "success", "bi-check-circle-fill"),
    
    /**
     * Comprobante rechazado por Hacienda.
     */
    RECHAZADO("Rechazado", "Rechazado por Hacienda", "danger", "bi-x-circle-fill"),
    
    /**
     * Error al procesar el comprobante.
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
