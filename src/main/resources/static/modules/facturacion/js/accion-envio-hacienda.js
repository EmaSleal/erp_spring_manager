/**
 * T-040 (P-086): decide what the «send to Hacienda» button of the invoice modal does, from the state of the
 * current (vigente) comprobante. No DOM dependencies so it can be tested with node --test.
 *
 *   no comprobante or RECHAZADO  -> 'emitir'   (POST /comprobantes/procesar/{facturaId}: emit / re-emit)
 *   NO_TRANSMITIDO or ERROR      -> 'reenviar' (POST /comprobantes/{idComprobante}/reenviar)
 *   anything else                -> null       (no send button; GENERADO/FIRMADO included, as before)
 *
 * T-057 (ADR-073 §2): a SIMPLIFICADO (RTS) invoice is never sent to Hacienda -> null whatever the comprobante state.
 * T-057 (ADR-076 §4): an invoice with the key of a comprobante issued outside the ERP is never sent either -> null.
 */
(function (global) {
    'use strict';

    const REENVIABLES_EN_MODAL = ['NO_TRANSMITIDO', 'ERROR'];

    /**
     * @param {string|null|undefined} estadoVigente estado del comprobante vigente, o vacío si no hay
     * @param {string|null|undefined} regimenEmisor régimen sellado de la factura (GENERAL | SIMPLIFICADO); vacío = GENERAL
     * @param {string|null|undefined} claveExterna clave del comprobante emitido fuera del ERP; vacío = no hay
     */
    function accionEnvioHacienda(estadoVigente, regimenEmisor, claveExterna) {
        if (regimenEmisor === 'SIMPLIFICADO' || claveExterna) {
            return null;
        }
        if (!estadoVigente || estadoVigente === 'RECHAZADO') {
            return 'emitir';
        }
        return REENVIABLES_EN_MODAL.includes(estadoVigente) ? 'reenviar' : null;
    }

    /**
     * Si el detalle muestra el formulario de la clave de un comprobante emitido fuera del ERP (T-057, ADR-076 §4): factura
     * GENERAL (o sin régimen en el DTO), no anulada y sin comprobante del ERP vigente distinto de RECHAZADO. El servidor
     * es la autoridad y repite estas reglas (más la de claves duplicadas).
     */
    function puedeRegistrarClaveExterna(estadoVigente, regimenEmisor, anulada) {
        if (regimenEmisor === 'SIMPLIFICADO' || anulada === true) {
            return false;
        }
        return !estadoVigente || estadoVigente === 'RECHAZADO';
    }

    /** Texto del botón para la acción, o null si no hay botón. */
    function textoBotonEnvio(accion) {
        if (accion === 'emitir') return 'Enviar a Hacienda';
        if (accion === 'reenviar') return 'Reenviar a Hacienda';
        return null;
    }

    global.AccionEnvioHacienda = { accionEnvioHacienda, textoBotonEnvio, puedeRegistrarClaveExterna };
})(typeof window !== 'undefined' ? window : globalThis);
