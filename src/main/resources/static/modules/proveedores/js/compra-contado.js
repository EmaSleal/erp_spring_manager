/*
 * T-058 (ADR-077 §1-2, §7-9): formulario rápido «Compra de contado». Lógica pura y pintado sin DOM propio: estado del formulario,
 * líneas (reutilizan window.CxpLineas), validaciones espejo de las del servidor, armado del único envío multipart y
 * confirmación. El JS no conoce reglas de IVA: el tratamiento lo fija la regla que devuelve el servidor
 * (GET /api/cuentas-pagar/politica-iva). Todo se pinta con createElement y textContent: nada del servidor se interpreta como HTML.
 * Expone window.CompraContado. Requiere cxp-lineas.js.
 */
(function (global) {
    'use strict';

    var CODIGO_CUENTA_COMPRA = '5.3.01.001';
    var ETIQUETA_CAJA = 'Caja general (1.1.01.001)';
    var METODOS = [
        { codigo: 'EFECTIVO', etiqueta: 'Efectivo', requiereCuentaBancaria: false },
        { codigo: 'TRANSFERENCIA', etiqueta: 'Transferencia', requiereCuentaBancaria: true },
        { codigo: 'CHEQUE', etiqueta: 'Cheque', requiereCuentaBancaria: true },
        { codigo: 'SINPE', etiqueta: 'SINPE Móvil', requiereCuentaBancaria: true },
        { codigo: 'DEPOSITO', etiqueta: 'Depósito', requiereCuentaBancaria: true }
    ];
    var EXTENSIONES_ADJUNTO = ['xml', 'pdf', 'jpg', 'jpeg', 'png'];
    var TAMANO_MAXIMO_ADJUNTO = 5 * 1024 * 1024;
    var URL_COMPRA = '/api/compras/contado';
    var ETIQUETAS_REGIMEN = {
        GENERAL: 'Régimen general',
        SIMPLIFICADO: 'Régimen simplificado',
        NO_CONTRIBUYENTE: 'No contribuyente',
        OTRO: 'Otro régimen',
        SIN_VERIFICAR: 'Sin verificar (use «Verificar régimen» en el proveedor)'
    };

    function lineas() { return global.CxpLineas; }

    function esVacio(v) {
        return v === null || v === undefined || String(v).trim() === '';
    }

    function copiar(o) {
        var n = {};
        Object.keys(o).forEach(function (k) { n[k] = o[k]; });
        return n;
    }

    function requiereCuentaBancaria(metodo) {
        for (var i = 0; i < METODOS.length; i++) {
            if (METODOS[i].codigo === metodo) return METODOS[i].requiereCuentaBancaria;
        }
        return true;
    }

    /** Id de la cuenta 5.3.01.001 en el listado de cuentas elegibles; '' si no está (no se usa ningún id fijo). */
    function cuentaPorDefecto(cuentas) {
        var lista = cuentas || [];
        for (var i = 0; i < lista.length; i++) {
            if (lista[i].codigo === CODIGO_CUENTA_COMPRA) return lista[i].id;
        }
        return '';
    }

    function nuevaLinea(regla, cuentas) {
        var l = lineas().lineaPorDefecto(regla);
        l.cuentaId = cuentaPorDefecto(cuentas);
        return l;
    }

    function estadoInicial(hoy, cuentas, regla) {
        return {
            proveedor: null,
            numero: '',
            clave: '',
            fecha: hoy || '',
            metodoPago: 'EFECTIVO',
            cuentaBancariaId: '',
            referencia: '',
            lineas: [nuevaLinea(regla, cuentas)]
        };
    }

    function agregarLinea(estado, regla, cuentas) {
        var n = copiar(estado);
        n.lineas = estado.lineas.concat([nuevaLinea(regla, cuentas)]);
        return n;
    }

    /** Quita una línea; la última no se quita (una compra tiene al menos una). */
    function quitarLinea(estado, indice) {
        if (estado.lineas.length <= 1) return estado;
        var n = copiar(estado);
        n.lineas = estado.lineas.filter(function (l, i) { return i !== indice; });
        return n;
    }

    function cambiarLinea(estado, indice, campo, valor, regla) {
        var n = copiar(estado);
        n.lineas = estado.lineas.map(function (l, i) {
            return i === indice ? lineas().cambiarLinea(l, campo, valor, regla) : l;
        });
        return n;
    }

    /** Aplica una regla nueva (cambió la fecha o el proveedor): tratamiento fijo e IVA trasladado 0 si la regla lo exige. */
    function aplicarRegla(estado, regla) {
        var n = copiar(estado);
        n.lineas = lineas().aplicarPolitica(estado.lineas, regla).map(function (l) {
            if (regla && regla.ivaTrasladadoCero && !esVacio(l.iva)) l.iva = '0';
            return l;
        });
        return n;
    }

    function textoTamano(bytes) {
        if (bytes % (1024 * 1024) === 0) return (bytes / (1024 * 1024)) + ' MB';
        if (bytes % 1024 === 0) return (bytes / 1024) + ' KB';
        return bytes + ' bytes';
    }

    /** Espejo de la validación del servidor (que es la que manda): extensión y tamaño. archivo = { name, size } o null. */
    function validarAdjunto(archivo, maximo) {
        if (!archivo) return null;
        var nombre = String(archivo.name || '');
        var punto = nombre.lastIndexOf('.');
        var ext = punto < 0 ? '' : nombre.substring(punto + 1).toLowerCase();
        if (EXTENSIONES_ADJUNTO.indexOf(ext) < 0) return 'El adjunto debe ser un archivo XML, PDF, JPG o PNG';
        var limite = maximo || TAMANO_MAXIMO_ADJUNTO;
        if (archivo.size > limite) return 'El adjunto excede el tamaño máximo permitido de ' + textoTamano(limite);
        return null;
    }

    /** opc: { regla, hoy, adjunto, maxAdjunto }. Devuelve { errores, advertencias }. */
    function validar(estado, opc) {
        opc = opc || {};
        var errores = [];
        if (!estado.proveedor || esVacio(estado.proveedor.id)) errores.push('Seleccione o cree el proveedor');
        var r = lineas().validar({
            numero: estado.numero, fecha: estado.fecha, hoy: opc.hoy, clave: estado.clave,
            lineas: estado.lineas, regla: opc.regla
        });
        errores = errores.concat(r.errores);
        if (opc.regla && opc.regla.ivaTrasladadoCero) {
            estado.lineas.forEach(function (l, i) {
                var iva = esVacio(l.iva) ? 0 : parseFloat(l.iva);
                if (!isNaN(iva) && iva > 0) {
                    errores.push('La línea ' + (i + 1) + ': el proveedor del régimen simplificado no traslada IVA (el IVA trasladado debe ser 0)');
                }
            });
        }
        if (requiereCuentaBancaria(estado.metodoPago) && esVacio(estado.cuentaBancariaId)) {
            errores.push('Seleccione la cuenta bancaria de origen del pago');
        }
        var errAdjunto = validarAdjunto(opc.adjunto, opc.maxAdjunto);
        if (errAdjunto) errores.push(errAdjunto);
        return { errores: errores, advertencias: r.advertencias };
    }

    /** Parte JSON «datos» del envío (CompraContadoDTO). En EFECTIVO el origen es la caja general: no hay cuenta bancaria. */
    function armarDatos(estado) {
        var base = lineas().armarPeticion({
            numero: estado.numero, clave: estado.clave, fecha: estado.fecha, vencimiento: null, lineas: estado.lineas
        });
        var banco = requiereCuentaBancaria(estado.metodoPago) && !esVacio(estado.cuentaBancariaId)
            ? parseInt(estado.cuentaBancariaId, 10) : null;
        return {
            proveedorId: estado.proveedor ? estado.proveedor.id : null,
            numeroComprobanteProveedor: base.numeroComprobanteProveedor,
            claveComprobante: base.claveComprobante,
            fechaComprobante: base.fechaComprobante,
            descripcion: null,
            moneda: 'CRC',
            lineas: base.lineas,
            metodoPago: estado.metodoPago,
            idCuentaBancariaOrigen: banco,
            referencia: esVacio(estado.referencia) ? null : String(estado.referencia).trim()
        };
    }

    /** Un solo multipart: la parte «datos» (JSON) y, si hay, el archivo «adjunto». */
    function armarEnvio(datos, archivo, FormDataCtor, BlobCtor) {
        var fd = new FormDataCtor();
        fd.append('datos', new BlobCtor([JSON.stringify(datos)], { type: 'application/json' }));
        if (archivo) fd.append('adjunto', archivo);
        return fd;
    }

    var MSG_RED = 'No se pudo registrar la compra; intente de nuevo';

    function leer(resp) {
        return Promise.resolve().then(function () { return resp.json(); }).catch(function () { return null; });
    }

    /**
     * Valida y envía. fetchFn(url, opciones) devuelve una promesa de { ok, status, json() } (fetch con el CSRF ya puesto).
     * opc: { regla, hoy, adjunto, archivo, maxAdjunto, FormData, Blob }.
     * Resuelve { ok, status, errores, mensaje, resultado, advertencias }; nunca rechaza.
     */
    function registrar(fetchFn, estado, opc) {
        var v = validar(estado, opc);
        if (v.errores.length) {
            return Promise.resolve({ ok: false, status: 0, errores: v.errores, advertencias: v.advertencias });
        }
        var cuerpo = armarEnvio(armarDatos(estado), opc.archivo, opc.FormData, opc.Blob);
        return Promise.resolve().then(function () {
            return fetchFn(URL_COMPRA, { method: 'POST', body: cuerpo });
        }).then(function (resp) {
            return leer(resp).then(function (json) {
                if (resp.ok) {
                    var datos = json && json.data ? json.data : null;
                    return {
                        ok: true, status: resp.status, errores: [], advertencias: datos && datos.advertencias ? datos.advertencias : [],
                        mensaje: json && json.message ? String(json.message) : '', resultado: datos
                    };
                }
                var msg = resp.status === 403 ? 'No tiene permiso para registrar compras de contado'
                    : (json && json.message ? String(json.message) : MSG_RED);
                return { ok: false, status: resp.status, errores: [msg], advertencias: [] };
            });
        }).catch(function () {
            return { ok: false, status: 0, errores: [MSG_RED], advertencias: [] };
        });
    }

    // ── Proveedor ────────────────────────────────────────────────────────────

    /** Cédula sin guiones ni espacios si tiene entre 9 y 12 dígitos; null si no. */
    function cedulaNormalizada(texto) {
        var c = String(texto === null || texto === undefined ? '' : texto).replace(/[\s-]/g, '');
        return /^\d{9,12}$/.test(c) ? c : null;
    }

    function buscarProveedores(fetchFn, texto) {
        var q = esVacio(texto) ? '' : String(texto).trim();
        if (q.length < 2) return Promise.resolve({ proveedores: [] });
        return Promise.resolve().then(function () {
            return fetchFn('/api/proveedores/buscar?q=' + encodeURIComponent(q));
        }).then(function (resp) {
            return leer(resp).then(function (json) {
                if (resp.ok) return { proveedores: json && json.data ? json.data : [] };
                return { error: resp.status === 403 ? 'No tiene permiso para buscar proveedores' : 'No se pudo buscar proveedores' };
            });
        }).catch(function () { return { error: 'No se pudo buscar proveedores' }; });
    }

    /** Alta mínima con la cédula (el servidor consulta /fe/ae y guarda el régimen; si no responde queda SIN_VERIFICAR). */
    function crearProveedor(fetchFn, cedulaTexto, nombre) {
        var cedula = cedulaNormalizada(cedulaTexto);
        if (!cedula) return Promise.resolve({ error: 'La cédula debe tener entre 9 y 12 dígitos' });
        if (esVacio(nombre)) return Promise.resolve({ error: 'El nombre del proveedor es obligatorio' });
        var cuerpo = { cedula: cedula, nombre: String(nombre).trim(), tipoProveedor: 'NACIONAL', moneda: 'CRC' };
        return Promise.resolve().then(function () {
            return fetchFn('/api/proveedores', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(cuerpo)
            });
        }).then(function (resp) {
            return leer(resp).then(function (json) {
                if (resp.ok && json && json.data) return { proveedor: json.data };
                if (resp.status === 403) return { error: 'No tiene permiso para crear proveedores' };
                return { error: json && json.message ? String(json.message) : 'No se pudo crear el proveedor' };
            });
        }).catch(function () { return { error: 'No se pudo crear el proveedor' }; });
    }

    // ── Pintado (solo createElement y textContent) ───────────────────────────

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

    function etiquetaRegimen(codigo) {
        return ETIQUETAS_REGIMEN[codigo] || String(codigo || '');
    }

    function renderConfirmacion(doc, contenedor, resultado) {
        limpiar(contenedor);
        var caja = el(doc, 'div', 'alert alert-success py-2 px-3 mb-2',
            'Compra de contado registrada: ' + resultado.numeroCuentaPorPagar);
        if (resultado.numeroPago) caja.appendChild(el(doc, 'div', 'small', 'Pago ' + resultado.numeroPago + ' por el total'));
        if (resultado.tieneAdjunto) caja.appendChild(el(doc, 'div', 'small', 'Comprobante adjunto guardado'));
        contenedor.appendChild(caja);
        (resultado.advertencias || []).forEach(function (m) {
            contenedor.appendChild(el(doc, 'div', 'alert alert-warning py-1 px-2 mb-1 small', m));
        });
    }

    function renderErrores(doc, contenedor, errores) {
        limpiar(contenedor);
        (errores || []).forEach(function (m) {
            contenedor.appendChild(el(doc, 'div', 'alert alert-danger py-1 px-2 mb-1 small', m));
        });
    }

    /** Resultados de la búsqueda; sin resultados y con texto de cédula ofrece crear el proveedor con esa cédula. */
    function renderProveedores(doc, contenedor, lista, textoBuscado, onElegir, onCrear) {
        limpiar(contenedor);
        (lista || []).forEach(function (p) {
            var b = el(doc, 'button', 'list-group-item list-group-item-action',
                p.nombre + ' · ' + p.cedula + ' · ' + etiquetaRegimen(p.regimenHacienda));
            b.setAttribute('type', 'button');
            b.addEventListener('click', function () { if (onElegir) onElegir(p); });
            contenedor.appendChild(b);
        });
        if (lista && lista.length) return;
        var cedula = cedulaNormalizada(textoBuscado);
        if (!cedula) {
            contenedor.appendChild(el(doc, 'div', 'text-muted small', 'No hay proveedores con ese nombre o cédula'));
            return;
        }
        contenedor.appendChild(el(doc, 'div', 'text-muted small mb-1', 'No hay un proveedor con la cédula ' + cedula));
        var crear = el(doc, 'button', 'btn btn-sm btn-outline-primary', 'Crear proveedor con la cédula ' + cedula);
        crear.setAttribute('type', 'button');
        crear.addEventListener('click', function () { if (onCrear) onCrear(cedula); });
        contenedor.appendChild(crear);
    }

    function renderProveedorElegido(doc, contenedor, proveedor) {
        limpiar(contenedor);
        if (!proveedor) return;
        contenedor.appendChild(el(doc, 'div', 'fw-semibold', proveedor.nombre));
        contenedor.appendChild(el(doc, 'div', 'small text-muted', 'Cédula ' + proveedor.cedula));
        contenedor.appendChild(el(doc, 'span', 'badge bg-secondary', etiquetaRegimen(proveedor.regimenHacienda)));
    }

    global.CompraContado = {
        CODIGO_CUENTA_COMPRA: CODIGO_CUENTA_COMPRA,
        ETIQUETA_CAJA: ETIQUETA_CAJA,
        METODOS: METODOS,
        requiereCuentaBancaria: requiereCuentaBancaria,
        cuentaPorDefecto: cuentaPorDefecto,
        nuevaLinea: nuevaLinea,
        estadoInicial: estadoInicial,
        agregarLinea: agregarLinea,
        quitarLinea: quitarLinea,
        cambiarLinea: cambiarLinea,
        aplicarRegla: aplicarRegla,
        validarAdjunto: validarAdjunto,
        validar: validar,
        armarDatos: armarDatos,
        armarEnvio: armarEnvio,
        registrar: registrar,
        cedulaNormalizada: cedulaNormalizada,
        buscarProveedores: buscarProveedores,
        crearProveedor: crearProveedor,
        etiquetaRegimen: etiquetaRegimen,
        renderConfirmacion: renderConfirmacion,
        renderErrores: renderErrores,
        renderProveedores: renderProveedores,
        renderProveedorElegido: renderProveedorElegido
    };
})(window);
