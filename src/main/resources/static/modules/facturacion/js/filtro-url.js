/**
 * T-043 (ADR-048): filtros que el panel de inicio pasa por la URL a los listados de comprobantes y de notas de
 * crédito (?estado=X&vigente=true&ambiente=Y). Los filtra el SERVIDOR (la API los acepta y devuelve el total
 * filtrado); aquí solo se leen de la URL y se arma la consulta. Funciones puras, sin DOM, para node --test.
 */
(function (global) {
    'use strict';

    const NOMBRE_VALIDO = /^[A-Z_]{1,30}$/;

    function nombre(valor) {
        const v = (valor || '').trim();
        return NOMBRE_VALIDO.test(v) ? v : '';
    }

    /**
     * @param {string} search query string, p. ej. window.location.search
     * @returns {{estado: string, vigente: boolean, ambiente: string}} estado y ambiente vacíos si faltan o no tienen
     *     forma de nombre de enum
     */
    function leerFiltros(search) {
        const params = new URLSearchParams(search || '');
        return {
            estado: nombre(params.get('estado')),
            vigente: params.get('vigente') === 'true',
            ambiente: nombre(params.get('ambiente'))
        };
    }

    /** Query de la API de comprobantes: página, tamaño y solo los filtros del servidor que tienen valor. */
    function queryServidor(filtros, pagina, tamano) {
        const f = filtros || {};
        const p = new URLSearchParams();
        p.set('page', String(pagina || 0));
        p.set('size', String(tamano || 100));
        if (f.vigente) p.set('vigente', 'true');
        if (f.ambiente) p.set('ambiente', f.ambiente);
        return p.toString();
    }

    /** ¿Hay algún filtro que se aplica en el servidor (vigente o ambiente)? */
    function hayFiltroServidor(filtros) {
        return !!(filtros && (filtros.vigente || filtros.ambiente));
    }

    /** Texto del filtro activo del panel, p. ej. «Solo vigentes · ambiente PRODUCCION»; vacío si no hay. */
    function textoFiltro(filtros) {
        const f = filtros || {};
        const partes = [];
        if (f.vigente) partes.push('Solo vigentes');
        if (f.ambiente) partes.push('ambiente ' + f.ambiente);
        return partes.join(' · ');
    }

    /** «Mostrando 1-100 de 130»; sin total (respuesta sin filtro de servidor) devuelve vacío. */
    function textoTotal(total, pagina, tamano, enPagina) {
        const t = Number(total);
        if (!Number.isFinite(t) || total === null || total === undefined || total === '') return '';
        if (t <= 0) return 'Sin comprobantes';
        const desde = (pagina || 0) * tamano + 1;
        const hasta = Math.min(desde + Math.max(enPagina || 0, 1) - 1, t);
        return 'Mostrando ' + desde + '-' + hasta + ' de ' + t;
    }

    global.FiltroUrl = { leerFiltros, queryServidor, hayFiltroServidor, textoFiltro, textoTotal };
})(typeof window !== 'undefined' ? window : globalThis);
