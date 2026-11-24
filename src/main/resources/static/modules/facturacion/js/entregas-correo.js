/**
 * T-055 (ADR-070): acción «Reenviar comprobante por correo» e historial de entregas del comprobante, compartidos por el
 * modal de la factura y el detalle de la nota de crédito. Todo el texto dinámico (destinatario, error del proveedor,
 * mensajes del servidor) se pinta con createElement/textContent; no se escribe HTML con datos. Sin dependencias del DOM
 * real para probarlo con node --test (recibe el `document` y las dependencias de red).
 *
 * Contrato: `correo` = { comprobanteId, estadoComprobante, destinatarioPorDefecto, puedeReenviar } (null sin comprobante
 * vigente). GET/POST /api/facturas/electronica/comprobantes/{id}/entregas.
 */
(function (global) {
    'use strict';

    const ETIQUETAS = {
        PENDIENTE: 'Pendiente',
        ENVIADO: 'Enviado',
        FALLIDO: 'Fallido',
        OMITIDO_SIN_CORREO: 'Omitido: sin correo',
        OMITIDO_SANDBOX: 'Omitido: ambiente de pruebas'
    };
    const CLASES = {
        PENDIENTE: 'bg-warning text-dark',
        ENVIADO: 'bg-success',
        FALLIDO: 'bg-danger',
        OMITIDO_SIN_CORREO: 'bg-secondary',
        OMITIDO_SANDBOX: 'bg-secondary'
    };
    const TEXTO_ACCION = 'Reenviar comprobante por correo';
    const TEXTO_VACIO = 'Sin entregas por correo.';
    const ERROR_CORREO = 'Indique un correo electrónico válido';
    const ERROR_RED = 'No se pudo conectar con el servidor. Intente nuevamente.';
    const REGEX_CORREO = /^[^\s@,;]+@[^\s@,;.]+(\.[^\s@,;.]+)+$/;

    function correoValido(valor) {
        return typeof valor === 'string' && REGEX_CORREO.test(valor.trim());
    }

    function etiquetaEstado(estado) {
        if (estado == null || estado === '') return '-';
        return ETIQUETAS[estado] || String(estado);
    }

    /** Mensaje del cuerpo de error del servidor ({message} o {error}); si no hay, el texto por defecto. */
    function mensajeError(cuerpo, porDefecto) {
        for (const k of ['message', 'error']) {
            const v = cuerpo && typeof cuerpo[k] === 'string' ? cuerpo[k].trim() : '';
            if (v) return v;
        }
        return porDefecto;
    }

    function mostrarAccion(correo) {
        return !!correo && correo.puedeReenviar === true && correo.comprobanteId != null;
    }

    function formatearFecha(valor) {
        const m = typeof valor === 'string' ? /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?/.exec(valor) : null;
        if (!m) return '-';
        return m[3] + '/' + m[2] + '/' + m[1] + (m[4] ? ' ' + m[4] + ':' + m[5] : '');
    }

    function el(doc, tag, texto, clase) {
        const e = doc.createElement(tag);
        if (clase) e.className = clase;
        if (texto !== undefined && texto !== null) e.textContent = String(texto);
        return e;
    }

    function vaciar(e) {
        e.textContent = '';
    }

    /** Filas del historial: destinatario, estado, intentos, fecha y error resumido. */
    function pintarHistorial(doc, tbody, entregas) {
        vaciar(tbody);
        if (!Array.isArray(entregas) || entregas.length === 0) {
            tbody.textContent = TEXTO_VACIO;
            return;
        }
        entregas.forEach((e) => {
            const tr = el(doc, 'tr');
            tr.appendChild(el(doc, 'td', e.destinatario || '-'));
            const tdEstado = el(doc, 'td');
            tdEstado.appendChild(el(doc, 'span', etiquetaEstado(e.estado), 'badge ' + (CLASES[e.estado] || 'bg-secondary')));
            tr.appendChild(tdEstado);
            tr.appendChild(el(doc, 'td', e.intentos == null ? '0' : e.intentos));
            tr.appendChild(el(doc, 'td', formatearFecha(e.fechaEnvio || e.fechaCreacion)));
            tr.appendChild(el(doc, 'td', e.ultimoError || '-', 'small'));
            tbody.appendChild(tr);
        });
    }

    function urlEntregas(comprobanteId) {
        return '/api/facturas/electronica/comprobantes/' + encodeURIComponent(comprobanteId) + '/entregas';
    }

    /**
     * Pinta el bloque en `contenedor`. deps: fetch, headers() con CSRF y Content-Type, notify(tipo, mensaje).
     * Devuelve una promesa que se resuelve al cargar el historial.
     */
    async function montar(doc, contenedor, correo, deps) {
        vaciar(contenedor);
        if (!correo || correo.comprobanteId == null) return;
        const d = deps || {};
        const doFetch = d.fetch || global.fetch;
        const avisar = d.notify || (() => {});
        const cabeceras = d.headers || (() => ({ 'Content-Type': 'application/json' }));
        const url = urlEntregas(correo.comprobanteId);

        const tbody = el(doc, 'tbody');
        let boton = null;
        let input = null;

        async function cargarHistorial() {
            try {
                const res = await doFetch(url);
                const cuerpo = res.ok ? await res.json() : null;
                if (Array.isArray(cuerpo)) pintarHistorial(doc, tbody, cuerpo);
                else tbody.textContent = 'No se pudo cargar el historial de correos.';
            } catch (err) {
                tbody.textContent = 'No se pudo cargar el historial de correos.';
            }
        }

        async function reenviar() {
            const destinatario = (input.value || '').trim();
            if (!correoValido(destinatario)) {
                avisar('error', ERROR_CORREO);
                return;
            }
            boton.disabled = true;
            try {
                const res = await doFetch(url, {
                    method: 'POST',
                    headers: cabeceras(),
                    body: JSON.stringify({ destinatario })
                });
                const cuerpo = await res.json().catch(() => null);
                if (res.ok) {
                    avisar('success', 'Reenvío registrado: el correo se enviará en breve.');
                    await cargarHistorial();
                } else {
                    avisar('error', mensajeError(cuerpo, 'No se pudo registrar el reenvío.'));
                }
            } catch (err) {
                avisar('error', ERROR_RED);
            }
            boton.disabled = false;
        }

        contenedor.appendChild(el(doc, 'h6', 'Correo del comprobante', 'fw-bold mt-3'));
        if (mostrarAccion(correo)) {
            const grupo = el(doc, 'div', null, 'input-group input-group-sm mb-2');
            input = el(doc, 'input', null, 'form-control');
            input.setAttribute('type', 'email');
            input.setAttribute('placeholder', 'correo@ejemplo.com');
            input.setAttribute('aria-label', 'Destinatario del reenvío');
            input.value = correo.destinatarioPorDefecto || '';
            boton = el(doc, 'button', TEXTO_ACCION, 'btn btn-outline-primary');
            boton.setAttribute('type', 'button');
            boton.addEventListener('click', reenviar);
            grupo.appendChild(input);
            grupo.appendChild(boton);
            contenedor.appendChild(grupo);
        }
        const tabla = el(doc, 'table', null, 'table table-sm table-bordered align-middle');
        const cab = el(doc, 'tr');
        ['Destinatario', 'Estado', 'Intentos', 'Fecha', 'Error'].forEach((t) => cab.appendChild(el(doc, 'th', t)));
        const thead = el(doc, 'thead', null, 'table-light');
        thead.appendChild(cab);
        tabla.appendChild(thead);
        tabla.appendChild(tbody);
        const envoltorio = el(doc, 'div', null, 'table-responsive');
        envoltorio.appendChild(tabla);
        contenedor.appendChild(envoltorio);
        await cargarHistorial();
    }

    /** Dependencias del navegador: CSRF de las metas, showToast de common.js. */
    function dependenciasNavegador() {
        return {
            fetch: (u, o) => global.fetch(u, o),
            headers: () => {
                const h = { 'Content-Type': 'application/json' };
                const t = global.document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
                const n = global.document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
                if (t && n) h[n] = t;
                return h;
            },
            notify: (tipo, msg) => { if (typeof global.showToast === 'function') global.showToast(tipo, msg); }
        };
    }

    function montarEnPagina(contenedor, correo) {
        if (!contenedor || !global.document) return Promise.resolve();
        return montar(global.document, contenedor, correo, dependenciasNavegador());
    }

    global.EntregasCorreo = { correoValido, etiquetaEstado, mensajeError, mostrarAccion, pintarHistorial, montar, montarEnPagina };
})(typeof window !== 'undefined' ? window : globalThis);
