package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;

/**
 * Qué presenta el PDF de una factura según su comprobante vigente (ADR-045 §6, P-030): título, si lleva clave y
 * consecutivo, si lleva la leyenda «En trámite ante Hacienda» y si lleva la leyenda de la resolución MH-DGT-RES-0027-2024.
 * T-057: una factura del régimen simplificado sale como comprobante de venta del RTS, sin clave, consecutivo, leyendas ni IVA.
 * Decisión pura, sin acceso a datos.
 */
record PdfFiscalDecision(String titulo, boolean mostrarClaveYConsecutivo, boolean enTramite, boolean leyendaResolucion,
                         boolean mostrarIva, boolean esComprobanteRts, String leyendaRts) {

    /** Documento sin leyenda del RTS (todo salvo el comprobante de venta del régimen simplificado). */
    PdfFiscalDecision(String titulo, boolean mostrarClaveYConsecutivo, boolean enTramite, boolean leyendaResolucion,
                      boolean mostrarIva) {
        this(titulo, mostrarClaveYConsecutivo, enTramite, leyendaResolucion, mostrarIva, false, null);
    }

    /** Documento con IVA (todo salvo el comprobante de venta del régimen simplificado). */
    PdfFiscalDecision(String titulo, boolean mostrarClaveYConsecutivo, boolean enTramite, boolean leyendaResolucion) {
        this(titulo, mostrarClaveYConsecutivo, enTramite, leyendaResolucion, true);
    }

    static final String TITULO_PROFORMA = "Documento no fiscal (proforma)";
    static final String TITULO_RECHAZADO = "Documento no fiscal (rechazado por Hacienda)";
    static final String TITULO_FACTURA = "Factura electrónica";
    static final String TITULO_TIQUETE = "Tiquete electrónico";
    static final String TITULO_NOTA_CREDITO = "Nota de crédito electrónica";
    static final String TITULO_NOTA_NO_FISCAL = "Documento no fiscal";
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

    /**
     * Misma tabla de estados para una nota de crédito (T-042, ADR-045 §6): título «Nota de crédito electrónica» en los
     * estados fiscales y «Documento no fiscal» en los demás.
     *
     * @param estado estado del comprobante vigente de la nota, o null si no tiene
     */
    static PdfFiscalDecision deNota(EstadoComprobante estado) {
        if (estado == null) {
            return new PdfFiscalDecision(TITULO_NOTA_NO_FISCAL, false, false, false);
        }
        return switch (estado) {
            case ACEPTADO -> new PdfFiscalDecision(TITULO_NOTA_CREDITO, true, false, true);
            case ENVIADO, PROCESANDO, ENVIO_INCIERTO, ERROR_HACIENDA ->
                new PdfFiscalDecision(TITULO_NOTA_CREDITO, true, true, true);
            case RECHAZADO -> new PdfFiscalDecision(TITULO_RECHAZADO, false, false, false);
            case GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO, ANULADO ->
                new PdfFiscalDecision(TITULO_NOTA_NO_FISCAL, false, false, false);
        };
    }

    /**
     * Comprobante de venta del régimen simplificado (T-059, ADR-078): título y leyenda salen de la configuración de
     * facturación; sin clave ni consecutivo, sin leyendas de Hacienda ni IVA. La lectura no inventa texto: un título nulo
     * queda vacío y una leyenda en blanco no se imprime.
     */
    static PdfFiscalDecision deSimplificado(String titulo, String leyenda) {
        String leyendaLimpia = leyenda == null || leyenda.isBlank() ? null : leyenda.strip();
        return new PdfFiscalDecision(titulo == null ? "" : titulo.strip(), false, false, false, false, true, leyendaLimpia);
    }

    private static PdfFiscalDecision proforma() {
        return new PdfFiscalDecision(TITULO_PROFORMA, false, false, false);
    }
}
