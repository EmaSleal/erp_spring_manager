/**
 * T-043 (ADR-048): pinta el estado de los contadores del panel de inicio de Facturación. Un contador en 0 se muestra
 * en estado neutro; uno mayor que 0, en estado de alerta. Solo classList y textContent leído; no escribe HTML. Sin
 * dependencias del DOM real para probarlo con node --test.
 */
(function (global) {
    'use strict';

    const CLASE_NEUTRO = 'contador-neutro';
    const CLASE_ALERTA = 'contador-alerta';

    /** @returns {'neutro'|'alerta'} alerta solo si el valor es un número mayor que 0 */
    function estadoContador(valor) {
        const n = Number(String(valor == null ? '' : valor).trim());
        return Number.isFinite(n) && n > 0 ? 'alerta' : 'neutro';
    }

    /** Aplica la clase de estado a una tarjeta según el número de su elemento [data-contador]. */
    function pintarTarjeta(tarjeta) {
        const numero = tarjeta.querySelector('[data-contador]');
        const estado = estadoContador(numero ? numero.textContent : null);
        tarjeta.classList.remove(CLASE_NEUTRO, CLASE_ALERTA);
        tarjeta.classList.add(estado === 'alerta' ? CLASE_ALERTA : CLASE_NEUTRO);
        return estado;
    }

    function pintarContadores(doc) {
        return Array.from(doc.querySelectorAll('[data-contador-card]')).map(pintarTarjeta);
    }

    const api = { estadoContador, pintarTarjeta, pintarContadores, CLASE_NEUTRO, CLASE_ALERTA };
    global.PanelInicio = api;

    if (global.document && typeof global.document.addEventListener === 'function') {
        global.document.addEventListener('DOMContentLoaded', () => pintarContadores(global.document));
    }
})(typeof window !== 'undefined' ? window : globalThis);
