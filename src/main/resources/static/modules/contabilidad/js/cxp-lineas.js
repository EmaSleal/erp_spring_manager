/*
 * T-052/T-054 (ADR-066, ADR-067): comprobante del proveedor y líneas de la cuenta por pagar, cada una con su cuenta y su
 * tratamiento de IVA. Funciones puras (cálculo por tratamiento, validaciones espejo de las del servidor) y ayudas de
 * pintado que solo usan createElement y textContent: ningún dato del servidor se interpreta como HTML.
 * Expone window.CxpLineas.
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

    var TRATAMIENTOS = [
        { codigo: 'PLENO', etiqueta: 'Acreditable pleno' },
        { codigo: 'USO_COMUN', etiqueta: 'Uso común (prorrata)' },
        { codigo: 'LIMITADO', etiqueta: 'Limitado a tarifa reducida' },
        { codigo: 'NO_ACREDITABLE', etiqueta: 'No acreditable' }
    ];
    var TOPE_CUENTAS_VISIBLES = 200;

    function fmt(n) {
        return Number(n || 0).toFixed(2);
    }

    function etiquetaTratamiento(codigo) {
        for (var i = 0; i < TRATAMIENTOS.length; i++) {
            if (TRATAMIENTOS[i].codigo === codigo) return TRATAMIENTOS[i].etiqueta;
        }
        return codigo || '—';
    }

    /**
     * Línea nueva. regla (opcional, la devuelve GET /api/cuentas-pagar/politica-iva, T-058): si fija un tratamiento, la línea
     * nace con él; sin regla es PLENO. El JS no conoce las reglas, solo aplica lo que dice el servidor.
     */
    function lineaPorDefecto(regla) {
        var trat = regla && regla.tratamientoFijo ? regla.tratamientoFijo : 'PLENO';
        return { cuentaId: '', descripcion: '', codigoTarifa: CODIGO_GENERAL, base: '', iva: '', tratamiento: trat, tarifaLimite: '' };
    }

    /** Copia de las líneas con el tratamiento fijo de la regla (si lo hay); sin regla o sin tratamiento fijo, iguales. */
    function aplicarPolitica(lineas, regla) {
        return (lineas || []).map(function (l) {
            var n = {};
            Object.keys(l).forEach(function (k) { n[k] = l[k]; });
            if (regla && regla.tratamientoFijo) {
                n.tratamiento = regla.tratamientoFijo;
                n.tarifaLimite = '';
            }
            return n;
        });
    }

    var MSG_POLITICA_NO_DISPONIBLE = 'No se pudo consultar la política de IVA de la fecha; intente de nuevo';

    /**
     * Consulta la política de IVA de compra de una fecha (y proveedor). fetchFn(url) devuelve una promesa con
     * { ok, status, json() } (fetch o su envoltura con CSRF). Devuelve la regla, { error: mensaje } si el servidor la
     * rechaza (409 sin tramo) o falla, o null si no hay fecha.
     */
    function consultarPolitica(fetchFn, fecha, proveedorId) {
        if (esVacio(fecha)) return Promise.resolve(null);
        var url = '/api/cuentas-pagar/politica-iva?fecha=' + encodeURIComponent(String(fecha).trim());
        if (!esVacio(proveedorId)) url += '&proveedorId=' + encodeURIComponent(String(proveedorId));
        return Promise.resolve().then(function () { return fetchFn(url); }).then(function (resp) {
            return resp.json().then(function (cuerpo) {
                if (resp.ok && cuerpo && cuerpo.data) return cuerpo.data;
                return { error: cuerpo && cuerpo.message ? String(cuerpo.message) : MSG_POLITICA_NO_DISPONIBLE };
            });
        }).catch(function () { return { error: MSG_POLITICA_NO_DISPONIBLE }; });
    }

    /**
     * Cálculo de una línea (espejo del servidor, ADR-067 §3), en centavos y con mitad hacia arriba:
     * PLENO = IVA; USO_COMUN = IVA × porcentaje del año; LIMITADO = base × tarifa límite (sin pasar del IVA);
     * NO_ACREDITABLE = 0. No acreditable = IVA − acreditable. prorrata: porcentaje del año del comprobante o null.
     * sinProrrata = USO_COMUN sin porcentaje (el acreditable no se puede calcular; se informa 0).
     */
    function calcularLinea(linea, prorrata) {
        var ivaC = aCentavos(linea.iva);
        var baseC = aCentavos(linea.base);
        var acreC = 0;
        var sinProrrata = false;
        switch (linea.tratamiento) {
            case 'USO_COMUN':
                if (prorrata === null || prorrata === undefined || isNaN(Number(prorrata))) sinProrrata = true;
                else acreC = ivaEnCentavos(ivaC, Number(prorrata));
                break;
            case 'LIMITADO':
                acreC = esVacio(linea.tarifaLimite) ? 0 : Math.min(ivaC, ivaEnCentavos(baseC, Number(linea.tarifaLimite) || 0));
                break;
            case 'NO_ACREDITABLE':
                acreC = 0;
                break;
            default:
                acreC = ivaC;
        }
        return { iva: deCentavos(ivaC), acreditable: deCentavos(acreC), noAcreditable: deCentavos(ivaC - acreC),
            sinProrrata: sinProrrata, _ivaC: ivaC, _acreC: acreC, _baseC: baseC };
    }

    function totales(lineas, prorrata) {
        var sb = 0, si = 0, sa = 0;
        (lineas || []).forEach(function (l) {
            var r = calcularLinea(l, prorrata);
            sb += r._baseC;
            si += r._ivaC;
            sa += r._acreC;
        });
        return { subtotal: deCentavos(sb), impuesto: deCentavos(si), total: deCentavos(sb + si),
            acreditable: deCentavos(sa), noAcreditable: deCentavos(si - sa) };
    }

    function usaProrrata(lineas) {
        return (lineas || []).some(function (l) { return l.tratamiento === 'USO_COMUN'; });
    }

    /** Año de una fecha AAAA-MM-DD, o null. */
    function anioDe(fecha) {
        var m = /^(\d{4})-\d{2}-\d{2}/.exec(esVacio(fecha) ? '' : String(fecha));
        return m ? parseInt(m[1], 10) : null;
    }

    var MSG_VENCIMIENTO = 'La fecha de vencimiento no puede ser anterior a la fecha del comprobante';

    /**
     * Fecha de vencimiento propuesta (T-053, criterio 15): fecha del comprobante (AAAA-MM-DD) + días de crédito del
     * proveedor; sin días (0, nulo, vacío o negativo) es de contado y vence el día del comprobante. Cadena vacía si la
     * fecha del comprobante no es válida. Mismo cálculo que el servidor.
     */
    function calcularVencimiento(fechaComprobante, diasCredito) {
        var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(esVacio(fechaComprobante) ? '' : String(fechaComprobante));
        if (!m) return '';
        var dias = parseInt(diasCredito, 10);
        if (isNaN(dias) || dias < 0) dias = 0;
        var f = new Date(Date.UTC(parseInt(m[1], 10), parseInt(m[2], 10) - 1, parseInt(m[3], 10) + dias));
        return f.getUTCFullYear() + '-' + String(f.getUTCMonth() + 1).padStart(2, '0') + '-' + String(f.getUTCDate()).padStart(2, '0');
    }

    /**
     * Aplica el cambio de un campo a una línea y devuelve la línea nueva: base o tarifa recalculan el IVA (que luego se
     * puede corregir a mano) y un tratamiento distinto de LIMITADO limpia la tarifa límite.
     */
    function cambiarLinea(linea, campo, valor, regla) {
        var n = {};
        Object.keys(linea).forEach(function (k) { n[k] = linea[k]; });
        n[campo] = valor;
        if (campo === 'base' || campo === 'codigoTarifa') {
            var pct = porcentajeDe(n.codigoTarifa);
            n.iva = pct === null || esVacio(n.base) ? '' : String(calcularFila(n.base, pct));
            // T-058: el proveedor del régimen simplificado no traslada IVA; la tarifa del bien se conserva
            if (regla && regla.ivaTrasladadoCero && n.iva !== '') n.iva = '0';
        }
        if (regla && regla.tratamientoFijo) n.tratamiento = regla.tratamientoFijo;
        if ((campo === 'tratamiento' || (regla && regla.tratamientoFijo)) && n.tratamiento !== 'LIMITADO') n.tarifaLimite = '';
        return n;
    }

    function sinTildes(t) {
        var s = String(t === null || t === undefined ? '' : t).toLowerCase();
        return typeof s.normalize === 'function' ? s.normalize('NFD').replace(/[̀-ͯ]/g, '') : s;
    }

    /** Hojas cuyo código o nombre contienen el texto (sin distinguir mayúsculas ni tildes). */
    function filtrarCuentas(cuentas, texto) {
        if (!cuentas) return [];
        var q = sinTildes(texto).trim();
        if (!q) return cuentas.slice();
        return cuentas.filter(function (c) {
            return sinTildes(c.codigo).indexOf(q) >= 0 || sinTildes(c.nombre).indexOf(q) >= 0;
        });
    }

    /**
     * Validaciones del formulario, con las mismas reglas que el servidor. datos: { numero, clave, fecha, hoy (AAAA-MM-DD),
     * lineas, prorrata (porcentaje del año del comprobante o null), totalComprobante, regla (T-058, opcional: la política de
     * IVA que devolvió el servidor) }.
     * Devuelve { errores, advertencias } (textos fijos).
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

        if (!esVacio(datos.vencimiento) && !esVacio(datos.fecha) && String(datos.vencimiento) < String(datos.fecha)) {
            errores.push(MSG_VENCIMIENTO);
        }

        var lineas = datos.lineas || [];
        if (!lineas.length) errores.push('La cuenta por pagar debe tener al menos una línea');
        var ivaCero = !!(datos.regla && datos.regla.ivaTrasladadoCero);
        lineas.forEach(function (l, i) {
            var n = i + 1;
            if (esVacio(l.cuentaId)) {
                errores.push('La línea ' + n + ' requiere una cuenta contable');
                return;
            }
            if (esVacio(l.base) || isNaN(parseFloat(l.base)) || parseFloat(l.base) <= 0) {
                errores.push('La línea ' + n + ' requiere una base mayor a 0');
                return;
            }
            if (esVacio(l.iva) || isNaN(parseFloat(l.iva)) || parseFloat(l.iva) < 0) {
                errores.push('La línea ' + n + ' requiere un IVA no negativo');
                return;
            }
            var pct = porcentajeDe(l.codigoTarifa);
            if (pct === null) {
                errores.push('La tarifa de IVA de la línea ' + n + ' no es válida');
                return;
            }
            if (l.tratamiento === 'LIMITADO') {
                var lim = esVacio(l.tarifaLimite) ? NaN : parseFloat(l.tarifaLimite);
                if (isNaN(lim) || lim <= 0 || lim >= pct) {
                    errores.push('La tarifa límite de la línea ' + n + ' debe ser mayor a 0 y menor que la tarifa');
                    return;
                }
            }
            var esperado = ivaEnCentavos(aCentavos(l.base), pct);
            // T-058: con IVA trasladado 0 (proveedor SIMPLIFICADO) la diferencia con base × tarifa es lo correcto
            if (!ivaCero && Math.abs(aCentavos(l.iva) - esperado) > TOLERANCIA_CENTAVOS) {
                advertencias.push('La línea ' + n + ': el IVA ' + fmt(parseFloat(l.iva))
                    + ' difiere de la base por la tarifa (' + fmt(deCentavos(esperado)) + ')');
            }
        });

        if (usaProrrata(lineas) && (datos.prorrata === null || datos.prorrata === undefined || isNaN(Number(datos.prorrata)))) {
            var anio = anioDe(datos.fecha);
            advertencias.push('No hay porcentaje de prorrata configurado para ' + (anio === null ? 'el año del comprobante' : anio));
        }

        if (!esVacio(datos.totalComprobante) && !isNaN(parseFloat(datos.totalComprobante))) {
            var t = totales(lineas, datos.prorrata).total;
            if (Math.abs(aCentavos(datos.totalComprobante) - aCentavos(t)) > TOLERANCIA_CENTAVOS) {
                advertencias.push('El total del comprobante (' + fmt(parseFloat(datos.totalComprobante))
                    + ') no cuadra con las líneas (' + fmt(t) + ')');
            }
        }
        return { errores: errores, advertencias: advertencias };
    }

    /** Cuerpo del POST /api/cuentas-pagar; los totales los calcula el servidor a partir de las líneas. */
    function armarPeticion(datos) {
        var cuerpo = {};
        var extra = datos.extra || {};
        Object.keys(extra).forEach(function (k) { cuerpo[k] = extra[k]; });
        cuerpo.numeroComprobanteProveedor = String(datos.numero || '').trim();
        cuerpo.claveComprobante = esVacio(datos.clave) ? null : String(datos.clave).trim();
        cuerpo.fechaComprobante = datos.fecha;
        cuerpo.fechaEmision = datos.fecha;
        cuerpo.fechaVencimiento = esVacio(datos.vencimiento) ? null : datos.vencimiento;
        cuerpo.lineas = (datos.lineas || []).map(function (l) {
            return {
                descripcion: esVacio(l.descripcion) ? null : String(l.descripcion).trim(),
                cuentaContableId: parseInt(l.cuentaId, 10),
                codigoTarifa: l.codigoTarifa,
                base: deCentavos(aCentavos(l.base)),
                iva: deCentavos(aCentavos(l.iva)),
                tratamiento: l.tratamiento || 'PLENO',
                tarifaLimite: l.tratamiento === 'LIMITADO' && !esVacio(l.tarifaLimite) ? parseFloat(l.tarifaLimite) : null
            };
        });
        return cuerpo;
    }

    /** Valida el registro de aceptación del comprobante (mismas reglas que el servidor); devuelve el mensaje o null. */
    function validarAceptacion(datos) {
        if (datos.estado !== 'ACEPTADO' && datos.estado !== 'RECHAZADO') return 'El estado debe ser Aceptado o Rechazado';
        if (esVacio(datos.fecha)) return 'La fecha de aceptación es obligatoria';
        if (datos.hoy && String(datos.fecha) > String(datos.hoy)) return 'La fecha de aceptación no puede ser futura';
        if (datos.fechaComprobante && String(datos.fecha) < String(datos.fechaComprobante)) {
            return 'La fecha de aceptación no puede ser anterior a la fecha del comprobante';
        }
        var numero = esVacio(datos.numero) ? '' : String(datos.numero).trim();
        if (!numero) return 'El número del mensaje receptor es obligatorio';
        if (numero.length > LARGO_NUMERO) return 'El número del mensaje receptor no puede exceder 50 caracteres';
        return null;
    }

    function puedeRegistrarAceptacion(cuenta, tienePermiso) {
        // T-058: una CxP del régimen simplificado (NO_APLICA) no se acepta ni se rechaza
        return !!tienePermiso && !!cuenta && cuenta.estado !== 'ANULADA' && cuenta.estadoAceptacion !== 'NO_APLICA';
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
    /** Recorre los hijos de un nodo (en el navegador y en las pruebas); f recibe cada descendiente. */
    function recorrer(nodo, f) {
        var hijos = nodo.children || [];
        for (var i = 0; i < hijos.length; i++) {
            f(hijos[i]);
            recorrer(hijos[i], f);
        }
    }

    function opcionesDeCuentas(doc, sel, cuentas, texto, elegida) {
        limpiar(sel);
        var vacia = el(doc, 'option', '', '— Seleccionar —');
        vacia.value = '';
        sel.appendChild(vacia);
        var visibles = filtrarCuentas(cuentas, texto);
        var hayElegida = false;
        visibles.slice(0, TOPE_CUENTAS_VISIBLES).forEach(function (c) {
            var o = el(doc, 'option', '', c.codigo + ' - ' + c.nombre);
            o.value = String(c.id);
            if (String(c.id) === String(elegida)) hayElegida = true;
            sel.appendChild(o);
        });
        if (!esVacio(elegida) && !hayElegida) {
            // la cuenta ya elegida no se pierde aunque el buscador no la muestre
            (cuentas || []).forEach(function (c) {
                if (String(c.id) === String(elegida)) {
                    var o = el(doc, 'option', '', c.codigo + ' - ' + c.nombre);
                    o.value = String(c.id);
                    sel.appendChild(o);
                }
            });
        }
        sel.value = esVacio(elegida) ? '' : String(elegida);
    }

    function celda(doc, hijo, clase) {
        var td = doc.createElement('td');
        if (clase) td.className = clase;
        td.appendChild(hijo);
        return td;
    }

    /**
     * Fila editable de una línea: buscador y selector de hojas, descripción, tarifa, base, IVA, tratamiento, tarifa límite
     * (solo se ve con LIMITADO), acreditable y no acreditable (calculados) y quitar.
     * cuentas: [{ id, codigo, nombre }]. handlers: { cambio(i, campo, valor), quitar(i) }.
     */
    function renderLinea(doc, linea, indice, cuentas, handlers, regla) {
        var tr = doc.createElement('tr');
        tr.setAttribute('data-fila', String(indice));

        var buscar = el(doc, 'input', 'form-control form-control-sm mb-1');
        buscar.setAttribute('type', 'search');
        buscar.setAttribute('placeholder', 'Buscar cuenta...');
        buscar.setAttribute('aria-label', 'Buscar cuenta');
        buscar.setAttribute('data-campo', 'buscarCuenta');
        var selCuenta = el(doc, 'select', 'form-select form-select-sm');
        selCuenta.setAttribute('data-campo', 'cuentaId');
        selCuenta.setAttribute('aria-label', 'Cuenta contable');
        opcionesDeCuentas(doc, selCuenta, cuentas, '', linea.cuentaId);
        buscar.addEventListener('input', function () {
            opcionesDeCuentas(doc, selCuenta, cuentas, buscar.value, selCuenta.value);
        });
        selCuenta.addEventListener('change', function () { handlers.cambio(indice, 'cuentaId', selCuenta.value); });
        var tdCuenta = doc.createElement('td');
        tdCuenta.appendChild(buscar);
        tdCuenta.appendChild(selCuenta);
        tr.appendChild(tdCuenta);

        var desc = el(doc, 'input', 'form-control form-control-sm');
        desc.setAttribute('type', 'text');
        desc.setAttribute('maxlength', '300');
        desc.setAttribute('data-campo', 'descripcion');
        desc.setAttribute('aria-label', 'Descripción de la línea');
        desc.value = esVacio(linea.descripcion) ? '' : String(linea.descripcion);
        desc.addEventListener('input', function () { handlers.cambio(indice, 'descripcion', desc.value); });
        tr.appendChild(celda(doc, desc));

        var selTarifa = el(doc, 'select', 'form-select form-select-sm');
        selTarifa.setAttribute('data-campo', 'codigoTarifa');
        selTarifa.setAttribute('aria-label', 'Tarifa de IVA');
        TARIFAS.forEach(function (t) {
            var o = el(doc, 'option', '', t.etiqueta);
            o.value = t.codigo;
            selTarifa.appendChild(o);
        });
        selTarifa.value = linea.codigoTarifa;
        selTarifa.addEventListener('change', function () { handlers.cambio(indice, 'codigoTarifa', selTarifa.value); });
        tr.appendChild(celda(doc, selTarifa));

        ['base', 'iva'].forEach(function (campo) {
            var inp = el(doc, 'input', 'form-control form-control-sm text-end');
            inp.setAttribute('type', 'number');
            inp.setAttribute('step', '0.01');
            inp.setAttribute('min', '0');
            inp.setAttribute('data-campo', campo);
            inp.setAttribute('aria-label', campo === 'base' ? 'Base' : 'IVA');
            inp.value = linea[campo] === undefined || linea[campo] === null ? '' : String(linea[campo]);
            inp.addEventListener('input', function () { handlers.cambio(indice, campo, inp.value); });
            tr.appendChild(celda(doc, inp));
        });

        var selTrat = el(doc, 'select', 'form-select form-select-sm');
        selTrat.setAttribute('data-campo', 'tratamiento');
        selTrat.setAttribute('aria-label', 'Tratamiento del IVA');
        TRATAMIENTOS.forEach(function (t) {
            var o = el(doc, 'option', '', t.etiqueta);
            o.value = t.codigo;
            selTrat.appendChild(o);
        });
        var fijo = regla && regla.tratamientoFijo ? regla.tratamientoFijo : null;
        selTrat.value = fijo || linea.tratamiento || 'PLENO';
        selTrat.disabled = !!fijo;
        selTrat.addEventListener('change', function () { handlers.cambio(indice, 'tratamiento', selTrat.value); });
        var limite = el(doc, 'input', 'form-control form-control-sm text-end mt-1');
        limite.setAttribute('type', 'number');
        limite.setAttribute('step', '0.01');
        limite.setAttribute('min', '0');
        limite.setAttribute('data-campo', 'tarifaLimite');
        limite.setAttribute('placeholder', 'Tarifa límite %');
        limite.setAttribute('aria-label', 'Tarifa límite');
        limite.value = esVacio(linea.tarifaLimite) ? '' : String(linea.tarifaLimite);
        limite.addEventListener('input', function () { handlers.cambio(indice, 'tarifaLimite', limite.value); });
        var envoltura = el(doc, 'div', '');
        envoltura.setAttribute('data-envoltura', 'tarifaLimite');
        envoltura.style.display = linea.tratamiento === 'LIMITADO' ? '' : 'none';
        envoltura.appendChild(limite);
        var tdTrat = doc.createElement('td');
        tdTrat.appendChild(selTrat);
        tdTrat.appendChild(envoltura);
        tr.appendChild(tdTrat);

        ['acreditable', 'noAcreditable'].forEach(function (campo) {
            var td = el(doc, 'td', 'text-end');
            td.setAttribute('data-calculado', campo);
            tr.appendChild(td);
        });

        var btn = el(doc, 'button', 'btn btn-sm btn-outline-danger');
        btn.setAttribute('type', 'button');
        btn.setAttribute('data-accion', 'quitar');
        btn.setAttribute('title', 'Quitar línea');
        btn.setAttribute('aria-label', 'Quitar línea');
        btn.appendChild(el(doc, 'i', 'fas fa-times'));
        btn.addEventListener('click', function () { handlers.quitar(indice); });
        tr.appendChild(celda(doc, btn));

        actualizarLinea(tr, linea, null);
        return tr;
    }

    /** Actualiza los valores calculados de la fila (acreditable, no acreditable) y la visibilidad de la tarifa límite. */
    function actualizarLinea(fila, linea, prorrata) {
        var r = calcularLinea(linea, prorrata);
        recorrer(fila, function (x) {
            var calc = x.getAttribute ? x.getAttribute('data-calculado') : null;
            if (calc === 'acreditable') x.textContent = r.sinProrrata ? '—' : fmt(r.acreditable);
            else if (calc === 'noAcreditable') x.textContent = r.sinProrrata ? '—' : fmt(r.noAcreditable);
            else if (x.getAttribute && x.getAttribute('data-envoltura') === 'tarifaLimite') {
                x.style.display = linea.tratamiento === 'LIMITADO' ? '' : 'none';
            }
        });
    }

    function renderLineas(doc, tbody, lineas, cuentas, handlers, prorrata, regla) {
        limpiar(tbody);
        lineas.forEach(function (l, i) {
            var fila = renderLinea(doc, l, i, cuentas, handlers, regla);
            actualizarLinea(fila, l, prorrata);
            tbody.appendChild(fila);
        });
    }

    function fila2(doc, etiqueta, valor) {
        var row = el(doc, 'div', 'row mb-1');
        row.appendChild(el(doc, 'div', 'col-sm-4 text-muted', etiqueta));
        row.appendChild(el(doc, 'div', 'col-sm-8 fw-semibold', valor === null || valor === undefined || valor === '' ? '—' : valor));
        return row;
    }

    /** Detalle de una CxP (DTO de la API): comprobante, líneas con cuenta y tratamiento, y aceptación del comprobante. */
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
        if (c.adjuntoNombre) {
            // T-058 (ADR-077 §7): comprobante adjunto de la compra de contado; el enlace se arma con setAttribute, el nombre es texto
            var filaAdjunto = el(doc, 'div', 'row mb-1');
            filaAdjunto.appendChild(el(doc, 'div', 'col-sm-4 text-muted', 'Comprobante adjunto'));
            var celdaAdjunto = el(doc, 'div', 'col-sm-8 fw-semibold');
            var enlace = el(doc, 'a', '', c.adjuntoNombre);
            enlace.setAttribute('href', '/api/cuentas-pagar/' + encodeURIComponent(String(c.id)) + '/adjunto');
            enlace.setAttribute('rel', 'noopener');
            celdaAdjunto.appendChild(enlace);
            filaAdjunto.appendChild(celdaAdjunto);
            contenedor.appendChild(filaAdjunto);
        }
        contenedor.appendChild(fila2(doc, 'Subtotal', c.subtotal === null || c.subtotal === undefined ? null : moneda + ' ' + fmt(c.subtotal)));
        contenedor.appendChild(fila2(doc, 'IVA', c.impuesto === null || c.impuesto === undefined ? null : moneda + ' ' + fmt(c.impuesto)));
        contenedor.appendChild(fila2(doc, 'Monto', moneda + ' ' + fmt(c.monto)));
        contenedor.appendChild(fila2(doc, 'Saldo', moneda + ' ' + fmt(c.saldoPendiente)));
        if (c.estado === 'ANULADA') {
            contenedor.appendChild(fila2(doc, 'Fecha de anulación', c.fechaAnulacion));
            contenedor.appendChild(fila2(doc, 'Motivo de anulación', c.motivoAnulacion));
        }

        contenedor.appendChild(el(doc, 'h6', 'mt-3', 'Aceptación del comprobante'));
        contenedor.appendChild(fila2(doc, 'Estado', c.estadoAceptacion === 'NO_APLICA' ? 'No aplica (RTS)' : (c.estadoAceptacion || 'PENDIENTE')));
        contenedor.appendChild(fila2(doc, 'Fecha del mensaje receptor', c.fechaAceptacion));
        contenedor.appendChild(fila2(doc, 'Número del mensaje receptor', c.numeroMensajeReceptor));
        if (c.diasDesdeComprobante !== null && c.diasDesdeComprobante !== undefined) {
            contenedor.appendChild(fila2(doc, 'Desde la fecha del comprobante', c.diasDesdeComprobante + ' días'));
        }

        contenedor.appendChild(el(doc, 'h6', 'mt-3', 'Líneas'));
        var tabla = el(doc, 'table', 'table table-sm align-middle mb-0');
        var thead = doc.createElement('thead');
        var trh = doc.createElement('tr');
        ['#', 'Cuenta', 'Descripción', 'Tarifa', 'Base', 'IVA', 'Tratamiento', 'Acreditable', 'No acreditable'].forEach(function (t, i) {
            trh.appendChild(el(doc, 'th', i >= 4 && i !== 6 ? 'text-end' : '', t));
        });
        thead.appendChild(trh);
        tabla.appendChild(thead);
        var tbody = doc.createElement('tbody');
        var lineas = c.lineas || [];
        if (!lineas.length) {
            var vacio = doc.createElement('tr');
            var td = el(doc, 'td', 'text-muted', 'Sin líneas');
            td.setAttribute('colspan', '9');
            vacio.appendChild(td);
            tbody.appendChild(vacio);
        }
        lineas.forEach(function (l) {
            var tr = doc.createElement('tr');
            tr.appendChild(el(doc, 'td', '', l.numeroLinea));
            tr.appendChild(el(doc, 'td', '', (l.cuentaCodigo || '') + ' ' + (l.cuentaNombre || '')));
            tr.appendChild(el(doc, 'td', '', l.descripcion));
            tr.appendChild(el(doc, 'td', '', l.codigoTarifa ? (l.codigoTarifa + ' (' + l.porcentaje + '%)') : 'Sin tarifa'));
            tr.appendChild(el(doc, 'td', 'text-end', fmt(l.base)));
            tr.appendChild(el(doc, 'td', 'text-end', fmt(l.iva)));
            var trat = etiquetaTratamiento(l.tratamiento);
            if (l.tratamiento === 'LIMITADO' && l.tarifaLimite !== null && l.tarifaLimite !== undefined) trat += ' (' + l.tarifaLimite + '%)';
            if (l.tratamiento === 'USO_COMUN' && l.porcentajeProrrata !== null && l.porcentajeProrrata !== undefined) {
                trat += ' (prorrata ' + l.porcentajeProrrata + '%)';
            }
            tr.appendChild(el(doc, 'td', '', trat));
            tr.appendChild(el(doc, 'td', 'text-end', fmt(l.ivaAcreditable)));
            tr.appendChild(el(doc, 'td', 'text-end', fmt(l.ivaNoAcreditable)));
            tbody.appendChild(tr);
        });
        tabla.appendChild(tbody);
        contenedor.appendChild(tabla);
    }

    /**
     * Pinta el texto de la política de IVA (o el error de consulta) en el contenedor, solo con textContent. Una regla sin
     * mensaje deja el contenedor vacío.
     */
    function renderPolitica(doc, contenedor, regla) {
        limpiar(contenedor);
        if (!regla) return;
        if (regla.error) {
            contenedor.appendChild(el(doc, 'div', 'alert alert-danger py-1 px-2 mb-1 small', regla.error));
            return;
        }
        if (!esVacio(regla.mensaje)) {
            contenedor.appendChild(el(doc, 'div', 'alert alert-info py-1 px-2 mb-1 small', regla.mensaje));
        }
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

    global.CxpLineas = {
        renderHistorial: renderHistorial,
        calcularVencimiento: calcularVencimiento,
        TARIFAS: TARIFAS,
        TRATAMIENTOS: TRATAMIENTOS,
        porcentajeDe: porcentajeDe,
        calcularFila: calcularFila,
        calcularLinea: calcularLinea,
        totales: totales,
        lineaPorDefecto: lineaPorDefecto,
        aplicarPolitica: aplicarPolitica,
        consultarPolitica: consultarPolitica,
        renderPolitica: renderPolitica,
        cambiarLinea: cambiarLinea,
        usaProrrata: usaProrrata,
        anioDe: anioDe,
        filtrarCuentas: filtrarCuentas,
        validar: validar,
        armarPeticion: armarPeticion,
        validarAceptacion: validarAceptacion,
        puedeRegistrarAceptacion: puedeRegistrarAceptacion,
        puedeAnular: puedeAnular,
        validarMotivo: validarMotivo,
        renderMensajes: renderMensajes,
        renderLinea: renderLinea,
        actualizarLinea: actualizarLinea,
        renderLineas: renderLineas,
        renderDetalle: renderDetalle
    };
})(typeof window !== 'undefined' ? window : this);
