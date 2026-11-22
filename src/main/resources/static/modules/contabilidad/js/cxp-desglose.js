/*
 * T-052 (ADR-066 §1-2): comprobante del proveedor y desglose de IVA de la cuenta por pagar.
 * Funciones puras (cálculo, validaciones espejo de las del servidor) y ayudas de pintado que solo usan
 * createElement y textContent: ningún dato del servidor se interpreta como HTML.
 * Expone window.CxpDesglose.
 */
(function (global) {
    'use strict';

    var TARIFAS = [
        { codigo: '01', porcentaje: 0, etiqueta: '01 - Tarifa 0% (art. 32, num 1)' },
        { codigo: '02', porcentaje: 1, etiqueta: '02 - Reducida 1%' },
        { codigo: '03', porcentaje: 2, etiqueta: '03 - Reducida 2%' },
        { codigo: '04', porcentaje: 4, etiqueta: '04 - Reducida 4%' },
        { codigo: '05', porcentaje: 0, etiqueta: '05 - Transitorio 0%' },
        { codigo: '06', porcentaje: 4, etiqueta: '06 - Transitorio 4%' },
        { codigo: '07', porcentaje: 8, etiqueta: '07 - Transitorio 8%' },
        { codigo: '08', porcentaje: 13, etiqueta: '08 - General 13%' },
        { codigo: '09', porcentaje: 0.5, etiqueta: '09 - Reducida 0,5%' },
        { codigo: '10', porcentaje: 0, etiqueta: '10 - Exenta' },
        { codigo: '11', porcentaje: 0, etiqueta: '11 - Tarifa 0% sin derecho a crédito' }
    ];
    var CODIGO_GENERAL = '08';
    var TOLERANCIA_CENTAVOS = 1;
    var LARGO_NUMERO = 50;

    function esVacio(v) {
        return v === null || v === undefined || String(v).trim() === '';
    }

    /** Importe en centavos enteros (evita errores de coma flotante al sumar); NaN y vacío cuentan como 0. */
    function aCentavos(v) {
        if (esVacio(v)) return 0;
        var n = parseFloat(String(v).trim());
        return isNaN(n) ? 0 : Math.round(n * 100);
    }

    function deCentavos(c) {
        return c / 100;
    }

    function porcentajeDe(codigo) {
        for (var i = 0; i < TARIFAS.length; i++) {
            if (TARIFAS[i].codigo === codigo) return TARIFAS[i].porcentaje;
        }
        return null;
    }

    function ivaEnCentavos(baseCentavos, porcentaje) {
        // porcentaje con hasta dos decimales (0,5; 13): entero × 100 y mitad hacia arriba, como HALF_UP del servidor
        var num = baseCentavos * Math.round(porcentaje * 100);
        return Math.floor((num + 5000) / 10000);
    }

    /** IVA de una fila = base × tarifa / 100, a 2 decimales (mitad hacia arriba). */
    function calcularFila(base, porcentaje) {
        return deCentavos(ivaEnCentavos(aCentavos(base), Number(porcentaje) || 0));
    }

    function totales(filas) {
        var sb = 0;
        var si = 0;
        (filas || []).forEach(function (f) {
            sb += aCentavos(f.base);
            si += aCentavos(f.iva);
        });
        return { subtotal: deCentavos(sb), impuesto: deCentavos(si), total: deCentavos(sb + si) };
    }

    function filaPorDefecto() {
        return { codigoTarifa: CODIGO_GENERAL, base: '', iva: '' };
    }

    function fmt(n) {
        return Number(n || 0).toFixed(2);
    }

    /**
     * Validaciones del formulario, con las mismas reglas que el servidor. datos: { numero, clave, fecha, hoy (AAAA-MM-DD),
     * filas: [{ codigoTarifa, base, iva }], totalComprobante }. Devuelve { errores, advertencias } (textos fijos).
     */
    function validar(datos) {
        var errores = [];
        var advertencias = [];
        var numero = esVacio(datos.numero) ? '' : String(datos.numero).trim();
        if (!numero) errores.push('El número del comprobante del proveedor es obligatorio');
        else if (numero.length > LARGO_NUMERO) errores.push('El número del comprobante del proveedor no puede exceder 50 caracteres');

        if (esVacio(datos.fecha)) errores.push('La fecha de emisión del comprobante es obligatoria');
        else if (datos.hoy && String(datos.fecha) > String(datos.hoy)) errores.push('La fecha de emisión del comprobante no puede ser futura');

        if (!esVacio(datos.clave) && !/^\d{50}$/.test(String(datos.clave).trim())) {
            errores.push('La clave del comprobante debe tener 50 dígitos');
        }

        var filas = datos.filas || [];
        if (!filas.length) errores.push('El desglose de IVA debe tener al menos una fila');
        filas.forEach(function (f, i) {
            var n = i + 1;
            if (esVacio(f.base) || esVacio(f.iva) || isNaN(parseFloat(f.base)) || isNaN(parseFloat(f.iva))
                || parseFloat(f.base) < 0 || parseFloat(f.iva) < 0) {
                errores.push('La fila ' + n + ' del desglose requiere base e IVA no negativos');
                return;
            }
            var pct = porcentajeDe(f.codigoTarifa);
            if (pct === null) {
                errores.push('La tarifa de IVA de la fila ' + n + ' del desglose no es válida');
                return;
            }
            var esperado = ivaEnCentavos(aCentavos(f.base), pct);
            if (Math.abs(aCentavos(f.iva) - esperado) > TOLERANCIA_CENTAVOS) {
                advertencias.push('La fila ' + n + ' del desglose: el IVA ' + fmt(parseFloat(f.iva))
                    + ' difiere de la base por la tarifa (' + fmt(deCentavos(esperado)) + ')');
            }
        });

        if (!esVacio(datos.totalComprobante) && !isNaN(parseFloat(datos.totalComprobante))) {
            var t = totales(filas).total;
            if (Math.abs(aCentavos(datos.totalComprobante) - aCentavos(t)) > TOLERANCIA_CENTAVOS) {
                advertencias.push('El total del comprobante (' + fmt(parseFloat(datos.totalComprobante))
                    + ') no cuadra con el desglose (' + fmt(t) + ')');
            }
        }
        return { errores: errores, advertencias: advertencias };
    }

    /** Cuerpo del POST /api/cuentas-pagar; subtotal, impuesto y monto salen del desglose. */
    function armarPeticion(datos) {
        var filas = datos.filas || [];
        var t = totales(filas);
        var cuerpo = {};
        var extra = datos.extra || {};
        Object.keys(extra).forEach(function (k) { cuerpo[k] = extra[k]; });
        cuerpo.numeroComprobanteProveedor = String(datos.numero || '').trim();
        cuerpo.claveComprobante = esVacio(datos.clave) ? null : String(datos.clave).trim();
        cuerpo.fechaComprobante = datos.fecha;
        cuerpo.fechaEmision = datos.fecha;
        cuerpo.subtotal = t.subtotal;
        cuerpo.impuesto = t.impuesto;
        cuerpo.monto = t.total;
        cuerpo.desglose = filas.map(function (f) {
            return { codigoTarifa: f.codigoTarifa, base: deCentavos(aCentavos(f.base)), iva: deCentavos(aCentavos(f.iva)) };
        });
        return cuerpo;
    }

    function puedeAnular(cuenta, tienePermiso) {
        return !!tienePermiso && !!cuenta && cuenta.estado !== 'ANULADA' && cuenta.estado !== 'PAGADA';
    }

    function validarMotivo(motivo) {
        var m = esVacio(motivo) ? '' : String(motivo).trim();
        return (m.length < 3 || m.length > 180) ? 'El motivo de anulación debe tener entre 3 y 180 caracteres' : null;
    }

    // ── Pintado (solo createElement + textContent) ────────────────────────────

    function el(doc, tag, clase, texto) {
        var e = doc.createElement(tag);
        if (clase) e.className = clase;
        if (texto !== undefined && texto !== null) e.textContent = String(texto);
        return e;
    }

    function limpiar(nodo) {
        if (typeof nodo.replaceChildren === 'function') nodo.replaceChildren();
        else nodo.textContent = '';
    }

    function renderMensajes(doc, contenedor, resultado) {
        limpiar(contenedor);
        (resultado.errores || []).forEach(function (m) {
            contenedor.appendChild(el(doc, 'div', 'alert alert-danger py-1 px-2 mb-1 small', m));
        });
        (resultado.advertencias || []).forEach(function (m) {
            contenedor.appendChild(el(doc, 'div', 'alert alert-warning py-1 px-2 mb-1 small', m));
        });
    }

    /** Fila editable del desglose: tarifa, base, IVA y botón de quitar. handlers: { cambio(i, campo, valor), quitar(i) }. */
    function renderFila(doc, fila, indice, handlers) {
        var tr = doc.createElement('tr');
        tr.setAttribute('data-fila', String(indice));

        var tdTarifa = doc.createElement('td');
        var sel = el(doc, 'select', 'form-select form-select-sm');
        sel.setAttribute('data-campo', 'codigoTarifa');
        TARIFAS.forEach(function (t) {
            var o = el(doc, 'option', '', t.etiqueta);
            o.value = t.codigo;
            sel.appendChild(o);
        });
        sel.value = fila.codigoTarifa;
        sel.addEventListener('change', function () { handlers.cambio(indice, 'codigoTarifa', sel.value); });
        tdTarifa.appendChild(sel);
        tr.appendChild(tdTarifa);

        ['base', 'iva'].forEach(function (campo) {
            var td = doc.createElement('td');
            var inp = el(doc, 'input', 'form-control form-control-sm text-end');
            inp.setAttribute('type', 'number');
            inp.setAttribute('step', '0.01');
            inp.setAttribute('min', '0');
            inp.setAttribute('data-campo', campo);
            inp.value = fila[campo] === undefined || fila[campo] === null ? '' : String(fila[campo]);
            inp.addEventListener('input', function () { handlers.cambio(indice, campo, inp.value); });
            td.appendChild(inp);
            tr.appendChild(td);
        });

        var tdAccion = doc.createElement('td');
        var btn = el(doc, 'button', 'btn btn-sm btn-outline-danger');
        btn.setAttribute('type', 'button');
        btn.setAttribute('data-accion', 'quitar');
        btn.setAttribute('title', 'Quitar fila');
        btn.setAttribute('aria-label', 'Quitar fila');
        btn.appendChild(el(doc, 'i', 'fas fa-times'));
        btn.addEventListener('click', function () { handlers.quitar(indice); });
        tdAccion.appendChild(btn);
        tr.appendChild(tdAccion);
        return tr;
    }

    function renderDesglose(doc, tbody, filas, handlers) {
        limpiar(tbody);
        filas.forEach(function (f, i) { tbody.appendChild(renderFila(doc, f, i, handlers)); });
    }

    function fila2(doc, etiqueta, valor) {
        var row = el(doc, 'div', 'row mb-1');
        row.appendChild(el(doc, 'div', 'col-sm-4 text-muted', etiqueta));
        row.appendChild(el(doc, 'div', 'col-sm-8 fw-semibold', valor === null || valor === undefined || valor === '' ? '—' : valor));
        return row;
    }

    /** Detalle de una CxP (DTO de la API) con el comprobante del proveedor y su desglose. */
    function renderDetalle(doc, contenedor, c) {
        limpiar(contenedor);
        var moneda = c.moneda || 'CRC';
        contenedor.appendChild(fila2(doc, 'Número', c.numero));
        contenedor.appendChild(fila2(doc, 'Proveedor', c.proveedorNombre));
        contenedor.appendChild(fila2(doc, 'Comprobante del proveedor', c.numeroComprobanteProveedor));
        contenedor.appendChild(fila2(doc, 'Fecha del comprobante', c.fechaEmisionComprobante));
        contenedor.appendChild(fila2(doc, 'Clave', c.claveComprobante));
        contenedor.appendChild(fila2(doc, 'Estado', c.estado));
        contenedor.appendChild(fila2(doc, 'Orden de compra', c.ordenCompraNumero));
        contenedor.appendChild(fila2(doc, 'Subtotal', c.subtotal === null || c.subtotal === undefined ? null : moneda + ' ' + fmt(c.subtotal)));
        contenedor.appendChild(fila2(doc, 'IVA', c.impuesto === null || c.impuesto === undefined ? null : moneda + ' ' + fmt(c.impuesto)));
        contenedor.appendChild(fila2(doc, 'Monto', moneda + ' ' + fmt(c.monto)));
        contenedor.appendChild(fila2(doc, 'Saldo', moneda + ' ' + fmt(c.saldoPendiente)));
        if (c.estado === 'ANULADA') {
            contenedor.appendChild(fila2(doc, 'Fecha de anulación', c.fechaAnulacion));
            contenedor.appendChild(fila2(doc, 'Motivo de anulación', c.motivoAnulacion));
        }

        contenedor.appendChild(el(doc, 'h6', 'mt-3', 'Desglose de IVA'));
        var tabla = el(doc, 'table', 'table table-sm align-middle mb-0');
        var thead = doc.createElement('thead');
        var trh = doc.createElement('tr');
        ['Tarifa', 'Base', 'IVA'].forEach(function (t, i) {
            trh.appendChild(el(doc, 'th', i ? 'text-end' : '', t));
        });
        thead.appendChild(trh);
        tabla.appendChild(thead);
        var tbody = doc.createElement('tbody');
        var desglose = c.desglose || [];
        if (!desglose.length) {
            var vacio = doc.createElement('tr');
            var td = el(doc, 'td', 'text-muted', 'Sin desglose');
            td.setAttribute('colspan', '3');
            vacio.appendChild(td);
            tbody.appendChild(vacio);
        }
        desglose.forEach(function (d) {
            var tr = doc.createElement('tr');
            var etiqueta = d.codigoTarifa ? (d.codigoTarifa + ' (' + d.porcentaje + '%)') : 'Sin tarifa';
            tr.appendChild(el(doc, 'td', '', etiqueta));
            tr.appendChild(el(doc, 'td', 'text-end', fmt(d.base)));
            tr.appendChild(el(doc, 'td', 'text-end', fmt(d.iva)));
            tbody.appendChild(tr);
        });
        tabla.appendChild(tbody);
        contenedor.appendChild(tabla);
    }

    /** Historial de pagos de una CxP (DTO de la API): una fila por pago, todo con textContent. */
    function renderHistorial(doc, tbody, pagos) {
        limpiar(tbody);
        if (!pagos || !pagos.length) {
            var vacio = doc.createElement('tr');
            var td = el(doc, 'td', 'text-center text-muted py-3', 'Sin pagos registrados.');
            td.setAttribute('colspan', '6');
            vacio.appendChild(td);
            tbody.appendChild(vacio);
            return;
        }
        pagos.forEach(function (p) {
            var moneda = p.moneda || 'CRC';
            var tr = doc.createElement('tr');
            tr.appendChild(el(doc, 'td', 'fw-bold', p.numero || '—'));
            tr.appendChild(el(doc, 'td', '', p.fecha || '—'));
            var tdMonto = el(doc, 'td', 'text-end', moneda + ' ' + fmt(p.monto));
            if (p.tipoCambio && moneda !== 'CRC') {
                tdMonto.appendChild(el(doc, 'small', 'text-muted ms-1', '(TC ' + p.tipoCambio + ')'));
            }
            tr.appendChild(tdMonto);
            var tdMetodo = el(doc, 'td', 'text-center');
            tdMetodo.appendChild(el(doc, 'span', 'badge bg-secondary', p.metodoPago || '—'));
            if (p.cuentaBancaria) {
                tdMetodo.appendChild(doc.createElement('br'));
                tdMetodo.appendChild(el(doc, 'small', 'text-muted', p.cuentaBancaria));
            }
            tr.appendChild(tdMetodo);
            tr.appendChild(el(doc, 'td', '', p.referencia || '—'));
            var tdEstado = el(doc, 'td', 'text-center');
            tdEstado.appendChild(el(doc, 'span', p.estado === 'CONFIRMADO' ? 'badge badge-secondary' : 'badge badge-outline-secondary', p.estado || '—'));
            tr.appendChild(tdEstado);
            tbody.appendChild(tr);
        });
    }

    global.CxpDesglose = {
        renderHistorial: renderHistorial,
        TARIFAS: TARIFAS,
        porcentajeDe: porcentajeDe,
        calcularFila: calcularFila,
        totales: totales,
        filaPorDefecto: filaPorDefecto,
        validar: validar,
        armarPeticion: armarPeticion,
        puedeAnular: puedeAnular,
        validarMotivo: validarMotivo,
        renderMensajes: renderMensajes,
        renderFila: renderFila,
        renderDesglose: renderDesglose,
        renderDetalle: renderDetalle
    };
})(typeof window !== 'undefined' ? window : this);
