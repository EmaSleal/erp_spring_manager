// T-042 (ADR-047): detalle de la nota de crédito. Datos de GET /api/facturas/notas-credito/{id}/detalle; todo el texto
// dinámico se pinta con createElement/textContent (helpers de nota-credito.js).
(function () {
    'use strict';

    const NC = NotaCredito;
    let notaId = null;
    let detalle = null;

    function $(id) {
        return document.getElementById(id);
    }

    function csrfHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        if (token && header) headers[header] = token;
        return headers;
    }

    function vaciar(e) {
        e.textContent = '';
    }

    /** Par «etiqueta / valor» de una lista de definición; el valor puede ser texto o un nodo. */
    function par(dl, etiqueta, valor) {
        dl.appendChild(NC.el(document, 'dt', etiqueta, 'col-sm-5'));
        const dd = NC.el(document, 'dd', null, 'col-sm-7');
        if (valor instanceof Node) dd.appendChild(valor);
        else dd.textContent = valor == null || valor === '' ? '-' : String(valor);
        dl.appendChild(dd);
    }

    function pintarDatos(d) {
        const dl = $('nd-datos');
        vaciar(dl);
        par(dl, 'Número', d.numero);
        par(dl, 'Estado', NC.badge(document, NC.badgeNota(d.estado)));
        par(dl, 'Código', d.codigoReferencia + ' - ' + (d.codigoNombre || ''));
        par(dl, 'Razón', d.razon);
        par(dl, 'Fecha de emisión', NC.formatearFecha(d.fechaEmision, true));
        par(dl, 'Fecha de aplicación', NC.formatearFecha(d.fechaAplicacion, true));
        par(dl, 'Moneda', d.moneda);
        if (d.moneda && d.moneda !== 'CRC') par(dl, 'Tipo de cambio', d.tipoCambio);
        par(dl, 'Cliente', d.clienteNombre);
        par(dl, 'Usuario', d.usuarioCreador);
    }

    function pintarReferencia(d) {
        const dl = $('nd-referencia');
        vaciar(dl);
        let factura = d.numeroFactura || '-';
        if (d.idFactura) {
            factura = NC.enlace(document, d.numeroFactura || ('#' + d.idFactura), '#', 'text-decoration-none');
            factura.addEventListener('click', (ev) => {
                ev.preventDefault();
                openModal({ getAttribute: () => String(d.idFactura) });
            });
        }
        par(dl, 'Factura', factura);
        const clave = NC.el(document, 'span', d.claveReferencia || '-', 'font-monospace small');
        par(dl, 'Clave', clave);
        par(dl, 'Código', d.codigoReferencia + ' - ' + (d.codigoNombre || ''));
        par(dl, 'Razón', d.razon);
        par(dl, 'Fecha de emisión de la factura (FechaEmisionIR)', NC.formatearFecha(d.fechaEmisionReferencia, true));
    }

    function pintarLineas(d) {
        const cuerpo = $('nd-lineas');
        vaciar(cuerpo);
        (d.lineas || []).forEach((l) => {
            const tr = document.createElement('tr');
            const iva = NC.formatearMonto(l.montoIva, d.moneda) + (l.tarifaIva != null ? ' (' + l.tarifaIva + '%)' : '');
            tr.appendChild(NC.celda(document, l.producto));
            tr.appendChild(NC.celda(document, l.descripcion));
            tr.appendChild(NC.celda(document, l.codigoCabys, 'font-monospace'));
            tr.appendChild(NC.celda(document, l.cantidad, 'text-end'));
            tr.appendChild(NC.celda(document, l.unidadMedida));
            tr.appendChild(NC.celda(document, NC.formatearMonto(l.precioUnitario, d.moneda), 'text-end'));
            tr.appendChild(NC.celda(document, iva, 'text-end'));
            tr.appendChild(NC.celda(document, NC.formatearMonto(l.total, d.moneda), 'text-end'));
            cuerpo.appendChild(tr);
        });
        const t = $('nd-totales');
        vaciar(t);
        par(t, 'Subtotal', NC.formatearMonto(d.subtotal, d.moneda));
        par(t, 'Descuento', NC.formatearMonto(d.descuento, d.moneda));
        par(t, 'Impuesto', NC.formatearMonto(d.impuesto, d.moneda));
        par(t, 'Total', NC.formatearMonto(d.total, d.moneda) + ' ' + (d.moneda || ''));
    }

    function pintarComprobante(d) {
        const c = d.comprobante;
        const info = $('nd-comprobante');
        const acciones = $('nd-comprobante-acciones');
        vaciar(info);
        vaciar(acciones);
        if (!c) {
            info.textContent = 'La nota no tiene comprobante electrónico.';
        } else {
            const dl = NC.el(document, 'dl', null, 'row mb-0');
            par(dl, 'Estado', NC.badge(document, NC.badgeComprobante(c.estado, c.fechaEnvio)));
            const amb = NC.ambienteDe(c);
            if (amb.etiqueta) {
                par(dl, 'Ambiente', NC.el(document, 'span', amb.etiqueta.texto, 'badge ' + amb.etiqueta.clase));
            }
            par(dl, 'Consecutivo', c.consecutivo);
            par(dl, 'Clave', NC.el(document, 'span', c.clave || '-', 'font-monospace small'));
            par(dl, 'Fecha de envío', NC.formatearFecha(c.fechaEnvio, true));
            par(dl, 'Fecha de respuesta', NC.formatearFecha(c.fechaRespuesta, true));
            par(dl, 'Mensaje de Hacienda', (c.codigoRespuesta ? c.codigoRespuesta + ' - ' : '') + (c.mensajeRespuesta || ''));
            par(dl, 'Último error', c.ultimoError);
            info.appendChild(dl);
            if (c.consultableEnHacienda) {
                acciones.appendChild(NC.botonAccionComprobante(document, 'Consultar estado',
                    'btn btn-sm btn-outline-info', amb.bloqueo, (ev) => consultarEstado(c, ev.currentTarget)));
            }
            if (c.puedeReenviar) {
                acciones.appendChild(NC.botonAccionComprobante(document, 'Reenviar',
                    'btn btn-sm btn-outline-warning', amb.bloqueo, (ev) => reenviar(c, ev.currentTarget)));
            }
            if (amb.bloqueo.bloqueado && amb.bloqueo.motivo && (c.consultableEnHacienda || c.puedeReenviar)) {
                acciones.appendChild(NC.el(document, 'div', amb.bloqueo.motivo, 'small text-danger mt-1 motivo-ambiente'));
            }
        }
        const cuerpo = $('nd-historial');
        vaciar(cuerpo);
        (d.historial || []).forEach((h) => {
            const tr = document.createElement('tr');
            tr.appendChild(NC.celda(document, NC.badge(document, NC.badgeComprobante(h.estado, h.fechaEnvio))));
            tr.appendChild(NC.celda(document, NC.formatearFecha(h.fechaEnvio, true)));
            tr.appendChild(NC.celda(document, NC.formatearFecha(h.fechaRespuesta, true)));
            tr.appendChild(NC.celda(document, h.codigoRespuesta || '-'));
            tr.appendChild(NC.celda(document, h.mensajeRespuesta || '-'));
            tr.appendChild(NC.celda(document, h.ultimoError || '-'));
            cuerpo.appendChild(tr);
        });
    }

    function pintarAsiento(d) {
        const card = $('nd-asiento-card');
        if (!d.idAsientoReversion) {
            card.style.display = 'none';
            return;
        }
        const cont = $('nd-asiento');
        vaciar(cont);
        cont.appendChild(NC.enlace(document, d.numeroAsientoReversion || ('Asiento ' + d.idAsientoReversion),
            '/contabilidad/asientos/' + encodeURIComponent(d.idAsientoReversion), 'text-decoration-none'));
        card.style.display = '';
    }

    function pintar(d) {
        detalle = d;
        pintarDatos(d);
        pintarReferencia(d);
        pintarLineas(d);
        pintarComprobante(d);
        pintarAsiento(d);
        // T-044 (P-099): con otro ambiente que el activo el botón se ve deshabilitado, con el motivo fijo
        const botonSustituta = $('nd-sustituta');
        const motivoSustituta = typeof d.motivoSustitutaNoDisponible === 'string' ? d.motivoSustitutaNoDisponible : '';
        const sustitutaBloqueada = d.puedeEmitirSustituta !== true && motivoSustituta !== '';
        botonSustituta.style.display = d.puedeEmitirSustituta === true || sustitutaBloqueada ? '' : 'none';
        botonSustituta.disabled = sustitutaBloqueada;
        botonSustituta.title = sustitutaBloqueada ? motivoSustituta : '';
        const textoMotivoSustituta = $('nd-sustituta-motivo');
        if (textoMotivoSustituta) {
            textoMotivoSustituta.textContent = sustitutaBloqueada ? motivoSustituta : '';
            textoMotivoSustituta.style.display = sustitutaBloqueada ? '' : 'none';
        }
        $('nd-contenido').style.display = '';
    }

    function error(texto) {
        const e = $('nd-error');
        e.textContent = texto;
        e.style.display = '';
    }

    async function cargar() {
        try {
            const res = await fetch('/api/facturas/notas-credito/' + encodeURIComponent(notaId) + '/detalle');
            const cuerpo = await res.json().catch(() => null);
            if (!res.ok || !cuerpo || !cuerpo.data) {
                error(res.status === 404 ? 'La nota de crédito no existe' : 'No se pudo cargar la nota de crédito');
                return;
            }
            pintar(cuerpo.data);
        } catch (e) {
            error('No se pudo cargar la nota de crédito');
        }
    }

    // Mismas acciones y mensajes que comprobantes.html
    async function reenviar(c, boton) {
        if (!confirm('¿Reenviar comprobante a Hacienda?')) return;
        boton.disabled = true;
        try {
            const res = await fetch('/api/facturas/electronica/comprobantes/' + encodeURIComponent(c.id) + '/reenviar',
                { method: 'POST', headers: csrfHeaders() });
            const cuerpo = await res.json().catch(() => ({}));
            const r = NC.resultadoReenvio(res.ok, cuerpo);
            showToast(r.tipo, r.mensaje);
        } catch (e) {
            showToast('error', 'Error reenviando comprobante');
        }
        cargar();
    }

    async function consultarEstado(c, boton) {
        boton.disabled = true;
        try {
            const res = await fetch('/api/facturas/electronica/comprobantes/' + encodeURIComponent(c.id)
                + '/consultar-estado-hacienda', { method: 'POST', headers: csrfHeaders() });
            const cuerpo = await res.json().catch(() => ({}));
            const r = NC.resultadoConsulta(res.status, cuerpo);
            showToast(r.tipo, r.mensaje);
            if (r.exito) {
                cargar();
                return;
            }
        } catch (e) {
            showToast('error', 'Error consultando el estado en Hacienda');
        }
        boton.disabled = false;
    }

    /** Misma acción que el modal de la factura (facturas.js emitirFacturaSustituta): POST /api/facturas/{id}/sustituta. */
    async function emitirSustituta() {
        const boton = $('nd-sustituta');
        if (!detalle || !detalle.idFactura) return;
        boton.disabled = true;
        try {
            const res = await fetch('/api/facturas/' + encodeURIComponent(detalle.idFactura) + '/sustituta', {
                method: 'POST',
                headers: csrfHeaders(),
                body: JSON.stringify(SustitucionFactura.cuerpoPedido())
            });
            const cuerpo = await res.json().catch(() => null);
            if (!res.ok || !cuerpo || !cuerpo.data || !cuerpo.data.idFactura) {
                showToast('error', SustitucionFactura.mensajeDeError(cuerpo));
                boton.disabled = false;
                return;
            }
            window.location.href = '/facturas/editar/' + encodeURIComponent(cuerpo.data.idFactura);
        } catch (e) {
            showToast('error', SustitucionFactura.mensajeDeError(null));
            boton.disabled = false;
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        notaId = $('nc-detalle').getAttribute('data-nota-id');
        $('nd-pdf').setAttribute('href', '/facturas/notas-credito/' + encodeURIComponent(notaId) + '/pdf');
        $('nd-sustituta').addEventListener('click', emitirSustituta);
        cargar();
    });
})();
