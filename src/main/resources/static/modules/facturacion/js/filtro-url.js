/**
 * T-043 (ADR-048): filtros que el panel de inicio pasa por la URL a los listados de comprobantes y de notas de
 * crédito (?estado=X&vigente=true). Funciones puras, sin DOM, para probarlas con node --test.
 */
(function (global) {
    'use strict';

    const ESTADO_VALIDO = /^[A-Z_]{1,30}$/;

    /**
     * @param {string} search query string, p. ej. window.location.search
     * @returns {{estado: string, vigente: boolean}} estado vacío si falta o no tiene forma de estado
     */
    function leerFiltros(search) {
        const params = new URLSearchParams(search || '');
        const estado = (params.get('estado') || '').trim();
        return {
            estado: ESTADO_VALIDO.test(estado) ? estado : '',
            vigente: params.get('vigente') === 'true'
        };
    }

    /** Con vigente, oculta las filas cuya bandera `vigente` es false (el historial); sin la bandera, las deja. */
    function soloVigentes(filas, vigente) {
        if (!vigente || !Array.isArray(filas)) return filas;
        return filas.filter((f) => f && f.vigente !== false);
    }

    global.FiltroUrl = { leerFiltros, soloVigentes };
})(typeof window !== 'undefined' ? window : globalThis);
