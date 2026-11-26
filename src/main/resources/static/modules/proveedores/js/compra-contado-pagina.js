/*
 * T-058 (ADR-077 §1-2, §7-9): cableado de la pantalla «Compra de contado». Toda la lógica vive en window.CompraContado (con
 * pruebas en node); aquí solo se conectan los campos, se piden los datos al servidor y se pinta con createElement y
 * textContent: ningún dato del servidor se interpreta como HTML. El CSRF lo agrega el fetch envuelto de common.js.
 */
(function () {
    'use strict';

    var CC = window.CompraContado;
    var CL = window.CxpLineas;
    var form = document.getElementById('formCompraContado');
    if (!form || !CC || !CL) return;

    var hoy = form.getAttribute('data-hoy') || '';
    var maxAdjunto = parseInt(form.getAttribute('data-adjunto-max'), 10) || undefined;
    var cuentas = [];
    var regla = null;
    var estado = CC.estadoInicial(hoy, cuentas, null);
    var consultaPolitica = 0;
    var temporizadorBusqueda = null;
    var cedulaPorCrear = null;

    function porId(id) { return document.getElementById(id); }
    function texto(id, valor) { porId(id).textContent = valor; }
    function actualizar(cambios) { estado = Object.assign({}, estado, cambios); }
    function archivoElegido() {
        var f = porId('ccAdjunto').files;
        return f && f.length ? f[0] : null;
    }

    // ── Líneas ───────────────────────────────────────────────────────────────
    var manejadores = {
        cambio: function (i, campo, valor) {
            estado = CC.cambiarLinea(estado, i, campo, valor, regla);
            var fila = document.querySelector('#ccLineas tr[data-fila="' + i + '"]');
            if (fila) {
                var iva = fila.querySelector('[data-campo="iva"]');
                if (iva && campo !== 'iva') iva.value = estado.lineas[i].iva;
            }
            pintarTotal();
        },
        quitar: function (i) {
            estado = CC.quitarLinea(estado, i);
            pintarLineas();
        }
    };

    function pintarTotal() {
        var t = CL.totales(estado.lineas, null);
        texto('ccTotal', 'Total a pagar: CRC ' + (t && t.total !== undefined ? Number(t.total).toFixed(2) : '0.00'));
    }

    function pintarLineas() {
        CL.renderLineas(document, porId('ccLineas'), estado.lineas, cuentas, manejadores, null, regla);
        pintarTotal();
    }

    // ── Pago: método y origen (caja general por defecto con EFECTIVO) ───────
    function opcion(valor, etiqueta) {
        var o = document.createElement('option');
        o.value = valor;
        o.textContent = etiqueta;
        return o;
    }

    function pintarMetodos() {
        var sel = porId('ccMetodo');
        sel.replaceChildren();
        CC.METODOS.forEach(function (m) { sel.appendChild(opcion(m.codigo, m.etiqueta)); });
        sel.value = estado.metodoPago;
    }

    function pintarOrigen(cuentasBanco, mensaje) {
        var sel = porId('ccOrigen');
        sel.replaceChildren();
        if (!CC.requiereCuentaBancaria(estado.metodoPago)) {
            sel.appendChild(opcion('', CC.ETIQUETA_CAJA));
            sel.disabled = true;
            texto('ccOrigenAyuda', 'El efectivo sale de la caja general.');
            return;
        }
        sel.disabled = false;
        sel.appendChild(opcion('', 'Seleccione la cuenta bancaria...'));
        (cuentasBanco || []).forEach(function (c) {
            sel.appendChild(opcion(c.idCuentaBancaria,
                c.entidad + ' (' + c.moneda + ')' + (c.cuentaBanco ? ' - ' + c.cuentaBanco : '')));
        });
        sel.value = estado.cuentaBancariaId;
        texto('ccOrigenAyuda', mensaje || ((cuentasBanco || []).length
            ? 'Cuenta de la empresa de la que sale el pago.'
            : 'No hay cuentas bancarias activas en CRC. Configúrelas en Configuración → Empresa.'));
    }

    function cargarCuentasBanco() {
        if (!CC.requiereCuentaBancaria(estado.metodoPago)) {
            pintarOrigen([]);
            return;
        }
        fetch('/api/cuentas-bancarias/activas?moneda=CRC')
            .then(function (r) { return r.json().then(function (j) { return { ok: r.ok, json: j }; }); })
            .then(function (r) {
                if (r.ok && r.json && Array.isArray(r.json.data)) pintarOrigen(r.json.data);
                else pintarOrigen([], (r.json && r.json.message) || 'No se pudieron cargar las cuentas bancarias');
            })
            .catch(function () { pintarOrigen([], 'No se pudieron cargar las cuentas bancarias'); });
    }

    // ── Política de IVA de la fecha y del proveedor (la decide el servidor) ──
    function actualizarPolitica() {
        var consulta = ++consultaPolitica;
        var proveedorId = estado.proveedor ? estado.proveedor.id : '';
        CL.consultarPolitica(fetch.bind(window), estado.fecha, proveedorId).then(function (r) {
            if (consulta !== consultaPolitica) return;
            regla = r && !r.error ? r : null;
            CL.renderPolitica(document, porId('ccPoliticaMensaje'), r);
            estado = CC.aplicarRegla(estado, regla);
            pintarLineas();
        });
    }

    // ── Proveedor ────────────────────────────────────────────────────────────
    function mensajeProveedor(msg, clase) {
        var c = porId('ccMensajeProveedor');
        c.replaceChildren();
        if (!msg) return;
        var d = document.createElement('div');
        d.className = clase || 'alert alert-danger py-1 px-2 small';
        d.textContent = msg;
        c.appendChild(d);
    }

    function elegirProveedor(p) {
        actualizar({ proveedor: p });
        porId('ccResultadosProveedor').replaceChildren();
        porId('ccNuevoProveedor').style.display = 'none';
        cedulaPorCrear = null;
        CC.renderProveedorElegido(document, porId('ccProveedorElegido'), p);
        actualizarPolitica();
    }

    function buscar() {
        var q = porId('ccBuscarProveedor').value;
        mensajeProveedor('');
        porId('ccNuevoProveedor').style.display = 'none';
        CC.buscarProveedores(fetch.bind(window), q).then(function (r) {
            if (r.error) {
                mensajeProveedor(r.error);
                return;
            }
            if (String(q).trim().length < 2) {
                porId('ccResultadosProveedor').replaceChildren();
                return;
            }
            CC.renderProveedores(document, porId('ccResultadosProveedor'), r.proveedores, q, elegirProveedor, function (cedula) {
                cedulaPorCrear = cedula;
                texto('ccNuevoCedula', 'Se creará con la cédula ' + cedula + ' y se consultará su régimen en Hacienda.');
                porId('ccNuevoProveedor').style.display = '';
                porId('ccNuevoNombre').focus();
            });
        });
    }

    function crearProveedor() {
        var boton = porId('ccBtnCrearProveedor');
        boton.disabled = true;
        CC.crearProveedor(fetch.bind(window), cedulaPorCrear, porId('ccNuevoNombre').value).then(function (r) {
            boton.disabled = false;
            if (r.error) {
                mensajeProveedor(r.error);
                return;
            }
            mensajeProveedor('Proveedor creado: ' + r.proveedor.nombre, 'alert alert-success py-1 px-2 small');
            porId('ccNuevoNombre').value = '';
            elegirProveedor(r.proveedor);
        });
    }

    // ── Envío ────────────────────────────────────────────────────────────────
    function reiniciar() {
        regla = null;
        estado = CC.estadoInicial(hoy, cuentas, null);
        porId('ccNumero').value = '';
        porId('ccClave').value = '';
        porId('ccFecha').value = hoy;
        porId('ccReferencia').value = '';
        porId('ccAdjunto').value = '';
        porId('ccBuscarProveedor').value = '';
        CC.renderProveedorElegido(document, porId('ccProveedorElegido'), null);
        pintarMetodos();
        pintarOrigen([]);
        CL.renderPolitica(document, porId('ccPoliticaMensaje'), null);
        pintarLineas();
    }

    function enviar(evento) {
        evento.preventDefault();
        var boton = porId('ccBtnRegistrar');
        var adjunto = archivoElegido();
        boton.disabled = true;
        porId('ccConfirmacion').replaceChildren();
        CC.registrar(fetch.bind(window), estado, {
            regla: regla, hoy: hoy, adjunto: adjunto, archivo: adjunto, maxAdjunto: maxAdjunto, FormData: FormData, Blob: Blob
        }).then(function (r) {
            boton.disabled = false;
            CL.renderMensajes(document, porId('ccMensajes'), r);
            if (r.ok) {
                CC.renderConfirmacion(document, porId('ccConfirmacion'), r.resultado || {});
                reiniciar();
            }
        });
    }

    // ── Arranque ─────────────────────────────────────────────────────────────
    porId('ccNumero').addEventListener('input', function () { actualizar({ numero: porId('ccNumero').value }); });
    porId('ccClave').addEventListener('input', function () { actualizar({ clave: porId('ccClave').value }); });
    porId('ccReferencia').addEventListener('input', function () { actualizar({ referencia: porId('ccReferencia').value }); });
    porId('ccFecha').addEventListener('change', function () {
        actualizar({ fecha: porId('ccFecha').value });
        actualizarPolitica();
    });
    porId('ccMetodo').addEventListener('change', function () {
        actualizar({ metodoPago: porId('ccMetodo').value, cuentaBancariaId: '' });
        cargarCuentasBanco();
    });
    porId('ccOrigen').addEventListener('change', function () { actualizar({ cuentaBancariaId: porId('ccOrigen').value }); });
    porId('ccBtnAgregarLinea').addEventListener('click', function () {
        estado = CC.agregarLinea(estado, regla, cuentas);
        pintarLineas();
    });
    porId('ccBuscarProveedor').addEventListener('input', function () {
        clearTimeout(temporizadorBusqueda);
        temporizadorBusqueda = setTimeout(buscar, 300);
    });
    porId('ccBtnCrearProveedor').addEventListener('click', crearProveedor);
    form.addEventListener('submit', enviar);

    porId('ccFecha').value = hoy;
    pintarMetodos();
    pintarOrigen([]);
    pintarLineas();
    actualizarPolitica();
    // cuentas elegibles como cuenta de una línea; la 5.3.01.001 se resuelve por código, no por id
    fetch('/api/cuentas-pagar/cuentas-contrapartida')
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (j) {
            cuentas = (j && j.data && j.data.cuentas) || [];
            estado = Object.assign({}, estado, { lineas: [CC.nuevaLinea(regla, cuentas)] });
            pintarLineas();
        })
        .catch(function () {
            CL.renderMensajes(document, porId('ccMensajes'), { errores: ['No se pudieron cargar las cuentas de las líneas'] });
        });
})();
