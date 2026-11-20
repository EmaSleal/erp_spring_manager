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

    // T-046 (ADR-050): el asiento lleva siempre la fecha de la nota; el código solo decide el mes del IVA.
    const ADVERTENCIA_MISMO_MES = 'La factura es de este mismo mes: el código 01 tiene el mismo efecto';
    const ZONA_FISCAL = 'America/Costa_Rica';

    const ADVERTENCIA_MISMO_MES_14 = 'La factura es de este mismo mes: el código 02 tiene el mismo efecto';
    const ERROR_LINEA_MONTO = 'El monto debe ser un número mayor que 0';
    const ERROR_LINEA_DECIMALES = 'El monto admite dos decimales como máximo';
    const ERROR_LINEA_TOPE = 'El monto supera el disponible de la línea';
    const ERROR_FACTURA_TOPE = 'La suma de los montos supera el disponible de la factura';
    const ERROR_SIN_LINEAS = 'Indique el monto a acreditar de al menos una línea';

    const CODIGOS = [
        { codigo: '01', nombre: 'Anula documento de referencia',
          efecto: 'El asiento se registra con la fecha de la nota; el IVA se atribuye al mes de la nota.',
          parcial: false },
        { codigo: '13', nombre: 'Anula documento de referencia por error material',
          efecto: 'El asiento se registra con la fecha de la nota; el IVA se atribuye al mes de la factura original',
          parcial: false },
        // T-047 (ADR-051): nota parcial, con monto por línea
        { codigo: '02', nombre: 'Corrige monto',
          efecto: 'El asiento se registra con la fecha de la nota; el IVA se atribuye al mes de la nota.',
          parcial: true },
        { codigo: '14', nombre: 'Corrige monto por error material',
          efecto: 'El asiento se registra con la fecha de la nota; el IVA se atribuye al mes de la factura original',
          parcial: true }
    ];

    /** Códigos de la modalidad: 'TOTAL' (01, 13) o 'PARCIAL' (02, 14). */
    function codigosDe(modalidad) {
        const parcial = modalidad === 'PARCIAL';
        return CODIGOS.filter((c) => c.parcial === parcial);
    }

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

    /** Mes (AAAA-MM) de una fecha en la hora de Costa Rica. */
    function mesActual(ahora) {
        const partes = new Intl.DateTimeFormat('en-CA', { timeZone: ZONA_FISCAL, year: 'numeric', month: '2-digit' })
            .formatToParts(ahora && typeof ahora.getTime === 'function' ? ahora : new Date());
        const y = partes.find((p) => p.type === 'year').value;
        const m = partes.find((p) => p.type === 'month').value;
        return y + '-' + m;
    }

    /**
     * Texto del efecto contable del código. Para el 13, con el mes (AAAA-MM) de la factura original que da el servidor
     * (periodoFacturaOriginal): «... al mes de la factura original (AAAA-MM)».
     */
    function textoEfecto(codigo, periodoOriginal) {
        const c = CODIGOS.find((x) => x.codigo === codigo);
        if (!c) return '';
        if ((codigo === '13' || codigo === '14') && typeof periodoOriginal === 'string' && /^\d{4}-\d{2}$/.test(periodoOriginal)) {
            return c.efecto + ' (' + periodoOriginal + ')';
        }
        return c.efecto;
    }

    /** Advertencia fija, no bloqueante: la nota 13 sobre una factura del mes en curso equivale a la 01 (ADR-050 §4). */
    function advertenciaMismoMes(codigo, periodoOriginal, ahora) {
        if ((codigo !== '13' && codigo !== '14') || typeof periodoOriginal !== 'string'
            || periodoOriginal !== mesActual(ahora)) {
            return '';
        }
        return codigo === '14' ? ADVERTENCIA_MISMO_MES_14 : ADVERTENCIA_MISMO_MES;
    }

    function nombreCodigo(codigo) {
        const c = CODIGOS.find((x) => x.codigo === codigo);
        return c ? c.nombre : '';
    }

    /** Pedido de la nota; `lineas` ([{idLineaFactura, montoAcreditar}]) solo en la nota parcial (02/14). */
    function cuerpoPedido(idFactura, codigo, razon, lineas) {
        const cuerpo = {
            idFactura: idFactura,
            tipo: 'CREDITO',
            codigoReferencia: codigo,
            razon: typeof razon === 'string' ? razon.trim() : ''
        };
        if (Array.isArray(lineas) && lineas.length > 0) cuerpo.lineas = lineas;
        return cuerpo;
    }

    // ---- Nota parcial (T-047): montos por línea con IVA incluido, topes y total en vivo ----

    /** Céntimos enteros de un monto con a lo sumo dos decimales; null si no es un número decimal simple. */
    function aCentimos(valor) {
        const m = /^(\d+)(?:\.(\d{1,2}))?$/.exec(String(valor).trim());
        return m ? Number(m[1]) * 100 + Number(((m[2] || '') + '00').slice(0, 2)) : null;
    }

    function deCentimos(c) {
        return Math.floor(c / 100) + '.' + String(c % 100).padStart(2, '0');
    }

    /**
     * Error del monto escrito en una línea frente a su disponible, o null. Vacío = la línea no se acredita (sin error).
     * Un número con más de dos decimales, cero o negativo, o mayor que el disponible se rechaza; el límite exacto pasa.
     */
    function validarMontoLinea(texto, disponible) {
        const t = typeof texto === 'string' ? texto.trim() : '';
        if (t === '') return null;
        if (!/^-?\d+(\.\d+)?$/.test(t)) return 'El monto debe ser un número';
        const c = aCentimos(t);
        if (c === null) return t.startsWith('-') ? ERROR_LINEA_MONTO : ERROR_LINEA_DECIMALES;
        if (c <= 0) return ERROR_LINEA_MONTO;
        const d = Math.round(Number(disponible) * 100);
        return c > d ? ERROR_LINEA_TOPE : null;
    }

    /**
     * Estado de la nota parcial a partir de las líneas del servidor (notaCreditoLineas), lo escrito por línea
     * ({idLineaFactura: texto}) y el disponible de la factura (notaCreditoDisponible).
     * Devuelve {total, lineas, errores, errorGeneral, valido}; el total en vivo se suma en céntimos.
     */
    function calcularParcial(lineasServidor, entradas, disponibleFactura) {
        const errores = {};
        const lineas = [];
        let suma = 0;
        (lineasServidor || []).forEach((l) => {
            const texto = entradas ? entradas[l.idLineaFactura] : '';
            const error = validarMontoLinea(texto, l.disponible);
            if (error) {
                errores[l.idLineaFactura] = error;
                return;
            }
            const c = typeof texto === 'string' && texto.trim() !== '' ? aCentimos(texto) : null;
            if (c !== null) {
                suma += c;
                lineas.push({ idLineaFactura: l.idLineaFactura, montoAcreditar: Number(deCentimos(c)) });
            }
        });
        let errorGeneral = '';
        if (Object.keys(errores).length === 0) {
            if (lineas.length === 0) errorGeneral = ERROR_SIN_LINEAS;
            else if (suma > Math.round(Number(disponibleFactura) * 100)) errorGeneral = ERROR_FACTURA_TOPE;
        }
        return {
            total: deCentimos(suma), lineas: lineas, errores: errores, errorGeneral: errorGeneral,
            valido: Object.keys(errores).length === 0 && errorGeneral === ''
        };
    }

    /** «Total» o «Parcial» según la bandera `parcial` que da el servidor. */
    function textoModalidad(nota) {
        return nota && nota.parcial === true ? 'Parcial' : 'Total';
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

    /** Fecha sin hora (AAAA-MM-DD, p. ej. la fecha contable) como dd/mm/aaaa, sin desfase de zona horaria. */
    function formatearDia(valor) {
        const m = typeof valor === 'string' ? /^(\d{4})-(\d{2})-(\d{2})/.exec(valor) : null;
        return m ? m[3] + '/' + m[2] + '/' + m[1] : '-';
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
     * Tabla de líneas de la nota parcial: línea, descripción, total, acreditado, disponible y el campo de monto (IVA
     * incluido). Todo por createElement + textContent; `onInput(input)` se engancha a cada campo.
     */
    function pintarLineasParcial(doc, cuerpo, lineas, moneda, onInput) {
        (lineas || []).forEach((l) => {
            const tr = doc.createElement('tr');
            tr.appendChild(celda(doc, String(l.numeroLinea == null ? '' : l.numeroLinea)));
            tr.appendChild(celda(doc, l.descripcion || '-'));
            tr.appendChild(celda(doc, formatearMonto(l.total, moneda), 'text-end'));
            tr.appendChild(celda(doc, formatearMonto(l.acreditado, moneda), 'text-end'));
            tr.appendChild(celda(doc, formatearMonto(l.disponible, moneda), 'text-end'));
            const input = el(doc, 'input', null, 'form-control form-control-sm text-end nc-monto-linea');
            input.setAttribute('type', 'text');
            input.setAttribute('inputmode', 'decimal');
            input.setAttribute('data-id-linea', String(l.idLineaFactura));
            input.setAttribute('aria-label', 'Monto a acreditar (IVA incluido) de la línea ' + l.numeroLinea);
            if (Number(l.disponible) <= 0) input.setAttribute('disabled', 'disabled');
            if (onInput) input.addEventListener('input', () => onInput(input));
            const feedback = el(doc, 'div', null, 'invalid-feedback d-block nc-monto-error');
            const td = celda(doc, input);
            td.appendChild(feedback);
            tr.appendChild(td);
            cuerpo.appendChild(tr);
        });
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
        const tdCodigo = celda(doc, (n.codigoReferencia || '') + ' - ' + (n.codigoNombre || ''));
        tdCodigo.appendChild(el(doc, 'span', textoModalidad(n), 'badge ms-1 '
            + (n.parcial === true ? 'bg-info text-dark' : 'bg-secondary')));
        tr.appendChild(tdCodigo);
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
        codigosDe, aCentimos, deCentimos, validarMontoLinea, calcularParcial, textoModalidad, pintarLineasParcial,
        ERROR_SIN_LINEAS, ERROR_LINEA_TOPE, ERROR_FACTURA_TOPE,
        estadoBoton, contadorRazon, validarRazon, textoEfecto, advertenciaMismoMes, mesActual, ADVERTENCIA_MISMO_MES, nombreCodigo, cuerpoPedido,
        mensajeResultado, mensajeDeError, formatearMonto, resumen,
        badgeNota, badgeComprobante, queryListado, textoPagina, resultadoReenvio, resultadoConsulta, formatearFecha, formatearDia,
        el, badge, enlace, boton, botonAccionComprobante, ambienteDe, celda, filaNota
    };
})(typeof window !== 'undefined' ? window : globalThis);
