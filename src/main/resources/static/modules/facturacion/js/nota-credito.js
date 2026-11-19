// T-042 (ADR-047): lógica de la nota de crédito en la interfaz (modal de emisión, listado y detalle). Sin DOM propio:
// los helpers de pintado reciben el `document` (o una fábrica equivalente) y solo usan createElement/textContent, para
// probarlos con node (src/test/js/nota-credito.test.mjs). Ningún dato dinámico pasa por innerHTML.
(function (global) {
    'use strict';

    const RAZON_MIN = 3;
    const RAZON_MAX = 180;
    const MOTIVO_PAGOS = 'La nota de crédito sobre facturas con pagos llega en una próxima versión';
    const AVISO_RESUMEN = 'La factura quedará anulada cuando Hacienda acepte la nota; después podrá emitir la factura sustituta';
    const ERROR_RAZON = 'La razón es obligatoria y debe tener entre ' + RAZON_MIN + ' y ' + RAZON_MAX + ' caracteres';
    const MENSAJE_EMITIDA = 'Nota de crédito emitida.';

    const CODIGOS = [
        { codigo: '01', nombre: 'Anula documento de referencia',
          efecto: 'El asiento de reversión se registra en el periodo de la fecha de la nota.' },
        { codigo: '13', nombre: 'Anula documento de referencia por error material',
          efecto: 'El asiento de reversión va al periodo de la factura original.' }
    ];

    // ---- Botón «Emitir nota de crédito» (solo banderas del servidor; sin roles) ----

    /** {visible, habilitado, motivo} a partir de puedeEmitirNotaCredito / motivoNotaCreditoNoDisponible. */
    function estadoBoton(detalle) {
        if (detalle && detalle.puedeEmitirNotaCredito === true) {
            return { visible: true, habilitado: true, motivo: null };
        }
        const motivo = detalle && typeof detalle.motivoNotaCreditoNoDisponible === 'string'
            ? detalle.motivoNotaCreditoNoDisponible.trim() : '';
        if (motivo) {
            return { visible: true, habilitado: false, motivo: motivo };
        }
        return { visible: false, habilitado: false, motivo: null };
    }

    // ---- Razón ----

    function contadorRazon(texto) {
        const n = typeof texto === 'string' ? texto.length : 0;
        return n + '/' + RAZON_MAX;
    }

    /** null si la razón (recortada) tiene de 3 a 180 caracteres; si no, el texto fijo del error. */
    function validarRazon(texto) {
        const r = typeof texto === 'string' ? texto.trim() : '';
        return r.length >= RAZON_MIN && r.length <= RAZON_MAX ? null : ERROR_RAZON;
    }

    function textoEfecto(codigo) {
        const c = CODIGOS.find((x) => x.codigo === codigo);
        return c ? c.efecto : '';
    }

    function nombreCodigo(codigo) {
        const c = CODIGOS.find((x) => x.codigo === codigo);
        return c ? c.nombre : '';
    }

    function cuerpoPedido(idFactura, codigo, razon) {
        return {
            idFactura: idFactura,
            tipo: 'CREDITO',
            codigoReferencia: codigo,
            razon: typeof razon === 'string' ? razon.trim() : ''
        };
    }

    // ---- Mensajes ----

    function mensajeServidor(cuerpo) {
        return cuerpo && typeof cuerpo.message === 'string' ? cuerpo.message.trim() : '';
    }

    /** Resultado de la emisión: el mensaje fijo del servidor o, si falta, uno fijo propio. */
    function mensajeResultado(cuerpo) {
        return mensajeServidor(cuerpo) || MENSAJE_EMITIDA;
    }

    /** Mensaje del error por estado HTTP: 400/404/409/422 con el texto del servidor si existe; si no, uno fijo. */
    function mensajeDeError(status, cuerpo) {
        const servidor = mensajeServidor(cuerpo);
        switch (status) {
            case 400: return servidor || 'Los datos de la nota de crédito no son válidos';
            case 404: return servidor || 'La factura no existe';
            case 409: return servidor || 'La nota de crédito no se puede emitir en el estado actual de la factura';
            case 422: return servidor || 'El comprobante de la nota no pudo generarse';
            default: return 'No se pudo emitir la nota de crédito. Intente de nuevo';
        }
    }

    // ---- Resumen previo a confirmar ----

    function simboloMoneda(moneda) {
        return { CRC: '₡', USD: '$', EUR: '€' }[moneda] || '';
    }

    function formatearMonto(valor, moneda) {
        const n = Number(valor);
        if (valor === null || valor === undefined || valor === '' || Number.isNaN(n)) return '-';
        const partes = Math.abs(n).toFixed(2).split('.');
        const entero = partes[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        return (n < 0 ? '-' : '') + simboloMoneda(moneda) + entero + '.' + partes[1];
    }

    /** Campos del resumen a partir del FacturaDetalleDTO. */
    function resumen(detalle) {
        const d = detalle || {};
        const moneda = d.monedaFE || 'CRC';
        return {
            factura: d.numeroFactura || ('Factura ' + (d.idFactura == null ? '' : d.idFactura)).trim(),
            cliente: d.cliente && d.cliente.nombre ? d.cliente.nombre : '-',
            monto: formatearMonto(d.total, moneda),
            moneda: moneda,
            aviso: AVISO_RESUMEN
        };
    }

    // ---- Listado / detalle ----

    const ESTADOS_NOTA = {
        EN_TRAMITE: { texto: 'En trámite', clase: 'bg-warning text-dark' },
        APLICADA: { texto: 'Aplicada', clase: 'bg-success' },
        RECHAZADA: { texto: 'Rechazada', clase: 'bg-danger' }
    };

    const ESTADOS_COMPROBANTE = {
        ACEPTADO: { texto: 'Aceptado', clase: 'bg-success' },
        PROCESANDO: { texto: 'Procesando', clase: 'bg-warning text-dark' },
        RECHAZADO: { texto: 'Rechazado', clase: 'bg-danger' },
        FIRMADO: { texto: 'Firmado', clase: 'bg-info' },
        ENVIADO: { texto: 'Enviado', clase: 'bg-primary' },
        ERROR: { texto: 'Error', clase: 'bg-danger' },
        NO_TRANSMITIDO: { texto: 'No transmitido', clase: 'bg-secondary' },
        ENVIO_INCIERTO: { texto: 'Envío incierto', clase: 'bg-warning text-dark' },
        ERROR_HACIENDA: { texto: 'Error en Hacienda', clase: 'bg-danger' },
        ANULADO: { texto: 'Anulado', clase: 'bg-secondary' },
        GENERADO: { texto: 'Generado', clase: 'bg-light text-dark' }
    };

    function badgeNota(estado) {
        return ESTADOS_NOTA[estado] || { texto: estado || '-', clase: 'bg-secondary' };
    }

    /** Badge del comprobante; ENVIO_INCIERTO reciente se muestra «Enviando…» (T-037, EstadoEnvio). */
    function badgeComprobante(estado, fechaEnvio, ahora) {
        if (!estado) return { texto: 'Sin comprobante', clase: 'bg-secondary' };
        const envio = global.EstadoEnvio;
        if (envio && envio.estaEnviando(estado, fechaEnvio, ahora)) {
            return { texto: 'Enviando…', clase: 'bg-info text-dark' };
        }
        return ESTADOS_COMPROBANTE[estado] || { texto: estado, clase: 'bg-secondary' };
    }

    /** Query del listado: solo los filtros con valor. */
    function queryListado(filtros, pagina, tamano) {
        const f = filtros || {};
        const p = new URLSearchParams();
        ['desde', 'hasta', 'cliente', 'estado', 'ambiente'].forEach((k) => {
            const v = typeof f[k] === 'string' ? f[k].trim() : '';
            if (v) p.set(k, v);
        });
        p.set('page', String(pagina || 0));
        p.set('size', String(tamano || 20));
        return p.toString();
    }

    /** Texto de la página: «Mostrando 1-20 de 45» / «Sin notas de crédito». */
    function textoPagina(pag) {
        if (!pag || !pag.totalElementos) return 'Sin notas de crédito';
        const desde = pag.pagina * pag.tamano + 1;
        const hasta = Math.min((pag.pagina + 1) * pag.tamano, pag.totalElementos);
        return 'Mostrando ' + desde + '-' + hasta + ' de ' + pag.totalElementos
            + ' (página ' + (pag.pagina + 1) + ' de ' + Math.max(pag.totalPaginas || 1, 1) + ')';
    }

    /** Resultado de «Reenviar»: mensajes fijos del servidor con respaldo (igual que comprobantes). */
    function resultadoReenvio(ok, cuerpo) {
        const m = mensajeServidor(cuerpo);
        if (ok && cuerpo && cuerpo.success) return { tipo: 'success', mensaje: m || 'Comprobante reenviado' };
        return { tipo: ok ? 'warning' : 'error', mensaje: m || 'No se pudo reenviar el comprobante' };
    }

    /** Resultado de «Consultar estado»: 429 informativo (disponibleDesde viaja en el mensaje del servidor). */
    function resultadoConsulta(status, cuerpo) {
        const m = mensajeServidor(cuerpo);
        if (cuerpo && cuerpo.success) return { tipo: 'success', mensaje: m || 'Estado consultado', exito: true };
        return { tipo: status === 429 ? 'info' : 'warning', mensaje: m || 'No se pudo consultar el estado', exito: false };
    }

    function formatearFecha(valor, conHora) {
        if (!valor) return '-';
        const d = new Date(valor);
        if (Number.isNaN(d.getTime())) return '-';
        const dos = (n) => String(n).padStart(2, '0');
        const dia = dos(d.getDate()) + '/' + dos(d.getMonth() + 1) + '/' + d.getFullYear();
        return conHora ? dia + ' ' + dos(d.getHours()) + ':' + dos(d.getMinutes()) : dia;
    }

    // ---- Pintado seguro (solo createElement + textContent) ----

    function el(doc, tag, texto, clase) {
        const e = doc.createElement(tag);
        if (clase) e.className = clase;
        if (texto !== undefined && texto !== null) e.textContent = String(texto);
        return e;
    }

    function badge(doc, b) {
        return el(doc, 'span', b.texto, 'badge ' + b.clase);
    }

    function enlace(doc, texto, href, clase) {
        const a = el(doc, 'a', texto, clase);
        a.setAttribute('href', href);
        return a;
    }

    function boton(doc, texto, clase, onClick) {
        const b = el(doc, 'button', texto, clase);
        b.setAttribute('type', 'button');
        if (onClick) b.addEventListener('click', onClick);
        return b;
    }

    /**
     * T-044 (P-099): badge del ambiente del comprobante y bloqueo de «Reenviar»/«Consultar estado» según las banderas
     * del servidor (ambiente-comprobante.js, que se carga después; se resuelve al pintar, no al cargar).
     */
    function ambienteDe(c) {
        const AC = global.AmbienteComprobante;
        return {
            etiqueta: AC && c && c.ambiente ? AC.etiqueta(c.ambiente) : null,
            bloqueo: AC ? AC.bloqueo(c) : { bloqueado: false, motivo: null }
        };
    }

    /** Botón de acción del comprobante: deshabilitado, con el motivo fijo como título, si el ambiente no coincide. */
    function botonAccionComprobante(doc, texto, clase, bloqueo, onClick) {
        if (!bloqueo.bloqueado) {
            return boton(doc, texto, clase, onClick);
        }
        const b = boton(doc, texto, clase, null);
        b.setAttribute('disabled', 'disabled');
        if (bloqueo.motivo) b.setAttribute('title', bloqueo.motivo);
        return b;
    }

    function celda(doc, hijo, clase) {
        const td = el(doc, 'td', null, clase);
        if (typeof hijo === 'string') td.textContent = hijo;
        else if (hijo) td.appendChild(hijo);
        return td;
    }

    /**
     * Fila del listado. acciones: {verFactura(n), consultar(n, boton), reenviar(n, boton)}.
     * Todo el texto dinámico (número, cliente, estados...) entra por textContent.
     */
    function filaNota(doc, n, acciones) {
        const tr = doc.createElement('tr');
        const id = encodeURIComponent(n.id);
        tr.appendChild(celda(doc, enlace(doc, n.numero, '/facturas/notas-credito/' + id, 'fw-bold text-decoration-none')));
        const fac = enlace(doc, n.numeroFactura || ('#' + n.idFactura), '#', 'text-decoration-none');
        fac.addEventListener('click', (ev) => {
            if (ev && ev.preventDefault) ev.preventDefault();
            acciones.verFactura(n);
        });
        tr.appendChild(celda(doc, fac));
        tr.appendChild(celda(doc, n.clienteNombre || '-'));
        tr.appendChild(celda(doc, (n.codigoReferencia || '') + ' - ' + (n.codigoNombre || '')));
        tr.appendChild(celda(doc, formatearMonto(n.total, n.moneda) + ' ' + (n.moneda || ''), 'text-end'));
        tr.appendChild(celda(doc, formatearFecha(n.fechaEmision, true)));
        tr.appendChild(celda(doc, badge(doc, badgeNota(n.estado))));
        const amb = ambienteDe(n);
        const celdaComprobante = el(doc, 'div');
        celdaComprobante.appendChild(badge(doc, badgeComprobante(n.estadoComprobante, n.fechaEnvioComprobante)));
        if (amb.etiqueta) {
            celdaComprobante.appendChild(el(doc, 'span', amb.etiqueta.texto, 'badge ms-1 ' + amb.etiqueta.clase));
        }
        tr.appendChild(celda(doc, celdaComprobante));
        const acc = el(doc, 'div', null, 'd-flex flex-wrap gap-1');
        acc.appendChild(enlace(doc, 'Ver detalle', '/facturas/notas-credito/' + id, 'btn btn-sm btn-outline-primary'));
        const pdf = enlace(doc, 'PDF', '/facturas/notas-credito/' + id + '/pdf', 'btn btn-sm btn-outline-secondary');
        pdf.setAttribute('target', '_blank');
        acc.appendChild(pdf);
        if (n.consultableEnHacienda) {
            const b = botonAccionComprobante(doc, 'Consultar estado', 'btn btn-sm btn-outline-info', amb.bloqueo, null);
            if (!amb.bloqueo.bloqueado) b.addEventListener('click', () => acciones.consultar(n, b));
            acc.appendChild(b);
        }
        if (n.puedeReenviar) {
            const b = botonAccionComprobante(doc, 'Reenviar', 'btn btn-sm btn-outline-warning', amb.bloqueo, null);
            if (!amb.bloqueo.bloqueado) b.addEventListener('click', () => acciones.reenviar(n, b));
            acc.appendChild(b);
        }
        if (amb.bloqueo.bloqueado && amb.bloqueo.motivo && (n.consultableEnHacienda || n.puedeReenviar)) {
            acc.appendChild(el(doc, 'small', amb.bloqueo.motivo, 'text-danger w-100 motivo-ambiente'));
        }
        tr.appendChild(celda(doc, acc));
        return tr;
    }

    global.NotaCredito = {
        RAZON_MIN, RAZON_MAX, MOTIVO_PAGOS, AVISO_RESUMEN, ERROR_RAZON, CODIGOS,
        estadoBoton, contadorRazon, validarRazon, textoEfecto, nombreCodigo, cuerpoPedido,
        mensajeResultado, mensajeDeError, formatearMonto, resumen,
        badgeNota, badgeComprobante, queryListado, textoPagina, resultadoReenvio, resultadoConsulta, formatearFecha,
        el, badge, enlace, boton, botonAccionComprobante, ambienteDe, celda, filaNota
    };
})(typeof window !== 'undefined' ? window : globalThis);
