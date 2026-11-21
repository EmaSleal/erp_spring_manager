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
    const ERROR_TOPE_SALDO = 'El monto excede el saldo disponible del saldo a favor';
    const ERROR_TOPE_FACTURA = 'El monto excede el saldo pendiente de la factura destino';
    const ERROR_RAZON = 'La razón es obligatoria y debe tener entre 3 y 180 caracteres';
    const RAZON_MIN = 3;
    const RAZON_MAX = 180;

    const centimos = (n) => Math.round(Number(n) * 100);
    const money = (valor, moneda) => NC.formatearMonto(valor, moneda);

    // ---- validación ----

    /** Monto de la aplicación: > 0, a lo sumo dos decimales, ≤ disponible (saldo vivo − reservado) y ≤ saldo pendiente de la factura. */
    function validarMonto(texto, disponible, saldoPendiente) {
        const t = typeof texto === 'string' ? texto.trim() : String(texto == null ? '' : texto);
        if (!/^\d+(\.\d{1,2})?$/.test(t) || centimos(t) <= 0) return { valido: false, mensaje: ERROR_MONTO };
        if (centimos(t) > centimos(disponible)) return { valido: false, mensaje: ERROR_TOPE_SALDO };
        if (centimos(t) > centimos(saldoPendiente)) return { valido: false, mensaje: ERROR_TOPE_FACTURA };
        return { valido: true, mensaje: '' };
    }

    function montoMaximo(disponible, saldoPendiente) {
        return (Math.min(centimos(disponible), centimos(saldoPendiente)) / 100).toFixed(2);
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
        return origenNota(saldo) + ' · ' + NC.formatearDia(saldo.fechaOrigen) + ' · disponible '
            + money(disponibleDe(saldo), saldo.moneda);
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
            const v = validarMonto(monto.value, disponibleDe(s), datos.saldoPendiente);
            ponerError(error, v.mensaje);
            boton.disabled = !v.valido;
            return v.valido;
        }
        const cambio = () => {
            const s = elegido();
            ponerError(error, '');
            if (ayuda) ayuda.textContent = s ? 'Máximo: ' + money(montoMaximo(disponibleDe(s), datos.saldoPendiente), datos.moneda) : '';
            monto.value = s ? montoMaximo(disponibleDe(s), datos.saldoPendiente) : '';
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

    // ---- devolución en dinero (T-049, B2, ADR-053/058) ----

    const ERROR_IBAN = 'El IBAN debe ser costarricense: CR seguido de 20 dígitos';
    const ERROR_IBAN_CONTROL = 'El dígito de control del IBAN no es válido';
    const ERROR_TOPE_DISPONIBLE = 'El monto excede el saldo disponible del saldo a favor';
    const MEDIOS = [['CORREO', 'Correo electrónico'], ['CARTA', 'Carta'], ['WHATSAPP', 'WhatsApp'], ['OTRO', 'Otro']];
    const ESTADOS_DEV = { SOLICITADA: ['Solicitada', 'bg-warning text-dark'], APROBADA: ['Aprobada', 'bg-primary'],
        PAGADA: ['Pagada', 'bg-success'], RECHAZADA: ['Rechazada', 'bg-danger'], ANULADA: ['Anulada', 'bg-dark'] };

    function disponibleDe(s) {
        if (s && s.disponible != null) return Number(s.disponible);
        return (centimos(s ? s.saldoVivo : 0) - centimos(s && s.reservado != null ? s.reservado : 0)) / 100;
    }

    /** IBAN costarricense: sin espacios y en mayúsculas, CR + 20 dígitos y dígito de control ISO 13616 (mod 97). */
    function validarIban(texto) {
        const n = String(texto == null ? '' : texto).replace(/\s+/g, '').toUpperCase();
        if (!/^CR\d{20}$/.test(n)) return { valido: false, mensaje: ERROR_IBAN, normalizado: n };
        const reordenado = n.slice(4) + n.slice(0, 4);
        let resto = 0;
        for (const c of reordenado) {
            const digitos = c >= 'A' && c <= 'Z' ? String(c.charCodeAt(0) - 55) : c;
            for (const d of digitos) resto = (resto * 10 + Number(d)) % 97;
        }
        return resto === 1 ? { valido: true, mensaje: '', normalizado: n }
            : { valido: false, mensaje: ERROR_IBAN_CONTROL, normalizado: n };
    }

    function validarMontoDevolucion(texto, disponible) {
        const t = typeof texto === 'string' ? texto.trim() : String(texto == null ? '' : texto);
        if (!/^\d+(\.\d{1,2})?$/.test(t) || centimos(t) <= 0) return { valido: false, mensaje: ERROR_MONTO };
        if (centimos(t) > centimos(disponible)) return { valido: false, mensaje: ERROR_TOPE_DISPONIBLE };
        return { valido: true, mensaje: '' };
    }

    function hoyLocal() {
        const d = new Date();
        const p = (n) => String(n).padStart(2, '0');
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
    }

    /** Fecha yyyy-MM-dd obligatoria y no futura; devuelve el mensaje de error o ''. */
    function errorFecha(texto, hoy, nombre) {
        const t = String(texto == null ? '' : texto).trim();
        const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(t);
        if (!m || Number(m[2]) < 1 || Number(m[2]) > 12 || Number(m[3]) < 1 || Number(m[3]) > 31) return 'La ' + nombre + ' es obligatoria';
        return t > hoy ? 'La ' + nombre + ' no puede ser futura' : '';
    }

    function validarSolicitud(v, disponible, hoy) {
        const e = {};
        const monto = validarMontoDevolucion(v.monto, disponible);
        if (!monto.valido) e.monto = monto.mensaje;
        const iban = validarIban(v.iban);
        if (!iban.valido) e.iban = iban.mensaje;
        const nombre = String(v.nombreTitular == null ? '' : v.nombreTitular).trim();
        if (nombre.length < 3 || nombre.length > 100) e.nombreTitular = 'El nombre del titular debe tener entre 3 y 100 caracteres';
        if (!String(v.identificacionTitular == null ? '' : v.identificacionTitular).trim()) e.identificacionTitular = 'La identificación del titular es obligatoria';
        if (!MEDIOS.some((m) => m[0] === v.medioSolicitud)) e.medioSolicitud = 'Seleccione el medio por el que se solicitó';
        const f = errorFecha(v.fechaSolicitud, hoy, 'fecha de la solicitud');
        if (f) e.fechaSolicitud = f;
        return e;
    }

    function validarPago(v, hoy) {
        const e = {};
        if (!String(v.idCuentaBancaria == null ? '' : v.idCuentaBancaria).trim()) e.idCuentaBancaria = 'Seleccione la cuenta bancaria de la empresa';
        if (!String(v.numeroComprobante == null ? '' : v.numeroComprobante).trim()) e.numeroComprobante = 'El número de comprobante es obligatorio';
        const f = errorFecha(v.fechaComprobante, hoy, 'fecha del comprobante');
        if (f) e.fechaComprobante = f;
        return e;
    }

    function puedeSolicitar(p) {
        return !!(p && (p.pagoCrear || p.devolucionAprobar));
    }

    /** Acciones disponibles de una devolución según su estado y los permisos del usuario. */
    function accionesDevolucion(estado, p) {
        const a = [];
        const aprueba = !!(p && p.devolucionAprobar);
        const anula = !!(p && (p.pagoAnular || p.devolucionAprobar));
        if (estado === 'SOLICITADA') {
            if (aprueba) a.push('aprobar', 'rechazar');
            if (anula) a.push('anular');
        } else if (estado === 'APROBADA') {
            if (aprueba) a.push('pagar');
            if (anula) a.push('anular');
        }
        return a;
    }

    function urlDevolucion(id, accion) {
        return API + '/devoluciones/' + encodeURIComponent(id) + '/' + accion;
    }

    /** Historial legible de una devolución: una línea por hito (solicitud, aprobación, rechazo, anulación, pago). */
    function historialDevolucion(d) {
        const l = [];
        const dia = (f) => NC.formatearDia(f);
        l.push('Solicitada por ' + (d.solicitadoPorNombre || '-') + ' el ' + dia(d.fechaCreacion) + ' · medio ' + (d.medioSolicitud || '-')
            + ' · fecha de la solicitud ' + dia(d.fechaSolicitud));
        if (d.aprobadoPorNombre || d.fechaAprobacion) {
            l.push('Aprobada por ' + (d.aprobadoPorNombre || '-') + ' el ' + dia(d.fechaAprobacion)
                + (d.verificoCuenta ? ' · verificó que la cuenta pertenece al cliente (' + dia(d.fechaVerificacion) + ')' : ''));
        }
        if (d.estado === 'RECHAZADA') {
            l.push('Rechazada por ' + (d.rechazadoPorNombre || '-') + ' el ' + dia(d.fechaRechazo) + ' · Razón: ' + (d.razon || '-'));
        }
        if (d.estado === 'ANULADA') {
            l.push('Anulada por ' + (d.anuladoPorNombre || '-') + ' el ' + dia(d.fechaAnulacion) + ' · Razón: ' + (d.razon || '-'));
        }
        if (d.estado === 'PAGADA') {
            let t = 'Pagada por ' + (d.pagadoPorNombre || '-') + ' el ' + dia(d.fechaPago) + ' · cuenta ' + (d.cuentaBancaria || '-')
                + ' · comprobante ' + (d.numeroComprobante || '-') + ' del ' + dia(d.fechaComprobante);
            if (d.numeroAsiento) t += ' · asiento ' + d.numeroAsiento;
            l.push(t);
            const tc = textoTc(d);
            if (tc) l.push(tc);
        }
        return l;
    }

    // ---- formularios en línea ----

    /**
     * Formulario en línea. `campos`: [{ nombre, etiqueta, tipo: text|date|select|checkbox, opciones, valor, maxlength }].
     * `validar(valores)` devuelve { campo: mensaje }; los mensajes se pintan bajo el campo (data-error) cuando el campo se
     * tocó o se intentó enviar, y el botón se deshabilita mientras haya errores. `enviar(valores)` devuelve
     * { ok, mensaje }: si falla, el mensaje queda en el formulario (data-error-form) y este no se cierra.
     */
    function crearFormulario(doc, cfg) {
        const form = NC.el(doc, 'div', null, 'border rounded p-2 mt-2 bg-light');
        form.appendChild(NC.el(doc, 'div', cfg.titulo, 'fw-semibold mb-2'));
        const ctl = {};
        const errores = {};
        const tocados = {};
        cfg.campos.forEach((c) => {
            const grupo = NC.el(doc, 'div', null, c.tipo === 'checkbox' ? 'form-check mb-2' : 'mb-2');
            let control;
            if (c.tipo === 'select') {
                control = doc.createElement('select');
                control.className = 'form-select form-select-sm';
                (c.opciones || []).forEach((o) => {
                    const op = doc.createElement('option');
                    op.value = o[0];
                    op.textContent = o[1];
                    control.appendChild(op);
                });
            } else {
                control = doc.createElement('input');
                control.setAttribute('type', c.tipo === 'checkbox' ? 'checkbox' : (c.tipo || 'text'));
                control.className = c.tipo === 'checkbox' ? 'form-check-input' : 'form-control form-control-sm';
                if (c.maxlength) control.setAttribute('maxlength', String(c.maxlength));
            }
            control.setAttribute('data-campo', c.nombre);
            if (c.valor != null && c.tipo !== 'checkbox') control.value = String(c.valor);
            const etiqueta = NC.el(doc, 'label', c.etiqueta, c.tipo === 'checkbox' ? 'form-check-label' : 'form-label small mb-1');
            const msg = NC.el(doc, 'div', '', 'small text-danger');
            msg.setAttribute('data-error', c.nombre);
            if (c.tipo === 'checkbox') { grupo.appendChild(control); grupo.appendChild(etiqueta); } else { grupo.appendChild(etiqueta); grupo.appendChild(control); }
            grupo.appendChild(msg);
            form.appendChild(grupo);
            ctl[c.nombre] = control;
            errores[c.nombre] = msg;
        });
        const errorForm = NC.el(doc, 'div', '', 'small text-danger mb-2');
        errorForm.setAttribute('data-error-form', '1');
        errorForm.style.display = 'none';
        form.appendChild(errorForm);
        const enviarBtn = NC.el(doc, 'button', cfg.textoBoton || 'Enviar', 'btn btn-sm btn-primary me-2');
        enviarBtn.setAttribute('type', 'button');
        enviarBtn.setAttribute('data-accion', 'enviar');
        const cancelar = NC.el(doc, 'button', 'Cancelar', 'btn btn-sm btn-outline-secondary');
        cancelar.setAttribute('type', 'button');
        cancelar.setAttribute('data-accion', 'cancelar');
        form.appendChild(enviarBtn);
        form.appendChild(cancelar);

        const valores = () => {
            const v = {};
            cfg.campos.forEach((c) => { v[c.nombre] = c.tipo === 'checkbox' ? !!ctl[c.nombre].checked : ctl[c.nombre].value; });
            return v;
        };
        let enCurso = false;
        const refrescar = (todos) => {
            const e = cfg.validar(valores());
            cfg.campos.forEach((c) => {
                errores[c.nombre].textContent = (todos || tocados[c.nombre]) && e[c.nombre] ? e[c.nombre] : '';
            });
            enviarBtn.disabled = enCurso || Object.keys(e).length > 0 || !!form.bloqueado;
            return e;
        };
        cfg.campos.forEach((c) => {
            const alCambiar = () => { tocados[c.nombre] = true; ponerError(errorForm, ''); refrescar(false); };
            ctl[c.nombre].addEventListener('input', alCambiar);
            ctl[c.nombre].addEventListener('change', alCambiar);
        });
        enviarBtn.addEventListener('click', async () => {
            if (Object.keys(refrescar(true)).length > 0) return;
            enCurso = true;
            enviarBtn.disabled = true;
            ponerError(errorForm, '');
            let r;
            try {
                r = await cfg.enviar(valores());
            } catch (e) {
                r = { ok: false, mensaje: ERROR_GENERICO };
            }
            enCurso = false;
            if (!r.ok) {
                ponerError(errorForm, r.mensaje || ERROR_GENERICO);
                refrescar(false);
                return;
            }
            if (typeof cfg.alExito === 'function') cfg.alExito(r.data);
        });
        cancelar.addEventListener('click', () => { if (typeof cfg.alCancelar === 'function') cfg.alCancelar(); });
        refrescar(false);
        return { form, ctl, enviarBtn, errorForm, refrescar };
    }

    const campoRazon = () => ({ nombre: 'razon', etiqueta: 'Razón (3 a 180 caracteres)', tipo: 'text', maxlength: RAZON_MAX });
    const validarCampoRazon = (v) => {
        const r = validarRazon(v.razon);
        return r.valido ? {} : { razon: r.mensaje };
    };

    /** Abre en `panel` el formulario de la acción. `ctx`: { doc, fetchFn, hoy, onCambio, saldo, devolucion }. */
    function abrirFormulario(panel, accion, ctx) {
        const cerrar = () => { panel.textContent = ''; };
        const alExito = () => { cerrar(); if (typeof ctx.onCambio === 'function') ctx.onCambio(); };
        const peticion = (url, cuerpo) => enviar(ctx.fetchFn, url, JSON.stringify(cuerpo), ctx.doc);
        const d = ctx.devolucion;
        const s = ctx.saldo;
        let f;
        if (accion === 'solicitar') {
            const disp = disponibleDe(s);
            f = crearFormulario(ctx.doc, {
                titulo: 'Solicitar devolución en dinero (' + s.moneda + ', disponible ' + money(disp, s.moneda) + ')',
                textoBoton: 'Solicitar devolución', alExito, alCancelar: cerrar,
                campos: [
                    { nombre: 'monto', etiqueta: 'Monto a devolver', tipo: 'text', valor: disp.toFixed(2) },
                    { nombre: 'iban', etiqueta: 'IBAN de la cuenta del cliente', tipo: 'text', maxlength: 40 },
                    { nombre: 'nombreTitular', etiqueta: 'Nombre del titular', tipo: 'text', maxlength: 100 },
                    { nombre: 'identificacionTitular', etiqueta: 'Identificación del titular', tipo: 'text', maxlength: 50 },
                    { nombre: 'medioSolicitud', etiqueta: 'Medio por el que el cliente lo solicitó', tipo: 'select',
                        opciones: [['', 'Seleccione...']].concat(MEDIOS) },
                    { nombre: 'fechaSolicitud', etiqueta: 'Fecha de la solicitud', tipo: 'date' }],
                validar: (v) => validarSolicitud(v, disp, ctx.hoy),
                enviar: (v) => peticion(API + '/' + encodeURIComponent(s.id) + '/devoluciones', {
                    monto: Number(String(v.monto).trim()), iban: validarIban(v.iban).normalizado, nombreTitular: String(v.nombreTitular).trim(),
                    identificacionTitular: String(v.identificacionTitular).trim(), medioSolicitud: v.medioSolicitud,
                    fechaSolicitud: String(v.fechaSolicitud).trim() })
            });
        } else if (accion === 'aprobar') {
            f = crearFormulario(ctx.doc, {
                titulo: 'Aprobar la devolución de ' + money(d.monto, d.moneda), textoBoton: 'Aprobar', alExito, alCancelar: cerrar,
                campos: [{ nombre: 'verificoCuenta', etiqueta: 'Verifiqué que la cuenta pertenece al cliente', tipo: 'checkbox' }],
                validar: (v) => (v.verificoCuenta ? {} : { verificoCuenta: 'Debe confirmar que verificó la cuenta' }),
                enviar: () => peticion(urlDevolucion(d.id, 'aprobar'), { verificoCuenta: true })
            });
        } else if (accion === 'rechazar' || accion === 'anular') {
            f = crearFormulario(ctx.doc, {
                titulo: (accion === 'rechazar' ? 'Rechazar' : 'Anular') + ' la devolución de ' + money(d.monto, d.moneda),
                textoBoton: accion === 'rechazar' ? 'Rechazar' : 'Anular', alExito, alCancelar: cerrar,
                campos: [campoRazon()], validar: validarCampoRazon,
                enviar: (v) => peticion(urlDevolucion(d.id, accion), { razon: String(v.razon).trim() })
            });
        } else if (accion === 'pagar') {
            f = crearFormulario(ctx.doc, {
                titulo: 'Registrar el pago de ' + money(d.monto, d.moneda), textoBoton: 'Registrar pago', alExito, alCancelar: cerrar,
                campos: [
                    { nombre: 'idCuentaBancaria', etiqueta: 'Cuenta bancaria de la empresa (' + d.moneda + ')', tipo: 'select',
                        opciones: [['', 'Cargando cuentas...']] },
                    { nombre: 'numeroComprobante', etiqueta: 'Número de comprobante', tipo: 'text', maxlength: 50 },
                    { nombre: 'fechaComprobante', etiqueta: 'Fecha del comprobante', tipo: 'date' }],
                validar: (v) => validarPago(v, ctx.hoy),
                enviar: (v) => peticion(urlDevolucion(d.id, 'pagar'), { idCuentaBancaria: Number(v.idCuentaBancaria),
                    numeroComprobante: String(v.numeroComprobante).trim(), fechaComprobante: String(v.fechaComprobante).trim() })
            });
            f.form.bloqueado = true;
            f.refrescar(false);
            cargarCuentas(ctx, d.moneda, f);
        }
        panel.textContent = '';
        if (f) panel.appendChild(f.form);
    }

    function textoCuenta(c) {
        return (c.entidad || 'Cuenta') + ' · ' + (c.cuentaBanco || '-') + (c.cuentaIban ? ' · ' + c.cuentaIban : '');
    }

    async function cargarCuentas(ctx, moneda, f) {
        const select = f.ctl.idCuentaBancaria;
        let cuentas = null;
        try {
            const r = await ctx.fetchFn('/api/cuentas-bancarias/activas?moneda=' + encodeURIComponent(moneda),
                { headers: { Accept: 'application/json' } });
            const json = await leer(r);
            cuentas = r.ok && json && Array.isArray(json.data) ? json.data : null;
        } catch (e) {
            cuentas = null;
        }
        select.textContent = '';
        const vacia = ctx.doc.createElement('option');
        vacia.value = '';
        vacia.textContent = 'Seleccione...';
        select.appendChild(vacia);
        (cuentas || []).filter((c) => !c.moneda || c.moneda === moneda).forEach((c) => {
            const o = ctx.doc.createElement('option');
            o.value = String(c.idCuentaBancaria);
            o.textContent = textoCuenta(c);
            select.appendChild(o);
        });
        if (!cuentas || select.hijos.length < 2) {
            ponerError(f.errorForm, cuentas ? 'No hay una cuenta bancaria activa de la empresa en ' + moneda
                : 'No se pudieron cargar las cuentas bancarias de la empresa');
            return;
        }
        f.form.bloqueado = false;
        f.refrescar(false);
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

    function pintarDevoluciones(doc, saldo, devoluciones, panel, ctx) {
        const envoltura = NC.el(doc, 'div', null, 'table-responsive mt-2');
        const tabla = NC.el(doc, 'table', null, 'table table-sm align-middle mb-0');
        const cabecera = doc.createElement('thead');
        const trc = doc.createElement('tr');
        ['Estado', 'Monto', 'Titular / IBAN', 'Historial', ''].forEach((t) => trc.appendChild(NC.el(doc, 'th', t)));
        cabecera.appendChild(trc);
        tabla.appendChild(cabecera);
        const cuerpo = doc.createElement('tbody');
        devoluciones.forEach((d) => {
            const tr = doc.createElement('tr');
            const est = ESTADOS_DEV[d.estado] || [d.estado, 'bg-secondary'];
            const tdEstado = doc.createElement('td');
            tdEstado.appendChild(NC.el(doc, 'span', est[0], 'badge ' + est[1]));
            tr.appendChild(tdEstado);
            tr.appendChild(NC.celda(doc, money(d.monto, d.moneda || saldo.moneda), 'text-end'));
            const tdTitular = doc.createElement('td');
            tdTitular.appendChild(NC.el(doc, 'div', (d.nombreTitular || '-') + ' · ' + (d.identificacionTitular || '-')));
            tdTitular.appendChild(NC.el(doc, 'div', d.iban || '-', 'small text-muted'));
            tr.appendChild(tdTitular);
            const tdHist = doc.createElement('td');
            historialDevolucion(d).forEach((linea) => tdHist.appendChild(NC.el(doc, 'div', linea, 'small')));
            tr.appendChild(tdHist);
            const tdAcc = NC.el(doc, 'td', null, 'text-nowrap');
            const rotulos = { aprobar: 'Aprobar', rechazar: 'Rechazar', anular: 'Anular', pagar: 'Pagar' };
            const estilos = { aprobar: 'btn-outline-success', rechazar: 'btn-outline-danger', anular: 'btn-outline-secondary', pagar: 'btn-outline-primary' };
            accionesDevolucion(d.estado, ctx.permisos).forEach((a) => {
                const b = NC.el(doc, 'button', rotulos[a], 'btn btn-sm me-1 ' + estilos[a]);
                b.setAttribute('type', 'button');
                b.setAttribute('data-accion', a);
                b.setAttribute('data-id-devolucion', d.id);
                b.addEventListener('click', () => abrirFormulario(panel, a, { ...ctx, saldo, devolucion: d }));
                tdAcc.appendChild(b);
            });
            tr.appendChild(tdAcc);
            cuerpo.appendChild(tr);
        });
        tabla.appendChild(cuerpo);
        envoltura.appendChild(tabla);
        return envoltura;
    }

    /**
     * Sección «Saldos a favor» del estado de cuenta: vivos por moneda y, por saldo, disponible, reservado, aplicaciones y
     * devoluciones. `opciones`: { permisos: {pagoCrear, pagoAnular, devolucionAprobar}, fetchFn, hoy, onCambio }; sin permisos
     * no se ofrece ninguna acción.
     */
    function pintarEstadoCuenta(doc, cont, items, opciones) {
        const opts = opciones || {};
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
            titulo.appendChild(NC.el(doc, 'span', 'Reservado ' + money(s.reservado == null ? 0 : s.reservado, s.moneda)
                + ' · Disponible ' + money(disponibleDe(s), s.moneda), 'text-muted'));
            titulo.appendChild(NC.el(doc, 'span', s.estado === 'AGOTADO' ? 'Agotado' : 'Vigente',
                'badge ' + (s.estado === 'AGOTADO' ? 'bg-secondary' : 'bg-success')));
            bloque.appendChild(titulo);
            if (i.aplicaciones && i.aplicaciones.length) {
                bloque.appendChild(pintarAplicaciones(doc, i.aplicaciones));
            } else {
                bloque.appendChild(NC.el(doc, 'small', 'Sin aplicaciones', 'text-muted'));
            }
            const panel = NC.el(doc, 'div');
            const ctx = { doc, fetchFn: opts.fetchFn, hoy: opts.hoy || hoyLocal(), onCambio: opts.onCambio, permisos: opts.permisos || {} };
            bloque.appendChild(NC.el(doc, 'div', 'Devoluciones', 'fw-semibold mt-3'));
            if (i.devoluciones && i.devoluciones.length) {
                bloque.appendChild(pintarDevoluciones(doc, s, i.devoluciones, panel, ctx));
            } else {
                bloque.appendChild(NC.el(doc, 'small', 'Sin devoluciones', 'text-muted'));
            }
            if (s.estado === 'VIGENTE' && disponibleDe(s) > 0 && puedeSolicitar(ctx.permisos)) {
                const b = NC.el(doc, 'button', 'Solicitar devolución', 'btn btn-sm btn-outline-primary mt-2');
                b.setAttribute('type', 'button');
                b.setAttribute('data-accion', 'solicitar');
                b.addEventListener('click', () => abrirFormulario(panel, 'solicitar', { ...ctx, saldo: s }));
                bloque.appendChild(b);
            }
            bloque.appendChild(panel);
            cont.appendChild(bloque);
        });
    }

    global.SaldoFavor = {
        API, validarMonto, montoMaximo, validarRazon, cuerpoReversion, cuerpoAplicacion, aplicar, reversar,
        textoOpcion, textoReparto, pintarOpciones, iniciarPanelAplicar, pintarPagos, accionReversar,
        totalesVivos, pintarEstadoCuenta, disponibleDe, validarIban, validarMontoDevolucion, validarSolicitud, validarPago,
        puedeSolicitar, accionesDevolucion, historialDevolucion, crearFormulario
    };
})(typeof window !== 'undefined' ? window : globalThis);
