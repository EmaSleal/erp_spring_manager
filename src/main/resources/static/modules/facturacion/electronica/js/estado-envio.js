/**
 * T-037 (ADR-043 §4): ENVIO_INCIERTO es también la marca previa al POST de recepción. Mientras el envío es reciente
 * (fecha_envio de menos de 2 minutos) se muestra «Enviando…»; pasado ese tiempo, «Envío incierto» (se verificará con
 * Hacienda). Sin dependencias del DOM para poder probarlo con node --test.
 */
(function (global) {
    'use strict';

    const MINUTOS_ENVIANDO = 2;

    /**
     * @param {string} estado estado del comprobante
     * @param {string|null} fechaEnvio fecha_envio (ISO local, como la serializa el servidor)
     * @param {number} [ahora] milisegundos de "ahora" (para pruebas); por defecto Date.now()
     * @returns {boolean} true si el comprobante se está enviando en este momento
     */
    function estaEnviando(estado, fechaEnvio, ahora) {
        if (estado !== 'ENVIO_INCIERTO' || !fechaEnvio) {
            return false;
        }
        const envio = new Date(fechaEnvio).getTime();
        if (Number.isNaN(envio)) {
            return false;
        }
        const transcurrido = (ahora === undefined ? Date.now() : ahora) - envio;
        return transcurrido >= -60000 && transcurrido < MINUTOS_ENVIANDO * 60000;
    }

    /** Texto del estado de envío: «Enviando…», «Envío incierto» o null si el estado no es ENVIO_INCIERTO. */
    function etiquetaEnvio(estado, fechaEnvio, ahora) {
        if (estado !== 'ENVIO_INCIERTO') {
            return null;
        }
        return estaEnviando(estado, fechaEnvio, ahora) ? 'Enviando…' : 'Envío incierto';
    }

    global.EstadoEnvio = { estaEnviando, etiquetaEnvio, MINUTOS_ENVIANDO };
})(typeof window !== 'undefined' ? window : globalThis);
