package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;

/**
 * Qué presenta el PDF de una factura según su comprobante vigente (ADR-045 §6, P-030): título, si lleva clave y
 * consecutivo, si lleva la leyenda «En trámite ante Hacienda» y si lleva la leyenda de la resolución MH-DGT-RES-0027-2024.
 * Decisión pura, sin acceso a datos.
 */
record PdfFiscalDecision(String titulo, boolean mostrarClaveYConsecutivo, boolean enTramite, boolean leyendaResolucion) {

    static final String TITULO_PROFORMA = "Documento no fiscal (proforma)";
    static final String TITULO_RECHAZADO = "Documento no fiscal (rechazado por Hacienda)";
    static final String TITULO_FACTURA = "Factura electrónica";
    static final String TITULO_TIQUETE = "Tiquete electrónico";
    static final String LEYENDA_EN_TRAMITE = "En trámite ante Hacienda";

    /** @param estado estado del comprobante vigente, o null si la factura no tiene comprobante vigente */
    static PdfFiscalDecision de(EstadoComprobante estado, TipoComprobanteElectronico tipo) {
        if (estado == null) {
            return proforma();
        }
        String titulo = tipo == TipoComprobanteElectronico.TIQUETE_ELECTRONICO ? TITULO_TIQUETE : TITULO_FACTURA;
        return switch (estado) {
            case ACEPTADO -> new PdfFiscalDecision(titulo, true, false, true);
            // ERROR_HACIENDA: Hacienda lo recibió y quedó sin resolver; se trata como en trámite.
            case ENVIADO, PROCESANDO, ENVIO_INCIERTO, ERROR_HACIENDA -> new PdfFiscalDecision(titulo, true, true, true);
            case RECHAZADO -> new PdfFiscalDecision(TITULO_RECHAZADO, false, false, false);
            case GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO, ANULADO -> proforma();
        };
    }

    private static PdfFiscalDecision proforma() {
        return new PdfFiscalDecision(TITULO_PROFORMA, false, false, false);
    }
}
