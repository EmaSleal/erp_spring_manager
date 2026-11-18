// T-039 (ADR-045): lógica pura de la factura sustituta en el modal de factura; sin DOM, probada con node
// (src/test/js/sustitucion-factura.test.mjs).
(function (global) {
    'use strict';

    /** El botón «Emitir factura sustituta» solo aparece cuando el servidor lo indica (misma regla que el endpoint). */
    function puedeEmitir(detalle) {
        return !!detalle && detalle.puedeEmitirSustituta === true;
    }

    /** «Sustituye a FA… (anulada por NC…)»; la parte de la nota solo si el servidor la conoce. */
    function textoSustituye(ref) {
        if (!ref || !ref.numeroFactura) return '';
        const nota = ref.numeroNota ? ' (anulada por ' + ref.numeroNota + ')' : '';
        return 'Sustituye a ' + ref.numeroFactura + nota;
    }

    /** «Sustituida por FA…». */
    function textoSustituidaPor(ref) {
        if (!ref || !ref.numeroFactura) return '';
        return 'Sustituida por ' + ref.numeroFactura;
    }

    /** Cuerpo del POST: sin razón el servidor usa la sugerida. */
    function cuerpoPedido(razon) {
        const r = typeof razon === 'string' ? razon.trim() : '';
        return r ? { razon: r } : {};
    }

    /** Mensaje para el usuario según la respuesta del servidor; nunca texto técnico. */
    function mensajeDeError(respuestaJson) {
        const m = respuestaJson && typeof respuestaJson.message === 'string' ? respuestaJson.message.trim() : '';
        return m || 'No se pudo crear la factura sustituta';
    }

    global.SustitucionFactura = { puedeEmitir, textoSustituye, textoSustituidaPor, cuerpoPedido, mensajeDeError };
})(typeof window !== 'undefined' ? window : globalThis);
