// T-049 (B2, ADR-053/058): devolución en dinero del saldo a favor. Validación de IBAN y montos, acciones por estado y
// permisos, formularios con error del servidor sin cerrarse, textContent y el panel B1 con «disponible».
//   node --test src/test/js/*.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const JS = path.join(raiz, 'src/main/resources/static/modules/facturacion/js');
const TEMPLATES = path.join(raiz, 'src/main/resources/templates');
const MAL = '<img src=x onerror=alert(1)>';

function cargar() {
    const ctx = { window: {}, console, Number, Math, Date };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(path.join(JS, 'nota-credito.js'), 'utf8'), ctx);
    vm.runInContext(fs.readFileSync(path.join(JS, 'saldo-favor.js'), 'utf8'), ctx);
    return ctx.window.SaldoFavor;
}

function crear(tag) {
    const e = {
        tag, hijos: [], attrs: {}, className: '', listeners: {}, _texto: '', value: '', disabled: false, checked: false,
        style: {},
        appendChild(h) { this.hijos.push(h); return h; },
        setAttribute(k, v) { this.attrs[k] = String(v); },
        getAttribute(k) { return this.attrs[k] ?? null; },
        addEventListener(ev, fn) { this.listeners[ev] = fn; },
        get textContent() { return this._texto; },
        set textContent(v) { this._texto = String(v); this.hijos = []; }
    };
    for (const prop of ['innerHTML', 'outerHTML', 'insertAdjacentHTML']) {
        Object.defineProperty(e, prop, { set() { throw new Error(prop + ' asignado'); }, get() { return ''; } });
    }
    return e;
}
const textoDe = (e) => e._texto + e.hijos.map(textoDe).join('|');
const recorrer = (e, f) => { f(e); e.hijos.forEach((h) => recorrer(h, f)); };
const buscar = (e, attr, valor) => { let r = null; recorrer(e, (x) => { if (!r && x.attrs[attr] === valor) r = x; }); return r; };
const acciones = (e) => { const r = []; recorrer(e, (x) => { if (x.attrs['data-accion'] && x.tag === 'button') r.push(x.attrs['data-accion']); }); return r; };
const documento = () => ({ createElement: crear, getElementById: () => null, querySelector: () => null });
const flush = () => new Promise((r) => setImmediate(r));

/** IBAN costarricense válido (CR + 2 dígitos de control + 18 dígitos) calculado con mod 97. */
function ibanValido(cuerpo18) {
    const pasar = (t) => t.replace(/[A-Z]/g, (c) => String(c.charCodeAt(0) - 55));
    const num = BigInt(pasar(cuerpo18 + 'CR00'));
    const control = String(98n - (num % 97n)).padStart(2, '0');
    return 'CR' + control + cuerpo18;
}
const IBAN = ibanValido('015201001026284066');

const SALDO = (extra = {}) => ({ id: 5, idCliente: 1, moneda: 'CRC', montoOriginal: 1000, saldoVivo: 600, monto_reservado: 0,
    reservado: 100, disponible: 500, fechaOrigen: '2026-11-05', numeroNota: 'NC-00007', estado: 'VIGENTE', ...extra });
const DEV = (estado, extra = {}) => ({ id: 11, idSaldoFavor: 5, moneda: 'CRC', monto: 100, estado, medioSolicitud: 'CORREO',
    fechaSolicitud: '2026-11-10', iban: IBAN, nombreTitular: 'Ana Pérez', identificacionTitular: '1-1111-1111',
    solicitadoPorNombre: 'María', fechaCreacion: '2026-11-10T09:00:00', ...extra });
const TODOS = { pagoCrear: true, pagoAnular: true, devolucionAprobar: true };
const NADA = { pagoCrear: false, pagoAnular: false, devolucionAprobar: false };

// ---- IBAN ----

test('IBAN: formato CR + 20 dígitos y dígito de control mod 97', () => {
    const s = cargar();
    assert.equal(s.validarIban(IBAN).valido, true);
    assert.equal(s.validarIban(' ' + IBAN.toLowerCase().replace(/(.{4})/g, '$1 ') + ' ').valido, true);
    assert.equal(s.validarIban(IBAN).normalizado, IBAN);
    assert.equal(s.validarIban('').valido, false);
    assert.match(s.validarIban('ES9121000418450200051332').mensaje, /CR/);
    assert.match(s.validarIban(IBAN.slice(0, -1)).mensaje, /CR/);
    assert.match(s.validarIban(IBAN + '0').mensaje, /CR/);
    assert.match(s.validarIban('CR05A15201001026284066').mensaje, /CR/);
    const roto = IBAN.slice(0, 2) + (IBAN[2] === '9' ? '0' : String(Number(IBAN[2]) + 1)) + IBAN.slice(3);
    const v = s.validarIban(roto);
    assert.equal(v.valido, false);
    assert.match(v.mensaje, /dígito de control/);
    const otro = ibanValido('000000000000000001');
    assert.equal(s.validarIban(otro).valido, true);
});

// ---- montos y campos ----

test('monto de la devolución: > 0, dos decimales y tope en el disponible', () => {
    const s = cargar();
    const base = 'El monto debe ser mayor a 0 y tener a lo sumo dos decimales';
    assert.equal(s.validarMontoDevolucion('', 500).mensaje, base);
    assert.equal(s.validarMontoDevolucion('0', 500).mensaje, base);
    assert.equal(s.validarMontoDevolucion('1.234', 500).mensaje, base);
    assert.equal(s.validarMontoDevolucion('500.01', 500).mensaje, 'El monto excede el saldo disponible del saldo a favor');
    assert.equal(s.validarMontoDevolucion('500', 500).valido, true);
    assert.equal(s.validarMontoDevolucion('0.01', 500).valido, true);
});

test('solicitud: titular 3 a 100, identificación, medio, fecha no futura; errores por campo', () => {
    const s = cargar();
    const ok = { monto: '100', iban: IBAN, nombreTitular: 'Ana', identificacionTitular: '1', medioSolicitud: 'CORREO',
        fechaSolicitud: '2026-11-10' };
    assert.deepEqual({ ...s.validarSolicitud(ok, 500, '2026-11-20') }, {});
    const e = s.validarSolicitud({ monto: '0', iban: 'x', nombreTitular: 'Al', identificacionTitular: ' ', medioSolicitud: '',
        fechaSolicitud: '2026-11-21' }, 500, '2026-11-20');
    assert.deepEqual(Object.keys(e).sort(), ['fechaSolicitud', 'iban', 'identificacionTitular', 'medioSolicitud', 'monto', 'nombreTitular']);
    assert.ok(s.validarSolicitud({ ...ok, nombreTitular: 'x'.repeat(101) }, 500, '2026-11-20').nombreTitular);
    assert.ok(s.validarSolicitud({ ...ok, medioSolicitud: 'PALOMA' }, 500, '2026-11-20').medioSolicitud);
    assert.ok(s.validarSolicitud({ ...ok, fechaSolicitud: '' }, 500, '2026-11-20').fechaSolicitud);
});

test('pago: cuenta, número y fecha del comprobante no futura; razón de 3 a 180', () => {
    const s = cargar();
    assert.deepEqual({ ...s.validarPago({ idCuentaBancaria: '3', numeroComprobante: 'C-1', fechaComprobante: '2026-11-20' }, '2026-11-20') }, {});
    const e = s.validarPago({ idCuentaBancaria: '', numeroComprobante: ' ', fechaComprobante: '2026-11-21' }, '2026-11-20');
    assert.deepEqual(Object.keys(e).sort(), ['fechaComprobante', 'idCuentaBancaria', 'numeroComprobante']);
    assert.equal(s.validarRazon('ab').valido, false);
    assert.equal(s.validarRazon('abc').valido, true);
    assert.equal(s.validarRazon('x'.repeat(181)).valido, false);
});

// ---- acciones por estado y permisos ----

test('acciones por estado y permisos', () => {
    const s = cargar();
    const a = (estado, p) => [...s.accionesDevolucion(estado, p)].sort();
    assert.deepEqual(a('SOLICITADA', TODOS), ['anular', 'aprobar', 'rechazar']);
    assert.deepEqual(a('APROBADA', TODOS), ['anular', 'pagar']);
    for (const e of ['PAGADA', 'RECHAZADA', 'ANULADA']) assert.deepEqual(a(e, TODOS), []);
    assert.deepEqual(a('SOLICITADA', NADA), []);
    assert.deepEqual(a('APROBADA', NADA), []);
    assert.deepEqual(a('SOLICITADA', { ...NADA, pagoAnular: true }), ['anular']);
    assert.deepEqual(a('APROBADA', { ...NADA, pagoAnular: true }), ['anular']);
    assert.deepEqual(a('SOLICITADA', { ...NADA, devolucionAprobar: true }), ['anular', 'aprobar', 'rechazar']);
    assert.deepEqual(a('SOLICITADA', { ...NADA, pagoCrear: true }), []);
    assert.equal(s.puedeSolicitar(NADA), false);
    assert.equal(s.puedeSolicitar({ ...NADA, pagoCrear: true }), true);
    assert.equal(s.puedeSolicitar({ ...NADA, devolucionAprobar: true }), true);
});

test('estado de cuenta: disponible, reservado, devoluciones con historial y botones según estado y permisos', () => {
    const s = cargar();
    const cont = crear('div');
    const items = [{ saldo: SALDO(), aplicaciones: [], devoluciones: [
        DEV('SOLICITADA', { id: 1 }),
        DEV('APROBADA', { id: 2, aprobadoPorNombre: 'Luis', fechaAprobacion: '2026-11-11T10:00:00', verificoCuenta: true,
            fechaVerificacion: '2026-11-11T10:00:00' }),
        DEV('PAGADA', { id: 3, pagadoPorNombre: 'Eva', fechaPago: '2026-11-12T10:00:00', cuentaBancaria: 'BAC CR99',
            numeroComprobante: 'CMP-9', fechaComprobante: '2026-11-12', numeroAsiento: 'AS-77' }),
        DEV('RECHAZADA', { id: 4, rechazadoPorNombre: 'Luis', fechaRechazo: '2026-11-11T10:00:00', razon: MAL }),
        DEV('ANULADA', { id: 5, anuladoPorNombre: 'Eva', fechaAnulacion: '2026-11-11T10:00:00', razon: 'Error del cliente' })] }];
    s.pintarEstadoCuenta(documento(), cont, items, { permisos: TODOS, hoy: '2026-11-20', fetchFn: async () => ({}) });
    const t = textoDe(cont);
    assert.match(t, /Reservado ₡100\.00/);
    assert.match(t, /Disponible ₡500\.00/);
    assert.match(t, /Devoluciones/);
    for (const x of ['Solicitada', 'Aprobada', 'Pagada', 'Rechazada', 'Anulada', 'María', 'Luis', 'Eva', 'CMP-9', 'AS-77', 'BAC CR99',
        'Error del cliente']) assert.ok(t.includes(x), x);
    assert.ok(t.includes(MAL));
    assert.match(t, /verific/i);
    const a = acciones(cont);
    assert.equal(a.filter((x) => x === 'aprobar').length, 1);
    assert.equal(a.filter((x) => x === 'rechazar').length, 1);
    assert.equal(a.filter((x) => x === 'pagar').length, 1);
    assert.equal(a.filter((x) => x === 'anular').length, 2);
    assert.equal(a.filter((x) => x === 'solicitar').length, 1);

    const sin = crear('div');
    s.pintarEstadoCuenta(documento(), sin, items, { permisos: NADA, hoy: '2026-11-20' });
    assert.deepEqual(acciones(sin), []);
    const solo = crear('div');
    s.pintarEstadoCuenta(documento(), solo, items, { permisos: { ...NADA, pagoCrear: true }, hoy: '2026-11-20' });
    assert.deepEqual(acciones(solo), ['solicitar']);
});

test('«Solicitar» solo con saldo VIGENTE y disponible mayor a 0; el USD muestra tipo de cambio y diferencial', () => {
    const s = cargar();
    const f = (saldo) => { const c = crear('div'); s.pintarEstadoCuenta(documento(), c, [{ saldo, aplicaciones: [], devoluciones: [] }],
        { permisos: TODOS, hoy: '2026-11-20' }); return acciones(c); };
    assert.deepEqual(f(SALDO()), ['solicitar']);
    assert.deepEqual(f(SALDO({ disponible: 0, reservado: 600 })), []);
    assert.deepEqual(f(SALDO({ estado: 'AGOTADO', disponible: 0, saldoVivo: 0 })), []);
    const c = crear('div');
    s.pintarEstadoCuenta(documento(), c, [{ saldo: SALDO({ moneda: 'USD' }), aplicaciones: [], devoluciones: [
        DEV('PAGADA', { moneda: 'USD', tcDiaValor: 510.25, tcDiaFecha: '2026-11-12', diferencialNeto: -1.5 })] }],
    { permisos: NADA, hoy: '2026-11-20' });
    assert.match(textoDe(c), /TC del día 510\.25/);
    assert.match(textoDe(c), /diferencial neto/);
});

// ---- formularios ----

function armar(devs, saldo = SALDO(), respuestas = []) {
    const llamadas = [];
    const fetchFn = async (url, init) => {
        llamadas.push({ url, init });
        const r = respuestas.shift() || { ok: true, cuerpo: { data: {} } };
        return { ok: r.ok, json: async () => r.cuerpo };
    };
    const cont = crear('div');
    let cambios = 0;
    cargar().pintarEstadoCuenta(documento(), cont, [{ saldo, aplicaciones: [], devoluciones: devs }],
        { permisos: TODOS, hoy: '2026-11-20', fetchFn, onCambio: () => { cambios++; } });
    return { cont, llamadas, cambios: () => cambios };
}
const boton = (cont, accion) => buscar(cont, 'data-accion', accion);
const campo = (cont, n) => buscar(cont, 'data-campo', n);
const errorDe = (cont, n) => buscar(cont, 'data-error', n);
const escribir = (c, v) => { c.value = v; c.listeners.input && c.listeners.input(); c.listeners.change && c.listeners.change(); };

test('solicitar: validación en vivo del IBAN y del monto, envío y recarga', async () => {
    const t = armar([]);
    boton(t.cont, 'solicitar').listeners.click();
    const enviar = boton(t.cont, 'enviar');
    assert.equal(enviar.disabled, true);
    assert.equal(campo(t.cont, 'monto').value, '500.00');            // propone el disponible
    escribir(campo(t.cont, 'iban'), 'CR12');
    assert.match(errorDe(t.cont, 'iban')._texto, /CR/);
    escribir(campo(t.cont, 'monto'), '500.01');
    assert.match(errorDe(t.cont, 'monto')._texto, /disponible/);
    escribir(campo(t.cont, 'nombreTitular'), 'Ana Pérez');
    escribir(campo(t.cont, 'identificacionTitular'), '1-1111-1111');
    escribir(campo(t.cont, 'medioSolicitud'), 'CORREO');
    escribir(campo(t.cont, 'fechaSolicitud'), '2026-11-10');
    assert.equal(enviar.disabled, true);
    escribir(campo(t.cont, 'iban'), IBAN);
    assert.equal(errorDe(t.cont, 'iban')._texto, '');
    escribir(campo(t.cont, 'monto'), '120.50');
    assert.equal(enviar.disabled, false);
    await enviar.listeners.click();
    assert.equal(t.llamadas[0].url, '/api/facturas/saldos-favor/5/devoluciones');
    assert.equal(t.llamadas[0].init.method, 'POST');
    assert.deepEqual(JSON.parse(t.llamadas[0].init.body), { monto: 120.5, iban: IBAN, nombreTitular: 'Ana Pérez',
        identificacionTitular: '1-1111-1111', medioSolicitud: 'CORREO', fechaSolicitud: '2026-11-10' });
    assert.equal(t.cambios(), 1);
});

test('el error del servidor (409) se muestra en el formulario sin cerrarlo ni recargar', async () => {
    const t = armar([], SALDO(), [{ ok: false, cuerpo: { success: false, message: MAL } }]);
    boton(t.cont, 'solicitar').listeners.click();
    escribir(campo(t.cont, 'iban'), IBAN);
    escribir(campo(t.cont, 'nombreTitular'), 'Ana Pérez');
    escribir(campo(t.cont, 'identificacionTitular'), '1');
    escribir(campo(t.cont, 'medioSolicitud'), 'CARTA');
    escribir(campo(t.cont, 'fechaSolicitud'), '2026-11-10');
    const enviar = boton(t.cont, 'enviar');
    await enviar.listeners.click();
    const err = buscar(t.cont, 'data-error-form', '1');
    assert.equal(err._texto, MAL);
    assert.equal(err.style.display, '');
    assert.ok(boton(t.cont, 'enviar'));                                 // el formulario sigue ahí
    assert.equal(t.cambios(), 0);
    assert.equal(enviar.disabled, false);
});

test('aprobar exige la declaración de verificación; rechazar y anular exigen razón de 3 a 180', async () => {
    let t = armar([DEV('SOLICITADA')]);
    boton(t.cont, 'aprobar').listeners.click();
    const ok = boton(t.cont, 'enviar');
    assert.equal(ok.disabled, true);
    assert.match(textoDe(t.cont), /Verifiqué que la cuenta pertenece al cliente/);
    const chk = campo(t.cont, 'verificoCuenta');
    chk.checked = true; chk.listeners.change();
    assert.equal(ok.disabled, false);
    await ok.listeners.click();
    assert.equal(t.llamadas[0].url, '/api/facturas/saldos-favor/devoluciones/11/aprobar');
    assert.deepEqual(JSON.parse(t.llamadas[0].init.body), { verificoCuenta: true });
    assert.equal(t.cambios(), 1);

    t = armar([DEV('SOLICITADA')]);
    boton(t.cont, 'rechazar').listeners.click();
    escribir(campo(t.cont, 'razon'), 'ab');
    assert.equal(boton(t.cont, 'enviar').disabled, true);
    assert.match(errorDe(t.cont, 'razon')._texto, /3 y 180/);
    escribir(campo(t.cont, 'razon'), '  Cuenta errónea ');
    await boton(t.cont, 'enviar').listeners.click();
    assert.equal(t.llamadas[0].url, '/api/facturas/saldos-favor/devoluciones/11/rechazar');
    assert.deepEqual(JSON.parse(t.llamadas[0].init.body), { razon: 'Cuenta errónea' });

    t = armar([DEV('APROBADA')]);
    boton(t.cont, 'anular').listeners.click();
    escribir(campo(t.cont, 'razon'), 'Cliente desistió');
    await boton(t.cont, 'enviar').listeners.click();
    assert.equal(t.llamadas[0].url, '/api/facturas/saldos-favor/devoluciones/11/anular');
});

test('pagar: carga las cuentas de la moneda del saldo, valida el comprobante y envía', async () => {
    const cuentas = { ok: true, cuerpo: { data: [{ idCuentaBancaria: 3, entidad: MAL, moneda: 'USD', cuentaBanco: '123', cuentaIban: 'CR1' }] } };
    const t = armar([DEV('APROBADA', { moneda: 'USD' })], SALDO({ moneda: 'USD' }), [cuentas, { ok: true, cuerpo: { data: {} } }]);
    boton(t.cont, 'pagar').listeners.click();
    await flush();
    assert.equal(t.llamadas[0].url, '/api/cuentas-bancarias/activas?moneda=USD');
    const sel = campo(t.cont, 'idCuentaBancaria');
    assert.equal(sel.hijos.length, 2);
    assert.equal(sel.hijos[1].value, '3');
    assert.ok(sel.hijos[1]._texto.includes(MAL));
    escribir(sel, '3');
    escribir(campo(t.cont, 'numeroComprobante'), 'CMP-1');
    escribir(campo(t.cont, 'fechaComprobante'), '2026-11-21');
    assert.match(errorDe(t.cont, 'fechaComprobante')._texto, /futura/);
    assert.equal(boton(t.cont, 'enviar').disabled, true);
    escribir(campo(t.cont, 'fechaComprobante'), '2026-11-20');
    await boton(t.cont, 'enviar').listeners.click();
    assert.equal(t.llamadas[1].url, '/api/facturas/saldos-favor/devoluciones/11/pagar');
    assert.deepEqual(JSON.parse(t.llamadas[1].init.body), { idCuentaBancaria: 3, numeroComprobante: 'CMP-1', fechaComprobante: '2026-11-20' });
    assert.equal(t.cambios(), 1);
});

test('pagar sin cuentas activas de la moneda avisa y no permite enviar', async () => {
    const t = armar([DEV('APROBADA')], SALDO(), [{ ok: true, cuerpo: { data: [] } }]);
    boton(t.cont, 'pagar').listeners.click();
    await flush();
    assert.match(buscar(t.cont, 'data-error-form', '1')._texto, /cuenta bancaria activa/);
    assert.equal(boton(t.cont, 'enviar').disabled, true);
});

// ---- panel B1 con «disponible» y fuentes ----

test('el panel «Aplicar saldo a favor» usa el disponible (no el saldo vivo) para texto, máximo y tope', () => {
    const s = cargar();
    const saldo = SALDO({ saldoVivo: 600, reservado: 200, disponible: 400 });
    assert.equal(s.textoOpcion(saldo), 'NC-00007 · 05/11/2026 · disponible ₡400.00');
    assert.equal(s.montoMaximo(s.disponibleDe(saldo), 900), '400.00');
    assert.equal(s.validarMonto('400.01', s.disponibleDe(saldo), 900).mensaje, 'El monto excede el saldo disponible del saldo a favor');
    assert.equal(s.disponibleDe({ saldoVivo: 600 }), 600);
    assert.equal(s.disponibleDe({ saldoVivo: 600, reservado: 150 }), 450);
});

test('el fuente no usa innerHTML y la plantilla declara los permisos y el contexto', () => {
    const codigo = fs.readFileSync(path.join(JS, 'saldo-favor.js'), 'utf8');
    assert.doesNotMatch(codigo, /\.(innerHTML|outerHTML)\s*[+\-]?=/);
    const html = fs.readFileSync(path.join(TEMPLATES, 'modules/facturacion/pagos/estado-cuenta-cliente.html'), 'utf8');
    assert.match(html, /id="perm-pago-crear"[^>]*sec:authorize="[^"]*PAGO_CREAR/);
    assert.match(html, /id="perm-pago-anular"[^>]*sec:authorize="[^"]*PAGO_ANULAR/);
    assert.match(html, /id="perm-devolucion-aprobar"[^>]*sec:authorize="[^"]*SALDO_FAVOR_DEVOLUCION_APROBAR/);
});
