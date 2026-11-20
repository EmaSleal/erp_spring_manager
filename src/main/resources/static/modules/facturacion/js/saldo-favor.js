// T-048 (B1, ADR-053): interfaz del saldo a favor del cliente. Opción «Aplicar saldo a favor» del registro de pago,
// pagos NOTA_CREDITO de la factura con su reversión, estado de cuenta y reparto de la nota. Requiere nota-credito.js
// (helpers el/celda/formatearMonto/formatearDia). Todo texto dinámico se pinta con createElement + textContent; las
// pruebas están en src/test/js/saldo-favor.test.mjs.
(function (global) {
    'use strict';

    const API = '/api/facturas/saldos-favor';
    const NC = global.NotaCredito;
    const ERROR_GENERICO = 'No se pudo completar la operación';
    const ERROR_MONTO = 'El monto debe ser mayor a 0 y tener a lo sumo dos decimales';
    const ERROR_TOPE_SALDO = 'El monto excede el saldo vivo del saldo a favor';
    const ERROR_TOPE_FACTURA = 'El monto excede el saldo pendiente de la factura destino';
    const ERROR_RAZON = 'La razón es obligatoria y debe tener entre 3 y 180 caracteres';
    const RAZON_MIN = 3;
    const RAZON_MAX = 180;

    const centimos = (n) => Math.round(Number(n) * 100);
    const money = (valor, moneda) => NC.formatearMonto(valor, moneda);

    // ---- validación ----

    /** Monto de la aplicación: > 0, a lo sumo dos decimales, ≤ saldo vivo y ≤ saldo pendiente de la factura. */
    function validarMonto(texto, saldoVivo, saldoPendiente) {
        const t = typeof texto === 'string' ? texto.trim() : String(texto == null ? '' : texto);
        if (!/^\d+(\.\d{1,2})?$/.test(t) || centimos(t) <= 0) return { valido: false, mensaje: ERROR_MONTO };
        if (centimos(t) > centimos(saldoVivo)) return { valido: false, mensaje: ERROR_TOPE_SALDO };
        if (centimos(t) > centimos(saldoPendiente)) return { valido: false, mensaje: ERROR_TOPE_FACTURA };
        return { valido: true, mensaje: '' };
    }

    function montoMaximo(saldoVivo, saldoPendiente) {
        return (Math.min(centimos(saldoVivo), centimos(saldoPendiente)) / 100).toFixed(2);
    }

    function validarRazon(razon) {
        const t = typeof razon === 'string' ? razon.trim() : '';
        const valido = t.length >= RAZON_MIN && t.length <= RAZON_MAX;
        return { valido, mensaje: valido ? '' : ERROR_RAZON };
    }

    function cuerpoReversion(razon) {
        return JSON.stringify({ razon: String(razon == null ? '' : razon).trim() });
    }

    function cuerpoAplicacion(idFactura, monto) {
        return JSON.stringify({ idFactura: Number(idFactura), monto: Number(monto) });
    }

    // ---- red ----

    function csrfHeaders(doc) {
        const headers = { 'Content-Type': 'application/json' };
        if (doc && typeof doc.querySelector === 'function') {
            const token = doc.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const header = doc.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            if (token && header) headers[header] = token;
        }
        return headers;
    }

    async function leer(respuesta) {
        try {
            return await respuesta.json();
        } catch (e) {
            return null;
        }
    }

    function mensajeDe(cuerpo) {
        return cuerpo && typeof cuerpo.message === 'string' && cuerpo.message.trim() ? cuerpo.message : ERROR_GENERICO;
    }

    async function enviar(fetchFn, url, cuerpo, doc) {
        try {
            const r = await fetchFn(url, { method: 'POST', headers: csrfHeaders(doc), body: cuerpo });
            const json = await leer(r);
            return r.ok ? { ok: true, data: json ? json.data : null } : { ok: false, mensaje: mensajeDe(json) };
        } catch (e) {
            return { ok: false, mensaje: ERROR_GENERICO };
        }
    }

    function aplicar(fetchFn, idSaldo, idFactura, monto, doc) {
        return enviar(fetchFn, API + '/' + encodeURIComponent(idSaldo) + '/aplicaciones', cuerpoAplicacion(idFactura, monto), doc);
    }

    function reversar(fetchFn, idAplicacion, razon, doc) {
        return enviar(fetchFn, API + '/aplicaciones/' + encodeURIComponent(idAplicacion) + '/reversion',
            cuerpoReversion(razon), doc);
    }

    // ---- textos ----

    function origenNota(o) {
        return o.numeroNota || o.numeroNotaOrigen || o.claveNota || o.claveNotaOrigen || ('Nota ' + (o.idNota || o.idNotaOrigen || ''));
    }

    function textoOpcion(saldo) {
        return origenNota(saldo) + ' · ' + NC.formatearDia(saldo.fechaOrigen) + ' · saldo vivo '
            + money(saldo.saldoVivo, saldo.moneda);
    }

    function textoReparto(d) {
        if (!d || d.estado !== 'APLICADA' || d.montoCreditoCxc == null || d.montoSaldoFavor == null) return '';
        return 'A cuentas por cobrar ' + money(d.montoCreditoCxc, d.moneda)
            + ' · a saldo a favor ' + money(d.montoSaldoFavor, d.moneda);
    }

    // ---- opción «Aplicar saldo a favor» ----

    function pintarOpciones(doc, select, saldos) {
        select.textContent = '';
        const vacia = doc.createElement('option');
        vacia.value = '';
        vacia.textContent = 'Seleccione un saldo a favor...';
        select.appendChild(vacia);
        (saldos || []).forEach((s) => {
            const o = doc.createElement('option');
            o.value = String(s.id);
            o.textContent = textoOpcion(s);
            select.appendChild(o);
        });
    }

    function mostrar(elemento, visible) {
        if (elemento) elemento.style.display = visible ? '' : 'none';
    }

    function ponerError(elemento, texto) {
        if (!elemento) return;
        elemento.textContent = texto || '';
        mostrar(elemento, !!texto);
    }

    /**
     * Carga los saldos VIGENTES del mismo cliente y moneda de la factura (/para-factura) y enlaza el panel. Elementos:
     * sf-panel, sf-select, sf-monto, sf-error, sf-ayuda, sf-boton, sf-info. Los errores del servidor se muestran en
     * sf-error sin ocultar el panel. `onAplicado(aplicacion)` se llama con la aplicación creada.
     */
    async function iniciarPanelAplicar(doc, fetchFn, opciones) {
        const $ = (id) => doc.getElementById(id);
        const panel = $('sf-panel');
        const select = $('sf-select');
        const monto = $('sf-monto');
        const error = $('sf-error');
        const ayuda = $('sf-ayuda');
        const boton = $('sf-boton');
        const info = $('sf-info');
        const idFactura = opciones.idFactura;
        let datos = null;
        mostrar(panel, false);
        if (!panel || !select || !monto || !boton) return;
        // Una llamada nueva (otra factura elegida) invalida a la anterior y retira sus manejadores
        const secuencia = (panel.sfSecuencia = (panel.sfSecuencia || 0) + 1);
        if (panel.sfManejadores && typeof select.removeEventListener === 'function') {
            select.removeEventListener('change', panel.sfManejadores.cambio);
            monto.removeEventListener('input', panel.sfManejadores.entrada);
            boton.removeEventListener('click', panel.sfManejadores.clic);
        }
        panel.sfManejadores = null;
        ponerError(error, '');
        try {
            const r = await fetchFn(API + '/para-factura/' + encodeURIComponent(idFactura), { headers: { Accept: 'application/json' } });
            const json = await leer(r);
            datos = r.ok && json ? json.data : null;
        } catch (e) {
            datos = null;
        }
        if (secuencia !== panel.sfSecuencia) return;
        if (!datos || datos.admiteAplicacion !== true || !Array.isArray(datos.saldos) || datos.saldos.length === 0) return;

        pintarOpciones(doc, select, datos.saldos);
        if (info) info.textContent = 'Saldo pendiente de la factura ' + (datos.numeroFactura || '') + ': '
            + money(datos.saldoPendiente, datos.moneda);
        boton.disabled = true;
        mostrar(panel, true);

        const elegido = () => datos.saldos.find((s) => String(s.id) === String(select.value)) || null;
        function validar() {
            const s = elegido();
            if (!s) {
                boton.disabled = true;
                return false;
            }
            const v = validarMonto(monto.value, s.saldoVivo, datos.saldoPendiente);
            ponerError(error, v.mensaje);
            boton.disabled = !v.valido;
            return v.valido;
        }
        const cambio = () => {
            const s = elegido();
            ponerError(error, '');
            if (ayuda) ayuda.textContent = s ? 'Máximo: ' + money(montoMaximo(s.saldoVivo, datos.saldoPendiente), datos.moneda) : '';
            monto.value = s ? montoMaximo(s.saldoVivo, datos.saldoPendiente) : '';
            validar();
        };
        const clic = async () => {
            const s = elegido();
            if (!s) {
                ponerError(error, 'Seleccione un saldo a favor');
                return;
            }
            if (!validar()) return;
            boton.disabled = true;
            const resultado = await aplicar(fetchFn, s.id, idFactura, monto.value, doc);
            boton.disabled = false;
            if (!resultado.ok) {
                ponerError(error, resultado.mensaje);
                return;
            }
            ponerError(error, '');
            if (typeof opciones.onAplicado === 'function') opciones.onAplicado(resultado.data);
        };
        select.addEventListener('change', cambio);
        monto.addEventListener('input', validar);
        boton.addEventListener('click', clic);
        panel.sfManejadores = { cambio, entrada: validar, clic };
    }

    // ---- pagos de la factura ----

    function esPagoSaldoFavor(p) {
        return p && p.tipoPago === 'NOTA_CREDITO';
    }

    /** Celda de acciones de un pago NOTA_CREDITO: «Reversar» pide la razón (3 a 180) y llama a onReversar(id, razon). */
    function accionReversar(doc, pago, opciones) {
        const td = NC.el(doc, 'td', null, 'text-nowrap');
        if (!(opciones && opciones.puedeReversar) || !esPagoSaldoFavor(pago) || pago.estado === 'ANULADO'
            || pago.idAplicacionSaldoFavor == null) return td;
        const abrir = NC.el(doc, 'button', 'Reversar', 'btn btn-sm btn-outline-danger');
        abrir.setAttribute('type', 'button');
        abrir.setAttribute('data-id-aplicacion', pago.idAplicacionSaldoFavor);
        const razon = doc.createElement('input');
        razon.setAttribute('type', 'text');
        razon.setAttribute('maxlength', String(RAZON_MAX));
        razon.setAttribute('placeholder', 'Razón (3 a 180 caracteres)');
        razon.className = 'form-control form-control-sm mt-1';
        razon.style.display = 'none';
        const confirmar = NC.el(doc, 'button', 'Confirmar reversión', 'btn btn-sm btn-danger mt-1');
        confirmar.setAttribute('type', 'button');
        confirmar.style.display = 'none';
        const aviso = NC.el(doc, 'div', null, 'small text-danger');
        aviso.style.display = 'none';
        abrir.addEventListener('click', () => {
            razon.style.display = '';
            confirmar.style.display = '';
        });
        confirmar.addEventListener('click', () => {
            const v = validarRazon(razon.value);
            ponerError(aviso, v.mensaje);
            if (v.valido && typeof opciones.onReversar === 'function') {
                opciones.onReversar(pago.idAplicacionSaldoFavor, razon.value.trim(), { aviso, confirmar });
            }
        });
        [abrir, razon, confirmar, aviso].forEach((h) => td.appendChild(h));
        return td;
    }

    /** Historial de pagos del detalle de factura: pagos NOTA_CREDITO como «Saldo a favor» con su nota de origen. */
    function pintarPagos(doc, cont, detalle, opciones) {
        const moneda = detalle.monedaFE || 'CRC';
        cont.textContent = '';
        const pagado = NC.el(doc, 'p', null, 'mb-2');
        pagado.appendChild(NC.el(doc, 'strong', 'Total pagado: '));
        pagado.appendChild(NC.el(doc, 'span', money(detalle.totalPagado || 0, moneda)));
        cont.appendChild(pagado);
        const saldo = NC.el(doc, 'p', null, 'mb-2');
        saldo.appendChild(NC.el(doc, 'strong', 'Saldo pendiente: '));
        saldo.appendChild(NC.el(doc, 'span', money(detalle.saldoPendiente || 0, moneda),
            Number(detalle.saldoPendiente) > 0 ? 'text-danger' : 'text-success'));
        cont.appendChild(saldo);

        const envoltura = NC.el(doc, 'div', null, 'table-responsive mt-3');
        const tabla = NC.el(doc, 'table', null, 'table table-sm table-bordered');
        const cabecera = doc.createElement('thead');
        cabecera.className = 'table-light';
        const trc = doc.createElement('tr');
        ['Número pago', 'Fecha', 'Método', 'Monto', 'Estado', ''].forEach((t) => trc.appendChild(NC.el(doc, 'th', t)));
        cabecera.appendChild(trc);
        tabla.appendChild(cabecera);
        const cuerpo = doc.createElement('tbody');
        (detalle.pagos || []).forEach((p) => {
            const tr = doc.createElement('tr');
            const numero = doc.createElement('td');
            numero.appendChild(NC.el(doc, 'span', p.numeroPago));
            let metodo = p.metodoPagoDescripcion;
            if (esPagoSaldoFavor(p)) {
                numero.appendChild(NC.el(doc, 'span', 'Saldo a favor', 'badge bg-info text-dark ms-1'));
                numero.appendChild(NC.el(doc, 'div', 'Nota de origen: ' + origenNota(p), 'small text-muted'));
                metodo = 'Saldo a favor';
            }
            tr.appendChild(numero);
            tr.appendChild(NC.celda(doc, NC.formatearFecha(p.fechaPago, false)));
            tr.appendChild(NC.celda(doc, metodo || '-'));
            tr.appendChild(NC.celda(doc, money(p.monto, moneda), 'text-end'));
            const estadoTd = doc.createElement('td');
            estadoTd.appendChild(NC.el(doc, 'span', p.estadoDescripcion || p.estado,
                'badge ' + (p.estado === 'CONCILIADO' ? 'bg-primary' : p.estado === 'CONFIRMADO' ? 'bg-success'
                    : p.estado === 'PENDIENTE' ? 'bg-warning' : 'bg-secondary')));
            tr.appendChild(estadoTd);
            tr.appendChild(accionReversar(doc, p, opciones));
            cuerpo.appendChild(tr);
        });
        tabla.appendChild(cuerpo);
        envoltura.appendChild(tabla);
        cont.appendChild(envoltura);
    }

    // ---- estado de cuenta ----

    function totalesVivos(items) {
        const totales = {};
        (items || []).forEach((i) => {
            if (i.saldo && i.saldo.estado === 'VIGENTE') {
                const m = i.saldo.moneda || 'CRC';
                totales[m] = ((totales[m] == null ? 0 : centimos(totales[m])) + centimos(i.saldo.saldoVivo)) / 100;
            }
        });
        return totales;
    }

    function textoTc(a) {
        if (a.tcDiaValor == null) return '';
        let t = 'TC del día ' + a.tcDiaValor + (a.tcDiaFecha ? ' (' + NC.formatearDia(a.tcDiaFecha) + ')' : '');
        if (a.diferencialNeto != null) t += ' · diferencial neto ' + money(a.diferencialNeto, 'CRC');
        return t;
    }

    function pintarAplicaciones(doc, aplicaciones) {
        const envoltura = NC.el(doc, 'div', null, 'table-responsive');
        const tabla = NC.el(doc, 'table', null, 'table table-sm align-middle mb-0');
        const cabecera = doc.createElement('thead');
        const trc = doc.createElement('tr');
        ['Estado', 'Factura', 'Pago', 'Monto', 'Fecha', 'Tipo de cambio', 'Reversión'].forEach((t) => trc.appendChild(NC.el(doc, 'th', t)));
        cabecera.appendChild(trc);
        tabla.appendChild(cabecera);
        const cuerpo = doc.createElement('tbody');
        aplicaciones.forEach((a) => {
            const tr = doc.createElement('tr');
            const reversada = a.estado === 'REVERSADA';
            const estado = doc.createElement('td');
            estado.appendChild(NC.el(doc, 'span', reversada ? 'Reversada' : 'Aplicada',
                'badge ' + (reversada ? 'bg-dark' : 'bg-success')));
            tr.appendChild(estado);
            tr.appendChild(NC.celda(doc, a.numeroFactura || '-'));
            tr.appendChild(NC.celda(doc, a.numeroPago || '-'));
            tr.appendChild(NC.celda(doc, money(a.monto, a.moneda), 'text-end'));
            tr.appendChild(NC.celda(doc, NC.formatearDia(a.fechaAplicacion)));
            tr.appendChild(NC.celda(doc, textoTc(a) || '-'));
            tr.appendChild(NC.celda(doc, reversada
                ? 'Reversada el ' + NC.formatearDia(a.fechaReversion) + ' · Razón: ' + (a.razonReversion || '-') : '-'));
            cuerpo.appendChild(tr);
        });
        tabla.appendChild(cuerpo);
        envoltura.appendChild(tabla);
        return envoltura;
    }

    /** Sección «Saldos a favor» del estado de cuenta: vivos por moneda y, por saldo, sus aplicaciones. */
    function pintarEstadoCuenta(doc, cont, items) {
        cont.textContent = '';
        if (!items || items.length === 0) {
            cont.textContent = 'El cliente no tiene saldos a favor';
            return;
        }
        const totales = totalesVivos(items);
        const monedas = Object.keys(totales).sort();
        if (monedas.length === 0) {
            cont.appendChild(NC.el(doc, 'p', 'Sin saldo a favor vivo', 'fw-semibold mb-2'));
        }
        monedas.forEach((m) => cont.appendChild(
            NC.el(doc, 'p', 'Saldo a favor vivo ' + m + ': ' + money(totales[m], m), 'fw-semibold mb-1')));
        items.forEach((i) => {
            const s = i.saldo;
            const bloque = NC.el(doc, 'div', null, 'border rounded p-2 mt-3');
            const titulo = NC.el(doc, 'div', null, 'd-flex flex-wrap gap-2 align-items-center mb-2');
            titulo.appendChild(NC.el(doc, 'strong', 'Nota ' + origenNota(s)));
            titulo.appendChild(NC.el(doc, 'span', NC.formatearDia(s.fechaOrigen), 'text-muted'));
            titulo.appendChild(NC.el(doc, 'span', 'Original ' + money(s.montoOriginal, s.moneda) + ' · vivo '
                + money(s.saldoVivo, s.moneda)));
            titulo.appendChild(NC.el(doc, 'span', s.estado === 'AGOTADO' ? 'Agotado' : 'Vigente',
                'badge ' + (s.estado === 'AGOTADO' ? 'bg-secondary' : 'bg-success')));
            bloque.appendChild(titulo);
            if (i.aplicaciones && i.aplicaciones.length) {
                bloque.appendChild(pintarAplicaciones(doc, i.aplicaciones));
            } else {
                bloque.appendChild(NC.el(doc, 'small', 'Sin aplicaciones', 'text-muted'));
            }
            cont.appendChild(bloque);
        });
    }

    global.SaldoFavor = {
        API, validarMonto, montoMaximo, validarRazon, cuerpoReversion, cuerpoAplicacion, aplicar, reversar,
        textoOpcion, textoReparto, pintarOpciones, iniciarPanelAplicar, pintarPagos, accionReversar,
        totalesVivos, pintarEstadoCuenta
    };
})(typeof window !== 'undefined' ? window : globalThis);
