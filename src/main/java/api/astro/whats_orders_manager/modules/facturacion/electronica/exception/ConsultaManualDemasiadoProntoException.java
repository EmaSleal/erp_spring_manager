package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Se superó el límite de consultas manuales de estado del comprobante (ADR-031 §4: 1 cada 30 s). Se responde 429 con
 * el momento a partir del cual se puede volver a consultar.
 */
@Getter
public class ConsultaManualDemasiadoProntoException extends RuntimeException {

    private final LocalDateTime disponibleDesde;

    public ConsultaManualDemasiadoProntoException(String message, LocalDateTime disponibleDesde) {
        super(message);
        this.disponibleDesde = disponibleDesde;
    }
}
