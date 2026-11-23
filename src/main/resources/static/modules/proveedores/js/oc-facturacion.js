/*
 * T-053 (ADR-066 §4, ADR-067): detalle de la orden de compra para facturar. Muestra el total, lo facturado (CxP no
 * anuladas), el pendiente, el exceso y las CxP de la orden, arma las líneas de la factura del proveedor a partir de los
 * detalles de la orden (con la cuenta propuesta, editable) y pinta las advertencias. Solo createElement y textContent:
 * ningún dato del servidor se interpreta como HTML. Usa window.CxpLineas (cxp-lineas.js) para las líneas.
 * Expone window.OcFacturacion.
 */
(function (global) {
    'use strict';

    function fmt(n) {
        var v = Number(n);
        return (isNaN(v) ? 0 : v).toFixed(2);
    }

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

    function esVacio(v) {
        return v === null || v === undefined || String(v).trim() === '';
    }

    /**
     * Líneas de la factura a partir de los detalles de la orden: una por detalle (descripción, base = cantidad × precio
     * unitario, tarifa general al 13 %, IVA pleno) con la cuenta propuesta (parámetro COMPRAS_MERCADERIA) si la hay; si
     * no hay propuesta la cuenta queda vacía (se elige a mano). Sin detalles, una línea por defecto. El usuario las edita,
     * quita o agrega (p. ej. un flete a su propia cuenta).
     */
    function lineasDesdeOc(detalles, cuentaPropuestaId) {
        var C = global.CxpLineas;
        var lista = detalles || [];
        var cuenta = esVacio(cuentaPropuestaId) ? '' : cuentaPropuestaId;
        if (!lista.length) {
            var unica = C.lineaPorDefecto();
            unica.cuentaId = cuenta;
            return [unica];
        }
        return lista.map(function (d) {
            var linea = C.lineaPorDefecto();
            var base = Math.round((Number(d.cantidad) || 0) * (Number(d.precioUnitario) || 0) * 100) / 100;
            linea.cuentaId = cuenta;
            linea.descripcion = esVacio(d.descripcion) ? '' : String(d.descripcion);
            linea.base = base > 0 ? base : '';
            linea.iva = base > 0 ? C.calcularFila(base, C.porcentajeDe(linea.codigoTarifa)) : '';
            return linea;
        });
    }

    /** Aviso fijo cuando lo facturado supera el total de la orden; null si no hay exceso. No bloquea nada. */
    function advertenciaExceso(dto) {
        if (!dto || !(Number(dto.exceso) > 0)) return null;
        return 'Lo facturado (' + fmt(dto.facturado) + ') supera el total de la orden de compra ' + dto.numero
            + ' (' + fmt(dto.total) + ') por ' + fmt(dto.exceso);
    }

    /** ¿Se muestra «Registrar factura del proveedor»? Orden recibida (la decide el servidor) y permiso de crear CxP. */
    function puedeFacturar(dto, tienePermiso) {
        return !!(dto && dto.admiteFacturar && tienePermiso);
    }

    function badgeEstado(estado) {
        var clases = {
            PENDIENTE: 'badge bg-warning text-dark', PARCIAL: 'badge bg-info text-dark', PAGADA: 'badge bg-success',
            VENCIDA: 'badge bg-danger', ANULADA: 'badge bg-secondary'
        };
        return clases[estado] || 'badge bg-light text-dark border';
    }

    function tarjeta(doc, etiqueta, valor, clase) {
        var col = el(doc, 'div', 'col-6 col-md-3');
        var caja = el(doc, 'div', 'border rounded p-2 h-100');
        caja.appendChild(el(doc, 'div', 'small text-muted', etiqueta));
        caja.appendChild(el(doc, 'div', 'fw-bold ' + (clase || ''), valor));
        col.appendChild(caja);
        return col;
    }

    /** Pinta el detalle de la orden: encabezado, totales, advertencia de exceso, CxP y detalles. */
    function renderDetalle(doc, contenedor, dto) {
        limpiar(contenedor);
        var moneda = dto.moneda || '';

        var cab = el(doc, 'div', 'mb-3');
        cab.appendChild(el(doc, 'span', 'fw-bold me-2', dto.numero));
        cab.appendChild(el(doc, 'span', 'badge bg-light text-dark border me-2', dto.estado));
        cab.appendChild(el(doc, 'span', 'text-muted', dto.proveedorNombre));
        contenedor.appendChild(cab);

        var fila = el(doc, 'div', 'row g-2 mb-3');
        fila.appendChild(tarjeta(doc, 'Total de la orden (' + moneda + ')', fmt(dto.total)));
        fila.appendChild(tarjeta(doc, 'Facturado', fmt(dto.facturado)));
        fila.appendChild(tarjeta(doc, 'Pendiente de facturar', fmt(dto.pendiente)));
        fila.appendChild(tarjeta(doc, 'Exceso', fmt(dto.exceso), Number(dto.exceso) > 0 ? 'text-danger' : ''));
        contenedor.appendChild(fila);

        var aviso = advertenciaExceso(dto);
        if (aviso) contenedor.appendChild(el(doc, 'div', 'alert alert-warning py-2 small', aviso));

        contenedor.appendChild(el(doc, 'h6', 'mt-3', 'Facturas del proveedor (cuentas por pagar)'));
        var cuentas = dto.cuentas || [];
        if (!cuentas.length) {
            contenedor.appendChild(el(doc, 'p', 'text-muted small', 'Esta orden aún no tiene facturas registradas.'));
        } else {
            var tabla = el(doc, 'table', 'table table-sm align-middle');
            var cabecera = el(doc, 'tr');
            ['CxP', 'N.º de comprobante', 'Fecha', 'Total', 'Estado', 'Aceptación'].forEach(function (t, i) {
                cabecera.appendChild(el(doc, 'th', i === 3 ? 'text-end' : '', t));
            });
            var thead = el(doc, 'thead');
            thead.appendChild(cabecera);
            tabla.appendChild(thead);
            var tbody = el(doc, 'tbody');
            cuentas.forEach(function (c) {
                var tr = el(doc, 'tr');
                tr.appendChild(el(doc, 'td', 'small', c.numero));
                tr.appendChild(el(doc, 'td', 'small', esVacio(c.numeroComprobanteProveedor) ? '—' : c.numeroComprobanteProveedor));
                tr.appendChild(el(doc, 'td', 'small', esVacio(c.fechaComprobante) ? '—' : c.fechaComprobante));
                tr.appendChild(el(doc, 'td', 'small text-end', fmt(c.monto)));
                var tdEstado = el(doc, 'td');
                tdEstado.appendChild(el(doc, 'span', badgeEstado(c.estado), c.estado));
                tr.appendChild(tdEstado);
                tr.appendChild(el(doc, 'td', 'small', esVacio(c.estadoAceptacion) ? '—' : c.estadoAceptacion));
                tbody.appendChild(tr);
            });
            tabla.appendChild(tbody);
            var envoltura = el(doc, 'div', 'table-responsive');
            envoltura.appendChild(tabla);
            contenedor.appendChild(envoltura);
        }

        var detalles = dto.detalles || [];
        if (detalles.length) {
            contenedor.appendChild(el(doc, 'h6', 'mt-3', 'Detalle de la orden'));
            var t2 = el(doc, 'table', 'table table-sm align-middle');
            var c2 = el(doc, 'tr');
            ['Descripción', 'Cantidad', 'Precio', 'Total'].forEach(function (t, i) {
                c2.appendChild(el(doc, 'th', i > 0 ? 'text-end' : '', t));
            });
            var th2 = el(doc, 'thead');
            th2.appendChild(c2);
            t2.appendChild(th2);
            var tb2 = el(doc, 'tbody');
            detalles.forEach(function (d) {
                var tr = el(doc, 'tr');
                tr.appendChild(el(doc, 'td', 'small', esVacio(d.descripcion) ? '—' : d.descripcion));
                tr.appendChild(el(doc, 'td', 'small text-end', d.cantidad));
                tr.appendChild(el(doc, 'td', 'small text-end', fmt(d.precioUnitario)));
                tr.appendChild(el(doc, 'td', 'small text-end', fmt(d.total)));
                tb2.appendChild(tr);
            });
            t2.appendChild(tb2);
            var env2 = el(doc, 'div', 'table-responsive');
            env2.appendChild(t2);
            contenedor.appendChild(env2);
        }
    }

    /** Pinta una lista de advertencias (las de la respuesta del alta, p. ej. el exceso) sin bloquear. */
    function renderAdvertencias(doc, contenedor, advertencias) {
        limpiar(contenedor);
        (advertencias || []).forEach(function (m) {
            contenedor.appendChild(el(doc, 'div', 'alert alert-warning py-2 small mb-2', m));
        });
    }

    /** Pinta las filas de recepción (texto del servidor solo con textContent). */
    function renderFilasRecepcion(doc, tbody, detalles) {
        limpiar(tbody);
        if (!detalles || !detalles.length) {
            var vacia = el(doc, 'tr');
            var td = el(doc, 'td', 'text-center text-muted', 'Esta OC no tiene líneas para recibir.');
            td.setAttribute('colspan', '6');
            vacia.appendChild(td);
            tbody.appendChild(vacia);
            return;
        }
        detalles.forEach(function (d) {
            var porRecibir = d.porRecibir !== undefined && d.porRecibir !== null ? d.porRecibir : (d.cantidad - d.cantidadRecibida);
            var tr = el(doc, 'tr');
            tr.setAttribute('data-detalle-id', d.id);
            tr.appendChild(el(doc, 'td', 'small fw-semibold', d.descripcion));
            tr.appendChild(el(doc, 'td', 'text-end small', d.cantidad));
            tr.appendChild(el(doc, 'td', 'text-end small', d.cantidadRecibida));
            var inRec = el(doc, 'input', 'form-control form-control-sm inp-rec');
            inRec.setAttribute('type', 'number');
            inRec.setAttribute('min', '0');
            inRec.setAttribute('max', porRecibir);
            inRec.value = porRecibir;
            var tdRec = el(doc, 'td');
            tdRec.appendChild(inRec);
            tr.appendChild(tdRec);
            var inLote = el(doc, 'input', 'form-control form-control-sm inp-lote');
            inLote.setAttribute('type', 'text');
            inLote.setAttribute('placeholder', 'Opcional');
            var tdLote = el(doc, 'td');
            tdLote.appendChild(inLote);
            tr.appendChild(tdLote);
            var inVenc = el(doc, 'input', 'form-control form-control-sm inp-venc');
            inVenc.setAttribute('type', 'date');
            var tdVenc = el(doc, 'td');
            tdVenc.appendChild(inVenc);
            tr.appendChild(tdVenc);
            tbody.appendChild(tr);
        });
    }

    global.OcFacturacion = {
        lineasDesdeOc: lineasDesdeOc,
        advertenciaExceso: advertenciaExceso,
        puedeFacturar: puedeFacturar,
        renderDetalle: renderDetalle,
        renderAdvertencias: renderAdvertencias,
        renderFilasRecepcion: renderFilasRecepcion
    };
})(typeof window !== 'undefined' ? window : this);
