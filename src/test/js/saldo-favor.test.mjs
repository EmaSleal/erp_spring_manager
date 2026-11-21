// T-048 (B1, ADR-053): interfaz del saldo a favor. Opción «Aplicar saldo a favor» (lista, validación en vivo,
// error del servidor sin cerrar), reversión con razón, estado de cuenta, pagos de la factura y reparto de la nota.
// Todo el texto dinámico se pinta con textContent.   node --test 'src/test/js/*.test.mjs'
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
    const ctx = { window: {}, console, Number, Math };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(path.join(JS, 'nota-credito.js'), 'utf8'), ctx);
    vm.runInContext(fs.readFileSync(path.join(JS, 'saldo-favor.js'), 'utf8'), ctx);
    return ctx.window.SaldoFavor;
}

/** Elemento falso: registra textContent, hijos y listeners; falla si alguien asigna innerHTML/outerHTML. */
function crear(tag) {
    const e = {
        tag, hijos: [], attrs: {}, className: '', listeners: {}, _texto: '', value: '', disabled: false, style: {},
        appendChild(h) { this.hijos.push(h); return h; },
        setAttribute(k, v) { this.attrs[k] = String(v); },
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

function documento(ids = []) {
    const mapa = {};
    ids.forEach((id) => { mapa[id] = crear(id.startsWith('sf-select') ? 'select' : 'div'); });
    return { createElement: crear, getElementById: (id) => mapa[id] ?? null, mapa };
}

const SALDO = (extra = {}) => ({ id: 5, idCliente: 1, moneda: 'CRC', montoOriginal: 1000, saldoVivo: 600,
    fechaOrigen: '2026-11-05', numeroNota: 'NC-00007', claveNota: 'K'.repeat(50), estado: 'VIGENTE', ...extra });

// ---- validación en vivo ----

test('validarMonto: mayor a 0, a lo sumo dos decimales, tope por saldo vivo y por saldo pendiente', () => {
    const s = cargar();
    const v = (t) => s.validarMonto(t, 600, 400);
    const base = 'El monto debe ser mayor a 0 y tener a lo sumo dos decimales';
    assert.deepEqual({ ...v('') }, { valido: false, mensaje: base });
    assert.equal(v('0').mensaje, base);
    assert.equal(v('-5').mensaje, base);
    assert.equal(v('abc').mensaje, base);
    assert.equal(v('10.123').mensaje, base);
    assert.equal(v('400.01').mensaje, 'El monto excede el saldo pendiente de la factura destino');
    assert.equal(s.validarMonto('700', 600, 900).mensaje, 'El monto excede el saldo disponible del saldo a favor');
    assert.deepEqual({ ...v('400') }, { valido: true, mensaje: '' });
    assert.equal(v('0.01').valido, true);
    assert.equal(v('399.99').valido, true);
});

test('montoMaximo es el menor entre el saldo vivo y el saldo pendiente', () => {
    const s = cargar();
    assert.equal(s.montoMaximo(600, 400), '400.00');
    assert.equal(s.montoMaximo(300.5, 400), '300.50');
});

test('opción del saldo: nota de origen, fecha y saldo vivo; pintada con textContent', () => {
    const s = cargar();
    assert.equal(s.textoOpcion(SALDO()), 'NC-00007 · 05/11/2026 · disponible ₡600.00');
    assert.equal(s.textoOpcion(SALDO({ numeroNota: null, claveNota: 'ABC' })), 'ABC · 05/11/2026 · disponible ₡600.00');
    assert.equal(s.textoOpcion(SALDO({ moneda: 'USD' })), 'NC-00007 · 05/11/2026 · disponible $600.00');
    const doc = documento();
    const select = crear('select');
    s.pintarOpciones(doc, select, [SALDO({ numeroNota: MAL })]);
    assert.equal(select.hijos.length, 2);
    assert.equal(select.hijos[0].value, '');
    assert.equal(select.hijos[1].value, '5');
    assert.ok(select.hijos[1]._texto.startsWith(MAL));
});

// ---- panel «Aplicar saldo a favor» ----

function panel({ respuestas }) {
    const doc = documento(['sf-panel', 'sf-select', 'sf-monto', 'sf-error', 'sf-ayuda', 'sf-boton', 'sf-info']);
    const llamadas = [];
    const fetchFn = async (url, init) => {
        llamadas.push({ url, init });
        const r = respuestas.shift();
        return { ok: r.ok, json: async () => r.cuerpo };
    };
    return { doc, llamadas, fetchFn, m: doc.mapa };
}
const PARA_FACTURA = { success: true, data: { idFactura: 9, numeroFactura: 'F-9', moneda: 'CRC', saldoPendiente: 400,
    admiteAplicacion: true, saldos: [SALDO()], aplicaciones: [] } };

test('el panel lista solo lo que devuelve /para-factura y se oculta sin saldos o si la factura no admite', async () => {
    const s = cargar();
    let p = panel({ respuestas: [{ ok: true, cuerpo: PARA_FACTURA }] });
    await s.iniciarPanelAplicar(p.doc, p.fetchFn, { idFactura: 9 });
    assert.equal(p.llamadas[0].url, '/api/facturas/saldos-favor/para-factura/9');
    assert.equal(p.m['sf-panel'].style.display, '');
    assert.equal(p.m['sf-select'].hijos.length, 2);

    p = panel({ respuestas: [{ ok: true, cuerpo: { data: { ...PARA_FACTURA.data, saldos: [] } } }] });
    await s.iniciarPanelAplicar(p.doc, p.fetchFn, { idFactura: 9 });
    assert.equal(p.m['sf-panel'].style.display, 'none');

    p = panel({ respuestas: [{ ok: true, cuerpo: { data: { ...PARA_FACTURA.data, admiteAplicacion: false } } }] });
    await s.iniciarPanelAplicar(p.doc, p.fetchFn, { idFactura: 9 });
    assert.equal(p.m['sf-panel'].style.display, 'none');
});

test('al elegir un saldo se propone el máximo y el monto se valida en vivo (botón y mensaje)', async () => {
    const s = cargar();
    const p = panel({ respuestas: [{ ok: true, cuerpo: PARA_FACTURA }] });
    await s.iniciarPanelAplicar(p.doc, p.fetchFn, { idFactura: 9 });
    p.m['sf-select'].value = '5';
    p.m['sf-select'].listeners.change();
    assert.equal(p.m['sf-monto'].value, '400.00');
    assert.equal(p.m['sf-boton'].disabled, false);

    p.m['sf-monto'].value = '500';
    p.m['sf-monto'].listeners.input();
    assert.equal(p.m['sf-boton'].disabled, true);
    assert.equal(p.m['sf-error'].textContent, 'El monto excede el saldo pendiente de la factura destino');
    p.m['sf-monto'].value = '10.5';
    p.m['sf-monto'].listeners.input();
    assert.equal(p.m['sf-boton'].disabled, false);
    assert.equal(p.m['sf-error'].textContent, '');
});

test('el error del servidor se muestra con textContent, sin cerrar ni ocultar el panel', async () => {
    const s = cargar();
    const p = panel({ respuestas: [{ ok: true, cuerpo: PARA_FACTURA },
        { ok: false, cuerpo: { success: false, message: MAL } }] });
    let aplicado = false;
    await s.iniciarPanelAplicar(p.doc, p.fetchFn, { idFactura: 9, onAplicado: () => { aplicado = true; } });
    p.m['sf-select'].value = '5';
    p.m['sf-select'].listeners.change();
    p.m['sf-monto'].value = '100';
    await p.m['sf-boton'].listeners.click();
    assert.equal(p.llamadas[1].url, '/api/facturas/saldos-favor/5/aplicaciones');
    assert.equal(p.llamadas[1].init.method, 'POST');
    assert.equal(p.llamadas[1].init.headers['Content-Type'], 'application/json');
    assert.equal(p.llamadas[1].init.body, '{"idFactura":9,"monto":100}');
    assert.equal(p.m['sf-error'].textContent, MAL);
    assert.equal(p.m['sf-error'].style.display, '');
    assert.equal(p.m['sf-panel'].style.display, '');
    assert.equal(aplicado, false);
    assert.equal(p.m['sf-boton'].disabled, false);
});

test('con éxito llama a onAplicado con la aplicación', async () => {
    const s = cargar();
    const p = panel({ respuestas: [{ ok: true, cuerpo: PARA_FACTURA },
        { ok: true, cuerpo: { success: true, data: { id: 3, numeroPago: 'PAG-1' } } }] });
    let recibido = null;
    await s.iniciarPanelAplicar(p.doc, p.fetchFn, { idFactura: 9, onAplicado: (a) => { recibido = a; } });
    p.m['sf-select'].value = '5';
    p.m['sf-select'].listeners.change();
    await p.m['sf-boton'].listeners.click();
    assert.equal(recibido.id, 3);
});

test('sin saldo elegido o con monto inválido el pedido no sale', async () => {
    const s = cargar();
    const p = panel({ respuestas: [{ ok: true, cuerpo: PARA_FACTURA }] });
    await s.iniciarPanelAplicar(p.doc, p.fetchFn, { idFactura: 9 });
    await p.m['sf-boton'].listeners.click();
    assert.equal(p.llamadas.length, 1);
    assert.equal(p.m['sf-error'].textContent, 'Seleccione un saldo a favor');
});

// ---- reversión ----

test('razón de la reversión: 3 a 180 caracteres tras recortar', () => {
    const s = cargar();
    assert.equal(s.validarRazon('ab').valido, false);
    assert.equal(s.validarRazon('  ab  ').valido, false);
    assert.equal(s.validarRazon('abc').valido, true);
    assert.equal(s.validarRazon('x'.repeat(180)).valido, true);
    assert.equal(s.validarRazon('x'.repeat(181)).valido, false);
    assert.equal(s.validarRazon(null).mensaje, 'La razón es obligatoria y debe tener entre 3 y 180 caracteres');
    assert.equal(s.cuerpoReversion('  Error de captura '), '{"razon":"Error de captura"}');
});

test('reversar: PUT/POST a /aplicaciones/{id}/reversion y devuelve el mensaje del servidor si falla', async () => {
    const s = cargar();
    const llamadas = [];
    const ok = async (url, init) => { llamadas.push({ url, init }); return { ok: true, json: async () => ({ data: { id: 3 } }) }; };
    const r = await s.reversar(ok, 3, 'Error de captura');
    assert.equal(llamadas[0].url, '/api/facturas/saldos-favor/aplicaciones/3/reversion');
    assert.equal(llamadas[0].init.method, 'POST');
    assert.equal(r.ok, true);
    const mal = async () => ({ ok: false, json: async () => ({ message: 'La aplicación ya fue reversada' }) });
    const e = await s.reversar(mal, 3, 'Error de captura');
    assert.deepEqual({ ok: e.ok, mensaje: e.mensaje }, { ok: false, mensaje: 'La aplicación ya fue reversada' });
    const sinCuerpo = async () => ({ ok: false, json: async () => { throw new Error('x'); } });
    assert.equal((await s.reversar(sinCuerpo, 3, 'abc')).mensaje, 'No se pudo completar la operación');
});

// ---- pagos de la factura ----

const PAGO_NC = { idPago: 40, numeroPago: 'PAG-40', fechaPago: '2026-11-10T09:00:00', monto: 100, estado: 'CONFIRMADO',
    estadoDescripcion: 'Confirmado', metodoPago: 'OTROS', metodoPagoDescripcion: 'Otros', tipoPago: 'NOTA_CREDITO',
    idAplicacionSaldoFavor: 3, idNotaOrigen: 7, claveNotaOrigen: 'CLAVE-NC', numeroNotaOrigen: null };
const PAGO_NORMAL = { idPago: 41, numeroPago: 'PAG-41', fechaPago: '2026-11-11T09:00:00', monto: 50, estado: 'CONFIRMADO',
    estadoDescripcion: 'Confirmado', metodoPagoDescripcion: 'Efectivo', tipoPago: 'PAGO_FACTURA' };

test('los pagos NOTA_CREDITO se rotulan «Saldo a favor» con la nota de origen y ofrecen Reversar, no Anular', () => {
    const s = cargar();
    const doc = documento();
    const cont = crear('div');
    s.pintarPagos(doc, cont, { monedaFE: 'CRC', totalPagado: 150, saldoPendiente: 0,
        pagos: [{ ...PAGO_NC, metodoPagoDescripcion: MAL }, PAGO_NORMAL] }, { puedeReversar: true, onReversar() {} });
    const cuerpo = cont.hijos.find((h) => h.tag === 'div').hijos[0].hijos[1];
    const filaNc = cuerpo.hijos[0];
    const texto = textoDe(filaNc);
    assert.match(texto, /Saldo a favor/);
    assert.match(texto, /Nota de origen: CLAVE-NC/);
    assert.match(texto, /Reversar/);
    assert.doesNotMatch(texto, /Anular/);
    assert.doesNotMatch(textoDe(cuerpo.hijos[1]), /Reversar|Saldo a favor/);
});

test('sin permiso de anular pagos no hay botón Reversar; el número de la nota tiene prioridad sobre la clave', () => {
    const s = cargar();
    const doc = documento();
    const cont = crear('div');
    s.pintarPagos(doc, cont, { monedaFE: 'USD', pagos: [{ ...PAGO_NC, numeroNotaOrigen: 'NC-00007' }] },
        { puedeReversar: false });
    const t = textoDe(cont);
    assert.doesNotMatch(t, /Reversar/);
    assert.match(t, /Nota de origen: NC-00007/);
    assert.match(t, /\$100\.00/);
});

test('un pago NOTA_CREDITO anulado no ofrece Reversar', () => {
    const s = cargar();
    const cont = crear('div');
    s.pintarPagos(documento(), cont, { monedaFE: 'CRC', pagos: [{ ...PAGO_NC, estado: 'ANULADO' }] }, { puedeReversar: true });
    assert.doesNotMatch(textoDe(cont), /Reversar/);
});

test('Reversar pide la razón y llama a onReversar con el id de la aplicación y la razón', async () => {
    const s = cargar();
    const cont = crear('div');
    const pedidos = [];
    s.pintarPagos(documento(), cont, { monedaFE: 'CRC', pagos: [PAGO_NC] },
        { puedeReversar: true, onReversar: (id, razon) => pedidos.push([id, razon]) });
    const fila = cont.hijos[cont.hijos.length - 1].hijos[0].hijos[1].hijos[0];
    const acciones = fila.hijos[fila.hijos.length - 1];
    const boton = acciones.hijos.find((h) => h.tag === 'button');
    assert.equal(boton.attrs['data-id-aplicacion'], '3');
    boton.listeners.click();
    assert.deepEqual(pedidos, []);       // aún falta la razón
    const input = acciones.hijos.find((h) => h.tag === 'input');
    const confirmar = acciones.hijos.filter((h) => h.tag === 'button')[1];
    input.value = 'ab';
    confirmar.listeners.click();
    assert.deepEqual(pedidos, []);
    input.value = ' Error de captura ';
    confirmar.listeners.click();
    assert.deepEqual(pedidos, [[3, 'Error de captura']]);
});

// ---- estado de cuenta ----

const ITEMS = [
    { saldo: SALDO({ numeroNota: MAL }), aplicaciones: [
        { id: 3, estado: 'VIGENTE', monto: 400, moneda: 'CRC', numeroFactura: 'F-9', numeroPago: 'PAG-40',
            fechaAplicacion: '2026-11-10T09:00:00' },
        { id: 4, estado: 'REVERSADA', monto: 100, moneda: 'CRC', numeroFactura: 'F-8', fechaAplicacion: '2026-11-08T09:00:00',
            razonReversion: MAL, fechaReversion: '2026-11-09T09:00:00' }] },
    { saldo: SALDO({ id: 6, moneda: 'USD', saldoVivo: 25, montoOriginal: 25, numeroNota: 'NC-8' }), aplicaciones: [
        { id: 7, estado: 'VIGENTE', monto: 5, moneda: 'USD', numeroFactura: 'F-USD', fechaAplicacion: '2026-11-10T09:00:00',
            tcDiaValor: 510.25, tcDiaFecha: '2026-11-10', tcDiaTipo: 'VENTA', diferencialNeto: -1.5 }] },
    { saldo: SALDO({ id: 8, estado: 'AGOTADO', saldoVivo: 0, numeroNota: 'NC-9' }), aplicaciones: [] }
];

test('totales de saldos vivos por moneda: solo VIGENTES', () => {
    const s = cargar();
    assert.deepEqual({ ...s.totalesVivos(ITEMS) }, { CRC: 600, USD: 25 });
    assert.deepEqual({ ...s.totalesVivos([]) }, {});
});

test('estado de cuenta: totales por moneda, un bloque por saldo y aplicaciones con la reversión y su razón', () => {
    const s = cargar();
    const cont = crear('div');
    s.pintarEstadoCuenta(documento(), cont, ITEMS);
    const t = textoDe(cont);
    assert.match(t, /Saldo a favor vivo CRC: ₡600\.00/);
    assert.match(t, /Saldo a favor vivo USD: \$25\.00/);
    assert.ok(t.includes(MAL));                       // razón y nota hostiles como texto
    assert.match(t, /Reversada/);
    assert.match(t, /Razón: /);
    assert.match(t, /F-9/);
    assert.match(t, /TC del día 510\.25/);
    assert.match(t, /Agotado/);
});

test('estado de cuenta sin saldos pinta el aviso y no falla', () => {
    const s = cargar();
    const cont = crear('div');
    s.pintarEstadoCuenta(documento(), cont, []);
    assert.equal(textoDe(cont), 'El cliente no tiene saldos a favor');
});

// ---- nota ----

test('reparto de la nota: a cuentas por cobrar y a saldo a favor', () => {
    const s = cargar();
    assert.equal(s.textoReparto({ estado: 'APLICADA', moneda: 'CRC', montoCreditoCxc: 700, montoSaldoFavor: 300 }),
        'A cuentas por cobrar ₡700.00 · a saldo a favor ₡300.00');
    assert.equal(s.textoReparto({ estado: 'APLICADA', moneda: 'USD', montoCreditoCxc: 10, montoSaldoFavor: 0 }),
        'A cuentas por cobrar $10.00 · a saldo a favor $0.00');
    assert.equal(s.textoReparto({ estado: 'EN_TRAMITE', moneda: 'CRC', montoCreditoCxc: null, montoSaldoFavor: null }), '');
    assert.equal(s.textoReparto({ estado: 'APLICADA', moneda: 'CRC' }), '');
});

// ---- cableado en plantillas y fuentes ----

test('el fuente no usa innerHTML/outerHTML con datos y nota-credito.js carga antes', () => {
    const codigo = fs.readFileSync(path.join(JS, 'saldo-favor.js'), 'utf8');
    assert.match(codigo, /textContent/);
    assert.doesNotMatch(codigo, /\.(innerHTML|outerHTML)\s*[+\-]?=/);
    for (const f of ['modules/facturacion/pagos/form.html', 'modules/facturacion/pagos/estado-cuenta-cliente.html',
        'modules/facturacion/notas-credito-detalle.html', 'shared/components/facturas-modal.html']) {
        const html = fs.readFileSync(path.join(TEMPLATES, f), 'utf8');
        const sf = html.indexOf('js/saldo-favor.js');
        assert.ok(sf > 0, f + ' carga saldo-favor.js');
        assert.ok(html.indexOf('js/nota-credito.js') > 0 && html.indexOf('js/nota-credito.js') < sf, f + ' orden');
    }
});

test('la Reversar de la plantilla se condiciona con PAGO_ANULAR y el panel con PAGO_CREAR', () => {
    const modal = fs.readFileSync(path.join(TEMPLATES, 'shared/components/facturas-modal.html'), 'utf8');
    assert.match(modal, /id="perm-pago-anular"[^>]*sec:authorize="[^"]*PAGO_ANULAR/);
    const form = fs.readFileSync(path.join(TEMPLATES, 'modules/facturacion/pagos/form.html'), 'utf8');
    assert.match(form, /id="sf-panel"/);
});
