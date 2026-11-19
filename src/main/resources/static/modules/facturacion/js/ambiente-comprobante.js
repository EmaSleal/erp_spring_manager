/**
 * T-044 (P-099): what the UI shows for the Hacienda environment of a comprobante, from the flags computed by the
 * server (ambiente, ambienteCoincide, motivoAmbiente). The rule itself lives in the server; this only paints it.
 * No DOM dependencies so it can be tested with node --test.
 */
(function (global) {
    'use strict';

    const ETIQUETAS = {
        SANDBOX: { texto: 'Sandbox', clase: 'bg-warning text-dark' },
        PRODUCCION: { texto: 'Producción', clase: 'bg-danger' }
    };
    const SIN_AMBIENTE = { texto: 'Sin ambiente', clase: 'bg-secondary' };

    /** @returns {{texto:string, clase:string}} badge text and Bootstrap classes for an ambiente name */
    function etiqueta(ambiente) {
        return ETIQUETAS[ambiente] || SIN_AMBIENTE;
    }

    /**
     * @param {{ambienteCoincide?: boolean, motivoAmbiente?: string|null}} comprobante
     * @returns {{bloqueado: boolean, motivo: string|null}} bloqueado only when the server says the ambiente differs
     */
    function bloqueo(comprobante) {
        if (!comprobante || comprobante.ambienteCoincide !== false) {
            return { bloqueado: false, motivo: null };
        }
        return { bloqueado: true, motivo: comprobante.motivoAmbiente || null };
    }

    /**
     * State of an action button (Reenviar, Consultar estado, Enviar).
     * @param {boolean} permiteElEstado the state-based rule (puedeReenviar / consultableEnHacienda)
     * @returns {null|{habilitada: boolean, motivo: string|null}} null = no button; habilitada=false = shown disabled
     */
    function estadoAccion(permiteElEstado, comprobante) {
        if (!permiteElEstado) {
            return null;
        }
        const b = bloqueo(comprobante);
        return { habilitada: !b.bloqueado, motivo: b.motivo };
    }

    global.AmbienteComprobante = { etiqueta, bloqueo, estadoAccion };
})(typeof window !== 'undefined' ? window : globalThis);
