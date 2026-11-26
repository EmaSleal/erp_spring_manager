/**
 * T-040 (P-086): decide what the «send to Hacienda» button of the invoice modal does, from the state of the
 * current (vigente) comprobante. No DOM dependencies so it can be tested with node --test.
 *
 *   no comprobante or RECHAZADO  -> 'emitir'   (POST /comprobantes/procesar/{facturaId}: emit / re-emit)
 *   NO_TRANSMITIDO or ERROR      -> 'reenviar' (POST /comprobantes/{idComprobante}/reenviar)
 *   anything else                -> null       (no send button; GENERADO/FIRMADO included, as before)
 *
 * T-057 (ADR-073 §2): a SIMPLIFICADO (RTS) invoice is never sent to Hacienda -> null whatever the comprobante state.
 */
(function (global) {
    'use strict';

    const REENVIABLES_EN_MODAL = ['NO_TRANSMITIDO', 'ERROR'];

    /**
     * @param {string|null|undefined} estadoVigente estado del comprobante vigente, o vacío si no hay
     * @param {string|null|undefined} regimenEmisor régimen sellado de la factura (GENERAL | SIMPLIFICADO); vacío = GENERAL
     */
    function accionEnvioHacienda(estadoVigente, regimenEmisor) {
        if (regimenEmisor === 'SIMPLIFICADO') {
            return null;
        }
        if (!estadoVigente || estadoVigente === 'RECHAZADO') {
            return 'emitir';
        }
        return REENVIABLES_EN_MODAL.includes(estadoVigente) ? 'reenviar' : null;
    }

    /** Texto del botón para la acción, o null si no hay botón. */
    function textoBotonEnvio(accion) {
        if (accion === 'emitir') return 'Enviar a Hacienda';
        if (accion === 'reenviar') return 'Reenviar a Hacienda';
        return null;
    }

    global.AccionEnvioHacienda = { accionEnvioHacienda, textoBotonEnvio };
})(typeof window !== 'undefined' ? window : globalThis);
