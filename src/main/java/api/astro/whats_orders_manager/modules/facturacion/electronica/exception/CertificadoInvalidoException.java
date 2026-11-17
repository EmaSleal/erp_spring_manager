package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator.Causa;

/** T-035: el certificado no se puede usar para firmar; el mensaje es fijo por causa (sin texto de la excepción original). */
public class CertificadoInvalidoException extends RuntimeException {

    private final Causa causa;

    public CertificadoInvalidoException(Causa causa) {
        super(causa.mensaje());
        this.causa = causa;
    }

    public Causa getCausa() {
        return causa;
    }
}
